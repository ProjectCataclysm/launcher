package cataclysm.utils;

/**
 * 
 * <br><br><i>Created 23 июл. 2019 г. / 20:16:17</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * @author Knoblul
 */
public class LoginHolder {
	private String uuid;
	private String sessionId;
	private String username;
	
	public LoginHolder(String uuid, String sessionId, String username) {
		this.uuid = uuid;
		this.sessionId = sessionId;
		this.username = username;
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
}
