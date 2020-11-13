package cataclysm.launcher.ui.forms.login;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.*;
import cataclysm.launcher.utils.exception.APIException;
import cataclysm.launcher.utils.Account;
import cataclysm.launcher.utils.AsyncTasks;
import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.utils.logging.Log;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;


/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.10.2020 5:24
 *
 * @author Knoblul
 */
public class LoginForm extends BorderPane {
	private final Launcher launcher;
	private final LabelField errorLabel;

	private final CustomTextField loginField;
	private final CustomPasswordField passwordField;
	private final ProgressIndicator loginProgress;
	private final Button loginButton;

	public LoginForm(Launcher launcher) {
		this.launcher = launcher;

		getStyleClass().add("login-form");

		Label title = new Label("ВХОД");
		title.getStyleClass().add("title");

		errorLabel = new LabelField();
		errorLabel.getStyleClass().add("error-modal-label");
		errorLabel.setPrefHeight(50);
		errorLabel.setMaxHeight(USE_PREF_SIZE);
		errorLabel.setMinHeight(USE_PREF_SIZE);

		loginField = new CustomTextField();
		loginField.setPromptText("Логин");

		passwordField = new CustomPasswordField();
		passwordField.setPromptText("Пароль");

		Button restoreButton = new IconButton("ВОССТАНОВЛЕНИЕ ПАРОЛЯ");
		restoreButton.getStyleClass().add("reset-button");
		restoreButton.setOnAction(event -> PlatformHelper.browseURL("https://project-cataclysm.ru/passreset.php"));

		Label questionLabel = new Label("Еще не зарегистрировались?");
		questionLabel.getStyleClass().add("question-label");

		Button registerButton = new IconButton("РЕГИСТРАЦИЯ");
		registerButton.getStyleClass().add("register-button");
		registerButton.setOnAction(event -> PlatformHelper.browseURL("https://project-cataclysm.ru/registration.php"));

		VBox questionPane = new VBox(restoreButton, questionLabel, registerButton);
		questionPane.getStyleClass().addAll("question-pane", "center-pane");

		loginButton = new LargeButton("Вход");
		loginButton.setOnAction(this::performLogin);
		loginButton.getStyleClass().add("login-button");
		loginButton.setMaxWidth(Double.MAX_VALUE);
		loginButton.setDisable(true);
		loginButton.setDefaultButton(true);

		ChangeListener<String> fieldChangeListener = (observable, oldValue, newValue) -> {
			boolean canLogin = !Strings.isNullOrEmpty(loginField.getText()) && !Strings.isNullOrEmpty(passwordField.getText());
			if (newValue != null && newValue.contains("@") && !Account.EMAIL_PATTERN.matcher(newValue.trim()).matches()) {
				setErrorText("Некорректный email");
				canLogin = false;
			}
			setErrorText(null);
			loginButton.setDisable(!canLogin);
		};

		loginField.textProperty().addListener(fieldChangeListener);
		passwordField.textProperty().addListener(fieldChangeListener);

		loginProgress = new ProgressIndicator();
		loginProgress.setPrefSize(32, 32);
		loginProgress.setVisible(false);

		StackPane loginButtonWrapper = new StackPane(loginButton, loginProgress);
		loginButtonWrapper.setMaxHeight(USE_PREF_SIZE);

		VBox wrapper = new VBox(loginField, passwordField);
		wrapper.getStyleClass().add("text-field-wrapper");
		VBox center = new VBox(title, errorLabel, wrapper, questionPane, loginButtonWrapper);
		center.getStyleClass().addAll("login-form-grid", "padded-pane", "center-pane");
		center.setMaxWidth(280);
		setCenter(center);
	}

	private void performLogin(ActionEvent actionEvent) {
		Log.msg("Logging in...");

		CompletableFuture<Account> future = CompletableFuture.supplyAsync(this::login, AsyncTasks.executor);
		AsyncTasks.whenComplete(future, (account, exception) -> {
			Runnable runLater;
			if (exception != null) {
				Log.err(exception, "Login failed:");
				runLater = () -> {
					setError(exception);
					postLogin();
				};
			} else {
				runLater = () -> {
					launcher.performTransition(launcher.getMainForm());
					postLogin();
				};
			}

			Platform.runLater(runLater);
		});
	}

	private Account login() {
		Platform.runLater(() -> {
			preLogin();
			setError(null);
		});

		long waitTime = System.currentTimeMillis();
		Account account = launcher.getAccount();
		try {
			account.login(loginField.getText().trim(), passwordField.getText());
			return account;
		} catch (IOException e) {
			// удаляем данные о логине если логин неудачен
			try {
				account.logout();
			} catch (IOException x) {
				x.printStackTrace();
			}

			throw new CompletionException(e);
		} finally {
			// уменьшаем скорость запросов к апи
			if (System.currentTimeMillis() - waitTime < 1000) {
				try {
					Thread.sleep(1000 - (System.currentTimeMillis() - waitTime));
				} catch (InterruptedException ignored) {

				}
			}
		}
	}

	private void preLogin() {
		setDisable(true);
		loginButton.setVisible(false);
		loginProgress.setVisible(true);
	}

	private void postLogin() {
		setDisable(false);
		loginButton.setVisible(true);
		loginProgress.setVisible(false);
	}

	private void setErrorText(String text) {
		errorLabel.setText(text);
	}

	public void setError(Throwable error) {
		if (error == null) {
			setErrorText(null);
		} else if (error instanceof APIException) {
			setErrorText(error.getLocalizedMessage());
		} else {
			setErrorText(error.toString());
		}
	}

	public void clear() {
		setErrorText(null);
		loginField.clear();
		passwordField.clear();
	}
}
