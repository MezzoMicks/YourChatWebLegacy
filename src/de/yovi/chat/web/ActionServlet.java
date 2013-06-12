package de.yovi.chat.web;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.yovi.chat.ChannelInfo;
import de.yovi.chat.ChatUtils;
import de.yovi.chat.api.ActionHandlerRemote;
import de.yovi.chat.api.Friend;
import de.yovi.chat.api.FriendList;
import de.yovi.chat.api.Profile;
import de.yovi.chat.api.ProfileImage;
import de.yovi.chat.api.User;
import de.yovi.chat.client.Translator;
import de.yovi.chat.messaging.MediaSegment;
import de.yovi.chat.system.ActionHandler;
import de.yovi.chat.user.ProfileHandler;

/**
 * Servlet implementation class ActionServlet
 */
@WebServlet
public class ActionServlet extends HttpServlet {

	private final static Logger logger = Logger.getLogger(ActionServlet.class);
	
	private static final long serialVersionUID = 1L;
       
	private enum Action {

		SETTINGS("settings"), REFRESH("refresh"), READPROFILE("readprofile"), INVITE("invite");

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
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActionServlet() {
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
		String actionStr = request.getParameter("action");
		User user = (User) request.getSession().getAttribute("user");
		logger.debug(user + " does "+ actionStr);
		if (actionStr != null && user != null) {
			Action action = Action.getById(actionStr);
			if (action != null) {
				ActionHandlerRemote ah = new ActionHandler();
				response.setContentType("application/json");
				response.setStatus(200);
				response.flushBuffer();
				Writer writer = response.getWriter();
				switch (action) {
				case REFRESH:
					String lang = request.getParameter("lang");
					if (lang == null) {
						lang = "de";
					}
					ChannelInfo info = ah.getChannelInfo(user, null);
					ChannelInfo[] rooms = ah.getRoomList(user);
					String font = user.getFont();
					writeRefresh(info, rooms, font, user.isAway(), lang, writer);
					break;
				case SETTINGS:
					String setFont = request.getParameter("font");
					String setColor = request.getParameter("color");
					String setRoom = request.getParameter("room");
					boolean asyncMode = Boolean.parseBoolean(request.getParameter("asyncmode"));
					ah.setSettings(user, setColor, setFont, setRoom, asyncMode);
					break;
				case INVITE:
					boolean trial = Boolean.parseBoolean(request.getParameter("trial"));
					String key = ah.createInvitation(user, trial);
					writer.write("{\"key\":\"" + key + "\"}");
					break;
				case READPROFILE:
					String profileUser = request.getParameter("user");
					Profile profile = new ProfileHandler().getProfile(user, profileUser);
					if (profile != null) {
						writeProfile(profile, writer);
					}
					break;
				}
				writer.flush();
				response.flushBuffer();
			}
		} else {
			logger.warn("no action or user!");
		}
	}

	private void writeProfile(Profile profile, Writer writer) throws IOException {
		SimpleDateFormat sdf_datetime = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		writer.write('{');
		writer.write("\"name\":");
		writer.write("\"" + ChatUtils.escape(profile.getName()) + "\",");
		writer.write("\"lastlogin\":");
		writer.write("\"" + sdf_datetime.format(profile.getLastLogin()) + "\",");
		if (profile.getImage() != null) {
			writer.write("\"avatartext\":");
			writer.write("\"" + ChatUtils.escape(profile.getImage().getTitle()) + "\",");
			writer.write("\"avatarurl\":");
			writer.write("\"http://placehold.it/160x160\",");
			
//			writer.write("\"data/db.image." + profile.getImage().getID() + ".png\",");
		}
		writer.write("\"about\":");
		writer.write("\"" + ChatUtils.escape(profile.getAbout()) + "\",");
		writer.write("\"birthday\":");
		writer.write("\"" + sdf_datetime.format(profile.getDateOfBirth()) + "\",");
		writer.write("\"location\":");
		writer.write("\"" + profile.getLocation() + "\"");
		if (profile.getCollage() != null) {
			writer.write(',');
			writer.write("\"images\":[");
			boolean first = true;
			for (ProfileImage image : profile.getCollage()) {
				if (!first) {
					writer.write(',');
				} else {
					first = false;
				}
				writer.write("{");
				writer.write("\"title\":\"" + ChatUtils.escape(image.getTitle()) + "\",");
				writer.write("\"url\":\"img/400x300.gif\"");
//				writer.write("\"url\":\"data/db.image." + image.getID() + ".png\"");
				writer.write("}");
			}
			writer.write("]");
		}
		if (profile.getFriendLists() != null) {
			writer.write(',');
			writer.write("\"friendlists\":[");
			boolean first = true;
			for (FriendList list : profile.getFriendLists()) {
				if (!first) {
					writer.write(',');
				} else {
					first = false;
				}
				writer.write("{");
				writer.write("\"title\":\"" + ChatUtils.escape(list.getName()) + "\",");
				writer.write("\"visible\":\"" + list.isVisible() + "\",");
				if (list.getFriends() != null) {
					writer.write("\"friends\":[");
					first = true;
					for (Friend friend : list.getFriends()) {
						if (!first) {
							writer.write(',');
						} else {
							first = false;
						}
						writer.write("{");
						writer.write("\"name\":\"" + ChatUtils.escape(friend.getUserName()) + "\",");
						writer.write("\"lastlogin\":\"" + sdf_datetime.format(profile.getLastLogin()) + "\",");
						writer.write("\"confirmed\":\"" + friend.isConfirmed() + "\",");
						writer.write("\"online\":\"" + friend.isOnline() + "\"");
						writer.write("}");
					}
					writer.write("]");
				}
				writer.write("}");
			}
			writer.write("]");
		}
		writer.write("}");
	}
	
