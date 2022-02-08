package cataclysm;

import cataclysm.launch.Launcher;
import cataclysm.ui.DialogUtils;
import cataclysm.utils.VersionHelper;

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
					pw.println(VersionHelper.VERSION);
				}
				return;
			}

			new Launcher(args);
		} catch (Throwable t) {
			DialogUtils.showError("Ошибка", t);
			System.exit(1);
		}
	}
}
