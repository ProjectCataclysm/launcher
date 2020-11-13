package cataclysm.launcher;

import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.utils.Updater;
import cataclysm.launcher.utils.logging.Log;
import cataclysm.launcher.utils.reporter.CrashReporter;
import javafx.application.Application;
import javafx.application.Platform;
import ru.knoblul.mutex4j.Message;
import ru.knoblul.mutex4j.Mutex4j;
import ru.knoblul.mutex4j.MutexAlreadyTakenException;
import ru.knoblul.mutex4j.MutexNotTakenException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.10.2020 0:39
 *
 * @author Knoblul
 */
public class Main {
	public static final String LOCK_ID = "project-cataclysm-launcher";
	private static final String SHOW_COMMAND = "show";
	private static final String REPORT_COMMAND = "report";

	public static void main(String[] args) {
		if (args.length > 0 && args[0].equals("--version")) {
			try {
				Files.write(Paths.get("version.txt"), Collections.singleton(Updater.VERSION));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return;
		}

		Log.msg("Initializing...");

		if (args.length > 0 && args[0].equals("--update")) {
			Log.msg("Update argument detected. Moving self into stub jar...");
			Updater.migrateUpdateJarAndRestart(Paths.get("."));
			return;
		}

		try {
			Mutex4j.lock(LOCK_ID, message -> {
				Log.msg("New JUniqie command: '%s'", message);

				try {
					Platform.runLater(cataclysm.launcher.ui.Launcher.instance::showStage);
					if (message.equals(REPORT_COMMAND)) {
						Platform.runLater(() -> {
							try {
								cataclysm.launcher.ui.Launcher.instance.initCrashReporter();
							} catch (IOException e) {
								Log.warn(e, "Failed to init crash-reporter");
							}
						});
					}
				} catch (Throwable e) {
					Log.warn(e, "Failed to execute JUniqie command '%s'");
				}

				return null;
			});

			Application.launch(cataclysm.launcher.ui.Launcher.class, args);
		} catch (MutexAlreadyTakenException e) {
			Log.msg("Launcher already open / sending mutex commands only");

			if (args.length > 0 && args[0].equals("--reporter")) {
				try {
					Mutex4j.sendMessage(LOCK_ID, new Message(REPORT_COMMAND));
				} catch (MutexNotTakenException x) {
					CrashReporter.createCrashReportTrace(PlatformHelper.getLauncherDirectoryPath());
				}
			} else {
				try {
					Mutex4j.sendMessage(LOCK_ID, new Message(SHOW_COMMAND));
				} catch (MutexNotTakenException x) {
					Log.warn(x, "Failed to send show command");
				}
			}

			Log.msg("Exiting...");
		}
	}
}
