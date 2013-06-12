package de.yovi.chat.web;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import de.yovi.chat.api.PrivateMessage;
import de.yovi.chat.api.User;
import de.yovi.chat.communication.PrivateMessageHandler;
import de.yovi.chat.communication.PrivateMessageHandlerRemote;

@WebServlet
public class MessageServlet extends HttpServlet {


	private final static Logger logger = Logger.getLogger(MessageServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private enum Action {
		
		INBOX("inbox"), OUTBOX("outbox"), READ("read"), SEND("send"), DELETE("delete");
		
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
	
	public MessageServlet() {
		super();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doRequest(req, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doRequest(req, resp);
	}
	
	/**
	 * Internal evaluation of the request, regardless if it's a POST or a GET
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user = (User) request.getSession().getAttribute("user");
		String actionId = request.getParameter("action");
		logger.debug(user + " does " + actionId);
		// No user no chance!
		if (user != null && actionId != null) {
			PrivateMessageHandlerRemote pmh = new PrivateMessageHandler();
			// What's the action?
			Action action = Action.getById(actionId);
			if (action != null) {
				Writer writer = response.getWriter();
				response.setContentType("application/json");
				response.setStatus(200);
				response.flushBuffer();
				if (!user.isGuest()) {
					switch (action) {
					case INBOX:
					case OUTBOX:
						logger.info(user + " fetches inbox at " +  request.getParameter("timestamp"));
						int page = strToInt(request.getParameter("page"), -1);
						int limit = strToInt(request.getParameter("limit"), -1);
						boolean onlyCount = Boolean.parseBoolean(request.getParameter("onlycount"));
						PrivateMessage[] messages;
						int[] counts;
						// Do we fetch the Inbox or the Outbox?
						if (action == Action.INBOX) {
							if (onlyCount) {
								messages = null;
							} else {
								messages = pmh.readInbox(user, page, limit);
							}
							counts = pmh.countInbox(user);
						} else {
							if (onlyCount) {
								messages = null;
							} else {
								messages = pmh.readOutbox(user, page, limit);
							}
							counts = pmh.countOutbox(user);
						}
						writeMessages(messages, counts[0], counts[1], writer);
						writer.flush();
						response.flushBuffer();
						break;
					case DELETE:
						long messageId = Long.valueOf(request.getParameter("id"));
						logger.info(user + " deletes message with id " + messageId + " at "+  request.getParameter("timestamp"));
						if (!pmh.deleteMessage(user, messageId)) {
							response.setStatus(302);
						}
						break;
					case SEND:
						String recipient = request.getParameter("recipient");
						String subject = request.getParameter("subject");
						String body = request.getParameter("body");
						logger.info(user + " sends " + subject + " at " +  request.getParameter("timestamp"));
						PrivateMessage sentMessage = pmh.sendMessage(user, recipient, subject, body);
						if (sentMessage != null) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
							writeMessage(sentMessage,sdf, writer);
						}
						break;
					case READ:
						long id = Long.parseLong(request.getParameter("id"));
						logger.info(user + " reads " + id + " at " +  request.getParameter("timestamp"));
						PrivateMessage readMessage = pmh.readMessage(user, id);
						if (readMessage != null) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
							writeMessage(readMessage, sdf, writer);
						} else {
							response.setStatus(404);
						}
						break;
					}
					writer.flush();
					response.flushBuffer();
				}
			}
		}
		
	}
	
	/**
	 * Writes List of {@link PrivateMessages}, plus some additional Info to a JSON-Object
	 * @param messages
	 * @param maxCount
	 * @param unreadCount
	 * @param writer
	 * @throws IOException
	 */
	private void writeMessages(PrivateMessage[] messages, int maxCount, int unreadCount, Writer writer) throws IOException {
		writer.write('{');
		writer.write("\"count\":");
		writer.write("\"" + maxCount + "\",");
		writer.write("\"unread\":");
		writer.write("\"" + unreadCount + "\"");
		if (messages != null) {
			writer.write(",\"messages\":[");
			boolean first = true;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			for (PrivateMessage message : messages) {
				if (!first) {
					writer.write(',');
				} else {
					first = false;
				}
				writeMessage(message, sdf, writer);
			}
			writer.write("]");
		}
		writer.write("}");
	}

	/**
	 * Writes a single {@link PrivateMessage} as a JSON-Object
	 * @param message
	 * @param writer
	 * @throws IOException
	 */
	private void writeMessage(PrivateMessage message, SimpleDateFormat dateformat, Writer writer) throws IOException {
		writer.write("{\"id\":\"" + message.getId() + "\",");
		String subject = StringEscapeUtils.escapeHtml(trim(message.getSubject()));
		String body = StringEscapeUtils.escapeHtml(trim(message.getBody()));
		body = body != null ? body.replace("\n", "<br/>") : "";
		writer.write("\"subject\":\"" + subject + "\",");
		writer.write("\"body\":\"" + body + "\",");
		writer.write("\"recipient\":\"" + trim(message.getRecipient()) + "\",");
		writer.write("\"sender\":\"" + trim(message.getSender()) + "\",");
		writer.write("\"date\":\"" + dateformat.format(message.getDate()) + "\",");
		writer.write("\"read\":\"" + message.isRead() + "\"}");
	}

	/**
	 * Trims the supplied input and returns null in case of null
	 * @param input
	 * @return null or trimmed String
	 */
	private static String trim(String input) {
		if (input == null) {
			return null;
		} else {
			return input.trim();
		}
	}
	
	
	/**
	 * Parses the Input into a number
	 * @param input
	 * @param def
	 * @return int result or def
	 */
	private static int strToInt(String input, int def) {
		try {
			return Integer.parseInt(input);
		} catch (NumberFormatException nfe) {
			return def;
		}
	}
	
}
