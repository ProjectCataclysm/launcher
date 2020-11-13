package ru.knoblul.mutex4j;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Representation of application mutex, that contains FileChannel and FileLock instances.
 * Also contains optional message listener, that gets created when {@link #startMessageListening(MessageHandler)}
 * called (e.g. when message handler gets passed into {@link Mutex4j#lock} method)
 * <br>Created: 15.10.2020 15:24
 * @author Knoblul
 */
public class AppMutex {
	private final String mutexId;
	private final Path mutexFilePath;
	private final FileChannel channel;
	private final FileLock lock;

	private MessageHandlerServer server;

	AppMutex(String mutexId, Path mutexFilePath, FileChannel channel, FileLock lock) {
		this.mutexId = mutexId;
		this.mutexFilePath = mutexFilePath;
		this.channel = channel;
		this.lock = lock;
	}

	public String getMutexId() {
		return mutexId;
	}

	/**
	 * This method creating new message listener for specified message handler,
	 * binding socket on random port, storing that port into managed lock file,
	 * spawning thread and starts listening for messages in that thread.
	 *
	 * @param messageHandler will be used in message listener when new messages arrives
	 * @throws IOException when I/O error occurs
	 */
	void startMessageListening(MessageHandler messageHandler) throws IOException {
		server = new MessageHandlerServer(mutexId, messageHandler);
		int port = server.bind();
		Channels.newOutputStream(channel).write(new byte[] { (byte) (port >> 8), (byte) port });
		server.listen();
	}

	/**
	 * Frees all used resources by this application mutex instance.
	 */
	void free() {
		try {
			lock.release();
		} catch (Throwable ignored) {

		}

		try {
			channel.close();
		} catch (Throwable ignored) {

		}

		if (server != null) {
			try {
				server.stop();
			} catch (Throwable ignored) {

			}
		}

		try {
			Files.delete(mutexFilePath);
		} catch (Throwable ignored) {

		}
	}

	@Override
	public String toString() {
		return "AppMutex{"+ mutexId +'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AppMutex mutex = (AppMutex) o;
		return mutexId.equals(mutex.mutexId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mutexId);
	}
}
