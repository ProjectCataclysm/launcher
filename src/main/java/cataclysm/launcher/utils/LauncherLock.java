package cataclysm.launcher.utils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LauncherLock {
	public static void lock() {
		try {
			Path lockPath = LauncherConfig.LAUNCHER_DIR_PATH.resolve("launcher.lock");
			FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			FileLock lock = channel.tryLock();
			if (lock == null) {
				throw new AlreadyLaunchedException();
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file lock", e);
		}
	}

	/**
	 * <br><br>REVOM ENGINE / ProjectCataclysm
	 * <br>Created: 07.08.2022 15:32
	 *
	 * @author Knoblul
	 */
	public static class AlreadyLaunchedException extends RuntimeException {

	}
}
