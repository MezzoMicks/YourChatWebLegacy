package de.yovi.chat.processing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.processing.api.ProcessorResult.ThumbGenerator;
import de.yovi.chat.web.Configurator;

public class WebsiteThumbGenerator implements ThumbGenerator {
	
	private final static Logger logger = Logger.getLogger(WebsiteThumbGenerator.class);

	private static final String BIGIMAGE_PREFIX = "bigimage: ";
	private final String urlAsString;
	private final String title;
	
	public WebsiteThumbGenerator(String urlAsString, String title) {
		this.urlAsString = urlAsString;
		if (title != null) {
			title = title.trim();
			String titleLC = title.toLowerCase();
			if (!titleLC.endsWith(".jpg") && !titleLC.endsWith(".jpeg")) {
				title += ".jpg";
			}
		} else {
			title = "webpreview.jpg";
		}
		this.title = title;
	}
	
	@Override
	public Map<ImageSize, String> generate(ImageSize... imageSizes) throws IOException {
		Map<ImageSize, String> result = new HashMap<ChatUtils.ImageSize, String>(imageSizes.length);
		if (Configurator.isPhantomjsPresent()) {
			File tmpFile = File.createTempFile("web", ".jpg", Configurator.getDataFile());
			logger.info("Creating Preview for website \"" + urlAsString + "\"");
			String renderJSPath = Configurator.getBinDir() + File.separator + "render.js";
			String[] cmds = new String[] { Configurator.getPhantomjs(), renderJSPath, "\"" + urlAsString + "\"", tmpFile.getAbsolutePath() };
			String cmdString = "";
			if (logger.isDebugEnabled()) {
				for (String cmd : cmds) {
					cmdString += cmd + " ";
				}
				logger.debug("Creating thumbnail for website, spawning \"" + cmdString + "\"");
			}
			try {
				// Process exec = Runtime.getRuntime().exec(cmdString);
				ProcessBuilder pb = new ProcessBuilder(Configurator.getPhantomjs(), renderJSPath, urlAsString, tmpFile.getAbsolutePath());
				Process process = pb.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				InputStream inputStream = null;
				String line;
				while ((line = reader.readLine()) != null) {
					logger.debug("exec>" + line);
					if (line.startsWith(BIGIMAGE_PREFIX)) {
						logger.info("got big image, using url " + line);
						inputStream = new URL(line.substring(BIGIMAGE_PREFIX.length())).openStream();
					}
				}
				int exe = process.waitFor();
				logger.debug("exec:" + exe);
				Thread.sleep(100);
				logger.debug("resizing to thumbsize");
				if (inputStream == null) {
					logger.debug("reading inputstream from " + tmpFile.getAbsolutePath());
					inputStream = new FileInputStream(tmpFile);
				}
				if (inputStream != null) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					IOUtils.copy(inputStream, bos);
					ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
					for (ImageSize size : imageSizes) {
						bis.reset();
						String thumbname = ChatUtils.createAndStoreResized(size.getPrefix(), bis, title, size.getSize(), size.getSize(), null);
						result.put(size, "data/" + thumbname);
					}
				}
				if (tmpFile != null) {
					tmpFile.delete();
				}
			} catch (InterruptedException e) {
				logger.error("error while executing phantomjs", e);
			} catch (IOException e) {
				logger.error("error while executing phantomjs", e);
			}
		}
		return result;
	}
}