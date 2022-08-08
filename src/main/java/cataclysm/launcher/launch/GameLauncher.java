package cataclysm.launcher.launch;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.account.Session;
import cataclysm.launcher.ui.DialogUtils;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;
import cataclysm.launcher.utils.PlatformHelper;
import com.google.common.collect.Lists;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 31.07.2022 19:06
 *
 * @author Knoblul
 */
public class GameLauncher {
	private final LauncherApplication application;
	private final AtomicBoolean launched = new AtomicBoolean();

	public GameLauncher(LauncherApplication application) {
		this.application = application;
	}

	private int startGame(LauncherConfig config, Session session) throws IOException, InterruptedException {
		List<String> command = Lists.newArrayList();

		boolean customJava = true;
		if (PlatformHelper.getPlatform() == PlatformHelper.Platform.WINDOWS) {
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
		if (config.limitMemoryMegabytes > 0) {
			command.add("-Xms256m");
			command.add("-Xmx" + (config.limitMemoryMegabytes) + "m");
		}

		if (PlatformHelper.getPlatform() == PlatformHelper.Platform.MAC) {
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

		if (config.fullscreen) {
			command.add("--fullscreen");
		}

		// проверяем тикеты на наличие тикета билд-сервера
		if (session.getTickets().contains("e:build")) {
			command.add("--buildServerAccess");
		}

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(config.gameDirectoryPath.toFile());
		Process process = pb.start();
		return process.waitFor();
	}

	public void startGame() {
		if (launched.get()) {
			throw new IllegalStateException("Игра уже запущена!");
		}

		Session session = application.getAccountManager().getSession();
		if (session == null) {
			throw new IllegalStateException("Вход в аккаунт не был выполнен!");
		}

		launched.set(true);
		LauncherConfig config = application.getConfig();
		Thread t = new Thread(() -> {
			Platform.runLater(() -> application.getPrimaryStage().hide());
			Log.msg("Starting game process...");

			int exitCode = 0;
			try {
				exitCode = startGame(config, session);
			} catch (InterruptedException e) {
				Platform.runLater(() -> DialogUtils.showError("Ошибка: доступ к процессу игры был прерван", e, true));
			} catch (IOException e) {
				Platform.runLater(() -> DialogUtils.showError("Ошибка запуска процесса игры", e, true));
			} finally {
				Platform.runLater(() -> application.getPrimaryStage().show());
				launched.set(false);
			}

			application.getOverlays().hideAll();

			if (exitCode != 0) {
				String message = String.format("Игра была аварийно завершена с кодом %d (0x%X)", exitCode, exitCode);
				Platform.runLater(() -> DialogUtils.showError(message, null, true));
			}
		});
		t.setDaemon(false);
		t.setName("Game Process Thread");
		t.start();
	}
}
