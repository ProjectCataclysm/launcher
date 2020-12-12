package cataclysm.launcher.ui.forms.launch.tasks;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 05.12.2020 19:06
 *
 * @author Knoblul
 */
public class TaskProgress {
	private final String title;
	private final String message;
	private final double progress;

	public TaskProgress(String title, String message, double progress) {
		this.title = title;
		this.message = message;
		this.progress = progress;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public double getProgress() {
		return progress;
	}
}
