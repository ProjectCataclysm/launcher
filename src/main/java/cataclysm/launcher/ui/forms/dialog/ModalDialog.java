package cataclysm.launcher.ui.forms.dialog;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.WindowHeader;
import cataclysm.launcher.ui.controls.LargeButton;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 10.10.2020 7:07
 *
 * @author Knoblul
 */
public class ModalDialog extends BorderPane {
	public static final int YES_OPTION = 0;
	public static final int NO_OPTION = 1;
	public static final int CLOSE_OPTION = 2;

	private final Stage stage;

	private final boolean infoDialog;
	private VBox messageContainer;

	private ParallelTransition appearAnimation;

	private int selectedOption = CLOSE_OPTION;

	public ModalDialog(Launcher launcher, boolean infoDialog) {
		this.infoDialog = infoDialog;

		stage = new Stage();
		stage.initOwner(launcher.getPrimaryStage());

		fill();

		StackPane shadowWrapper = new StackPane(this);
		shadowWrapper.setMaxSize(600, USE_PREF_SIZE);
		shadowWrapper.getStyleClass().add("modal-shadow");

		StackPane stageWrapper = new StackPane(shadowWrapper);
		stageWrapper.getStyleClass().add("modal-stage");
		stageWrapper.setOpacity(0);

		Scene scene = new Scene(stageWrapper);
		scene.setFill(Color.TRANSPARENT);
		stage.setScene(scene);
		stage.initStyle(StageStyle.TRANSPARENT);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setOnCloseRequest(event -> close());

		Stage primaryStage = launcher.getPrimaryStage();

		ChangeListener<Number> stageListener = (observable, oldValue, newValue) -> {
			stage.setX(primaryStage.getX());
			stage.setY(primaryStage.getY());
			stage.setWidth(primaryStage.getWidth());
			stage.setHeight(primaryStage.getHeight());
		};

		stageListener.changed(null, null, null);

		primaryStage.xProperty().addListener(stageListener);
		primaryStage.yProperty().addListener(stageListener);
		primaryStage.widthProperty().addListener(stageListener);
		primaryStage.heightProperty().addListener(stageListener);
		stage.initOwner(primaryStage);
		primaryStage.getScene().getStylesheets().addListener((ListChangeListener<String>) change ->
				stage.getScene().getStylesheets().setAll(primaryStage.getScene().getStylesheets()));
		stage.getScene().getStylesheets().setAll(primaryStage.getScene().getStylesheets());

		createAnimation();
	}

	private void createAnimation() {
		Duration duration = Duration.seconds(0.3);

		FadeTransition dialogFade = new FadeTransition(duration, stage.getScene().getRoot());
		dialogFade.setFromValue(0);
		dialogFade.setToValue(1);
		dialogFade.setInterpolator(Interpolator.EASE_BOTH);

		ScaleTransition dialogScale = new ScaleTransition(duration, this);
		dialogScale.setFromX(1.25);
		dialogScale.setFromY(1.25);
		dialogScale.setToX(1);
		dialogScale.setToY(1);
		dialogScale.setInterpolator(Interpolator.EASE_BOTH);

		appearAnimation = new ParallelTransition(dialogFade, dialogScale);
		appearAnimation.setInterpolator(Interpolator.EASE_BOTH);
	}

	private void fill() {
		setTop(new WindowHeader(stage, false));
		getStyleClass().add("modal-dialog");

		messageContainer = new VBox();
		messageContainer.getStyleClass().addAll("padded-pane", "message-wrapper");
		messageContainer.setMaxHeight(USE_PREF_SIZE);
		setCenter(messageContainer);

		HBox bottom = new HBox();
		bottom.getStyleClass().addAll("modal-options", "padded-pane");

		if (infoDialog) {
			LargeButton okButton = new LargeButton("Ок");
			okButton.setOnAction(event -> {
				selectedOption = YES_OPTION;
				close();
			});
			bottom.getChildren().add(okButton);
		} else {
			LargeButton yesButton = new LargeButton("Да");
			yesButton.setOnAction(event -> {
				selectedOption = YES_OPTION;
				close();
			});
			bottom.getChildren().add(yesButton);

			LargeButton noButton = new LargeButton("Нет");
			noButton.setOnAction(event -> {
				selectedOption = NO_OPTION;
				close();
			});
			noButton.getStyleClass().add("no");
			bottom.getChildren().add(noButton);
		}

		setBottom(bottom);
	}

	public void setTitle(String title) {
		stage.setTitle(title);
	}

	public void setMessage(String message) {
		Label label = new Label(message);
		label.getStyleClass().add("message");
		messageContainer.getChildren().setAll(label);
	}

	public void setMessage(Node message) {
		messageContainer.getChildren().setAll(message);
	}

	public void close() {
		appearAnimation.setRate(-1);
		appearAnimation.play();
		appearAnimation.setOnFinished(event -> stage.close());
	}

	public int show() {
		selectedOption = CLOSE_OPTION;
		appearAnimation.setRate(1);
		appearAnimation.play();
		stage.showAndWait();
		return selectedOption;
	}
}
