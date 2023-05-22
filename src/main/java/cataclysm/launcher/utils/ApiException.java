package cataclysm.launcher.utils;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 9:55
 *
 * @author Knoblul
 */
public class ApiException extends Exception {
	public ApiException(String message) {
		super(message);
	}

	@Override
	public String getLocalizedMessage() {
		String code = getMessage();
		switch (code) {
			case "api.error.unavailable":
				return "Сервис временно недоступен";
			case "api.error.internal":
				return "Внутренняя ошибка сервера";
			case "api.error.invalidSession":
				return "Сессия устарела, повторите вход";
			case "api.error.clientAuth.invalidCredentials":
				return "Неверно введен логин или пароль";
			case "api.error.clientAuth.limitReached":
				return "Превышено максимальное число допустимых попыток. Попробуйте через 5 минут.";
			case "api.error.clientBanned":
				return "Вы забанены :/";
			default:
				return "Неизвестная ошибка (" + code + ")";
		}
	}
}
