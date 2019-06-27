package cataclysm.launch;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import cataclysm.io.sanitization.Resource;
import cataclysm.utils.HttpHelper;

/**
 * 
 * 
 * <br><br><i>Created 16 окт. 2018 г. / 21:32:13</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * @author Knoblul
 */
public class DownloadingManager extends JComponent {
	private static final long serialVersionUID = -6748902640791102435L;
	private static final int DLSPEED_MEASURE_PERIOD = 500;

	private static byte[] buffer = new byte[4096];

	private JProgressBar downloadProgress;
	private JProgressBar overallProgress;

	private JLabel currentFileLoading;
	private JLabel currentSpeed;

	private Timer timerDownloadSpeed;
	private int downloadedBytes;
	private boolean measureDownloadSpeed;

	public DownloadingManager() {
		fill();

		timerDownloadSpeed = new Timer("DownloadSpeedCalculator", true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				synchronized (currentSpeed) {
					if (measureDownloadSpeed) {
						int speed = (int)(downloadedBytes/(double)DLSPEED_MEASURE_PERIOD*1000.0);
						currentSpeed.setText(readableSpeed(speed));
						downloadedBytes = 0;
					}
				}
			}
		};
		timerDownloadSpeed.schedule(task, 0, DLSPEED_MEASURE_PERIOD);
	}
	
	private static String readableSpeed(int size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "Б", "КБ", "МБ", "ГБ", "ТБ" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups]
				+ "/сек";
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

	private void downloadFile(File root, Resource resource) throws IOException {
		File output = new File(root, resource.getLocal());
		URL url = HttpHelper.clientURL(resource.getRemote());
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
		downloadProgress.setMinimum(0);
		downloadProgress.setValue(0);
		downloadProgress.setMaximum(connection.getContentLength());
		currentFileLoading.setText("Загружаем " + resource.getRemote());

		long totalLen = 0;
		try (InputStream in = connection.getInputStream(); 
				OutputStream out = Files.newOutputStream(output.toPath())) {
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
				totalLen += len;
				downloadProgress.setValue((int) totalLen);
				synchronized (currentSpeed) {
					downloadedBytes += len;
				}
			}
		} catch (IOException e) {
			throw new IOException("Could not download " + resource.getRemote(), e);
		} finally {
			HttpHelper.close(connection);
		}

		currentFileLoading.setText("");
	}

	private void downloadAndUnpackArchive(File root, Resource resource) throws IOException {
		File unpackDir = new File(root, resource.getLocal());
		File zipFile = new File(root, resource.getRemote());

//		if (unpackDir.exists()) {
//			if (!unpackDir.delete()) {
//				throw new IOException("can't delete dir " + unpackDir);
//			}
//			deleteFileOrFolder(unpackDir.toPath());
//		}

		URL url = HttpHelper.clientURL(resource.getRemote());
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
		downloadProgress.setMinimum(0);
		downloadProgress.setValue(0);
		downloadProgress.setMaximum(connection.getContentLength());
		currentFileLoading.setText("Загружаем " + resource.getRemote());

		long totalLen = 0;
		try (InputStream in = connection.getInputStream(); 
				OutputStream out = Files.newOutputStream(zipFile.toPath())) {
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
				totalLen += len;
				downloadProgress.setValue((int) totalLen);
				synchronized (currentSpeed) {
					downloadedBytes += len;
				}
			}
		} catch (Throwable e) {
			throw new IOException("Could not download " + resource.getRemote(), e);
		} finally {
			HttpHelper.close(connection);
		}

		synchronized (currentSpeed) {
			measureDownloadSpeed = false;
			currentSpeed.setText("");
		}

		downloadProgress.setMinimum(0);
		downloadProgress.setValue(0);
		currentFileLoading.setText("Распаковываем " + zipFile.getName());
		int entryCount = 0;

		try (InputStream in = Files.newInputStream(zipFile.toPath());
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
		
		try (InputStream in = Files.newInputStream(zipFile.toPath());
				ZipInputStream zip = new ZipInputStream(in)) {
			ZipEntry entry;
			int index = 0;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					File output = new File(unpackDir, entry.getName());
					File parent = output.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}

					try (OutputStream out = Files.newOutputStream(output.toPath())) {
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
			throw new IOException("Could not unpack " + zipFile, e);
		} finally {
			zipFile.delete();
			currentFileLoading.setText("");
			synchronized (currentSpeed) {
				measureDownloadSpeed = true;
			}
		}
	}

	private void performDownload(File root, Resource resource) throws IOException {
		synchronized (currentSpeed) {
			measureDownloadSpeed = true;
		}

		if (resource.isFolder()) {
			downloadAndUnpackArchive(root, resource);
		} else {
			downloadFile(root, resource);
		}

		synchronized (currentSpeed) {
			measureDownloadSpeed = false;
			currentSpeed.setText("");
		}
	}

	void perform(List<Resource> resources) throws IOException {
		File root = Launcher.config.gameDirectory;

		overallProgress.setMinimum(0);
		overallProgress.setMaximum(resources.size()+1);
		overallProgress.setValue(1);

		for (int i = 0; i < resources.size(); i++) {
			performDownload(root, resources.get(i));
			overallProgress.setValue(i+2);
		}
	}
}
