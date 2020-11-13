package cataclysm.launcher.io;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 16.10.2020 14:04
 *
 * @author Knoblul
 */
public class HttpDownloadRequest {
	private final URL remoteFileURL;
	private final Path localFilePath;

	private boolean localFileAppend;
	private long requestedOffset;
	private long requestedSize;
	private LongConsumer downloadSizeConsumer;
	private LongConsumer downloadProgressConsumer;

	public HttpDownloadRequest(URL remoteFileURL, Path localFilePath) {
		this.remoteFileURL = Objects.requireNonNull(remoteFileURL);
		this.localFilePath = Objects.requireNonNull(localFilePath);
	}

	public URL getRemoteFileURL() {
		return remoteFileURL;
	}

	public Path getLocalFilePath() {
		return localFilePath;
	}

	public boolean isLocalFileAppend() {
		return localFileAppend;
	}

	public void setLocalFileAppend(boolean localFileAppend) {
		this.localFileAppend = localFileAppend;
	}

	public long getRequestedOffset() {
		return requestedOffset;
	}

	public void setRequestedOffset(long requestedOffset) {
		if (requestedOffset < 0) {
			throw new IllegalArgumentException(String.valueOf(requestedSize));
		}

		this.requestedOffset = requestedOffset;
	}

	public long getRequestedSize() {
		return requestedSize;
	}

	public void setRequestedSize(long requestedSize) {
		if (requestedSize < 0) {
			throw new IllegalArgumentException(String.valueOf(requestedSize));
		}

		this.requestedSize = requestedSize;
	}

	public LongConsumer getDownloadSizeConsumer() {
		return downloadSizeConsumer;
	}

	public void setDownloadSizeConsumer(LongConsumer downloadSizeConsumer) {
		this.downloadSizeConsumer = downloadSizeConsumer;
	}

	public LongConsumer getDownloadProgressConsumer() {
		return downloadProgressConsumer;
	}

	public void setDownloadProgressConsumer(LongConsumer downloadProgressConsumer) {
		this.downloadProgressConsumer = downloadProgressConsumer;
	}
}
