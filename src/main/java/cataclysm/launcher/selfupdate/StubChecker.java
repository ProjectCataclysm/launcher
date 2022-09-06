package cataclysm.launcher.selfupdate;

import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.LauncherLock;
import cataclysm.launcher.utils.Log;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 05.08.2022 16:41
 *
 * @author Knoblul
 */
public class StubChecker {
	static Path currentJarPath() {
		try {
			Path path = Paths.get(StubChecker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			String fn = path.getFileName().toString().toLowerCase(Locale.ROOT);
			if (Files.isDirectory(path) || !fn.endsWith(".jar") && !fn.endsWith(".exe")) {
				return null;
			}

			return path;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	static Path stubJarPath() {
		return LauncherConfig.LAUNCHER_DIR_PATH.resolve("launcher.jar");
	}

	private static Path checkLocation() {
		Path jar = currentJarPath();
		Path stubJar = stubJarPath();
		Path updateJar = AutoUpdater.updateJarPath();

		// Если запуск из IDE, то игнорим всю логику
		// Если запуск из exe-файла, то смотрим есть ли файл стаба на компе, если есть запускаем через него
		//  если нет, то AutoUpdater должен его скачать
		if (jar == null || jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe")
				&& !Files.isRegularFile(stubJar)) {
			return null;
		}

		if (jar.equals(updateJar) || !Files.isRegularFile(stubJar)) {
			try {
				Log.msg("Copying self into " + stubJar);
				Files.copy(jar, stubJar, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException("Failed to copy " + jar + " into " + jar, e);
			}
		}

		return !jar.equals(stubJar) ? stubJar : null;
	}

	static void restart(Path path) {
		try {
			LauncherLock.unlock();
			List<String> command = Lists.newArrayList();
			command.add("java");
			command.add("-Xmx256m"); // Так как лаунчер по кд висит в фоне, ему надо как можно минимум памяти задать
			command.add("-jar");
			command.add(path.toAbsolutePath().toString());
			new ProcessBuilder(command).start();
		} catch (IOException e) {
			throw new RuntimeException("Failed to restart launcher as " + path, e);
		}

		throw new LauncherRestartedException();
	}

	public static void check(String[] args) {
		if (args.length > 0) {
			// Если в параметр был передан файл с окночнанием .jar, то удаляем
			// этот файл
			// нужно для удаления родительского джарника, напр. при запуске
			// лаунчера через обновляющий джар
			Path src = Paths.get(args[0]);
			if (src.getFileName().endsWith(".jar")) {
				try {
					Files.deleteIfExists(src);
				} catch (IOException e) {
					Log.err(e, "Failed to delete jar");
				}
			}
		}

		Path location = checkLocation();
		if (location != null) {
			Log.msg("Restarting as " + location);
			// перезапускаем лаунчер в другом джаре
			restart(location);
		}
	}
}
