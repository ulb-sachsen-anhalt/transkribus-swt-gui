package eu.transkribus.swt.util;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.ListItemRenderer;
import org.eclipse.nebula.widgets.gallery.MyDefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.util.EnumUtils;
import eu.transkribus.swt.util.ThumbnailWidget.ThmbImgLoadThread;
import eu.transkribus.swt_gui.la.LayoutAnalysisDialog;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class ThumbnailManager extends Dialog{
	protected final static Logger logger = LoggerFactory.getLogger(ThumbnailManager.class);
	
	static final int TEXT_TO_THUMB_OFFSET = 5;
	
	public static final int THUMB_WIDTH = 80;
	public static final int THUMB_HEIGHT = 120;
	
	//protected List<ThmbImg> thumbs = new ArrayList<>();
	
	protected ThmbImgLoadThread loadThread;
	
//	TableViewer tv;
	protected Method setItemHeightMethod;
	protected Gallery gallery;
	
	protected Composite groupComposite;
	protected Composite labelComposite;
	
	//Combo labelCombo; 
	Combo statusCombo;

	Composite editCombos;
	
	Menu contextMenu;
	
	protected Label statisticLabel;
	protected Label pageNrLabel;
	protected Label totalTranscriptsLabel;
	
	protected GalleryItem group;
	
	protected Button reload, showOrigFn, createThumbs, startLA;
	//protected ToolItem reload, showOrigFn, createThumbs, showPageManager;
//	protected TextToolItem infoTi;

	protected List<URL> urls;
	protected List<String> names=null;
	
	protected List<TrpTranscriptMetadata> transcripts;
	
	protected List<Integer> nrTranscribedLines;
	
//	protected NoGroupRenderer groupRenderer;
	protected AbstractGridGroupRenderer groupRenderer;
	
	ThumbnailWidget thumbsWidget;
	
	static int thread_counter=0;
	
	static final Color lightGreen = new Color(Display.getCurrent(), 200, 255, 200);
	static final Color lightYellow = new Color(Display.getCurrent(), 255, 255, 200);
	static final Color lightRed = new Color(Display.getCurrent(), 252, 204, 188);
	private int totalLinesTranscribed = 0;
	
	private int maxWidth = 0;
	
	private static final boolean DISABLE_TRANSCRIBED_LINES=false;
	
	Shell shell;
	public Shell getShell() {
		return shell;
	}

	TrpDocMetadata docMd;
	TrpMainWidget mw;
		
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {

		shell.setSize(1100, 800);
		SWTUtil.centerShell(shell);
				
		shell.open();
		shell.layout();
		
		//take the thumbs from the widget to show in the manager
		thumbsWidget.loadThumbsIntoManager();
		addStatisticalNumbers();
		
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		while (display.readAndDispatch ());
		shell.dispose();
		
		return null;
	}
	
	public ThumbnailManager(Composite parent, int style, ThumbnailWidget thumbnailWidget, TrpMainWidget mw) {
		super(parent.getShell(), style |= (SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS | SWT.MAX));
		
		this.mw = mw;
		
		thumbsWidget = thumbnailWidget;
		
		if (Storage.getInstance().getDoc() == null){
			return;
		}

		if (Storage.getInstance().getDoc() != null){
			docMd = Storage.getInstance().getDoc().getMd();
		}
		
		shell = new Shell((Shell)parent, style);
		shell.setText("Document Manager");
	
		FillLayout l = new FillLayout();
		l.marginHeight = 5;
		l.marginWidth = 5;
		shell.setLayout(l);
		
		Composite container = new Composite(shell, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		
		groupComposite = new Composite(container, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.makeColumnsEqualWidth = true;
		groupComposite.setLayout(gl);
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		groupComposite.setLayoutData(gridData);
		
		labelComposite = new Composite(groupComposite, SWT.NONE);
		labelComposite.setLayout(new GridLayout(1, true));
		labelComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		statisticLabel = new Label(labelComposite, SWT.TOP);
		statisticLabel.setText("Loaded Document is " + docMd.getTitle() + " with ID " + docMd.getDocId());
		statisticLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		editCombos = new Composite(groupComposite, SWT.NONE);
		editCombos.setLayout(new GridLayout(2, true));
		editCombos.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		statusCombo = initComboWithLabel(editCombos, "Edit status: ", SWT.DROP_DOWN | SWT.READ_ONLY);
		statusCombo.setItems(EnumUtils.stringsArray(EditStatus.class));
		statusCombo.setEnabled(false);
		statusCombo.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				logger.debug(statusCombo.getText());
		        changeVersionStatus(statusCombo.getText());
			}


		});
		
//		labelCombo = initComboWithLabel(editCombos, "Edit label: ", SWT.DROP_DOWN | SWT.READ_ONLY);
//		String[] tmps = {"Upcoming feature - cannot be set at at the moment", "GT", "eLearning"};
//		labelCombo.setItems(tmps);
//		labelCombo.setEnabled(false);
		
		Label la = new Label(editCombos, SWT.CENTER);
		la.setText("Layout Analysis");
		startLA = new Button(editCombos, SWT.PUSH);
		startLA.setText("Configure");
		startLA.setEnabled(false);
		
		startLA.addListener(SWT.Selection, event-> {
			String pages = getPagesString();
            try {
				setup_layout_recognition(pages);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
		
		Composite btns = new Composite(container, 0);
		btns.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		btns.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		reload = new Button(btns, SWT.PUSH);
		reload.setToolTipText("Reload thumbs");
		reload.setImage(Images.REFRESH);
		reload.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				logger.debug("reloading thumbwidget...");
				reload();
			}
		});

		gallery = new Gallery(groupComposite, SWT.V_SCROLL | SWT.MULTI);
		gallery.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		group = new GalleryItem(gallery, SWT.NONE);
		
		groupRenderer = new NoGroupRenderer();
		//groupRenderer = new DefaultGalleryGroupRenderer();
		
		groupRenderer.setMinMargin(2);
		groupRenderer.setItemHeight(THUMB_HEIGHT);
		groupRenderer.setItemWidth(THUMB_WIDTH);
		groupRenderer.setAutoMargin(true);
		groupRenderer.setAlwaysExpanded(true);

		gallery.setGroupRenderer(groupRenderer);
		//gallery.setVirtualGroups(true);

		if (true) {
			MyDefaultGalleryItemRenderer ir = new MyDefaultGalleryItemRenderer();
//			DefaultGalleryItemRenderer ir = new DefaultGalleryItemRenderer();
			ir.setShowLabels(true);
			gallery.setItemRenderer(ir);

		} else {
			ListItemRenderer ir = new ListItemRenderer();
			ir.setShowLabels(true);
			gallery.setItemRenderer(ir);
		}
		
		gallery.addListener(SWT.MouseDoubleClick, new Listener() {

			@Override
			public void handleEvent(Event event) {					
					if (gallery.getSelectionCount()>=1) {
						Event e = new Event();
						e.index = group.indexOf(gallery.getSelection()[0]);
						
						btns.pack();
						
						thumbsWidget.notifyListeners(SWT.Selection, e);
					}
				
			}
		});
		
		gallery.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	if (gallery.getSelectionCount()>=1){
            		//set to true if edits are allowed
            		enableEdits(true);
            	}
            	else{
            		enableEdits(false);
            	}            	
            }


		});
		
	    contextMenu = new Menu(gallery);
	    gallery.setMenu(contextMenu);
	    //at the moment not enabled because not the total functionality to edit status, label is available
	    contextMenu.setEnabled(false);
	    
	    addMenuItems(contextMenu, EnumUtils.stringsArray(EditStatus.class));
	    
	    MenuItem movePage = new MenuItem(contextMenu, SWT.CASCADE);
	    movePage.setText("Move page to");
	    
	    Menu moveMenu = new Menu(contextMenu);
	    MenuItem moveFront = new MenuItem(moveMenu, SWT.NONE);
	    moveFront.setText("Beginning");
	    MenuItem moveEnd = new MenuItem(moveMenu, SWT.NONE);
	    moveEnd.setText("End");
	    MenuItem moveSpecific = new MenuItem(moveMenu, SWT.NONE);
	    moveSpecific.setText("Select page");
	    movePage.setMenu(moveMenu);
	    
	    

	    
