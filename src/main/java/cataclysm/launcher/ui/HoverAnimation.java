package cataclysm.launcher.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 17:46
 *
 * @author Knoblul
 */
public class HoverAnimation {
	public static void install(Node node) {
		FadeTransition fade = new FadeTransition(Duration.millis(50));
		fade.setFromValue(1);
		fade.setToValue(0.75);

		ScaleTransition scale = new ScaleTransition(Duration.millis(50));
		scale.setFromX(1);
		scale.setFromY(1);
		scale.setFromZ(1);
		scale.setToX(1.05);
		scale.setToY(1.05);
		scale.setToZ(1.05);

		ParallelTransition transition = new ParallelTransition(node, scale, fade);
		transition.setInterpolator(Interpolator.EASE_BOTH);
		node.setOnMouseEntered(e -> {
			transition.setRate(1);
			transition.play();
		});
		node.setOnMouseExited(e -> {
			transition.setRate(-1);
			transition.play();
		});
	}
}
