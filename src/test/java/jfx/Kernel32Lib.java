package jfx;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 29.01.2022 16:48
 *
 * @author Knoblul
 */
public interface Kernel32Lib extends Library {
	Kernel32Lib INSTANCE = Native.loadLibrary("kernel32", Kernel32Lib.class);

	class OSVERSIONINFO extends Structure {
		private static final List<String> FIELD_ORDER = Collections.unmodifiableList(Arrays.asList("dwOSVersionInfoSize", "dwMajorVersion", "dwMinorVersion", "dwBuildNumber", "dwPlatformId", "szCSDVersion"));

		public int dwOSVersionInfoSize;
		public int dwMajorVersion;
		public int dwMinorVersion;
		public int dwBuildNumber;
		public int dwPlatformId;
		public char[] szCSDVersion;

		public OSVERSIONINFO() {
			szCSDVersion = new char[128];
			dwOSVersionInfoSize = size();
		}

		@Override
		protected List<String> getFieldOrder() {
			return FIELD_ORDER;
		}

		public int getDwOSVersionInfoSize() {
			return dwOSVersionInfoSize;
		}

		public int getDwMajorVersion() {
			return dwMajorVersion;
		}

		public int getDwMinorVersion() {
			return dwMinorVersion;
		}

		public int getDwBuildNumber() {
			return dwBuildNumber;
		}

		public int getDwPlatformId() {
			return dwPlatformId;
		}

		public String getSzCSDVersion() {
			return Native.toString(szCSDVersion);
		}

		@Override
		public String toString() {
			return "OSVERSIONINFO{" +
					"dwOSVersionInfoSize=" + dwOSVersionInfoSize +
					", dwMajorVersion=" + dwMajorVersion +
					", dwMinorVersion=" + dwMinorVersion +
					", dwBuildNumber=" + dwBuildNumber +
					", dwPlatformId=" + dwPlatformId +
					", szCSDVersion='" + getSzCSDVersion() + '\'' +
					'}';
		}
	}

	int GetLastError();

	boolean GetVersionExW(OSVERSIONINFO struct);
}
