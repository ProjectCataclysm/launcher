package cataclysm.launcher.utils.logging;

import com.google.common.base.Throwables;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class RvmLogFormatter extends Formatter {
	private final String LINE_SEPARATOR = System.getProperty("line.separator");
	private final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");
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
			message.append(Throwables.getStackTraceAsString(thr));
		}

		return message.toString();
	}
}
