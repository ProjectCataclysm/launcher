package cataclysm.utils;

import cataclysm.ui.ConfigFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created 16 ���. 2018 �. / 22:09:12
 *
 * @author Knoblul
 */
public class PlatformHelper {
	private static final PlatformOS PLATFORM;
	private static String osArchIdentifier;

	static {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			PLATFORM = PlatformOS.WINDOWS;
		} else if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("SunOS")
				|| osName.startsWith("Unix")) {
			PLATFORM = PlatformOS.LINUX;
		} else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
			PLATFORM = PlatformOS.MAC;
		} else {
			throw new RuntimeException("Unknown platform: " + osName);
		}

		String arch = System.getProperty("os.arch");
		switch (PLATFORM) {
			case LINUX:
				osArchIdentifier = arch.startsWith("arm") || arch.startsWith("aarch64")
						? "linux-" + (arch.contains("64") || arch.contains("armv8") ? "arm64" : "arm32")
						: "linux";
				break;
			case MAC:
				osArchIdentifier = "macos";
				break;
			case WINDOWS:
				osArchIdentifier = System.getenv("ProgramFiles(x86)") != null ? "win64" : "win32";
				break;
		}
	}

	public static String getOsArchIdentifier() {
		return osArchIdentifier;
	}

	public static PlatformOS getOS() {
		return PLATFORM;
	}

	public static Path getDefaultGameDirectory() {
		String path = ".project-cataclysm/";
		switch (getOS()) {
			case WINDOWS:
				return Paths.get(System.getenv("APPDATA"), path);
			case MAC:
				return Paths.get(System.getProperty("user.home", ""), "Library/Application Support/" + path);
			default:
				return Paths.get(System.getProperty("user.home", ""), path);
		}
	}

	public static int getAvailableMemory() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		return ConfigFrame.roundUpToPowerOfTwo((int) (maxMemory / 1024 / 1024));
	}

	public static int getMaximumMemory() {
		return osArchIdentifier.contains("64") ? 16 * 1024 : 2048;
	}

	public static void setLookAndFeel() {
		ImageIO.setUseCache(false); // Disable on-disc stream cache should speed up texture pack reloading.

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	public enum PlatformOS {
		WINDOWS,
		MAC,
		LINUX
	}
}
