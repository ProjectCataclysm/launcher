package cataclysm.launch;

import cataclysm.io.sanitation.Resource;
import cataclysm.io.sanitation.ResourceMaster;
import cataclysm.io.sanitation.SanitationManager;
import cataclysm.ui.*;
import cataclysm.utils.*;
import cataclysm.utils.PlatformHelper.PlatformOS;
import com.google.common.collect.Lists;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created 27 сент. 2018 г. / 21:10:13 
 * @author Knoblul
 */
public class Launcher implements Runnable {
	public static Path workDirPath;
	
	static {
		workDirPath = Paths.get(System.getProperty("user.home"), "ProjectCataclysm");
		if (!Files.exists(workDirPath)) {
			try {
				Files.createDirectories(workDirPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static ConfigFrame config;
	public static LauncherFrame frame;
	public static LoginFrame loginFrame;
	public static LaunchStatusFrame statusFrame;

	private static ResourceMaster resourceMaster;
	private static SanitationManager sanitizer;
	private static DownloadingManager downloader;

	public static Launcher instance;
	
	private static volatile boolean launched;

	public Launcher(String[] args) {
//		ConsoleDisconnector.disconnect();
		PlatformHelper.setLookAndFeel();
		if (!LauncherLock.isAvailable()) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(null, "Лаунчер уже запущен", "Ошибка", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(LauncherLock::unlock));

		instance = this;
		frame = new LauncherFrame();

		config = new ConfigFrame();
		loginFrame = new LoginFrame();
		statusFrame = new LaunchStatusFrame();
		resourceMaster = new ResourceMaster();
		sanitizer = new SanitationManager();
		downloader = new DownloadingManager();
		VersionHelper versionHelper = new VersionHelper();
		
		if (versionHelper.shouldStartLauncher(args)) {
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
				exitCode = startGame();
				Log.msg("---- GAME STOPPED WITH CODE %d ----", exitCode);
			} catch (InterruptedException ignored) { }
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

	private int startGame() throws IOException, InterruptedException {
		List<String> command = Lists.newArrayList();

		boolean customJava = true;
		if (PlatformHelper.getOS() == PlatformOS.WINDOWS) {
			Path javaPath = config.gameDirectoryPath.resolve(Paths.get("jre8", "bin", "java.exe"));
			if (Files.exists(javaPath)) {
				command.add(javaPath.toAbsolutePath().toString());
				customJava = false;
			}
		}

		if (customJava) {
			command.add("java");
		}

		command.add("-Dfile.encoding=UTF-8");

		// логгинг вылетов
		command.add("-XX:ErrorFile=crashes/fatal_pid_%p.log");
		command.add("-XX:HeapDumpPath=crashes/heapdump_pid_%p.hprof");
		
		// оптимизация
		command.add("-XX:+UnlockExperimentalVMOptions");
		command.add("-XX:+UseG1GC");
		command.add("-XX:G1NewSizePercent=20");
		command.add("-XX:G1ReservePercent=20");
		command.add("-XX:MaxGCPauseMillis=50");
		command.add("-XX:G1HeapRegionSize=32M");

		// макс. размер стека вызова
		command.add("-Xss1M");

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
		command.add("start.StartClient");
		
		// данные для входа
		LoginHolder loginHolder = loginFrame.getLoginHolder();
		command.add("--uuid");
		command.add(loginHolder.getUUID());
		command.add("--sessionId");
		command.add(loginHolder.getSessionId());
		command.add("--username");
		command.add(loginHolder.getUsername());

		if (loginHolder.isBuildServerAccess()) {
			command.add("--buildServerAccess");
		}

		command.add("--texturesQuality");
		command.add(Integer.toString(config.texturesQuality));

		ProcessBuilder pb = new ProcessBuilder(command);

		Path logsPath = config.gameDirectoryPath.resolve("logs");
		if (!Files.exists(logsPath)) {
			Files.createDirectories(logsPath);
		}
		
		Path errorFilePath = logsPath.resolve("launcher-stderr.log");
		Path outFilePath = logsPath.resolve("launcher-stdout.log");
		
		pb.redirectError(errorFilePath.toFile());
		pb.redirectOutput(outFilePath.toFile());
		
		pb.directory(config.gameDirectoryPath.toFile());
		Process process = pb.start();
		return process.waitFor();

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
