package cataclysm.launcher;

import cataclysm.launcher.ui.DialogUtils;
import cataclysm.launcher.selfupdate.LauncherVersionManager;

import java.io.PrintWriter;

/**
 * Created 21 ���. 2018 �. / 17:14:34 
 * @author Knoblul
 */
public class Main {
	public static void main(String[] args) {
		try {
			// для градла - создаём текстовый файл с версией
			if (args.length > 0 && args[0].equals("-version")) {
				try (PrintWriter pw = new PrintWriter("version.txt")) {
					pw.println(LauncherVersionManager.VERSION);
				}
				return;
			}

			new Launcher(args);
		} catch (Throwable t) {
			DialogUtils.showError("Ошибка", t);
			throw new RuntimeException(t);
		}
	}
}
