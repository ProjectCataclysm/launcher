package cataclysm.launcher.ui.forms.main.news;

import cataclysm.launcher.ui.controls.LabelField;
import cataclysm.launcher.utils.AsyncTasks;
import cataclysm.launcher.utils.logging.Log;
import com.google.common.collect.Lists;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 03.10.2020 1:27
 *
 * @author Knoblul
 */
public class NewsPane extends StackPane {
	private final VBox contentWrapper;
	private final ProgressIndicator loadingIndicator;
	private final StackPane modalPane;
	private final LabelField errorLabel;

	public NewsPane() {
		getStyleClass().add("news");

		contentWrapper = new VBox();
		contentWrapper.getStyleClass().addAll("padded-pane", "news-container");

		ScrollPane newsPaneScroll = new ScrollPane(contentWrapper);
		newsPaneScroll.getStyleClass().add("news-scroll");
		newsPaneScroll.setFitToWidth(true);
		newsPaneScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

		BorderPane newsScrollWrapper = new BorderPane(newsPaneScroll);
		newsScrollWrapper.getStyleClass().add("news-scroll-wrapper");
		getChildren().add(newsScrollWrapper);

		loadingIndicator = new ProgressIndicator();
		loadingIndicator.setMaxSize(32, 32);

		modalPane = new StackPane();
		modalPane.getStyleClass().add("content-card");

		errorLabel = new LabelField();
		errorLabel.getStyleClass().add("error-modal-label");
		getChildren().add(modalPane);
	}

	@SuppressWarnings("unchecked")
	public void updateNews(CompletableFuture<JSONObject> future) {
		Log.msg("Requesting news...");
		modalPane.getChildren().setAll(loadingIndicator);

		AsyncTasks.whenComplete(future, (response, exception) -> {
			Runnable fxTask;
			if (exception == null) {
				List<JSONObject> newsArray = (JSONArray) response.get("news");
				List<NewsPostElement> postElements = Lists.newArrayList();
				for (JSONObject newsItem : newsArray) {
					NewsPost newsPost = new NewsPost();
					newsPost.parse(newsItem);
					postElements.add(new NewsPostElement(this, newsPost));
				}

				fxTask = () -> {
					contentWrapper.getChildren().setAll(postElements);
					getChildren().remove(modalPane);
				};
			} else {
				Log.warn(exception, "Failed to get news");
				fxTask = () -> {
					errorLabel.setText("Не удалось загрузить новости:\n\n" + exception.toString());
					modalPane.getChildren().setAll(errorLabel);
				};
			}

			Platform.runLater(fxTask);
		});
	}
}
