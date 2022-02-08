import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 01.12.2021 0:58
 *
 * @author Knoblul
 */
public class ServTest {

	public static RandomAccessFile openPipe() {
		for (int i = 0; i < 10; i++) {
			try {
				RandomAccessFile pipe = new RandomAccessFile("\\\\\\\\?\\\\pipe\\\\discord-ipc-" + i, "rw");
				return pipe;
			} catch (FileNotFoundException ignored) {

			} catch (Throwable t) {
				t.printStackTrace();
				break;
			}
		}

		return null;
	}

	public static void main(String[] args) throws IOException {

//		List<String> strings = Files.readAllLines(Paths.get("C:\\Users\\Mihail\\Desktop\\port.txt"));
//		Map<String, String> map = new HashMap<>();
//		for (String string : strings) {
//			String[] kv = string.split("=");
//			map.put(kv[0], kv[1]);
//		}
//
//		System.out.println(map);

		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(Paths.get("C:\\Users\\Mihail\\Desktop\\minecraft.jar")));
			 BufferedWriter writer = Files.newBufferedWriter(Paths.get("C:\\Users\\Mihail\\Desktop\\vanilla_crc32.txt"))) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					writer.write(entry.getName() + "=" + entry.getCrc() + "\n");
				}
			}
		}

//
//		try (RandomAccessFile pipe1 = openPipe(); RandomAccessFile pipe2 = openPipe()) {
//
//			System.out.println(pipe2.read());
//			System.out.println(pipe1.read());
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		//iRlIVyMfEO
//		try (ServerSocket socket = new ServerSocket(1399)) {
//			while (true) {
//				try (Socket accept = socket.accept()) {
//					System.out.println("New client: " + accept.getRemoteSocketAddress());
//
//					try (BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()))) {
//						String ln;
//						while ((ln = reader.readLine()) != null) {
//							System.out.println(ln);
//						}
//
//						try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(accept.getOutputStream
//						()))) {
//							writer.write("Lorem ipsum dolor sit amet Lorem ipsum dolor sit amet Lorem ipsum dolor sit
//							amet Lorem ipsum dolor sit amet Lorem ipsum dolor sit amet Lorem ipsum dolor sit amet");
//						}
//					}
//				} catch (Throwable e) {
//					e.printStackTrace();
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
}
