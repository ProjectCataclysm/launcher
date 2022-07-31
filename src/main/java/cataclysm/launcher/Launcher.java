package cataclysm.launcher;

import cataclysm.launcher.download.DownloadingManager;
import cataclysm.launcher.download.santation.Resource;
import cataclysm.launcher.download.santation.ResourceMaster;
import cataclysm.launcher.download.santation.SanitationManager;
import cataclysm.launcher.launch.GameLauncher;
import cataclysm.launcher.ui.*;
import cataclysm.launcher.utils.LauncherLock;
import cataclysm.launcher.utils.Log;
import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.selfupdate.LauncherVersionManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created 27 сент. 2018 г. / 21:10:13
 *
 * @author Knoblul
 */
public class Launcher implements Runnable {
	public static final Path workDirPath;

	public static ConfigFrame config;
	public static LauncherFrame frame;
	public static LoginFrame loginFrame;
	public static LaunchStatusFrame statusFrame;

	private static ResourceMaster resourceMaster;
	private static SanitationManager sanitizer;
	private static DownloadingManager downloader;

	public static Launcher instance;

	private static volatile boolean launched;
	private GameLauncher gameLauncher;

	public Launcher(String[] args) {
//		ConsoleDisconnector.disconnect();
		PlatformHelper.setLookAndFeel();
		if (!LauncherLock.isAvailable()) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(null, "Лаунчер уже запущен", "Ошибка", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		instance = this;
		frame = new LauncherFrame();
		loginFrame = new LoginFrame();
		config = new ConfigFrame();
		statusFrame = new LaunchStatusFrame();
		resourceMaster = new ResourceMaster();
		sanitizer = new SanitationManager();
		downloader = new DownloadingManager();
		gameLauncher = new GameLauncher(config);

		LauncherVersionManager versionManager = new LauncherVersionManager();
		if (versionManager.shouldStartLauncher(args)) {
			frame.showLauncher();
			loginFrame.initialize();
		} else {
			System.exit(0);
		}
	}

	public static void launch() {
		new Thread(instance).start();
	}

	@Override
	public void run() {
		try {
			if (launched) {
				throw new RuntimeException("Игра уже запущена");
			}
			launched = true;

			loginFrame.initialize();
			if (loginFrame.isNotLoggedIn()) {
				return;
			}

			frame.setVisible(false);

			statusFrame.fill(sanitizer);
			statusFrame.showFrame();

			Log.msg("Retrieving download info...");
			resourceMaster.retrieveDownloadInformation();

			Log.msg("Retrieving hashes...");
			resourceMaster.retrieveHashes();

			Log.msg("Sanitizing...");
			List<Resource> toDownload = sanitizer.perform(resourceMaster);
			if (!toDownload.isEmpty()) {
				Log.msg("Downloading %d files/archives...", toDownload.size());
				statusFrame.fill(downloader);
				downloader.perform(toDownload);
			}
		} catch (Throwable t) {
			statusFrame.setVisible(false);
			frame.setVisible(true);
			launched = false;
			DialogUtils.showError("Ошибка загрузки файлов", t);
			return;
		}

		try {
			Log.msg("STARTING GAYM!!1!");
			statusFrame.setVisible(false);
			config.setVisible(false);

			int exitCode = 0;
			try {
				exitCode = gameLauncher.startGame(loginFrame.getSession());
				Log.msg("---- GAME STOPPED WITH CODE %d ----", exitCode);
			} catch (InterruptedException ignored) {
			}
			launched = false;

			if (exitCode != 0) {
				statusFrame.setVisible(false);
				frame.setVisible(true);

				JOptionPane.showMessageDialog(frame, "Игра была аварийно завершена с кодом " + exitCode, "Ошибка",
						JOptionPane.ERROR_MESSAGE);
			}

			frame.setVisible(true);
		} catch (Throwable t) {
			statusFrame.setVisible(false);
			DialogUtils.showError("Невозможно запустить игру", t);
			frame.setVisible(true);
			launched = false;
		}
	}

	static {
		try {
			workDirPath = Files.createDirectories(Paths.get(System.getProperty("user.home"), "ProjectCataclysm"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to create work directory", e);
		}
	}
}
