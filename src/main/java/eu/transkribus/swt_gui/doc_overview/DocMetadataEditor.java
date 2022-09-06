package eu.transkribus.swt_gui.doc_overview;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.nebula.widgets.datechooser.DateChooserCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.enums.ScriptType;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.EnumUtils;
import eu.transkribus.core.util.FinereaderUtils;
import eu.transkribus.core.util.StrUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.edit_decl_manager.EditDeclManagerDialog;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.metadata.DocUserMetadataEditor;
import eu.transkribus.swt_gui.tools.LanguageSelectionTable;

public class DocMetadataEditor extends Composite {
	private final static Logger logger = LoggerFactory.getLogger(DocMetadataEditor.class);
	
	Text titleText;
	Text authorityText;
//	Link backlink;
	Text backlink;
	Text extIdText;
	Tree hierarchyTree;
	Text authorText;
	Label uploadedLabel;
	Text genreText;
	Text writerText;
	Button saveBtn;
	Text descriptionText;
	Combo scriptTypeCombo, scriptTypeCombo2;
	LanguageSelectionTable langTable;
	
	DateChooserCombo createdFrom, createdTo;
	
	Button enableCreatedFromBtn, enableCreatedToBtn, openEditDeclManagerBtn;
	EditDeclManagerDialog edm;
	
	DocMetadataEditorListener listener;
	
	DocUserMetadataEditor userMd;
	
//	FutureTask futureSaveTask;
	


	Thread saveThread;
	
	String currentHierarchy=null, currentExternalId=null;
	
	public static final String PRINT_META_SCRIPTTYPE = "Printed";
	
	public DocMetadataEditor(Composite parent, int style) {
		this(parent, style, null);
	}
	
