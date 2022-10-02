package cataclysm.launcher.launch;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.account.Session;
import cataclysm.launcher.ui.DialogUtils;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;
import cataclysm.launcher.utils.PlatformHelper;
import com.google.common.collect.Lists;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
		/*
		TODO добавить в лаунчер функцию формирования архива после краша
			crash-report-<date>.zip
			- exit.txt (там код ошибки и pid)
			- launcher.log
			- fatal.log
			- heapdump.hprof
			- crashx.log
			- log0.log
		 */

		// Может быть проблема при закрытии игры через диспетчер задач, код завершения 1
		List<String> command = Lists.newArrayList();

		boolean customJava = true;
		if (PlatformHelper.getPlatform() == PlatformHelper.Platform.WINDOWS) {
			Path javaPath = config.gameDirectoryPath.resolve(Paths.get("jre8", "bin", "java.exe"));
			if (Files.isExecutable(javaPath)) {
				command.add(javaPath.toAbsolutePath().toString());
				customJava = false;
			}
		}

		// пример аргументов IntelliJ при запуске:
		//Command Line: exit -Xms128m -Xmx750m -XX:ReservedCodeCacheSize=512m -XX:+IgnoreUnrecognizedVMOptions -XX:+UseG1GC -XX:SoftRefLRUPolicyMSPerMB=50 -XX:CICompilerCount=2 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -ea -Dsun.io.useCanonCaches=false -Djdk.http.auth.tunneling.disabledSchemes="" -Djdk.attach.allowAttachSelf=true -Djdk.module.illegalAccess.silent=true -Dkotlinx.coroutines.debug=off -Xmx8192m -Djb.vmOptionsFile=C:\Users\Mihail\AppData\Roaming\JetBrains\IdeaIC2021.3\idea64.exe.vmoptions -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.vendor.name=JetBrains -Didea.paths.selector=IdeaIC2021.3 -Didea.platform.prefix=Idea -Didea.jre.check=true -Dsplash=true -Dide.native.launcher=true -XX:ErrorFile=C:\Users\Mihail\java_error_in_idea64_%p.log -XX:HeapDumpPath=C:\Users\Mihail\java_error_in_idea64.hprof

		// для репортов JVM
		Files.createDirectories(config.gameDirectoryPath.resolve("crashes"));

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

		// показывать стектрейсы из JVM
//		command.add("-XX:-OmitStackTraceInFastThrow");

		// создаем хипдамп при недостатке памяти
		command.add("-XX:+HeapDumpOnOutOfMemoryError");

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

		List<String> classPath = Lists.newArrayList();
		Path binPath = config.gameDirectoryPath.resolve("bin");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(binPath)) {
			for (Path path : ds) {
				Path relativePath = config.gameDirectoryPath.relativize(path);
				classPath.add(relativePath.toString());
			}
		}

		Path binNativesPath = binPath.resolve("natives");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(binNativesPath)) {
			for (Path path : ds) {
				Path relativePath = config.gameDirectoryPath.relativize(path);
				classPath.add(relativePath.toString());
			}
		}

		Collections.sort(classPath);
		classPath.add(0, "core.jar");

		command.add(String.join(File.pathSeparator, classPath));
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

		if (!config.forwardCompatRender) {
			command.add("--forwardCompatRenderDisabled");
		}

		// проверяем тикеты на наличие тикета билд-сервера
		if (session.getTickets().contains("e:build")) {
			command.add("--buildServerAccess");
		}

		// проверяем тикеты на наличие тикета тест-сервера
		if (session.getTickets().contains("e:test")) {
			command.add("--testServerAccess");
		}

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(config.gameDirectoryPath.toFile());

		pb.inheritIO();

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

		Log.msg("Starting game process...");
		Platform.runLater(() -> {
			application.getTrayManager().install();
			application.setStartGameAvailable(false);
		});

		launched.set(true);
		LauncherConfig config = application.getConfig();
		Thread t = new Thread(() -> {
			int exitCode = 0;
			try {
				exitCode = startGame(config, session);
			} catch (InterruptedException e) {
				Platform.runLater(() -> DialogUtils.showError("Ошибка: доступ к процессу игры был прерван", e, true));
			} catch (IOException e) {
				Platform.runLater(() -> DialogUtils.showError("Ошибка запуска процесса игры", e, true));
			} finally {
				launched.set(false);
			}

			int finalExitCode = exitCode;
			Platform.runLater(() -> {
				application.setStartGameAvailable(true);
				if (finalExitCode != 0) {
					if (finalExitCode == -1337) {
						DialogUtils.showError("Файлы игры устарели."
								+ " Необходимо перезапустить игру, чтобы скачать последнюю версию.", null, false);
					} else {
						DialogUtils.showGameError(finalExitCode);
					}
				}
			});
		});
		t.setDaemon(false);
		t.setName("Game Process Thread");
		t.start();
	}

	public boolean isLaunched() {
		return launched.get();
	}
}
