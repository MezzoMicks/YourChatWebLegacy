package de.yovi.chat.processing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.processing.api.ProcessorResult.ThumbGenerator;
import de.yovi.chat.web.Configurator;

public class VideoThumbGenerator implements ThumbGenerator {

	private final static Logger logger = Logger.getLogger(VideoThumbGenerator.class);
	
	private final String title;
	private final URL videoURL;
	private transient URLConnection urlConnection;
	private transient InputStream inputStream;

	public VideoThumbGenerator(String title, String url) throws MalformedURLException {
		this.title = title;
		videoURL = new URL(url);
	}
	public VideoThumbGenerator(String title, URL url) {
		this.title = title;
		videoURL = url;
	}
	public VideoThumbGenerator(String title, URLConnection urlConnection) {
		this.title = title;
		this.urlConnection = urlConnection;
		videoURL = urlConnection.getURL();
	}
	
	public VideoThumbGenerator(String title, InputStream stream) {
		this.title = title;
		videoURL = null;
		inputStream = stream;
	}
	
	@Override
	public Map<ImageSize, String> generate(ImageSize... imageSizes) throws IOException {
		Map<ImageSize, String> result = new HashMap<ChatUtils.ImageSize, String>(imageSizes.length);
		if (Configurator.isFFMPEGThumbnailerPresent()) {
			File sourceFile = null;
			boolean deleteSource = false;
			if (videoURL != null) {
				if ("file".equals(videoURL.getProtocol())) {
					try {
						sourceFile = new File(videoURL.toURI());
					} catch (URISyntaxException e) {
						logger.error("Invalid videoSource!", e);
					}
				} else {
					String name = videoURL.getPath();
					int lastIxOfSlash = name.lastIndexOf('/');
					if (lastIxOfSlash >= 0) {
						name = name.substring(lastIxOfSlash);
					}
					sourceFile = File.createTempFile("source", title, Configurator.getDataFile());
					FileOutputStream fos = new FileOutputStream(sourceFile);
					if (urlConnection == null) {
						urlConnection = videoURL.openConnection();
					}
					IOUtils.copy(urlConnection.getInputStream(), fos);
					fos.close();
					deleteSource = true;
				}
			} else if (inputStream != null) {
				sourceFile = File.createTempFile("source", title, Configurator.getDataFile());
				FileOutputStream fos = new FileOutputStream(sourceFile);
				IOUtils.copy(inputStream, fos);
				fos.close();
				deleteSource = true;
			} 
			
			if (sourceFile == null) {
				logger.warn("Couldn't get sourcefile for " + title);
				sourceFile = null;
			} else {
				logger.info("Creating Preview for video \"" + sourceFile.getAbsolutePath() + "\"");
				File tmpFile = File.createTempFile("video", ".jpg", Configurator.getDataFile());
				String videoFileArg = "-i\"" + sourceFile.getAbsolutePath() + "\"";
				String tmpFileArg = "-o\"" + tmpFile.getAbsolutePath() + "\"";
				String stripeArg = "-f";
				String timeArg = "-t25%";
				String sizeArg = "-s640";
				String[] cmds = new String[] {
						Configurator.getFFMPEGThumbnailer(),
						videoFileArg,
						tmpFileArg,
						stripeArg,
						timeArg,
						sizeArg
				};
				String cmdString = "";
				if (logger.isDebugEnabled()) {
					for (String cmd : cmds) {
						cmdString += cmd + " ";
					}
					logger.debug("Creating thumbnail for video, spawning \"" + cmdString + "\"");
				}
				try {
					ProcessBuilder pb = new ProcessBuilder(Configurator.getFFMPEGThumbnailer(), "-i", sourceFile.getAbsolutePath(), "-o", tmpFile.getAbsolutePath(), stripeArg, timeArg, sizeArg);
					Process process = pb.start();
					int exe = process.waitFor();
					logger.debug("exec:" + exe);
					Thread.sleep(100);
					logger.debug("resizing to thumbsize");
					logger.debug("reading inputstream from " + tmpFile.getAbsolutePath());
					InputStream inputStream = new FileInputStream(tmpFile);
					if (inputStream != null) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						IOUtils.copy(inputStream, bos);
						bos.close();
						ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
						result = new ImageThumbGenerator(title, bis).generate(imageSizes);
					}
					if (tmpFile != null) {
						tmpFile.delete();
					}
					if (deleteSource && sourceFile != null) {
						sourceFile.delete();
					}
				} catch (InterruptedException e) {
					logger.error("error while executing ffmpegthumbnailer", e);
				} catch (IOException e) {
					logger.error("error while executing ffmpegthumbnailer", e);
				}
			}
		}
		return result;
	}
	
}
