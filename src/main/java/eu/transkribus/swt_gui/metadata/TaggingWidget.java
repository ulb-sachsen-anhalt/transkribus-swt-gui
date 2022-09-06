package eu.transkribus.swt_gui.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpLocation;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.SysUtils;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.databinding.DataBinder;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettings;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.metadata.CustomTagPropertyTableNew.ICustomTagPropertyTableNewListener;
import eu.transkribus.swt_gui.metadata.TagSpecsWidget.TagSpecsWidgetListener;
import eu.transkribus.swt_gui.transcription.ATranscriptionWidget;

public class TaggingWidget extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(TaggingWidget.class);
	
	SashForm verticalSf;

	TagListWidget tagListWidget; 
	TranscriptionTaggingWidget transcriptionTaggingWidget;
	Shell transcriptionTaggingWidgetShell;
	
	Button enableTagEditorBtn, searchTagsBtn;
	Button applyPropertiesToAllSelectedBtn;
	Button enableHighlightingBtn;
	
	public TaggingWidget(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new GridLayout(1, false));
		
		verticalSf = new SashForm(this, SWT.VERTICAL);
		verticalSf.setLayout(new GridLayout(1, false));
		verticalSf.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		tagListWidget = new TagListWidget(verticalSf, 0);
		tagListWidget.setLayoutData(new GridData(GridData.FILL_BOTH));
//		tagListWidget.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
//			@Override
//			public void selectionChanged(SelectionChangedEvent arg0) {
//				updateBtns();
//			}
//		});
		
		tagListWidget.getTableViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				List<CustomTag> selected = tagListWidget.getSelectedTags();
				if (!selected.isEmpty()) {
					logger.debug("showing tag: "+selected.get(0));
					TrpMainWidget.getInstance().showLocation(new TrpLocation(selected.get(0)));
				}
			}
		});
		
		tagListWidget.getTableViewer().getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateBtns();
				selectSelectedTagFromTagListInTranscriptionWidget();
			}
		});
		
		tagListWidget.getTableViewer().getTable().addKeyListener(new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent arg0) {
				CustomTag tag = tagListWidget.getSelectedTag();
				logger.debug("the tag name: " + (tag != null ? tag.getTagName() : "no tagname"));
				if (tag != null && tag.getTagName().equals("_checkMe")){
					char key = arg0.character;
					logger.debug("key pressed in tagging widget table: " + key);
					ATranscriptionWidget tw = TrpMainWidget.getInstance().getUi().getSelectedTranscriptionWidget();
					switch (key){
						case 'a':
							//logger.debug("selected tag: " + tagListWidget.getSelectedTag());
							String altText = (String)tag.getAttributeValue("alternative");
							String currLineText = tw.getCurrentLineObject().getUnicodeText();
							String beforeTag = currLineText.substring(0, tag.getOffset());
							String afterTag = currLineText.substring(tag.getEnd());
							String newLineText = beforeTag.concat(altText).concat(afterTag);
							logger.debug("User wants to replace the old text: " + tag.getContainedText() + " with this alternative: " + altText);	
							tw.getCurrentLineObject().setUnicodeText(newLineText, this);
							tagListWidget.refreshTable();
							tw.redraw();
							break;
						case 's':
							//user keeps the current text and deletes the _checkMe tag because it was checked
							CustomTag tag2 = tagListWidget.getSelectedTag();
							logger.debug("User keeps the current text " + tag2.getContainedText());
							tw.getCurrentLineObject().getCustomTagList().deleteTagAndContinuations(tag2);
							tagListWidget.refreshTable();
							tw.redraw();
							break;
						default: break;
					}
				}
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		enableTagEditorBtn = new Button(tagListWidget.getBtnsContainer(), SWT.TOGGLE);
//		enableTagEditorBtn.setText("Show text based tag editor");
		enableTagEditorBtn.setToolTipText("Shows / hides the tagging editor");
		enableTagEditorBtn.setImage(Images.getOrLoad("/icons/tag_blue_edit.png"));
		DataBinder.get().bindBeanToWidgetSelection(TrpSettings.SHOW_TEXT_TAG_EDITOR_PROPERTY, TrpConfig.getTrpSettings(), enableTagEditorBtn);
		
		enableHighlightingBtn = new Button(tagListWidget.getBtnsContainer(), SWT.TOGGLE);
		enableHighlightingBtn.setToolTipText("Enable table highlighting (does not work properly on MacOS)");
		enableHighlightingBtn.setImage(Images.getOrLoad("/icons/color_swatch.png"));
		SWTUtil.onSelectionEvent(enableHighlightingBtn, e -> {
			tagListWidget.setEnableTagHighlighting(enableHighlightingBtn.getSelection());
		});
		enableHighlightingBtn.setSelection(!SysUtils.IS_OSX);
		tagListWidget.setEnableTagHighlighting(enableHighlightingBtn.getSelection());
		
		searchTagsBtn = new Button(tagListWidget.getBtnsContainer(), SWT.PUSH);
		searchTagsBtn.setToolTipText("Search for tags...");
		searchTagsBtn.setImage(Images.FIND);
		SWTUtil.onSelectionEvent(searchTagsBtn, e -> {
			TrpMainWidget.getInstance().openSearchForTagsDialog();
		});
		
//		DataBinder.get().bindBeanToWidgetSelection(TrpSettings.SHOW_TEXT_TAG_EDITOR_PROPERTY, TrpConfig.getTrpSettings(), enableTagEditorBtn);		
		
		transcriptionTaggingWidget = new TranscriptionTaggingWidget(verticalSf, 0);
		transcriptionTaggingWidget.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		transcriptionTaggingWidget.getTabFolder().addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void maximize(CTabFolderEvent event) {
				setTaggingEditorVisiblity(2);
			}
			
			@Override
			public void minimize(CTabFolderEvent event) {
				setTaggingEditorVisiblity(1);
			}
		});
		
		// show properties for selected tag...
		transcriptionTaggingWidget.getTagSpecsWidget().getTableViewer().getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TagSpecsWidget tagSpecsWidget = transcriptionTaggingWidget.getTagSpecsWidget();
				TagPropertyEditor tagPropEditor = transcriptionTaggingWidget.getTagPropertyEditor();
				
				if (tagPropEditor.isSettingCustomTag()) { // if currently setting a custom tag in the property editor, ignore selection changed events from transcription widget!
					return;
				}
				
				CustomTagSpec selectedSpec = tagSpecsWidget.getSelected();
				if (selectedSpec == null) {
					return;
				}
				
