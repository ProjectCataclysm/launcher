package cataclysm.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * Функция генерации хешей файлов.
 * Теперь хеш генерируется таким образом:
 * 1. Файл разбивается на чанки по 1мб.
 * 2. Для каждого чанка считается его CRC32.
 * 3. Из всех итоговых CRC32 считаем SHA-1 (обновляем {@link MessageDigest} с помощью {@link CRC32#getValue()})
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 30.09.2020 3:00
 * @author Knoblul
 */
public class ChecksumUtil {
	public static String computeChecksum(InputStream in) throws IOException {
		try {
			CRC32 crc32 = new CRC32();
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[8 * 1024];

			int consumedBlockSize = 0;
			List<Long> checksums = new ArrayList<>();
			int blockLength = 1024 * 1024;

			while (true) {
				int len = in.read(buffer, 0, Math.min(blockLength - consumedBlockSize, buffer.length));
				if (len == -1) {
					break;
				}

				consumedBlockSize += len;
				crc32.update(buffer, 0, len);

				if (consumedBlockSize >= blockLength) {
					checksums.add(crc32.getValue());
					crc32.reset();
					consumedBlockSize = 0;
				}
			}

			if (consumedBlockSize > 0) {
				checksums.add(crc32.getValue());
			}

			byte[] longValueBuffer = new byte[Long.BYTES];
			for (long checksum : checksums) {
				for (int i = 7; i >= 0; i--) {
					longValueBuffer[i] = (byte) (checksum & 0xFFL);
					checksum >>= 8;
				}

				messageDigest.update(longValueBuffer);
			}

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
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String computeChecksum(Path filePath) {
		try (InputStream in = Files.newInputStream(filePath)) {
			return computeChecksum(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
