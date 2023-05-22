package cataclysm.launcher.ui;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 06.08.2022 20:09
 *
 * @author Knoblul
 */
public class DownloadProgressBar extends ProgressBar {
	private long contentLength;
	private long currentProgress;

	public DownloadProgressBar() {
		getStyleClass().add("download-progress");
		setMaxWidth(Double.MAX_VALUE);
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
		currentProgress = 0;
		// фиксит баг https://bugs.openjdk.org/browse/JDK-8207837
		setProgress(0);
		setProgress(INDETERMINATE_PROGRESS);
	}

	public void addProgress(long value) {
		currentProgress += value;
		currentProgress = Math.min(currentProgress, contentLength);

		if (contentLength > 0 && currentProgress >= 0) {
			setProgress((double) currentProgress / contentLength);
		} else {
			setProgress(0);
		}
	}
}
