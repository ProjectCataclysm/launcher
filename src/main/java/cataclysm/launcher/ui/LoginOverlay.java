package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.ApiException;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConstants;
import cataclysm.launcher.utils.Log;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 13:08
 *
 * @author Knoblul
 */
public class LoginOverlay extends AbstractLauncherOverlay {
	public static final PseudoClass PSEUDO_CLASS_WRONG_INPUT = PseudoClass.getPseudoClass("wrong-input");
	private final ProgressIndicator loginIndicator;
	private final GridPane grid;
	private final Label error;
	private final TextField loginField;
	private final TextField passwordField;

	public LoginOverlay() {
		getStyleClass().add("login-form-overlay");

		grid = new GridPane();
		grid.getStyleClass().add("login-fields");

		grid.getColumnConstraints().add(0, new ColumnConstraints());
		ColumnConstraints rightConst = new ColumnConstraints();
		rightConst.setHalignment(HPos.RIGHT);
		grid.getColumnConstraints().add(1, rightConst);

		Label title = new Label("Вход");
		title.setMaxWidth(Double.MAX_VALUE);
		title.getStyleClass().addAll("title", "bold-font");

		grid.add(title, 0, 0, 2, 1);

		grid.add(new Label("Логин:"), 0, 1);
		loginField = new TextField();
		loginField.setOnAction(__ -> submitLogin());
		loginField.textProperty()
				.addListener(__ -> loginField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, false));
		grid.add(loginField, 1, 1);

		grid.add(new Label("Пароль:"), 0, 2);
		passwordField = new PasswordField();
		passwordField.setOnAction(__ -> submitLogin());
		passwordField.textProperty()
				.addListener(__ -> passwordField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, false));
//		CheckBox showPassword = new CheckBox();
//		AnchorPane.setRightAnchor(showPassword, 1.0);
//		AnchorPane anchorPane = new AnchorPane(showPassword);
//		anchorPane.setPickOnBounds(false);
//		fields.add(new StackPane(password, anchorPane), 1, 2);
		grid.add(passwordField, 1, 2);

		error = new Label();
		error.getStyleClass().add("error-message");
		error.setVisible(false);
		grid.add(error, 0, 3, 2, 1);

		Label restoreLabel = new Label("Восстановление аккаунта");
		restoreLabel.getStyleClass().add("help-question");
		restoreLabel.setOnMouseClicked(event -> HttpClientWrapper.browse("https://" + LauncherConstants.WORK_SITE_URL + "/password?action=restorePassword"));
		grid.add(restoreLabel, 1, 4);

		Label registerLabel = new Label("Регистрация");
		registerLabel.getStyleClass().add("help-question");
		registerLabel.setOnMouseClicked(event -> HttpClientWrapper.browse("https://" + LauncherConstants.WORK_SITE_URL + "/signup"));
		grid.add(registerLabel, 1, 5);

		Button submitButton = new Button("Войти");
		submitButton.getStyleClass().add("submit");
		submitButton.setOnAction(event -> submitLogin());
		grid.add(submitButton, 1, 6);

		loginIndicator = new ProgressBar();
		loginIndicator.getStyleClass().addAll("logging-in-indicator", "download-progress");
		loginIndicator.setMaxWidth(Double.MAX_VALUE);
		loginIndicator.setMaxHeight(USE_PREF_SIZE);
		loginIndicator.setVisible(false);

		VBox progressWrapper = new VBox(loginIndicator);
		progressWrapper.setPickOnBounds(false);

		StackPane form = new StackPane(grid, progressWrapper);
		form.getStyleClass().add("login-form");
		form.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
		getChildren().add(form);

		showHideAnimation = createAnimation(form);
	}

	private Transition createAnimation(StackPane form) {
		Duration duration = Duration.millis(500);
		FadeTransition fade = new FadeTransition(duration, this);
		fade.setFromValue(0.0);
		fade.setToValue(1.0);

		ScaleTransition scale = new ScaleTransition(duration);
		scale.setFromX(1.5);
		scale.setFromY(1.5);
		scale.setFromZ(1.5);
		scale.setToX(1.0);
		scale.setToY(1.0);
		scale.setToZ(1.0);

		ParallelTransition result = new ParallelTransition(form, fade, scale);
		result.setInterpolator(Interpolator.EASE_BOTH);
		result.setDelay(Duration.millis(500));
		return result;
	}

	private void submitLogin() {
		String login = loginField.getText();
		String password = passwordField.getText();
		boolean proceed = true;

		if (login.trim().isEmpty()) {
			loginField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, true);
			proceed = false;
		}

		if (password.isEmpty()) {
			passwordField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, true);
			proceed = false;
		}

		if (!proceed) {
			return;
		}

		loginIndicator.setProgress(0);
		loginIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		loginIndicator.setVisible(true);
		loginField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, false);
		passwordField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, false);
		error.getStyleClass().remove("error-clickable");
		error.setOnMouseClicked(null);
		grid.setDisable(true);

		LauncherApplication launcher = LauncherApplication.getInstance();
		Executor delayedExecutor = launcher.getDelayedExecutor(2);
		CompletableFuture.runAsync(() -> launcher.getAccountManager().authorize(login, password), delayedExecutor)
				.handleAsync((result, cause) -> handleAuthResult(cause), Platform::runLater)
				.whenComplete(Log::logFutureErrors);
	}

	private Void handleAuthResult(Throwable cause) {
		Throwable realCause = cause;
		if (cause instanceof CompletionException) {
			realCause = cause.getCause();
		}

//		if (realCause instanceof AuthorizationException) {
//			realCause = realCause.getCause();
//		}

		if (realCause != null) {
			Log.err(realCause, "Failed to login");
		}

		if (realCause != null) {
			showError(realCause);
		} else {
			setVisible(false);
			LauncherApplication app = LauncherApplication.getInstance();
			app.getSessionInfoPane().init(app.getAccountManager().getSession());
		}

		return null;
	}

	private void showError(Throwable cause) {
		if (LauncherConstants.isUnreachableException(cause)) {
			error.setText("Сервис временно недоступен. Проверьте подключение к интернету и повторите попытку позднее.");
		} else if (cause instanceof ApiException) {
			if (cause.getMessage().equals("api.error.clientAuth.invalidCredentials")) {
				loginField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, true);
				passwordField.pseudoClassStateChanged(PSEUDO_CLASS_WRONG_INPUT, true);
			}

			error.setText(cause.getLocalizedMessage());
		} else {
			error.getStyleClass().add("error-clickable");
			error.setText("Неизвестная ошибка. Нажмите, чтобы уточнить детали.");
			error.setOnMouseClicked(__ -> DialogUtils.showError(null, cause, false));
		}

		//wrong-input

		error.setVisible(true);
		loginIndicator.setVisible(false);
		grid.setDisable(false);
		loginIndicator.setProgress(0);
		loginIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
	}

	public void reset() {
		loginField.clear();
		passwordField.clear();
		error.setVisible(false);
		loginIndicator.setVisible(false);
		grid.setDisable(false);
		loginIndicator.setProgress(0);
		loginIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
	}
}
