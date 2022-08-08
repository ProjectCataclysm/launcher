package cataclysm.launcher.utils;

import com.google.common.base.Throwables;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.*;

/**
 * Created 28 . 2018 . / 18:03:14
 * @author Knoblul
 */
public class Log {
	private static final Logger logger;
	
	static {
		PrintStream out = System.out;
		PrintStream err = System.err;

		Logger rootLogger = Logger.getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			rootLogger.removeHandler(handler);
		}

		rootLogger.setUseParentHandlers(false);
		rootLogger.addHandler(new RvmLoggingPrintHandler(out, err));

		try {
			Path logsDir = Files.createDirectories(LauncherConfig.LAUNCHER_DIR_PATH.resolve("logs"));
			String pattern = logsDir.toAbsolutePath() + "/launcher-%g.log";
			FileHandler fileHandler = new FileHandler(pattern, 1024 * 1024 * 100, 3, false);
			rootLogger.addHandler(fileHandler);
		} catch (IOException e) {
			err.printf("Failed to set file handler for logger: %n%s%n", Throwables.getStackTraceAsString(e));
		}

		RvmLogFormatter formatter = new RvmLogFormatter();
		handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			handler.setFormatter(formatter);
			try {
				handler.setEncoding("UTF-8");
			} catch (UnsupportedEncodingException e) {
				err.printf("Failed to set encoding for log handler %s:%n%s%n",
						handler, Throwables.getStackTraceAsString(e));
			}
		}

		System.setOut(new LoggingPrintStream(out, Logger.getLogger("STDOUT"), Level.INFO));
		System.setErr(new LoggingPrintStream(err, Logger.getLogger("STDERR"), Level.SEVERE));

		logger = Logger.getLogger("LAUNCHER");
	}
	
	public static void err(Throwable t, String msg, Object... args) {
		logger.log(Level.SEVERE, "! " + String.format(msg, args), t);
	}
	
	public static void err(String msg, Object... args) {
		err(null, msg, args);
	}
	
	public static void msg(String msg, Object... args) {
		logger.log(Level.INFO, "* " + String.format(msg, args));
	}

	public static void logFutureErrors(Object ignored, Throwable cause) {
		if (cause != null) {
			err(cause, "future task error");
		}
	}

	private static final class RvmLoggingPrintHandler extends Handler {
		private final PrintStream out;
		private final PrintStream err;

		private static final SimpleFormatter DEFAULT_FORMATTER = new SimpleFormatter();

		RvmLoggingPrintHandler(PrintStream out, PrintStream err) {
			this.out = out;
			this.err = err;
		}

		@Override
		public void publish(LogRecord record) {
			Formatter formatter = getFormatter();
			if (formatter == null) {
				formatter = DEFAULT_FORMATTER;
			}

			String message = formatter.format(record);
			if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
				err.print(message);
			} else {
				out.print(message);
			}
		}

		@Override
		public void flush() {
			err.flush();
			out.flush();
		}

		@Override
		public void close() throws SecurityException {
			err.close();
			out.close();
		}
	}

	private static final class RvmLogFormatter extends Formatter {
		// используем SimpleDateFormat из-за того что он быстрее нового Java Time API
		private final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");

		private final String LINE_SEPARATOR = System.getProperty("line.separator");
		private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

		@Override
		public String format(LogRecord record) {
			StringBuilder message = new StringBuilder();

			Level level = record.getLevel();
			String name = level.getLocalizedName();
			if (name == null) {
				name = level.getName();
			}

			String threadName = threadMXBean.getThreadInfo(record.getThreadID()).getThreadName();
			name = threadName + "/" + Optional.ofNullable(name).orElse("");

			message.append(DATE_FORMATTER.format(record.getMillis()));
			message.append(" [").append(name).append("] ");

			if (record.getLoggerName() != null) {
				message.append("[").append(record.getLoggerName()).append("] ");
			} else {
				message.append("[] ");
			}

			message.append(record.getMessage());
			message.append(LINE_SEPARATOR);
			Throwable thr = record.getThrown();

			if (thr != null) {
				message.append(getStackTraceAsString(thr));
			}

			return message.toString();
		}

		private static String getStackTraceAsString(Throwable throwable) {
			StringWriter stringWriter = new StringWriter();
			throwable.printStackTrace(new PrintWriter(stringWriter));
			return stringWriter.toString();
		}
	}

	private static final class LoggingPrintStream extends PrintStream {
		private final Logger logger;
		private final Level level;

		public LoggingPrintStream(PrintStream original, Logger logger, Level level) {
			super(original);
			this.logger = logger;
			this.level = level;
		}

		@Override
		public void println(Object o) {
			logger.log(level, Objects.toString(o));
		}

		@Override
		public void println(String s) {
			logger.log(level, s);
		}
	}
}
