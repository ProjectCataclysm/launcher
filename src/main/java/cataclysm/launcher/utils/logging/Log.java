package cataclysm.launcher.utils.logging;

import com.google.common.base.Throwables;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <br><br><i>Created 24 янв. 2019 г. / 20:36:56</i><br>
 * SME REDUX / NOT FOR FREE USE!
 *
 * @author Knoblul
 */
public final class Log {
	private static Logger logger;

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
			String pattern = "logs/launcher-latest-%g.log";
			Path logsDir = Paths.get("logs");
			if (!Files.exists(logsDir)) {
				Files.createDirectories(logsDir);
			}

			// на сервере логи будут скапливаться по 100 мб в файле, затем ротейтится
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
	}

	public static Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger("Launcher");
		}
		return logger;
	}

	public static void msg(String msg, Object... args) {
		getLogger().log(Level.INFO, args.length == 0 ? msg : String.format(Locale.US, msg, args));
	}

	public static void err(String msg, Object... args) {
		err(null, msg, args);
	}

	public static void err(Throwable t, String msg, Object... args) {
		getLogger().log(Level.SEVERE, args.length == 0 ? msg : String.format(Locale.US, msg, args), t);
	}

	public static void warn(String msg, Object... args) {
		warn(null, msg, args);
	}

	public static void warn(Throwable t, String msg, Object... args) {
		getLogger().log(Level.WARNING, args.length == 0 ? msg : String.format(Locale.US, msg, args), t);
	}

	public static void dbg(String msg, Object... args) {
		getLogger().log(Level.FINEST, args.length == 0 ? msg : String.format(Locale.US, msg, args));
	}
}
