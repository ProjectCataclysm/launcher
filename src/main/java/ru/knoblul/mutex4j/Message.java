package ru.knoblul.mutex4j;

/**
 * <br>Created: 15.10.2020 16:04
 * @author Knoblul
 */
public class Message {
	private final String text;

	/**
	 * Flag indicating that after sending the message we should
	 * wait for response from mutex.
	 */
	private boolean shouldWaitResponse;

	/**
	 * Read timeout, in milliseconds, for socket that we reading response from.
	 * Value 0 indicating that no read timeout has been specified.
	 */
	private int readTimeout;

	public Message(String text) {
		this.text = text;
	}

	public Message(String text, boolean shouldWaitResponse) {
		this.text = text;
		this.shouldWaitResponse = shouldWaitResponse;
	}

	public Message(String text, boolean shouldWaitResponse, int readTimeout) {
		this.text = text;
		this.shouldWaitResponse = shouldWaitResponse;
		this.readTimeout = readTimeout;
	}

	public String getText() {
		return text;
	}

	public boolean isShouldWaitResponse() {
		return shouldWaitResponse;
	}

	public void setShouldWaitResponse(boolean shouldWaitResponse) {
		this.shouldWaitResponse = shouldWaitResponse;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}
}
