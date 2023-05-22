package cataclysm.launcher.utils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LauncherLock {
	private static FileChannel channel;
	private static FileLock lock;

	public static void unlock() {
		try {
			lock.release();
		} catch (IOException e) {
			Log.err(e, "failed to release file lock");
		}

		try {
			channel.close();
		} catch (IOException e) {
			Log.err(e, "failed to close lock file");
		}
	}

	public static void lock() {
		try {
			Path lockPath = LauncherConfig.LAUNCHER_DIR_PATH.resolve("launcher.lock");
			channel = FileChannel.open(lockPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			lock = channel.tryLock();
			if (lock == null) {
				throw new AlreadyLaunchedException();
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file lock", e);
		}
	}

	/**
	 * <br><br>ProjectCataclysm
	 * <br>Created: 07.08.2022 15:32
	 *
	 * @author Knoblul
	 */
	public static class AlreadyLaunchedException extends RuntimeException {

	}
}
