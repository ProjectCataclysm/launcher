package cataclysm.launcher.selfupdate;

import cataclysm.launcher.ui.LoadingOverlay;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.LauncherConstants;
import cataclysm.launcher.utils.Log;
import javafx.application.Platform;
import okio.BufferedSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 05.08.2022 16:42
 *
 * @author Knoblul
 */
public class AutoUpdater {
	public static final String VERSION = getVersion();

	static Path updateJarPath() {
		return LauncherConfig.LAUNCHER_DIR_PATH.resolve("launcher_update.jar");
	}

	private static void updateLauncher(LoadingOverlay updateUI) {
		try {
			Path updateJar = updateJarPath();
			Log.msg("Downloading launcher update...");
			String url = "https://" + LauncherConstants.LAUNCHER_URL + "/launcher.jar";

			try (HttpClientWrapper.HttpResponse response = HttpClientWrapper.get(url);
			     BufferedSource source = response.getBody().source();
			     OutputStream out = Files.newOutputStream(updateJar)) {
				Platform.runLater(() -> updateUI.showLoading("Скачиваем обновление...", response.getBody().contentLength()));
				int len;
				byte[] buffer = new byte[8 * 1024];
				while ((len = source.read(buffer)) != -1) {
					out.write(buffer, 0, len);
					int finalLen = len;
					Platform.runLater(() -> updateUI.getLoadingProgress().addProgress(finalLen));
				}
			}

			StubChecker.restart(updateJar);
		} catch (IOException e) {
			throw new RuntimeException("Failed to update launcher", e);
		}
	}

	private static void checkForUpdates(LoadingOverlay updateUI) {
		String version;
		String url = "https://" + LauncherConstants.LAUNCHER_URL + "/version.txt";
		try (HttpClientWrapper.HttpResponse response = HttpClientWrapper.get(url)) {
			version = response.getBody().string().trim();
		} catch (IOException e) {
			throw new RuntimeException("Failed to check version", e);
		}

		if (!VERSION.equals(version)) {
			Log.err("Launcher outdated!");
			// обновляем лаунчер
			updateLauncher(updateUI);
		}
	}
	private static void relaunchExeAsStub(Path jar, LoadingOverlay updateUI) {
		Path stub = StubChecker.stubJarPath();
		if (jar != null && jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe")) {
			if (!Files.isRegularFile(stub)) {
				Log.err("Launcher not downloaded as jar!");
				updateLauncher(updateUI);
			}
		}
	}

	public static boolean updateEnabled() {
		// проверяем, запущены ли мы в IDE?
		return StubChecker.currentJarPath() != null;
	}

	public static void check(LoadingOverlay updateUI) {
		Path jar = StubChecker.currentJarPath();

		// если лаунчер обёрнут в exe, то перезапускаем его в jar
		relaunchExeAsStub(jar, updateUI);

		// проверяем наличие обновлений
		checkForUpdates(updateUI);
	}

	private static String getVersion() {
		try (InputStream in = AutoUpdater.class.getResourceAsStream("/build.properties")) {
			if (in == null) {
				throw new FileNotFoundException();
			}

			Properties properties = new Properties();
			properties.load(in);
			String version = properties.getProperty("version");
			if (version == null) {
				throw new IllegalArgumentException("build.properties does not contains 'version' parameter");
			}

			return version;
		} catch (IOException e) {
			throw new RuntimeException("Failed to get version", e);
		}
	}
}
