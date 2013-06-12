package de.yovi.chat.processing.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.yovi.chat.processing.VideoThumbGenerator;
import de.yovi.chat.processing.api.ContentType;
import de.yovi.chat.processing.api.ProcessorPlugin;
import de.yovi.chat.processing.api.ProcessorResult;

public class VideoProcessorPlugin implements ProcessorPlugin {

	private final static Logger logger = Logger.getLogger(VideoProcessorPlugin.class);
	
	@Override
	public ProcessorResult process(URLConnection connection) {
		String contentType = connection.getContentType();
		if (contentType != null && contentType.startsWith("video")) {
			String name = connection.getURL().getFile();
			if (name == null || name.isEmpty()) {
				name = "video_" + connection.getURL().getHost();
			} else {
				int ixOfSlash = name.lastIndexOf(File.separatorChar);
				name = name.substring(ixOfSlash + 1);
			}
			return new VideoProcessorResult(connection, name);
		} else {
			return null;
		}
	}
	
	private class VideoProcessorResult implements ProcessorResult {
		
		private final URLConnection videoConnection;
		private final String title;
		
		public VideoProcessorResult( URLConnection videoConnection, String title) {
			this.videoConnection = videoConnection;
			this.title = title;
		}
		
		@Override
		public String getTitle() {
			return title;
		}
		
		@Override
		public ThumbGenerator getThumbGenerator() {
			return new VideoThumbGenerator(title, videoConnection);
		}
		
		@Override
		public ContentType getType() {
			return ContentType.VIDEO;
		}
		
	}
 }
