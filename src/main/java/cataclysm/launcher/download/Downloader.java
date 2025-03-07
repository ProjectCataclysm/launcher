package cataclysm.launcher.download;

import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.PlatformHelper;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <br><br>launcher
 * <br>Created: 05.03.2025 17:34
 *
 * @author Knoblul
 */
abstract class Downloader {
	protected final Path gameDirPath;
	protected final String clientUrl;
	protected final AtomicLong bpsCounter;
	protected final DownloadingManager.DownloadProgressListener listener;

	protected Map<String, RemoteFile> availableFiles;

	Downloader(Path gameDirPath, String clientUrl, AtomicLong bpsCounter,
	           DownloadingManager.DownloadProgressListener listener) {
		this.gameDirPath = gameDirPath;
		this.clientUrl = clientUrl;
		this.bpsCounter = bpsCounter;
		this.listener = listener;
	}

	private void retrieveBundles() {
		Map<String, RemoteFile> availableFiles = new HashMap<>();

		listener.updateState(DownloadingManager.DownloadState.RETRIEVE_FILES_LIST);
		try (HttpClientWrapper.HttpGetResponse r = HttpClientWrapper.get(clientUrl + "/deploy.json");
		     Reader reader = r.getBody().charStream()) {
			Type type = new TypeToken<Map<String, RemoteFile[]>>(){}.getType();
			Map<String, RemoteFile[]> bundles = HttpClientWrapper.GSON.fromJson(reader, type);

			for (RemoteFile file : Objects.requireNonNull(bundles.get("common"), "No bundle: common")) {
				if (availableFiles.put(file.local, file) != null) {
					throw new IllegalArgumentException("Already presents: " + file.local);
				}
			}

			String platformBundleId = PlatformHelper.getPlatformBundleId();
			for (RemoteFile file : Objects.requireNonNull(bundles.get(platformBundleId), "No bundle: " + platformBundleId)) {
				if (availableFiles.put(file.local, file) != null) {
					throw new IllegalArgumentException("Already presents: " + file.local);
				}
			}

			this.availableFiles = Collections.unmodifiableMap(availableFiles);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to get deploy.json", t);
		}
	}

	abstract CompletableFuture<Void> download0(ExecutorService parallelExecutor);

	final CompletableFuture<Void> download(ExecutorService parallelExecutor) {
		return CompletableFuture.runAsync(this::retrieveBundles, parallelExecutor)
			.thenComposeAsync(__ -> download0(parallelExecutor), parallelExecutor);
	}

	/**
	 * <br><br>launcher
	 * <br>Created: 05.03.2025 0:11
	 *
	 * @author Knoblul
	 */
	static class RemoteFile {
		private final String remote;
		private final String local;
		private final boolean verify;
		private final String checksum;
		private final long timestamp;
		private final long size;

		public RemoteFile(String remote, String local, boolean verify, String checksum, long timestamp, long size) {
			this.remote = remote;
			this.local = local;
			this.verify = verify;
			this.checksum = checksum;
			this.timestamp = timestamp;
			this.size = size;
		}

		public String getRemote() {
			return remote;
		}

		public String getLocal() {
			return local;
		}

		public boolean isVerify() {
			return verify;
		}

		public String getChecksum() {
			return checksum;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public long getSize() {
			return size;
		}
	}
}
