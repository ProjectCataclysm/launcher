package cataclysm.launcher.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 14 . 2019 . / 13:48:15
 *
 * @author Knoblul
 */
public class ConsoleDisconnector {
	public static void disconnect() {
		PrintStream stream = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				
			}
		});
		
		System.setOut(stream);
		System.setErr(stream);
	}
}
