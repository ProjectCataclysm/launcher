package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.download.DownloadingManager;
import cataclysm.launcher.launch.GameLauncher;
import cataclysm.launcher.utils.Log;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 18:26
 *
 * @author Knoblul
 */
public class GameLoadingWindow extends LoadingOverlay implements DownloadingManager.DownloadProgressListener {
	private final LauncherApplication application = LauncherApplication.getInstance();
	private final DownloadingManager downloadingManager = new DownloadingManager(application, this);
	private final GameLauncher gameLauncher = new GameLauncher(application);

	private final Label percentLabel;
	private final Label speedLabel;
	private final DownloadProgressBar fileProgress;

	public GameLoadingWindow() {
		getStyleClass().add("game-loading-overlay");

		fileProgress = new DownloadProgressBar();
		fileProgress.getStyleClass().add("update-progress");

		percentLabel = new Label();
		percentLabel.getStyleClass().addAll("download-percent", "bold-font");

		speedLabel = new Label();
		speedLabel.getStyleClass().addAll("download-speed", "bold-font");
		AnchorPane.setRightAnchor(speedLabel, 1.0);
	}

	@Override
	public void showLoading(String title, long contentLength) {
		updatePane.getChildren().setAll(loadingTitleLabel, loadingProgress);
		super.showLoading(title, contentLength);
	}

	public void startGamePressed() {
		downloadingManager.start();
	}

	@Override
	public void updateDownloadSpeed(long bytesPerSecond) {
		Platform.runLater(() -> speedLabel.setText(DownloadingManager.downloadSpeedString(bytesPerSecond)));
	}

	@Override
	public void updateState(DownloadingManager.DownloadState state) {
		Platform.runLater(() -> {
			switch (state) {
				case RETRIEVE_FILES_LIST:
					showLoading("Получение информации о файлах...", 0);
					break;
				case CHECKING_FILES:
					showLoading("Проверка файлов игры...", 0);
					break;
				case DOWNLOADING_FILES:
					percentLabel.setText("0%");
					speedLabel.setText(DownloadingManager.downloadSpeedString(0L));
					updatePane.getChildren().setAll(loadingTitleLabel, new AnchorPane(percentLabel, speedLabel), fileProgress);
					super.showLoading("Загрузка файлов игры", 0);
					break;
				case FINISHED:
					showLoading("Готово", 0);
					CompletableFuture.runAsync(gameLauncher::startGame).whenComplete((result, cause) -> {
						if (cause != null) {
							Log.err(cause, "Game launcher error");
						}
					});
					break;
				default:
					showLoading("???", 0);
					break;
			}
		});
	}

	@Override
	public void downloadFailed(Throwable cause) {
		Platform.runLater(() -> showError("Failed to download", "Не удалось запустить игру",
			cause, this::startGamePressed));
	}

	@Override
	public void initDownloadSize(long totalBytesPending) {
		Platform.runLater(() -> fileProgress.setContentLength(totalBytesPending));
	}