	private void writeRefresh(ChannelInfo info, ChannelInfo[] rooms, String font, boolean away, String lang, Writer writer) throws IOException {
		Translator translator = Translator.getInstance(lang);
		writer.write('{');
		writer.write("\"away\":");
		writer.write("\"" + away + "\",");
		writer.write("\"font\":");
		writer.write("\"" + (font != null ? font.trim() : "null") + "\",");
		if (info != null) {
			writer.write("\"room\":");
			writer.write("\"" + ChatUtils.escape(info.getName()) + "\",");
			writer.write("\"background\":");
			writer.write("\"" + info.getBgColor() + "\",");
			writer.write("\"foreground\":");
			writer.write("\"" + info.getFgColor() + "\",");
			writer.write("\"backgroundimage\":");
			writer.write("\"" + info.getBgImage() + "\",");
			if (info.getUsers() != null) {
				writer.write("\"users\":[");
				
				boolean first = true;
				for (User user : info.getUsers()) {
					if (!first) {
						writer.write(',');
					} else {
						first = false;
					}
					writeUser(writer, user);
				}
				writer.write("],");
			}
			if (info.getMedia() != null) {
				writer.write("\"medias\":");
				writer.write("[");
				boolean first = true;
				for (MediaSegment media : info.getMedia()) {
					if (!first) {
						writer.write(',');
					} else {
						first = false;
					}
					writer.write("{\"link\":\"" + media.getContent() + "\",");
					String name = media.getAlternateName();
					if (name == null) {
						name = media.getContent();
					} else  {
						if (name.charAt(0) == '$') {
							name = translator.translate(name.substring(1));
						}
						name = ChatUtils.escape(name);
					}
					writer.write("\"name\":\"" + name + "\",");
					writer.write("\"preview\":\"" + media.getPreview() + "\",");
					writer.write("\"pinky\":\"" + media.getPinky() + "\",");
					writer.write("\"type\":\"" + media.getType() + "\",");
					writer.write("\"user\":\"" + ChatUtils.escape(media.getUser()) + "\"}");
				}
				writer.write("],");
			}
			if (info.getOtherUsers() != null) {
				writer.write("\"others\":");
				writer.write("[");
				boolean first = true;
				for (User user : info.getOtherUsers()) {
					if (!first) {
						writer.write(',');
					} else {
						first = false;
					}
					writeUser(writer, user);
				}
				writer.write("],");
			}
		}
		if (rooms != null) {
			writer.write("\"rooms\":[");
			boolean first = true;
			for (ChannelInfo room : rooms) {
				if (!first) {
					writer.write(',');
				} else {
					first = false;
				}
				writer.write("{\"name\":\"" + ChatUtils.escape(room.getName()) + "\",");
				writer.write("\"color\":\"" + room.getBgColor() + "\",");
				User[] users = room.getUsers();
				writer.write("\"users\":\"" + (users != null ? users.length : "null") + "\"}");
			}
			writer.write("]");
		}
		writer.write("}");
	}

	private void writeUser(Writer writer, User user) throws IOException {
		writer.write("{");
		writer.write("\"username\":\"" + ChatUtils.escape(user.getUserName()) + "\",");
		writer.write("\"alias\":\"" + ChatUtils.escape(user.getAlias()) + "\",");
		writer.write("\"color\":\"" + user.getColor() + "\",");
		writer.write("\"guest\":\"" + user.isGuest() + "\",");
		writer.write("\"away\":\"" + user.isAway() + "\",");
		writer.write("\"avatar\":\"" + user.getAvatarID() + "\"");
		writer.write("}");
	}
	
	/*
	private static String urlEscape(String input) {
		if (input == null) {
			return null;
		} else {
			try {
				return URLEncoder.encode(input, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				return null;
			}
		}
	}
	*/
	
}
