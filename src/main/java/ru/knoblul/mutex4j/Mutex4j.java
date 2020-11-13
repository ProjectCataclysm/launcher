package ru.knoblul.mutex4j;

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * <br>Created: 15.10.2020 11:45
 *
 * @author Knoblul
 */
public class Mutex4j {
	/**
	 * Maximum application mutex message length.
	 */
	public static final int MAX_MESSAGE_BYTES_LENGTH = 4096;

	/**
	 * Directory that will be used for storing lock files.
	 */
	private static final Path DEFAULT_LOCK_STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".mutex4j");

	/**
	 * Tracking collection for all application mutex instances created by this application.
	 */
	private static final Set<AppMutex> APP_MUTEXES = new HashSet<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(Mutex4j::freeMutexes));
	}

	/**
	 * Free all application mutexes created by application.
	 */
	private static void freeMutexes() {
		synchronized (APP_MUTEXES) {
			APP_MUTEXES.forEach(AppMutex::free);
			APP_MUTEXES.clear();
		}
	}

	/**
	 * Gets file path to mutex lock file
	 *
	 * @param mutexId the mutex id for which path instance will be created
	 * @return path to lock file for target mutex id
	 */
	private static Path getMutexFilePath(String mutexId) {
		Path lockStoragePath = DEFAULT_LOCK_STORAGE_PATH;//Optional.ofNullable(customLockStoragePath).orElse(DEFAULT_LOCK_STORAGE_PATH);
		if (!Files.exists(lockStoragePath)) {
			try {
				Files.createDirectories(lockStoragePath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create lock storage directory", e);
			}
		}

		int hashCode = mutexId.hashCode();
		String mutexLockFileID = hashCode < 0 ? ("0" + (-hashCode)) : String.valueOf(hashCode);
		return lockStoragePath.resolve(mutexLockFileID + ".lock");
	}

	/**
	 * Attempts to acquire lock for target mutex id.
	 * When success and optional message handler is present - it will be used for handling incoming client messages
	 * in self listening thread (yes, new thread will be created for message handler).
	 *
	 * @param mutexId        the mutex id.
	 * @param messageHandler an optional message handler that will be used for handling incoming messages from clients.
	 * @param blockingMode   if set to <code>true</code> this method will block application until lock is available.
	 * @return optional response from mutex. Will be null if locked mutex does not have {@link MessageHandler} OR if
	 * {@link Message#isShouldWaitResponse()} is false (in that case we simply do not wait for response and immediately return 0).
	 * @throws MutexAlreadyTakenException when mutex for target id already locked or can't failed to lock for target id.
	 */
	public static AppMutex lock(String mutexId, MessageHandler messageHandler, boolean blockingMode)
			throws MutexAlreadyTakenException {
		Path mutexFilePath = getMutexFilePath(mutexId);

		try {
			if (Files.exists(mutexFilePath)) {
				// try remove lock file, if succeed - previous mutex file
				// is probably was not deleted after application exit
				Files.delete(mutexFilePath);
			}

			if (!Files.exists(mutexFilePath)) {
				Files.createFile(mutexFilePath);
			}

			// open mutex file channel (open options indicating that mutex file will be created if it does not exists,
			// and also that channel must be writeable and deleted when channel gets closed)
//			FileChannel channel = (FileChannel) Files.newByteChannel(mutexFilePath, StandardOpenOption.WRITE,
//					StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE);
			// FileChannel.open does not prevent deletion for some reason,
			// so we just open old java's file input stream :)
			FileOutputStream fileOutputStream = new FileOutputStream(mutexFilePath.toFile(), true);

			FileChannel channel;
			FileLock lock;
			try {
				channel = fileOutputStream.getChannel();
				lock = blockingMode ? channel.lock(2, 1, false)
						: channel.tryLock(2, 1, false);
				if (lock == null) {
					throw new OverlappingFileLockException();
				}
			} catch (Throwable e) {
				fileOutputStream.close();
				if (e instanceof OverlappingFileLockException) {
					throw new MutexAlreadyTakenException(mutexId);
				} else {
					throw e;
				}
			}

			AppMutex mutex = new AppMutex(mutexId, mutexFilePath, channel, lock);
			if (messageHandler != null) {
				try {
					mutex.startMessageListening(messageHandler);
				} catch (Throwable e) {
					// when error occurs
					mutex.free();
					throw new IOException("Failed to start message handling server for lock id " + mutexId, e);
				}
			}

			// storing mutex instance in tracking collection
			synchronized (APP_MUTEXES) {
				APP_MUTEXES.add(mutex);
			}

			return mutex;
		} catch (IOException e) {
			throw new MutexAlreadyTakenException(mutexId);
		}
	}

	/**
	 * See {@link #lock(String, MessageHandler, boolean)}
	 */
	public static AppMutex lock(String mutexId, MessageHandler messageHandler) throws MutexAlreadyTakenException {
		return lock(mutexId, messageHandler, false);
	}

	/**
	 * See {@link #lock(String, MessageHandler, boolean)}
	 */
	public static AppMutex lock(String mutexId, boolean blockingMode) throws MutexAlreadyTakenException {
		return lock(mutexId, null, blockingMode);
	}

	/**
	 * See {@link #lock(String, MessageHandler, boolean)}
	 */
	public static AppMutex lock(String mutexId) throws MutexAlreadyTakenException {
		return lock(mutexId, null, false);
	}

	/**
	 * Connects to target port and sends message. Waits for response if necessary.
	 *
	 * @param port    target port
	 * @param message the message object, representing text message that will be sent to target port.
	 * @return optional response from server. Will be null if {@link Message#isShouldWaitResponse()} is <code>false</code>;
	 * @throws IOException              when I/O error occurs.
	 * @throws IllegalArgumentException when {@link Message#getText()}'s bytes is longer
	 *                                  than {@link #MAX_MESSAGE_BYTES_LENGTH}
	 */
	private static String sendMessageViaSocket(int port, Message message) throws IOException {
		byte[] messageBytes = message.getText().getBytes();
		if (messageBytes.length > MAX_MESSAGE_BYTES_LENGTH) {
			throw new IllegalArgumentException("Message byte length is too big: " + messageBytes.length +
					", must be less or equal " + MAX_MESSAGE_BYTES_LENGTH);
		}

		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setSoTimeout(message.getReadTimeout());
			socket.send(new DatagramPacket(messageBytes, 0, messageBytes.length, new InetSocketAddress("localhost", port)));

			if (message.isShouldWaitResponse()) {
				DatagramPacket incomingPacket = new DatagramPacket(new byte[MAX_MESSAGE_BYTES_LENGTH],
						MAX_MESSAGE_BYTES_LENGTH);
				socket.receive(incomingPacket);
				return new String(incomingPacket.getData(), 0, incomingPacket.getLength());
			} else {
				return null;
			}
		}
	}

	/**
	 * Trying to send message to locked mutex on target mutex id.
	 *
	 * @param mutexId target mutex id.
	 * @param message the message object, representing payload that will be send.
	 * @return optional response from mutex. Will be null if locked mutex does not have {@link MessageHandler} OR if
	 * {@link Message#isShouldWaitResponse()} is <code>false</code> (in that case we simply do not wait
	 * for response and immediately return <code>null</code>).
	 * @throws MutexNotTakenException   when mutex for target id not locked.
	 * @throws IllegalArgumentException when {@link Message#getText()}'s bytes is longer
	 *                                  than {@link #MAX_MESSAGE_BYTES_LENGTH}
	 */
	public static String sendMessage(String mutexId, Message message) throws MutexNotTakenException {
		Path mutexFilePath = getMutexFilePath(mutexId);
		if (!Files.isRegularFile(mutexFilePath)) {
			throw new MutexNotTakenException(mutexId);
		}

		// open mutex file channel for writing (locking) and reading purposes only.
		try (FileChannel channel = FileChannel.open(mutexFilePath, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			try {
				FileLock lock = channel.tryLock(2, 1, false);
				if (lock == null) {
					throw new OverlappingFileLockException();
				}

				// if acquire lock succeed - that actually is sign that message will not be send,
				// so we throw exception in that case.
				throw new MutexNotTakenException(mutexId);
			} catch (IOException | OverlappingFileLockException ignored) {

			}

			// read the port value from lock file
			byte[] portBytes = new byte[2];
			if (Channels.newInputStream(channel).read(portBytes) != portBytes.length) {
				throw new EOFException("Failed to read port bytes from lock file");
			}

			// unpack port value from bytes
			int port = ((portBytes[0] << 8) | (portBytes[1] & 0xFF)) & 0xFFFF;

			// do the send-receive operation
			return sendMessageViaSocket(port, message);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MutexNotTakenException(mutexId);
		}
	}

	/**
	 * async version of {@link #sendMessage(String, Message)}.
	 *
	 * @param mutexId  target mutex id.
	 * @param message  the message object, representing message that will be send to mutex.
	 * @param executor an optional executor. If not specified, default executor for CompletableFuture will be used.
	 * @return future, that contains response from mutex.
	 */
	public static CompletableFuture<String> sendMessageAsync(String mutexId, Message message, Executor executor) {
		Supplier<String> task = () -> {
			try {
				return sendMessage(mutexId, message);
			} catch (MutexNotTakenException e) {
				throw new CompletionException(e);
			}
		};
		return executor != null ? CompletableFuture.supplyAsync(task, executor) : CompletableFuture.supplyAsync(task);
	}

	/**
	 * async version of {@link #sendMessage(String, Message)}.
	 *
	 * @param mutexId target mutex id.
	 * @param message the message object, representing message that will be send to mutex.
	 * @return future, that contains response from mutex.
	 */
	public static CompletableFuture<String> sendMessageAsync(String mutexId, Message message) {
		return sendMessageAsync(mutexId, message, null);
	}

	/**
	 * Frees application mutex - close backing lock/channel and stops message listening.
	 * That's it - after this call mutex instance gets invalidated and no longer available
	 * - you can again lock on AppMutex's id.
	 *
	 * @param mutex target application mutex.
	 */
	public static void unlock(AppMutex mutex) {
		synchronized (APP_MUTEXES) {
			if (APP_MUTEXES.remove(mutex)) {
				mutex.free();
			}
		}
	}
}
