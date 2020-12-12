package cataclysm.launcher.ui.forms.launch;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.ui.forms.dialog.ModalDialog;
import cataclysm.launcher.ui.forms.launch.tasks.LaunchTaskChain;
import cataclysm.launcher.ui.forms.launch.tasks.TaskProgress;
import cataclysm.launcher.ui.forms.main.game.StatusPane;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.concurrent.CompletableFuture;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 05.12.2020 16:46
 *
 * @author Knoblul
 */
public class LaunchForm extends BorderPane {
	private final StatusPane statusPane;
	private final ProgressBar progressBar;

	private CompletableFuture<Void> launchFuture;
	private final IconButton pauseResumeButton;

	public LaunchForm(Launcher launcher, StatusPane statusPane) {
		this.statusPane = statusPane;
		getStyleClass().addAll("padded-pane", "downloading-pane");
		setCenter(new Label("IF U READ DIS VI VON ZULUL"));

		progressBar = new ProgressBar(0);
		progressBar.setProgress(0.3F);
		progressBar.setPrefWidth(300);

		pauseResumeButton = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.PAUSE));
		pauseResumeButton.setOnAction(event -> {



		});

		IconButton stopButton = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.STOP));
		stopButton.setOnAction(event -> {
			if (launcher.showModal(false, "Подвтердите",
					"Вы уверены что хотите отменить загрузку?") == ModalDialog.YES_OPTION) {
				if (launchFuture != null) {
					statusPane.setLaunchFormVisible(false);
					launchFuture.cancel(false);
					launchFuture = null;
				}
			}
		});

		HBox hbox = new HBox(progressBar, pauseResumeButton, stopButton);
		hbox.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, null)));
		hbox.getStyleClass().add("downloading");
		setCenter(hbox);
	}

	private void handleLaunchProgressUpdate(TaskProgress taskProgress) {
		progressBar.setProgress(taskProgress.getProgress());
	}

	public void launch() {
		statusPane.setLaunchFormVisible(true);
		launchFuture = new LaunchTaskChain().executeChain(this::handleLaunchProgressUpdate);
		launchFuture.whenComplete((__, cause) -> Platform.runLater(() -> statusPane.handleLaunchTaskChainCompleted(cause)));
	}
}
