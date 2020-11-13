package cataclysm.launcher.utils;

import cataclysm.launcher.utils.logging.Log;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 09.10.2020 17:59
 *
 * @author Knoblul
 */
public class Account {
	public static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-_.+]*[\\w-_.]@([\\w]+\\.)+[\\w]+[\\w]$");

	private final Path sessionStorePath;

	private final ReadOnlyStringWrapper uuid = new ReadOnlyStringWrapper();
	private final ReadOnlyStringWrapper sessionId = new ReadOnlyStringWrapper();
	private final ReadOnlyStringWrapper username = new ReadOnlyStringWrapper();

	public Account(Path launcherDirPath) {
		this.sessionStorePath = launcherDirPath.resolve("session.txt");
	}

	public String getUUID() {
		return uuid.get();
	}

	public ReadOnlyStringProperty uuidProperty() {
		return uuid;
	}

	public String getSessionId() {
		return sessionId.get();
	}

	public ReadOnlyStringProperty sessionIdProperty() {
		return sessionId;
	}

	public String getUsername() {
		return username.get();
	}

	public ReadOnlyStringProperty usernameProperty() {
		return username;
	}

	@SuppressWarnings("unchecked")
	public void login(String login, String password) throws IOException {
		JSONObject request = new JSONObject();
		request.put("login", login);
		request.put("password", password);

		JSONObject response = HttpHelper.executeJsonObjectRequest(HttpHelper.API_URL + "/auth", request);
		String sessionId = (String) response.get("sessionId");
		Files.write(sessionStorePath, Collections.singleton(sessionId));
		Log.msg("Successfully logged in as %s", response.get("username"));

		Platform.runLater(() -> {
			username.set((String) response.get("username"));
			uuid.set((String) response.get("uuid"));
			this.sessionId.set((String) response.get("sessionId"));
		});
	}

	@SuppressWarnings("unchecked")
	public void validate() throws IOException {
		String sessionId = Files.readAllLines(sessionStorePath).get(0).trim();
		JSONObject request = new JSONObject();
		request.put("sessionId", sessionId);

		JSONObject response = HttpHelper.executeJsonObjectRequest(HttpHelper.API_URL + "/validate", request);
		Log.msg("User: %s", response.get("username"));

		Platform.runLater(() -> {
			this.sessionId.set(sessionId);
			username.set((String) response.get("username"));
			uuid.set((String) response.get("uuid"));
		});
	}

	@SuppressWarnings("unchecked")
	public void logout() throws IOException {
		if (getSessionId() != null) {
			JSONObject request = new JSONObject();
			request.put("sessionId", getSessionId());

			Platform.runLater(() -> {
				uuid.set(null);
				sessionId.set(null);
				username.set(null);
			});

			HttpHelper.executeJsonObjectRequest(HttpHelper.API_URL + "/invalidate", request);
		}

		Files.deleteIfExists(sessionStorePath);
	}
}
