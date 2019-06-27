package cataclysm.launch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import com.google.common.collect.Lists;

import cataclysm.io.sanitization.Resource;
import cataclysm.io.sanitization.ResourceRetriever;
import cataclysm.io.sanitization.SanitizationManager;
import cataclysm.ui.ConfigFrame;
import cataclysm.ui.DialogUtils;
import cataclysm.ui.LaunchStatusFrame;
import cataclysm.ui.LauncherFrame;
import cataclysm.utils.LauncherLock;
import cataclysm.utils.Log;
import cataclysm.utils.VersionHelper;

/**
 * Created 27 сент. 2018 г. / 21:10:13 
 * @author Knoblul
 */
public class Launcher implements Runnable {
	public static File workDir;
	
	static {
		workDir = new File(FileSystemView.getFileSystemView().getDefaultDirectory(), "cataclysmLauncher");
		if (!workDir.exists()) {
			workDir.mkdirs();
		}
	}
	
	public static ConfigFrame config;
	public static LauncherFrame frame;
	public static LaunchStatusFrame statusFrame;

	private static VersionHelper versionHelper;
	private static ResourceRetriever resourceRetriever;
	private static SanitizationManager sanitizer;
	private static DownloadingManager downloader;

	public static Launcher instance;
	
	private static volatile boolean launched;

	public Launcher(String[] args) {
//		ConsoleDisconnector.disconnect();
		
		if (!LauncherLock.isAvaible()) {
			JOptionPane.showMessageDialog(null, "Лаунчер уже запущен", "Ошибка", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(LauncherLock.unlock()));
		
		instance = this;
		config = new ConfigFrame();
		frame = new LauncherFrame();
		statusFrame = new LaunchStatusFrame();
		resourceRetriever = new ResourceRetriever();
		sanitizer = new SanitizationManager();
		downloader = new DownloadingManager();
		versionHelper = new VersionHelper();
		
		frame.showVersionChecker(versionHelper);
		if (versionHelper.shouldStartLauncher(args)) {
			frame.showLauncher();
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

			frame.setVisible(false);
			statusFrame.showFrame();

			List<Resource> resources = resourceRetriever.retrieveResources();
			Log.msg("Sanitizing...");
			statusFrame.fill(sanitizer);
			sanitizer.perform(resources);

			if (!resources.isEmpty()) {
				Log.msg("Downloading %d files/archives...", resources.size());
				statusFrame.fill(downloader);
				downloader.perform(resources);
			}

			Log.msg("STARTING GAYM!!1!");
			statusFrame.setVisible(false);
			config.setVisible(false);
			
			int exitCode = 0;
			try {
				exitCode = startGame();
				Log.msg("---- GAME STOPPED WITH CODE %d ----", exitCode);
			} catch (InterruptedException e) { }
			
			if (exitCode != 0) {
				statusFrame.setVisible(false);
				frame.setVisible(true);
				launched = false;
				
				JOptionPane.showMessageDialog(frame,
						"Игра была аварийно завершена\nTODO: добавить отправку отчёта об ошибке на сервера SME",
						"Ошибка", JOptionPane.ERROR_MESSAGE);
			} else {
				System.exit(0);
			}
		} catch (Throwable t) {
			statusFrame.setVisible(false);
			DialogUtils.showError("Невозможно запустить игру", t);
			frame.setVisible(true);
			launched = false;
		}
	}

	private int startGame() throws IOException, InterruptedException {
		List<String> command = Lists.newArrayList();
		command.add("java");
		command.add("-Xms256m");
		command.add("-Xmx" + config.memory + "m");
		command.add("-Djava.library.path=");
		//command.add("-Dfile.encoding=UTF-8");
		command.add("-cp");
		command.add("sme_client.jar");
		command.add("start.StartMinecraft");
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(config.gameDirectory);
		return pb.inheritIO().start().waitFor();
	}
}
