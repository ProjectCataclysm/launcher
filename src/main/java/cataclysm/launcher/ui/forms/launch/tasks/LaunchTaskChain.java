package cataclysm.launcher.ui.forms.launch.tasks;

import cataclysm.launcher.utils.AsyncTasks;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 05.12.2020 18:50
 *
 * @author Knoblul
 */
public class LaunchTaskChain {
	private static final List<LaunchTask> tasks;

	static {
		List<LaunchTask> taskRegistry = Lists.newArrayList();
		taskRegistry.add(new CheckGameFilesTask());
		taskRegistry.add(new DownloadClientFiles());
		taskRegistry.add(new LaunchGameTask());
		tasks = Collections.unmodifiableList( taskRegistry);
	}

	public CompletableFuture<Void> executeChain(Consumer<TaskProgress> progressConsumer) {
		return CompletableFuture.allOf(tasks.stream().map(task -> supplyTask(task, progressConsumer))
				.toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<Void> supplyTask(LaunchTask task, Consumer<TaskProgress> progressConsumer) {
		task.setProgressConsumer(progressConsumer);
		return CompletableFuture.runAsync(task, AsyncTasks.executor);
	}
}
