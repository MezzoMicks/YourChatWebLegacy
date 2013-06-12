package de.yovi.chat.messaging;

import de.yovi.chat.api.Info;
import de.yovi.chat.api.Segment;
import de.yovi.chat.processing.api.ContentType;

public class MediaSegment implements Segment {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2786199409351908730L;
	private final String user;
	private final String content;
	private final ContentType type;
	private final String preview;
	private final String pinky;
	private String alternateName = null;
	private Info info = null;
	
	public MediaSegment(String user, String content, ContentType type, String preview, String pinky) {
		super();
		this.user = user;
		this.content = content;
		this.type = type;
		this.preview = preview;
		this.pinky = pinky;
	}

	/**
	 * The originator of this {@link MediaSegment}
	 * @return {@link String}
	 */
	public String getUser() {
		return user;
	}

	@Override
	public ContentType getType() {
		return type;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public Info getInfo() {
		return info;
	}
	
	@Override
	public String getPreview() {
		return preview;
	}

	@Override
	public String getPinky() {
		return pinky;
	}
	
	public void setInfo(Info info) {
		this.info = info;
	}
	
	@Override
	public String getAlternateName() {
		return alternateName;
	}
	
	public void setAlternateName(String alternateName) {
		this.alternateName = alternateName;
	}
	
}
