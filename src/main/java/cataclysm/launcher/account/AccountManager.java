package cataclysm.launcher.account;

import cataclysm.launcher.utils.ApiException;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.Log;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;

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
				useSession(HttpClientWrapper.postJsonRequest(
					"client/validate",
					Collections.singletonMap("accessToken", accessToken),
					Session.class
				));
			} catch (Exception e) {
				if (e instanceof ApiException) {
					useSession(null);
				}

				throw new RuntimeException("Failed to validate session", e);
			}
		}
	}

	public void authorize(String login, String password) {
		try {
			HashMap<Object, Object> req = new HashMap<>();
			req.put("login", login);
			req.put("password", password);
			useSession(HttpClientWrapper.postJsonRequest("client/auth", req, Session.class));
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to authorize", e);
		}
	}

//	private void parseSession(JSONObject response) {
//		JSONObject userNode = (JSONObject) response.get("user");
//		JSONObject profileNode = (JSONObject) userNode.get("profile");
//		Profile profile = new Profile((String) profileNode.get("uuid"), (String) profileNode.get("email"),
//				(String) profileNode.get("username"));
//
//		Set<String> ticketsRaw = new HashSet<>();
//		JSONArray ticketsNode = (JSONArray) userNode.get("tickets");
//		for (Object o : ticketsNode) {
//			JSONObject node = (JSONObject) o;
//			ticketsRaw.add(node.get("type") + ":" + node.get("name"));
//		}
//
//		String accessToken = Objects.requireNonNull((String) response.get("accessToken"));
//		int balance = ((Number) userNode.get("balance")).intValue();
//		session = new Session(profile, accessToken, ticketsRaw, balance);
//
//		saveSession();
//	}

	private void useSession(@Nullable Session session) {
		this.session = session;

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

	public void logout() {
		if (session != null) {
			try {
				HttpClientWrapper.postJsonRequest(
					"client/invalidate",
					Collections.singletonMap("accessToken", session.getAccessToken()),
					null
				);
			} catch (Exception e) {
				Log.err(e, "Failed to logout");
			}
		}

		useSession(null);
	}
}
