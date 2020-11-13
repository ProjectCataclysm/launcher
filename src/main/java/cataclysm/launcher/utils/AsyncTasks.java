package cataclysm.launcher.utils;

import cataclysm.launcher.utils.logging.Log;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 09.10.2020 18:41
 *
 * @author Knoblul
 */
public class AsyncTasks {
	public static final Executor executor;

	static {
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("Launcher Task Executor")
				.setDaemon(true)
				.setUncaughtExceptionHandler((t, e) -> Log.err(e, "Exception in thread " + t.getName()))
				.build();

		executor = Executors.newSingleThreadExecutor(threadFactory);
	}

	private static Throwable unpackException(Throwable exception) {
		if (exception instanceof CompletionException) {
			exception = exception.getCause();
		}

		return exception;
	}

	public static <T> void whenComplete(CompletableFuture<T> future, BiConsumer<T, Throwable> o) {
		future.whenComplete((v, e) -> o.accept(v, unpackException(e)));
	}
}
