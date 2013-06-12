package de.yovi.chat.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.api.ActionHandlerRemote;
import de.yovi.chat.api.Message;
import de.yovi.chat.api.Message.Preset;
import de.yovi.chat.api.Segment;
import de.yovi.chat.api.User;
import de.yovi.chat.client.Translator;
import de.yovi.chat.system.ActionHandler;

/**
 * Servlet implementation class ListenerServlet
 */
public class ListenerServlet extends HttpServlet {
	
	private final static Logger logger = Logger.getLogger(ListenerServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private final static int ALIVE_CYCLES = 100;
	
	private static final String HEADER = 
			"<HTML><HEAD><TITLE>Chat-Output</TITLE>" + //
			"<link href=\"css/bootstrap.min.css\" rel=\"stylesheet\" media=\"screen\">" +//		
			"<style>" + //
			" body {background:transparent; line-height:1em; margin: 0 8px; color:#000; line-height:normal}" + //
			" .username {font-weight:bold; }" +//
			" .useralias {font-weight:bold; font-style:italic; }" +//
			"</style>" + //
			"<script type=\"text/javascript\">" +//
			"   function openProfile(usernam) {parent.openProfile(username);}" + //
			"   function refresh() {parent.refresh();}" + //
			"   function stop() {return false}" + //
			"   function toolify(element) {parent.toolify(element); return false;}" + //
			"	function preview(element, click) {parent.preview(element, click); return false;}"  +//
			"	function moves() {" +
			" 		if (typeof parent.scrolling == 'undefined' || parent.scrolling) window.scroll(1,5000000); window.setTimeout(\"moves()\", 100);} moves();" + //
			"</script>" + //
			"</HEAD>" + //
			"<BODY style=\"overflow-x: hidden;\">" 
			//+ //
			//"<div style=\"position:absolute; bottom:0; right:0;\"><input type=\"checkbox\" value=\"true\" onclick=\"toggleScrolling(this);\"/>&nbsp;auto-scroll</div>";
			;
	private static final String STOP_SCRIPT = 
			"<script type=\"text/javascript\">" + //
			"	stop();" + //
			"</script>";
	
	private static final String REFRESH_SCRIPT = 
			"<script type=\"text/javascript\">" + //
			"	refresh();" + //
			"</script>";
	
	private static final String PROFILE_SCRIPT = 
			"<script type=\"text/javascript\">" + //
			"	openProfile('%s');" + //
			"</script>";
	
	
	public static final String USER_TAG  = 
			"<span class=\"username\" style=\"color:#%s\">" + //
			"%s" + //
			"</span>";
	

	public static final String USER_ALIAS_TAG  = 
			"<span style=\"color:#%s\" data-animation=\"false\" data-title=\"%s\" data-placement=\"right\" onmouseover=\"toolify(this);\" class=\"useralias\">" + //
			"%s" + //
			"</span>";
	
	private static Map<String, Integer> senslessCycleMap = new ConcurrentHashMap<String, Integer>();
    
	private enum OutputType {

		SYNC("sync"), ASYNC("async"), REGISTER("register");

		private final String id;

		private OutputType(String id) {
			this.id = id;
		}

		private static OutputType getById(String id) {
			if (id != null) {
				id = id.trim().toLowerCase();
				for (OutputType value : values()) {
					if (value.id.equals(id)) {
						return value;
					}
				}
			}
			return SYNC;
		}
	}
	
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		output(request, response);
	}