//				CustomTag selectedTagInPropertyEditor = tagPropEditor.propsTable.getSelectedTag();
//				if (selectedTagInPropertyEditor!=null && selectedTagInPropertyEditor.getTagName().equals(selectedSpec.getCustomTag().getTagName())) {
//					logger.debug("a tag of this type is already selected - skipping!");
//					return;
//				}
				
				// set this tag spec in tag property editor -> WARNING: dangerous!
				if (true) {
				CustomTag protoTagCopy = selectedSpec.getCustomTag().copy();
				tagPropEditor.setCustomTag(protoTagCopy, false);
				tagListWidget.getTableViewer().setSelection(null); // clear selection in tagListWidget
				}
			}
		});
		
		transcriptionTaggingWidget.getTagSpecsWidget().addListener(new TagSpecsWidgetListener() {
			@Override
			public void addTagButtonClicked(CustomTagSpec tagSpec) {
//				CustomTagDef selTagDef = getSelected();
				if (TrpMainWidget.getInstance() != null && tagSpec != null && tagSpec.getCustomTag()!=null) {
					CustomTag ct = tagSpec.getCustomTag();
					Map<String, Object> attsMap = ct.getAttributeNamesValuesMap();
					
					CustomTag selectedTagInPropEditor = transcriptionTaggingWidget.getTagPropertyEditor().getPropsTable().getSelectedTag();
					
					// take attributes from property editor if its the same tag!
					if (selectedTagInPropEditor!=null && selectedTagInPropEditor.getTagName().equals(ct.getTagName())) {
						attsMap = transcriptionTaggingWidget.getTagPropertyEditor().getPropsTable().getSelectedTag().getAttributeNamesValuesMap();	
					}
					
					logger.debug("adding tag: "+ct.getTagName()+", attributes: "+attsMap);
					
					// TODO: add attributes from property editor!???
					TrpMainWidget.getInstance().addTagForSelection(ct.getTagName(), attsMap, null);
				}
			}
		});
		
