package de.yovi.chat.messaging;

import de.yovi.chat.api.Info;
import de.yovi.chat.api.Segment;
import de.yovi.chat.processing.api.ContentType;

/**
 * Represents a textual part of a message
 * @author Michi
 *
 */
public class StringSegment implements Segment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6109235839252072231L;
	private String content;
	
	public StringSegment(String content) {
		this.content = content;
	}

	@Override
	public ContentType getType() {
		return ContentType.TEXT;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public Info getInfo() {
		return null;
	}
	
	@Override
	public String getPreview() {
		return null;
	}
	
	@Override
	public String getPinky() {
		return null;
	}
	
	public void append(String content) {
		this.content += ' ' + content;
	}
	
	@Override
	public String getAlternateName() {
		return null;
	}
	

}
