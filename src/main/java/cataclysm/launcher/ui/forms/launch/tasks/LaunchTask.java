package cataclysm.launcher.ui.forms.launch.tasks;

import java.util.function.Consumer;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 05.12.2020 18:48
 *
 * @author Knoblul
 */
public abstract class LaunchTask implements Runnable {
	private Consumer<TaskProgress> progressConsumer;

	void setProgressConsumer(Consumer<TaskProgress> progressConsumer) {
		this.progressConsumer = progressConsumer;
	}

	protected void notifyProgress(TaskProgress progress) {
		if (progressConsumer != null) {
			progressConsumer.accept(progress);
		}
	}
}
