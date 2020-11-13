package ru.knoblul.winjfx;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.lang.reflect.Method;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 12.10.2020 10:10
 *
 * @author Knoblul
 */
public class WindowHandleAccessor {
	private final Stage stage;
	private final ReadOnlyLongWrapper handle = new ReadOnlyLongWrapper();

	private WindowHandleAccessor(Stage stage) {
		this.stage = stage;
		stage.onShownProperty().bind(Bindings.createObjectBinding(() -> this::onShown));
	}

	public static WindowHandleAccessor create(Stage stage) {
		return new WindowHandleAccessor(stage);
	}

	private void onShown(WindowEvent event) {
		try {
			@SuppressWarnings("deprecation")
			com.sun.javafx.tk.TKStage tkStage = stage.impl_getPeer();
			Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
			getPlatformWindow.setAccessible(true);
			Object platformWindow = getPlatformWindow.invoke(tkStage);
			Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
			getNativeHandle.setAccessible(true);
			handle.set((long) getNativeHandle.invoke(platformWindow));
		} catch (Exception e) {
			throw new RuntimeException("Failed to get stage native window handle", e);
		}
	}

	public ReadOnlyLongProperty handleProperty() {
		return handle;
	}

	public long getHandle() {
		return handle.get();
	}
}