	private void output(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		User user = (User) request.getSession().getAttribute("user");
		response.setContentType("text/html");
//		response.setContentLength(-1);
		response.setStatus(200);
		
//        Response.AppendHeader("Keep-Alive", "timeout=3, max=993"); // HTTP 1.1 
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-stale=0, post-check=0, pre-check=0");
		response.setHeader("Connection", "close");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT"); // HTTP 1.1 
		response.setCharacterEncoding("UTF-8");
		PrintWriter writer = response.getWriter();
		OutputType type = OutputType.getById(request.getParameter("output"));
		logger.debug(request.getRemoteAddr() + " does " + type);
		if (user != null) {
			final String listenid = request.getParameter("listenid");
			logger.debug(user + 	" starts to listen with id " + listenid);
			String lang = request.getParameter("lang");
			if (lang == null) {
				lang = "de";
			}

			String htmlString = request.getParameter("html");
			boolean html = htmlString == null || Boolean.parseBoolean(htmlString);
			Translator translator = Translator.getInstance(lang);
			ActionHandlerRemote ah = new ActionHandler();
			if (type == OutputType.SYNC) {
				if(html) {
					writer.write(HEADER);
					// 5 KB initial load... 
					// TODO configurable BURST-Mode (some initial MB to trick Filters!)
					writeDummy(writer, 1024 * 5);
					writer.flush();
					response.flushBuffer();
				}
				try {
					boolean stop = false;
					int senselessCycles = 0;
					do {
						Thread.sleep(200l);
						Message[] listen = ah.listen(user, listenid);
						switch (decodeMessages(writer, listen, translator, html, true)) {
						case STOP:
							stop = true;
							if (html) {
								writer.write(STOP_SCRIPT);
							}
							break;
						case REFRESH:
							if (html) {
								senselessCycles = 0;
								writer.write(REFRESH_SCRIPT);
							}
							break;
						case EMPTY:
							if (html) {
								if (++senselessCycles % ALIVE_CYCLES == 0) {
									senselessCycles = 0;
									writeDummy(writer, 16);
								}
							}
							break;
						case NONE:
						default:
							break;
						};
						writer.flush();
						response.flushBuffer();
					} while (!stop);
						
					if (stop) {
						logger.info("Obsolet stream for " + user.getUserName() + " interrupted!");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else if (type == OutputType.ASYNC) {
				Message[] listen = ah.listen(user, listenid);
				Trigger trigger = decodeMessages(writer, listen, translator, html, true);
				if (html) {
					switch (trigger) {
					case REFRESH:
						writer.write(REFRESH_SCRIPT);
						break;
					case STOP:
						writer.write(STOP_SCRIPT);
						break;
					case EMPTY:
						Integer senselessCycles = senslessCycleMap.get(listenid);
						if (senselessCycles == null) {
							senselessCycles = 0;
						}
						if (++senselessCycles % ALIVE_CYCLES == 0) {
							senselessCycles = 0;
							writeDummy(writer, 16);
						}
						senslessCycleMap.put(listenid, senselessCycles);
						break;
					default:
						break;
					}
				} else if (trigger == Trigger.STOP) {
					response.sendError(307);
				}
			} else if (type == OutputType.REGISTER) {
				logger.info(user + 	" starts to registers to Listen");
				ah.resetAndRejoin(user);
				writer.write(user.getListenId());
			}
		} else {
			writer.write("No User!");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		output(request, response);
	}
	
	public enum Trigger {
		
		NONE, REFRESH, STOP, EMPTY
		
	}

	public static Trigger decodeMessages(Writer writer, Message[] messages, Translator translator, boolean html, boolean live) throws IOException {
		boolean refresh = false;
		boolean stop = false;
		boolean empty = true;
		if (messages == null) {
			stop = true;
		} else {
			for (Message msg : messages) {
				empty = false;
				boolean profile = false;
				StringBuilder sb = new StringBuilder();
				StringBuilder lineBuilder = new StringBuilder();
				String profileScript = null;
				Preset code = Preset.getByCode(msg.getCode());
				switch (code) {
				case TIMEOUT:
				case DUPLICATESESSION:
					stop = true;
					break;
				case CHANNEL_BG_CHANGED:
				case CHANNEL_FG_CHANGED:
				case CHANNEL_NOTALLOWED:
				case CLOSE_CHANNEL:
				case CREATE_DONE:
				case INVITETO_USER:
				case INVITE_USER:
				case JOIN_CHANNEL:
				case LEFT_CHANNEL:
				case OPEN_CHANNEL:
				case OPEN_CHANNEL_ALREADY:
				case SWITCH_CHANNEL:
				case MESSAGE:
				case SETTINGS:
				case USER_AWAY:
				case USER_BACK:
				case USER_ALIAS_SET:
				case USER_ALIAS_CLEARED:
				case CLEAR_LOG:
				case CLEAR_MEDIA:
				case REFRESH:
					refresh = true;
					break;
				case PROFILE_OPEN:
					profile = true;
					break;
				case DEFAULT:
					lineBuilder.append("|ORIGIN|: ");
				}
				Segment[] segments = msg.getSegments();
				if (segments != null) {
					for (Segment seg : segments) {
						String content = seg.getContent();
						switch (seg.getType()) {
						case TEXT:
							if (content.charAt(0) == '$') {
								if (profile && html && live) {
									if (content.startsWith("$PROFILE_OPEN")) {
										Map<String, String> map = Translator.parse2map(content);
										String profileUser = map.get("user");
										if (profileUser != null) {
											profileScript = String.format(PROFILE_SCRIPT, profileUser);
										}
									}
								}
								content = translator.translate(content.substring(1));
							}
							if (html) {
								content = ChatUtils.escape(content);
							}
							break;
						case VIDEO:
						case IMAGE:
						case WEBSITE:
							refresh = true;
							if (html) {
								String text = seg.getAlternateName() != null ? seg.getAlternateName() : seg.getContent();
								if (live && seg.getPreview() != null) {
									text = StringEscapeUtils.escapeHtml(text);
									content = String.format("<a target=\"_blank\" onmouseover=\"preview(this, false);\" href=\"%1$s\" data-preview=\"%3$s\">%2$s</a>", content, text, seg.getPreview()); 
								} else {
									content = String.format("<a target=\"_blank\" href=\"%1$s\">%2$s</a>", content, text); 
								}
							} else {
								content = seg.getContent();
							}
							break;
						default:
							break;
						}
						lineBuilder.append(content);
						lineBuilder.append(' ');
					}
					String line;
					User origin = msg.getOrigin();
					if (origin != null) {
						String name = origin.getUserName();
						if (html) {
							if (origin.getAlias() != null) {
								name = String.format(ListenerServlet.USER_ALIAS_TAG, origin.getColor(), StringEscapeUtils.escapeHtml(name), StringEscapeUtils.escapeHtml(origin.getAlias()));
							} else {
								name = String.format(ListenerServlet.USER_TAG, origin.getColor(), StringEscapeUtils.escapeHtml(name));
							}
						}
						line = lineBuilder.toString();
						line = line.replace("|ORIGIN|", name);
					} else if (html) { 
						line = lineBuilder.toString();
					} else {
						line = lineBuilder.toString();
					}
					if (html) {
						line = makeCursive(line);
					}
					sb.append(line);
					sb.append('\n');
					if (html) {
						sb.append("<br />");
					}
					if (profileScript != null) {
						writer.write(profileScript);
					}
					writer.write(sb.toString());
				}
			}
		}
		if (stop) {
			return Trigger.STOP;
		} else if (refresh) {
			return Trigger.REFRESH;
		} else if (empty) {
			return Trigger.EMPTY;
		} else {
			return Trigger.NONE;
		}
	}
	
	private static String makeCursive(String content) {
		int ixOfAst;
		boolean iOpen = false;
		int offset = 0;
		while ((ixOfAst = content.indexOf('*', offset)) >= 0) {
			offset = ixOfAst + 1;
			// if there's no open i-tag
			if (!iOpen) {
				// and the next char is 'the end'
				char next;
				if (content.length() <= (ixOfAst + 1)) {
					// skip this one
					continue;
				// or is a whitespace or another asterisk
				} else if ((next = content.charAt(ixOfAst + 1)) == '*' || Character.isWhitespace(next)) {
					// skip this one
					continue;
				}
			}
			String before = content.substring(0, ixOfAst);
			String after = content.substring(ixOfAst + 1);
			content = before;
			if (iOpen) {
				content += "</i>";
				iOpen = false;
				offset += 3;
			} else {
				content += "<i>";
				iOpen = true;
				offset += 2;
			}
			content += after;
		}
		if (iOpen) {
			content += "</i>";
		}
		return content;
	}
	
	private static void writeDummy(Writer writer, int length) throws IOException {
		writer.write("<!-- ");
		Random rnd = new Random();
		while (length-- > 0) {
			writer.write((rnd.nextInt(10)+48));
		}
		writer.write("-->");
	}
	
}
