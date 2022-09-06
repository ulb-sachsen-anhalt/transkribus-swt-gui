package eu.transkribus.swt_gui.upload;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.FtpConsts;
import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpDocDir;
import eu.transkribus.swt.mytableviewer.ColumnConfig;
import eu.transkribus.swt.mytableviewer.MyTableViewer;
import eu.transkribus.swt.util.DefaultTableColumnViewerSorter;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

// Not used anymore
@Deprecated
public class UploadFromFtpDialog extends Dialog {
	private final static Logger logger = LoggerFactory.getLogger(UploadFromFtpDialog.class);
	
	private final static String ENC_USERNAME = Storage.getInstance().getUser().getUserName().replace("@", "%40");
	
	private final static String INFO_MSG = 
			"You can upload folders containing image files to:\n\n"
			+ FtpConsts.FTP_PROT + FtpConsts.FTP_URL + "\n\n"
			+ "by using your favorite FTP client.\n"
			+ "For accessing the FTP server please use your\n"
			+ "Transkribus credentials.\n"
			+ "After the upload is done, you can ingest the\n"
			+ "documents into the platform by selecting the\n"
			+ "respective folders and the collection, to which\n"
			+ "the documents should be linked, within this Dialog.";
	
	
	String dirName;
	Link link;
	private Table docDirTable;
	private MyTableViewer docDirTv;
	List<TrpDocDir> docDirs = new ArrayList<>(0);
	String title;
	TrpCollection selColl;
	private List<TrpDocDir> selDocDirs;
	Text newCollText;
	Button addCollBtn;
	Button reloadBtn;
	Button helpBtn;
	Combo collCombo;
	Storage store = Storage.getInstance();
	TrpMainWidget mw = TrpMainWidget.getInstance();

	public static final String DIRECTORY_COL = "Directory";
	public static final String TITLE_COL = "Title";
	public static final String NR_OF_IMGS_COL = "Nr. of Images";
	public static final String SIZE_COL = "Size";
	public static final String CREATE_DATE_COL = "Last modified";
	
	public static final ColumnConfig[] DOC_DIR_COLS = new ColumnConfig[] {
		new ColumnConfig(DIRECTORY_COL, 180, true, DefaultTableColumnViewerSorter.ASC),
		new ColumnConfig(TITLE_COL, 110, false, DefaultTableColumnViewerSorter.ASC),
		new ColumnConfig(NR_OF_IMGS_COL, 110, false, DefaultTableColumnViewerSorter.ASC),
		new ColumnConfig(SIZE_COL, 100, false, DefaultTableColumnViewerSorter.ASC),
		new ColumnConfig(CREATE_DATE_COL, 150, false, DefaultTableColumnViewerSorter.ASC),
	};
	
	public UploadFromFtpDialog(Shell parentShell, TrpCollection selColl) {
		super(parentShell);
		this.selColl = selColl;
		
		setShellStyle(SWT.SHELL_TRIM | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
	}
	
	@Override protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout)container.getLayout();
		gridLayout.numColumns = 3;
//		GridData gridData = (GridData)container.getLayoutData();
//		gridData.widthHint = 600;
//		gridData.heightHint = 500;
//		gridData.minimumWidth = 600;
//		gridData.minimumHeight = 500;
//		container.setSize(700, 600);

		
		Label lblDir = new Label(container, SWT.NONE);
		lblDir.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDir.setText("Location:");
		link = new Link(container, SWT.NONE);
	    final String linkText = "<a href=\"" + FtpConsts.FTP_PROT + ENC_USERNAME + "@" + FtpConsts.FTP_URL + "\">"+ FtpConsts.FTP_PROT + FtpConsts.FTP_URL + "</a>";
	    link.setText(linkText);
	    helpBtn = new Button(container, SWT.NONE);
		helpBtn.setImage(Images.getOrLoad("/icons/help.png"));
		helpBtn.setToolTipText("What's that?");
		
//		Label lblDir = new Label(container, SWT.NONE);
//		lblDir.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
//		lblDir.setText("Directory:");
		docDirTv = new MyTableViewer(container, SWT.MULTI | SWT.FULL_SELECTION);
		docDirTv.setContentProvider(new ArrayContentProvider());
		docDirTv.setLabelProvider(new DocDirTableLabelProvider(docDirTv));
		
