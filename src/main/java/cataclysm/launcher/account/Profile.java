package cataclysm.launcher.account;

import java.util.Objects;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 31.07.2022 18:57
 *
 * @author Knoblul
 */
public class Profile {
	private final String uuid;
	private final String email;
	private final String username;

	public Profile(String uuid, String email, String username) {
		this.uuid = Objects.requireNonNull(uuid);
		this.email = Objects.requireNonNull(email);
		this.username = Objects.requireNonNull(username);
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
