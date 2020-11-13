package ru.knoblul.mutex4j;

/**
 * Exception that get thrown when mutex already locked.
 * <br>Created: 15.10.2020 11:46
 * @author Knoblul
 */
public class MutexAlreadyTakenException extends Exception {
	public MutexAlreadyTakenException(String mutexId) {
		super(mutexId);
	}
}
