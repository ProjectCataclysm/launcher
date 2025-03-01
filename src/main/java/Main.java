import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.selfupdate.LauncherRestartedException;
import cataclysm.launcher.selfupdate.StubChecker;
import cataclysm.launcher.ui.AwtErrorDialog;
import cataclysm.launcher.utils.LauncherLock;
import cataclysm.launcher.utils.Log;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Created 21 сент. 2018 г. / 17:14:34 
 * @author Knoblul
 */
public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		new LauncherApplication().start(primaryStage);
	}

	public static void main(String[] args) {
		Log.msg("Starting launcher...");

		try {
			Thread.sleep(300);
			LauncherLock.lock();
		} catch (LauncherLock.AlreadyLaunchedException e) {
			AwtErrorDialog.showError("Лаунчер уже запущен", null);
			return;
		} catch (Throwable t) {
			AwtErrorDialog.showError("Ошибка запуска лаунчера", t);
			return;
		}

		try {
			StubChecker.check(args);
		} catch (LauncherRestartedException e) {
			System.exit(0);
			return;
		} catch (Throwable t) {
			AwtErrorDialog.showError(t.toString(), t);
			return;
		}

		try {
			launch(args);
		} catch (Throwable t) {
			AwtErrorDialog.showError("Ошибка запуска", t);
		}
	}
}
