package de.yovi.chat.processing.api;

/**
 * The type of content, which a MessageElement or Segment may have
 * @author Michi
 *
 */
public enum ContentType {

	/**
	 * The type of content is unknown
	 */
	UNKNOWN, 
	/**
	 * normal Text should be marked as this
	 */
	TEXT, 
	/**
	 * JPG/PNG/GIF..
	 */
	IMAGE, 
	/**
	 * Embedded Videos, FLVs, MPG...
	 */
	VIDEO, 
	/**
	 * URLs which direct to plain old Websites
	 */
	WEBSITE,
	/**
	 * PDF/TXT/DOC-Links should be this
	 */
	DOCUMENT,
	/**
	 * Roomprotocols are marked this way
	 */
	PROTOCOL

}