package de.yovi.chat.processing.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import org.apache.log4j.Logger;

import de.yovi.chat.processing.DefaultImageResult;
import de.yovi.chat.processing.api.ProcessorPlugin;
import de.yovi.chat.processing.api.ProcessorResult;

public class ImageProcessorPlugin implements ProcessorPlugin {

	private final static Logger logger = Logger.getLogger(ImageProcessorPlugin.class);
	
	private final static String[] EXTENSIONS = new String[] {
			"jpg", "jpeg", "png", "gif", "tif", "tiff", "bmp"
	};
	
	@Override
	public ProcessorResult process(URLConnection connection) {
		logger.debug("file " + connection.getURL().getFile());
		logger.debug("path " + connection.getURL().getPath());
		String name = connection.getURL().getFile();
		if (name == null || name.isEmpty()) {
			name = "image_" + connection.getURL().getHost();
		} else {
			int ixOfSlash = name.lastIndexOf(File.separatorChar);
			name = name.substring(ixOfSlash + 1);
		}
		String contentType = connection.getContentType();
		if (contentType != null && contentType.startsWith("image/")) {
			return new DefaultImageResult(name, connection);
		} else {
			String urlAsString = connection.getURL().toExternalForm();
			String lcUrlAsString = urlAsString.toLowerCase();
			for (String extension : EXTENSIONS) {
				if (lcUrlAsString.endsWith(extension)) {
					return new DefaultImageResult(name, connection);
				}
			}
			return null;
		}
	}
	
	public static void main(String[] args) throws IOException {
		java.net.URL url = new java.net.URL("http://upload.wikimedia.org/wikipedia/commons/9/92/Colorful_spring_garden.jpg");
		new ImageProcessorPlugin().process(url.openConnection());
	}
	
}
