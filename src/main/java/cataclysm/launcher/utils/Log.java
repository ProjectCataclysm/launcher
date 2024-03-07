package cataclysm.launcher.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Created 28 . 2018 . / 18:03:14
 * @author Knoblul
 */
public class Log {
	private static Logger logger;

	private static Logger getLogger() {
		if (logger == null) {
			logger = LogManager.getLogger("ProjectCataclysm/Launcher");
		}

		return logger;
	}

	public static void err(Throwable t, String msg, Object... args) {
		getLogger().log(Level.ERROR, "! " + String.format(msg, args), t);
	}
	
	public static void err(String msg, Object... args) {
		err(null, msg, args);
	}
	
	public static void msg(String msg, Object... args) {
		getLogger().log(Level.INFO, "* " + String.format(msg, args));
	}

	public static void logFutureErrors(Object ignored, Throwable cause) {
		if (cause != null) {
			err(cause, "future task error");
		}
	}

	public static void configureLogging() {
		PrintStream out = System.out;
		PrintStream err = System.err;

		// из-за fat-jar приходится юзать программную загрузку файла конфигурации log4j
		try (InputStream in = Log.class.getResourceAsStream("/log4j2.xml")) {
			if (in == null) {
				throw new NoSuchFileException("Resource not found");
			}

			ConfigurationSource source = new ConfigurationSource(in);
			Configurator.initialize(null, source);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load log4j configuration file", e);
		}

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();

		RollingFileAppender.Builder<?> builder = new RollingFileAppender.Builder<>();
		builder.setName("RootFile");
		builder.setLayout(PatternLayout.newBuilder()
			.withPattern(config.getStrSubstitutor().getVariableResolver().lookup("pattern"))
			.build());
		builder.withFileName("logs/latest-launcher.log");
		builder.withFilePattern("logs/latest-launcher-%i.log");
		builder.withPolicy(OnStartupTriggeringPolicy.createPolicy(0));
		builder.withStrategy(DefaultRolloverStrategy.newBuilder().withMax("6").build());
		RollingFileAppender appender = builder.build();
		if (appender == null) {
			throw new RuntimeException("Failed to create file appender for root logger");
		}

		appender.start();
		config.addAppender(appender);
		config.getRootLogger().addAppender(appender, Level.INFO, null);

		ctx.updateLoggers();

		System.setOut(new LoggingPrintStream(out, LogManager.getLogger("STDOUT"), Level.INFO));
		System.setErr(new LoggingPrintStream(err, LogManager.getLogger("STDERR"), Level.ERROR));
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
