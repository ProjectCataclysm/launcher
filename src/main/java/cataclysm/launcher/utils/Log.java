package cataclysm.launcher.utils;

import java.text.SimpleDateFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.base.Throwables;

/**
 * Created 28 . 2018 . / 18:03:14
 * @author Knoblul
 */
public class Log {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static Logger logger;
	
	static {
		logger = Logger.getLogger("LAUNCHER");
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new Formatter() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			@Override
			public String format(LogRecord record) {
				StringBuffer msg = new StringBuffer();
				Level lvl = record.getLevel();
				String name = lvl.getLocalizedName();

				msg.append(dateFormat.format(Long.valueOf(record.getMillis())));
				
				if (name == null) {
					name = lvl.getName();
				}

				if ((name != null) && (name.length() > 0)) {
					msg.append(" [" + name + "] ");
				} else {
					msg.append(" ");
				}

				if (record.getLoggerName() != null) {
					msg.append("[" + record.getLoggerName() + "] ");
				} else {
					msg.append("[] ");
				}
				
				msg.append(record.getMessage());
		        msg.append(LINE_SEPARATOR);
				
				Throwable t = record.getThrown();
				if (t != null) {
					msg.append(Throwables.getStackTraceAsString(t));
				}
				
				return msg.toString();
			}
		});
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
	}
	
	public static void err(Throwable t, String msg, Object... args) {
		logger.log(Level.SEVERE, "! " + String.format(msg, args), t);
	}
	
	public static void err(String msg, Object... args) {
		err(null, msg, args);
	}
	
	public static void msg(String msg, Object... args) {
		logger.log(Level.FINE, "* " + String.format(msg, args));
	}
}
