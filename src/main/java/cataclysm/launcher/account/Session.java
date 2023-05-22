package cataclysm.launcher.account;

import java.util.Objects;
import java.util.Set;

/**
 * <br><br><i>Created 23 июл. 2019 г. / 20:16:17</i><br>
 *
 * @author Knoblul
 */
public class Session {
	private final Profile profile;
	private final String accessToken;
	private final Set<String> tickets;
	private final int balance;

	public Session(Profile profile, String accessToken, Set<String> tickets, int balance) {
		this.profile = Objects.requireNonNull(profile);
		this.accessToken = Objects.requireNonNull(accessToken);
		this.tickets = Objects.requireNonNull(tickets);
		this.balance = balance;
	}

	public Profile getProfile() {
		return profile;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public Set<String> getTickets() {
		return tickets;
	}

	public int getBalance() {
		return balance;
	}
}
