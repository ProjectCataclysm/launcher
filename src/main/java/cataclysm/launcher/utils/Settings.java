package cataclysm.launcher.utils;

import cataclysm.launcher.utils.logging.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 06.10.2020 23:59
 *
 * @author Knoblul
 */
public class Settings {
	private final Path configFilePath;
	private final Properties properties = new Properties();

	public int allocatedMemory = 2048;
	public int downloadSpeedLimit = 0;

	public boolean hideToTrayOnClose = false;

	public ReporterPolicy reporterPolicy = ReporterPolicy.ASK;
	public LaunchVisibilityAction launchVisibilityAction = LaunchVisibilityAction.HIDE_TO_TRAY;

	public Path gameDirPath = (PlatformHelper.getOS() == PlatformHelper.PlatformOS.WINDOWS ? Paths.get(".")
			: PlatformHelper.getLauncherDirectoryPath()).resolve("game");

	public Settings(Path launcherDirPath) {
		this.configFilePath = launcherDirPath.resolve("config.cfg");

		if (!Files.exists(gameDirPath)) {
			try {
				Files.createDirectories(gameDirPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void load() {
		if (!Files.exists(configFilePath)) {
			save();
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(configFilePath)) {
			properties.clear();
			properties.load(reader);
			allocatedMemory = Integer.parseInt(properties.getProperty("allocated_memory", Integer.toString(allocatedMemory)));
			downloadSpeedLimit = Integer.parseInt(properties.getProperty("download_speed_limit", Integer.toString(downloadSpeedLimit)));
			hideToTrayOnClose = Boolean.parseBoolean(properties.getProperty("tray_on_close", Boolean.toString(hideToTrayOnClose)));
			launchVisibilityAction = LaunchVisibilityAction.valueOf(properties.getProperty("launch_visibility_action", LaunchVisibilityAction.HIDE_TO_TRAY.name()).toUpperCase());
			reporterPolicy = ReporterPolicy.valueOf(properties.getProperty("crash_reporter_policy", ReporterPolicy.ASK.name()).toUpperCase());
			if (PlatformHelper.getOS() != PlatformHelper.PlatformOS.WINDOWS) {
				gameDirPath = Paths.get(properties.getProperty("game_location", gameDirPath.toString()));
				if (!Files.exists(gameDirPath)) {
					try {
						Files.createDirectories(gameDirPath);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			Log.warn(e, "Failed to load config file");
		}
	}

	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(configFilePath)) {
			properties.clear();
			properties.setProperty("allocated_memory", Integer.toString(allocatedMemory));
			properties.setProperty("download_speed_limit", Integer.toString(downloadSpeedLimit));
			properties.setProperty("tray_on_launch", Boolean.toString(hideToTrayOnClose));
			properties.setProperty("launch_visibility_action", launchVisibilityAction.name());
			properties.setProperty("crash_reporter_policy", reporterPolicy.name());

			if (PlatformHelper.getOS() != PlatformHelper.PlatformOS.WINDOWS) {
				properties.setProperty("game_location", gameDirPath.toString());
			}
			properties.store(writer, "Project Cataclysm Launcher");
		} catch (IOException e) {
			Log.warn(e, "Failed to save config file");
		}
	}

	public int getAllocatedMemory() {
		return Math.min(Math.max(allocatedMemory, 512), PlatformHelper.getMaximumMemoryMB());
	}

	public enum ReporterPolicy {
		ASK("Спрашивать перед отправкой"),
		ALWAYS_SEND("Отправлять всегда"),
		NEVER_SEND("Никогда не отправлять"),

		;

		private final String name;

		ReporterPolicy(String name) {
			this.name = name;
		}

		public static ReporterPolicy find(String name) {
			for (ReporterPolicy value : values()) {
				if (value.name.equals(name)) {
					return value;
				}
			}

			throw new IllegalArgumentException(name);
		}

		public String getName() {
			return name;
		}
	}

	public enum LaunchVisibilityAction {
		HIDE_TO_TRAY("Свернуть лаунчер в область уведомлений"),
		KEEP_OPEN("Оставить лаунчер открытым"),
		CLOSE("Закрыть лаунчер"),

		;

		final String name;

		LaunchVisibilityAction(String name) {
			this.name = name;
		}

		public static LaunchVisibilityAction find(String name) {
			for (LaunchVisibilityAction value : values()) {
				if (value.name.equals(name)) {
					return value;
				}
			}

			throw new IllegalArgumentException(name);
		}

		public String getName() {
			return name;
		}
	}
}
