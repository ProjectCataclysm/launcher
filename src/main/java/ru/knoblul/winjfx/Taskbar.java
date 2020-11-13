package ru.knoblul.winjfx;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 11.10.2020 18:46
 *
 * @author Knoblul
 */
public class Taskbar {
	private static final boolean TASKBAR_PROGRESS_SUPPORTED = WinJFX.isLibrarySupported()
			&& WinJFX.isTaskbarProgressSupported();

	private final WindowHandleAccessor windowHandleAccessor;
	private long taskbarHandle;
	private long progressCompleted;
	private long progressTotal;
	private TaskbarProgressState progressState = TaskbarProgressState.OFF;

	public Taskbar(WindowHandleAccessor windowHandleAccessor) {
		this.windowHandleAccessor = windowHandleAccessor;

		if (TASKBAR_PROGRESS_SUPPORTED) {
			windowHandleAccessor.handleProperty().addListener((observable, oldValue, newValue) -> {
				destroy();
				taskbarHandle = WinJFX.createTaskbar();
			});
		}
	}

	public TaskbarProgressState getProgressState() {
		return progressState;
	}

	public void setProgressState(TaskbarProgressState progressState) {
		if (!TASKBAR_PROGRESS_SUPPORTED) {
			return;
		}

		if (taskbarHandle == 0) {
			throw new IllegalStateException("Taskbar destroyed");
		}

		if (this.progressState != progressState) {
			this.progressState = progressState;
			WinJFX.setTaskbarProgressState(windowHandleAccessor.getHandle(), taskbarHandle, progressState.ordinal());
		}
	}

	public void setProgress(long completed, long total) {
		if (!TASKBAR_PROGRESS_SUPPORTED) {
			return;
		}

		if (taskbarHandle == 0) {
			throw new IllegalStateException("Taskbar destroyed");
		}

		if (this.progressCompleted != completed || this.progressTotal != total) {
			this.progressCompleted = completed;
			this.progressTotal = total;
			WinJFX.setTaskbarProgress(windowHandleAccessor.getHandle(), taskbarHandle, completed, total);
		}
	}

	public void destroy() {
		if (!TASKBAR_PROGRESS_SUPPORTED) {
			return;
		}

		if (taskbarHandle != 0) {
			WinJFX.destroyTaskbar(taskbarHandle);
			taskbarHandle = 0;
		}
	}

	public enum TaskbarProgressState {
		OFF,
		INDETERMINATE,
		NORMAL,
		ERROR,
		PAUSED
	}
}
