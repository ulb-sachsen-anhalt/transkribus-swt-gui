package eu.transkribus.swt_gui.mainwidget.menubar;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.transkribus.swt.util.Images;
import eu.transkribus.swt_gui.Msgs;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class TrpMenuBar {
	static class CascadeMenu {
		public CascadeMenu(Menu m, MenuItem mi) {
			super();
			this.m = m;
			this.mi = mi;
		}
		public Menu m;
		public MenuItem mi;
	}
	
	Point point;
	
//	TrpMainWidget mw;
	
	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	Menu menuBar;
	
	//
	CascadeMenu docsMenu;
	MenuItem openLocalDocItem;
	MenuItem uploadItem;
	MenuItem exportItem;
	MenuItem syncXmlItem;
	MenuItem syncTextItem;
	MenuItem syncTextLinesItem;
	
	// OLD MENU STUFF:

	// file menu:
//	MenuItem fileMenuItem;
//	Menu fileMenu;
	CascadeMenu fileMenu;
	
	MenuItem saveTranscriptionToNewFileMenuItem;
	MenuItem saveTranscriptionMenuItem;
	
	MenuItem replaceImageItem;

	MenuItem openLocalPageFileItem;
//	MenuItem uploadImagesFromPdfFileItem;
	
	MenuItem syncWordsWithLinesMenuItem;
	
	MenuItem proxySettingsMenuItem;
	MenuItem autoSaveSettingsMenuItem;
	MenuItem createThumbsMenuItem;
	MenuItem movePagesByFilelistItem;
	MenuItem deletePageMenuItem;
	MenuItem addPageMenuItem;
	
	/*
	 * experimental - not shown to user
	 */
	MenuItem addReplacements;
	MenuItem convertTextDiffs;
	
	/*
	 * this handles the reading order of lines when the sorting is YX except for lines on the same horizontal line
	 * -> in this case sort via XY
	 */
	MenuItem applyReadingOrder_XY_If_Y_Overlaps;
	MenuItem applyReadingOrder_4_Columns;
	
	CascadeMenu collMenu;
	//MenuItem manageCollectionsItem;
	MenuItem userActivityItem;
		
	CascadeMenu languageMenu;
	
	// view menu:
	CascadeMenu viewMenu;
	MenuItem viewMenuItem;
	MenuItem viewSettingsMenuItem;
//	MenuItem showLeftViewMenuItem;
//	MenuItem showRightViewMenuItem;
//	MenuItem showBottomViewMenuItem;
	
	// segmentation menu:
	CascadeMenu segmentationMenu;
	MenuItem segmentationMenuItem;
	MenuItem showAllMenuItem;
	MenuItem hideAllMenuItem;
	MenuItem showPrintspaceMenuItem;
	MenuItem showRegionsMenuItem;
	MenuItem showLinesMenuItem;
	MenuItem showBaselinesMenuItem;
	MenuItem showWordsMenuItem;
	
	MenuItem aboutMenuItem;
	MenuItem helpMenuItem;
	MenuItem changelogMenuItem;
	MenuItem exitItem;
	MenuItem updateMenuItem;
	
//	MenuItem loadTestsetMenuItem;
	
	MenuItem installMenuItem;
//	MenuItem tipsOfTheDayMenuItem;
	MenuItem bugReportItem;
	
	MenuItem loadLastDoc;


	public TrpMenuBar(Decorations parent) {		
		init(parent);
	}
	
	CascadeMenu createCascadeMenu(Menu parent, Image img, String txt) {
		MenuItem mi = createItem(parent, SWT.CASCADE, img, txt);
		Menu m = new Menu(mi);
		mi.setMenu(m);
		
		return new CascadeMenu(m, mi);
	}
	
	MenuItem createItem(Menu parent, int style, Image img, String txt) {
		MenuItem mi = new MenuItem(parent, style);
		mi.setImage(img);
		mi.setText(txt);
		
		return mi;
	}
			
	private void init(Decorations parent) {
		menuBar = new Menu(parent, SWT.POP_UP);
		
		// DOCUMENTS MENU:
		docsMenu = createCascadeMenu(menuBar, null, "&Document");
		
		openLocalDocItem = createItem(docsMenu.m, SWT.NONE, Images.FOLDER, "Open local document...");	
		uploadItem = createItem(docsMenu.m, SWT.NONE, Images.FOLDER_IMPORT, "Import document(s)...");
		exportItem = createItem(docsMenu.m, SWT.NONE, Images.FOLDER_GO, "Export document...");
		syncXmlItem = createItem(docsMenu.m, SWT.NONE, Images.getOrLoad("/icons/database_refresh.png"), "Sync local transcriptions with doc...");
		syncTextItem = createItem(docsMenu.m, SWT.NONE, Images.getOrLoad("/icons/page_add.png"), "Sync local text files with doc...");
		addPageMenuItem = createItem(docsMenu.m, 0, Images.getOrLoad("/icons/image_add.png"), "Add page(s) to document on server...");
		addReplacements = createItem(docsMenu.m, 0, Images.getOrLoad("/icons/page_attach.png"), "Search/Replace chosen chars in transcript");
		addReplacements.setToolTipText("Choose a txt file with a list of replacements, first the string to be replaced and after a comma the new string, e.g. \"ſ,s\" (to replace long s with round s). Additional replacements can be entered in new lines.");
		if (Storage.getInstance().isAdminLoggedIn()){
			convertTextDiffs = createItem(docsMenu.m, 0, Images.getOrLoad("/icons/page_attach.png"), "Convert text diffs intor abbrev tag...");
		}
		applyReadingOrder_XY_If_Y_Overlaps = createItem(docsMenu.m, 0, Images.getOrLoad("/icons/page_attach.png"), "Apply new shape sorting - if shapes appear on same height, sort in a row...");
		applyReadingOrder_XY_If_Y_Overlaps.setToolTipText("Sorts shapes according to Y coordinate except lines on the same horizontal line - in this case sorting is done with the X coordiante");
		
		applyReadingOrder_4_Columns = createItem(docsMenu.m, 0, Images.getOrLoad("/icons/page_attach.png"), "Apply column wise region/line sorting...");
		applyReadingOrder_4_Columns.setToolTipText("Sorts shapes according to Y coordinate - if several columns appear start with first column and go on with second (ltr or rtl)");
		
		createThumbsMenuItem = createItem(docsMenu.m, SWT.CHECK, null, "Create thumbs when opening local folder");
		movePagesByFilelistItem = createItem(docsMenu.m, SWT.PUSH, null, "Sort pages by filename list...");
		syncTextLinesItem = createItem(docsMenu.m, SWT.NONE, Images.getOrLoad("/icons/page_attach.png"), "Sync text of regions to lines...");
		
		// FILE MENU:
		fileMenu = createCascadeMenu(menuBar, null, "&Page");
		
		saveTranscriptionMenuItem = createItem(fileMenu.m, SWT.NONE, Images.DISK, "Save");
		saveTranscriptionToNewFileMenuItem = createItem(fileMenu.m, SWT.NONE, Images.getOrLoad("/icons/page_save.png"), "Save transcription to new file");
		replaceImageItem = createItem(fileMenu.m, SWT.NONE, Images.IMAGE_EDIT, "Replace image of current page on server...");
		openLocalPageFileItem = createItem(fileMenu.m, 0, null, "Open local page file for current page...");		
		deletePageMenuItem = createItem(fileMenu.m, 0, null, "Delete current page from server");
		syncWordsWithLinesMenuItem = createItem(fileMenu.m, SWT.NONE, null, "Sync word transcription with text in lines");
		
		collMenu = createCascadeMenu(menuBar, null, "&Collections");
		//manageCollectionsItem = createItem(collMenu.m, SWT.NONE, null, "Manage...");
		userActivityItem = createItem(collMenu.m, SWT.NONE, null, "Show user activity...");
		
		// VIEW menu
		viewMenu = createCascadeMenu(menuBar, null, "&View");
		viewSettingsMenuItem = createItem(viewMenu.m, SWT.PUSH, Images.getOrLoad("/icons/palette.png"), "Change viewing settings");
				
		// SEGMENTATION SUB-MENU:
		segmentationMenu = createCascadeMenu(viewMenu.m, null, "Segmentation");
		
		showAllMenuItem = createItem(segmentationMenu.m, SWT.PUSH, null, "Show all");		
		hideAllMenuItem = createItem(segmentationMenu.m, SWT.PUSH, null, "Hide all");
		
		showRegionsMenuItem = createItem(segmentationMenu.m, SWT.CHECK, null, "Show regions");
		showLinesMenuItem = createItem(segmentationMenu.m, SWT.CHECK, null, "Show lines");
		showBaselinesMenuItem = createItem(segmentationMenu.m, SWT.CHECK, null, "Show baselines");
		showWordsMenuItem = createItem(segmentationMenu.m, SWT.CHECK, null, "Show words");
		showPrintspaceMenuItem = createItem(segmentationMenu.m, SWT.CHECK, null, "Show printspace");
		
//		tipsOfTheDayMenuItem = createItem(viewMenu.m, SWT.PUSH, null, "Show tips of the day...");

		// HELP MENU:
//		mntmhelp = new MenuItem(menuBar, SWT.CASCADE);
//		mntmhelp.setText("&Help");
//		
//		helpMenu = new Menu(mntmhelp);
//		mntmhelp.setMenu(helpMenu);
		
		loadLastDoc = createItem(menuBar, SWT.CHECK, null,  "Load last modified doc at start");
		loadLastDoc.setToolTipText("Choose this if you want to load the last modified document everytime you start Transkribus");
		loadLastDoc.setSelection(TrpConfig.getTrpSettings().isLoadMostRecentDocOnLogin());
		
//		loadLastDoc.addSelectionListener(new SelectionAdapter() {
//
//	        @Override
//	        public void widgetSelected(SelectionEvent event) {
//	            MenuItem btn = (MenuItem) event.getSource();
//	            System.out.println(btn.getSelection());
//	        }
//	    });


		proxySettingsMenuItem = createItem(menuBar, SWT.PUSH, Images.getOrLoad("/icons/server_connect.png"), "Proxy settings...");
		
		autoSaveSettingsMenuItem = createItem(menuBar, SWT.NONE, Images.DISK, "Autosave settings");
		
		if (false) {
		languageMenu = createCascadeMenu(menuBar, null, "Language (todo)");		
		for (Locale l : Msgs.LOCALES) {
			MenuItem li = new MenuItem(languageMenu.m, SWT.RADIO);
			li.setText(l.getDisplayName());
			li.setData(l);
			if (l.equals(TrpConfig.getTrpSettings().getLocale()))
				li.setSelection(true);
		}
		}

		updateMenuItem = createItem(menuBar, SWT.NONE, Images.getOrLoad("/icons/update_wiz.gif"), "Check for updates");
		
		installMenuItem = createItem(menuBar, SWT.NONE, Images.getOrLoad("/icons/install_wiz.gif"), "Install a specific version...");
		
		bugReportItem = createItem(menuBar, SWT.NONE, Images.BUG, "Send a bug report or feature request");
		
		changelogMenuItem = createItem(menuBar, SWT.NONE, Images.getOrLoad("/icons/new.png"), "What's new in Transkribus");
		
		aboutMenuItem = createItem(menuBar, SWT.NONE, Images.getOrLoad("/icons/information.png"), "About");
		
		helpMenuItem = createItem(menuBar, 0, Images.HELP, "Help");

		exitItem = createItem(menuBar, SWT.NONE, Images.getOrLoad("/icons/door_out.png"), "Exit");
	}
	
	// GETTERS AND SETTERS
	
	public Menu getMenuBar() { return menuBar; }
	
	public MenuItem getOpenLocalPageFileItem() {
		return openLocalPageFileItem;
	}
		
//	public MenuItem getManageCollectionsMenuItem() {
//		return manageCollectionsItem;
//	}
	
	public MenuItem getSyncWordsWithLinesMenuItem() {
		return syncWordsWithLinesMenuItem;
	}
	
	public MenuItem getSaveTranscriptionToNewFileMenuItem() {
		return saveTranscriptionToNewFileMenuItem;
	}
	
	public MenuItem getSaveTranscriptionMenuItem() {
		return saveTranscriptionMenuItem;
	}

	public MenuItem getSegmentationMenuItem() {
		return segmentationMenuItem;
	}
	
	public MenuItem getShowAllMenuItem() { return showAllMenuItem; }
	public MenuItem getHideAllMenuItem() { return hideAllMenuItem; }

//	public MenuItem getMntmhelp() {
//		return mntmhelp;
//	}
//
//	public Menu getHelpMenu() {
//		return helpMenu;
//	}

	public MenuItem getAboutMenuItem() {
		return aboutMenuItem;
	}
	
	public MenuItem getWhatsnewMenuItem() { return changelogMenuItem; }
	
	public MenuItem getUpdateMenuItem() { return updateMenuItem; }
	public MenuItem getInstallMenuItem() { return installMenuItem; }
//	public MenuItem getTipsOfTheDayMenuItem() { return tipsOfTheDayMenuItem; }
	public MenuItem getBugReportItem() { return bugReportItem; }

//	public MenuItem getLoadTestsetMenuItem() {
//		return loadTestsetMenuItem;
//	}

	public MenuItem getDeletePageMenuItem() {
		return deletePageMenuItem;
	}

	public void setDeletePageMenuItem(MenuItem deletePageMenuItem) {
		this.deletePageMenuItem = deletePageMenuItem;
	}

	public MenuItem getReplaceImageItem() {
		return replaceImageItem;
	}
	
	public void updateVisibility(boolean canManage){
		docsMenu.mi.setEnabled(canManage);
		collMenu.mi.setEnabled(canManage);
		fileMenu.mi.setEnabled(canManage);
	}

//	public void showAll(boolean value) {
//		this.viewSets.setShowAll(value);
//	}

}
