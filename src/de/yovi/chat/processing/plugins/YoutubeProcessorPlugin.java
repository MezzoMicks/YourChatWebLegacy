package de.yovi.chat.processing.plugins;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.processing.api.ContentType;
import de.yovi.chat.processing.api.ProcessorPlugin;
import de.yovi.chat.processing.api.ProcessorResult;

public class YoutubeProcessorPlugin implements ProcessorPlugin {

	private final static Logger logger = Logger.getLogger(YoutubeProcessorPlugin.class);
	
	@Override
	public ProcessorResult process(URLConnection connection) {
		ProcessorResult result = null;
		URL url = connection.getURL();
		if (url.getHost().contains("youtube.com")) {
			String query = url.getQuery();
			int ixOfVideo = query != null ? query.indexOf("v=") : -1;
			if (ixOfVideo >= 0) {
				String id = query.substring(ixOfVideo + 2);
				int nextParam = id.indexOf('&');
				if (nextParam >= 0) {
					id = query.substring(0, nextParam);
				}
				result = new YoutubeProcessorResult(id);
			}
		}
		return result;
	}
	
	private class YoutubeProcessorResult implements ProcessorResult {
		
		private final String vid;
		
		public YoutubeProcessorResult(String vid) {
			this.vid = vid;
		}
		
		@Override
		public ThumbGenerator getThumbGenerator() {
			return new ThumbGenerator() {
				
				@Override
				public Map<ImageSize, String> generate(ImageSize... imageSizes) {
					Map<ImageSize, String> result = new HashMap<ChatUtils.ImageSize, String>();
					for (ImageSize size : imageSizes) {
						String name;
						switch (size) {
						case PREVIEW:
							name = "http://img.youtube.com/vi/" + vid + "/0.jpg";
							break;
						case PINKY:
							name = "http://img.youtube.com/vi/" + vid + "/default.jpg";
							break;
						default:
							name = null;
							break;
						}
						result.put(size, name);
					}
					return result;
				}
			};
		}
		
		@Override
		public ContentType getType() {
			return ContentType.VIDEO;
		}
		
		@Override
		public String getTitle() {
			return null;
		}
		
	}
	
}
