package cataclysm;

import cataclysm.launch.Launcher;

/**
 * Created 21 ���. 2018 �. / 17:14:34 
 * @author Knoblul
 */
public class Main {
	public static void main(String[] args) {
		try {
			new Launcher(args);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
}
