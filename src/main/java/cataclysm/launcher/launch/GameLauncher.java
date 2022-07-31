package cataclysm.launcher.launch;

import cataclysm.launcher.account.Session;
import cataclysm.launcher.ui.ConfigFrame;
import cataclysm.launcher.utils.PlatformHelper;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 31.07.2022 19:06
 *
 * @author Knoblul
 */
public class GameLauncher {
	private final ConfigFrame config;

	public GameLauncher(ConfigFrame config) {
		this.config = config;
	}

	public int startGame(Session session) throws IOException, InterruptedException {
		List<String> command = Lists.newArrayList();

		boolean customJava = true;
		if (PlatformHelper.getOS() == PlatformHelper.PlatformOS.WINDOWS) {
			Path javaPath = config.gameDirectoryPath.resolve(Paths.get("jre8", "bin", "java.exe"));
			if (Files.isExecutable(javaPath)) {
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

		if (PlatformHelper.getOS() == PlatformHelper.PlatformOS.MAC) {
			command.add("-XstartOnFirstThread");
		}

		// вырубаем Ipv6 сокеты по дефолту для ускорения работы нетворкинга
		command.add("-Djava.net.preferIPv4Stack=true");

		// либрарипатх
		command.add("-Djava.library.path=");

		// класспатх
		command.add("-cp");
		command.add("core.jar");
		command.add("start.StartClient");

		// данные для входа
		command.add("--uuid");
		command.add(session.getProfile().getUuid());
		command.add("--username");
		command.add(session.getProfile().getUsername());
		command.add("--accessToken");
		command.add(session.getAccessToken());

		// проверяем тикеты на наличие тикета билд-сервера
		if (session.getTickets().contains("e:build")) {
			command.add("--buildServerAccess");
		}

		command.add("--texturesQuality");
		command.add(Integer.toString(config.texturesQuality));

		ProcessBuilder pb = new ProcessBuilder(command);

		Path logsPath = Files.createDirectories(config.gameDirectoryPath.resolve("logs"));
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
