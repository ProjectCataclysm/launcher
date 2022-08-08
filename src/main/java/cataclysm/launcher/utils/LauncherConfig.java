package cataclysm.launcher.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 06.08.2022 13:32
 *
 * @author Knoblul
 */
public class LauncherConfig {
	public static final Path LAUNCHER_DIR_PATH;

	public Path gameDirectoryPath = PlatformHelper.getDefaultGameDirectory();
	public int limitMemoryMegabytes;
	public boolean fullscreen = true;
	private final Path configFile = LAUNCHER_DIR_PATH.resolve("launcher.cfg");

	public LauncherConfig() {
		Log.msg("Config file %s", configFile);
		load();
	}

	public void load() {
		try (InputStream in = Files.newInputStream(configFile)) {
			Log.msg("Loading config file...");
			Properties props = new Properties();
			props.load(in);

			gameDirectoryPath = Paths.get(props.getProperty("gameDirectory", gameDirectoryPath.toString()));
			limitMemoryMegabytes = roundUpToPowerOfTwo(
					Integer.parseInt(props.getProperty("limitMemory", Integer.toString(limitMemoryMegabytes))));
			fullscreen = Boolean.parseBoolean(props.getProperty("fullscreen", Boolean.toString(fullscreen)));
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
			props.setProperty("fullscreen", Boolean.toString(fullscreen));
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

	static {
		try {
			LAUNCHER_DIR_PATH = Files.createDirectories(Paths.get(System.getProperty("user.home"), "ProjectCataclysm"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to create work directory", e);
		}
	}
}
