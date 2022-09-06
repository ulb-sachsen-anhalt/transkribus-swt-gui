package eu.transkribus.swt_gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.i18n.UTF8Control;

public class Msgs {
	
	private static final Logger logger = LoggerFactory.getLogger(Msgs.class);
	
	final static String LOCALES_BASENAME = "i18n/Messages";

	public final static Locale EN_LOCALE = new Locale("en", "US");
	public final static Locale DE_LOCALE = new Locale("de", "DE");
	
	public final static Locale DEFAULT_LOCALE = EN_LOCALE;
	
	public final static List<Locale> LOCALES = new ArrayList<>();
	
	static {
		LOCALES.add(EN_LOCALE);
		LOCALES.add(DE_LOCALE);
	}
	
	static ResourceBundle messages; //ResourceBundle.getBundle(LOCALES_BASENAME, TrpConfig.getTrpSettings().getLocale());
	static final UTF8Control utf8Control = new UTF8Control();
	
	static {
		Locale.setDefault(DEFAULT_LOCALE);
		
		if (TrpConfig.getTrpSettings()!=null) {
			setLocale(TrpConfig.getTrpSettings().getLocale());
		} 
		else {
			setLocale(EN_LOCALE);
		}
//		setLocale(DE_LOCALE);
	}
	
	public static Locale getLocale() {
		
		return messages.getLocale();
	}
	
	public static void setLocale(Locale l) {
		messages = ResourceBundle.getBundle(LOCALES_BASENAME, l, utf8Control);
//		messages = ResourceBundle.getBundle(LOCALES_BASENAME, l);
	}
	
	

	/**
	 * Returns a message with the given key.
	 * If key is not found:
	 * <ul>
	 * <li>if the key includes a hierarchy (represented by "."), the leaf-key is extracted and returned capitalized.</li>
	 * <li>if it does not include a hierarchy, then the capitalized key is returned.</li>
	 * </ul>
	 */
	public static String get(String key) {
		if(StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("Key must not be empty.");
		}
		try {
			return messages.getString(key);
		} catch (MissingResourceException mre) {
			final String msg;
			logger.error("A resource key is missing in locale " + getLocale().toString() + ": " + key);
			if(key.contains(".")) {
				final String[] split = key.split("\\.");
				msg = split[split.length-1];
			} else {
				msg = key;
				
			}
			return StringUtils.capitalize(msg);
		}
	}

	public static String get(String key, final String alternative) {
		if(StringUtils.isEmpty(alternative)) {
			return get(key);
		}
		try {
			return messages.getString(key);
		} catch (MissingResourceException mre) {
			logger.debug("A resource key is missing in locale " + getLocale().toString() + ": " + key);
			return alternative;
		}
	}
	
	/**
	 * Returns a message with the given key
	 */
	public static String get3(String key) {
		return messages.getString(key);
	}
	
	/**
	 * Returns a message with the given key, returning the key itself if it was not found
	 */
	public static String get2(String key) {
		try {
			return get3(key);
		} catch (Exception e) {
			return key;
		}
	}
	
	public static String get2(String key, String alternative) {
		try {
			return get3(key);
		} catch (Exception e) {
			if (alternative != null)
				return alternative;
			else
				return key;
		}
	}
	
	public static void main(String[] args) {
		String key="documents";
		
		// default locale:
		System.out.println("current locale = "+Msgs.getLocale());
		System.out.println(Msgs.get(key));
		
		// set german locale:
		Msgs.setLocale(DE_LOCALE);

		System.out.println("current locale = "+Msgs.getLocale());
		System.out.println(Msgs.get(key));
		
		
		// set a locale that is not available - should fall back to default
		Msgs.setLocale(new Locale("pt", "BR"));
		
		System.out.println("current locale = "+Msgs.getLocale());
		System.out.println(Msgs.get3(key));
		
	
	}

}


