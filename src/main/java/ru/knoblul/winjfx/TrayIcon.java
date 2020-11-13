package ru.knoblul.winjfx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.List;
import java.util.Optional;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 14.10.2020 8:09
 *
 * @author Knoblul
 */
public final class TrayIcon {
	private static final int NATIVE_MENU_ID_OFFSET = 1;
	private static final int MF_STRING = 0;

	private long handle;
	private Object awtTrayIconInstance;

	private final ContextMenu contextMenu;
	private final StringProperty tooltip;
	private final ObjectProperty<EventHandler<ActionEvent>> onAction;

	public TrayIcon(Image image) {
		if (WinJFX.isLibrarySupported()) {
			if (image.getWidth() != 16 || image.getHeight() != 16) {
				throw new IllegalArgumentException("Image width and height must be 16x16 pixels");
			}

			byte[] imagePixelBytes = new byte[16 * 16 * 4];

			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int index = (x + y * 16) * 4;
					int pixel = image.getPixelReader().getArgb(x, y);
					imagePixelBytes[index] = (byte) (pixel);
					imagePixelBytes[index + 1] = (byte) (pixel >> 8);
					imagePixelBytes[index + 2] = (byte) (pixel >> 16);
					imagePixelBytes[index + 3] = (byte) (pixel >> 24);
				}
			}

			handle = WinJFX.createTrayIcon(this, imagePixelBytes);
		} else {
			awtTrayIconInstance = AwtTrayIcon.createTrayIcon(image, this::clicked);
		}

		contextMenu = new ContextMenu();
		contextMenu.getItems().addListener((ListChangeListener<MenuItem>) change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					for (int i = 0; i < change.getRemoved().size(); i++) {
						int index = change.getFrom() + i + NATIVE_MENU_ID_OFFSET;
						if (WinJFX.isLibrarySupported()) {
							if (handle != 0) {
								WinJFX.updateTrayIconPopupItem(handle, index, MF_STRING, null);
							}
						} else if (awtTrayIconInstance != null) {
							AwtTrayIcon.updateMenuItem(awtTrayIconInstance, index, null, this::clicked);
						}
					}
				} else {
					List<? extends MenuItem> addedSubList = change.getAddedSubList();
					for (int i = 0; i < addedSubList.size(); i++) {
						MenuItem item = addedSubList.get(i);
						String itemName = item.getText() != null ? item.getText() : "";
						int index = change.getFrom() + i + NATIVE_MENU_ID_OFFSET;
						if (WinJFX.isLibrarySupported()) {
							if (handle != 0) {
								WinJFX.updateTrayIconPopupItem(handle, index, MF_STRING, itemName);
							}
						} else if (awtTrayIconInstance != null) {
							AwtTrayIcon.updateMenuItem(awtTrayIconInstance, index, itemName, this::clicked);
						}
					}
				}
			}
		});

		tooltip = new SimpleStringProperty();
		tooltip.addListener((observable, oldValue, newValue) -> {
			if (WinJFX.isLibrarySupported()) {
				if (handle != 0) {
					WinJFX.setTrayIconTooltip(handle, newValue);
				}
			} else if (awtTrayIconInstance != null) {
				AwtTrayIcon.setTrayIconTooltip(awtTrayIconInstance, newValue);
			}
		});

		onAction = new SimpleObjectProperty<>();
	}

	public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
		return onAction;
	}

	public EventHandler<ActionEvent> getOnAction() {
		return onAction.get();
	}

	public void setOnAction(EventHandler<ActionEvent> eventHandler) {
		onAction.set(eventHandler);
	}

	void clicked() {
		Optional.of(getOnAction()).ifPresent(onAction -> onAction.handle(new ActionEvent(null, null)));
	}

	void clicked(int itemIdentifier) {
		int index = itemIdentifier - NATIVE_MENU_ID_OFFSET;
		List<MenuItem> items = contextMenu.getItems();
		if (index >= 0 && index < items.size()) {
			MenuItem item = items.get(index);
			item.fire();
		}
	}

	public ContextMenu getContextMenu() {
		return contextMenu;
	}

	public StringProperty tooltipProperty() {
		return tooltip;
	}

	public String getTooltip() {
		return tooltip.get();
	}

	public void setTooltip(String tooltip) {
		if (tooltip != null && tooltip.length() >= 128) {
			throw new IllegalArgumentException("Tooltip length must be less than 127 characters");
		}
		this.tooltip.set(tooltip);
	}

	public void showMessage(String title, String message, MessageType messageType) {
		if (WinJFX.isLibrarySupported()) {
			if (handle != 0) {
				WinJFX.trayIconShowMessage(handle, title, message, messageType.ordinal());
			}
		} else if (awtTrayIconInstance != null) {
			AwtTrayIcon.showMessage(awtTrayIconInstance, title, message, messageType.ordinal());
		}
	}

	public void destroy() {
		if (handle != 0) {
			WinJFX.destroyTrayIcon(handle);
			handle = 0;
		}

		if (awtTrayIconInstance != null) {
			AwtTrayIcon.removeTrayIcon(awtTrayIconInstance);
			awtTrayIconInstance = null;
		}
	}

	public enum MessageType {
		ERROR,
		WARNING,
		INFO,
		USER,
		SIMPLE
	}
}
