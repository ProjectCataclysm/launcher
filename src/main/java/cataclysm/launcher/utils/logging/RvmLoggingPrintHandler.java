package cataclysm.launcher.utils.logging;

import java.io.PrintStream;
import java.util.logging.*;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 19.09.2020 14:53
 *
 * @author Knoblul
 */
final class RvmLoggingPrintHandler extends Handler {
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
