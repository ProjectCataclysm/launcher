package cataclysm.launcher.download;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * <br><br>launcher
 * <br>Created: 08.03.2025 3:55
 *
 * @author Knoblul
 */
class MigrationHelper {
	private static void deleteDirRecursive(Path dirPath) {
		try (Stream<Path> paths = Files.walk(dirPath)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					Log.err(e, "");
				}
			});
		} catch (IOException e) {
			Log.err(e, "");
		}
	}

	private static void moveDirRecursive(Path srcDirPath, Path dstDirPath) {
		try (Stream<Path> paths = Files.walk(srcDirPath)) {
			Files.createDirectories(dstDirPath);
			paths.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					if (srcDirPath.equals(p)) {
						Files.delete(p);
					} else {
						Files.move(p, dstDirPath.resolve(srcDirPath.relativize(p)),
							StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (IOException e) {
					Log.err(e, "");
				}
			});
		} catch (IOException e) {
			Log.err(e, "");
		}
	}

	private static void migrateOldFiles(Path gameDirPath, Path rootGameDirPath) {
		deleteDirRecursive(rootGameDirPath.resolve("jre8"));
		deleteDirRecursive(rootGameDirPath.resolve("bin"));
		moveDirRecursive(rootGameDirPath.resolve("res"), gameDirPath.resolve("res"));
		moveDirRecursive(rootGameDirPath.resolve("stats"), gameDirPath.resolve("stats"));
		moveDirRecursive(rootGameDirPath.resolve("crashes"), gameDirPath.resolve("crashes"));
		moveDirRecursive(rootGameDirPath.resolve("logs"), gameDirPath.resolve("logs"));

		try {
			Files.delete(rootGameDirPath.resolve("core.jar"));
		} catch (IOException e) {
			Log.err(e, "");
		}

		try {
			Files.createDirectories(gameDirPath.resolve("res"));
		} catch (IOException e) {
			Log.err(e, "");
		}
	}

	static CompletableFuture<Void> migrate(Path gameDirPath, DownloadingManager.DownloadProgressListener listener, Executor executor) {
		LauncherConfig config = LauncherApplication.getInstance().getConfig();
		Path rootGameDirPath = config.getGameDirectoryPath();
		if (Files.exists(rootGameDirPath.resolve("core.jar"))
			&& config.clientBranch == LauncherConfig.ClientBranch.MAIN) {
			return CompletableFuture.runAsync(() -> {
				listener.updateState(DownloadingManager.DownloadState.CHECKING_FILES);
				MigrationHelper.migrateOldFiles(gameDirPath, rootGameDirPath);
			}, executor);
		}

		return CompletableFuture.completedFuture(null);
	}
}
