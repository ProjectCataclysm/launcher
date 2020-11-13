package cataclysm.launcher.utils.logging;

import java.io.PrintStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class LoggingPrintStream extends PrintStream {
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
