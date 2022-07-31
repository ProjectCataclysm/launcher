package cataclysm.launcher.download;

import cataclysm.launcher.Launcher;
import cataclysm.launcher.download.santation.Resource;
import cataclysm.launcher.utils.HttpHelper;
import org.apache.http.client.methods.CloseableHttpResponse;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 16 окт. 2018 г. / 21:32:13
 *
 * @author Knoblul
 */
public class DownloadingManager extends JComponent {
	private static final String[] STORAGE_UNITS = new String[] { "Б", "КБ", "МБ", "ГБ", "ТБ" };
	private static final int DL_SPEED_MEASURE_PERIOD = 500;
	private static final byte[] buffer = new byte[8192];

	private JProgressBar downloadProgress;
	private JProgressBar overallProgress;

	private JLabel currentFileLoading;
	private JLabel currentSpeed;

	private final Object speedLock = new Object();

	private int downloadedBytes;
	private boolean measureDownloadSpeed;

	public DownloadingManager() {
		fill();

		Timer timerDownloadSpeed = new Timer("DownloadSpeedCalculator", true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				synchronized (speedLock) {
					if (measureDownloadSpeed) {
						int speed = (int)(downloadedBytes/(double) DL_SPEED_MEASURE_PERIOD * 1000.0);
						currentSpeed.setText(readableSpeed(speed));
						downloadedBytes = 0;
					}
				}
			}
		};
		timerDownloadSpeed.schedule(task, 0, DL_SPEED_MEASURE_PERIOD);
	}
	
	private static String readableSpeed(int size) {
		if (size <= 0) {
			return "0 Б/сек";
		}

		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#")
				.format(size / Math.pow(1024, digitGroups)) + " " + STORAGE_UNITS[digitGroups] + "/сек";
	}

	private void fill() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;

		JLabel title = new JLabel("Загружаем файлы...");
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 40));

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		add(title, gbc);

		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.SOUTHEAST;
		gbc.insets.set(2, 2, 2, 2);
		add(currentSpeed = new JLabel(), gbc);
		currentSpeed.setFont(currentSpeed.getFont().deriveFont(Font.PLAIN, 20));

		gbc.gridy++;
		gbc.weighty = 0;
		add(currentFileLoading = new JLabel(), gbc);
		currentFileLoading.setFont(currentFileLoading.getFont().deriveFont(Font.PLAIN, 20));

		downloadProgress = new JProgressBar();
		downloadProgress.setBorderPainted(false);
		downloadProgress.setForeground(Color.GREEN.darker());
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.SOUTH;
		add(downloadProgress, gbc);

		overallProgress = new JProgressBar();
		overallProgress.setBorderPainted(false);
		overallProgress.setForeground(Color.GREEN.darker());
		gbc.gridy++;
		gbc.weightx = 1;
		add(overallProgress, gbc);

		currentFileLoading.setText("...");
	}

	private void downloadFile(Path localFilePath, Resource resource) throws IOException {
		Files.createDirectories(localFilePath.getParent());

		String url = "https://" + HttpHelper.CLIENT_URL + "/" + resource.getRemote();
		try (CloseableHttpResponse response = HttpHelper.get(url)) {
			if (response.getEntity() == null) {
				throw new IOException("Server does not sent any file");
			}

			downloadProgress.setMinimum(0);
			downloadProgress.setValue(0);
			downloadProgress.setMaximum((int) response.getEntity().getContentLength());

			currentFileLoading.setText("Загружаем " + resource.getRemote());

			long totalLen = 0;
			try (InputStream in = response.getEntity().getContent();
			     OutputStream out = Files.newOutputStream(localFilePath)) {
				int len;
				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
					totalLen += len;
					downloadProgress.setValue((int) totalLen);
					synchronized (speedLock) {
						downloadedBytes += len;
					}
				}
			}


		} catch (IOException e) {
			throw new IOException("Could not download " + resource.getRemote(), e);
		}

		currentFileLoading.setText("");

		synchronized (speedLock) {
			measureDownloadSpeed = false;
			currentSpeed.setText("");
		}
	}

	private void downloadAndUnpackArchive(Path rootPath, Resource resource) throws IOException {
		Path zipPath = rootPath.resolve(resource.getRemote());
		downloadFile(zipPath, resource);

		Path unpackPath = rootPath.resolve(resource.getLocal());

		downloadProgress.setMinimum(0);
		downloadProgress.setValue(0);
		currentFileLoading.setText("Распаковываем " + zipPath.getFileName().toString());
		int entryCount = 0;

		try (InputStream in = Files.newInputStream(zipPath);
				ZipInputStream zip = new ZipInputStream(in)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					entryCount++;
				}
				zip.closeEntry();
			}
		}

		downloadProgress.setMaximum(entryCount);
		
		try (InputStream in = Files.newInputStream(zipPath, StandardOpenOption.DELETE_ON_CLOSE);
			 ZipInputStream zip = new ZipInputStream(in)) {
			ZipEntry entry;
			int index = 0;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					Path outputPath = unpackPath.resolve(entry.getName());
					Files.createDirectories(outputPath.getParent());

					try (OutputStream out = Files.newOutputStream(outputPath)) {
						int len;
						while ((len = zip.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}
					}

					downloadProgress.setValue(index++);
				}
				zip.closeEntry();
			}
		} catch (Throwable e) {
			throw new IOException("Could not unpack " + zipPath, e);
		} finally {
			currentFileLoading.setText("");
			synchronized (speedLock) {
				measureDownloadSpeed = true;
			}
		}
	}

	private void performDownload(Path rootPath, Resource resource) throws IOException {
		synchronized (speedLock) {
			measureDownloadSpeed = true;
		}

		if (resource.isFolder()) {
			downloadAndUnpackArchive(rootPath, resource);
		} else {
			downloadFile(rootPath.resolve(resource.getLocal()), resource);
		}
	}

	public void perform(List<Resource> resources) throws IOException {
		Path root = Launcher.config.gameDirectoryPath;

		overallProgress.setMinimum(0);
		overallProgress.setMaximum(resources.size()+1);
		overallProgress.setValue(1);

		for (int i = 0; i < resources.size(); i++) {
			performDownload(root, resources.get(i));
			overallProgress.setValue(i+2);
		}
	}
}
