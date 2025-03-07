package cataclysm.launcher.utils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Created 16 ���. 2018 �. / 22:09:12
 *
 * @author Knoblul
 */
public class PlatformHelper {
	private static final Platform PLATFORM;
	private static final String PLATFORM_BUNDLE_ID;
	private static final int MAX_MEMORY_MEGABYTES;

	static {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			PLATFORM = Platform.WINDOWS;
		} else if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("SunOS")
				|| osName.startsWith("Unix")) {
			PLATFORM = Platform.LINUX;
		} else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
			PLATFORM = Platform.MAC;
		} else {
			throw new IllegalArgumentException("Unknown platform: " + osName);
		}

		String bundleId = PLATFORM.name().toLowerCase(Locale.ROOT);

//		String arch = System.getProperty("os.arch");
//		switch (PLATFORM) {
//			case LINUX:
//				PLATFORM_BUNDLE_ID = arch.startsWith("arm") || arch.startsWith("aarch64")
//						? "linux-" + (arch.contains("64") || arch.contains("armv8") ? "arm64" : "arm32")
//						: "linux";
//				break;
//			case MAC:
//				PLATFORM_BUNDLE_ID = "macos";
//				break;
//			case WINDOWS:
//				PLATFORM_BUNDLE_ID = System.getenv("ProgramFiles(x86)") != null ? "win64" : "win32";
//				break;
//			default:
//				throw new IllegalArgumentException("Unknown platform: " + PLATFORM);
//		}

		PLATFORM_BUNDLE_ID = bundleId;

		int maxMemory = 0;
		try {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			Object attribute = mBeanServer.getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"), "TotalPhysicalMemorySize");
			if (!(attribute instanceof Long)) {
				throw new IllegalArgumentException("No such attribute: TotalPhysicalMemorySize");
			}

			maxMemory = (int) ((Long) attribute / 1024 / 1024);
		} catch (Throwable t) {
			Log.err(t, "Failed to determine max physical memory size");
		}

		// Ограничиваем в 32 гб
		MAX_MEMORY_MEGABYTES = maxMemory != 0 ? maxMemory : 16 * 1024;
	}

	public static String getPlatformBundleId() {
		return PLATFORM_BUNDLE_ID;
	}

	public static Platform getPlatform() {
		return PLATFORM;
	}

	public static Path getDefaultGameDirectory() {
		String path = ".project-cataclysm/";
		switch (PLATFORM) {
			case WINDOWS:
				return Paths.get(System.getenv("APPDATA"), path);
			case MAC:
				return Paths.get(System.getProperty("user.home", ""), "Library/Application Support/" + path);
			default:
				return Paths.get(System.getProperty("user.home", ""), path);
		}
	}

	public static int getMaxMemoryMegabytes() {
		return MAX_MEMORY_MEGABYTES;
	}

	public enum Platform {
		WINDOWS,
		MAC,
		LINUX
	}
}
