package cataclysm.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.UUID;

import cataclysm.launch.Launcher;

public class LauncherLock {
	private static FileOutputStream stream;
	private static FileLock lock;
	
	public static void lock() throws IOException {
		File lockFile = new File(Launcher.workDir, ".lock");
		if (!lockFile.exists()) {
			try (OutputStream out = Files.newOutputStream(lockFile.toPath())) {
				out.write(UUID.randomUUID().toString().getBytes());
			}
		}
		stream = new FileOutputStream(lockFile);
		lock = stream.getChannel().tryLock();
	}
	
	public static boolean isAvaible() {
		try {
			lock();
			return lock != null;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static void unlock() {
		try {
			lock.release();
			stream.close();
		} catch (Throwable e) {
			
		}
	}
}
