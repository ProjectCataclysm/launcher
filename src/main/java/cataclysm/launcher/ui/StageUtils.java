package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.Log;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;


/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 06.08.2022 15:44
 *
 * @author Knoblul
 */
public class StageUtils {
	private static final String LAUNCHER_STYLE = Objects.requireNonNull(
			LauncherApplication.class.getResource("/launcher.css")).toExternalForm();

	public static Scene createScene(Parent root, int width, int height) {
		Scene scene = new Scene(root, width, height);
		scene.getStylesheets().add(LAUNCHER_STYLE);
		return scene;
	}

	public static void setupScene(Stage stage) {
		stage.getScene().getStylesheets().add(LAUNCHER_STYLE);
	}

	public static void setupIcons(Stage stage) {
		try {
			stage.getIcons().setAll(
					Icons.readImage("icons/icon_16.png"),
					Icons.readImage("icons/icon_32.png"),
					Icons.readImage("icons/icon_64.png")
			);
		} catch (IOException e) {
			Log.err(e, "Failed to set application icons");
		}
	}
}
