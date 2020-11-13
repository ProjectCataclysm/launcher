package ru.knoblul.mutex4j;

/**
 * Interface that handles incoming messages from clients.
 *
 * <br>Created: 15.10.2020 12:45
 * @author Knoblul
 */
public interface MessageHandler {
	/**
	 * @param message text, received from client
	 * @return optional text that will be send to client
	 */
	String handleMessage(String message);
}
