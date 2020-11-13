package cataclysm.launcher.utils;

import cataclysm.launcher.Main;
import cataclysm.launcher.io.HttpDownloadRequest;
import cataclysm.launcher.utils.exception.LauncherUpToDateException;
import cataclysm.launcher.utils.logging.Log;
import ru.knoblul.mutex4j.Mutex4j;
import ru.knoblul.mutex4j.MutexAlreadyTakenException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.10.2020 3:35
 *
 * @author Knoblul
 */
public class Updater {
	public static final String VERSION = "1.5.07";
	private static final String STUB_JAR_NAME = "Launcher.jar";
	private static final String UPDATE_FILE_NAME = "Launcher-update.jar";

	public static void checkVersion(Path launcherDirPath) throws IOException {
		if (Files.deleteIfExists(launcherDirPath.resolve(UPDATE_FILE_NAME))) {
			Log.msg("Remove old update file.");
		}

		String version = HttpHelper.getFileContents(HttpHelper.launcherURL("version.txt")).trim();
		if (version.equals(VERSION)) {
			throw new LauncherUpToDateException();
		}
	}

	private static void relaunch(Path launcherDirPath, String jarFile) throws IOException {
		// так как джава на винде лежит в отдельной папке с лаунчером,
		// то запускаем джвм оттуда
		String javaHome = PlatformHelper.getOS() == PlatformHelper.PlatformOS.WINDOWS
				? System.getProperty("java.home") + "/bin/javaw.exe" : "java";

		ProcessBuilder pb = new ProcessBuilder(javaHome, "-jar", jarFile, "--update");
		pb.directory(launcherDirPath.toFile());
		pb.start();

		System.exit(0);
	}

	public static void updateLauncher(Path launcherDirPath) throws IOException {
		if (getRuntimeLocation(true) == null) {
			Log.msg("Skipping launcher update - dev environment detected");
			return;
		}

		HttpDownloadRequest downloadRequest = new HttpDownloadRequest(HttpHelper.launcherURL(STUB_JAR_NAME),
				launcherDirPath.resolve(UPDATE_FILE_NAME));
		HttpHelper.downloadFile(downloadRequest);

		relaunch(launcherDirPath, UPDATE_FILE_NAME);
	}

	private static Path getRuntimeLocation(boolean canBeNull) throws IOException {
		try {
			Path path = Paths.get(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (Files.isDirectory(path) || !path.getFileName().toString().toLowerCase().endsWith(".jar")
					&& !path.getFileName().toString().toLowerCase().endsWith(".exe")) {
				if (canBeNull) {
					return null;
				}

				throw new IOException("Wrong jar file: " + path);
			}
			return path;
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public static void migrateUpdateJarAndRestart(Path launcherDirPath) {
		// ждем пока родительский процесс "отпустит" лок (умрет)
		try {
			Mutex4j.lock(Main.LOCK_ID, true);
		} catch (MutexAlreadyTakenException e) {
			e.printStackTrace();
		}

		try {
			Path stubJarPath = launcherDirPath.resolve(STUB_JAR_NAME);
			Path currentJarPath = getRuntimeLocation(false);
			if (Objects.requireNonNull(currentJarPath).equals(stubJarPath)) {
				throw new IOException("Current jar same as stub jar");
			}

			if (!currentJarPath.getFileName().toString().equals(UPDATE_FILE_NAME)) {
				throw new IOException("Current file name mismatch - must be " +
						UPDATE_FILE_NAME + " but actual name is: " + currentJarPath);
			}

			Files.deleteIfExists(stubJarPath);
			Files.copy(currentJarPath, stubJarPath);
			relaunch(launcherDirPath, STUB_JAR_NAME);
		} catch (IOException e) {
			Log.err(e, "Failed to update laucher");
			javax.swing.JOptionPane.showMessageDialog(null,
					"Не удалось обновить лаунчер:\n" + e.toString(),
					"Ошибка",
					javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}

//	public static void restart() {
//		try {
//			relaunch(Paths.get("."), Objects.requireNonNull(getRuntimeLocation(false)).toString());
//		} catch (IOException e) {
//			Log.warn("Failed to restart launcher");
//		}
//	}
}
