package ru.knoblul.winjfx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 12.10.2020 10:16
 *
 * @author Knoblul
 */
public class MinimizeAnimationListener implements ChangeListener<Boolean> {
//	private static final int AW_VER_POSITIVE = 0x00000004;
//	private static final int AW_VER_NEGATIVE = 0x00000008;
//	private static final int AW_SLIDE = 0x00040000;
//	private static final int AW_HIDE = 0x00010000;
//
//	private static final long WS_ICONIC = 0x20000000;
	private static final long WS_MAXIMIZEBOX = 0x10000;
	private static final long WS_MINIMIZEBOX = 0x20000;
	private static final long WS_CAPTION = 0xC00000;

	private final WindowHandleAccessor windowHandleAccessor;
	private long windowStyleFlags;

	public MinimizeAnimationListener(Stage stage, WindowHandleAccessor windowHandleAccessor) {
		this.windowHandleAccessor = windowHandleAccessor;
		if (WinJFX.isLibrarySupported()) {
			windowHandleAccessor.handleProperty().addListener((observable, oldValue, newValue) -> {
				windowStyleFlags = WinJFX.getWindowStyleFlags(windowHandleAccessor.getHandle());
				changed(null, null, stage.isIconified());
			});
		}
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		final long minimizeFlags = (windowStyleFlags | WS_CAPTION | WS_MINIMIZEBOX) & ~WS_MAXIMIZEBOX;
		final long maximizeFlags = (windowStyleFlags | WS_MINIMIZEBOX) & ~WS_MAXIMIZEBOX;
		WinJFX.setWindowStyleFlags(windowHandleAccessor.getHandle(), newValue ? minimizeFlags : maximizeFlags);
	}
}
