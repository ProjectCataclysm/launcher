package ru.knoblul.winjfx;

import cataclysm.launcher.utils.logging.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 11.10.2020 13:43
 *
 * @author Knoblul
 */
public class WinJFX {
	public static final String LIBRARY_NAME = "WinJFX";
	private static boolean librarySupported;

	static {
		// check library mapping - should be exactly windows-like, or else it will not load
		String mappedLibraryName = System.mapLibraryName(LIBRARY_NAME);
		if (librarySupported = mappedLibraryName.equals(LIBRARY_NAME + ".dll")) {
			Log.msg("Enabling WinJFX...");

			try {
				System.loadLibrary(LIBRARY_NAME);
			} catch (UnsatisfiedLinkError ignored) {
				try {
					Path libFilePath = Paths.get(System.getProperty("java.io.tmpdir"),"ProjectCataclysmLauncher", mappedLibraryName);
					if (!Files.exists(libFilePath.getParent())) {
						Files.createDirectories(libFilePath);
					}
					Files.copy(WinJFX.class.getResourceAsStream("/" + mappedLibraryName), libFilePath, StandardCopyOption.REPLACE_EXISTING);
					System.load(libFilePath.toString());
				} catch (Exception e) {
					Log.warn(e, "Failed to load WinJFX");
					librarySupported = false;
				}
			}
		}
	}

	static native long getWindowStyleFlags(long windowHandle);

	static native void setWindowStyleFlags(long windowHandle, long windowStyleFlags);

//	static native void bringWindowToTop(long windowHandle);

	static native long createTrayIcon(Object trayIconInstance, byte[] iconBytes);

	static native void setTrayIconTooltip(long trayIconHandle, String tooltip);

	static native void updateTrayIconPopupItem(long trayIconHandle, int itemIdentifier, int itemTypeFlags, String itemName);

	@SuppressWarnings("unused")
	public static void onTrayIconClicked(Object trayIconInstance) {
		if (!(trayIconInstance instanceof TrayIcon)) {
			throw new IllegalStateException("Illegal tray icon instance");
		}

		((TrayIcon) trayIconInstance).clicked();
	}

	@SuppressWarnings("unused")
	public static void onTrayIconPopupSelected(Object trayIconInstance, int clickedIndex) {
		if (!(trayIconInstance instanceof TrayIcon)) {
			throw new IllegalStateException("Illegal tray icon instance");
		}

		((TrayIcon) trayIconInstance).clicked(clickedIndex);
	}

	static native void trayIconShowMessage(long trayIconHandle, String title, String message, int type);

	static native void destroyTrayIcon(long trayIconHandle);

	static native boolean isTaskbarProgressSupported();

	static native long createTaskbar();

	static native void setTaskbarProgressState(long windowHandle, long taskbarHandle, int state);

	static native void setTaskbarProgress(long windowHandle, long taskbarHandle, long completed, long total);

	static native void destroyTaskbar(long taskbarHandle);

	public static boolean isLibrarySupported() {
		return librarySupported;
	}
}