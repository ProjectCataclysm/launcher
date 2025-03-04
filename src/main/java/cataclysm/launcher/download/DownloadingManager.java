package cataclysm.launcher.download;

import cataclysm.launcher.assets.AssetInfo;
import cataclysm.launcher.assets.AssetInfoContainer;
import cataclysm.launcher.utils.*;
import okio.BufferedSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 16 окт. 2018 г. / 21:32:13
 *
 * @author Knoblul
 */
public class DownloadingManager {
	private LauncherConfig.ClientBranch clientBranch = LauncherConfig.ClientBranch.PRODUCTION;

	public LauncherConfig.ClientBranch getClientBranch() {
		return clientBranch;
	}

	public void setClientBranch(LauncherConfig.ClientBranch clientBranch) {
		this.clientBranch = clientBranch;
	}

	private String getCurrentClientUrl() {
		String url = "https://" + LauncherConstants.CLIENT_URL;
		if (clientBranch.getSubDirName() != null) {
			url = url + "/" + clientBranch.getSubDirName();
		}
		return url;
	}

	@SuppressWarnings("unchecked")
	public AssetInfoContainer loadAssetContainer() throws IOException {
		JSONObject root;
		String url = getCurrentClientUrl() + "/deploy.json";
		try (HttpClientWrapper.HttpResponse response = HttpClientWrapper.get(url);
		     Reader reader = response.getBody().charStream()) {
			try {
				root = (JSONObject) new JSONParser().parse(reader);
			} catch (ParseException e) {
				throw new IOException("Failed to parse response json", e);
			}
		}

		Set<String> protectedFolders = new HashSet<>();
		((JSONArray) root.get("protected_folders")).forEach(t -> protectedFolders.add((String) t));

		Map<String, String> hashes = new HashMap<>();
		Set<AssetInfo> assets = new HashSet<>();
		((JSONArray) root.get("resources_default"))
				.forEach(t -> assets.add(parseAsset((JSONObject) t, hashes)));
		((JSONArray) root.get("resources_" + PlatformHelper.getPlatformIdentifier()))
				.forEach(t -> assets.add(parseAsset((JSONObject) t, hashes)));

		return new AssetInfoContainer(Collections.unmodifiableSet(protectedFolders),
				Collections.unmodifiableSet(assets), Collections.unmodifiableMap(hashes));
	}

	private static AssetInfo parseAsset(JSONObject node, Map<String, String> hashes) {
		String local = (String) node.get("local");
		String remote = (String) node.get("remote");
		Object hash = node.get("hash");

		if (hash != null) {
			if (hash instanceof JSONObject) {
				@SuppressWarnings("unchecked")
				Set<Map.Entry<String, Object>> hashEntries = ((JSONObject) hash).entrySet();
				for (Map.Entry<String, Object> e : hashEntries) {
					hashes.put(local + "/" + e.getKey(), (String) e.getValue());
				}
			} else if (hash instanceof String) {
				hashes.put(local, (String) hash);
			} else {
				throw new IllegalArgumentException("Invalid hash type " + hash + " in asset " + remote);
			}
		}

		return new AssetInfo(local, remote, (Boolean) node.get("archive"), hash != null);
	}

	private void downloadFile(Path localFilePath, AssetInfo asset, DownloadProgressListener listener) throws IOException {
		Files.createDirectories(localFilePath.getParent());
		String url = getCurrentClientUrl() + "/" + asset.getRemote();
		try (HttpClientWrapper.HttpResponse responseBody = HttpClientWrapper.get(url);
		     BufferedSource source = responseBody.getBody().source()) {
			listener.newDownloadFile("Загрузка " + asset.getRemote(), responseBody.getBody().contentLength());
			try (OutputStream out = Files.newOutputStream(localFilePath)) {
				int len;
				byte[] buffer = new byte[8 * 1024];
				while ((len = source.read(buffer)) != -1) {
					out.write(buffer, 0, len);
					listener.fileBytesProgressAdded(len);
				}
			}
		}
	}

	private void downloadAndUnpackArchive(Path rootPath, AssetInfo asset, DownloadProgressListener listener) throws IOException {
		Path zipPath = rootPath.resolve(asset.getRemote());
		downloadFile(zipPath, asset, listener);

		Path unpackPath = rootPath.resolve(asset.getLocal());

		try {
			int entryCount = 0;
			try (ZipFile zip = new ZipFile(zipPath.toFile())) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (!entry.isDirectory()) {
						entryCount++;
					}
				}
			}

			listener.newDownloadFile("Распаковка " + asset.getRemote(), entryCount);

			try (ZipFile zip = new ZipFile(zipPath.toFile())) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (!entry.isDirectory()) {
						Path outputPath = unpackPath.resolve(entry.getName());
						Files.createDirectories(outputPath.getParent());
						try (InputStream in = zip.getInputStream(entry)) {
							Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
			}
		} catch (Throwable e) {
			throw new IOException("Could not unpack " + zipPath, e);
		}

		try {
			Files.deleteIfExists(zipPath);
		} catch (IOException e) {
			Log.err(e, "Failed to delete temporary file " + zipPath);
		}
	}

	private void downloadAsset(Path rootPath, AssetInfo asset, DownloadProgressListener listener) {
		try {
			if (asset.isArchive()) {
				downloadAndUnpackArchive(rootPath, asset, listener);
			} else {
				downloadFile(rootPath.resolve(asset.getLocal()), asset, listener);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to download " + asset.getRemote(), e);
		}
	}

	public void downloadAssets(Set<AssetInfo> assets, Path rootPath, DownloadProgressListener listener) {
		listener.setDownloadFileCount(assets.size());
		for (AssetInfo asset : assets) {
			downloadAsset(rootPath, asset, listener);
			listener.fileCountProgressAdded(1);
		}
	}

	public interface DownloadProgressListener {
		void setDownloadFileCount(int count);

		void fileCountProgressAdded(int added);

		void newDownloadFile(String name, long contentLength);

		void fileBytesProgressAdded(int added);
	}
}
