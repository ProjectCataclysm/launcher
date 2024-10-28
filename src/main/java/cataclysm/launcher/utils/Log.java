package cataclysm.launcher.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.PrintStream;
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

//		String pattern = "[%d{yyyy-MM-dd HH:mm:ss}] [%t/%level] [%c] %msg%n";
//		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//		Configuration config = ctx.getConfiguration();
//
//		AbstractAppender appender = new ConsoleAppender.Builder<>()
//			.setName("stdout")
//			.setLayout(PatternLayout.newBuilder()
//				.withPattern(pattern)
//				.build())
//			.build();
//		if (appender == null) {
//			throw new RuntimeException("Failed to create console appender for root logger");
//		}
//		appender.start();
//		config.addAppender(appender);
//		config.getRootLogger().addAppender(appender, Level.INFO, null);
//
//		appender = new RollingFileAppender.Builder<>()
//			.setName("RootFile")
//			.setLayout(PatternLayout.newBuilder()
//				.withPattern(pattern)
//				.build())
//			.withFileName("logs/latest-launcher.log")
//			.withFilePattern("logs/latest-launcher-%i.log")
//			.withPolicy(OnStartupTriggeringPolicy.createPolicy(0))
//			.withStrategy(DefaultRolloverStrategy.newBuilder().withMax("6").build())
//			.build();
//		if (appender == null) {
//			throw new RuntimeException("Failed to create file appender for root logger");
//		}
//
//		appender.start();
//		config.addAppender(appender);
//		config.getRootLogger().addAppender(appender, Level.INFO, null);
//
//		ctx.updateLoggers();

		ConfigurationBuilder<BuiltConfiguration> configBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();
		LayoutComponentBuilder pattern = configBuilder.newLayout("PatternLayout");
		pattern.addAttribute("pattern", "[%d{yyyy-MM-dd HH:mm:ss}] [%t/%level] [%c] %msg%n");

		AppenderComponentBuilder console = configBuilder.newAppender("stdout", "Console");
		console.add(pattern);
		configBuilder.add(console);

		AppenderComponentBuilder rollingFile = configBuilder.newAppender("file", "RollingFile");
		rollingFile.addAttribute("fileName", "logs/latest-launcher.log");
		rollingFile.addAttribute("filePattern", "logs/latest-launcher-%i.log");
		rollingFile.addComponent(configBuilder.newComponent("Policies")
			.addComponent(configBuilder.newComponent("OnStartupTriggeringPolicy")
				.addAttribute("minSize", 0)));
		rollingFile.addComponent(configBuilder.newComponent("DefaultRolloverStrategy")
				.addAttribute("max", 6));
		rollingFile.add(pattern);
		configBuilder.add(rollingFile);

		RootLoggerComponentBuilder rootLogger = configBuilder.newRootLogger(Level.INFO);
		rootLogger.add(configBuilder.newAppenderRef("stdout"));
		rootLogger.add(configBuilder.newAppenderRef("file"));
		configBuilder.add(rootLogger);

		Configurator.initialize(configBuilder.build());

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
