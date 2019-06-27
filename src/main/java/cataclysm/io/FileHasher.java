package cataclysm.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created 16 окт. 2018 г. / 23:34:11 
 * @author Knoblul
 */
public class FileHasher {
	private static final MessageDigest messageDigest;
	private static byte[] buffer;

	static {
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		buffer = new byte[4096];
	}
	
	public static String hash(File file) {
		try {
			messageDigest.reset();
			try (InputStream in = Files.newInputStream(file.toPath())) {
				int len;
				while ((len = in.read(buffer)) > 0) {
					messageDigest.update(buffer, 0, len);
				}
			}

			StringBuilder sb = new StringBuilder();
			byte[] digest = messageDigest.digest();
			for (byte b : digest) {
				if ((0xff & b) < 0x10) {
					sb.append("0" + Integer.toHexString((0xFF & b)));
				} else {
					sb.append(Integer.toHexString(0xFF & b));
				}
			}
			return sb.toString().toUpperCase();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
