package cataclysm.launch;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import com.google.common.collect.Lists;

import cataclysm.io.sanitization.Resource;
import cataclysm.io.sanitization.ResourceMaster;
import cataclysm.io.sanitization.SanitizationManager;
import cataclysm.ui.ConfigFrame;
import cataclysm.ui.DialogUtils;
import cataclysm.ui.LaunchStatusFrame;
import cataclysm.ui.LauncherFrame;
import cataclysm.ui.LoginFrame;
import cataclysm.utils.LauncherLock;
import cataclysm.utils.Log;
import cataclysm.utils.LoginHolder;
import cataclysm.utils.PlatformHelper;
import cataclysm.utils.PlatformHelper.PlatformOS;
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
	public static LoginFrame loginFrame;
	public static LaunchStatusFrame statusFrame;

	private static VersionHelper versionHelper;
	private static ResourceMaster resourceMaster;
	private static SanitizationManager sanitizer;
	private static DownloadingManager downloader;

	public static Launcher instance;
	
	private static volatile boolean launched;

	public Launcher(String[] args) {
//		ConsoleDisconnector.disconnect();
		PlatformHelper.setLookAndFeel();
		if (!LauncherLock.isAvaible()) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(null, "Лаунчер уже запущен", "Ошибка", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(LauncherLock::unlock));
		
		instance = this;
		frame = new LauncherFrame();
		frame.showLauncher();

		config = new ConfigFrame();
		loginFrame = new LoginFrame();
		statusFrame = new LaunchStatusFrame();
		resourceMaster = new ResourceMaster();
		sanitizer = new SanitizationManager();
		downloader = new DownloadingManager();
		versionHelper = new VersionHelper();
		
		if (versionHelper.shouldStartLauncher(args)) {
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

			frame.setVisible(false);
			statusFrame.showFrame();

			Log.msg("Retrieving download info...");
			resourceMaster.retrieveDownloadInformation();
			
			Log.msg("Retrieving hashes...");
			resourceMaster.retrieveHashes();
			
			Log.msg("Sanitizing...");
			statusFrame.fill(sanitizer);
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
				exitCode = startGame();
				Log.msg("---- GAME STOPPED WITH CODE %d ----", exitCode);
			} catch (InterruptedException e) { }
			
			if (exitCode != 0) {
				statusFrame.setVisible(false);
				frame.setVisible(true);
				launched = false;
				
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

	private int startGame() throws IOException, InterruptedException {
		List<String> command = Lists.newArrayList();
		command.add("java");

		// логгинг вылетов
		command.add("-XX:ErrorFile=crash-reports/fatal_pid_%p.log");
		
		// оптимизация
		command.add("-XX:+UseG1GC");
		command.add("-XX:+UseStringDeduplication");
		command.add("-Dfile.encoding=UTF-8");
		
		// антиинжект
		command.add("-XX:+DisableAttachMechanism");

		// память
		if (config.limitMemory) {
			command.add("-Xms256m");
			command.add("-Xmx" + (config.memory) + "m");
		}
		
		if (PlatformHelper.getOS() == PlatformOS.MAC) {
			command.add("-XstartOnFirstThread");
		}
		
		// либрарипатх
		command.add("-Djava.library.path=");
		
		// класспатх
		command.add("-cp");
		command.add("core.jar");
		command.add("start.StartMinecraft");
		
		// данные для входа
		LoginHolder loginHolder = loginFrame.getLoginHolder();
		command.add("--uuid");
		command.add(loginHolder.getUUID());
		command.add("--sessionId");
		command.add(loginHolder.getSessionId());
		command.add("--username");
		command.add(loginHolder.getUsername());
		
		ProcessBuilder pb = new ProcessBuilder(command);
		
		File logsDir = new File(config.gameDirectory, "logs");
		logsDir.mkdirs();
		
		File errorFile = new File(logsDir, "launcher_output1.log");
		File outFile = new File(logsDir, "launcher_output2.log");
		
		pb.redirectError(errorFile);
		pb.redirectOutput(outFile);
		
		pb.directory(config.gameDirectory);
		Process process = pb.start();
		int exitCode = process.waitFor();
		if (exitCode != 1) {
			errorFile.delete();
			outFile.delete();
		}
		
		return exitCode;
		
		
//		// https://stackoverflow.com/questions/5483830/process-waitfor-never-returns
//		BufferedReader tr = new BufferedReader(new InputStreamReader(processInputStream));
//		while ((tr.readLine()) != null) { }
//		
//		tr = new BufferedReader(new InputStreamReader(processErrorStream));
//		while ((tr.readLine()) != null) { }
//		//---------------------------
//
//		int exitCode = process.waitFor();
//		if (exitCode == 1) {
//			try (PrintWriter pw = new PrintWriter(new File(logsDir, "launch_error.txt"))) {
//				try (BufferedReader reader = new BufferedReader(
//						new InputStreamReader(processErrorStream, Charsets.UTF_8))) {
//					String ln;
//					while ((ln = reader.readLine()) != null) {
//						pw.println(ln);
//					}
//				}
//				
//				try (BufferedReader reader = new BufferedReader(
//						new InputStreamReader(processInputStream, Charsets.UTF_8))) {
//					String ln;
//					while ((ln = reader.readLine()) != null) {
//						pw.println(ln);
//					}
//				}
//			}
//		} else {
//			try (BufferedReader reader = new BufferedReader(new InputStreamReader(processInputStream))) {
//				while ((tr.readLine()) != null) { }
//			}
//			
//			try (BufferedReader reader = new BufferedReader(new InputStreamReader(processErrorStream))) {
//				while ((tr.readLine()) != null) { }
//			}
//		}
//		
//		return exitCode;
	}
}