		docDirTable = docDirTv.getTable();
		docDirTable.setHeaderVisible(true);
		docDirTable.setLinesVisible(true);
		docDirTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		docDirTv.addColumns(DOC_DIR_COLS);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		reloadBtn = new Button(container, SWT.NONE);
		reloadBtn.setImage(Images.REFRESH);
		reloadBtn.setToolTipText("Reload the directories from the FTP server");
		
		Label lblCollections = new Label(container, SWT.NONE);
		lblCollections.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCollections.setText("Collection:");
		
		collCombo = new Combo(container, SWT.READ_ONLY);
		collCombo.setToolTipText("This is the collection the document will be added to - you can only upload to collections with Owner / Editor rights");
		collCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(container, SWT.NONE);
		
		Label lblCreateCollection = new Label(container, SWT.NONE);
		lblCreateCollection.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCreateCollection.setText("Create collection:");
		
		newCollText = new Text(container, SWT.BORDER);
		newCollText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		newCollText.setToolTipText("The title of the new collection");
		
		addCollBtn = new Button(container, SWT.NONE);
		addCollBtn.setImage(Images.getOrLoad("/icons/add.png"));
		addCollBtn.setToolTipText("Creates a new collection with the name on the left - you will be the owner of the collection");
		
		updateDocDirs();
		updateCollections();
		addListener();
		return container;
	}
		
	private void addListener() {
		store.addListener(new IStorageListener() {
			@Override public void handleCollectionsLoadEvent(CollectionsLoadEvent cle) {
				if (getShell() != null && !getShell().isDisposed())
					updateCollections();
			}
		});
		
		link.addSelectionListener(new SelectionAdapter(){
	        @Override
	        public void widgetSelected(SelectionEvent e) {
	        	Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
	            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
	                try {
	                    desktop.browse(new URI(e.text));
	                } catch (Exception ex) {
	                	//UnsupportedOperationException - if the current platform does not support the Desktop.Action.BROWSE action
	                	//IOException - if the user default browser is not found, or it fails to be launched, or the default handler application failed to be launched
	                	//SecurityException - if a security manager exists and it denies the AWTPermission("showWindowWithoutWarningBanner") permission, or the calling thread is not allowed to create a subprocess; and not invoked from within an applet or Java Web Started application
	                	//IllegalArgumentException - if the necessary permissions are not available and the URI can not be converted to a URL
	                	logger.error("Could not open ftp client!");
	                	
	                	DialogUtil.showMessageBox(getShell(), "Could not find FTP client", INFO_MSG, SWT.NONE);
	                }
	            }
	        	
//	        	try {
//	        		//  Open default external browser 
//	        		PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
//	        	} catch (PartInitException ex) {
//	        		// TODO Auto-generated catch block
//	        		ex.printStackTrace();
//	            } catch (MalformedURLException ex) {
//	            	// TODO Auto-generated catch block
//	            	ex.printStackTrace();
//	            }
	        }
	    });
		
		collCombo.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				List<TrpCollection> ccm = store.getCollectionsCanManage();
				int i = collCombo.getSelectionIndex();
				if (i >= 0 && i < ccm.size()) {
					selColl = ccm.get(i);
				}
				updateBtnVisibility();
			}
		});
		
		addCollBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				if (store.isLoggedIn() && !newCollText.getText().isEmpty()) {
					try {
						store.getConnection().createCollection(newCollText.getText());
						store.reloadCollections();
					} catch (Exception e1) {
						mw.onError("Could not create new collection", e1.getMessage(), e1);
					}
				}
				
				
				List<TrpCollection> ccm = store.getCollectionsCanManage();
				int i = collCombo.getSelectionIndex();
				if (i >= 0 && i < ccm.size()) {
					selColl = ccm.get(i);
				}
				updateBtnVisibility();
			}
		});
		
		reloadBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				updateDocDirs();
			}
		});
		
		helpBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				DialogUtil.showInfoMessageBox(getParentShell(), "Information", INFO_MSG);
			}
		});
	}
	
	@Override protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		
		updateBtnVisibility();
		return ctrl;
	}
	
	private void updateBtnVisibility() {
		if (getButton(IDialogConstants.OK_ID) != null)
			getButton(IDialogConstants.OK_ID).setEnabled(collCombo.getSelectionIndex() != -1);
	}
	
	private TrpCollection getSelectedCollection() {
		List<TrpCollection> ccm = store.getCollectionsCanManage();
		int i = collCombo.getSelectionIndex();
		if (i < 0 || i >= ccm.size())
			return null;
		else
			return ccm.get(collCombo.getSelectionIndex());
	}
	
	private void updateDocDirs(){
		try {
			docDirs = store.listDocDirsOnFtp();
		} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
				| NoConnectionException e) {
			mw.onError("Error", "Could not load directory list!", e);
		}
		docDirTv.setInput(docDirs);
	}
	
	private void updateCollections() {
		collCombo.removeAll();
		List<TrpCollection> ccm = store.getCollectionsCanManage();
		
		int selCollId = selColl == null ? 0 : selColl.getColId();
		int selItemInd = 0;
		for(int i = 0; i < ccm.size(); i++){
			final TrpCollection c = ccm.get(i);
			logger.debug("collection name: "+c.getColName()+ " i = "+i);
			collCombo.add(c.getColName());
			if (c.getColId() == selCollId)
				selItemInd = i;
		}
		collCombo.select(selItemInd);
		
		
		if (collCombo.getItemCount() > 0 && collCombo.getSelectionIndex() == -1)
			collCombo.select(0);
		
		updateBtnVisibility();
	}

	// overriding this methods allows you to set the
	// title of the custom dialog
	@Override protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Ingest from FTP Server");
	}

	@Override protected Point getInitialSize() {
		return new Point(700, 500);
	}

	// override method to use "Login" as label for the OK button
	@Override protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Upload", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override protected void okPressed() {
		saveInput();
		if(selDocDirs == null || selDocDirs.isEmpty()){
			DialogUtil.showErrorMessageBox(getParentShell(), "Info", "You have to select directories for ingesting.");
		} else {
			super.okPressed();
		}
	}

	private void saveInput() {
		this.selDocDirs = getSelectedDocDirs();
		this.selColl =  getSelectedCollection();
	}
	
	public TrpCollection getCollection() {
		return selColl;
	}
	
	public List<TrpDocDir> getDocDirs(){
		return selDocDirs;
	}
		
	public List<TrpDocDir> getSelectedDocDirs(){
		IStructuredSelection sel = (IStructuredSelection) docDirTv.getSelection();
		List<TrpDocDir> list = new LinkedList<>();
		Iterator<Object> it = sel.iterator();
		while(it.hasNext()){
			final TrpDocDir docDir = (TrpDocDir)it.next();
			logger.debug("Selected dir: " + docDir.getName());
			list.add(docDir);
		}
		return list;	
	}
	
	public static void main(String[] args) {
		ApplicationWindow aw = new ApplicationWindow(null) {
			@Override
			protected Control createContents(Composite parent) {
				// getShell().setLayout(new FillLayout());
				getShell().setSize(300, 200);

				Button btn = new Button(parent, SWT.PUSH);
				btn.setText("Open upload dialog");
				btn.addSelectionListener(new SelectionListener() {

					@Override public void widgetSelected(SelectionEvent e) {
						(new UploadFromFtpDialog(getShell(), null)).open();
					}

					@Override public void widgetDefaultSelected(SelectionEvent e) {
					}
				});

				SWTUtil.centerShell(getShell());

				return parent;
			}
		};
		aw.setBlockOnOpen(true);
		aw.open();

		Display.getCurrent().dispose();
	}
}
