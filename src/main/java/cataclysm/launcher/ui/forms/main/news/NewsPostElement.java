package cataclysm.launcher.ui.forms.main.news;

import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.utils.PlatformHelper;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 03.10.2020 1:30
 *
 * @author Knoblul
 */
class NewsPostElement extends BorderPane {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private final Label date;
	private final Label text;
	private final Label likes;
	private final Label comments;
	private final Label reposts;
	private final Label views;
	private final Button linkButton;

	public NewsPostElement(NewsPane newsPane, NewsPost post) {
		getStyleClass().addAll("news-post", "content-card");

		date = new Label();
		date.getStyleClass().addAll("post-date");
		date.setMaxWidth(Float.MAX_VALUE);
		HBox.setHgrow(date, Priority.ALWAYS);
		Label latestIndicator;
		setTop(new HBox(latestIndicator = new Label("Новое"), date));
		latestIndicator.getStyleClass().add("latest-indicator");

		text = new Label();
		text.getStyleClass().addAll("post-text");
		text.setWrapText(true);
		text.setMaxWidth(Double.MAX_VALUE);
		linkButton = new IconButton("Подробнее...");
		linkButton.getStyleClass().add("post-link");

		VBox postTextWrapper = new VBox(text, new BorderPane(linkButton));
		postTextWrapper.getStyleClass().add("post-text-wrapper");
		setCenter(postTextWrapper);

		HBox postActions = new HBox(
				likes = new Label("", new MaterialDesignIconView(MaterialDesignIcon.HEART)),
				comments = new Label("", new MaterialDesignIconView(MaterialDesignIcon.COMMENT)),
				reposts = new Label("", new MaterialDesignIconView(MaterialDesignIcon.REPLY)),
				views = new Label("", new MaterialDesignIconView(MaterialDesignIcon.EYE))
		);
		views.getStyleClass().add("views-count");
		views.setMaxWidth(Float.MAX_VALUE);
		HBox.setHgrow(views, Priority.ALWAYS);
		postActions.getStyleClass().addAll("padded-pane", "post-actions");
		setBottom(postActions);

		maxHeightProperty().bind(newsPane.heightProperty().subtract(2));
		setPost(post);
	}

	public void setPost(NewsPost post) {
		long postTimeMillis = post.getDate() * 1000;

		if (System.currentTimeMillis() - postTimeMillis < TimeUnit.DAYS.toMillis(3)) {
			getStyleClass().add("latest-post");
		} else {
			getStyleClass().remove("latest-post");
		}

		date.setText("" + DATE_FORMAT.format(new Date(postTimeMillis)));
		text.setText(post.getText().trim());
		likes.setText(String.valueOf(post.getLikes()));
		comments.setText(String.valueOf(post.getComments()));
		reposts.setText(String.valueOf(post.getReposts()));
		views.setText(String.valueOf(post.getViews()));
		linkButton.setOnAction(event -> PlatformHelper.browseURL(post.getLink()));
	}
}
