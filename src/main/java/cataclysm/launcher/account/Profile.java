package cataclysm.launcher.account;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 31.07.2022 18:57
 *
 * @author Knoblul
 */
public class Profile {
	private final String uuid;
	private final String email;
	private final String username;

	public Profile(String uuid, String email, String username) {
		this.uuid = uuid;
		this.email = email;
		this.username = username;
	}

	public String getUuid() {
		return uuid;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}
}
