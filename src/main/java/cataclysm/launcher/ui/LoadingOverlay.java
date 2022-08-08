package cataclysm.launcher.ui;

import cataclysm.launcher.utils.LauncherConstants;
import cataclysm.launcher.utils.Log;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.08.2022 10:04
 *
 * @author Knoblul
 */
public class LoadingOverlay extends AbstractLauncherOverlay {
	protected final DownloadProgressBar loadingProgress;
	protected final Label loadingTitleLabel;

	protected final VBox updatePane;
	protected final VBox errorPane;

	public LoadingOverlay() {
		getStyleClass().add("launcher-update-dialog");

		loadingTitleLabel = new Label();
		loadingTitleLabel.getStyleClass().addAll("bold-font");
		loadingTitleLabel.setMaxWidth(Double.MAX_VALUE);
		loadingTitleLabel.setAlignment(Pos.CENTER);

		loadingProgress = new DownloadProgressBar();
		loadingProgress.getStyleClass().add("update-progress");
		updatePane = new VBox(loadingTitleLabel, loadingProgress);
		updatePane.setAlignment(Pos.BOTTOM_CENTER);
		updatePane.setVisible(false);

		errorPane = new VBox();
		errorPane.setAlignment(Pos.BOTTOM_CENTER);
		errorPane.setVisible(false);
		errorPane.getStyleClass().add("error-message");
		errorPane.setAlignment(Pos.CENTER);

		BorderPane bp = new BorderPane();
		bp.setBottom(new StackPane(updatePane, errorPane));
		getChildren().addAll(bp);

		showHideAnimation = setupAnimation(showHideAnimation, bp);
	}

	private Transition setupAnimation(Transition currentAnimation, BorderPane bp) {
		TranslateTransition transition = new TranslateTransition(Duration.millis(500), bp);
		transition.fromYProperty()
				.bind(bp.layoutYProperty()
						.add(Bindings.createDoubleBinding(() -> bp.layoutBoundsProperty().getValue().getHeight() * 0.3,
								bp.layoutBoundsProperty())));
		transition.setToY(0);
		ParallelTransition result = new ParallelTransition(this, currentAnimation, transition);
		result.setInterpolator(Interpolator.EASE_BOTH);
		return result;
	}

	public DownloadProgressBar getLoadingProgress() {
		return loadingProgress;
	}

	public void setTitle(String title) {
		Platform.runLater(() -> loadingTitleLabel.setText(title));
	}

	public void showLoading(String title, long contentLength) {
		loadingTitleLabel.setText(title);
		updatePane.setVisible(true);
		errorPane.setVisible(false);
		if (!isVisible()) {
			show();
		}

		loadingProgress.setContentLength(contentLength);
	}

	public void showError(String text, @Nullable Runnable onRetry, @Nullable Runnable onMoreInfo) {
		Label errorText = new Label(text);
		errorText.setWrapText(true);
		errorText.getStyleClass().add("error-text");

		HBox buttons = new HBox();
		buttons.getStyleClass().add("controls");

		if (onRetry != null) {
			Button retryButton = new Button("Повторить");
			retryButton.setOnAction(__ -> onRetry.run());
			retryButton.getStyleClass().add("retry-button");
			buttons.getChildren().add(retryButton);
		}

		if (onMoreInfo != null) {
			Button moreButton = new Button("Подробнее...");
			moreButton.setOnAction(__ -> onMoreInfo.run());
			moreButton.getStyleClass().add("more-button");
			buttons.getChildren().add(moreButton);
		}

		if (buttons.getChildren().isEmpty()) {
			errorPane.getChildren().setAll(errorText);
		} else {
			errorPane.getChildren().setAll(errorText, buttons);
		}

		errorPane.setVisible(true);
		updatePane.setVisible(false);
		if (!isVisible()) {
			show();
		}
	}

	public void showError(@Nullable String logMessage, String errorPrefix, Throwable cause,
	                      @Nullable Runnable onRetry) {
		Throwable realCause = cause;
		if (cause instanceof CompletionException && realCause.getCause() != null) {
			realCause = cause.getCause();
		}

		if (LauncherConstants.isUnreachableException(realCause)) {
			showError(errorPrefix
							+ ": сервис временно недоступен. Проверьте подключение к интернету и повторите попытку позднее.",
					onRetry, null);
		} else {
			Throwable finalRealCause = realCause;
			showError(errorPrefix + ": неизвестная ошибка. Нажмите \"подробнее\", чтобы уточнить детали.", onRetry,
					() -> DialogUtils.showError(errorPrefix, finalRealCause, false));
		}

		if (logMessage != null) {
			Log.err(realCause, logMessage);
		}
	}
}