	@Override
	public void updateDownloadProgress(long totalBytesDownloaded) {
		Platform.runLater(() -> {
			fileProgress.addProgress(totalBytesDownloaded - fileProgress.getCurrentProgress());
			percentLabel.setText(String.format(Locale.ROOT, "%.2f%%", fileProgress.getProgress() * 100D));
		});
	}

//	private Void handleGameFilesInfoRetrieved(AssetInfoContainer result, Throwable cause) {
//		if (cause != null) {
//			application.getOverlays()
//					.getGameLoading()
//					.showError("Failed to retrieve game files info", "Не удалось получить информацию о файлах игры",
//							cause, () -> retrieveGameFilesInfo(2));
//		} else {
//			checkGameFiles(result);
//		}
//
//		return null;
//	}
//
//	private void retrieveGameFilesInfo(int delay) {
//		CompletableFuture
//				.runAsync(() -> showLoading("Получение информации о файлах...", 0), Platform::runLater)
//				.thenApplyAsync(__ -> {
//					try {
//						downloadingManager.setClientBranch(application.getConfig().clientBranch);
//						return downloadingManager.loadAssetContainer();
//					} catch (IOException e) {
//						throw new RuntimeException("Failed to retrieve assets info", e);
//					}
//				}, application.getDelayedExecutor(delay))
//				.handleAsync(this::handleGameFilesInfoRetrieved, Platform::runLater)
//				.whenComplete(Log::logFutureErrors);
//	}
//
//	private void checkGameFiles(AssetInfoContainer assets) {
//		AtomicReference<ExecutorService> executorReference = new AtomicReference<>();
//		CompletableFuture.runAsync(() -> showLoading("Проверка файлов игры...", 0), Platform::runLater)
//				.thenComposeAsync(__ -> {
//					ThreadFactory threadFactory = new ThreadFactory() {
//						private int threadCounter = 0;
//
//						@Override
//						public Thread newThread(@NotNull Runnable r) {
//							Thread t = new Thread(r);
//							t.setName("File Checker " + (threadCounter++));
//							t.setDaemon(true);
//							return t;
//					}};
//					int nThreads = Runtime.getRuntime().availableProcessors();
//					ExecutorService executor = Executors.newFixedThreadPool(nThreads, threadFactory);
//					executorReference.set(executor);
//					return sanitationManager.sanitize(assets, executor, application.getConfig().getCurrentGameDirectoryPath());
//				}, application.getMainExecutor())
//				.whenCompleteAsync((result, cause) -> {
//					ExecutorService exec = executorReference.get();
//					try {
//						exec.shutdown();
//						if (!exec.awaitTermination(10, TimeUnit.SECONDS)) {
//							exec.shutdownNow();
//						}
//					} catch (Throwable ignored) {
//
//					}
//				}, application.getMainExecutor())
//				.handleAsync(this::handleGameFilesCheckedResult, Platform::runLater)
//				.whenComplete(Log::logFutureErrors);
//	}
//
//	private Void handleGameFilesCheckedResult(Set<AssetInfo> result, Throwable cause) {
//		if (cause != null) {
//			showError("Failed to check game files", "Не удалось проверить файлы игры", cause,
//					() -> retrieveGameFilesInfo(2));
//		} else if (!result.isEmpty()) {
//			downloadAssets(result);
//		} else {
//			startGame();
//		}
//
//		return null;
//	}
//
//	private void downloadAssets(Set<AssetInfo> assets) {
//		CompletableFuture.runAsync(() -> {
//					percentLabel.setText("0%");
//					speedLabel.setText(readableSpeed(0));
//					updatePane.getChildren().setAll(loadingTitleLabel, new AnchorPane(percentLabel, speedLabel), fileProgress, loadingProgress);
//					super.showLoading("Загружаем файлы игры...", 0);
//				}, Platform::runLater)
//				.thenRunAsync(() -> downloadingManager.downloadAssets(assets, application.getConfig().getCurrentGameDirectoryPath(),
//						this), application.getMainExecutor())
//				.handleAsync((result, cause) -> handleDownloadResult(assets, cause), Platform::runLater)
//				.whenComplete(Log::logFutureErrors);
//	}
//
//	private Void handleDownloadResult(Set<AssetInfo> ignored, Throwable cause) {
//		if (cause != null) {
//			showError("Failed to download assets", "Не удалось загрузить один или несколько файлов игры",
//					cause, () -> retrieveGameFilesInfo(2));
//		} else {
//			startGame();
//		}
//
//		return null;
//	}

//	private void startGame() {
//		CompletableFuture.runAsync(gameLauncher::startGame).whenComplete((result, cause) -> {
//			if (cause != null) {
//				Log.err(cause, "Game launcher error");
//			}
//		});
//	}

//	@Override
//	public void setDownloadFileCount(int count) {
//		Platform.runLater(() -> {
//			loadingProgress.setContentLength(count);
//			loadingProgress.setProgress(0.0);
//		});
//	}
//
//	@Override
//	public void fileCountProgressAdded(int added) {
//		Platform.runLater(() -> loadingProgress.addProgress(added));
//	}
//
//	@Override
//	public void newDownloadFile(String name, long contentLength) {
//		Platform.runLater(() -> {
//			percentLabel.setText("0%");
//			fileProgress.setContentLength(contentLength);
//			fileProgress.setProgress(0);
//		});
//	}
//
//	@Override
//	public void fileBytesProgressAdded(int added) {
//		Platform.runLater(() -> {
//			fileProgress.addProgress(added);
//			bytesCounter.addAndGet(added);
//			percentLabel.setText((int) (fileProgress.getProgress() * 100) + "%");
//		});
//	}
}
