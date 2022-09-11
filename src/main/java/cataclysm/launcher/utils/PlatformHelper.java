package cataclysm.launcher.utils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created 16 ���. 2018 �. / 22:09:12
 *
 * @author Knoblul
 */
public class PlatformHelper {
	private static final Platform PLATFORM;
	private static final boolean IS_64BIT;
	private static final String PLATFORM_IDENTIFIER;
	private static final int MAX_MEMORY;

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
			throw new RuntimeException("Unknown platform: " + osName);
		}

		String arch = System.getProperty("os.arch");
		switch (PLATFORM) {
			case LINUX:
				PLATFORM_IDENTIFIER = arch.startsWith("arm") || arch.startsWith("aarch64")
						? "linux-" + (arch.contains("64") || arch.contains("armv8") ? "arm64" : "arm32")
						: "linux";
				break;
			case MAC:
				PLATFORM_IDENTIFIER = "macos";
				break;
			case WINDOWS:
				PLATFORM_IDENTIFIER = System.getenv("ProgramFiles(x86)") != null ? "win64" : "win32";
				break;
			default:
				throw new IllegalArgumentException("Unknown platform: " + PLATFORM);
		}

		IS_64BIT = PLATFORM_IDENTIFIER.contains("64");

		int maxMemory = 0;
		try {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			Object attribute = mBeanServer.getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"),
					"TotalPhysicalMemorySize");
			if (!(attribute instanceof Long)) {
				throw new IllegalArgumentException("No such attribute: TotalPhysicalMemorySize");
			}

			maxMemory = LauncherConfig.roundUpToPowerOfTwo((int) ((Long) attribute / 1024 / 1024));
		} catch (Throwable t) {
			Log.err(t, "Failed to determine max physical memory size");
		}

		MAX_MEMORY = maxMemory;
	}

	public static String getPlatformIdentifier() {
		return PLATFORM_IDENTIFIER;
	}

	public static Platform getPlatform() {
		return PLATFORM;
	}

	public static Path getDefaultGameDirectory() {
		String path = ".project-cataclysm/";
		switch (getPlatform()) {
			case WINDOWS:
				return Paths.get(System.getenv("APPDATA"), path);
			case MAC:
				return Paths.get(System.getProperty("user.home", ""), "Library/Application Support/" + path);
			default:
				return Paths.get(System.getProperty("user.home", ""), path);
		}
	}

	public static boolean is64bit() {
		return IS_64BIT;
	}

	public static int getMaxMemory() {
		// Ограничиваем в 32 гб
		return Math.min(32 * 1024, MAX_MEMORY != 0 ? MAX_MEMORY : (IS_64BIT ? 16 * 1024 : 2048));
//		return 4 * 1024;
	}

	public enum Platform {
		WINDOWS,
		MAC,
		LINUX
	}
}
