package ru.knoblul.mutex4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Optional;

/**
 * <br>Created: 15.10.2020 13:05
 * @author Knoblul
 */
class MessageHandlerServer implements Runnable {
	private final String lockId;
	private final MessageHandler messageHandler;

	private DatagramSocket socket;

	private boolean listening;

	MessageHandlerServer(String lockId, MessageHandler messageHandler) {
		this.lockId = lockId;
		this.messageHandler = messageHandler;
	}

	public int bind() throws IOException {
		socket = new DatagramSocket();
		return socket.getLocalPort();
	}

	public void listen() {
		Thread listenThread = new Thread(this);
		listenThread.setDaemon(true);
		listenThread.setName("AppMutex Message Listener for " + lockId);
		listenThread.start();
	}

	public void stop() {
		listening = false;
		socket.close();
	}

	@Override
	public void run() {
		listening = true;
		while (listening) {
			try {
				DatagramPacket incomingPacket = new DatagramPacket(new byte[Mutex4j.MAX_MESSAGE_BYTES_LENGTH],
						Mutex4j.MAX_MESSAGE_BYTES_LENGTH);
				socket.receive(incomingPacket);
				String message = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
				String response = Optional.ofNullable(messageHandler.handleMessage(message)).orElse("");
				byte[] responseBytes = response.getBytes();
				socket.send(new DatagramPacket(responseBytes, 0, responseBytes.length, incomingPacket.getSocketAddress()));
			} catch (IOException e) {
				break;
			}
		}
	}
}
