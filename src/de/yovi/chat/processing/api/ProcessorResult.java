package de.yovi.chat.processing.api;

import java.io.IOException;
import java.util.Map;

import de.yovi.chat.ChatUtils.ImageSize;


public interface ProcessorResult {

	/**
	 * The type of this Results content
	 * @return {@link ContentType}
	 */
	public ContentType getType();
	
	/**
	 * The title of this content, if one is present
	 * @return {@link String} or null
	 */
	public String getTitle();
	
	/**
	 * Operation to be executed, when the actual thumbnail should be created, by this the actual sending of the message won't be delayed
	 * and the overhead of generating a thumbnail can be done afterwards by a separate thread
	 * <br>It is recommended to put all work, that can be done after the type of content has been identified, in here
	 * <br>The Subsystem will process these operations after the message has been pushed into the channel
	 * @return {@link Runnable}
	 */
	public ThumbGenerator getThumbGenerator();
	
	/**
	 * Generator-Object for Thumbnails, each Plugin may implement it's on thumbnail-generation or retrieval algorithm
	 */
	public interface ThumbGenerator {
		
		/**
		 * Generates a thumbnail for the supplied size (if possible, may also return something that's about 'similiar')
		 * @param width
		 * @param height
		 * @return
		 * @throws IOException
		 */
		public Map<ImageSize, String>generate(ImageSize... imageSizes) throws IOException;

	}
	
}
