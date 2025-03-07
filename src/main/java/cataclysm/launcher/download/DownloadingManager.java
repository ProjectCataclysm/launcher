package cataclysm.launcher.download;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.LauncherConstants;
import cataclysm.launcher.utils.Log;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 16 окт. 2018 г. / 21:32:13
 *
 * @author Knoblul
 */
public class DownloadingManager {
	private static final String[] STORAGE_UNITS = new String[] { "Б", "КБ", "МБ", "ГБ", "ТБ" };
	private static final Duration DL_SPEED_MEASURE_PERIOD = Duration.ofMillis(500);

	//Range: bytes=3-
	private final LauncherApplication application;
	private final DownloadProgressListener listener;

	private final AtomicLong bpsCounter = new AtomicLong();

	public DownloadingManager(LauncherApplication application, DownloadProgressListener listener) {
		this.application = application;
		this.listener = listener;

		LauncherApplication.getInstance().getMainExecutor().scheduleAtFixedRate(() -> {
			try {
				listener.updateDownloadSpeed(bpsCounter.getAndSet(0));
			} catch (Throwable t) {
				Log.err(t, "");
			}
		}, 0, DL_SPEED_MEASURE_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
	}

	private String getCurrentClientUrl() {
		LauncherConfig.ClientBranch clientBranch = application.getConfig().clientBranch;
		return "https://" + LauncherConstants.CLIENT_URL + "/" + clientBranch.getName();
	}

	public void start() {
		Path gameDirPath = LauncherApplication.getInstance().getConfig().getCurrentGameDirectoryPath();
		Downloader downloader = new WebDownloader(gameDirPath, getCurrentClientUrl(), bpsCounter, listener);
		ScheduledExecutorService executor = LauncherApplication.getInstance().getParallelExecutor();
		downloader.download(executor).whenComplete((result, cause) -> {
			if (cause != null) {
				listener.downloadFailed(cause);
			} else {
				listener.updateState(DownloadState.FINISHED);
			}
		});
	}

	public static String downloadSpeedString(long bps) {
		if (bps <= 0) {
			return "0 Б/сек";
		}

		int digitGroups = Math.min((int) (Math.log10(bps) / Math.log10(1024)), STORAGE_UNITS.length-1);
		return new DecimalFormat("#,##0.#")
			.format(bps / Math.pow(1024, digitGroups)) + " " + STORAGE_UNITS[digitGroups] + "/сек";
	}

	public enum DownloadState {
		RETRIEVE_FILES_LIST,
		CHECKING_FILES,
		DOWNLOADING_FILES,
		FINISHED,
	}

	public interface DownloadProgressListener {
		void initDownloadSize(long totalBytesPending);

		void updateDownloadProgress(long totalBytesDownloaded);

		void updateDownloadSpeed(long bytesPerSecond);

		void updateState(DownloadState state);

		void downloadFailed(Throwable cause);
	}
}
