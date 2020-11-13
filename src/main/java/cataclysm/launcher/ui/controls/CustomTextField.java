package cataclysm.launcher.ui.controls;

import com.google.common.base.Strings;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;


/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 08.10.2020 8:21
 *
 * @author Knoblul
 */
public class CustomTextField extends TextField {
	private final PseudoClass activePseudoClass;
	private final Label label;
	private final FadeTransition emptyFadeAnimation;
	private final ParallelTransition labelAnimation;
	private boolean labelVisible;

	public CustomTextField(String text) {
		super(text);
		activePseudoClass = PseudoClass.getPseudoClass("active");
		label = new Label();
		label.getStyleClass().add("placeholder-label");
		label.textProperty().bind(promptTextProperty());
		label.setOpacity(0);
		getChildren().add(label);

		Duration duration = Duration.seconds(0.3);

		TranslateTransition labelTranslate = new TranslateTransition(duration, label);
		labelTranslate.setFromY(0);
		labelTranslate.toYProperty().bind(label.heightProperty().add(Bindings.createDoubleBinding(() -> getInsets().getTop(), insetsProperty())).negate());
		labelTranslate.setInterpolator(Interpolator.EASE_BOTH);

		FadeTransition labelFade = new FadeTransition(duration, label);
		labelFade.setFromValue(0);
		labelFade.setToValue(1);
		labelFade.setInterpolator(Interpolator.EASE_BOTH);

		labelAnimation = new ParallelTransition(labelFade, labelTranslate);
		labelAnimation.setInterpolator(Interpolator.EASE_BOTH);

		emptyFadeAnimation = new FadeTransition(duration, this);
		emptyFadeAnimation.setFromValue(0.45);
		emptyFadeAnimation.setToValue(1);
		emptyFadeAnimation.setInterpolator(Interpolator.EASE_BOTH);

		setOpacity(0.45);

		textProperty().addListener((observable, oldValue, newValue) -> setLabelVisible(!Strings.isNullOrEmpty(newValue)));
	}

	public CustomTextField() {
		this(null);
	}

	private void setLabelVisible(boolean visible) {
		if (labelVisible != visible) {
			labelVisible = visible;
			labelAnimation.setRate(visible ? 1 : -1);
			labelAnimation.play();
			emptyFadeAnimation.setRate(visible ? 1 : -1);
			emptyFadeAnimation.play();
			label.pseudoClassStateChanged(activePseudoClass, visible);
		}
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		Insets insets = getInsets();
		double w = getWidth() - insets.getLeft() - insets.getRight();
		double h = label.prefHeight(w);
		double x = insets.getLeft();
		double y = insets.getTop();
		label.resizeRelocate(x, y, w, h);
	}
}
