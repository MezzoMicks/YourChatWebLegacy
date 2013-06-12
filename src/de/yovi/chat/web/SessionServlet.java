package de.yovi.chat.web;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

import de.yovi.chat.api.ActionHandlerRemote;
import de.yovi.chat.api.User;
import de.yovi.chat.system.ActionHandler;
import de.yovi.chat.system.PasswordUtil;

/**
 * Servlet implementation class SessionServlet
 */
public class SessionServlet extends HttpServlet implements HttpSessionListener {
	
	private final static Logger logger = Logger.getLogger(SessionServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private enum Action {

		SUGAR("sugar"), LOGIN("login"), REGISTER("register"), LOGOUT("logout");

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
    public SessionServlet() {
        super();
    }
    
	private Map<String, HttpSession> sessionMap = new ConcurrentHashMap<String, HttpSession>();

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		if (session != null) {
			sessionMap.put(session.getId(), session);
			logger.debug("Remembering Session " + session.getId());
		}
	}
	
	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		if (session != null) {
			sessionMap.remove(session.getId());
			logger.debug("Forgetting Session " + session.getId());
		}
	}
   
    @Override
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);
    	logger.debug(config.getServletContext().getContextPath());
    }
    
    @Override
    public void destroy() {
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	doRequest(request, response);
    }
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Action action = Action.getById(request.getParameter("action"));
		if (action != null) {
			HttpSession session = request.getSession(true);
			logger.debug(request.getRemoteAddr() + " does " + action);
			User newUser = null;
			switch (action) {
			case LOGIN:
				if (session != null) {
					String username = request.getParameter("username");
					String password = request.getParameter("passwordHash");
					String sugar = (String) session.getAttribute(SessionParameters.SUGAR);
					logger.debug("login -> user:" + username + " pass: " + password + " sugar: " + sugar);
					if (sugar.endsWith(username)) {
						ActionHandlerRemote ah = new ActionHandler();
						String realSugar = sugar.substring(0, sugar.length() - username.length());
						newUser = ah.login(username, password, realSugar, null);
						if (newUser == null) {
							response.sendRedirect("login.jsp");
						}
					} else {
						session.invalidate();
						response.sendRedirect("login.jsp");
						logger.error("Sugar didn't match user: got username '" + username + "' expected sugar '" + sugar + "'. Mixed up sessions?");
					}
				} else {
					response.sendRedirect("login.jsp");
					logger.error("Login without session!");
				}
				break;
			case LOGOUT:
				if (session != null) {
					User user = (User) session.getAttribute(SessionParameters.USER);
					String locaLogoutKey = (String) session.getAttribute(SessionParameters.LOGOUT_KEY);
					if (user != null) {
						ActionHandlerRemote ah = new ActionHandler();
						String paramLogoutKey = request.getParameter("key");
						if (paramLogoutKey.equals(locaLogoutKey)) {
							ah.logout(user);
							session.setAttribute(SessionParameters.USER, null);
							session.invalidate();
							response.sendRedirect("login.jsp");
						}
					} else {
						response.sendRedirect("login.jsp");
						logger.error("Logout without user!");
					}
				}
				break;
			case REGISTER:
				if (session != null) {
					String username = request.getParameter("username");
					String password = request.getParameter("passwordHash");
					String invitekey = request.getParameter("keyHash");
					ActionHandlerRemote ah = new ActionHandler();
					String sugar = (String) session.getAttribute(SessionParameters.SUGAR);
					String realSugar = sugar.substring(0, sugar.length() - username.length());
					newUser = ah.register(username, password, invitekey, realSugar);
					if (newUser == null) {
						response.sendRedirect("login.jsp");
					}
				} else {
					response.sendRedirect("login.jsp");
					logger.error("Register without session!");
				}
				break;
			case SUGAR:
				// Only possible if there's no Session!
				if (session != null) {
					String sugar = PasswordUtil.getSugar();
					session = request.getSession(true);
					String userName = request.getParameter("user");
					session.setAttribute(SessionParameters.SUGAR, sugar + userName);
					response.setContentType("application/json");
					response.getWriter().write("{\"sugar\":\"" + sugar + "\"}");
				} else {
					logger.warn("Sugarrequest without Session from User " + session.getAttribute(SessionParameters.USER));
				}
				break;
			}
			// New user to put in Session
			if (newUser != null) {
				session.setAttribute(SessionParameters.USER, newUser);
				String logoutKey = Long.toHexString(System.currentTimeMillis());
				session.setAttribute(SessionParameters.LOGOUT_KEY, logoutKey);
				session.setMaxInactiveInterval(-1);
				response.sendRedirect("");
			}
			
		}
	}

}
