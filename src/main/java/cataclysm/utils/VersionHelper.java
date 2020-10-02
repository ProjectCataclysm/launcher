package cataclysm.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.google.common.collect.Lists;

import cataclysm.launch.Launcher;
import cataclysm.ui.DialogUtils;

/**
 * Created 20 окт. 2018 г. / 22:51:07 
 * @author Knoblul
 */
public class VersionHelper extends JComponent {
	private static final long serialVersionUID = -6203764377406251750L;
	private JProgressBar progress;
	public static final String VERSION = "1.5.07";

	public VersionHelper() {
		progress = new JProgressBar();
		progress.setBorderPainted(false);
		progress.setForeground(Color.green.darker());
	}

	private void updateCheckingUI() {
		removeAll();

		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;

		JLabel title = new JLabel("Проверка наличия обновлений...");
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 20));

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		add(title, gbc);

		gbc.gridy = 1;

		ImageIcon icon = new ImageIcon(VersionHelper.class.getResource("/icons/loading.gif"));
		JLabel iconLabel = new JLabel();
		iconLabel.setIcon(icon);
		iconLabel.setOpaque(false);
		iconLabel.setHorizontalAlignment(JLabel.CENTER);
		add(iconLabel, gbc);

		revalidate();
		repaint();
		validate();
	}

	private void updatingProgressUI() {
		removeAll();

		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		JLabel title = new JLabel("Обновляем лаунчер");
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 30));

		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(title, gbc);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy = 1;
		add(progress, gbc);

		revalidate();
		repaint();
		validate();
	}

	private Path currentJarPath() throws IOException {
		try {
			Path path = Paths.get(VersionHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (Files.isDirectory(path) || !path.getFileName().toString().toLowerCase().endsWith(".jar")
					&& !path.getFileName().toString().toLowerCase().endsWith(".exe")) {
				return null;
			}
			return path;
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private Path stubJarPath() {
		return Launcher.workDirPath.resolve("launcher.jar");
	}
	
	private Path updateJarPath() {
		return Launcher.workDirPath.resolve("launcher_update.jar");
	}

	private Path checkLocation() {
		try {
			Path jar = currentJarPath();
			Path stubJar = stubJarPath();
			Path updateJar = updateJarPath();
			
			if (jar == null || jar.getFileName().toString().toLowerCase().endsWith(".exe")) {
				return null;
			}
			
			if (jar.equals(updateJar) || !Files.exists(stubJar)) {
				try {
					if (Files.exists(stubJar)) {
						Files.deleteIfExists(stubJar);
					}
					Files.copy(jar, stubJar);
				} catch (IOException e) {
					DialogUtils.showError("failed to copy update/launcher jar", e);
					throw new RuntimeException(e);
				}
			}
			
			return !jar.equals(stubJar) ? stubJar : null;
		} catch (IOException e) {
			DialogUtils.showError("checkLocation error!", e);
			throw new RuntimeException(e);
		}
	}

	private boolean checkUpdates() {
		updateCheckingUI();
		try {
			Path jar = currentJarPath();
			if (jar == null) {
				// eclipse-версия всегда Up-to-date
				return true;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			URL url = HttpHelper.launcherURL("version.txt");
			try (InputStream in = url.openStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				String version = reader.readLine();
				if (version == null) {
					throw new IOException("Malformed versions file");
				}
				return version.equals(VERSION);
			}
		} catch (IOException e) {
			DialogUtils.showError("Невозможно проверить наличие обновлений!", e);
			throw new RuntimeException(e);
		}
	}
	
	private void relaunch(Path path) {
		try {
			List<String> command = Lists.newArrayList();
			command.add("java");
			command.add("-jar");
			command.add(path.toAbsolutePath().toString());
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.start();
		} catch (IOException e) {
			DialogUtils.showError("relaunch error!", e);
		}
		
		System.exit(0);
	}
	
	private void updateLauncher() {
		updatingProgressUI();
		try {
			Path updateJar = updateJarPath();
			
			Log.msg("Downloading update...");

			URL url = HttpHelper.launcherURL("launcher.jar");
			URLConnection connection = HttpHelper.open(url);//url.openConnection();

			progress.setMaximum(connection.getContentLength());
			progress.setMinimum(0);
			progress.setValue(0);
			int total = 0;
			try (InputStream in = url.openStream();
					OutputStream out = Files.newOutputStream(updateJar)) {
				int len;
				byte[] buffer = new byte[4096];
				while ((len = in.read(buffer)) > 0) {
					out.write(buffer,0,len);
					progress.setValue(total += len);
				}
			}
			
			List<String> command = Lists.newArrayList();
			command.add("java");
			command.add("-jar");
			command.add(updateJar.toAbsolutePath().toString());
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.start();
		} catch (IOException e) {
			DialogUtils.showError("Невозможно обновить лаунчер!", e);
		}
		
		System.exit(0);
	}
	
	private boolean relaunchExeAsStub() {
		try {
			Path jar = currentJarPath();
			Path stub = stubJarPath();
			if (jar != null && jar.getFileName().toString().toLowerCase().endsWith(".exe")) {
				if (!Files.exists(stub)) {
					Log.err("Launcher outdated!");
					Launcher.frame.setVisible(false);
					Launcher.frame.showVersionChecker(this);
					updateLauncher();
				} else {
					relaunch(stub);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return false;
	}
	
	public boolean shouldStartLauncher(String[] args) {
		if (args.length > 0) {
			// если в параметр был передан файл с окночнанием .jar, то удаляем
			// этот файл
			// нужно для удаления родительского джарника, напр. при запуске
			// лаунчера через обновляющий джар
			Path src = Paths.get(args[0]);
			if (src.getFileName().endsWith(".jar")) {
				try {
					Files.deleteIfExists(src);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		Path relaunchLocation = checkLocation();
		if (relaunchLocation != null) {
			// перезапускаем лаунчер в другом джаре
			Launcher.frame.setVisible(false);
			relaunch(relaunchLocation);
			return false;
		}
		
		// если лаунчер обёрнут в exe, то перезапускаем его в джарнике
		if (relaunchExeAsStub()) {
			return false;
		}
		
		if (!checkUpdates()) {
			Log.err("Launcher outdated!");
			Launcher.frame.setVisible(false);
			Launcher.frame.showVersionChecker(this);

			// обновляем лаунчер
			updateLauncher();
			return false;
		}
		
		// все проверки прошли успешно, запускаем данный экземпляр как основной лаунчер
		return true;
	}
}
