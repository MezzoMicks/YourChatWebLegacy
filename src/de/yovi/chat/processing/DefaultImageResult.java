package de.yovi.chat.processing;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import de.yovi.chat.processing.api.ContentType;
import de.yovi.chat.processing.api.ProcessorResult;

public class DefaultImageResult implements ProcessorResult {
	
	private final String title;
	private final URL imageURL;
	private transient URLConnection imageURLConnection;
	
	public DefaultImageResult(String title, String url) throws MalformedURLException {
		this(title, new URL(url));
	}

	public DefaultImageResult(String title, URL url) {
		this.imageURL = url;
		this.title = title;
	}
	
	public DefaultImageResult(String title, URLConnection imageURLConnection) {
		this.imageURL = imageURLConnection.getURL();
		this.imageURLConnection = imageURLConnection;
		this.title = title;
	}

	@Override
	public ContentType getType() {
		return ContentType.IMAGE;
	}
	
	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public ThumbGenerator getThumbGenerator() {
		if (imageURLConnection != null) {
			return new ImageThumbGenerator(title, imageURLConnection);
		} else {
			return new ImageThumbGenerator(title, imageURL);
		}
	}
	
}
