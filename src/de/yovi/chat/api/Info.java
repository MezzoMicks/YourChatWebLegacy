package de.yovi.chat.api;

import java.io.Serializable;

/**
 * An Info can be supplied to further describe the nature of a {@link Segment}
 * @author Michi
 *
 */
public interface Info extends Serializable {

	public enum InfoType {
		INFO,WARN,ERROR
	}
	
	/**
	 * The type of this Info
	 * @return {@link InfoType}
	 */
	public InfoType getType();
	
	/**
	 * The text of this info
	 * @return String
	 */
	public String getText();
	
}
