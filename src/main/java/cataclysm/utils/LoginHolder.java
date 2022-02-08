package cataclysm.utils;

/**
 * <br><br><i>Created 23 июл. 2019 г. / 20:16:17</i><br>
 * SME REDUX / NOT FOR FREE USE!
 *
 * @author Knoblul
 */
public class LoginHolder {
	private final String uuid;
	private final String sessionId;
	private final String username;
	private final boolean buildServerAccess;

	public LoginHolder(String uuid, String sessionId, String username, boolean buildServerAccess) {
		this.uuid = uuid;
		this.sessionId = sessionId;
		this.username = username;
		this.buildServerAccess = buildServerAccess;
	}

	public String getUUID() {
		return uuid;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getUsername() {
		return username;
	}

	public boolean isBuildServerAccess() {
		return buildServerAccess;
	}
}
