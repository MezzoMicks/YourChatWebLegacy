package de.yovi.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

public class Translator {

	private final static Logger logger = Logger.getLogger(Translator.class);

	private final static String P_MAIN = "$MAIN";
	private final static Map<String, Translator> lang2instance = new HashMap<String, Translator>();
	private final Map<String, String> translations = new HashMap<String, String>();
	
	private Translator() {
		// hidden
	}
	
	public static Translator getInstance(String lang) {
		Translator instance = lang2instance.get(lang);
		if (instance == null) {
			instance = createInstance(lang);
		}
		return instance;
	}
	
	private static synchronized Translator createInstance(String lang) {
		Translator instance = lang2instance.get(lang);
		if (instance == null) {
			instance = new Translator();
			InputStream is = instance.getClass().getResourceAsStream("/messages_de.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			try {
				while ((line = br.readLine()) != null) {
					String[] values = line.split("=");
					if (values != null && values.length == 2) {
						values[1] = values[1].replace("[nl]", "\n");
						instance.translations.put(values[0], values[1]);
					}
				}
			} catch (IOException e) {
				logger.error(e);
			}
			lang2instance.put(lang, instance);
		}
		return instance;
	}

	public static Map<String, String> parse2map(String input) {
		Map<String, String> result = new HashMap<String, String>();
		String main = null;
		StringTokenizer tokenizer = new StringTokenizer(input, "{");
		while (tokenizer.hasMoreTokens()) {
			String paramString = tokenizer.nextToken();
			if (main == null) {
				main = paramString;
			} else {
				paramString = paramString.substring(0, paramString.length() - 1);
				int ixOfequals = paramString.indexOf('=');
				if (ixOfequals == -1) {
					result.put(paramString, null);
				} else {
					result.put(paramString.substring(0, ixOfequals), paramString.substring(ixOfequals + 1));
				}
			}
		}
		// Keine Parameter da gewesen, dann vollen String als Kommando nutzen
		result.put(P_MAIN, main);
		return result;
	}
	
	public String translate(String message) {
		Map<String, String> parameters = parse2map(message);
		String result = translations.get(parameters.remove(P_MAIN));
		for (Entry<String, String> param : parameters.entrySet()) {
			String unescaped = param.getValue();
			if (unescaped != null) {
				unescaped = StringEscapeUtils.unescapeHtml(unescaped);
			} else {
				unescaped = "";
			}
			result = result.replace("$" + param.getKey(), unescaped);
		}
		return result;
	}
	
	public static void main(String[] args) {
		System.out.println(getInstance("de").translate("WHISPER{user=Michi}{message=Fo���o du Sau}"));
	}
	
}
