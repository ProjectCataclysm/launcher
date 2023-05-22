package cataclysm.launcher.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 13:14
 *
 * @author Knoblul
 */
public class AbstractLauncherOverlay extends StackPane {
	protected Transition showHideAnimation;

	public AbstractLauncherOverlay() {
		getStyleClass().add("launcher-overlay");

		Duration duration = Duration.millis(500);
		FadeTransition fade = new FadeTransition(duration.divide(0.5), this);
		fade.setFromValue(0.0);
		fade.setToValue(1.0);
		showHideAnimation = new ParallelTransition(this, fade);
		showHideAnimation.setInterpolator(Interpolator.EASE_BOTH);
	}

	public void show() {
		showHideAnimation.setRate(1);
		showHideAnimation.play();
		showHideAnimation.setOnFinished(event -> {});
		setVisible(true);
	}

	public void hide() {
		showHideAnimation.setRate(-1);
		showHideAnimation.play();
		showHideAnimation.setOnFinished(event -> setVisible(false));
	}
}
