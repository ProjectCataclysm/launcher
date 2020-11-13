package cataclysm.launcher.utils;

import cataclysm.launcher.utils.logging.Log;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created 16 авг. 2018 г. / 22:09:12
 *
 * @author Knoblul
 */
public class PlatformHelper {
	private static final PlatformOS PLATFORM;
	private static String osArchIdentifier;

	private static boolean awtInit;
	private static Method awtDesktopBrowseMethod;
	private static Method awtDesktopOpenMethod;
	private static Object awtDesktopInstance;

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

		Log.msg("[SystemInfo] OS: %s (%s), OS Arch: %s, MaxRam: %dMB", osName, PLATFORM,
				osArchIdentifier, getMaximumMemoryMB());
	}

	public static String getOsArchIdentifier() {
		return osArchIdentifier;
	}

	public static PlatformOS getOS() {
		return PLATFORM;
	}

	public static Path getLauncherDirectoryPath() {
		Path launcherDirPath = Paths.get(".");

		if (getOS() != PlatformOS.WINDOWS) {
			String path = ".project-cataclysm/";
//			case WINDOWS:
//				launcherDirPath = Paths.get(System.getenv("APPDATA"), path);
//				break;
			if (getOS() == PlatformOS.MAC) {
				launcherDirPath = Paths.get(System.getProperty("user.home", ""), "Library/Application Support/" + path);
			} else {
				launcherDirPath = Paths.get(System.getProperty("user.home", ""), path);
			}
		}

		if (Files.exists(launcherDirPath)) {
			try {
				Files.createDirectories(launcherDirPath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to get launcher work dir", e);
			}
		}

		return launcherDirPath;
	}

	/**
	 * Returns the input value rounded up to the next highest power of two.
	 */
	private static int roundUpToPowerOfTwo(int value) {
		int result = value - 1;
		result |= result >> 1;
		result |= result >> 2;
		result |= result >> 4;
		result |= result >> 8;
		result |= result >> 16;
		return result + 1;
	}

	public static int getMaximumMemoryMB() {
		try {
			long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory
					.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
			return roundUpToPowerOfTwo((int) (memorySize / 1024 / 1024 / 2));
		} catch (Exception e) {
			return osArchIdentifier.contains("64") ? 4 * 1024 : 2048;
		}
	}

	private static void initAwtMethods() {
		if (!awtInit) {
			try {
				Class<?> desktopClass = Class.forName("java.awt.Desktop");
				Method desktopGetDesktop = desktopClass.getMethod("getDesktop");
				awtDesktopInstance = desktopGetDesktop.invoke(null);
				awtDesktopBrowseMethod = desktopClass.getMethod("browse", URI.class);
				awtDesktopOpenMethod = desktopClass.getMethod("open", File.class);
			} catch (Exception e) {
				Log.warn(e, "Failed get awt desktop browse/open method");
			}

			awtInit = true;
		}
	}

	public static void browseURL(String link) {
		initAwtMethods();

		if (awtDesktopBrowseMethod != null) {
			try {
				awtDesktopBrowseMethod.invoke(awtDesktopInstance, URI.create(link));
			} catch (Exception e) {
				Log.warn(e, "Failed to browse URL %s", link);
			}
		}
	}

	public static void openFile(Path filePath) {
		initAwtMethods();

		if (awtDesktopOpenMethod != null) {
			try {
				awtDesktopOpenMethod.invoke(awtDesktopInstance, filePath.toFile());
			} catch (Exception e) {
				Log.warn(e, "Failed to open file %s", filePath);
			}
		}
	}

	public static void showFile(Path filePath) {
		if (getOS() == PlatformOS.WINDOWS) {
			try {
				Runtime.getRuntime().exec("explorer.exe /select," + filePath);
				return;
			} catch (IOException e) {
				Log.warn(e, "Failed to show file %s, running fallback method...", filePath);
			}
		}

		openFile(filePath.getParent());
	}

	public enum PlatformOS {
		WINDOWS,
		MAC,
		LINUX
	}
}
