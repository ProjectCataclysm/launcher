package cataclysm.io.sanitization;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.google.common.collect.Maps;

import cataclysm.io.FileHasher;
import cataclysm.launch.Launcher;
import cataclysm.utils.HttpHelper;
import cataclysm.utils.Log;

/**
 * Created 16 окт. 2018 г. / 21:06:42 
 * @author Knoblul
 */
public class SanitizationManager extends JComponent {
	private static final long serialVersionUID = 7429535511171151670L;

	public SanitizationManager() {
		fill();
	}

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

	private boolean rescan(File root, File file, Map<String, String> hashes) {
		if (file.isDirectory()) {
			if (hashes.isEmpty()) {
				return file.listFiles().length == 0;
			}

			boolean clear = true;
			for (File f: file.listFiles()) {
				clear &= rescan(root, f, hashes);
			}
			return clear;
		} else {
			String fn = file.getAbsolutePath().substring(root.getAbsolutePath().length()+1);
			fn = fn.replace(File.separator, "/");
			return hashes.containsKey(fn);
		}
	}

	private boolean checkArchiveHashes(File root, Resource resource) throws IOException {
		File unpackDir = new File(root, resource.getUnpackDir());

		Map<String, String> hashes = Maps.newHashMap();

		try (InputStream in = HttpHelper.openStream(HttpHelper.clientURL(resource.getShaFile()));
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String ln;
			while ((ln = reader.readLine()) != null) {
				int idx = ln.lastIndexOf('=');
				if (idx == -1)
					throw new IOException("Malformed hash file");
				String fileName = ln.substring(0, idx);
				String fileHash = ln.substring(idx + 1);
				hashes.put(fileName, fileHash);
			}
		}

		if (!unpackDir.exists()) {
			if (!hashes.isEmpty()) {
				return false;
			} else {
				unpackDir.mkdirs();
			}
		}

		Set<Entry<String,String>> entries = hashes.entrySet();
		for (Entry<String, String> entry: entries) {
			File file = new File(unpackDir, entry.getKey());
			if (!file.exists()) {
				Log.msg("File not exist %s", file);
				return false;
			}

			String currHash = FileHasher.hash(file);
			if (!currHash.equals(entry.getValue())) {
				Log.msg("Hash diff %s %s <> %s", file, currHash, entry.getValue());
				return false;
			}
		}

		return rescan(unpackDir, unpackDir, hashes);
	}

	private boolean checkFileHash(File root, Resource resource) throws IOException {
		File output = new File(root, resource.getFile());
		if (!output.exists()) {
			Log.msg("File not exists %s", output);
			return false;
		}

		try (InputStream in = HttpHelper.openStream(HttpHelper.clientURL(resource.getShaFile()));
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String hash = reader.readLine();
			if (hash == null)
				throw new IOException("Error reading hash file");
			String currHash = FileHasher.hash(output);
			if (!currHash.equals(hash)) {
				Log.msg("Hash diff %s %s <> %s", output, currHash, hash);
				return false;
			}
		}

		return true;
	}

	private boolean exist(File root, Resource resource) {
		String fn = resource.isArchive() ? resource.getUnpackDir() : resource.getFile();
		return new File(root, fn).exists();
	}

	public void perform(List<Resource> resources) throws IOException {
		File gameDir = Launcher.config.gameDirectory;
		if (!gameDir.exists()) {
			gameDir.mkdirs();
		}
		
		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			if (resource.needSanitize()) {
				if (resource.isArchive()) {
					if (checkArchiveHashes(gameDir, resource)) {
						resources.remove(i--);
					}
				} else {
					if (checkFileHash(gameDir, resource)) {
						resources.remove(i--);
					}
				}
			} else {
				if (exist(gameDir, resource)) {
					resources.remove(i--);
				}
			}
		}
		
		// временный костыль для фикса дефолтовых оптифайновских настроек
		if (!new File(gameDir, "config/optionsof.txt").exists()) {
			Resource ri = new Resource("config.zip", false);
			if (!resources.contains(ri)) {
				resources.add(ri);
			}
		}

		System.out.println(resources);
	}
}
