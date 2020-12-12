package cataclysm.launcher.ui.forms.launch.tasks;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 05.12.2020 18:48
 *
 * @author Knoblul
 */
public class CheckGameFilesTask extends LaunchTask {
	@Override
	public void run() {
		notifyProgress(new TaskProgress("Проверка", "Проверяем файлы...", 0.0));
		try {
			Thread.sleep(10_000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
