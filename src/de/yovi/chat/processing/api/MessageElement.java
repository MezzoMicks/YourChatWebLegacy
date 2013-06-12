package de.yovi.chat.processing.api;

import java.util.List;

/**
 * Representation of a user's message or an element of that
 * @author Michi
 *
 */
public interface MessageElement {

	/**
	 * wether or not this element should be hidden from other chat members
	 * @return
	 */
	public boolean isHidden();
	
	/**
	 * wether this element was processed or not
	 * @return true | false
	 */
	public boolean isProcessed();
	
	/**
	 * String representation of this content
	 * @return {@link String}
	 */
	public String getTextContent();
	
	/**
	 * The result of the Processor if something could be processes
	 * @return {@link ProcessorResult}
	 */
	public ProcessorResult getProcessorResult();
	
	/**
	 * Splits this Element in three parts
	 * Generating a {@link List} of a maximum count of 3 elements
	 * <br>If this elements content is "this is a text" a call with <b>shredElement(5,9)</b> would result in three Elements
	 * <ul>
	 * <li>"this "</li>
	 * <li>"is a"</li>
	 * <li>" text"</li>
	 * </ul>
	 * @param start
	 * @param end
	 * @return
	 */
	public List<MessageElement> shredElement(int start, int end);
	
}
