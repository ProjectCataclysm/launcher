package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;
import cataclysm.launcher.utils.PlatformHelper;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created 16 ���. 2018 �. / 22:17:44 
 * @author Knoblul
 */
public class DialogUtils {
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss", Locale.ROOT);

	public static void showError(String message, @Nullable Throwable t, boolean log) {
		if (log) {
			Log.err(t, message != null ? message : "");
		}

		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Ошибка");

		if (t != null) {
			alert.setHeaderText(message);

			VBox dialogPaneContent = new VBox();
			Label label = new Label("Отладочные данные:");
			TextArea textArea = new TextArea();
			textArea.setEditable(false);
			textArea.setText(AwtErrorDialog.getThrowableStackTrace(t));
			dialogPaneContent.getChildren().addAll(label, textArea);
			alert.getDialogPane().setContent(dialogPaneContent);
		} else {
			alert.setHeaderText(null);
			alert.setContentText(message);
		}

		alert.initModality(Modality.APPLICATION_MODAL);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		StageUtils.setupIcons(stage);
		StageUtils.setupScene(stage);
		alert.showAndWait();
	}

	public static void showGameError(int exitCode) {
		String message = String.format("Игра была аварийно завершена с кодом %d (0x%X)\n\nВы желаете сформировать архив "
				+ "с отчетом о данной ошибке? Архив можно будет отправить нам, чтобы мы исправили вашу проблему.", exitCode, exitCode);
		Log.err(message);

		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Ошибка");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.getButtonTypes().setAll(new ButtonType("Сформировать", ButtonBar.ButtonData.YES),
				new ButtonType("Отмена", ButtonBar.ButtonData.NO));

		alert.initModality(Modality.APPLICATION_MODAL);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		StageUtils.setupIcons(stage);
		StageUtils.setupScene(stage);
		alert.showAndWait().ifPresent(selection -> {
			if (selection.getButtonData() == ButtonBar.ButtonData.YES) {
				createReportArchive(exitCode);
			}
		});
	}

	private static Stage showCreatingReportDialog() {
		Stage dialogStage = new Stage();
		dialogStage.setResizable(false);
		dialogStage.initModality(Modality.APPLICATION_MODAL);
		dialogStage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label();
		label.getStyleClass().addAll("title", "bold-font");
		label.setText("Создание архива...");

		ProgressIndicator pin = new ProgressIndicator();
		pin.getStyleClass().add("creation-progress");

		VBox root = new VBox(label, pin);
		root.getStyleClass().add("report-archive-dialog");

		Scene scene = new Scene(root);
		dialogStage.setScene(scene);
		StageUtils.setupIcons(dialogStage);
		StageUtils.setupScene(dialogStage);
		dialogStage.show();
		return dialogStage;
	}

	public static void createReportArchive(int exitCode) {
		Platform.runLater(() -> {
			Stage stage = showCreatingReportDialog();
			Thread t = new Thread(() -> {
				try {
					createReportArchiveImpl(exitCode);
				} catch (IOException e) {
					showError("Не удалось сформировать архив", e, true);
				}

				Platform.runLater(stage::hide);
			});
			t.setDaemon(true);
			t.setName("Report Creating");
			t.start();
		});
	}

	private static void createReportArchiveImpl(int exitCode) throws IOException {
		Path gameDirPath = LauncherApplication.getInstance().getConfig().getCurrentGameDirectoryPath();
		Path launcherDir = LauncherConfig.LAUNCHER_DIR_PATH;
		Path reportsPath = Files.createDirectories(gameDirPath.resolve("reports"));
		Path archivePath = reportsPath.resolve(
				"crash_report_" + LocalDateTime.now().format(DATETIME_FORMATTER) + ".zip");

		Log.msg("Creating report file " + archivePath.getFileName());

		Set<Path> filesToDelete = new HashSet<>();
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archivePath))) {
			if (exitCode != 0) {
				out.putNextEntry(new ZipEntry("exit_code.txt"));
				out.write(String.valueOf(exitCode).getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}

			// папка logs из папки с лаунчером
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(launcherDir.resolve("logs"))) {
				for (Path path : stream) {
					newEntry(path, "launcher/logs", out);
				}
			} catch (NoSuchFileException | FileNotFoundException ignored) {

			}

			// файл настроек лаунчера
			Path launcherCfgPath = launcherDir.resolve("launcher.cfg");
			if (Files.isRegularFile(launcherCfgPath)) {
				newEntry(launcherCfgPath, "launcher", out);
			}

			// папка logs из папки с игрой
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(gameDirPath.resolve("logs"))) {
				for (Path path : stream) {
					newEntry(path, "client/logs", out);
				}
			} catch (NoSuchFileException | FileNotFoundException ignored) {

			}

			// папка crashes из папки с игрой
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(gameDirPath.resolve("crashes"))) {
				for (Path path : stream) {
					if (newEntry(path, "client/crashes", out)) {
						filesToDelete.add(path);
					}
				}
			} catch (NoSuchFileException | FileNotFoundException ignored) {

			}

			// файл настроек игры
			Path settingsTxtPath = Paths.get(System.getProperty("user.home"))
					.resolve(Paths.get("ProjectCataclysm", "settings.txt"));
			if (Files.isRegularFile(settingsTxtPath)) {
				newEntry(settingsTxtPath, "client", out);
			}

			// JVM краш-репорты из корня игры
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(gameDirPath, "*.txt")) {
				for (Path path : stream) {
					if (newEntry(path, "client/crashes", out)) {
						filesToDelete.add(path);
					}
				}
			}

			if (PlatformHelper.getPlatform() == PlatformHelper.Platform.WINDOWS) {
				try {
					Path dxDiagPath = launcherDir.resolve("dxdiag.txt").toAbsolutePath();
					List<String> commandLine = new ArrayList<>();
					commandLine.add("dxdiag.exe");
					commandLine.add("/dontskip");
					commandLine.add("/whql:off");
					commandLine.add("/t");
					commandLine.add(dxDiagPath.toString());
					ProcessBuilder pb = new ProcessBuilder(commandLine);
					pb.start().waitFor();
					if (Files.isRegularFile(dxDiagPath)) {
						newEntry(dxDiagPath, null, out);
					}

					Files.deleteIfExists(dxDiagPath);
				} catch (Exception e) {
					Log.err(e, "DxDiag failed");
				}
			}
		}

//		for (Path path : filesToDelete) {
//			Files.deleteIfExists(path);
//		}

		if (PlatformHelper.getPlatform() == PlatformHelper.Platform.WINDOWS) {
			Runtime.getRuntime().exec("explorer.exe /select," + archivePath);
		} else {
			Desktop.getDesktop().open(archivePath.toFile());
		}
	}

	private static boolean newEntry(Path path, String entryDir, ZipOutputStream out) throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
		if (!attributes.isRegularFile()) {
			return false;
		}

		String fn = path.getFileName().toString();
		if (!fn.endsWith(".txt") && !fn.endsWith(".log") && !fn.endsWith(".hrpof") && !fn.equals("launcher.cfg")) {
			return false;
		}

		Log.msg("> " + path);

		ZipEntry e = new ZipEntry(entryDir != null ? (entryDir + "/" + fn) : fn);
		e.setLastModifiedTime(attributes.lastModifiedTime());
		e.setLastAccessTime(attributes.lastAccessTime());
		e.setCreationTime(attributes.creationTime());

		out.putNextEntry(e);
		Files.copy(path, out);
		out.closeEntry();

		return true;
	}
}