//		transcriptionTaggingWidget.getTabFolder().addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				// on change to "Properties" tab: select tag that is selected in the TagListWidget
//				if (true /*transcriptionTaggingWidget.isTagPropertyEditorSelected()*/) {
//					transcriptionTaggingWidget.updateSelectedTag(tagListWidget.getSelectedTags());
//				}
//			}
//		});
		
		applyPropertiesToAllSelectedBtn = new Button(transcriptionTaggingWidget.getTagPropertyEditor().getBtnsComposite(), 0);
		applyPropertiesToAllSelectedBtn.setText("Apply to selected");
		applyPropertiesToAllSelectedBtn.setToolTipText("Applies the property values to the selected tags of the same type");
		SWTUtil.onSelectionEvent(applyPropertiesToAllSelectedBtn, e -> {
			List<CustomTag> selected = tagListWidget.getSelectedTags();
			if (selected.isEmpty()) {
				return;
			}
			
			if (!tagListWidget.isSelectedTagsOfSameType()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "All selected tags must have the same type!");
				return;
			}
			
			CustomTag st = transcriptionTaggingWidget.getTagPropertyEditor().propsTable.getSelectedTag();
			logger.debug("selected tag in property editor: "+st);
			for (CustomTag t : selected) {
				if (st.getTagName().equals(t.getTagName())) {
					logger.debug("applying attributes to tag: "+t);
					t.setAttributes(st, false, true);
					logger.debug("after: "+t);
				}
			}
			
			refreshTagList();
//			refreshTagsFromStorageAndCurrentSelection();
		});
		
		transcriptionTaggingWidget.getTagPropertyEditor().propsTable.addListener(new ICustomTagPropertyTableNewListener() {
			@Override
			public void onPropertyChanged(CustomTag tag, String property, Object value) {
				logger.debug("property changed: "+property+"/"+value);
//				refreshTagList();
//				refreshTagsFromStorageAndCurrentSelection();
				tagListWidget.getTableViewer().refresh(tag);
			}
		});
		
		SWTUtil.onSelectionEvent(transcriptionTaggingWidget.getTagPropertyEditor().getNextBtn(), e->jumpToNextTag(false));
		SWTUtil.onSelectionEvent(transcriptionTaggingWidget.getTagPropertyEditor().getPrevBtn(), e->jumpToNextTag(true));
		transcriptionTaggingWidget.getTagPropertyEditor().getPropsTable().getTableViewer().getTable().addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				logger.debug("traverse event in TagPropertyEditor: "+e.detail);
				if (e.detail == SWT.TRAVERSE_ARROW_NEXT) {
					e.doit = false;
					jumpToNextTag(false);
				}
				else if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
					e.doit = false;
					jumpToNextTag(true);
				}
			}
		});
		
		Storage.getInstance().addListener(new IStorageListener() {
			public void handleTranscriptLoadEvent(TranscriptLoadEvent arg) {
				refreshTagsFromStorageAndCurrentSelection();
			}

			public void handleTranscriptSaveEvent(TranscriptSaveEvent tse) {
//				refreshTagsFromStorageAndCurrentSelection(); // FIXME: necessary???
			}
		});

		verticalSf.setWeights(new int[] { 60, 40 } );
		
		setTaggingEditorVisiblity(TrpConfig.getTrpSettings().isShowTextTagEditor());
		
		updateBtns();
	}
	
	private void selectSelectedTagFromTagListInTranscriptionWidget() {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		if (mw == null) {
			return;
		}
		
		tagListWidget.setDisableTagUpdate(true);
		List<CustomTag> selected = tagListWidget.getSelectedTags();
		if (selected.size()==1) {
			CustomTag tag = selected.get(0);
			mw.showLocation(new TrpLocation(tag));
			mw.getUi().getTaggingWidget().getTranscriptionTaggingWidget().getTagPropertyEditor().setCustomTag(tag, false);
			mw.getUi().getTaggingWidget().getTranscriptionTaggingWidget().getTagSpecsWidget().getTableViewer().getTable().deselectAll();
		}
		tagListWidget.setDisableTagUpdate(false);				
	}
	
	public void refreshTagList() {
		tagListWidget.refreshTable();
	}
	
	public void refreshTagsFromStorageAndCurrentSelection() {
		refreshTagList();
		
		ATranscriptionWidget tWidget = TrpMainWidget.getInstance().getUi().getSelectedTranscriptionWidget();
		List<CustomTag> selectedTags = new ArrayList<>();
		if (tWidget != null) {
			selectedTags.addAll(tWidget.getCustomTagsForCurrentOffset());
		}
		updateSelectedTag(selectedTags);
	}
	
	public synchronized void updateSelectedTag(List<CustomTag> tags) {
		tagListWidget.updateSelectedTag(tags);
		
		if (TrpMainWidget.getInstance().getUi().getTabWidget().isTextTaggingItemSeleced() && TrpConfig.getTrpSettings().isShowTextTagEditor()) {
			transcriptionTaggingWidget.updateSelectedTag(tags);
		}
	}
	
	private void updateBtns() {
		applyPropertiesToAllSelectedBtn.setEnabled(tagListWidget.getSelectedTag()!=null && tagListWidget.isSelectedTagsOfSameType());
	}
	
	public void setTaggingEditorVisiblity(boolean visible) {
		setTaggingEditorVisiblity(visible ? 1 : 0);
	}
	
	public void setTaggingEditorVisiblity(int visibility) {
		logger.debug("setTaggingEditorVisiblity: "+visibility);
		
		enableTagEditorBtn.setSelection(visibility > 0);
		
		if (visibility <= 1) {
			transcriptionTaggingWidget.setParent(verticalSf);
			transcriptionTaggingWidget.moveBelow(null);
			transcriptionTaggingWidget.getTabFolder().setMaximizeVisible(true);
			transcriptionTaggingWidget.getTabFolder().setMinimizeVisible(false);
			transcriptionTaggingWidget.pack();
			
			if (!SWTUtil.isDisposed(transcriptionTaggingWidgetShell)) {
				logger.trace("disposing shell!");
				transcriptionTaggingWidgetShell.dispose();
			}
			
			verticalSf.setWeights(new int[] { 55, 45 });
			if (true) // false -> show editor always
			if (visibility<=0) {
				verticalSf.setMaximizedControl(tagListWidget);
			} else {
				verticalSf.setMaximizedControl(null);
				if (true /*transcriptionTaggingWidget.isTagPropertyEditorSelected()*/) {
//					transcriptionTaggingWidget.getTagPropertyEditor().findAndSetNextTag();
				}
			}
		} else {
			if (SWTUtil.isDisposed(transcriptionTaggingWidgetShell)) {
//				int shellStyle = SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE /*| SWT.MAX*/;
				int shellStyle = SWT.MODELESS | SWT.SHELL_TRIM;
				transcriptionTaggingWidgetShell = new Shell(getDisplay(), shellStyle);
				transcriptionTaggingWidgetShell.setText("Text based Tagging");
				transcriptionTaggingWidgetShell.setLayout(new FillLayout());
				
				// on closing this shell -> dock this widget again!
				transcriptionTaggingWidgetShell.addListener(SWT.Close, new Listener() {
					@Override public void handleEvent(Event event) {
						TrpConfig.getTrpSettings().setShowTextTagEditor(true);
					}
				});				
			
				Point l = this.toDisplay(this.getLocation());
				int height=600;
				transcriptionTaggingWidgetShell.setSize(400, height);
				transcriptionTaggingWidgetShell.setLocation(l.x, l.y+this.getSize().y-height);		
			}
			
			transcriptionTaggingWidgetShell.setVisible(true);
			transcriptionTaggingWidgetShell.setActive();

			transcriptionTaggingWidget.setParent(transcriptionTaggingWidgetShell);
			transcriptionTaggingWidget.getTabFolder().setMaximizeVisible(false);
			transcriptionTaggingWidget.getTabFolder().setMinimizeVisible(true);
			transcriptionTaggingWidget.pack();
			transcriptionTaggingWidget.layout();
			verticalSf.setWeights(new int[] { 100 });
			verticalSf.setMaximizedControl(tagListWidget);
			
			transcriptionTaggingWidgetShell.layout(true);
			transcriptionTaggingWidgetShell.open();
		}
	}
	
	public TagListWidget getTagListWidget() {
		return tagListWidget;
	}
	
	public TranscriptionTaggingWidget getTranscriptionTaggingWidget() {
		return transcriptionTaggingWidget;
	}
	
	public void updateVisibility(boolean setEnabled){
		
		enableTagEditorBtn.setEnabled(setEnabled);
		searchTagsBtn.setEnabled(setEnabled);
		applyPropertiesToAllSelectedBtn.setEnabled(setEnabled);
		tagListWidget.tv.getTable().setEnabled(setEnabled);
		
	}
	
	public void jumpToNextTag(boolean previous) {
		logger.trace("jumpToNextTag: previous="+previous);
		
		CustomTag selected = tagListWidget.getSelectedTag();
		List<CustomTag> sortedTags = tagListWidget.getTagsAsSortedInUi();
		int index = sortedTags.indexOf(selected);
		
		List<CustomTag> nextSelected = new ArrayList<>();
		
		if (!sortedTags.isEmpty()) {
			if (selected == null || index==-1) {
				nextSelected.add(sortedTags.get(0));
			}
			else {
				CustomTag neighbor = CoreUtils.getNeighborElement(sortedTags, selected, previous, true);
				if (neighbor != null) {
					nextSelected.add(neighbor);
				}
			}			
		}
		
		tagListWidget.updateSelectedTag(nextSelected);
		selectSelectedTagFromTagListInTranscriptionWidget();
//		updateSelectedTag(nextSelected); // FIXME: this call creates a confusing loop of events...
	}

}
