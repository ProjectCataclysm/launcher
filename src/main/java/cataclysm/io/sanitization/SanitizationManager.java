package cataclysm.io.sanitization;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import cataclysm.io.FileHasher;
import cataclysm.launch.Launcher;

/**
 * Created 16 окт. 2018 г. / 21:06:42 
 * @author Knoblul
 */
public class SanitizationManager extends JComponent {
	private static final long serialVersionUID = 7429535511171151670L;

	public SanitizationManager() {
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

		ImageIcon icon = new ImageIcon(SanitizationManager.class.getResource("/icons/loading.gif"));
		JLabel iconlal = new JLabel();
		iconlal.setIcon(icon);
		iconlal.setOpaque(false);
		iconlal.setHorizontalAlignment(JLabel.CENTER);
		add(iconlal, gbc);
	}
	
	private boolean recursiveCheckHashes(File root, File file, Map<String, String> hashes) {
		if (file.isDirectory()) {
			boolean result = true;
			for (File f: file.listFiles()) {
				result &= recursiveCheckHashes(root, f, hashes);
			}
			return result;
		} else {
			String fn = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
			fn = fn.replace(File.separatorChar, '/');
			
			System.out.println("Hasing " + file);
			Stopwatch sw = Stopwatch.createStarted();
			String hash = FileHasher.hash(file);
			sw.stop();
			System.out.println("hashed in " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0);
			return hashes.get(fn) != null && hashes.get(fn).equals(hash);
		}
	}
	
	private boolean findMissingFiles(File root, File file, Map<String, String> hashes) {
		List<File> files = Lists.newArrayList();
		String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
		for (String fn: hashes.keySet()) {
			path = path.replace(File.separatorChar, '/');
			if (fn.startsWith(path)) {
				files.add(new File(root, fn));
			}
		}
		
		for (File f: files) {
			if (!f.exists()) {
				return true;
			}
		}
		
		return false;
	}
	
	private void recursiveDeleteExtraFiles(File root, File file, Map<String, String> hashes) {
		if (file.isDirectory()) {
			// пробегаемся по всем файлам в директории
			for (File f: file.listFiles()) {
				recursiveDeleteExtraFiles(root, f, hashes);
			}
			
			// удаляем пустые директории
			if (file.listFiles().length == 0) {
				file.delete();
			}
		} else {
			// получаем имя файла
			String fn = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
			fn = fn.replace(File.separatorChar, '/');
			
			// проверяем, есть ли имя в хешах
			if (!hashes.containsKey(fn)) {
				file.delete();
			}
		}
	}

	public List<Resource> perform(ResourceMaster master) throws IOException {
		// получаем игровую директорию
		File root = Launcher.config.gameDirectory;
		if (!root.exists()) {
			// если директория остуствует, то создаём её и добавляем ВСЕ ресурсы
			// на скачивание
			root.mkdirs();
			return master.getResources();
		}
		
		List<Resource> toDownload = Lists.newArrayList();
		List<Resource> resources = master.getResources();
		Map<String, String> hashes = master.getHashes();
		
		// удаляем все посторонние файлы из защищёных директорий
		for (String folder : master.getProtectedFolders()) {
			recursiveDeleteExtraFiles(root, new File(root, folder), hashes);
		}

		// санитизируем папку с игрой, сравниваем хеши файлов с hashes
		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			File local = new File(root, resource.getLocal());
			if (!local.exists()) {
				// если локальный файл не существует добавляем ресурс на
				// скачивание
				toDownload.add(resource);
				continue;
			}

			if (resource.isHashed()) {
				// ВАЖНО: не сканируем рут!
				if (root.getAbsolutePath().equals(local.getAbsolutePath())) {
					continue;
				}
				
				// если недостаёт файлов или файлы изменены - то добавляем
				// ресурс на скачивание
				if (findMissingFiles(root, local, hashes) || !recursiveCheckHashes(root, local, hashes)) {
					toDownload.add(resource);
				}
			}
		}
		
		return toDownload;
	}
}
