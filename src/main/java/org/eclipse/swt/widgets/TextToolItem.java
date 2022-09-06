package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.SysUtils;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt.util.SWTUtil;

public class TextToolItem extends ACustomToolItem {
	private static final Logger logger = LoggerFactory.getLogger(TextToolItem.class);
	
	public final static int DEFAULT_FONT_SIZE = 10;
	
	Text text;
	String hintText=null;
	
	public TextToolItem (ToolBar parent, int style) {
		super (parent, style);
	}

	public TextToolItem (ToolBar parent, int style, int index) {
		super(parent, style, index);
	}
		
	@Override
	protected void initControl() {
		text = new Text(parent, controlStyle | SWT.CENTER | SWT.SINGLE);
		if (SysUtils.IS_OSX) {
			text.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					center();
				}
			});
		}
		
//		FontData[] fD = text.getFont().getFontData();
//		fD[0].setHeight(DEFAULT_FONT_SIZE);
//		text.setFont(Fonts.createFont(fD[0]));

		this.setControl(text);
	}
	
	public void setAutoSelectTextOnFocus() {
		SWTUtil.addSelectOnFocusToText(text);
	}
	
	public void setNonEditable() {
		text.setBackground(Colors.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		text.setEditable(false);
	}

	@Override
	public void setText (String string) {
		text.setText(string);
		updateSize();
	}
	
	@Override
	public String getText() {
		return text.getText();
	}
	
	public Text getTextControl() {
		return text;
	}
	
	public void resizeToMessage() {
		if (text.getMessage()!=null) {
			GC gc = new GC(text);
			this.setWidth(gc.stringExtent(text.getMessage()).x+5);
			gc.dispose();
		}
	}
	
	public void setMessage(String message) {
		text.setMessage(message);
	}
	
	@Override public void setToolTipText(String string) {
//		super.setToolTipText(string);
		text.setToolTipText(string);
	}
	
}
