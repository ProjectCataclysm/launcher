package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.assets.AssetInfo;
import cataclysm.launcher.assets.AssetInfoContainer;
import cataclysm.launcher.download.DownloadingManager;
import cataclysm.launcher.download.santation.SanitationManager;
import cataclysm.launcher.launch.GameLauncher;
import cataclysm.launcher.utils.Log;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 18:26
 *
 * @author Knoblul
 */
public class GameLoadingWindow extends LoadingOverlay implements DownloadingManager.DownloadProgressListener {
	private static final String[] STORAGE_UNITS = new String[] { "Б", "КБ", "МБ", "ГБ", "ТБ" };
	private static final int DL_SPEED_MEASURE_PERIOD = 500;

	//Range: bytes=3-
	private final LauncherApplication application = LauncherApplication.getInstance();
	private final SanitationManager sanitationManager = new SanitationManager();
	private final DownloadingManager downloadingManager = new DownloadingManager();
	private final GameLauncher gameLauncher = new GameLauncher(application);

	private final Label percentLabel;
	private final Label speedLabel;
	private final DownloadProgressBar fileProgress;
	private final AtomicInteger bytesCounter = new AtomicInteger();

	public GameLoadingWindow() {
		getStyleClass().add("game-loading-overlay");

		fileProgress = new DownloadProgressBar();
		fileProgress.getStyleClass().add("update-progress");

		percentLabel = new Label();
		percentLabel.getStyleClass().addAll("download-percent", "bold-font");

		speedLabel = new Label();
		speedLabel.getStyleClass().addAll("download-speed", "bold-font");
		AnchorPane.setRightAnchor(speedLabel, 1.0);

		LauncherApplication.getInstance().getParallelExecutor()
				.scheduleAtFixedRate(this::updateDownloadSpeed, 0, DL_SPEED_MEASURE_PERIOD, TimeUnit.MILLISECONDS);
	}

	private void updateDownloadSpeed() {
		Platform.runLater(() -> speedLabel.setText(readableSpeed(bytesCounter.getAndSet(0))));
	}

	private static String readableSpeed(int size) {
		if (size <= 0) {
			return "0 Б/сек";
		}

		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#")
				.format(size / Math.pow(1024, digitGroups)) + " " + STORAGE_UNITS[digitGroups] + "/сек";
	}

	@Override
	public void showLoading(String title, long contentLength) {
		updatePane.getChildren().setAll(loadingTitleLabel, loadingProgress);
		super.showLoading(title, contentLength);
	}

	public void startGamePressed() {
		retrieveGameFilesInfo(0);
	}

	private Void handleGameFilesInfoRetrieved(AssetInfoContainer result, Throwable cause) {
		if (cause != null) {
			application.getOverlays()
					.getGameLoading()
					.showError("Failed to retrieve game files info", "Не удалось получить информацию о файлах игры",
							cause, () -> retrieveGameFilesInfo(2));
		} else {
			checkGameFiles(result);
		}

		return null;
	}

	private void retrieveGameFilesInfo(int delay) {
		CompletableFuture
				.runAsync(() -> showLoading("Получение информации о файлах...", 0), Platform::runLater)
				.thenApplyAsync(__ -> {
					try {
						downloadingManager.setClientBranch(application.getConfig().clientBranch);
						return downloadingManager.loadAssetContainer();
					} catch (IOException e) {
						throw new RuntimeException("Failed to retrieve assets info", e);
					}
				}, application.getDelayedExecutor(delay))
				.handleAsync(this::handleGameFilesInfoRetrieved, Platform::runLater)
				.whenComplete(Log::logFutureErrors);
	}

	private void checkGameFiles(AssetInfoContainer assets) {
		AtomicReference<ExecutorService> executorReference = new AtomicReference<>();
		CompletableFuture.runAsync(() -> showLoading("Проверка файлов игры...", 0), Platform::runLater)
				.thenComposeAsync(__ -> {
					ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("File Checker #%d")
							.setDaemon(true)
							.build();
					int nThreads = Runtime.getRuntime().availableProcessors();
					ExecutorService executor = Executors.newFixedThreadPool(nThreads, threadFactory);
					executorReference.set(executor);
					return sanitationManager.sanitize(assets, executor, application.getConfig().getCurrentGameDirectoryPath());
				}, application.getMainExecutor())
				.whenCompleteAsync((result, cause) -> {
					//noinspection UnstableApiUsage
					MoreExecutors.shutdownAndAwaitTermination(executorReference.get(),
							Duration.of(10, ChronoUnit.SECONDS));
				}, application.getMainExecutor())
				.handleAsync(this::handleGameFilesCheckedResult, Platform::runLater)
				.whenComplete(Log::logFutureErrors);
	}

	private Void handleGameFilesCheckedResult(Set<AssetInfo> result, Throwable cause) {
		if (cause != null) {
			showError("Failed to check game files", "Не удалось проверить файлы игры", cause,
					() -> retrieveGameFilesInfo(2));
		} else if (!result.isEmpty()) {
			downloadAssets(result);
		} else {
			startGame();
		}

		return null;
	}

	private void downloadAssets(Set<AssetInfo> assets) {
		CompletableFuture.runAsync(() -> {
					percentLabel.setText("0%");
					speedLabel.setText(readableSpeed(0));
					updatePane.getChildren().setAll(loadingTitleLabel, new AnchorPane(percentLabel, speedLabel), fileProgress, loadingProgress);
					super.showLoading("Загружаем файлы игры...", 0);
				}, Platform::runLater)
				.thenRunAsync(() -> downloadingManager.downloadAssets(assets, application.getConfig().getCurrentGameDirectoryPath(),
						this), application.getMainExecutor())
				.handleAsync((result, cause) -> handleDownloadResult(assets, cause), Platform::runLater)
				.whenComplete(Log::logFutureErrors);
	}

	private Void handleDownloadResult(Set<AssetInfo> ignored, Throwable cause) {
		if (cause != null) {
			showError("Failed to download assets", "Не удалось загрузить один или несколько файлов игры",
					cause, () -> retrieveGameFilesInfo(2));
		} else {
			startGame();
		}

		return null;
	}

	private void startGame() {
		CompletableFuture.runAsync(gameLauncher::startGame).whenComplete((result, cause) -> {
			if (cause != null) {
				Log.err(cause, "Game launcher error");
			}
		});
	}

	@Override
	public void setDownloadFileCount(int count) {
		Platform.runLater(() -> {
			loadingProgress.setContentLength(count);
			loadingProgress.setProgress(0.0);
		});
	}

	@Override
	public void fileCountProgressAdded(int added) {
		Platform.runLater(() -> loadingProgress.addProgress(added));
	}

	@Override
	public void newDownloadFile(String name, long contentLength) {
		Platform.runLater(() -> {
			percentLabel.setText("0%");
			fileProgress.setContentLength(contentLength);
			fileProgress.setProgress(0);
		});
	}

	@Override
	public void fileBytesProgressAdded(int added) {
		Platform.runLater(() -> {
			fileProgress.addProgress(added);
			bytesCounter.addAndGet(added);
			percentLabel.setText((int) (fileProgress.getProgress() * 100) + "%");
		});
	}
}
