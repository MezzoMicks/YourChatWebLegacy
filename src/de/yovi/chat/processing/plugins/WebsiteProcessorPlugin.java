package de.yovi.chat.processing.plugins;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;

import de.yovi.chat.processing.ImageThumbGenerator;
import de.yovi.chat.processing.WebsiteThumbGenerator;
import de.yovi.chat.processing.api.ContentType;
import de.yovi.chat.processing.api.ProcessorPlugin;
import de.yovi.chat.processing.api.ProcessorResult;

public class WebsiteProcessorPlugin implements ProcessorPlugin {

	private final static Logger logger = Logger.getLogger(WebsiteProcessorPlugin.class);
	private final static int MIN_SIZE = 450 * 450;
	private final Pattern titlePattern = Pattern.compile("<title>(.*)</title>", Pattern.CASE_INSENSITIVE);
	private final Pattern imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
	
	@Override
	public ProcessorResult process(URLConnection connection) {
		String contentType = connection.getContentType();
		if (contentType != null && contentType.startsWith("text/html")) {
			URL url = connection.getURL();
			try {
				String title = null;
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				int length = connection.getContentLength();
				StringBuilder fullText = length > 0  ? new StringBuilder(length) : new StringBuilder(100 * 1024);
				while ((line = br.readLine()) != null) {
					Matcher matcher = titlePattern.matcher(line);
					if (matcher.find()) {
						String group = matcher.group(1);
						if (group.length() > 40) {
							title = group.substring(0, 39) + "...";
						} else {
							title = group;
						}
					}
					fullText.append(line);
				}
				Matcher matcher = imgPattern.matcher(fullText.toString());
				Set<String> imageSrcs = new HashSet<String>();
				while (matcher.find()) {
					imageSrcs.add(matcher.group(1));
//					for (int i = 0; i < matcher.groupCount(); i++) {
//					}
				}
				Set<ImageInputStream> streams = new HashSet<ImageInputStream>(imageSrcs.size());
				ImageReader bigImageReader = null;
				for (String src : imageSrcs) {
					URL srcURL;
					// Fully qualified?
					if (src.startsWith("http://")) {
						srcURL = new URL(src);
					// also, but without protocol?
					} else if (src.startsWith("//")) {
						srcURL = new URL("http:" + src);
					//  absolute serverpath?
					} else if (src.startsWith("/")) {
						srcURL = new URL(url.getProtocol() + "://" + url.getHost() + src);
					// relative serverpath?
					} else {
						String parent = url.getPath();
						int lastIxOfSlash = parent.lastIndexOf('/');
						if (lastIxOfSlash > 0) {
							parent = parent.substring(0, lastIxOfSlash);
						}
						srcURL = new URL(url.getProtocol() + "://" + url.getHost() + "/" + parent + "/" + src);
					}
					// Fetch the Image and evaluate it's size!
					ImageInputStream in = ImageIO.createImageInputStream(srcURL.openStream());
					streams.add(in);
					int size = -1;
					final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
					if (readers.hasNext()) {
						ImageReader reader = readers.next();
						reader.setInput(in);
						size = reader.getWidth(0) * reader.getHeight(0);
						if (size > MIN_SIZE) {
							if (bigImageReader != null) {
								logger.debug("More than one big image found!");
								bigImageReader.dispose();
								bigImageReader = null;
								break;
							} else {
								logger.debug("Big image found... taking it");
								bigImageReader = reader;
							}
						} else {
							reader.dispose();
						}
					}
//					BufferedImage image = ImageIO.read(srcURL);
					if (size >= MIN_SIZE) {
						System.out.println(src + " has " + size);
					}
				}
				WebsiteProcessorResult result;
				if (bigImageReader != null) {
					BufferedImage image = bigImageReader.read(0);
					result = new WebsiteProcessorResult(image, url.toExternalForm(), title);
				} else {
					result = new WebsiteProcessorResult(url.toExternalForm(), title);
				}
				for (ImageInputStream stream : streams) {
					stream.close();
				}
				return result;
			} catch (IOException e) {
				logger.error(e);
				return null;
			}
		} else {
			return null;
		}
	}
	
	private class WebsiteProcessorResult implements ProcessorResult {
		
		private final String urlAsString;
		private final String title;
		private transient BufferedImage image = null;
		
		public WebsiteProcessorResult(BufferedImage image, String urlAsString, String title) {
			this.image = image;
			this.urlAsString = urlAsString;
			this.title = title;
		}
		
		public WebsiteProcessorResult(String urlAsString, String title) {
			this.urlAsString = urlAsString;
			this.title = title;
		}
		
		@Override
		public String getTitle() {
			return title;
		}
		
		@Override
		public ThumbGenerator getThumbGenerator() {
			if (image != null) {
				return new ImageThumbGenerator(title, image);
			} else {
				return new WebsiteThumbGenerator(urlAsString, title);
			}
		}
		
		@Override
		public ContentType getType() {
			return ContentType.WEBSITE;
		}
		
	}
 }
