package cataclysm.launcher.ui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 13:19
 *
 * @author Knoblul
 */
public class LauncherOverlayManager extends StackPane  {
	private final LoadingOverlay loading = add(new LoadingOverlay());
	private final LoginOverlay login = add(new LoginOverlay());
	private final GameLoadingWindow gameLoading = add(new GameLoadingWindow());
	private boolean currentlyBlurred;

	public LauncherOverlayManager(Parent mainForm) {
		getStyleClass().add("launcher-overlay-manager");

		setVisible(false);
		setPickOnBounds(false);
		Duration duration = Duration.millis(500);
		GaussianBlur blur = new GaussianBlur(0);
		Timeline timeline = new Timeline();
		timeline.getKeyFrames().addAll(
				new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
				new KeyFrame(duration, new KeyValue(blur.radiusProperty(), 20))
		);
		mainForm.setEffect(blur);

		FadeTransition fade = new FadeTransition(duration, this);
		fade.setInterpolator(Interpolator.EASE_BOTH);
		fade.setFromValue(0.0);
		fade.setToValue(1.0);

		ParallelTransition transition = new ParallelTransition(timeline, fade);

		for (Node child : getChildren()) {
			if (child instanceof AbstractLauncherOverlay) {
				child.visibleProperty()
						.addListener(event -> updateBlurVisibility((AbstractLauncherOverlay) child, transition));
			}
		}
	}

	public void hideAll() {
		for (Node child : getChildren()) {
			if (child instanceof AbstractLauncherOverlay) {
				((AbstractLauncherOverlay) child).hide();
				child.setVisible(false);
			}
		}
	}

	private void updateBlurVisibility(AbstractLauncherOverlay overlay, Animation timeline) {
		boolean visible = false;
		if (overlay.isVisible()) {
			visible = true;

			for (Node child : getChildren()) {
				if (child instanceof AbstractLauncherOverlay && child != overlay) {
					((AbstractLauncherOverlay) child).hide();
				}
			}
		} else {
			for (Node child : getChildren()) {
				if (child instanceof AbstractLauncherOverlay) {
					if (child.isVisible()) {
						visible = true;
						break;
					}
				}
			}
		}

		if (currentlyBlurred != visible) {
			timeline.stop();
			timeline.setOnFinished(null);
			if (visible) {
				setVisible(true);
			} else {
				timeline.setOnFinished(event -> setVisible(false));
			}

 			timeline.setRate(visible ? 1 : -1);
			timeline.play();
			currentlyBlurred = visible;
		}
	}

	public <T extends AbstractLauncherOverlay> T add(T overlay) {
		overlay.setVisible(false);
		getChildren().add(overlay);
		return overlay;
	}

	public LoadingOverlay getLoading() {
		return loading;
	}

	public LoginOverlay getLogin() {
		return login;
	}

	public GameLoadingWindow getGameLoading() {
		return gameLoading;
	}
}
