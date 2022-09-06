package eu.transkribus.swt_gui.transcription;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.enums.TranscriptionLevel;
import eu.transkribus.swt_gui.canvas.CanvasKeys;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.metadata.CustomTagSpec;

public abstract class ATranscriptionWidgetListener implements Listener, KeyListener {
	private final static Logger logger = LoggerFactory.getLogger(ATranscriptionWidgetListener.class);
	
	TrpMainWidget mainWidget;
	ATranscriptionWidget transcriptionWidget;
	
//	TagPropertyPopup tagPropertyPopup;

	public ATranscriptionWidgetListener(TrpMainWidget mainWidget, ATranscriptionWidget transcriptionWidget) {
		this.mainWidget = mainWidget;
		this.transcriptionWidget = transcriptionWidget;
		
		transcriptionWidget.addListener(SWT.FocusIn, this);
		transcriptionWidget.addListener(SWT.Selection, this);
		transcriptionWidget.addListener(SWT.DefaultSelection, this);
		transcriptionWidget.addListener(SWT.Modify, this);
		
//		transcriptionWidget.addListener(SWT.KeyDown, listener);
		transcriptionWidget.getText().addKeyListener(this);
		
		transcriptionWidget.getVkItem().addListener(SWT.Selection, this);
		
		// listener for change of transcription widget type:
		SelectionAdapter transcriptTypeListener = new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				if (!(e.getSource() instanceof MenuItem))
					return;
				
				MenuItem mi = (MenuItem) e.getSource();
				if (mi.getSelection()) {
					mainWidget.getUi().changeToTranscriptionWidget((TranscriptionLevel) mi.getData());
				}
			}
		};
		transcriptionWidget.getTranscriptionTypeLineBasedItem().addSelectionListener(transcriptTypeListener);
		transcriptionWidget.getTranscriptionTypeWordBasedItem().addSelectionListener(transcriptTypeListener);
		
//		transcriptionWidget.getTranscriptionTypeItem().ti.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				if (e.detail != SWT.ARROW) {
//					ATranscriptionWidget.Type type = (Type) transcriptionWidget.getTranscriptionTypeItem().getSelected().getData();
//					mainWidget.getUi().changeToTranscriptionWidget(type);					
//				}
//			}
//		});
	}
	
	@Override public void keyPressed(KeyEvent e) {
		logger.trace("key pressed: "+e);
		
		if ( CanvasKeys.isAltKeyDown(e.stateMask) && (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_LEFT) ) {			
			if (e.keyCode == SWT.ARROW_LEFT)
				mainWidget.jumpToPreviousRegion();
			else
				mainWidget.jumpToNextRegion();
		}
		else if ( CanvasKeys.isCtrlKeyDown(e.stateMask) && (e.keyCode == SWT.ARROW_DOWN 
					|| e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT)) {
				mainWidget.jumpToNextCell(e.keyCode);
		}
		// insert custom tags with shortcuts
		else if ( CanvasKeys.isAltKeyDown(e.stateMask)) {
			String shortCut = ""+(char)e.keyCode;
			CustomTagSpec cDef = Storage.getInstance().getCustomTagSpecWithShortCut(shortCut);
			if (cDef != null) {
				logger.debug("CustomTagDef shortcut matched: "+cDef);
				mainWidget.addTagForSelection(cDef.getCustomTag(), null);
			}
		}
		// insert virtual keys with shortcuts
		else if ( CanvasKeys.isCtrlKeyDown(e.stateMask) ) {
			String shortCut = ""+(char)e.keyCode;
			Pair<Integer, String> vk = Storage.getInstance().getVirtualKeyShortCutValue(shortCut);
			if (vk != null) {
				transcriptionWidget.insertTextIfFocused(vk.getRight());
			}
		}
		
	}
	
	@Override public void keyReleased(KeyEvent e) {
	}
		
	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.FocusIn) {
			handleFocus(event);
		}
		else if (event.type == SWT.Selection) {
			if (event.widget == transcriptionWidget.getVkItem()) {
				mainWidget.openVkDialog();
			} else {
				handleSelectionChanged(event);	
			}
		}
		else if (event.type == SWT.DefaultSelection) {
			handleDefaultSelectionChanged(event);
		}		
		else if (event.type == SWT.Modify) {
			handleTextModified(event);
		}
	}

	protected abstract void handleTextModified(Event event);

	protected void handleDefaultSelectionChanged(Event event) {
		logger.debug("handleDefaultSelectionChanged, called by " + event.widget + " " + event.item);
		List<CustomTag> tagsUnderCursor = transcriptionWidget.getCustomTagsForCurrentOffset();
		logger.debug("tagsUnderCursor: "+tagsUnderCursor.size());
		for (CustomTag t : tagsUnderCursor) {
			logger.debug("t = "+t);
		}
		
		// todo: nächste Zeile an dieser Stelle entfernen und nur durch TAG_SET_EVENT aufrufen
		//if (event.widget.getClass() instance of )
		mainWidget.getUi().getTaggingWidget().updateSelectedTag(tagsUnderCursor);
		if (tagsUnderCursor.size() > 0 && mainWidget.getUi().getTabWidget().isCommentsItemSelected()) {
			mainWidget.getUi().getCommentsWidget().setSelectedComment(tagsUnderCursor.get(0));
		}
	}

	protected abstract void handleSelectionChanged(Event event);

	protected abstract void handleFocus(Event event);

}
