package cataclysm.launcher.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Помощники для генерации хешей.
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 30.09.2020 3:00
 * @author Knoblul
 */
public class ChecksumUtil {
	public static String computeChecksum(InputStream in) throws IOException {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[8 * 1024];
			int len;
			while ((len = in.read(buffer)) != -1) {
				messageDigest.update(buffer, 0, len);
			}

			return hexDigest(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String computeChecksum(Path filePath) throws IOException {
		try (InputStream in = Files.newInputStream(filePath)) {
			return computeChecksum(in);
		}
	}

	public static String hexDigest(byte[] hashBytes) {
		/*
				StringBuilder sb = new StringBuilder();
				byte[] digest = messageDigest.digest();
				for (byte b : digest) {
					if ((0xff & b) < 0x10) {
						sb.append("0").append(Integer.toHexString((0xFF & b)));
					} else {
						sb.append(Integer.toHexString(0xFF & b));
					}
				}

				return sb.toString().toUpperCase(Locale.ROOT);
		 */

		StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
		for (byte hashByte : hashBytes) {
			String hex = Integer.toHexString(hashByte & 0xFF);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
