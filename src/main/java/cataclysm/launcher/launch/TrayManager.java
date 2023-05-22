package cataclysm.launcher.launch;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.Log;
import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 10.09.2022 23:30
 *
 * @author Knoblul
 */
public class TrayManager {
	private final SystemTray tray;
	private final TrayIcon trayIcon;

	public TrayManager() {
		if (!SystemTray.isSupported()) {
			Log.err("Tray icon is not supported");
			tray = null;
			trayIcon = null;
			return;
		}

		BufferedImage image;
		try {
			image = ImageIO.read(Objects.requireNonNull(TrayManager.class.getResource("/icons/icon_16.png")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		tray = SystemTray.getSystemTray();
		trayIcon = new TrayIcon(image, "Project Cataclysm Launcher");
		trayIcon.addActionListener(__ -> {
			Platform.runLater(() -> LauncherApplication.getInstance().getPrimaryStage().show());
			uninstall();
		});
	}

	public void install() {
		if (tray != null && trayIcon != null) {
			try {
				tray.add(trayIcon);
			} catch (Exception e) {
				Log.err(e, "Failed to create tray icon");
			}
		}
	}

	public void uninstall() {
		if (tray != null && trayIcon != null) {
			tray.remove(trayIcon);
		}
	}
}
