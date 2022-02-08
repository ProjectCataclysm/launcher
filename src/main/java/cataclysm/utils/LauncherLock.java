package cataclysm.utils;

import cataclysm.launch.Launcher;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LauncherLock {
	private static FileChannel channel;
	private static FileLock lock;

	public static void lock() throws IOException {
		Path lockPath = Launcher.workDirPath.resolve("launcher.lock");
		if (!Files.exists(lockPath)) {
			Files.createFile(lockPath);
		}
		channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
		lock = channel.tryLock();
		if (lock == null) {
			throw new IOException("Two instances cant run at the same time");
		}
	}

	public static boolean isAvailable() {
		try {
			lock();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void unlock() {
		try {
			lock.close();
		} catch (IOException ignored) {
		}

		try {
			channel.close();
		} catch (Throwable ignored) {
		}
	}
}
