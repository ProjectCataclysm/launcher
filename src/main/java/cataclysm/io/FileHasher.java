package cataclysm.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import fr.cryptohash.SHA1;

/**
 * Created 16 окт. 2018 г. / 23:34:11 
 * @author Knoblul
 */
public class FileHasher {
	private static final SHA1 digest = new SHA1();
	private static byte[] buffer = new byte[8192];

	public static String hash(File file) {
		try {
			digest.reset();
			try (InputStream in = Files.newInputStream(file.toPath())) {
				int len;
				while ((len = in.read(buffer)) > 0) {
					digest.update(0, len, buffer);
				}
			}

			StringBuilder sb = new StringBuilder();
			byte[] result = digest.digest();
			for (byte b : result) {
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
