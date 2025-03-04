package cataclysm.launcher.utils;

import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 06.08.2022 13:32
 *
 * @author Knoblul
 */
public class LauncherConfig {
	private static final Map<String, ClientBranch> CLIENT_BRANCH_NAME_MAP = new HashMap<>();

	public static final boolean IS_INSTALLATION = Files.isRegularFile(Paths.get(".setup"));
	public static final Path LAUNCHER_DIR_PATH;

	public Path gameDirectoryPath = IS_INSTALLATION && LAUNCHER_DIR_PATH.getParent() != null
	                                ? LAUNCHER_DIR_PATH.getParent().resolve("client")
	                                : PlatformHelper.getDefaultGameDirectory();

	public int limitMemoryMegabytes;

	private final Path configFile = LAUNCHER_DIR_PATH.resolve("launcher.cfg");

	public ClientBranch clientBranch = ClientBranch.PRODUCTION;

	public LauncherConfig() {
		Log.msg("Config file %s", configFile);
		load();
	}

	public Path getCurrentGameDirectoryPath() {
		return clientBranch.getSubDirName() != null
		       ? gameDirectoryPath.resolve(clientBranch.getSubDirName())
		       : gameDirectoryPath;
	}

	public void load() {
		try (InputStream in = Files.newInputStream(configFile)) {
			Log.msg("Loading config file...");
			Properties props = new Properties();
			props.load(in);

			gameDirectoryPath = Paths.get(props.getProperty("gameDirectory", gameDirectoryPath.toString()));
			limitMemoryMegabytes = roundUpToPowerOfTwo(Integer.parseInt(props.getProperty("limitMemory", Integer.toString(limitMemoryMegabytes))));
			clientBranch = CLIENT_BRANCH_NAME_MAP.getOrDefault(props.getProperty("clientUpdateBranch", "").toLowerCase(Locale.ROOT), ClientBranch.PRODUCTION);
		} catch (FileNotFoundException | NoSuchFileException e) {
			save();
		} catch (Exception e) {
			Log.err(e, "Can't load config file");
			save();
		}

		if (limitMemoryMegabytes != 0 && limitMemoryMegabytes < 1024
				|| limitMemoryMegabytes > PlatformHelper.getMaxMemory()) {
			limitMemoryMegabytes = 0;
		}
	}

	public void save() {
		try (OutputStream out = Files.newOutputStream(configFile)) {
			Log.msg("Saving config file...");
			Properties props = new Properties();
			props.setProperty("gameDirectory", gameDirectoryPath.toAbsolutePath().toString());
			props.setProperty("limitMemory", Integer.toString(limitMemoryMegabytes));
			props.setProperty("clientUpdateBranch", clientBranch.getName());
			props.store(out, "");
		} catch (IOException e) {
			Log.err(e, "Can't save config file");
		}
	}

	/**
	 * Returns the input value rounded up to the next highest power of two.
	 */
	public static int roundUpToPowerOfTwo(int value) {
		int result = value - 1;
		result |= result >> 1;
		result |= result >> 2;
		result |= result >> 4;
		result |= result >> 8;
		result |= result >> 16;
		return result + 1;
	}

	public enum ClientBranch {
		PRODUCTION("production", "Основной клиент", null),
		TEST("test", "Тестовый клиент", "branch_test"),

		;

		private final String name;
		private final String title;
		private final @Nullable String subDirName;

		ClientBranch(String name, String title, @Nullable String subDirName) {
			this.name = name;
			this.title = title;
			this.subDirName = subDirName;
		}

		public String getName() {
			return name;
		}

		public String getTitle() {
			return title;
		}

		public @Nullable String getSubDirName() {
			return subDirName;
		}
	}

	static {
		for (ClientBranch v : ClientBranch.values()) {
			CLIENT_BRANCH_NAME_MAP.put(v.getName().toLowerCase(Locale.ROOT), v);
		}

		if (IS_INSTALLATION) {
			LAUNCHER_DIR_PATH = Paths.get("").toAbsolutePath();
		} else {
			try {
				LAUNCHER_DIR_PATH = Files.createDirectories(Paths.get(System.getProperty("user.home"), "ProjectCataclysm"));
			} catch (IOException e) {
				throw new RuntimeException("Failed to create work directory", e);
			}
		}
	}
}
