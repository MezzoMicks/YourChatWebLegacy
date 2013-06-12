package de.yovi.chat.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.api.User;
import de.yovi.chat.communication.InputHandler;
import de.yovi.chat.communication.InputHandlerRemote;
import de.yovi.chat.user.ProfileHandler;
import de.yovi.chat.user.ProfileHandlerRemote;

public class InputServlet extends HttpServlet {

	private final static Logger logger = Logger.getLogger(InputServlet.class);

	private final ServletFileUpload upload;

	private static final int THRESHOLD_SIZE = 1024 * 1024 * 50;    // 1MB
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 50;    // 5MB
	private static final int REQUEST_SIZE = 1024 * 1024 * 51;    // 6MB

	private enum Action {

		TALK("talk"), WHISPER("whisper"), JOIN("join"), AWAY("away"), AVATAR("avatar"), GALLERY("gallery"), PROFILE("profile");

		private final String id;

		private Action(String id) {
			this.id = id;
		}

		private static Action getById(String id) {
			if (id != null) {
				id = id.trim().toLowerCase();
				for (Action value : values()) {
					if (value.id.equals(id)) {
						return value;
					}
				}
			}
			return null;
		}
	}

	public InputServlet() {
		super();
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(THRESHOLD_SIZE);
		factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
		upload = new ServletFileUpload(factory);
		upload.setFileSizeMax(MAX_FILE_SIZE);
		upload.setSizeMax(REQUEST_SIZE);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	private void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user = (User) request.getSession().getAttribute("user");
		// String actionId = request.getParameter("action");
		if (user != null) {
			String message = null;
			String title = null;
			String description = null;
			String actionId = null;
			String timestamp = null;
			String uploadName = null;
			String image = null;
			InputStream uploadStream = null;
			if (ServletFileUpload.isMultipartContent(request)) {
				try {
					List<FileItem> items = upload.parseRequest(request);
					for (FileItem item : items) {
						if (item.isFormField()) {
							// Process regular form field input
							String fieldName = item.getFieldName();
							if ("action".equals(fieldName)) {
								actionId = item.getString();
							} else if ("message".equals(fieldName)) {
								message = item.getString("UTF-8");
							} else if ("timestamp".equals(fieldName)) {
								timestamp = item.getString();
							} else if ("title".equals(fieldName)) {
								title = item.getString("UTF-8");
							} else if ("description".equals(fieldName)) {
								description = item.getString("UTF-8");
							} else if ("image".equals(fieldName)) {
								image = item.getString();
							}
						} else {
							// Process form file field (input type="file").
							uploadName = ChatUtils.replaceSpecialChars(item.getName());
							uploadStream = item.getInputStream();
						}
					}
				} catch (Exception e) {
					logger.error(new ServletException("Cannot parse multipart request.", e));
				}
			} else {
				actionId = request.getParameter("action");
				message = request.getParameter("message");
				timestamp = request.getParameter("timestamp");
				title = request.getParameter("title");
				description = request.getParameter("description");
			}
			logger.debug(user + " does " + actionId + " at " + timestamp);
			Action action = Action.getById(actionId);
			if (action != null) {
				ProfileHandlerRemote ph = new ProfileHandler();
				InputHandlerRemote ih = new InputHandler();
				switch (action) {
				case TALK:
					if (logger.isDebugEnabled()) {
						logger.debug(user + " says " + message);
					}
					if (uploadStream != null && uploadName != null) {
						if (message == null || message.trim().isEmpty()) {
							message = ">";
						}
						ih.talk(user, message, uploadStream, uploadName);
					} else {
						ih.talk(user, message, null, null);
					}
					break;
				case PROFILE:
					if (uploadStream != null && uploadName != null) {
						if (logger.isDebugEnabled()) {
							logger.debug(user + " adds image");
						}
						Long newID = ph.addProfileImage(user, uploadStream, uploadName, title, description, "avatar".equals(image));
						response.getWriter().write(newID != null ? newID.toString() : "null");
					} else {
						String field = request.getParameter("field");
						if (field.equals("about")) {
							String about = request.getParameter("value");
							if (about != null) {
								if (logger.isDebugEnabled()) {
									logger.debug(user + " sets about");
								}
								ph.setAbout(user, about);
							}
						} else if (field.equals("location")) {
							String location = request.getParameter("value");
							if (location != null) {
								if (logger.isDebugEnabled()) {
									logger.debug(user + " sets location");
								}
								ph.setLocation(user, location);
							}
						} else if (field.equals("gender")) {
							String gender = request.getParameter("value");
							if (gender != null) {
								if (logger.isDebugEnabled()) {
									logger.debug(user + " sets gender");
								}
								try {
									ph.setGender(user, Integer.parseInt(gender));
								} catch (NumberFormatException e) {
									logger.warn(e);
								}
							}
						} else if (field.equals("birthday")) {
							String birthday = request.getParameter("value");
							if (birthday != null) {
								if (logger.isDebugEnabled()) {
									logger.debug(user + " sets birthday");
								}
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
								try {
									ph.setBirthday(user, sdf.parse(birthday));
								} catch (ParseException e) {
									logger.warn(e);
								}
							}
						} else if (field.equals("delete")) {
							String id = request.getParameter("value");
							if (id != null) {
								if (logger.isDebugEnabled()) {
									logger.debug(user + " does delete");
								}
								try {
									ph.deleteImage(user, Long.parseLong(id));
								} catch (NumberFormatException e) {
									logger.warn(e);
								}
							}
						}
					}
					break;
//				case AVATAR:
//					if (logger.isDebugEnabled()) {
//						logger.debug(user + " adds " + filename + " as avatar");
//					}
//					ph.addProfileImage(user, filename, title, description, true);
//					break;
//				case GALLERY:
//					if (logger.isDebugEnabled()) {
//						logger.debug(user + " adds " + filename + " as galleryimage");
//					}
//					ph.addProfileImage(user, filename, title, description, false);
//					break;
				case JOIN:
					if (logger.isDebugEnabled()) {
						logger.debug(user + " joins "
								+ request.getParameter("room"));
					}
					String room = request.getParameter("room");
					ih.join(user, StringEscapeUtils.unescapeHtml(room));
					break;
				case AWAY:
					if (logger.isDebugEnabled()) {
						logger.debug(user + " sets away "
								+ request.getParameter("state"));
					}
					ih.setAway(user,
							Boolean.parseBoolean(request.getParameter("state")));
					break;
				case WHISPER:
					if (logger.isDebugEnabled()) {
						logger.debug(user + " whispers to "
								+ request.getParameter("user") + " : "
								+ request.getParameter("message"));
					}
					ih.whisper(user, request.getParameter("user"), request.getParameter("message"), uploadStream, uploadName);
					break;
				}
			}
		} else {
			logger.warn("no action or user!");
		}
	}
	
	public static void main(String[] args) {
		
		System.out.println(StringEscapeUtils.escapeJava("Bildschirmfoto Prüfungsergebnisse#Lübeck-Mozilla_Firefox.png"));
		System.out.println(StringEscapeUtils.escapeHtml("Bildschirmfoto Prüfungsergebnisse#Lübeck-Mozilla_Firefox.png"));
		
	}
	
}
