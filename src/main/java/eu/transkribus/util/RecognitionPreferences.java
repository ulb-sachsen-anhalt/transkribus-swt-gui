package eu.transkribus.util;

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.enums.ScriptType;
import eu.transkribus.util.TextRecognitionConfig.Mode;

public class RecognitionPreferences {
	private static final Logger logger = LoggerFactory.getLogger(RecognitionPreferences.class);
	
	private static Preferences pref = Preferences.userRoot().node("/trp/recognition");

	private final static String HTR = "HTR";
	private final static String OCR = "OCR";
	private final static String SEP = "|";
	
	private final static String MODE = "mode";
	private final static String LANGUAGE = "language";
	private final static String HTR_ID = "htrId";
	private final static String HTR_NAME = "htrName";
	private final static String TYPE_FACE = "typeFace";
	private final static String DICTIONARY = "dictionary";
	private final static String LM = "lm";
	
	public static TextRecognitionConfig getHtrConfig(final int colId, final String serverUri) {

		String modeString = pref.get(buildKey(HTR, colId, serverUri, MODE), null);
		if(modeString == null || modeString.isEmpty()) {
			return null;
		}
		
		Mode mode = Mode.valueOf(modeString);
		TextRecognitionConfig config = new TextRecognitionConfig(mode);
		config.setLanguage(pref.get(buildKey(HTR, colId, serverUri, LANGUAGE), null));
		switch(mode) {
			case CITlab:
			config.setDictionary(pref.get(buildKey(HTR, colId, serverUri, DICTIONARY), null));
			config.setHtrId(pref.getInt(buildKey(HTR, colId, serverUri, HTR_ID), 0));
			config.setHtrName(pref.get(buildKey(HTR, colId, serverUri, HTR_NAME), null));
			break;
		case UPVLC:
//			config.setDictionary(pref.get(buildKey(HTR, colId, serverUri, DICTIONARY), null));
			config.setLanguageModel(pref.get(buildKey(HTR, colId, serverUri, LM), null));
			config.setHtrId(pref.getInt(buildKey(HTR, colId, serverUri, HTR_ID), 0));
			config.setHtrName(pref.get(buildKey(HTR, colId, serverUri, HTR_NAME), null));			
			break;
		default:
			return null;
		}
		return config;
	}

	public static void save(int colId, final String serverUri, TextRecognitionConfig config) {
		if(config == null) {
			logger.debug("remocing config for colId="+colId+", serverUri="+serverUri);
			pref.remove(buildKey(HTR, colId, serverUri, MODE));
			pref.remove(buildKey(HTR, colId, serverUri, LANGUAGE));			
			pref.remove(buildKey(HTR, colId, serverUri, HTR_ID));
			pref.remove(buildKey(HTR, colId, serverUri, HTR_NAME));
			pref.remove(buildKey(HTR, colId, serverUri, LM));
		}
		else {
			Mode mode = config.getMode();
			pref.put(buildKey(HTR, colId, serverUri, MODE), mode.toString());
			pref.put(buildKey(HTR, colId, serverUri, LANGUAGE), config.getLanguage());
			
			final String dictKey, lmKey;
			switch(mode) {
			case CITlab:
				pref.putInt(buildKey(HTR, colId, serverUri, HTR_ID), config.getHtrId());
				pref.put(buildKey(HTR, colId, serverUri, HTR_NAME), config.getHtrName());
				dictKey = buildKey(HTR, colId, serverUri, DICTIONARY);
				if(config.getDictionary() != null) {
					pref.put(dictKey, config.getDictionary());
				} else {
					pref.remove(dictKey);
				}
				break;
			case UPVLC:
				pref.putInt(buildKey(HTR, colId, serverUri, HTR_ID), config.getHtrId());
				pref.put(buildKey(HTR, colId, serverUri, HTR_NAME), config.getHtrName());
				lmKey = buildKey(HTR, colId, serverUri, LM);
				if(config.getLanguageModel() != null) {
					pref.put(lmKey, config.getLanguageModel());
				} else {
					pref.remove(lmKey);
				}
				break;			
			default:
				break;	
			}	
		}
	}
	
	public static OcrConfig getOcrConfig(int colId, String serverUri) {
		
		OcrConfig config = new OcrConfig();
		final String langStr = pref.get(buildKey(OCR, colId, serverUri, LANGUAGE), null);
		final String typeFace = pref.get(buildKey(OCR, colId, serverUri, TYPE_FACE), null);
		
		logger.debug("Loading langStr: " + langStr);
		logger.debug("Loading typeFace: " + typeFace);
		
		if(typeFace == null || langStr == null) {
			return null;
		}
		
		config.setLanguages(langStr);
		config.setTypeFace(ScriptType.valueOf(typeFace));
		return config;
	}
	
	public static void save(int colId, final String serverUri, OcrConfig config) {
		if(config == null) {
			throw new IllegalArgumentException("Config is null!");
		}
		pref.put(buildKey(OCR, colId, serverUri, TYPE_FACE), config.getTypeFace().toString());
		pref.put(buildKey(OCR, colId, serverUri, LANGUAGE), config.getLanguageString());	
		logger.debug("Saving langStr: " + config.getLanguageString());
	}
	
	private static String buildKey(final String prefix, final int colId, final String serverUri, final String key) {
		return prefix + SEP + colId + SEP + serverUri + SEP + key;
	}

}