	public DocMetadataEditor(Composite parent, int style, final String message) {
		super(parent, style);
		setLayout(new GridLayout(2, false));
		
		if(message != null){
			Group warnGrp = new Group(this, SWT.NONE);
			warnGrp.setLayout(new GridLayout(1, false));
			warnGrp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			Label warning = new Label(warnGrp, SWT.NONE);
			warning.setText(message);
			warning.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
		}
		
		Label lblNewLabel = new Label(this, SWT.NONE);
		lblNewLabel.setText("Title:");
		
		titleText = new Text(this, SWT.BORDER);
		GridData gd_titleText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
//		gd_titleText.widthHint = 366;
//		gd_titleText.widthHint = 200;
		titleText.setLayoutData(gd_titleText);
		
		Label lblAuthority = new Label(this,SWT.NONE);
		lblAuthority.setText("Authority:");
		
		authorityText = new Text(this, SWT.BORDER);
		authorityText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label backlinkLbl = new Label(this, SWT.NONE);
		backlinkLbl.setText("Backlink:");
				
//		backlink = new Link(this, SWT.NONE);
		backlink = new Text(this, SWT.BORDER);
		backlink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	    
//		backlink.addSelectionListener(new SelectionAdapter(){
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//               //System.out.println("You have selected: "+e.text);
//               //  Open default external browser 
//               org.eclipse.swt.program.Program.launch(e.text);
//            }
//        });
		
		Label lblExtId = new Label(this, SWT.NONE);
		lblExtId.setText("External ID:");
		
		extIdText = new Text(this, SWT.BORDER);
		extIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblHierarchy = new Label(this, SWT.NONE);
		lblHierarchy.setText("Hierarchy:");
		
	    hierarchyTree = new Tree(this, SWT.BORDER);
	    hierarchyTree.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	    
	    new Label(this, 0);
	    Button editHierarchyBtn = new Button(this, 0);
	    editHierarchyBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	    editHierarchyBtn.setText("Edit...");
	    SWTUtil.onSelectionEvent(editHierarchyBtn, e -> {
	    	String hierarchy = DialogUtil.showTextInputDialog(getShell(), "Enter hierarchy", "Enter the hierarchy separated by slashes (/). Slashes at the start and end are ignored.", currentHierarchy);
	    	if (hierarchy == null) {
	    		return;
	    	}
	    	hierarchy = StringUtils.strip(hierarchy, "/");
	    	if (!StringUtils.equals(currentHierarchy, hierarchy)) {
	    		currentHierarchy = hierarchy;
	    		updateHierarchyTree();
	    	}
	    });
		
		Label lblAuthor = new Label(this, SWT.NONE);
		lblAuthor.setText("Author:");
		
		authorText = new Text(this, SWT.BORDER);
		authorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblUploaded = new Label(this, SWT.NONE);
		lblUploaded.setText("Uploaded:");
		
		uploadedLabel = new Label(this, SWT.BORDER);
		uploadedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblGenre = new Label(this, SWT.NONE);
		lblGenre.setText("Genre:");
		
		genreText = new Text(this, SWT.BORDER);
		genreText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblWriter = new Label(this, SWT.NONE);
		lblWriter.setText("Writer:");
		
		writerText = new Text(this, SWT.BORDER);
		writerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//		new Label(this, SWT.NONE);
		
		Label lblLang = new Label(this, SWT.NONE);
		lblLang.setText("Language:");
				
		langTable = new LanguageSelectionTable(this, 0);
		String[] tmpArr = new String[FinereaderUtils.ALL_LANGUAGES.size()];
		tmpArr = FinereaderUtils.ALL_LANGUAGES.toArray(tmpArr);
		langTable.setAvailableLanguages(tmpArr);
		GridData gdl = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gdl.heightHint = 100;
		langTable.setLayoutData(gdl);
		
		
		Label l0 = new Label(this, 0);
		l0.setText("Script type: ");
		
		Composite c = new Composite(this, SWT.NONE);
		c.setLayout(new GridLayout(2, true));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		scriptTypeCombo = new Combo(c, SWT.DROP_DOWN | SWT.READ_ONLY);
		scriptTypeCombo.setItems(new String[]{ScriptType.HANDWRITTEN.getStr(), PRINT_META_SCRIPTTYPE});
		scriptTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scriptTypeCombo.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				if(PRINT_META_SCRIPTTYPE.equals(scriptTypeCombo.getText())){
					scriptTypeCombo2.setEnabled(true);
				} else {
					scriptTypeCombo2.setText("");
//					scriptTypeCombo2.select(-1);
					scriptTypeCombo2.setEnabled(false);
				}
			}
		});
		scriptTypeCombo2 = new Combo(c, SWT.DROP_DOWN | SWT.READ_ONLY);
//		scriptTypeCombo2.setItems(EnumUtils.stringsArray(ScriptType.class));
		scriptTypeCombo2.setItems(new String[]{"", ScriptType.NORMAL.getStr(), ScriptType.NORMAL_LONG_S.getStr(), 
				ScriptType.GOTHIC.getStr()});
		scriptTypeCombo2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		

		
		Composite dateComposite = new Composite(this, SWT.NONE);
		dateComposite.setLayout(new GridLayout(5, true));
		dateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblCreateDates = new Label(dateComposite, SWT.NONE);
		lblCreateDates.setText("Date of writing:");
		lblCreateDates.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		enableCreatedFromBtn = new Button(dateComposite, SWT.CHECK);
		enableCreatedFromBtn.setSelection(false);
		enableCreatedFromBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				createdFrom.setEnabled(enableCreatedFromBtn.getSelection());
			}
		});
		enableCreatedFromBtn.setText("From:");		
//		createdFrom = new SCSimpleDateTimeWidget(dateComposite, SWT.NONE);
		createdFrom = new DateChooserCombo(dateComposite, SWT.NONE);
		createdFrom.setEnabled(false);

		enableCreatedToBtn = new Button(dateComposite, SWT.CHECK);
		enableCreatedToBtn.setSelection(false);
		enableCreatedToBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				createdTo.setEnabled(enableCreatedToBtn.getSelection());
			}
		});
		enableCreatedToBtn.setText("To:");
		enableCreatedToBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		createdTo = new DateChooserCombo(dateComposite, SWT.NONE);
		createdTo.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		createdTo.setEnabled(false);
		
		Label lblDescription = new Label(this, SWT.None);
		lblDescription.setText("Description: ");
		lblDescription.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		
