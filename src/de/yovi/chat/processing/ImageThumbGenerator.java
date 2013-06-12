package de.yovi.chat.processing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.processing.api.ProcessorResult.ThumbGenerator;

public class ImageThumbGenerator implements ThumbGenerator {

	private final static Logger logger = Logger.getLogger(ImageThumbGenerator.class);
	
	private final String title;
	private final URL imageURL;
	private transient URLConnection connection;
	private transient InputStream inputStream;
	private transient BufferedImage image;

	public ImageThumbGenerator(String title, String url) throws MalformedURLException {
		this.title = title;
		imageURL = new URL(url);
	}
	public ImageThumbGenerator(String title, URL url) {
		this.title = title;
		imageURL = url;
	}
	public ImageThumbGenerator(String title, URLConnection urlConnection) {
		this.title = title;
		imageURL = urlConnection.getURL();
		connection = urlConnection;
	}
	
	public ImageThumbGenerator(String title, InputStream stream) {
		this.title = title;
		imageURL = null;
		inputStream = stream;
	}
	
	public ImageThumbGenerator(String title, BufferedImage image) {
		this.title = title;
		imageURL = null;
		this.image = image;;
	}
	
	@Override
	public Map<ImageSize, String> generate(ImageSize... imageSizes) throws IOException {
		Map<ImageSize, String> result = new HashMap<ChatUtils.ImageSize, String>(imageSizes.length);
		try {
			String name = title != null ? title : "imagepreview.png";
			if (image != null) {
				for (ImageSize  size : imageSizes) {
					String filename = ChatUtils.createAndStoreResized(size.getPrefix(), image, name, size.getSize(), size.getSize(), null);
					result.put(size,  "data/" + filename);
				}	
			} else {
				if (inputStream == null) {
					if (connection == null) {
						connection = imageURL.openConnection();
					}
					inputStream = connection.getInputStream();
				}
				if (inputStream == null) {
					logger.error("couldn't open Stream for Image: " + title);
				} else {
					ByteArrayInputStream bis;
					if (inputStream instanceof ByteArrayInputStream) {
						bis = (ByteArrayInputStream) inputStream;
					} else {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						IOUtils.copy(inputStream, bos);
						inputStream.close();
						inputStream = null;
						bis = new ByteArrayInputStream(bos.toByteArray());
					}
					for (ImageSize  size : imageSizes) {
						bis.reset();
						String filename = ChatUtils.createAndStoreResized(size.getPrefix(), bis, name, size.getSize(), size.getSize(), null);
						result.put(size,  "data/" + filename);
					}	
				}
			}
		} catch (IOException e) {
			logger.error("couldn't open Stream for URL: " + imageURL, e);
		}
		return result;
	}
	
}
