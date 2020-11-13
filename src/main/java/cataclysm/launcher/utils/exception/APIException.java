package cataclysm.launcher.utils.exception;

import java.io.IOException;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 09.10.2020 18:18
 *
 * @author Knoblul
 */
public class APIException extends IOException {
	public APIException() {
		super();
	}

	public APIException(String message) {
		super(message);
	}

	public APIException(String message, Throwable cause) {
		super(message, cause);
	}

	public APIException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getLocalizedMessage() {
		String originalMessage = super.getLocalizedMessage();
		switch (originalMessage) {
			case "error.internal":
				return "Внутренняя ошибка сервера";
			case "error.badsessionvalue":
			case "error.invalidsession":
				return "Сессия устарела, повторите вход";
			case "error.auth.invalid.login":
			case "error.auth.invalid.password":
				return "Неверно указан логин или пароль";
			case "error.auth.limitreached":
				return "Превышено максимальное число допустимых попыток. Попробуйте через 5 минут.";
			case "error.profile.notfound":
				return "Профиль не найден";
			case "error.banned":
				return "Вы забанены :/";
			default:
				return "API: " + originalMessage;
		}
	}
}
