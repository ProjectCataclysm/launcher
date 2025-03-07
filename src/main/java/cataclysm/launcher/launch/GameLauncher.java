package cataclysm.launcher.launch;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.account.Session;
import cataclysm.launcher.ui.DialogUtils;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;
import cataclysm.launcher.utils.PlatformHelper;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <br><br>ProjectCataclysm
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
		Path gameDirPath = config.getCurrentGameDirectoryPath();

		// Может быть проблема при закрытии игры через диспетчер задач, код завершения 1
		List<String> command = new ArrayList<>();

		if (PlatformHelper.getPlatform() == PlatformHelper.Platform.WINDOWS) {
			Path javaPath = gameDirPath.resolve(Paths.get("bin", "game.exe"));
			if (!Files.isExecutable(javaPath)) {
				throw new RuntimeException("No executable found");
			}

			command.add(javaPath.toAbsolutePath().toString());
		}

		// пример аргументов IntelliJ при запуске:
		//Command Line: exit -Xms128m -Xmx750m -XX:ReservedCodeCacheSize=512m -XX:+IgnoreUnrecognizedVMOptions -XX:+UseG1GC -XX:SoftRefLRUPolicyMSPerMB=50 -XX:CICompilerCount=2 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -ea -Dsun.io.useCanonCaches=false -Djdk.http.auth.tunneling.disabledSchemes="" -Djdk.attach.allowAttachSelf=true -Djdk.module.illegalAccess.silent=true -Dkotlinx.coroutines.debug=off -Xmx8192m -Djb.vmOptionsFile=C:\Users\User\AppData\Roaming\JetBrains\IdeaIC2021.3\idea64.exe.vmoptions -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.vendor.name=JetBrains -Didea.paths.selector=IdeaIC2021.3 -Didea.platform.prefix=Idea -Didea.jre.check=true -Dsplash=true -Dide.native.launcher=true -XX:ErrorFile=C:\Users\User\java_error_in_idea64_%p.log -XX:HeapDumpPath=C:\Users\User\java_error_in_idea64.hprof

		// для репортов JVM
		Files.createDirectories(gameDirPath.resolve("crashes"));

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
		// убрал из-за падения производительности
//		command.add("-XX:-OmitStackTraceInFastThrow");

		// создаем хипдамп при недостатке памяти
		command.add("-XX:+HeapDumpOnOutOfMemoryError");

		// макс. размер стека вызова
		command.add("-Xss1M");

//		// антиинжект
//		command.add("-XX:+DisableAttachMechanism");

		// память
		command.add("-Xms256m");
		if (config.limitMemoryMegabytes > 0) {
			command.add("-Xmx" + (config.limitMemoryMegabytes) + "m");
		}

		if (PlatformHelper.getPlatform() == PlatformHelper.Platform.MAC) {
			command.add("-XstartOnFirstThread");
		}

		// вырубаем Ipv6 сокеты по дефолту для ускорения работы нетворкинга
		command.add("-Djava.net.preferIPv4Stack=true");

		// либрарипатх
		List<String> binPath = new ArrayList<>();
		Path binDir = gameDirPath.resolve("bin");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(binDir)) {
			for (Path path : ds) {
				binPath.add(gameDirPath.relativize(path).toString());
			}
		}
		Collections.sort(binPath);
		command.add("-Djava.library.path=" + String.join(File.pathSeparator, binPath));

		// класспатх
		List<String> classPath = new ArrayList<>();
		Path libDir = gameDirPath.resolve("lib");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(libDir)) {
			for (Path path : ds) {
				classPath.add(gameDirPath.relativize(path).toString());
			}
		}
		Collections.sort(classPath);
		command.add("-cp");
		command.add(String.join(File.pathSeparator, classPath));

		command.add("start.StartClient");

		command.add("--session");
		command.add(buildSessionString(session));

		command.add("--updateBranch");
		command.add(config.clientBranch.getName());

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(gameDirPath.toFile());

		pb.inheritIO();

		Process process = pb.start();
		return process.waitFor();
	}

	private static String buildSessionString(Session session) {
		return Base64.getEncoder()
			.withoutPadding()
			.encodeToString(HttpClientWrapper.GSON.toJson(session).getBytes(StandardCharsets.UTF_8));
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
			} catch (Throwable e) {
				Platform.runLater(() -> DialogUtils.showError("Ошибка запуска процесса игры", e, true));
			} finally {
				launched.set(false);
			}

			Log.msg("Game process finished with exit code %d", exitCode);

			int finalExitCode = exitCode;
			Platform.runLater(() -> {
				application.getTrayManager().uninstall();
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
