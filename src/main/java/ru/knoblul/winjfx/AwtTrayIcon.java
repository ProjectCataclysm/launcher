package ru.knoblul.winjfx;

import cataclysm.launcher.utils.logging.Log;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.TrayIcon;
import java.io.IOException;
import java.util.function.IntConsumer;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 14.10.2020 13:07
 *
 * @author Knoblul
 */
class AwtTrayIcon {
	private static java.awt.SystemTray systemTray;
	private static boolean initialized;

	private static void init() {
		if (!initialized) {
			try {
				java.awt.Toolkit.getDefaultToolkit();
				if (!java.awt.SystemTray.isSupported()) {
					throw new IOException("System tray not supported");
				}

				systemTray = java.awt.SystemTray.getSystemTray();
			} catch (Exception ignored) {

			}

			initialized = true;
		}
	}

	static Object createTrayIcon(Image iconImage, Runnable actionListener) {
		init();

		if (systemTray != null) {
			try {
				java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(SwingFXUtils.fromFXImage(iconImage, null));
				trayIcon.addActionListener(event -> actionListener.run());
				trayIcon.setPopupMenu(new PopupMenu());
				systemTray.add(trayIcon);
				return trayIcon;
			} catch (java.awt.AWTException e) {
				Log.warn(e, "Failed to add AWT tray icon");
			}
		}

		return null;
	}

	static void setTrayIconTooltip(Object trayIconInstance, String tooltip) {
		if (trayIconInstance instanceof java.awt.TrayIcon) {
			((java.awt.TrayIcon) trayIconInstance).setToolTip(tooltip);
		}
	}

	static void updateMenuItem(Object trayIconInstance, int itemIdentifier, String itemName, IntConsumer actionListener) {
		if (trayIconInstance instanceof java.awt.TrayIcon) {
			PopupMenu popupMenu = ((java.awt.TrayIcon) trayIconInstance).getPopupMenu();
			if (itemName == null) {
				popupMenu.remove(itemIdentifier);
			} else {
				MenuItem item = new MenuItem(itemName);
				item.addActionListener(event -> actionListener.accept(itemIdentifier));
				popupMenu.add(item);
			}
		}
	}

	static void removeTrayIcon(Object trayIconInstance) {
		if (systemTray != null && trayIconInstance instanceof java.awt.TrayIcon) {
			systemTray.remove((java.awt.TrayIcon) trayIconInstance);
		}
	}

	public static void showMessage(Object trayIconInstance, String title, String message, int ordinal) {
		if (systemTray != null && trayIconInstance instanceof java.awt.TrayIcon) {
			java.awt.TrayIcon trayIcon = (java.awt.TrayIcon) trayIconInstance;

			java.awt.TrayIcon.MessageType messageType = java.awt.TrayIcon.MessageType.NONE;
			switch (ordinal) {
				case 0:
					messageType = java.awt.TrayIcon.MessageType.ERROR;
					break;
				case 2:
					messageType = java.awt.TrayIcon.MessageType.WARNING;
					break;
				case 3:
					messageType = java.awt.TrayIcon.MessageType.INFO;
					break;
			}

			trayIcon.displayMessage(title, message, messageType);
		}
	}
}
