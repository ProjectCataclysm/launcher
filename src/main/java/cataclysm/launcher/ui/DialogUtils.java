package cataclysm.launcher.ui;

import cataclysm.launcher.utils.Log;
import com.google.common.base.Throwables;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;

/**
 * Created 16 ���. 2018 �. / 22:17:44 
 * @author Knoblul
 */
public class DialogUtils {
	public static void showError(String message, @Nullable Throwable t, boolean log) {
		if (log) {
			Log.err(t, message != null ? message : "");
		}

		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Ошибка");

		if (t != null) {
			alert.setHeaderText(message);

			VBox dialogPaneContent = new VBox();
			Label label = new Label("Отладочные данные:");
			TextArea textArea = new TextArea();
			textArea.setEditable(false);
			textArea.setText(Throwables.getStackTraceAsString(t));
			dialogPaneContent.getChildren().addAll(label, textArea);
			alert.getDialogPane().setContent(dialogPaneContent);
		} else {
			alert.setHeaderText(null);
			alert.setContentText(message);
		}

		alert.initModality(Modality.APPLICATION_MODAL);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		StageUtils.setupIcons(stage);
		StageUtils.setupScene(stage);
		alert.showAndWait();
	}
}
