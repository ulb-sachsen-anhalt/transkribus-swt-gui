package eu.transkribus.swt_gui.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.graphics.RGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import eu.transkribus.core.util.GsonUtil;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt_gui.TrpConfig;

public class CustomTagSpecUtil {
	private static final Logger logger = LoggerFactory.getLogger(CustomTagSpecUtil.class);
	
	public static <T extends CustomTagSpec> void checkTagSpecsConsistency(List<T> customTagSpecs) {
		checkTagDefsShortCutConsistency(customTagSpecs);
	}
	
	public static <T extends CustomTagSpec> void checkTagDefsShortCutConsistency(List<T> customTagSpecs) {
		for (CustomTagSpec cDef: customTagSpecs) {
			String sc1 = cDef.getShortCut();
			
			for (CustomTagSpec cDefOther : customTagSpecs) {
				String sc2 = cDefOther.getShortCut();
				
				if (cDef == cDefOther) {
					continue;
				}
				
				if (sc1!=null && sc2!=null && sc1.equals(sc2)) {
					cDefOther.setShortCut(null);
				}
			}
		}		
	}
	
	public static String writeCustomTagSpecsToJsonString(List<? extends CustomTagSpec> customTagSpecs) {
		return GsonUtil.toJson(customTagSpecs);
	}
	
	public static List<CustomTagSpec> readCustomTagSpecsFromJsonString(String jsonStr) {
		if (StringUtils.isEmpty(jsonStr)) {
			logger.debug("no tag specifications found - returning empty array!");
			return new ArrayList<>();
		}
		
		return GsonUtil.fromJson(jsonStr, new TypeToken<List<CustomTagSpec>>(){}.getType());
	}
	
	public static String getNewStructSpecColor(List<StructCustomTagSpec> specs) {
		for (int i=0; i<TaggingWidgetUtils.getNColors(); ++i) {
			String newColor = TaggingWidgetUtils.getColorCodeForIndex(i);
			RGB newColorRGB = Colors.toRGB(newColor);
			if (!newColorRGB.equals(StructCustomTagSpec.DEFAULT_COLOR)
					&& specs.stream().filter(t -> newColorRGB.equals(t.getRGB())).findFirst().orElse(null) == null) { // no spec with new color found --> return this color!
				return newColor;
			}
		}
		
		return Colors.toHex(StructCustomTagSpec.DEFAULT_COLOR);
	}
	
	
	public static List<StructCustomTagSpec> readStructCustomTagSpecsFromJsonString(String jsonStr) {
		if (StringUtils.isEmpty(jsonStr)) {
			logger.debug("no struct tag specifications found - returning empty array!");
			return new ArrayList<>();
		}
		
		return GsonUtil.fromJson(jsonStr, new TypeToken<List<StructCustomTagSpec>>(){}.getType());
	}
	
//	public static String writeSingleCustomTagSpecToJsonString(CustomTagSpec customTagSpecs) {
//		return GsonUtil.toJson(customTagSpecs);
//	}
	
//	public static CustomTagSpec readSingleCustomTagSpecFromJsonString(String jsonStr) {
//		return GsonUtil.fromJson(jsonStr, CustomTagSpec.class);
//	}
	
	public static List<CustomTagSpec> readCustomTagSpecsFromSettings() {
		try {
			String tagSpecStr = TrpConfig.getTrpSettings().getTagSpecs();
			logger.debug("tagSpecStr: "+tagSpecStr);
	
			List<CustomTagSpec> tagSpecs = readCustomTagSpecsFromJsonString(tagSpecStr);
			logger.debug("read "+tagSpecs.size()+" tag specifications");
			return tagSpecs;
		}
		catch (Exception e) {
			logger.error("Could not read tag spec string: "+e.getMessage(), e);
			return new ArrayList<>();
		}
	}
	
	public static void writeCustomTagSpecsToSettings(List<CustomTagSpec> tagSpecs) {
		try {
			String tagSpecString = writeCustomTagSpecsToJsonString(tagSpecs);
			logger.debug("writing tagSpecString: "+tagSpecString);
			TrpConfig.getTrpSettings().setTagSpecs(tagSpecString);
		}
		catch (Exception e) {
			logger.error("Could not write tag specs!", e);
		}
	}
	
	public static String getCollectionTagSpecsAsJsonString(List<CustomTagSpec> tagSpecs) {
		try {
			return writeCustomTagSpecsToJsonString(tagSpecs);
		}
		catch (Exception e) {
			logger.error("Could not write tag specs!");
		}
		return null;
	}
	
	public static List<StructCustomTagSpec> readStructCustomTagSpecsFromSettings() {
		try {
			String structTagSpecsStr = TrpConfig.getTrpSettings().getStructTagSpecs();
			logger.debug("structTagSpecStr: "+structTagSpecsStr);
	
			List<StructCustomTagSpec> structTagSpecs = readStructCustomTagSpecsFromJsonString(structTagSpecsStr);
			logger.debug("read "+structTagSpecs.size()+" struct tag specifications");
			return structTagSpecs;
		}
		catch (Exception e) {
			logger.error("Could not read struct tag spec string: "+e.getMessage(), e);
			return new ArrayList<>();
		}
	}
	
	public static void writeStructCustomTagSpecsToSettings(List<StructCustomTagSpec> structTagSpecs) {
		try {
			String structTagSpecString = writeCustomTagSpecsToJsonString(structTagSpecs);
			logger.debug("writing structTagSpecString: "+structTagSpecString);
			TrpConfig.getTrpSettings().setStructTagSpecs(structTagSpecString);
		}
		catch (Exception e) {
			logger.error("Could not write struct tag specs!");
		}
	}
	
	public static <T extends CustomTagSpec> T getCustomTagSpecWithShortCut(List<T> customTagSpecs, String shortCut) {
		if (shortCut == null) {
			return null;
		}
		
		return customTagSpecs.stream()
				.filter(cDef -> { return StringUtils.equals(shortCut, cDef.getShortCut());})
				.findFirst().orElse(null);
	}
}
