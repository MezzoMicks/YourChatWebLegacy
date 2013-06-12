package de.yovi.chat.api;

import java.io.Serializable;

import de.yovi.chat.processing.api.ContentType;

public interface Segment extends Serializable {

	/**
	 * The type of this Segment
	 * @return {@link ContentType}
	 */
	public ContentType getType();
	
	/**
	 * The actual content of this Segment
	 * @return String
	 */
	public String getContent();
	
	/**
	 * Info which further describes this Segment
	 * @return Info or null
	 */
	public Info getInfo();
	
	/**
	 * Thumbnail for content, if present
	 * @return URL to thumbnail or null
	 */
	public String getPreview();
	
	/**
	 * Thumbnail for content, if present
	 * @return URL to thumbnail or null
	 */
	public String getPinky();
	
	
	public String getAlternateName();
	
}
