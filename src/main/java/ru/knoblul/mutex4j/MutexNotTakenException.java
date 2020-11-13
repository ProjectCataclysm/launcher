package ru.knoblul.mutex4j;

/**
 * Exception that get thrown by {@link Mutex4j#sendMessage} when mutex is not locked.
 * <br>Created: 15.10.2020 15:48
 * @author Knoblul
 */
public class MutexNotTakenException extends Exception {
	public MutexNotTakenException(String mutexId) {
		super(mutexId);
	}
}