//		lblDescription.setLayoutData(GridData.HORIZONTAL_ALIGN_BEGINNING);
				
		descriptionText = new Text(this, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData descTextGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		final int nVisibleLines = 3;
		GC gc = new GC(descriptionText);
		int heightHint = nVisibleLines * gc.getFontMetrics().getHeight();
		descTextGridData.heightHint = heightHint;
		gc.dispose();
		descriptionText.setLayoutData(descTextGridData);
		
		userMd = new DocUserMetadataEditor(this, SWT.NONE);
		GridData gd_userMd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_userMd.heightHint = heightHint;
		userMd.setLayoutData(gd_userMd);

		
//		descriptionText.setLayoutData(GridData.FILL_BOTH);
		
		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayout(new GridLayout(2, true));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		openEditDeclManagerBtn = new Button(comp, SWT.PUSH);
		openEditDeclManagerBtn.setText("Editorial Declaration...");
		openEditDeclManagerBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		saveBtn = new Button(comp, SWT.NONE);
		saveBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		saveBtn.setImage(Images.DISK);
		saveBtn.setText("Save");
		
		addListener();
		
		setMetadataToGui(null);
	}
	
	void addListener() {
		listener = new DocMetadataEditorListener(this);
	}
	
	public void updateMetadataFromCurrentDoc() {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		TrpDoc doc = Storage.getInstance().getDoc();
		if (mw == null || doc == null  || doc.getMd() == null) {
			return;
		}
		updateMetadataObjectFromGui(doc.getMd());
	}
	
	void saveMd() {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		TrpDoc doc = Storage.getInstance().getDoc();
		
		if (mw == null || doc == null)
			return;
		
		boolean hasChanged = updateMetadataObjectFromGui(doc.getMd());
		
		if (!hasChanged) {
			return;
		}
		
		logger.debug("doc-md has changed - saving in background!");

		if (saveThread != null && saveThread.isAlive()) {
			saveThread.interrupt();
		}
		
		saveThread = new Thread(() -> { mw.saveDocMetadata(); } );
		saveThread.start();
	}
	
	/**
	 * Compare mutable md fields. Returns true if a value has changed. Empty string vs. null value does not count as change.
	 * 
	 * @param md
	 * @return
	 */
	public boolean hasChanged(TrpDocMetadata md) {
		if (md == null)
			return false;	
		
		if (!StrUtil.equalsContent(titleText.getText(), md.getTitle())) {
			logger.debug("title changed: '" + titleText.getText() + "' <-> '" + md.getTitle() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(authorityText.getText(), md.getAuthority())) {
			logger.debug("authority changed: '" + authorityText.getText() + "' <-> '" + md.getAuthority() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(extIdText.getText(), md.getExternalId())) {
			logger.debug("external ID changed: '" + extIdText.getText() + "' <-> '" + md.getExternalId() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(authorText.getText(), md.getAuthor())) {
			logger.debug("author changed: '" + authorText.getText() + "' <-> '" + md.getAuthor() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(genreText.getText(), md.getGenre())) {
			logger.debug("genre changed: '" + genreText.getText() + "' <-> '" + md.getGenre() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(writerText.getText(), md.getWriter())) {
			logger.debug("writer changed: '" + writerText.getText() + "' <-> '" + md.getWriter() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(descriptionText.getText(), md.getDesc())) {
			logger.debug("description changed: '" + descriptionText.getText() + "' <-> '" + md.getDesc() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(langTable.getSelectedLanguagesString(), md.getLanguage())) {
			logger.debug("language changed: '" + langTable.getSelectedLanguagesString() + "' <-> '" + md.getLanguage() + "'");
			return true;
		}
		
		if (!CoreUtils.equalsObjects(getSelectedScriptType(), md.getScriptType())) {
			logger.debug("scriptType changed: '" + getSelectedScriptType() + "' <-> '" + md.getScriptType() + "'");
			return true;
		}
		
		if (!CoreUtils.equalsObjects(getSelectedCreatedFromDate(), md.getCreatedFromDate())) {
			logger.debug("createDateFrom changed: '" + getSelectedCreatedFromDate() + "' <-> '" + md.getCreatedFromDate() + "'");
			return true;
		}
		if (!CoreUtils.equalsObjects(getSelectedCreatedToDate(), md.getCreatedToDate())) {
			logger.debug("createDateTo changed: '" + getSelectedCreatedToDate() + "' <-> '" + md.getCreatedToDate() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(currentHierarchy, md.getHierarchy())) {
			logger.debug("hierarchy changed: '" + currentHierarchy + "' <-> '" + md.getHierarchy() + "'");
			return true;
		}
		if (!StrUtil.equalsContent(backlink.getText(), md.getBacklink())) {
			logger.debug("backlink changed: '" + backlink.getText() + "' <-> '" + md.getBacklink() + "'");
			return true;
		}
		
		return false;
	}
	
	public boolean updateMetadataObjectFromGui(TrpDocMetadata md) {	
		if (!hasChanged(md))
			return false;
		
		logger.debug("updating doc-metadata object: "+md);
				
		//don't update missing fields with empty strings
		md.setTitle(StringUtils.isEmpty(titleText.getText()) ? null : titleText.getText());
		md.setAuthority(StringUtils.isEmpty(authorityText.getText()) ? null : authorityText.getText());
		md.setExternalId(StringUtils.isEmpty(extIdText.getText()) ? null : extIdText.getText());
		md.setAuthor(StringUtils.isEmpty(authorText.getText()) ? null : authorText.getText());
		md.setGenre(StringUtils.isEmpty(genreText.getText()) ? null : genreText.getText());
		md.setWriter(StringUtils.isEmpty(writerText.getText()) ? null : writerText.getText());
		md.setDesc(StringUtils.isEmpty(descriptionText.getText()) ? null : descriptionText.getText());
		md.setLanguage(StringUtils.isEmpty(langTable.getSelectedLanguagesString()) ? null : langTable.getSelectedLanguagesString());
		md.setScriptType(getSelectedScriptType());

		md.setCreatedFromDate(getSelectedCreatedFromDate());
		md.setCreatedToDate(getSelectedCreatedToDate());
		
		md.setHierarchy(currentHierarchy);
		md.setBacklink(StringUtils.isEmpty(backlink.getText()) ? null : backlink.getText());
		
		logger.debug("doc-metadata object after update: "+md);
		return true;
	}
	
	Date getSelectedCreatedFromDate() {
		if(isCreatedFromEnabled()){
			return getCreatedFromDate();
		} else {
			return null;
		}
	}
	
	Date getSelectedCreatedToDate() {
		if(isCreatedFromEnabled()){
			return getCreatedToDate();
		} else {
			return null;
		}
	}	
	
	ScriptType getSelectedScriptType() {
		final String stStr = getScriptTypeCombo().getText();
		ScriptType st;
		if(PRINT_META_SCRIPTTYPE.equals(stStr)){
			st = EnumUtils.fromString(ScriptType.class, getScriptTypeCombo2().getText());
		} else {
			st = EnumUtils.fromString(ScriptType.class, getScriptTypeCombo().getText());
		}
		logger.trace("script type1: "+st);
		return st;
	};
	
	public void setMetadataToGui(TrpDocMetadata md) {
		listener.setDeactivate(true);
		
		titleText.setText(md!=null && md.getTitle()!=null ? md.getTitle() : "");
		authorityText.setText(md!=null && md.getAuthority() !=null ? md.getAuthority() : "NA");
//	    backlink.setText(md!=null && md.getBacklink()!=null ? "<a href=\"" + md.getBacklink() +"\">"+md.getBacklink()+"</a>" : "NA");
	    backlink.setText(md!=null && md.getBacklink()!=null ? md.getBacklink() : "");
	    extIdText.setText(md!=null && md.getExternalId() !=null ? md.getExternalId() : "NA");
		authorText.setText(md!=null && md.getAuthor()!=null ? md.getAuthor() : "");
		uploadedLabel.setText(md!=null && md.getUploadTime()!=null&&md.getDocId()!=-1 ? md.getUploadTime().toString() : "NA");
		genreText.setText(md!=null && md.getGenre()!=null ? md.getGenre() : "");
		writerText.setText(md!=null && md.getWriter() != null ? md.getWriter() : "");
		descriptionText.setText(md!=null && md.getDesc() != null ? md.getDesc() : "");
		langTable.setSelectedLanguages(md!=null ? md.getLanguage() : "");
		initScriptTypeCombos(md!=null ? md.getScriptType() : null);
				
//		logger.debug("created from " + md.getCreatedFromDate());
//		logger.debug("created to " + md.getCreatedToDate());
		updateDateChooser(enableCreatedFromBtn, createdFrom, md != null ? md.getCreatedFromDate() : null);
		updateDateChooser(enableCreatedToBtn, createdTo, md != null ? md.getCreatedToDate() : null);
		
		//if (md!=null && md.getHierarchy()==null){
		hierarchyTree.clearAll(true);
		TreeItem [] items = hierarchyTree.getItems();
		for (int i = 0; i<items.length; i++){
			items[i].dispose();
		}
		hierarchyTree.setSize(0, 0);
		
		currentHierarchy = md!=null ? md.getHierarchy() : null;
		currentExternalId = md!=null ? md.getExternalId() : null;
		updateHierarchyTree();
		
//		if (md!=null && md.getHierarchy()!=null){		
////			hierarchyTree = new Tree(this, SWT.BORDER);
////		    hierarchyTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
//			//logger.debug("hierarchy " + md.getHierarchy());
//			
//			/*
//			 * extId is last level in hierarchy - sometimes it contains a slash; if so the extId needs to be ignored and added later on to the hierarchy tree
//			 */
//			String hierarchy =  md.getHierarchy();
//			boolean addExtIdAsTreeItem = false;
//			if (md.getExternalId() !=null && md.getHierarchy().endsWith(md.getExternalId())){
//				hierarchy = hierarchy.substring(0, hierarchy.lastIndexOf(md.getExternalId()));
//				addExtIdAsTreeItem = true;
//			}
//			
//			String[] levels = hierarchy.split("/");
//			List<TreeItem> treeItems = new ArrayList<TreeItem>();
//	        for (int i = 0; i < levels.length; i++) {
//	        	TreeItem treeItem;
//	        	if (i==0){
//	        		treeItem = new TreeItem(hierarchyTree, 0);
//	        	}
//	        	else{
//	        		treeItem = new TreeItem(treeItems.get(i-1), 0);
//	        		treeItems.get(i-1).setExpanded(true);
//	        	}
//	        	
//	        	treeItem.setText("" + levels[i]);
//	        	treeItems.add(treeItem);
//
//		    }
//	        
//	        if (addExtIdAsTreeItem){
//	        	TreeItem treeItem = new TreeItem(treeItems.get(treeItems.size()-1), 0);
//	        	treeItems.get(treeItems.size()-1).setExpanded(true);
//	        	treeItem.setText(md.getExternalId());
//	        	treeItems.add(treeItem);
//	        	
//	        }
//	        
//	        
//	        hierarchyTree.layout();
//	        hierarchyTree.redraw();
//		}
		this.layout();
		this.redraw();
		
		listener.setDeactivate(false);
	}
	
	private void updateHierarchyTree() {
		hierarchyTree.removeAll();
		if (!StringUtils.isEmpty(currentHierarchy)) {
			/*
			 * extId is last level in hierarchy - sometimes it contains a slash; if so the extId needs to be ignored and added later on to the hierarchy tree
			 */
			String hierarchy = currentHierarchy;
			boolean addExtIdAsTreeItem = false;
			if (currentExternalId !=null && currentHierarchy.endsWith(currentExternalId)){
				hierarchy = hierarchy.substring(0, hierarchy.lastIndexOf(currentExternalId));
				addExtIdAsTreeItem = true;
			}
			
			String[] levels = hierarchy.split("/");
			List<TreeItem> treeItems = new ArrayList<TreeItem>();
	        for (int i = 0; i < levels.length; i++) {
	        	TreeItem treeItem;
	        	if (i==0){
	        		treeItem = new TreeItem(hierarchyTree, 0);
	        	}
	        	else{
	        		treeItem = new TreeItem(treeItems.get(i-1), 0);
	        		treeItems.get(i-1).setExpanded(true);
	        	}
	        	
	        	treeItem.setText("" + levels[i]);
	        	treeItems.add(treeItem);

		    }
	        
	        if (addExtIdAsTreeItem){
	        	TreeItem treeItem = new TreeItem(treeItems.get(treeItems.size()-1), 0);
	        	treeItems.get(treeItems.size()-1).setExpanded(true);
	        	treeItem.setText(currentExternalId);
	        	treeItems.add(treeItem);
	        }
	        
	        hierarchyTree.layout();
	        hierarchyTree.redraw();
		}
	}
	
	/**
	 * 
	 * @param b
	 * @param c
	 * @param date
	 */
	private void updateDateChooser(Button b, DateChooserCombo c, Date date) {
		c.setValue(date!=null ? date : null);
		c.setEnabled(date!=null);
		b.setSelection(date!=null);
	}
	
	public void updateVisibility(boolean setEnabled){
		
		titleText.setEnabled(setEnabled);
		authorityText.setEnabled(setEnabled);
		extIdText.setEnabled(setEnabled);
		authorText.setEnabled(setEnabled);
		hierarchyTree.setEnabled(setEnabled);
		genreText.setEnabled(setEnabled);
		writerText.setEnabled(setEnabled);
		langTable.setEnabled(setEnabled);
		scriptTypeCombo.setEnabled(setEnabled);
		scriptTypeCombo2.setEnabled(setEnabled);
		descriptionText.setEnabled(setEnabled);
		enableCreatedToBtn.setEnabled(setEnabled);
		enableCreatedFromBtn.setEnabled(setEnabled);
		saveBtn.setEnabled(setEnabled);
		openEditDeclManagerBtn.setEnabled(setEnabled);
		
	}
	
	private void initScriptTypeCombos(ScriptType st) {
		if(st == null){
			scriptTypeCombo.select(-1);
			scriptTypeCombo2.select(-1);
			scriptTypeCombo2.setEnabled(false);
		} else if(st.equals(ScriptType.HANDWRITTEN)){
			scriptTypeCombo.select(scriptTypeCombo.indexOf(ScriptType.HANDWRITTEN.getStr()));
			scriptTypeCombo2.select(-1);
			scriptTypeCombo2.setEnabled(false);
		} else {
			scriptTypeCombo.select(scriptTypeCombo.indexOf(PRINT_META_SCRIPTTYPE));
			scriptTypeCombo2.select(scriptTypeCombo2.indexOf(st.getStr()));
			scriptTypeCombo2.setEnabled(true);
		}
	}

	public Text getTitleText() {
		return titleText;
	}
	
	public DocUserMetadataEditor getUserMd() {
		return userMd;
	}

	public Text getAuthorText() {
		return authorText;
	}


	public Label getUploadedLabel() {
		return uploadedLabel;
	}

	public Text getGenreText() {
		return genreText;
	}

	public Text getWriterText() {
		return writerText;
	}
	
	public Text getDescriptionText() {
		return descriptionText;
	}
	
	public Combo getScriptTypeCombo() { 
		return scriptTypeCombo;
	}
	
	public Combo getScriptTypeCombo2() { 
		return scriptTypeCombo2;
	}
	
	public LanguageSelectionTable getLangTable(){
		return langTable;
	}
	
	public Date getCreatedFromDate() {
		return createdFrom.getValue();
	}
		
	public boolean isCreatedFromEnabled(){
		return enableCreatedFromBtn.getSelection();
	}
	
	public Date getCreatedToDate() {
		return createdTo.getValue();
	}
	
	public boolean isCreatedToEnabled(){
		return enableCreatedToBtn.getSelection();
	}
	
}
