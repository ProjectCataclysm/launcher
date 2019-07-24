package cataclysm.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
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
	public static final String VERSION = "1.4";

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
		JLabel iconlal = new JLabel();
		iconlal.setIcon(icon);
		iconlal.setOpaque(false);
		iconlal.setHorizontalAlignment(JLabel.CENTER);
		add(iconlal, gbc);

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

	private File currentJarLocation() throws IOException {
		try {
			File jar = new File(VersionHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (jar.isDirectory() || !jar.getName().endsWith(".jar")) {
				return null;
			}
			return jar;
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private File stubJarLocation() throws IOException {
		return new File(Launcher.workDir, "launcher.jar");
	}
	
	private File updateJarLocation() throws IOException {
		return new File(Launcher.workDir, "launcher_update.jar");
	}

	private File checkLocation() {
		try {
			File jar = currentJarLocation();
			File stubJar = stubJarLocation();
			File updateJar = updateJarLocation();
			
			if (jar == null) {
				return null;
			}
			
			if (jar.equals(updateJar) || !stubJar.exists()) {
				try {
					if (stubJar.exists()) {
						stubJar.delete();
					}
					Files.copy(jar.toPath(), stubJar.toPath());
				} catch (IOException e) {
					DialogUtils.showError("failed to copy update/launcher jar", e);
					throw new RuntimeException(e);
				}
			}
			
			return jar != null && !jar.equals(stubJar) ? stubJar : null;
		} catch (IOException e) {
			DialogUtils.showError("checkLocation error!", e);
			throw new RuntimeException(e);
		}
	}

	private boolean checkUpdates() {
		updateCheckingUI();
		
		try {
			File jar = currentJarLocation();
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
	
	private void relaunch(File file) {
		try {
			List<String> command = Lists.newArrayList();
			command.add("java");
			command.add("-jar");
			command.add(file.getAbsolutePath());
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
			File updateJar = updateJarLocation();
			
			Log.msg("Downloading update...");

			URL url = HttpHelper.launcherURL("launcher.jar");
			URLConnection connection = HttpHelper.open(url);//url.openConnection();

			progress.setMaximum(connection.getContentLength());
			progress.setMinimum(0);
			progress.setValue(0);
			int total = 0;
			try (InputStream in = url.openStream();
					OutputStream out = Files.newOutputStream(updateJar.toPath())) {
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
			command.add(updateJar.getAbsolutePath());
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.start();
		} catch (IOException e) {
			DialogUtils.showError("Невозможно обновить лаунчер!", e);
		}
		
		System.exit(0);
	}
	
	public boolean shouldStartLauncher(String[] args) {
		if (args.length > 0) {
			// если в параметр был передан файл с окночнанием .jar, то удаляем
			// этот файл
			// нужно для удаления родительского джарника, напр. при запуске
			// лаунчера через обновляющий джар
			File src = new File(args[0]);
			if (src.getName().endsWith(".jar")) {
				src.delete();
			}
		}
		
		File relaunchLocation = checkLocation();
		if (relaunchLocation != null) {
			// перезапускаем лаунчер в другом джаре
			Launcher.frame.setVisible(false);
			relaunch(relaunchLocation);
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
