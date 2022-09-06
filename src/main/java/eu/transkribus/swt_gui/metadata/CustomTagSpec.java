package eu.transkribus.swt_gui.metadata;

import org.eclipse.swt.graphics.RGB;

import com.google.gson.annotations.JsonAdapter;

import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.util.APropertyChangeSupport;

/**
 * A specification of a custom tag for usage in the UI.
 * Includes also additional information such as shortcut character etc.
 */
@JsonAdapter(CustomTagSpecAdapter.class)
public class CustomTagSpec extends APropertyChangeSupport {
	public static RGB DEFAULT_COLOR = new RGB(0, 0, 255);
	
//	RGB rgb;
//	public static String RGB_PROPERTY="rgb";
	
	CustomTag customTag;
	public static String CUSTOM_TAG_PROPERTY="customTag";
	
	String shortCut;
	public static String SHORT_CUT_PROPERTY="shortCut";
	
	String color;
	public static String COLOR__PROPERTY="color";
	
	String label;
	String extras;
	Integer icon;
	
	public static String[] VALID_SHORTCUTS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
	
	public CustomTagSpec(CustomTag customTag) {
		this.customTag = customTag;
	}

//	public RGB getRGB() {
//		return rgb;
//	}
//
//	public void setRGB(RGB rgb) {
//		RGB old = this.rgb;
//		this.rgb = rgb;
//		firePropertyChange(RGB_PROPERTY, old, this.rgb);
//	}

	public CustomTag getCustomTag() {
		return customTag;
	}

	public void setCustomTag(CustomTag customTag) {
		CustomTag old = this.customTag;
		this.customTag = customTag;
		firePropertyChange(CUSTOM_TAG_PROPERTY, old, this.customTag);
	}

	public String getColor() {
		return color;
	}
	
	public void setColor(String color) {
		this.color = color;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getExtras() {
		return extras;
	}

	public void setExtras(String extras) {
		this.extras = extras;
	}

	public Integer getIcon() {
		return icon;
	}

	public void setIcon(Integer icon) {
		this.icon = icon;
	}

	public String getShortCut() {
		return shortCut;
	}

	public void setShortCut(String shortCut) {
		if (shortCut==null || isValidShortCut(shortCut)) {
			String old = this.shortCut;
			this.shortCut = shortCut;
			firePropertyChange(SHORT_CUT_PROPERTY, old, this.shortCut);
		}
	}
	
	public static boolean isValidShortCut(String sc) {
		for (String validC : VALID_SHORTCUTS) {
			if (validC.equals(sc)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "CustomTagDef [customTag=" + customTag + ", shortCut=" + shortCut + ", color=" + color +"]";
	}	

}
