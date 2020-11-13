package cataclysm.launcher.ui.forms.main.news;

import org.json.simple.JSONObject;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 03.10.2020 1:31
 *
 * @author Knoblul
 */
class NewsPost {
	private String text;
	private int likes;
	private int comments;
	private int reposts;
	private int views;
	private long date;
	private String link;

	public void parse(JSONObject json) {
		text = (String) json.get("text");
		likes = ((Number) json.get("likes")).intValue();
		comments = ((Number) json.get("comments")).intValue();
		reposts = ((Number) json.get("reposts")).intValue();
		views = ((Number) json.get("views")).intValue();
		date = ((Number) json.get("date")).longValue();
		link = (String) json.get("link");
	}

	public String getText() {
		return text;
	}

	public int getLikes() {
		return likes;
	}

	public int getComments() {
		return comments;
	}

	public int getReposts() {
		return reposts;
	}

	public int getViews() {
		return views;
	}

	public long getDate() {
		return date;
	}

	public String getLink() {
		return link;
	}
}
