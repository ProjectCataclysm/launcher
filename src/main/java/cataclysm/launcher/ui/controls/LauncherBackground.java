package cataclysm.launcher.ui.controls;

import com.google.common.collect.Lists;
import javafx.animation.*;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 02.10.2020 18:43
 *
 * @author Knoblul
 */
public class LauncherBackground extends StackPane {
	private final Cover cover1;
	private final Cover cover2;

	private final String imagePathFormat;
	private final int totalFrames;

	private int currentFrameIndex;
	private boolean oddFrame;

	private static class Cover extends StackPane {
		public Cover() {
			super();
		}

		private static void bindDeepOpacityAndVisibility(StackPane clip, Node node) {
			DoubleBinding deepOpacityProperty = node.opacityProperty().multiply(1);
			BooleanBinding deepVisibleProperty = node.visibleProperty().and(node.sceneProperty().isNotNull());

			Node parent = node;
			while ((parent = parent.getParent()) != null) {
				deepOpacityProperty = deepOpacityProperty.multiply(parent.opacityProperty());
				deepVisibleProperty = deepVisibleProperty.and(parent.visibleProperty()).and(parent.sceneProperty().isNotNull());
			}

			clip.opacityProperty().bind(deepOpacityProperty);
			clip.visibleProperty().bind(deepVisibleProperty);
		}

		StackPane newClipBlur(Pane child) {
			StackPane result = new StackPane();
			result.backgroundProperty().bind(backgroundProperty());
			getChildren().add(result);
			bindDeepOpacityAndVisibility(result, child);
			return result;
		}
	}

	public LauncherBackground(String imagePathFormat, int totalFrames) {
		getChildren().add(cover1 = new Cover());
		getChildren().add(cover2 = new Cover());

		this.totalFrames = totalFrames;
		this.imagePathFormat = imagePathFormat;

		currentFrameIndex = new Random().nextInt(totalFrames) + 1;
		showNextSlide(null);

		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), this::showNextSlide));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	private void showNextSlide(ActionEvent actionEvent) {
		currentFrameIndex = (currentFrameIndex + 1) % totalFrames;
		oddFrame = !oddFrame;

		Region managed = oddFrame ? cover1 : cover2;
		Image backgroundImage = new Image(String.format(imagePathFormat, currentFrameIndex + 1));

		BackgroundImage bi = new BackgroundImage(backgroundImage,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(BackgroundSize.AUTO,
						BackgroundSize.AUTO,
						true,
						true,
						false,
						true));

		managed.setBackground(new javafx.scene.layout.Background(bi));

		FadeTransition t1 = new FadeTransition(Duration.millis(500), cover1);
		t1.setToValue(oddFrame ? 1 : 0);

		FadeTransition t2 = new FadeTransition(Duration.millis(500), cover2);
		t2.setToValue(!oddFrame ? 1 : 0);

		ParallelTransition pt = new ParallelTransition(t1, t2);
		pt.playFromStart();
	}

	private final List<Supplier<Pane>> blurPaneSuppliers = Lists.newArrayList();

	public void buildBlurEffects() {
		for (Supplier<Pane> blurPaneSupplier : blurPaneSuppliers) {
			Pane child = blurPaneSupplier.get();

			GaussianBlur blur = new GaussianBlur(10);

			StackPane cover1Clip = cover1.newClipBlur(child);
			StackPane cover2Clip = cover2.newClipBlur(child);

			Rectangle clip1 = new Rectangle();
			Rectangle clip2 = new Rectangle();

			child.boundsInParentProperty().addListener((observable, oldValue, newValue) -> {
				Bounds bounds = cover1.sceneToLocal(child.localToScene(child.parentToLocal(newValue)));

				double x = bounds.getMinX();
				double y = bounds.getMinY();
				double w = bounds.getWidth();
				double h = bounds.getHeight();
				clip1.setX(x);
				clip2.setX(x);
				clip1.setY(y);
				clip2.setY(y);
				clip1.setWidth(w);
				clip2.setWidth(w);
				clip1.setHeight(h);
				clip2.setHeight(h);
			});

			cover1Clip.setEffect(blur);
			cover1Clip.setClip(clip2);

			cover2Clip.setEffect(blur);
			cover2Clip.setClip(clip1);
		}

		blurPaneSuppliers.clear();
	}

	public void createBlurEffect(Supplier<Pane> childSupplier) {
		blurPaneSuppliers.add(childSupplier);
	}
}
