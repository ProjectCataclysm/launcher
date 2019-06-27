package cataclysm.launch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * 
 * 
 * <br><br><i>Created 14 џэт. 2019 у. / 13:48:15</i><br>
 * SME REDUX / NOT FOR FREE USE!
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
