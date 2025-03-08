package cataclysm.launcher.download;

import cataclysm.launcher.utils.HttpClientWrapper;
import okio.BufferedSource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <br><br>launcher
 * <br>Created: 05.03.2025 17:34
 *
 * @author Knoblul
 */
class WebDownloader extends Downloader {
	private final List<RemoteFile> pendingFiles = new ArrayList<>();

	WebDownloader(Path gameDirPath, String clientUrl, AtomicLong bpsCounter,
	              DownloadingManager.DownloadProgressListener listener) {
		super(gameDirPath, clientUrl, bpsCounter, listener);
	}

	private void deleteExtraFiles(Path dirPath) throws IOException {
		Set<Path> foundFiles;
		try (Stream<Path> stream = Files.walk(dirPath, Integer.MAX_VALUE)) {
			foundFiles = stream.filter(subPath -> !Files.isDirectory(subPath)).collect(Collectors.toSet());
		} catch (NoSuchFileException | FileNotFoundException ignored) {
			return;
		}

		for (Path foundFile : foundFiles) {
			Path relFile = gameDirPath.relativize(foundFile);
			RemoteFile rf = availableFiles.get(relFile.toString().replace(File.separatorChar, '/'));
			if (rf == null) {
				Files.delete(foundFile);
			}
		}
	}

	private static long getGameFileFormatTimeStamp(Path filePath) throws IOException {
		String s = filePath.getFileName().toString();
		int extIndex = s.lastIndexOf('.');
		if (extIndex == -1) {
			// no extension
			return 0L;
		}

		// Все проприетарные форматы файлов имеют более-менее одинаковую структуру
		// и Unix-timestamp в самом начале.
		String ext = s.substring(extIndex + 1);
		if (ext.equalsIgnoreCase("rvmp")
			|| ext.equalsIgnoreCase("rvsa")
			|| ext.equalsIgnoreCase("rmap")) {
			try (DataInputStream data = new DataInputStream(Files.newInputStream(filePath))) {
				// sizeof(int Magic + var Version)
				int skipSize = 4 + (ext.equalsIgnoreCase("rmap") ? 1 : 4);
				if (data.skipBytes(skipSize) != skipSize) {
					throw new EOFException(filePath.toString());
				}

				// т.к. DataInputStream читает в Big-Endian кодировке, конвертируем в Little-Endian
				int timestampLittleEndian = Integer.reverseBytes(data.readInt());
				return Integer.toUnsignedLong(timestampLittleEndian);
			}
		}

		return 0;
	}

	private boolean checkModified(Path filePath, RemoteFile rf) throws IOException {
		long timestamp = getGameFileFormatTimeStamp(filePath);
		if (timestamp != 0L) {
			return timestamp != rf.getTimestamp() || rf.getSize() != Files.size(filePath);
		} else if (rf.isVerify()) {
			return !rf.getChecksum().equals(ChecksumUtil.computeChecksum(filePath));
		}

		BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
		return rf.getTimestamp() != attr.lastModifiedTime().to(TimeUnit.SECONDS)
			|| rf.getSize() != Files.size(filePath);
	}

	private CompletableFuture<Void> checkGameFiles(ExecutorService parallelExecutor) {
		listener.updateState(DownloadingManager.DownloadState.CHECKING_FILES);

		try {
			deleteExtraFiles(gameDirPath.resolve("bin"));
			deleteExtraFiles(gameDirPath.resolve("lib"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (Map.Entry<String, RemoteFile> e : availableFiles.entrySet()) {
			Path absPath = gameDirPath.resolve(e.getKey());
			futures.add(CompletableFuture.runAsync(() -> {
				try {
					if (!Files.exists(absPath) || checkModified(absPath, e.getValue())) {
						synchronized (pendingFiles) {
							pendingFiles.add(e.getValue());
						}
					}
				} catch (IOException x) {
					throw new RuntimeException("Failed to verify game file " + absPath, x);
				}
			}, parallelExecutor));
		}
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}

	private long downloadFile(RemoteFile rf, long totalBytesDownloaded) throws IOException {
		Path localFilePath = gameDirPath.resolve(rf.getLocal());
		Files.createDirectories(localFilePath.getParent());
		String url = clientUrl + "/" + rf.getRemote();
		try (HttpClientWrapper.HttpGetResponse r = HttpClientWrapper.get(url);
		     BufferedSource source = r.getBody().source()) {
			try (OutputStream out = Files.newOutputStream(localFilePath)) {
				int len;
				byte[] buffer = new byte[8 * 1024];
				while ((len = source.read(buffer)) != -1) {
					out.write(buffer, 0, len);
					totalBytesDownloaded += len;
					bpsCounter.addAndGet(len);
					listener.updateDownloadProgress(totalBytesDownloaded);
				}
			}
		}

		return totalBytesDownloaded;
	}

	private void downloadGameFiles() {
		listener.updateState(DownloadingManager.DownloadState.DOWNLOADING_FILES);
		listener.initDownloadSize(pendingFiles.stream().mapToLong(RemoteFile::getSize).sum());
		long totalBytesDownloaded = 0L;
		for (RemoteFile rf : pendingFiles) {
			try {
				totalBytesDownloaded = downloadFile(rf, totalBytesDownloaded);
			} catch (IOException e) {
				throw new RuntimeException("Failed to download " + rf.getRemote(), e);
			}
		}
	}

	@Override
	CompletableFuture<Void> download0(ExecutorService parallelExecutor) {
		return checkGameFiles(parallelExecutor).thenRunAsync(this::downloadGameFiles, parallelExecutor);
	}
}
