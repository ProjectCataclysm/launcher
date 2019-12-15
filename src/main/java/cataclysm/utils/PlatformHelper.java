package cataclysm.utils;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Created 16 ���. 2018 �. / 22:09:12 
 * @author Knoblul
 */
public class PlatformHelper {
	public enum PlatformOS {
		WINDOWS,
		MAC,
		LINUX
	}

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
			osArchIdentifier = arch.contains("64") ? "win64" : "win32";
			break;
		}
	}
	
	public static String getOsArchIdentifier() {
		return osArchIdentifier;
	}
	
	public static PlatformOS getOS() {
		return PLATFORM;
	}
	
	public static File getDefaultGameDirectory() {
		String path = ".project-cataclysm/";
		switch (getOS()) {
		case WINDOWS:
			return new File(System.getenv("APPDATA"), path);
		case MAC:
			return new File(System.getProperty("user.home", ""), "Library/Application Support/" + path);
		default:
			return new File(System.getProperty("user.home", ""), path);
		}
	}
	
	public static int getAvaibleMemory() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		return (int) (maxMemory / 1024 / 1024);
	}
	
	public static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}
}
