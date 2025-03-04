package cataclysm.launcher.account;

import cataclysm.launcher.utils.ApiException;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 06.08.2022 21:19
 *
 * @author Knoblul
 */
public class AccountManager {
	private @Nullable Session session;
	private final Path sessionFilePath = LauncherConfig.LAUNCHER_DIR_PATH.resolve("session");

	public @Nullable Session getSession() {
		return session;
	}

	@SuppressWarnings("unchecked")
	public void validateSession() {
		String accessToken = null;
		try {
			accessToken = String.join("", Files.readAllLines(sessionFilePath));
		} catch (FileNotFoundException | NoSuchFileException ignored) {
		} catch (Exception e) {
			Log.err(e, "Failed to load session file");
		}

		if (accessToken != null) {
			try {
				JSONObject request = new JSONObject();
				request.put("accessToken", accessToken);
				parseSession(Objects.requireNonNull(HttpClientWrapper.postJsonRequest("client/validate", request)));
			} catch (Exception e) {
				if (e instanceof ApiException) {
					session = null;
					saveSession();
				}

				throw new RuntimeException("Failed to validate session", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void authorize(String login, String password) {
		try {
			JSONObject request = new JSONObject();
			request.put("login", login);
			request.put("password", password);
			parseSession(Objects.requireNonNull(HttpClientWrapper.postJsonRequest("client/auth", request)));
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to authorize", e);
		}
	}

	private void parseSession(JSONObject response) {
		JSONObject userNode = (JSONObject) response.get("user");
		JSONObject profileNode = (JSONObject) userNode.get("profile");
		Profile profile = new Profile((String) profileNode.get("uuid"), (String) profileNode.get("email"),
				(String) profileNode.get("username"));

		Set<String> ticketsRaw = new HashSet<>();
		JSONArray ticketsNode = (JSONArray) userNode.get("tickets");
		for (Object o : ticketsNode) {
			JSONObject node = (JSONObject) o;
			ticketsRaw.add(node.get("type") + ":" + node.get("name"));
		}

		String accessToken = Objects.requireNonNull((String) response.get("accessToken"));
		int balance = ((Number) userNode.get("balance")).intValue();
		session = new Session(profile, accessToken, ticketsRaw, balance);

		saveSession();
	}

	@SuppressWarnings("unchecked")
	public void logout() {
		if (session != null) {
			try {
				JSONObject request = new JSONObject();
				request.put("accessToken", session.getAccessToken());
				HttpClientWrapper.postJsonRequest("client/invalidate", request);
			} catch (Exception e) {
				Log.err(e, "Failed to logout");
			}
		}

		session = null;
		saveSession();
	}

	private void saveSession() {
		try {
			if (session != null) {
				Files.write(sessionFilePath, Collections.singletonList(session.getAccessToken()));
			} else {
				Files.deleteIfExists(sessionFilePath);
			}
		} catch (Exception e) {
			Log.err(e, "Failed to write session to file");
		}
	}
}
