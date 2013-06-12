package de.yovi.chat.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import de.yovi.chat.persistence.PersistenceManager;

public class Configurator implements ServletContextListener {

	private final static Logger logger = Logger.getLogger(Configurator.class);

	private static Configurator instance;
	
	private byte[] passwordSalt = "TEST".getBytes();
	private boolean invitationRequired = false;
	
	private String[] channels = null;
	private String dataDir = null;
	private File dataFile = null;
	private String osname = null;
	private String osnameSimple = null;
	private String exeSuffix = null;
	private String baseDir = null;
	private String webInfDir = null;
	private String binDir = null;
	private String phantomjs = null;
	private String ffmpegthumbnailer = null;
	private Map<String, String> messagesOfTheDay = new HashMap<String, String>();
	private boolean phantomjsPresent = false;
	private boolean ffmpegthumbnailerPresent = false;
	private List<String> extraPlugins = new LinkedList<String>();
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		PersistenceManager.getInstance().closeEntityManagerFactory();
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		instance = this;
		// Fix might Help with some websites
		System.setProperty("http.agent", "");
		ServletContext context = event.getServletContext();
		webInfDir = context.getRealPath("WEB-INF");
		baseDir = context.getRealPath("/");
		checkSystem();
		readConfig();
		if (dataDir == null) {
			dataFile = (File)event.getServletContext().getAttribute("javax.servlet.context.tempdir");
			dataDir = dataFile.getAbsolutePath();
		}
		binDir = webInfDir + File.separator + "bin";
		checkPhantom();
		checkFFMPEGThumbnailer();
	}

	public void debug() {
		debug(null);
	}	
	
	public void debug(String dir) {
		if (dir == null) {
			dir = "./";
		} else if (!dir.endsWith("/")) {
			dir += "/";
		}
		instance = this;
		checkSystem();
		webInfDir =  new File(dir + "WebContent/WEB-INF").getAbsolutePath();
		baseDir =  new File("dir + WebContent").getAbsolutePath();
		readConfig();
		binDir = webInfDir + File.separator + "bin";
		checkPhantom();
		checkFFMPEGThumbnailer();
		checkDataDir(new File("data").getAbsolutePath());
	}

	private void checkPhantom() {
		if (phantomjs != null) {
			phantomjsPresent = new File(phantomjs).isFile();
		}
		logger.info("path to phantomjs is: " + phantomjs + (phantomjsPresent ? " (present)" : " (NOT present)"));
	}
	
	private void checkFFMPEGThumbnailer() {
		if (ffmpegthumbnailer != null) {
			ffmpegthumbnailerPresent = new File(ffmpegthumbnailer).isFile();
		}
		logger.info("path to ffmpegthumbnailer is: " + ffmpegthumbnailer + (ffmpegthumbnailerPresent ? " (present)" : " (NOT present)"));
	}

	private void checkSystem() {
		logger.info("Initializing application");
		osname = System.getProperty("os.name");
		logger.info("Operating system name: " + osname);
		osnameSimple = getOSNameSimple(osname);
		if (osnameSimple.equals("win")) {
			exeSuffix = ".exe";
		}
	}

	private void checkDataDir(String path) {
		dataDir = path;
		dataFile = new File(dataDir);
		String state = checkAndCreateFile(dataFile);
		logger.info("path for uploads is: " + dataDir + state);
	}

	public static boolean isInvitationRequired() {
		return instance.invitationRequired;
	}

	public static String getDataDir() {
		return instance.dataDir;
	}

	public static File getDataFile() {
		return instance.dataFile;
	}

	public static String getOsname() {
		return instance.osname;
	}

	public static String getOsnameSimple() {
		return instance.osnameSimple;
	}

	public static String getBinDir() {
		return instance.binDir;
	}

	public static String getBaseDir() {
		return instance.baseDir;
	}
	
	public static byte[] getSalt() {
		return instance.passwordSalt;
	}

	public static String getPhantomjs() {
		return instance.phantomjs;
	}

	public static boolean isPhantomjsPresent() {
		return instance.phantomjsPresent;
	}

	public static String getFFMPEGThumbnailer() {
		return instance.ffmpegthumbnailer;
	}

	public static boolean isFFMPEGThumbnailerPresent() {
		return instance.ffmpegthumbnailerPresent;
	}

	public static String[] getChannels() {
		return instance.channels;
	}

	public static List<String> getExtraPlugins() {
		return instance.extraPlugins;
	}
	
	public static String getMessageOfTheDay(String room) {
		return instance.messagesOfTheDay.get(room);
	}

	public static File createTempDirectory(File parent) throws IOException {
		final File temp = File.createTempFile("yourchat", "tmp", parent);

		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	private void readConfig() {
		BufferedReader br = null;
		try {
			logger.info("Reading properties");
			InputStream is = new FileInputStream(webInfDir + File.separator + "yourchat.properties");
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("#")) {
					String[] values = line.split("=");
					if (values != null && values.length == 2) {
						String param = values[0].trim();
						String value = values[1].trim();
						logger.debug(param + " = " + value);
						if (param.equals("ffmpegthumbnailer")) {
							ffmpegthumbnailer = value;
						} else if (param.equals("phantomjs")) {
							phantomjs = value;
						} else if (param.equals("channels")) {
							channels = value.split(";");
						} else if (param.equals("invitation_required")) {
							invitationRequired = Boolean.parseBoolean(value);
						} else if (param.equals("plugin")) {
							extraPlugins.add(value);
						} else if (param.equals("motd")) {
							int firstDDot = value.indexOf(':');
							if (firstDDot >= 0 && firstDDot + 1 > value.length()) {
								String room = value.substring(0, firstDDot);
								messagesOfTheDay.put(room, value.substring(firstDDot + 1));
							}
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}
	
	private String checkAndCreateFile(File dataFile) {
		int dirState = dataFile.isDirectory() ? 1 : 0;
		if (dirState == 0) {
			dirState = dataFile.mkdir() ? 2 : 0;
		}
		if (!dataFile.canWrite()) {
			dirState = -1;
		}
		String state;
		switch (dirState) {
		case -1:
			state = " (cannot write!)";
			break;
		case 0:
			state = " (cannot create!)";
			break;
		case 1:
			state = " (exists)";
			break;
		case 2:
			state = " (created)";
			break;
		default:
			state = " (?)";
		}
		return state;
	}

	private String getOSNameSimple(String osname) {
		osname = osname.toLowerCase();
		String result;
		if (osname.indexOf("win") >= 0) {
			result = "win";
		} else if (osname.indexOf("mac") >= 0) {
			result = "mac";
		} else if (osname.indexOf("nix") >= 0 || osname.indexOf("nux") >= 0
				|| osname.indexOf("aix") > 0) {
			result = "linux";
		} else if (osname.indexOf("sunos") >= 0) {
			result = "sunos";
		} else {
			result = "misc";
		}
		return result;
	}

}
