package cataclysm.launcher.download.santation;

import cataclysm.launcher.download.ChecksumUtil;
import cataclysm.launcher.Launcher;
import cataclysm.launcher.utils.Log;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created 16 окт. 2018 г. / 21:06:42
 *
 * @author Knoblul
 */
public class SanitationManager extends JComponent {
	public SanitationManager() {
		fill();
	}

//	private static void deleteFileOrFolder(final Path path) throws IOException {
//		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
//			@Override
//			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
//				Files.delete(file);
//				return FileVisitResult.CONTINUE;
//			}
//
//			@Override
//			public FileVisitResult visitFileFailed(final Path file, final IOException e) {
//				return handleException(e);
//			}
//
//			private FileVisitResult handleException(final IOException e) {
//				e.printStackTrace(); // replace with more robust error handling
//				return FileVisitResult.TERMINATE;
//			}
//
//			@Override
//			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
//				if (e != null)
//					return handleException(e);
//				Files.delete(dir);
//				return FileVisitResult.CONTINUE;
//			}
//		});
//	};

	private void fill() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;

		JLabel title = new JLabel("Проверяем файлы...");
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 40));

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		add(title, gbc);

		gbc.gridy = 1;

		ImageIcon icon = new ImageIcon(SanitationManager.class.getResource("/icons/loading.gif"));
		JLabel iconLabel = new JLabel();
		iconLabel.setIcon(icon);
		iconLabel.setOpaque(false);
		iconLabel.setHorizontalAlignment(JLabel.CENTER);
		add(iconLabel, gbc);
	}

	private boolean findModifiedFiles(Path rootPath, Path filePath, Map<String, String> hashes) {
		if (Files.isDirectory(filePath)) {
			try {
				try (DirectoryStream<Path> ds = Files.newDirectoryStream(filePath)) {
					for (Path path : ds) {
						if (findModifiedFiles(rootPath, path, hashes)) {
							return true;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				return true;
			}
			return false;
		} else {
			String fn = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
			Log.msg("Hashing " + filePath);
			Stopwatch sw = Stopwatch.createStarted();
			String hash = ChecksumUtil.computeChecksum(filePath);
			sw.stop();
			Log.msg("hashed in " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

//			System.out.println(fn + "|" + hashes.get(fn) + "|" + hash + "|" + (hashes.get(fn) == null || !hashes.get(fn).equals(hash)));
			return hashes.get(fn) == null || !hashes.get(fn).equals(hash);
		}
	}

	private boolean findMissingFiles(Path rootPath, Path filePath, Map<String, String> hashes) {
		List<Path> paths = Lists.newArrayList();
		String path = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
		for (String fn : hashes.keySet()) {
			if (fn.startsWith(path)) {
				paths.add(rootPath.resolve(fn));
			}
		}

		for (Path p : paths) {
			if (!Files.isRegularFile(p)) {
				return true;
			}
		}

		return false;
	}

	private void recursiveDeleteExtraFiles(Path rootPath, Path filePath, Map<String, String> hashes) throws IOException {
		if (Files.isDirectory(filePath)) {
			// пробегаемся по всем файлам в директории
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(filePath)) {
				for (Path path : ds) {
					recursiveDeleteExtraFiles(rootPath, path, hashes);
				}
			}

			int fileNum = 0;
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(filePath)) {
				for (Path ignored : ds) {
					fileNum++;
				}
			}

			// удаляем пустые директории
			if (fileNum == 0) {
				Files.deleteIfExists(filePath);
			}
		} else {
			// получаем имя файла
			String fn = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');

			// проверяем, есть ли имя в хешах
			if (!hashes.containsKey(fn)) {
				Files.deleteIfExists(filePath);
			}
		}
	}

	public List<Resource> perform(ResourceMaster master) throws IOException {
		// получаем игровую директорию
		Path rootPath = Launcher.config.gameDirectoryPath;
		if (!Files.isDirectory(rootPath)) {
			// если директория остуствует, то создаём её и добавляем ВСЕ ресурсы
			// на скачивание
			Files.createDirectories(rootPath);
			return master.getResources();
		}

		List<Resource> toDownload = Lists.newArrayList();
		List<Resource> resources = master.getResources();
		Map<String, String> hashes = master.getHashes();
		System.out.println(hashes);

		// удаляем все посторонние файлы из защищёных директорий
		for (String folder : master.getProtectedFolders()) {
			recursiveDeleteExtraFiles(rootPath, rootPath.resolve(folder), hashes);
		}

		// санитизируем папку с игрой, сравниваем хеши файлов с hashes
		for (Resource resource : resources) {
			Path localPath = rootPath.resolve(resource.getLocal());
			if (!Files.isRegularFile(localPath)) {
				// если локальный файл не существует добавляем ресурс на
				// скачивание
				toDownload.add(resource);
				continue;
			}

			if (resource.isHashed()) {
				// ВАЖНО: не сканируем рут!
				if (rootPath.equals(localPath)) {
					continue;
				}

				// если недостаёт файлов или файлы изменены - то добавляем
				// ресурс на скачивание
				if (findMissingFiles(rootPath, localPath, hashes) || findModifiedFiles(rootPath, localPath, hashes)) {
					toDownload.add(resource);
				}
			}
		}

		return toDownload;
	}
}
