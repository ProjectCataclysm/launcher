package cataclysm.launcher.utils;

import org.intellij.lang.annotations.PrintFormat;
import org.tinylog.Level;
import org.tinylog.format.MessageFormatter;
import org.tinylog.format.PrintfStyleFormatter;
import org.tinylog.provider.LoggingProvider;
import org.tinylog.provider.ProviderRegistry;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Created 28 . 2018 . / 18:03:14
 * @author Knoblul
 */
public class Log {
	private static final String ROOT_TAG = "ProjectCataclysm/Launcher";

	public static void msg(String msg) {
		if (Constants.MINIMUM_LEVEL_COVERS_INFO) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, ROOT_TAG, Level.INFO, null,
				Constants.FORMATTER, msg, Constants.EMPTY_ARGS);
		}
	}

	public static void msg(@PrintFormat String fmt, Object... args) {
		if (Constants.MINIMUM_LEVEL_COVERS_INFO) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, ROOT_TAG, Level.INFO, null,
				Constants.FORMATTER, fmt, args);
		}
	}

	public static void err(Throwable t, String msg) {
		if (Constants.MINIMUM_LEVEL_COVERS_ERROR) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, ROOT_TAG, Level.INFO, t,
				Constants.FORMATTER, msg, Constants.EMPTY_ARGS);
		}
	}

	public static void err(Throwable t, @PrintFormat String fmt, Object... args) {
		if (Constants.MINIMUM_LEVEL_COVERS_ERROR) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, ROOT_TAG, Level.INFO, t,
				Constants.FORMATTER, fmt, args);
		}
	}

	public static void err(String msg) {
		if (Constants.MINIMUM_LEVEL_COVERS_ERROR) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, ROOT_TAG, Level.INFO, null,
				Constants.FORMATTER, msg, Constants.EMPTY_ARGS);
		}
	}

	public static void err(@PrintFormat String fmt, Object... args) {
		if (Constants.MINIMUM_LEVEL_COVERS_ERROR) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, ROOT_TAG, Level.INFO, null,
				Constants.FORMATTER, fmt, args);
		}
	}

	public static void logFutureErrors(Object ignored, Throwable cause) {
		if (cause != null) {
			err(cause, "future task error");
		}
	}

	private static void wrapPrintStreams() {
		System.setOut(new LoggingPrintStream(System.out, "STDOUT", Level.INFO));
		System.setErr(new LoggingPrintStream(System.err, "STDERR", Level.ERROR));
	}

	static {
		wrapPrintStreams();
	}

	private interface Constants {
		LoggingProvider PROVIDER = ProviderRegistry.getLoggingProvider();
		MessageFormatter FORMATTER = new PrintfStyleFormatter(Locale.ROOT);

		int STACKTRACE_DEPTH = 2;

		Object[] EMPTY_ARGS = new Object[0];

//		boolean MINIMUM_LEVEL_COVERS_TRACE = isCoveredByMinimumLevel(Level.TRACE);
//		boolean MINIMUM_LEVEL_COVERS_DEBUG = isCoveredByMinimumLevel(Level.DEBUG);
		boolean MINIMUM_LEVEL_COVERS_INFO  = isCoveredByMinimumLevel(Level.INFO);
//		boolean MINIMUM_LEVEL_COVERS_WARN  = isCoveredByMinimumLevel(Level.WARN);
		boolean MINIMUM_LEVEL_COVERS_ERROR = isCoveredByMinimumLevel(Level.ERROR);

		static boolean isCoveredByMinimumLevel(final Level level) {
			return PROVIDER.getMinimumLevel(null).ordinal() <= level.ordinal();
		}
	}

	private static final class LoggingPrintStream extends PrintStream {
		private final String tag;
		private final Level level;

		public LoggingPrintStream(PrintStream original, String tag, Level level) {
			super(original);
			this.tag = tag;
			this.level = level;
		}

		@Override
		public void println(String s) {
			Constants.PROVIDER.log(Constants.STACKTRACE_DEPTH, tag,
				level, null, Constants.FORMATTER, s, Constants.EMPTY_ARGS);
		}

		@Override
		public void println(Object o) {
			println(Objects.toString(o));
		}
	}
}
