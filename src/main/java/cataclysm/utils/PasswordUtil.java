package cataclysm.utils;

import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * <br>
 * <br>
 * <i>Created 22 июл. 2019 г. / 16:27:17</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * 
 * @author Knoblul
 */
public class PasswordUtil {
	private static final String ALGORITHM = "HmacSHA256";
	private static final String SECURITY_SALT = "YQH38JFrZwqZNscSHFa3OQ";

	public static String hashPassword(String password) {
		try {
			byte[] salt = SECURITY_SALT.getBytes();
		    Mac mac = Mac.getInstance(ALGORITHM);
		    SecretKeySpec secret = new SecretKeySpec(salt, ALGORITHM);
		    mac.init(secret);
		    byte[] digest = mac.doFinal(password.getBytes());
		    MessageDigest md5 = MessageDigest.getInstance("SHA-256");
		    md5.update(digest);
		    md5.update(salt);
		    StringBuilder result = new StringBuilder();
		    digest = md5.digest();
		    for (byte b: digest) {
		    	if ((0xff & b) < 0x10) {
		    		result.append("0" + Integer.toHexString((0xFF & b)));
				} else {
					result.append(Integer.toHexString(0xFF & b));
				}
		    }
		    return result.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean verifyPassword(String password, String key) {
		String hash = hashPassword(password);
		return hash != null && hash.equals(key);
	}
}