//	    contextMenu.addMenuListener(new MenuAdapter()
//	    {
//	        public void menuShown(MenuEvent e)
//	        {
//	            MenuItem[] items = contextMenu.getItems();
//	            for (int i = 0; i < items.length; i++)
//	            {
//	                items[i].dispose();
//	            }
//	            MenuItem newItem = new MenuItem(contextMenu, SWT.NONE);
//	            newItem.setText("Menu for " + gallery.getSelection()[0].getText());
//	        }
//	    });
		
		shell.pack();
		

		
		
//		Composite container = new Composite(shell, SWT.NONE);
//		GridLayout layout = new GridLayout(1, false);
//		container.setLayout(layout);
//		
//		final ToolBar tb = new ToolBar(container, SWT.FLAT);
//		tb.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
//		
//		reload = new ToolItem(tb, SWT.PUSH);
//		reload.setToolTipText("Reload");
//		reload.setImage(Images.REFRESH);
//		reload.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				logger.debug("reloading thumbwidget...");
//				reload();
//			}
//		});
//		
//		createThumbs = new ToolItem(tb, SWT.PUSH);
//		createThumbs.setToolTipText("Create thumbnails for this local document");
//		createThumbs.setText("Create thumbs");
//				
//		groupComposite = new Composite(container, SWT.FILL);
//		GridLayout gl = new GridLayout(1, false);
//		groupComposite.setLayout(gl);
//		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
//		groupComposite.setLayoutData(gridData);
//		
//		labelComposite = new Composite(groupComposite, SWT.NONE);
//		labelComposite.setLayout(new GridLayout(1, true));
//		labelComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//
//		statisticLabel = new Label(labelComposite, SWT.TOP);
//		statisticLabel.setText("Document Overview of: " + docMd.getTitle());
//		statisticLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//					
//		gallery = new Gallery(groupComposite, SWT.V_SCROLL | SWT.MULTI);
//		gallery.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
//
//		group = new GalleryItem(gallery, SWT.NONE);
//		
//		groupRenderer = new NoGroupRenderer();
//		//groupRenderer = new DefaultGalleryGroupRenderer();
//		
//		groupRenderer.setMinMargin(2);
//		groupRenderer.setItemHeight(THUMB_HEIGHT);
//		groupRenderer.setItemWidth(THUMB_WIDTH);
//		groupRenderer.setAutoMargin(true);
//		groupRenderer.setAlwaysExpanded(true);
//
//		gallery.setGroupRenderer(groupRenderer);
//		//gallery.setVirtualGroups(true);
//
//		if (true) {
//			MyDefaultGalleryItemRenderer ir = new MyDefaultGalleryItemRenderer();
////			DefaultGalleryItemRenderer ir = new DefaultGalleryItemRenderer();
//			ir.setShowLabels(true);
//			gallery.setItemRenderer(ir);
//
//		} else {
//			ListItemRenderer ir = new ListItemRenderer();
//			ir.setShowLabels(true);
//			gallery.setItemRenderer(ir);
//		}
//		
//		gallery.addListener(SWT.MouseDoubleClick, new Listener() {
//
//			@Override
//			public void handleEvent(Event event) {					
//					if (gallery.getSelectionCount()>=1) {
//						Event e = new Event();
//						e.index = group.indexOf(gallery.getSelection()[0]);
//						
////						if (names != null && names.size()>e.index) {
////							infoTi.setText(names.get(e.index));
////						} else 
////							infoTi.setText(""+e.index);
////						infoTi.setWidth(100);
//						tb.pack();
//						
//						//notifyListeners(SWT.Selection, e);
//					}
//				
//			}
//		});
//				
//	    final Menu contextMenu = new Menu(gallery);
//	    gallery.setMenu(contextMenu);
//	    
//	    addMenuItems(contextMenu, EnumUtils.stringsArray(EditStatus.class));
//	    
////	    contextMenu.addMenuListener(new MenuAdapter()
////	    {
////	        public void menuShown(MenuEvent e)
////	        {
////	            MenuItem[] items = contextMenu.getItems();
////	            for (int i = 0; i < items.length; i++)
////	            {
////	                items[i].dispose();
////	            }
////	            MenuItem newItem = new MenuItem(contextMenu, SWT.NONE);
////	            newItem.setText("Menu for " + gallery.getSelection()[0].getText());
////	        }
////	    });
//		
//		shell.pack();
//		
//		open();
	}
	

	private void changeVersionStatus(String text){
		Storage storage = Storage.getInstance();
		String pages = getPagesString();
		
		String[] pageList = pages.split(",");
		
		if (!pages.equals("") && pageList.length >= 1){
			
			for (String page : pageList){
				int pageNr = Integer.valueOf(page);
				int colId = storage.getCurrentDocumentCollectionId();
				int docId = storage.getDocId();
				int transcriptId = storage.getPage().getCurrentTranscript().getTsId();
				try {
					storage.getConnection().updatePageStatus(colId, docId, pageNr, transcriptId, EditStatus.fromString(text), "test");
				} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
	
	private static Combo initComboWithLabel(Composite parent, String label, int comboStyle) {
		
		Label l = new Label(parent, SWT.LEFT);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		l.setText(label);
		
		Combo combo = new Combo(parent, comboStyle);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
				
		return combo;
	}
	
	protected void setup_layout_recognition(String pages) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		LayoutAnalysisDialog laD = new LayoutAnalysisDialog(shell);
		
		laD.create();
		//all selected pages are shown as default and are taken for segmentation
		laD.setPageSelectionToSelectedPages(pages);
		
		int ret = laD.open();

		if (ret == IDialogConstants.OK_ID) {
			try {
				List<String> jobIds = Storage.getInstance().analyzeLayoutOnLatestTranscriptOfPages(laD.getPages(),
						laD.isDoBlockSeg(), laD.isDoLineSeg(), laD.isDoWordSeg(), false, false, laD.getJobImpl(), null);
				
				if (jobIds != null && mw != null) {
					logger.debug("started jobs: "+jobIds.size());
					String jobIdsStr = mw.registerJobsToUpdate(jobIds);				
					Storage.getInstance().sendJobListUpdateEvent();
					mw.updatePageLock();
					
					DialogUtil.showInfoMessageBox(getShell(), jobIds.size()+ " jobs started", jobIds.size()+ " jobs started\nIDs:\n "+jobIdsStr);
				}
			} catch (Exception e) {
				mw.onError("Error", e.getMessage(), e);
			}
		}
	}

	private void addMenuItems(Menu contextMenu, String[] editStatusArray) {
		MenuItem tmp;
		
		Menu statusMenu = new Menu(contextMenu);
		MenuItem statusMenuItem = new MenuItem(contextMenu, SWT.CASCADE);
		statusMenuItem.setText("Edit Status");
		statusMenuItem.setMenu(statusMenu);
		//statusMenuItem.setEnabled(false);
		
		for (String editStatus : editStatusArray){
			tmp = new MenuItem(statusMenu, SWT.PUSH);
			tmp.setText(editStatus);
			tmp.addSelectionListener(new EditStatusMenuItemListener());
			//tmp.setEnabled(true);
		}
		
//		Menu labelMenu = new Menu(contextMenu);
//		MenuItem labelMenuItem = new MenuItem(contextMenu, SWT.CASCADE);
//		labelMenuItem.setText("Edit Label");
//		labelMenuItem.setMenu(labelMenu);
		
		Menu layoutMenu = new Menu(contextMenu);
		MenuItem layoutMenuItem = new MenuItem(contextMenu, SWT.CASCADE);
		layoutMenuItem.setText("Layout Analysis");
		layoutMenuItem.setMenu(layoutMenu);

		
		//just dummy labels for testing
//		tmp = new MenuItem(labelMenu, SWT.None);
//		tmp.setText("Upcoming feature - cannot be set at at the moment");
//		tmp.addSelectionListener(new EditLabelMenuItemListener());
//		tmp.setEnabled(false);
//		
//		tmp = new MenuItem(labelMenu, SWT.None);
//		tmp.setText("GT");
//		tmp.addSelectionListener(new EditLabelMenuItemListener());
//		tmp.setEnabled(false);
//		
//		tmp = new MenuItem(labelMenu, SWT.None);
//		tmp.setText("eLearning");
//		tmp.addSelectionListener(new EditLabelMenuItemListener());
//		tmp.setEnabled(false);
		
		tmp = new MenuItem(layoutMenu, SWT.None);
		tmp.setText("Configure");
		//tmp.setEnabled(false);
		
		tmp.addListener(SWT.Selection, event-> {
			String pages = getPagesString();
            try {
				setup_layout_recognition(pages);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });

	}
	
	private String getPagesString() {
		String pages = "";
		if (gallery.getSelectionCount() > 0) {
			for(GalleryItem si : gallery.getSelection()){
				int selectedPageNr = gallery.indexOf(si) + 1;
				String tmp = Integer.toString(selectedPageNr);
				pages += (pages.equals("")? tmp : ",".concat(tmp));
			}			
		}
		//logger.debug("pages String " + pages);
		return pages;
	}
	
	/*
	 * right click listener for the transcript table
	 * for the latest transcript the new status can be set with the right click button and by choosing the new status
	 */
	class EditStatusMenuItemListener extends SelectionAdapter {	
		
	    public void widgetSelected(SelectionEvent event) {
	    	System.out.println("You selected " + ((MenuItem) event.widget).getText());
	    	System.out.println("You selected cont.1 " + EnumUtils.fromString(EditStatus.class, ((MenuItem) event.widget).getText()));
//	    	System.out.println("You selected cont.2 " + EnumUtils.indexOf(EnumUtils.fromString(EditStatus.class, ((MenuItem) event.widget).getText())));

	    	//Storage.getInstance().getTranscriptMetadata().setStatus(EnumUtils.fromString(EditStatus.class, ((MenuItem) event.widget).getText()));
//				Storage.getInstance().saveTranscript(Storage.getInstance().getCurrentDocumentCollectionId(), null);
//				Storage.getInstance().setLatestTranscriptAsCurrent();
	    		/*
	    		 * change status: 
	    		 */
    		String tmp = ((MenuItem) event.widget).getText();
    		changeVersionStatus(tmp);   		
    		
			gallery.redraw();
			gallery.deselectAll();	    	
	    }
	}
	
	/*
	 * right click listener for the transcript table
	 * for the latest transcript the new status can be set with the right click button and by choosing the new status
	 */
	class EditLabelMenuItemListener extends SelectionAdapter {
	    public void widgetSelected(SelectionEvent event) {
//	    	System.out.println("You selected " + ((MenuItem) event.widget).getText());
//	    	System.out.println("You selected cont.1 " + EnumUtils.fromString(EditStatus.class, ((MenuItem) event.widget).getText()));
//	    	System.out.println("You selected cont.2 " + EnumUtils.indexOf(EnumUtils.fromString(EditStatus.class, ((MenuItem) event.widget).getText())));

//	    	Storage.getInstance().getTranscriptMetadata().setStatus(EnumUtils.fromString(EditStatus.class, ((MenuItem) event.widget).getText()));
//	    	try {
////				Storage.getInstance().saveTranscript(Storage.getInstance().getCurrentDocumentCollectionId(), null);
////				Storage.getInstance().setLatestTranscriptAsCurrent();
//				gallery.redraw();
//				gallery.deselectAll();
////			} catch (SessionExpiredException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
//			} catch (ServerErrorException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	    	
	    }
	}
		
	public void createGalleryItems(){
		//add text
		
		if(group.getItemCount() > 0){
			return;
		}

		for (int i=0; i<urls.size(); ++i) {
			final GalleryItem item = new GalleryItem(group, SWT.MULTI);
//			item.setText(0, "String 0\nString2");
//			item.setText(1, "String 1");
			item.setExpanded(true);
			
			item.setImage(Images.LOADING_IMG);
			item.setData("doNotScaleImage", new Object());
			
			String transcribedLinesText = thumbsWidget.determineItemColor(item, transcripts.get(i));
			
			totalLinesTranscribed += transcripts.get(i).getNrOfTranscribedLines();
			
			setItemText(item, i, transcribedLinesText);
			
		}
	}
	
	public void setUrls(List<URL> urls, List<String> names) {
		this.urls = urls;
		this.names = names;
	}
	
	public void setTranscripts(List<TrpTranscriptMetadata> transcripts2) {
		this.transcripts = transcripts2;
	}
	
//	public void disposeOldData() {
//		// dispose images:
//		for (ThmbImg th : thumbs) {
//			th.dispose();
//		}
//		thumbs.clear();
//		// dispose galler items:
//		for (GalleryItem item : group.getItems() ) {
//			item.clear();
//			item.dispose();
//		}
//	}
			
	public void reload() {
		thumbsWidget.reload();
		
		addStatisticalNumbers();

		
		
//		
//		
//		int N = !storage.isDocLoaded() ? 0 : storage.getDoc().getThumbUrls().size();
//		
//		/*
//		 * TODO: 
//		 * load transcripts
//		 * show info later on in thumbnail widget
//		 * 
//		 */
//		
//		logger.debug("reloading thumbs, nr of thumbs = "+ N);
//		
//		// remember index of selected item:
//		int selectedIndex=-1;
//		if (gallery.getSelectionCount() > 0) {
//			GalleryItem si = gallery.getSelection()[0];
////			logger.debug("si = "+si);
//			selectedIndex = gallery.indexOf(si);
//		}
//
//		// first: stop old thread
//		//stopActiveThread();
//		
//		// dispose old images:
//		disposeOldData();		
//		
////		logger.debug("reloading thumbs, is doc loaded: "+storage.isDocLoaded());
//		if (!storage.isDocLoaded())
//			return;
//		
//		// set url and page data:
//		setUrls(storage.getDoc().getThumbUrls(), storage.getDoc().getPageImgNames());
//		
//		setTranscripts(storage.getDoc().getTranscripts());
//		
//		//setImageTexts();
//		
//		// create new gallery items:
//		createGalleryItems();
//		
//
//
//		
//		// create a new thread and start it:
//		loadThread = thumbsWidget.loadThread;
//		boolean DO_THREADING = true;
//		if (DO_THREADING) {
//			logger.debug("starting thumbnail thread!");
//			loadThread.start();
//		}
//		else {
//			logger.debug("running thumbnail reload method");
//			loadThread.run(); // sequential version -> just call run() method
//		}
//		
//
//		
//		// select item previously selected:
//		logger.debug("previously selected index = "+selectedIndex+ " n-items = "+group.getItemCount());
//		if (selectedIndex >= 0 && selectedIndex<group.getItemCount()) {
//			GalleryItem it = group.getItem(selectedIndex);
//			logger.trace("it = "+it);
//			if (it != null) {
//				it.setExpanded(true);
//				gallery.setSelection( new GalleryItem[]{it} );
//			}
//		}	
	}
	
	private void addStatisticalNumbers() {
		
		Storage storage = Storage.getInstance();
		if (storage.getDoc() != null){
			if(pageNrLabel != null && !pageNrLabel.isDisposed()){
				pageNrLabel.dispose();
			}
			if(totalTranscriptsLabel != null && !totalTranscriptsLabel.isDisposed()){
				totalTranscriptsLabel.dispose();
			}
			pageNrLabel = new Label(labelComposite, SWT.NONE);
			pageNrLabel.setText("Nr of pages: " + storage.getDoc().getNPages());
			
			totalTranscriptsLabel = new Label(labelComposite, SWT.None);
			totalTranscriptsLabel.setText("Nr. of lines trancribed: " + totalLinesTranscribed);
	
			groupComposite.layout(true, true);
			
			//gallery.redraw();
		}
		
	}

	public void setItemTextAndBackground(GalleryItem galleryItem, int index) {
		if (SWTUtil.isDisposed(galleryItem))
			return;
		
		String transcribedLinesText = thumbsWidget.determineItemColor(galleryItem, transcripts.get(index));			
		
		setItemText(galleryItem, index, transcribedLinesText);
		

		
	}
	
	private void setItemText(GalleryItem item, int i, String transcribedLinesText) {
		String text=""+(i+1);
		
		
		GC gc = new GC(item.getParent());
		
		if (/*showOrigFn.getSelection() && */names!=null && i>=0 && i<names.size() && !names.get(i).isEmpty()) {
			//this shows the filename but is not really necessary in the thumbnail view
			//text+=": "+names.get(i);
			text+=": ";
			int tmp = gc.textExtent(text).x + 10;
			maxWidth = Math.max(maxWidth, tmp);
//			logger.debug("/////user id" + transcripts.get(i).getUserName());
//			logger.debug("/////status" + transcripts.get(i).getStatus());
			text+= (transcripts.get(i)!= null ? transcripts.get(i).getStatus().getStr() : "");
			if (transcripts.get(i)!= null){
				//tmp = gc.textExtent(transcripts.get(i).getStatus().getStr()).x + 10;
				tmp = gc.textExtent(text).x + 10;
				maxWidth = Math.max(maxWidth, tmp);
				//logger.debug("curr maxWidth " + maxWidth);
			}
			
			text+= (transcripts.get(i)!= null ? "\n"+transcripts.get(i).getUserName() : "");
			if (transcripts.get(i)!= null){
				tmp = gc.textExtent(transcripts.get(i).getUserName()).x + 10;
				maxWidth = Math.max(maxWidth, tmp);
			}
			
			if (!DISABLE_TRANSCRIBED_LINES && !transcribedLinesText.equals("")){
				text+=transcribedLinesText;
				tmp = gc.textExtent(transcribedLinesText).x + 10;
				maxWidth = Math.max(maxWidth, tmp);
			}
				
		}
//		else
//			text="Page "+(i+1);
		
		
		
		if (false) { // FIXME: try to wrap text...
		int s=0, e=2;
		String wrapped="";
		do {
			if (e>=text.length()) {
				wrapped+=text.substring(s, text.length());
				break;
			}
			
			int extent = gc.textExtent(text.substring(s, e)).x;
			if (extent>THUMB_WIDTH) {
				wrapped+=text.substring(s, e-1)+"\n";
				s = e;
				e++;
			}
			e++;
		} while(true);
		
		logger.debug("wrapped:\n"+wrapped);
		item.setText(wrapped);
		} else {
			int te = gc.textExtent(text).x + 10;
			int ty = gc.textExtent(text).y + 10;
			//groupRenderer.setItemWidth(Math.max(THUMB_WIDTH, te));
			
			groupRenderer.setItemWidth(Math.max(THUMB_WIDTH, maxWidth));		
			groupRenderer.setItemHeight(THUMB_HEIGHT + ty);
			//logger.debug("thumbText " + text);
			item.setText(text);
		}
		
		gc.dispose();
	}

//	public void setImages(List<Image> thumbImages) {
//		tv.setInput(thumbImages);
//	}
	
	private void enableEdits(boolean enable) {
		statusCombo.setEnabled(enable);
		//labelCombo.setEnabled(enable);
		startLA.setEnabled(enable);
		contextMenu.setEnabled(enable);
		
		
	}
	
	public Button getCreateThumbs() {
		return createThumbs;
	}

}
