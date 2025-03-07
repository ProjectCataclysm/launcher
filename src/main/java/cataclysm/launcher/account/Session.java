package cataclysm.launcher.account;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * <br><br><i>Created 23 июл. 2019 г. / 20:16:17</i><br>
 *
 * @author Knoblul
 */
public class Session {
	private final User user;
	private final @Nullable String accessToken;

	public Session(User user, @Nullable String accessToken) {
		this.user = user;
		this.accessToken = accessToken;
	}

	public User getUser() {
		return user;
	}

	public PlayerProfile getProfile() {
		return user.getProfile();
	}

	public int getBalance() {
		return user.getBalance();
	}

	public @Nullable String getAccessToken() {
		return accessToken;
	}

	public static final class PlayerProfile {
		private final UUID uuid;
		private final String username;
		private final String email;

		public PlayerProfile(UUID uuid, String username, String email) {
			this.uuid = uuid;
			this.username = username;
			this.email = email;
		}

		public UUID getUUID() {
			return uuid;
		}

		public String getUsername() {
			return username;
		}

		public String getUsernameWithUUID() {
			return username + " (" + uuid.toString() + ")";
		}

		public String getEmail() {
			return email;
		}
	}

	public static final class User {
		private final PlayerProfile profile;
		private final Ticket[] tickets;
		private final int balance;

		public User(PlayerProfile profile, Ticket[] tickets, int balance) {
			this.profile = profile;
			this.tickets = tickets;
			this.balance = balance;
		}

		public PlayerProfile getProfile() {
			return profile;
		}

		public Ticket[] getTickets() {
			return tickets;
		}

		public int getBalance() {
			return balance;
		}

		public static final class Ticket {
			private final String type;
			private final String name;

			public Ticket(String type, String name) {
				this.type = type;
				this.name = name;
			}

			@Override
			public boolean equals(Object object) {
				if (this == object) {
					return true;
				}
				if (object == null || getClass() != object.getClass()) {
					return false;
				}

				Ticket ticket = (Ticket) object;
				return type.equals(ticket.type) && name.equals(ticket.name);
			}

			@Override
			public int hashCode() {
				int result = type.hashCode();
				result = 31 * result + name.hashCode();
				return result;
			}
		}
	}
}
