package de.yovi.chat.processing.api;

import java.net.URLConnection;

public interface ProcessorPlugin {

	public ProcessorResult process(URLConnection connection);
	
}
