package cataclysm.launcher.utils.reporter;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.ui.forms.dialog.ModalDialog;
import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.utils.Settings;
import cataclysm.launcher.utils.logging.Log;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 14.10.2020 16:33
 *
 * @author Knoblul
 */
public class CrashReporter {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	public static final String REPORTER_TRACE_FILE = ".reporter-trace";

	public static void showCrashReporterDialog(Launcher launcher) throws IOException {
		Settings settings = launcher.getSettings();
		Path crashReportArchive = CrashReporter.formatCrashReportArchive(settings.gameDirPath);
		Settings.ReporterPolicy reporterPolicy = settings.reporterPolicy;

		boolean shouldSend = reporterPolicy == Settings.ReporterPolicy.ALWAYS_SEND;
		if (crashReportArchive != null && reporterPolicy == Settings.ReporterPolicy.ASK) {
			Label label = new Label("Мы обнаружили, что игра была завершена некорректно." +
					" Чтобы помочь нам решить эту проблему вы можете отправить на наш сервер сообщение об ошибке." +
					" Оно включает в себя архив с логами и краш-репортами с момента последней ошибки.\n" +
					"Вы хотите отправить эти файлы нам?\n");
			label.getStyleClass().add("message");

			IconButton button = new IconButton(crashReportArchive.getFileName().toString(),
					new MaterialDesignIconView(MaterialDesignIcon.ATTACHMENT));
			button.setWrapText(true);
			Tooltip.install(button, new Tooltip("Открыть папку с файлом"));
			button.setOnAction(event -> PlatformHelper.showFile(crashReportArchive));

			CheckBox rememberCheckBox = new CheckBox("Запомнить выбор");
			VBox message = new VBox(label, button, rememberCheckBox);
			message.getStyleClass().add("spaced-pane");

			int result = launcher.showModal(false, "Отчет об ошибке", message);
			if (result != ModalDialog.CLOSE_OPTION) {
				settings.reporterPolicy = result == ModalDialog.YES_OPTION ? Settings.ReporterPolicy.ALWAYS_SEND
						: Settings.ReporterPolicy.NEVER_SEND;
				settings.save();
			}

			shouldSend = result == ModalDialog.YES_OPTION;
		}

		if (crashReportArchive != null && shouldSend) {
			sendCrashReport(crashReportArchive);
		}

		if (!shouldSend) {
			Log.msg("Crash report send aborted.");
		}
	}

	private static void sendCrashReport(Path crashReportArchive) {
//		System.out.println("SENDING " + crashReportArchive);

		if (!Files.exists(crashReportArchive)) {

		}

//		HttpHelper.sendFile(crashReportArchive);
	}

	public static Path createReporterDirPath(Path gameDirPath) throws IOException {
		Path reporterDirectoryPath = gameDirPath.resolve("crashes/reporter");
		if (!Files.exists(reporterDirectoryPath)) {
			Files.createDirectories(reporterDirectoryPath);
		}
		return reporterDirectoryPath;
	}

	private static void collectFiles(Path from, Path to) throws IOException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(from,
				"{crash-log-*.txt,hs_err_pid*.log,fatal_pid_*.log,*.hprof}")) {
			for (Path filePath : ds) {
				Files.move(filePath, to.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	public static Path formatCrashReportArchive(Path gameDirPath) throws IOException {
		Path reporterDirPath = createReporterDirPath(gameDirPath);

		// очищаем директорию краш репортера перед формированием архива
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(reporterDirPath, "{*.txt,*.log,*.hprof}")) {
			for (Path filePath : ds) {
				Files.delete(filePath);
			}
		}

		collectFiles(gameDirPath, reporterDirPath);
		collectFiles(gameDirPath.resolve("crashes"), reporterDirPath);

		Path archiveFilePath = reporterDirPath.resolve("crash-report-" + DATE_FORMAT.format(new Date()) + ".zip");

		int totalWritten = 0;
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archiveFilePath));
			 DirectoryStream<Path> ds = Files.newDirectoryStream(reporterDirPath, "{*.txt,*.log,*.hprof}")) {
			for (Path filePath : ds) {
				ZipEntry zipEntry = new ZipEntry(filePath.getFileName().toString());
				zip.putNextEntry(zipEntry);
				Files.copy(filePath, zip);
				Files.delete(filePath);
				zip.closeEntry();
				totalWritten++;
			}
		}

		if (totalWritten == 0) {
			Files.delete(archiveFilePath);
			return null;
		}

		return archiveFilePath;
	}

	public static boolean deleteCrashReportTrace(Path gameDirPath) {
		try {
			Path reporterDirPath = createReporterDirPath(gameDirPath);
			Path traceFilePath = reporterDirPath.resolve(REPORTER_TRACE_FILE);
			return Files.deleteIfExists(traceFilePath);
		} catch (IOException e) {
			Log.warn(e, "Failed to delete crash-report trace file");
			return false;
		}
	}

	public static void createCrashReportTrace(Path gameDirPath) {
		// по сути трейс-файл будет индикатором о том что случился краш игры
		try {
			Path reporterDirPath = createReporterDirPath(gameDirPath);
			Path traceFilePath = reporterDirPath.resolve(REPORTER_TRACE_FILE);
			if (!Files.exists(traceFilePath)) {
				Files.createFile(traceFilePath);
			}
		} catch (IOException e) {
			Log.warn("Failed to create crash-report trace file");
		}
	}
}
