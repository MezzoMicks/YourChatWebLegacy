package de.yovi.chat.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.api.Message;
import de.yovi.chat.api.User;
import de.yovi.chat.client.Translator;
import de.yovi.chat.system.ActionHandler;
import de.yovi.chat.system.FileStorage;
import de.yovi.chat.user.ProfileHandler;

public class DataServlet extends HttpServlet {

	private final static Logger logger = Logger.getLogger(DataServlet.class);
	
	private static final long serialVersionUID = 1L;
    private final static String DB_IMAGE_PREFIX = "db.image."; 
    private final static String PREVIEW_PREFIX = "preview.";   
    private final static String THUMB_PREFIX = "thumb.";   
    private final static String PINKY_PREFIX = "pinky.";   
	
	private enum Type {
		TXT("txt"), HTML("html");

		private final String id;

		private Type(String id) {
			this.id = id;
		}

		private static Type getById(String id) {
			if (id != null) {
				id = id.trim().toLowerCase();
				for (Type value : values()) {
					if (value.id.equals(id)) {
						return value;
					}
				}
			}
			return HTML;
		}
	}
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DataServlet() {
        super();
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}
	
	private void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user = (User) request.getSession().getAttribute("user");
		if (user != null ) {
			String protocol = request.getParameter("protocol");
			if (protocol != null) {
				Type type = Type.getById(request.getParameter("type"));
				String lang = request.getParameter("lang");
				if (lang == null) {
					lang = "de";
				}
				ActionHandler ah = new ActionHandler();
				Message[] messages = ah.getProtocol(user, protocol);
				if (messages != null) {
					response.setStatus(200);
					PrintWriter writer = response.getWriter();
					Translator translator = Translator.getInstance(lang);
					String title = translator.translate("PROTOCOL{room=" + protocol + "}");
					if (type == Type.HTML) {
						response.setContentType("text/html");
						writer.write("<html><head><title>");
						writer.write(title);
						writer.write("</title></head><body");
						String font = user.getFont();
						if (font != null && !"null".equals(font)) {
							writer.write(" style=\"font-style:");
							writer.write(font);
							writer.write("\"");
						}
						writer.write(">");
						writer.write("<h1>");
						writer.write(title);
						writer.write("</h1>");
					} else {
						response.setContentType("text");
						writer.write(title);
						writer.write("\n");
						for (int i = 0; i < title.length(); i++) {
							writer.write("-");
						}
						writer.write("\n");
					}
					ListenerServlet.decodeMessages(writer, messages, translator, type == Type.HTML, false);
					if (type == Type.HTML) {
						writer.write("</body></html>");
					}
				} else {
					response.setStatus(401);
				}				
			} else {
				String url = request.getPathTranslated();
				int slash = url.lastIndexOf(File.separator);
				String id = url.substring(slash + 1);
				InputStream result = null;
				long length = 0;
				if (id != null && !"null".equals(id)) {
					logger.debug("user " + user + " fetches " + id);
					if (!id.contains("../")) {
						if (id.startsWith(DB_IMAGE_PREFIX)) {
							String shortenedId = id.substring(DB_IMAGE_PREFIX.length());
							ImageSize size;
							if (shortenedId.startsWith(THUMB_PREFIX)) {
								size = ImageSize.THUMBNAIL;
								shortenedId = shortenedId.substring(THUMB_PREFIX.length());
							} else if (shortenedId.startsWith(PREVIEW_PREFIX)) {
								size = ImageSize.PREVIEW;
								shortenedId = shortenedId.substring(PREVIEW_PREFIX.length());
							} else if (shortenedId.startsWith(PINKY_PREFIX)){
								size = ImageSize.PINKY;
								shortenedId = shortenedId.substring(PINKY_PREFIX.length());
							} else {
								size = ImageSize.ORIGINAL;
							}
							long imageid = Long.valueOf(shortenedId);
							byte[] data = new ProfileHandler().getProfileImage(imageid, size);
							if (data != null) {
								length = data.length;
								response.setStatus(200);
								response.setContentType("image/png");
								response.setContentLength((int)length);
								if (logger.isDebugEnabled()) {
									logger.debug("sending image " + imageid + " to " + user);
								}
								IOUtils.copy( new ByteArrayInputStream(data), response.getOutputStream());
							} else {
								sendNullFile(response, user);
							}
						} else {
							// A file will definately be send!
							response.setStatus(200);
							response.setContentType("image/png");
							FileStorage storage = FileStorage.getInstance();
							if (!storage.load(response.getOutputStream(), id)) {
								sendNullFile(response, user);
							} else {
								if (logger.isDebugEnabled()) {
									logger.debug("did send file for id = " + id + " to " + user);
								}
							}
						}
					} else {
						logger.warn("not good! user " + user + " tried " + id);
					}
				}
			}
		}
	}

	private void sendNullFile(HttpServletResponse response, User user)
			throws IOException, FileNotFoundException {
		long length;
		File nullFile = new File(Configurator.getBaseDir(), "null.png");
		length = nullFile.length();
		response.setContentLength((int)length);
		if (logger.isDebugEnabled()) {
			logger.debug("sending file " + nullFile.getAbsolutePath() + " to " + user);
		}
		IOUtils.copy(new FileInputStream(nullFile), response.getOutputStream());
	}

	
}
