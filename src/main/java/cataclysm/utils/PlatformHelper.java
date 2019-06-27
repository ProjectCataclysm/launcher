package cataclysm.utils;

import java.io.File;

/**
 * Created 16 окт. 2018 г. / 22:09:12 
 * @author Knoblul
 */
public class PlatformHelper {
	public enum PlatformOS {
		WINDOWS,
		MAC,
		SOLARIS,
		LINUX,
		UNKNOWN
	}
	
	public static PlatformOS getOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		
		if (osName.contains("win"))
			return PlatformOS.WINDOWS;
		
		if (osName.contains("mac"))
			return PlatformOS.MAC;
		
		if (osName.contains("solaris"))
			return PlatformOS.SOLARIS;
		
		if (osName.contains("sunos"))
			return PlatformOS.SOLARIS;
		
		if (osName.contains("linux"))
			return PlatformOS.LINUX;
		
		if (osName.contains("unix"))
			return PlatformOS.LINUX;
		
		return PlatformOS.UNKNOWN;
	}
	
	public static File getDefaultGameDirectory() {
		String path = ".project-cataclysm/";
		switch (PlatformHelper.getOS()) {
		case WINDOWS:
			return new File(System.getenv("APPDATA"), path);
		case MAC:
			return new File(System.getProperty("user.home", ""), "Library/Application Support/" + path);
		default:
			return new File(System.getProperty("user.home", ""), path);
		}
	}
	
	public static long getAvaibleMemory() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		return maxMemory;// / 1024 / 1024;
	}
}
