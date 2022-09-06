package eu.transkribus.swt_gui.mainwidget;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.ServerErrorException;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dea.fimagestore.core.beans.ImageMetadata;
import org.dea.fimgstoreclient.beans.ImgType;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.BidiUtil;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.transkribus.client.connection.TrpServerConn;
import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.exceptions.ClientVersionNotSupportedException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.exceptions.NullValueException;
import eu.transkribus.core.io.util.ImgFileFilter;
import eu.transkribus.core.io.util.ImgPriority;
import eu.transkribus.core.model.beans.DocumentSelectionDescriptor;
import eu.transkribus.core.model.beans.GroundTruthSelectionDescriptor;
import eu.transkribus.core.model.beans.JAXBPageTranscript;
import eu.transkribus.core.model.beans.TrpAction;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocDir;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpEvent;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.TrpUpload;
import eu.transkribus.core.model.beans.auth.TrpRole;
import eu.transkribus.core.model.beans.auth.TrpUserLogin;
import eu.transkribus.core.model.beans.customtags.CommentTag;
import eu.transkribus.core.model.beans.customtags.CssSyntaxTag;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory.TagRegistryChangeEvent;
import eu.transkribus.core.model.beans.customtags.CustomTagUtil;
import eu.transkribus.core.model.beans.customtags.StructureTag;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.model.beans.enums.ScriptType;
import eu.transkribus.core.model.beans.enums.TranscriptionLevel;
import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TableCellType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpLocation;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPageType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpShapeTypeUtils;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTableCellType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTableRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.model.beans.rest.JobParameters;
import eu.transkribus.core.model.builder.CommonExportPars;
import eu.transkribus.core.model.builder.ExportCache;
import eu.transkribus.core.model.builder.ExportUtils;
import eu.transkribus.core.model.builder.alto.AltoExportPars;
import eu.transkribus.core.model.builder.docx.DocxBuilder;
import eu.transkribus.core.model.builder.docx.DocxExportPars;
import eu.transkribus.core.model.builder.iiif.IIIFUtils;
import eu.transkribus.core.model.builder.ms.TrpXlsxBuilder;
import eu.transkribus.core.model.builder.ms.TrpXlsxTableBuilder;
import eu.transkribus.core.model.builder.pdf.PdfExportPars;
import eu.transkribus.core.model.builder.rtf.TrpRtfBuilder;
import eu.transkribus.core.model.builder.tei.TeiExportPars;
import eu.transkribus.core.model.builder.txt.TrpTxtBuilder;
import eu.transkribus.core.program_updater.ProgramPackageFile;
import eu.transkribus.core.rest.JobConst;
import eu.transkribus.core.util.AuthUtils;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.IntRange;
import eu.transkribus.core.util.MonitorUtil;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.core.util.PointStrUtils;
import eu.transkribus.core.util.SysUtils;
import eu.transkribus.core.util.SysUtils.JavaInfo;
import eu.transkribus.core.util.ZipUtils;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.CreateThumbsService;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.DocumentManager;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.LoginDialog;
import eu.transkribus.swt.util.MessageDialogStyledWithToggle;
import eu.transkribus.swt.util.SWTLog;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.SplashWindow;
import eu.transkribus.swt.util.databinding.DataBinder;
import eu.transkribus.swt_gui.Msgs;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.TrpGuiPrefs;
import eu.transkribus.swt_gui.canvas.CanvasAutoZoomMode;
import eu.transkribus.swt_gui.canvas.CanvasContextMenuListener;
import eu.transkribus.swt_gui.canvas.CanvasMode;
import eu.transkribus.swt_gui.canvas.CanvasScene;
import eu.transkribus.swt_gui.canvas.CanvasSettings;
import eu.transkribus.swt_gui.canvas.CanvasSettingsPropertyChangeListener;
import eu.transkribus.swt_gui.canvas.CanvasShapeObserver;
import eu.transkribus.swt_gui.canvas.CanvasWidget;
import eu.transkribus.swt_gui.canvas.SWTCanvas;
import eu.transkribus.swt_gui.canvas.listener.CanvasSceneListener;
import eu.transkribus.swt_gui.canvas.listener.ICanvasSceneListener;
import eu.transkribus.swt_gui.canvas.shapes.CanvasShapeUtil;
import eu.transkribus.swt_gui.canvas.shapes.ICanvasShape;
import eu.transkribus.swt_gui.collection_manager.CollectionEditorDialog;
import eu.transkribus.swt_gui.collection_manager.CollectionManagerDialog;
import eu.transkribus.swt_gui.collection_manager.CollectionUsersDialog;
import eu.transkribus.swt_gui.credits.CreditManagerDialog;
import eu.transkribus.swt_gui.dialogs.ActivityDialog;
import eu.transkribus.swt_gui.dialogs.AutoSaveDialog;
import eu.transkribus.swt_gui.dialogs.BatchImageReplaceDialog;
import eu.transkribus.swt_gui.dialogs.BugDialog;
import eu.transkribus.swt_gui.dialogs.ChangeLogDialog;
import eu.transkribus.swt_gui.dialogs.ChooseCollectionDialog;
import eu.transkribus.swt_gui.dialogs.CommonExportDialog;
import eu.transkribus.swt_gui.dialogs.DebuggerDialog;
import eu.transkribus.swt_gui.dialogs.InstallSpecificVersionDialog;
import eu.transkribus.swt_gui.dialogs.JavaVersionDialog;
import eu.transkribus.swt_gui.dialogs.PAGEXmlViewer;
import eu.transkribus.swt_gui.dialogs.PageSelectorDialog;
import eu.transkribus.swt_gui.dialogs.ProgramUpdaterDialog;
import eu.transkribus.swt_gui.dialogs.ProxySettingsDialog;
import eu.transkribus.swt_gui.dialogs.RemoveTextRegionsConfDialog;
import eu.transkribus.swt_gui.dialogs.SettingsDialog;
import eu.transkribus.swt_gui.dialogs.TrpLoginDialog;
import eu.transkribus.swt_gui.dialogs.TrpMessageDialog;
import eu.transkribus.swt_gui.dialogs.VersionsDiffBrowserDialog;
import eu.transkribus.swt_gui.edit_decl_manager.EditDeclManagerDialog;
import eu.transkribus.swt_gui.edit_decl_manager.EditDeclViewerDialog;
import eu.transkribus.swt_gui.factory.TrpShapeElementFactory;
import eu.transkribus.swt_gui.htr.treeviewer.HtrGroundTruthContentProvider.HtrGtDataSet;
import eu.transkribus.swt_gui.htr.treeviewer.HtrGroundTruthContentProvider.TrpHtrGtDocMetadata;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.ModelGtDataSet;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.TrpModelGtDocMetadata;
import eu.transkribus.swt_gui.mainwidget.menubar.TrpMenuBarListener;
import eu.transkribus.swt_gui.mainwidget.settings.PreferencesDialog;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettings;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettingsPropertyChangeListener;
import eu.transkribus.swt_gui.mainwidget.storage.PageLoadResult;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.mainwidget.storage.StorageUtil;
import eu.transkribus.swt_gui.metadata.PageMetadataWidgetListener;
import eu.transkribus.swt_gui.metadata.TaggingWidgetUtils;
import eu.transkribus.swt_gui.pagination_tables.JobsDialog;
import eu.transkribus.swt_gui.pagination_tables.RecycleBinDialog;
import eu.transkribus.swt_gui.pagination_tables.StrayDocsDialog;
import eu.transkribus.swt_gui.pagination_tables.TranscriptsDialog;
import eu.transkribus.swt_gui.search.SearchDialog;
import eu.transkribus.swt_gui.search.SearchDialog.SearchType;
import eu.transkribus.swt_gui.search.fulltext.FullTextSearchComposite;
import eu.transkribus.swt_gui.search.text_and_tags.TagSearchComposite;
import eu.transkribus.swt_gui.structure_tree.StructureTreeListener;
import eu.transkribus.swt_gui.table_editor.TableUtils;
import eu.transkribus.swt_gui.tools.DiffCompareTool;
import eu.transkribus.swt_gui.tools.ToolsWidgetListener;
import eu.transkribus.swt_gui.transcription.ATranscriptionWidget;
import eu.transkribus.swt_gui.transcription.LineEditorListener;
import eu.transkribus.swt_gui.transcription.LineTranscriptionWidget;
import eu.transkribus.swt_gui.transcription.LineTranscriptionWidgetListener;
import eu.transkribus.swt_gui.transcription.WordTranscriptionWidget;
import eu.transkribus.swt_gui.transcription.WordTranscriptionWidgetListener;
import eu.transkribus.swt_gui.upload.UploadDialog;
import eu.transkribus.swt_gui.upload.UploadDialogUltimate;
import eu.transkribus.swt_gui.util.GuiUtil;
import eu.transkribus.swt_gui.util.OAuthGuiUtil;
import eu.transkribus.swt_gui.vkeyboards.ITrpVirtualKeyboardsTabWidgetListener;
import eu.transkribus.swt_gui.vkeyboards.TrpVirtualKeyboardsDialog;
import eu.transkribus.swt_gui.vkeyboards.TrpVirtualKeyboardsTabWidget;
import eu.transkribus.util.RecentDocsPreferences;

public class TrpMainWidget {
	private final static boolean USE_SPLASH = true;
	private static final boolean RELOAD_TRANSCRIPT_ASYNC = true;
	private static final boolean RELOAD_PAGE_ASYNC = true;
	private final static Logger logger = LoggerFactory.getLogger(TrpMainWidget.class);

	private static Shell mainShell;
	// Ui stuff:
	// Display display = Display.getDefault();
	static Display display;	
	SWTCanvas canvas;
	TrpMainWidgetView ui;
	LoginDialog loginDialog;
	// LineTranscriptionWidget transcriptionWidget;
	HashSet<String> userCache = new HashSet<String>();

//	static Preferences prefNode = Preferences.userRoot().node( "/trp/recent_docs" );
//	private RecentDocsPreferences userPrefs = new RecentDocsPreferences(5, prefNode);

	public ProgramInfo info;
	public final String VERSION;
	public final String NAME;
	private final JavaInfo javaInfo;
	
	private final double readingOrderCircleInitWidth = 90;

	// Listener:
	// CanvasGlobalEventsFilter globalEventsListener;
	TrpMainWidgetKeyListener keyListener;
	PagesPagingToolBarListener pagesPagingToolBarListener;
//	RegionsPagingToolBarListener lineTrRegionsPagingToolBarListener;
//	RegionsPagingToolBarListener wordTrRegionsPagingToolBarListener;
	// TranscriptsPagingToolBarListener transcriptsPagingToolBarListener;
	ICanvasSceneListener canvasSceneListener;
	LineTranscriptionWidgetListener lineTranscriptionWidgetListener;
	WordTranscriptionWidgetListener wordTranscriptionWidgetListener;
	TrpShapeElementFactory shapeFactory;
	LineEditorListener lineEditorListener;
	StructureTreeListener structTreeListener;
	TrpMainWidgetViewListener mainWidgetViewListener;
	CanvasContextMenuListener canvasContextMenuListener;
	TranscriptObserver transcriptObserver;
	CanvasShapeObserver canvasShapeObserver;
	PageMetadataWidgetListener pageMetadataWidgetListener;
//	TextStyleTypeWidgetListener textStyleWidgetListener;
//	TaggingWidgetOldListener taggingWidgetListener;
	ToolsWidgetListener laWidgetListener;
//	JobTableWidgetListener jobOverviewWidgetListener;
//	TranscriptsTableWidgetListener versionsWidgetListener;
	TrpMainWidgetStorageListener mainWidgetStorageListener;
//	CollectionManagerListener collectionsManagerListener;
	TrpMenuBarListener menuListener;

	
	// Dialogs
	DocumentManager docManDiag;
	SearchDialog searchDiag;
	TrpVirtualKeyboardsDialog vkDiag;
	TranscriptsDialog versionsDiag;
	SettingsDialog viewSetsDiag;
	ProxySettingsDialog proxyDiag;
	PreferencesDialog preferencesDiag;
	AutoSaveDialog autoSaveDiag;
	DebuggerDialog debugDiag;
	VersionsDiffBrowserDialog browserDiag;
	BugDialog bugDialog;
	ChangeLogDialog changelogDialog;
	JavaVersionDialog javaVersionDialog;
	RecycleBinDialog recycleBinDiag;
	
	JobsDialog jobsDiag;
	CollectionManagerDialog cm;
	CollectionUsersDialog collUsersDiag;
	StrayDocsDialog strayDocsDialog;
	InstallSpecificVersionDialog updateDialog;
	
	EditDeclManagerDialog edDiag;
	ActivityDialog ad;
	Shell sleakDiag;
	CreditManagerDialog creditManagerDialog;
	
	/** Storage keeps track of the currently loaded collection, document, page, transcription etc. */
	Storage storage;
	boolean isPageLocked = false;

	String lastExportFolder = System.getProperty("user.home");
//	String lastExportPdfFn = System.getProperty("user.home");
//	String lastExportTeiFn = System.getProperty("user.home");
//	String lastExportDocxFn = System.getProperty("user.home");
//	String lastExportXlsxFn = System.getProperty("user.home");

	String lastLocalDocFolder = null;
	boolean sessionExpired = false;
	String lastLoginServer = "";

	static TrpMainWidget mw;

	static int tmpCount = 0;
	
	static Thread asyncSaveThread;
	static DocJobUpdater docJobUpdater;
	
//	DocLoadController docLoadController;
	AutoSaveController autoSaveController;
	DocSyncController docSyncController;
	ShapeEditController shapeEditController;
	DocPageLoadController docPageController;
//	TaggingController taggingController;
	CollectionUtilsController collectionUtilsController;

	private Runnable updateThumbsWidgetRunnable = new Runnable() {
		@Override public void run() {
			ui.getThumbnailWidget().reload();
		}
	};

	private TrpMainWidget(Composite parent) {
		// GlobalResourceManager.init();

		info = new ProgramInfo();
		VERSION = info.getVersion();
		NAME = info.getName();
		javaInfo = SysUtils.getJavaInfo();
		logger.info("Installed Java version:\n" + javaInfo.toPrettyString());

		Display.setAppName(NAME);
		Display.setAppVersion(VERSION);

		// String time = info.getTimestampString();
		RecentDocsPreferences.init();

		// Display display = Display.getDefault();
		// canvas = new TrpSWTCanvas(SWTUtil.dummyShell, SWT.NONE, this);
		ui = new TrpMainWidgetView(parent, this);
		canvas = ui.getCanvas();
	
		// transcriptionWidget = ui.getLineTranscriptionWidget();
		shapeFactory = new TrpShapeElementFactory(this);

		storage = Storage.getInstance();

		addListener();
		addUiBindings();
		
//		docLoadController = new DocLoadController(this);
		autoSaveController = new AutoSaveController(this);
		docSyncController = new DocSyncController(this);
		shapeEditController = new ShapeEditController(this);
		docPageController = new DocPageLoadController(this);
		collectionUtilsController = new CollectionUtilsController(this);
//		taggingController = new TaggingController(this);
		updateToolBars();
		
		docJobUpdater = new DocJobUpdater(this);
	}

	public static TrpMainWidget getInstance() {
		return mw;
	}
	
	public static TrpMainWidget i() {
		return getInstance();
	}	
	
	public static Display getDisplay() {
		return display;
	}
	
	public String registerJobsToUpdate(Collection<String> jobIds) {
		if (jobIds == null)
			return "no jobs started";
		
		return registerJobsToUpdate(jobIds.toArray(new String[0]));
	}
	
	public String registerJobsToUpdate(String... jobIds) {
		if (jobIds == null)
			return "no jobs started";
		
		String jobIdsStr = "";
		for (String jobId : jobIds) {
			docJobUpdater.registerJobToUpdate(jobId);
			jobIdsStr += jobId+"\n";
		}
		
		return jobIdsStr;
	}

	public Storage getStorage() {
		return storage;
	}

	public ProgramInfo getInfo() {
		return info;
	}

	public static TrpSettings getTrpSettings() {
		return TrpConfig.getTrpSettings();
	}
	
//	public DocLoadController getDocLoadController() {
//		return docLoadController;
//	}
	
	public AutoSaveController getAutoSaveController() {
		return autoSaveController;
	}
	
	public DocSyncController getDocSyncController() {
		return docSyncController;
	}
	
	public ShapeEditController getShapeEditController() {
		return shapeEditController;
	}
	
	public CollectionUtilsController getCollectionUtilsController() {
		return collectionUtilsController;
	}
	
	public String getLastLocalDocFolder() {
		return lastLocalDocFolder;
	}
	
	public void setLastLocalDocFolder(String lastLocalDocFolder) {
		this.lastLocalDocFolder = lastLocalDocFolder;
	}
	
//	public TaggingController getTaggingController() {
//		return taggingController;
//	}


	/**
	 * This method gets called in the {@link #show()} method after the UI is
	 * inited and displayed
	 */
	public void postInit() {
		// remove unused old jar files: (maybe there from program update):
		try {
			ProgramUpdaterDialog.removeUnusedJarFiles();
		} catch (Exception e) {
			logger.error("Error removing old jar files: " + e.getMessage());
		}
		
		try {
			ProgramUpdaterDialog.removeUnusedJarLibFiles();
		} catch (Exception e) {
			logger.error("Error removing old lib-jar files: " + e.getMessage(), e);
		}
		

		//read and set proxy settings
		storage.updateProxySettings();



		// init predifined tags:
		String tagNamesProp = TrpConfig.getTrpSettings().getTagNames();
		if (tagNamesProp != null) {
			logger.debug("tags loaded to registry are: -----> " + tagNamesProp);
			CustomTagFactory.addDBTagsToRegistry(tagNamesProp, false);
			
		}
		else {
			logger.debug("tags loaded to registry are: -----> null");
		}
		
		try {
			ui.pageMetadataEditor.readCustomMetadataFromJsonObject();
		} catch (Exception e) {
			logger.error("Error reading custom metadata object: "+e.getMessage(), e);
		}

		// check for updates:
		if (getTrpSets().isCheckForUpdates()) {
			ProgramUpdaterDialog.showTrayNotificationOnAvailableUpdateAsync(ui.getShell(), VERSION, info.getTimestamp(), false);
		}

		final boolean ENABLE_AUTO_LOGIN = true;
		if (ENABLE_AUTO_LOGIN && getTrpSets().isAutoLogin()) {
			String lastAccount = TrpGuiPrefs.getLastLoginAccountType();

			try {
				if (OAuthGuiUtil.TRANSKRIBUS_ACCOUNT_TYPE.equals(lastAccount)) {
					Pair<String, String> lastLogin = TrpGuiPrefs.getLastStoredCredentials();
					if (lastLogin != null) {
						login(TrpServerConn.PROD_SERVER_URI, lastLogin.getLeft(), lastLogin.getRight(), true);
					}
				} else {
					// >=1.13.0: Google Login is no longer supported. Stop here and show LoginDialog with instructions
					mw.loginDialog("Please set a password and use the Transkribus login");
				}
			} catch (Exception e) {
				logger.error("Error during login in postInit: "+e.getMessage(), e);
			}
		}

		// disabled this here --> use this functionality via ctrl+t+t+t
//		loadTestDocSpecifiedInLocalFile();
		
		// TEST:
//		if (TESTTABLES) {
//			loadTestDocSpecifiedInLocalFile();
//		}		
//		jumpToPage(1);

//		SWTUtil.mask2(ui.getStructureTreeWidget()); // TESt
//		MyInfiniteProgressPanel p = MyInfiniteProgressPanel.getInfiniteProgressPanelFor(ui.getStructureTreeWidget());
//		p.start();

//		final boolean DISABLE_CHANGELOG = true;
//		openChangeLogDialog(getTrpSets().isShowChangeLog() && !DISABLE_CHANGELOG);
	}
	
	/**
	 * Determines which document was most recently edited by the user and loads it.<br><br>
	 * 
	 * Currently, the most recent doc access action (Access Document, Save, change edit status) is retrieved from the server for the details.
	 */
	public void loadMostRecentDoc() {
		try {
			TrpAction action = storage.getConnection().getMostRecentDocumentAction();
			if (action == null) {
				logger.debug("no most recent doc found!");
				return;
			}
			logger.debug("most recent doc action: "+action);
			
			// load the document and jump to page if pageNr is not null
			loadRemoteDoc(action.getDocId(), action.getColId(), action.getPageNr() != null ? action.getPageNr()-1 : 0);
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			logger.error("Could not retrieve recently loaded doc. Doing nothin...", e);
		}
	}

	/** Tries to read the local file "loadThisDocOnStartup.txt" and load the specified document.<br/>
     * To auto load a remote document: specify the first line as: "colId docId".<br/>
	 * To auto load a local document: specify the path of the document (without spaces!) in the first line.<br/>
	 * Comment out a line using a # sign at the start.
	 * @return true if file was found and the document was loaded successfully
	 */
	public boolean loadTestDocSpecifiedInLocalFile() {
		try {
			final String TEST_DOC_FN = "./loadThisDocOnStartup.txt";
			logger.debug("loading test doc from file: "+new File(TEST_DOC_FN).getAbsolutePath());
			List<String> lines = Files.readAllLines(Paths.get(TEST_DOC_FN));
			
			for (int i=0; i<lines.size(); ++i) {
				String docStr = lines.get(i);
				if (!docStr.startsWith("#")) {
					logger.debug("found docStr: "+docStr);
					String[] splits = docStr.split(" ");
					boolean isLocal = docStr.toLowerCase().startsWith("c:") || docStr.toLowerCase().startsWith("/");
					if (!isLocal && splits.length == 2) { // remote doc
						try {
							int colid = Integer.parseInt(splits[0]);
							int docid = Integer.parseInt(splits[1]);
							loadRemoteDoc(docid, colid);
							return true;
						} catch (NumberFormatException e) {
							// ignore parsing errors and do nothing...
						}
					} else { // local doc
						loadLocalDoc(docStr);
						return true;
					}
				}
			}
		} catch (IOException e) {
			// no file found -> ignore and to not load anything
		}
		
		return false;
	}

	public void syncTextOfDocFromWordsToLinesAndRegions() {
		try {
			if (!storage.isDocLoaded())
				return;

			final int colId = storage.getCurrentDocumentCollectionId();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						storage.syncTextOfDocFromWordsToLinesAndRegions(colId, monitor);
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Applying text from words", true);
		} catch (InterruptedException ie) {
		} catch (Throwable e) {
			onError("Could not apply text", "Could not apply text", e);
		} finally {
			// reloadCurrentDocument(true);
			reloadCurrentPage(true, null, null);
		}
	}

	public Future<List<TrpDocMetadata>> reloadDocList(int colId) {
		try {
			
			if (colId == 0)
				return null;
			
			try {
				storage.checkConnection(true);
			} catch (NoConnectionException e1) {
				loginDialog("No conection to server!");
			}
			
			if (!storage.isLoggedIn())
				return null;

			canvas.getScene().selectObject(null, true, false); // security measure due to mysterious bug leading to freeze of progress dialog
			
			if (storage.getCollection(colId)==null) { // collection not found in Storage -> reload doclist!
				logger.warn("reloadDocList: colId not found in storage -> have to reload collections from server - should not happen here");
				reloadCollections();
			}
			ui.getServerWidget().setSelectedCollection(storage.getCollection(colId));

			Future<List<TrpDocMetadata>> doclist;
			try {
				doclist = storage.reloadDocList(colId);
			} catch (SessionExpiredException | ServerErrorException | ClientErrorException | IllegalArgumentException e) {
				if(e instanceof SessionExpiredException){
					loginDialog("Session Expired!");
					//retry
					ui.getServerWidget().setSelectedCollection(storage.getCollection(colId));
					doclist = storage.reloadDocList(colId);
				}
				throw e;
			}
			
			return doclist;
		} catch (Throwable e) {
			onError("Cannot load document list", "Could not connect to " + getStorage().getConnection().getServerUri(), e);
			return null;
		}
	}

//	public int getSelectedCollectionId() {
//		return ui.getDocOverviewWidget().getSelectedCollectionId();
//	}

//	public void reloadJobList() {
//		try {
//			ui.getJobOverviewWidget().refreshPage(true);
//			storage.startOrResumeJobThread();
//
////			storage.reloadJobs(!ui.getJobOverviewWidget().getShowAllJobsBtn().getSelection()); // should
//			// trigger
//			// event
//			// that
//			// updates
//			// gui!
//		} catch (Exception ex) {
//			onError("Error", "Error during update of jobs", ex);
//		}
//	}

	public void cancelJob(final String jobId) {
		try {
			storage.cancelJob(jobId);
		} catch (Exception ex) {
			onError("Error", "Error while canceling a job", ex);
		}
	}

	public void clearDocList() {
		getUi().getServerWidget().clearDocList();
	}

//	public void reloadHtrModels() {
//		try {
//			storage.reloadHtrModelsStr();
//			ui.getToolsWidget().setHtrModelList(storage.getHtrModelsStr());
//		} catch (Exception e) {
//			onError("Error", "Error during update of HTR models", e);
//		}
//	}

//	public void clearHtrModelList() {
//		storage.clearHtrModels();
//		getUi().getToolsWidget().clearHtrModelList();
//	}

	public String updateDocumentInfo() {
		String loadedDocStr = "", currentCollectionStr = "";
		int docId = -1;
		ScriptType st = null;
		String language = null;
		TrpCollection c = null;

		if (storage.isDocLoaded()) {
			docId = storage.getDoc().getId();
			TrpDocMetadata md = storage.getDoc().getMd();
			st = md.getScriptType();
			language = md.getLanguage();

			if (storage.isLocalDoc()) {
				loadedDocStr = md.getLocalFolder().getAbsolutePath();
			} else {
				if (md.getTitle() != null && !md.getTitle().isEmpty()) {
					loadedDocStr = md.getTitle();
					if(docId > 0) {
						loadedDocStr += ", ID: " + docId;
					}
				}
				c = storage.getDoc().getCollection();
				if (c != null)
					currentCollectionStr = c.getColName() + ", ID: " + c.getColId();
			}
		}

//		ui.getServerWidget().setAdminAreaVisible(storage.isAdminLoggedIn());
		ui.getDocInfoWidget().getLoadedDocText().setText(loadedDocStr);
		ui.getDocInfoWidget().getCurrentCollectionText().setText(currentCollectionStr);
		if(storage.isGtDoc()) {
			//ui.getServerWidget().updateHighlightedGroundTruthTreeViewerRow();
		} else {
			ui.getServerWidget().updateHighlightedRow();
		}

//		ui.toolsWidget.updateParameter(st, language);

		return loadedDocStr;
	}

	// update title:
	public void updatePageInfo() {
		String title = ui.APP_NAME;

		String loadedDocStr = updateDocumentInfo();

		String loadedPageStr = "";
		String fn = "";
		String key = "";
		int pageNr = -1;
		int pageId=-1, tsid=-1;
		String imgUrl = "", transcriptUrl = "";
		
		int collId = storage.getCurrentDocumentCollectionId();
		int docId = -1;

		if (storage.getDoc() != null) {
			docId = storage.getDoc().getId();

			if (storage.getPage() != null) {
				fn = storage.getPage().getImgFileName() != null ? storage.getPage().getImgFileName() : "";
//				key = storage.getPage().getKey() != null ? storage.getPage().getKey() : "";

//				imgUrl = CoreUtils.urlToString(storage.getPage().getUrl());
				if (storage.getCurrentImage() != null && !storage.getPage().hasImgError()) {
					imgUrl = CoreUtils.urlToString(storage.getCurrentImage().url);
				}
				pageNr = storage.getPage().getPageNr();
				pageId = storage.getPage().getPageId();

				if (storage.getTranscriptMetadata() != null 
						&& storage.getTranscriptMetadata().getUrl() != null 
						&& !storage.getPage().hasImgError()) {
					transcriptUrl = CoreUtils.urlToString(storage.getTranscriptMetadata().getUrl());
					tsid = storage.getTranscriptMetadata().getTsId();
				}

				loadedPageStr = "Page " + pageNr + ", file: " + fn;
				if (storage.isPageLocked()) {
					loadedPageStr += " (LOCKED)";
				}
			}

			title += ", Loaded doc: " + loadedDocStr + ", " + loadedPageStr;
			if (storage.isTranscriptEdited())
				title += "*";

//			if (shellInfoText.contains("Img Meta Info")){
//				shellInfoText = shellInfoText.substring(0, shellInfoText.indexOf("Img Meta Info")).concat("Img Meta Info: ("+ storage.getImageMetadata().getXResolution() +")");
//			}

			if (storage.getDoc().isRemoteDoc()) {
				ImageMetadata imgMd = storage.getCurrentImageMetadata();
				if (imgMd != null)
					title += " [Image Meta Info: (Resolution:" + imgMd.getxResolution() + ", w*h: " + imgMd.getWidth() + " * " + imgMd.getHeight() + ") ]";
			}

			TrpTextRegionType currRegion = storage.getCurrentRegionObject();
			TrpTextLineType currLine = storage.getCurrentLineObject();
			TrpWordType currWord = storage.getCurrentWordObject();
			if (currWord != null) {
				java.awt.Rectangle boundingRect = PointStrUtils.buildPolygon(currWord.getCoords().getPoints()).getBounds();
				title += " [ current word: w*h: " + boundingRect.width + " * " + boundingRect.height + " ]";
			} else if (currLine != null) {
				java.awt.Rectangle boundingRect = PointStrUtils.buildPolygon(currLine.getCoords().getPoints()).getBounds();
				title += " [ current line: w*h: " + boundingRect.width + " * " + boundingRect.height + " ]";
			} else if (currRegion != null) {
				java.awt.Rectangle boundingRect = PointStrUtils.buildPolygon(currRegion.getCoords().getPoints()).getBounds();
				title += " [ current region: w*h: " + boundingRect.width + " * " + boundingRect.height + " ]";
			}

		}

		ui.getDocInfoWidget().getLoadedDocText().setText(loadedDocStr);
		ui.getDocInfoWidget().getLoadedPageText().setText(fn);
		
		ui.getDocInfoWidget().getLoadedImageUrl().setText(imgUrl);
		ui.getDocInfoWidget().getLoadedTranscriptUrl().setText(transcriptUrl);
		
		if (!storage.isDocLoaded() || storage.isLocalDoc()) {
			ui.getDocInfoWidget().getIdsText().setText("NA");
		} else {
			ui.getDocInfoWidget().getIdsText().setText(pageId+"/"+tsid);
		}
		if(!storage.isGtDoc()) {
			ui.getServerWidget().updateHighlightedRow();
		}
		ui.getShell().setText(title);
		// updateDocMetadata();
	}

	private void addListener() {
		Listener closeListener = new Listener() {
			@Override public void handleEvent(Event event) {
				logger.debug("close event!");
				if (!saveTranscriptDialogOrAutosave()) {
					event.doit = false;
					return;
				}
				docJobUpdater.stop = true;

				logger.debug("stopping CreateThumbsService");
				CreateThumbsService.stop(true);

				System.exit(0);
//				storage.finalize();
			}
		};
		ui.getShell().addListener(SWT.Close, closeListener);

		// add global filter for key listening:
		keyListener = new TrpMainWidgetKeyListener(this);
		getUi().getDisplay().addFilter(SWT.KeyDown, keyListener);

		// dispose listener
		getUi().addDisposeListener(new DisposeListener() {
			@Override public void widgetDisposed(DisposeEvent e) {
				storage.logout();
				// remove key listener:
				getUi().getDisplay().removeFilter(SWT.KeyDown, keyListener);
			}
		});
		mainWidgetViewListener = new TrpMainWidgetViewListener(this);
		menuListener = new TrpMenuBarListener(this);
		
		canvasContextMenuListener = new CanvasContextMenuListener(this);

		// pages paging toolbar listener:
		pagesPagingToolBarListener = new PagesPagingToolBarListener(ui.getPagesPagingToolBar(), this);
		// transcripts paging toolbar listener:
		// transcriptsPagingToolBarListener = new
		// TranscriptsPagingToolBarListener(ui.getTranscriptsPagingToolBar(),
		// this);
		// CanvasSceneListener acts on add / remove shape and selection change:
		canvasSceneListener = new CanvasSceneListener(this);
		// add toolbar listener for transcription widgets:
//		lineTrRegionsPagingToolBarListener = new RegionsPagingToolBarListener(ui.getLineTranscriptionWidget().getRegionsPagingToolBar(), this);
//		wordTrRegionsPagingToolBarListener = new RegionsPagingToolBarListener(ui.getWordTranscriptionWidget().getRegionsPagingToolBar(), this);
		// act on transcription changes:
		lineTranscriptionWidgetListener = new LineTranscriptionWidgetListener(this, ui.getLineTranscriptionWidget());
		wordTranscriptionWidgetListener = new WordTranscriptionWidgetListener(this, ui.getWordTranscriptionWidget());

		// line editor listener (modify and enter pressed)
		// lineEditorListener = new LineEditorListener(this);
		// struct tree listener:
		structTreeListener = new StructureTreeListener(ui.getStructureTreeWidget().getTreeViewer(), true);
		// transcription observer:
		transcriptObserver = new TranscriptObserver(this);
		// shape observer:
		canvasShapeObserver = new CanvasShapeObserver(this);

		// listen for changes in canvas settings:
		getCanvas().getSettings().addPropertyChangeListener(new CanvasSettingsPropertyChangeListener(this));
		// listen for changes in trp settings:
		getTrpSets().addPropertyChangeListener(new TrpSettingsPropertyChangeListener(this));

		// resize listener (for debug output):
		ui.addListener(SWT.Resize, new Listener() {
			@Override public void handleEvent(Event event) {
				Rectangle rect = ui.getClientArea();
				logger.debug("shell: " + rect + ", canvas: " + getCanvas().getClientArea() + " canvasWidget: " + getCanvasWidget().getClientArea());
			}
		});

		ui.getThumbnailWidget().addListener(SWT.Selection, new Listener() {
			@Override public void handleEvent(Event event) {
				logger.debug("loading page " + event.index);
				jumpToPage(event.index);
			}
		});
		
		pageMetadataWidgetListener = new PageMetadataWidgetListener(this);
		
//		if (ui.getTextStyleWidget()!=null) {
//			textStyleWidgetListener = new TextStyleTypeWidgetListener(ui.getTextStyleWidget());
//		}

//		taggingWidgetListener = new TaggingWidgetOldListener(this);

		laWidgetListener = new ToolsWidgetListener(this);
		
//		jobOverviewWidgetListener = new JobTableWidgetListener(this);
//		versionsWidgetListener = new TranscriptsTableWidgetListener(this);

		// storage observer:
		mainWidgetStorageListener = new TrpMainWidgetStorageListener(this);

//		ui.getServerWidget().getShowJobsBtn().addSelectionListener(new SelectionListener() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				openJobsDialog();
//			}
//			@Override public void widgetDefaultSelected(SelectionEvent e) {}
//		});
		
		CustomTagFactory.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				Display.getDefault().asyncExec(() -> {
					if (arg instanceof TagRegistryChangeEvent) {
						logger.debug("registry has changed ");
						TagRegistryChangeEvent trce = (TagRegistryChangeEvent) arg;
						logger.debug("type of tag change " + trce.type);
						if (trce.type.equals(TagRegistryChangeEvent.CHANGED_TAG_COLOR) && getUi()!=null && getUi().getSelectedTranscriptionWidget()!=null) {
							TrpMainWidget.getInstance().getUi().getSelectedTranscriptionWidget().redrawText(true, false, false);
						}
						
						
												
						/*
						 * if tag registry has changed and user is logged in -> store into DB for the current user
						 * this tag list is used for the web interface!
						 */
						
//						if (storage.isLoggedIn()){
//							Storage.getInstance().updateCustomTagSpecsForUserInDB();
//						}
					}
				});
			}
		});
	}
	
	/**
	 * Add a comment tag for the current selection in the transcription widget.
	 * If commentText is empty or null, the user is prompted to input a comment.
	 */
	public void addCommentForSelection(String commentText) {
		try {
			// show dialog if commentText parameter is empty!
			if (StringUtils.isEmpty(commentText)) {
				InputDialog id = new InputDialog(getShell(), "Comment", "Please enter a comment: ", "", null);
				id.setBlockOnOpen(true);
				if (id.open() != Window.OK) {
					return;
				}
				commentText = id.getValue();
			}
			
			if (StringUtils.isEmpty(commentText)) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "Cannot add an empty comment!");
				return;
			}
				    			
			Map<String, Object> atts = new HashMap<>();
			atts.put(CommentTag.COMMENT_PROPERTY_NAME, commentText);
			addTagForSelection(CommentTag.TAG_NAME, atts, null);
			getUi().getCommentsWidget().reloadComments();
		} catch (Exception e) {
			onError("Error adding comment", e.getMessage(), e);
		}
		
		
	}
	
	private void addUiBindings() {
		DataBinder db = DataBinder.get();
		TrpSettings trpSets = getTrpSets();
		
		CanvasWidget cw = ui.canvasWidget;
		
		logger.debug("cw = "+cw);
		
		CanvasSettings canvasSet = cw.getCanvas().getSettings();
		
		// NOTE: docking props are synced via TrpSettingsPropertyChangeListener
//		db.bindBeanPropertyToObservableValue(TrpSettings.MENU_VIEW_DOCKING_STATE_PROPERTY, trpSets, 
//												Observables.observeMapEntry(ui.portalWidget.getDockingMap(), Position.LEFT));
//		db.bindBeanPropertyToObservableValue(TrpSettings.TRANSCRIPTION_VIEW_DOCKING_STATE_PROPERTY, trpSets, 
//												Observables.observeMapEntry(ui.portalWidget.getDockingMap(), Position.BOTTOM));

		db.bindBeanToWidgetSelection(TrpSettings.SHOW_TEXT_REGIONS_PROPERTY, trpSets, cw.getToolbar().showRegionsButton);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_LINES_PROPERTY, trpSets, cw.getToolbar().showLinesButton);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_BASELINES_PROPERTY, trpSets, cw.getToolbar().showBaselinesButton);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_WORDS_PROPERTY, trpSets, cw.getToolbar().showWordsButton);
//		db.bindBeanToWidgetSelection(TrpSettings.SHOW_PRINTSPACE_PROPERTY, trpSets, cw.getToolbar().showPrintspaceButton);
		db.bindBeanToWidgetSelection(TrpSettings.RENDER_BLACKENINGS_PROPERTY, trpSets, cw.getToolbar().renderBlackeningsButton);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_STRUCT_TYPE_COLOR_PROPERTY, trpSets, cw.getToolbar().showStructureColors);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_STRUCT_TYPE_TEXT_PROPERTY, trpSets, cw.getToolbar().showStructureText);
		
//		DataBinder.get().bindBoolBeanValueToToolItemSelection("editingEnabled", canvasSet, cw.getEditingEnabledToolItem());
		
		if (TrpSettings.ENABLE_LINE_EDITOR)
			db.bindBoolBeanValueToToolItemSelection(TrpSettings.SHOW_LINE_EDITOR_PROPERTY, trpSets, ui.showLineEditorToggle);
		
		db.bindBeanToWidgetSelection(TrpSettings.RECT_MODE_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getRectangleModeItem());
		db.bindBeanToWidgetSelection(CanvasSettings.USE_SCROLL_BARS_PROPERTY, canvasSet, ui.canvasWidget.getToolbar().getUseScrollBarsItem());
		db.bindBeanToWidgetSelection(TrpSettings.AUTO_CREATE_PARENT_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getAutoCreateParentItem());
		
		db.bindBeanToWidgetSelection(TrpSettings.ADD_LINES_TO_OVERLAPPING_REGIONS_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getAddLineToOverlappingRegionItem());
		db.bindBeanToWidgetSelection(TrpSettings.ADD_BASELINES_TO_OVERLAPPING_LINES_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getAddBaselineToOverlappingLineItem());
		db.bindBeanToWidgetSelection(TrpSettings.ADD_WORDS_TO_OVERLAPPING_LINES_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getAddWordsToOverlappingLineItem());
		
		db.bindBeanToWidgetSelection(CanvasSettings.LOCK_ZOOM_ON_FOCUS_PROPERTY, TrpConfig.getCanvasSettings(), ui.canvasWidget.getToolbar().getLockZoomOnFocusItem());
		
		db.bindBeanToWidgetSelection(TrpSettings.DELETE_LINE_IF_BASELINE_DELETED_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getDeleteLineIfBaselineDeletedItem());
		
		db.bindBeanToWidgetSelection(TrpSettings.SELECT_NEWLY_CREATED_SHAPE_PROPERTY, trpSets, ui.canvasWidget.getToolbar().getSelectNewlyCreatedShapeItem());
		
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_READING_ORDER_REGIONS_PROPERTY, trpSets, cw.getToolbar().showReadingOrderRegionsButton);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_READING_ORDER_LINES_PROPERTY, trpSets, cw.getToolbar().showReadingOrderLinesButton);
		db.bindBeanToWidgetSelection(TrpSettings.SHOW_READING_ORDER_WORDS_PROPERTY, trpSets, cw.getToolbar().showReadingOrderWordsButton);
	}
	
//	public TaggingWidgetOldListener getTaggingWidgetListener() {
//		return taggingWidgetListener;
//	}

	// boolean isThisDocOpen(TrpJobStatus job) {
	// return storage.isDocLoaded() && storage.getDoc().getId()==job.getDocId();
	// }

	// public void detachListener() {
	// globalEventsListener.detach();
	// }
	//
	// public void attachListener() {
	// globalEventsListener.attach();
	// }

	public void loginDialog(String message) {
		try {
			if (!getTrpSets().isServerSideActivated()) {
				throw new NotSupportedException("Connecting to the server not supported yet!");
			}

			// detachListener();

			if (loginDialog != null && !loginDialog.isDisposed()) {
				loginDialog.close();
			}

			List<String> storedUsers = TrpGuiPrefs.getUsers();
			
			final String[] serverProposals;
			final int defaultUriIndex;
			if(getTrpSets().isServerSelectionEnabled()) {
				serverProposals = TrpServerConn.SERVER_URIS;
				defaultUriIndex = TrpServerConn.DEFAULT_URI_INDEX;
			} else {
				serverProposals = new String[] { TrpServerConn.SERVER_URIS[TrpServerConn.DEFAULT_URI_INDEX] };
				defaultUriIndex = 0;
			}
			
			loginDialog = new TrpLoginDialog(getShell(), this, message, storedUsers.toArray(new String[0]), serverProposals, defaultUriIndex);
			loginDialog.open();

			// attachListener();
		} catch (Throwable e) {
			onError("Error during login", "Unable to login to server", e);
			ui.updateLoginInfo(false, "", "");
		}
	}

	/**
	 * Gets called when the login dialog is closed by a successful login
	 * attempt.<br>
	 * It's a verbose method name, I know ;-)
	 */
	public void onSuccessfullLoginAndDialogIsClosed() {
		logger.debug("onSuccessfullLoginAndDialogIsClosed");

		/*
		 * during login we want to load the last loaded doc from the previous logout
		 */
		//getTrpSets().getLastDocId();
//		if (getTrpSets().getLastDocId() != -1 && getTrpSets().getLastColId() != -1){
//			int colId = getTrpSets().getLastColId();
//			int docId = getTrpSets().getLastDocId();
//			loadRemoteDoc(docId, colId, 0);
//			getUi().getDocOverviewWidget().setSelectedCollection(colId, true);
//			getUi().getDocOverviewWidget().getDocTableWidget().loadPage("docId", docId, true);
//		}

		//section to load the last used document for each user - either local or remote doc
		if (false) {
			if (!RecentDocsPreferences.getItems().isEmpty()) {
				if (RecentDocsPreferences.isShowOnStartup()) {
					String docToLoad = RecentDocsPreferences.getItems().get(0);
					loadRecentDoc(docToLoad);
				}
			} else {
				//if no recent docs are available -> load the example doc
				if (false) {
					loadRemoteDoc(5014, 4);
//					getUi().getServerWidget().setSelectedCollection(4, true);
//					getUi().getServerWidget().getDocTableWidget().loadPage("docId", 5014, true);
				}
			}
		}

//		reloadDocList(ui.getDocOverviewWidget().getSelectedCollection());
//		reloadHtrModels();
		// reloadJobListForDocument();
	}
	
	/*
	public void loginAsync(String server, String user, String pw, boolean rememberCredentials) throws ClientVersionNotSupportedException, LoginException, Exception {
		if (!getTrpSets().isServerSideActivated()) {
			throw new NotSupportedException("Connecting to the server not supported yet!");
		}
		
		storage.loginAsync(server, user, pw, new AsyncCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				Display.getDefault().asyncExec(() -> {
					if (rememberCredentials) { // store credentials on successful login
						logger.debug("storing credentials for user: " + user);
						TrpGuiPrefs.storeCredentials(user, pw);
					}
					TrpGuiPrefs.storeLastLogin(user);
					TrpGuiPrefs.storeLastAccountType(OAuthGuiUtil.TRANSKRIBUS_ACCOUNT_TYPE);

					userCache.add(user);

					if (sessionExpired && !lastLoginServer.equals(server)) {
						closeCurrentDocument(true);
					}

					sessionExpired = false;
					lastLoginServer = server;
					
					// when user is logged in we can store the tag definitions into the DB
					// later on they are stored each time they change
					try {
						storage.updateCustomTagSpecsForUserInDB();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}

			@Override
			public void onError(Throwable error) {
				// TODO Auto-generated method stub
				
			}
		});
		
//		storage.login(server, user, pw);
//		return true;
	}*/

	public boolean login(String server, String user, String pw, boolean rememberCredentials) throws ClientVersionNotSupportedException, LoginException, Exception {
//		try {
			if (!getTrpSets().isServerSideActivated()) {
				throw new NotSupportedException("Connecting to the server not supported yet!");
			}
			
			storage.login(server, user, pw);
			if (rememberCredentials) { // store credentials on successful login
				logger.debug("storing credentials for user: " + user);
				TrpGuiPrefs.storeCredentials(user, pw);
			}
			TrpGuiPrefs.storeLastLogin(user);
			TrpGuiPrefs.storeLastAccountType(OAuthGuiUtil.TRANSKRIBUS_ACCOUNT_TYPE);

			userCache.add(user);

			if (sessionExpired && !lastLoginServer.equals(server)) {
				closeCurrentDocument(true);
			}

			sessionExpired = false;
			lastLoginServer = server;
			
			/*
			 * when user is logged in we can store the tag definitions into the DB
			 * later on they are stored each time they change
			 */
//			try {
//				storage.updateCustomTagSpecsForUserInDB();
//			} catch (Exception e) {
//				logger.error(e.getMessage(), e);
//			}
			
			return true;
//		}
//		catch (ClientVersionNotSupportedException e) {
//			DialogUtil.showErrorMessageBox(getShell(), "Version not supported anymore!", e.getMessage());
//			logger.error(e.getMessage(), e);
//			return false;
//		}
//		catch (LoginException e) {
//			logout(true, false);
//			logger.error(e.getMessage(), e);
//			return false;
//		}
//		catch (Exception e) {
//			logout(true, false);
//			logger.error(e.getMessage(), e);
//			return false;
//		}

		// finally {
		// ui.updateLoginInfo(storage.isLoggedIn(), getCurrentUserName(),
		// storage.getCurrentServer());
		// }
	}

	public void loadRecentDoc(String docToLoad) {
		if (docToLoad == null)
			return;
		
		try {
			storage.checkConnection(true);
		} catch (NoConnectionException e1) {
			// TODO Auto-generated catch block
			loginDialog("No connection to server!");
		}
				
		String[] tmp = docToLoad.split(";;;");
		if (tmp.length == 1) {
			if (new File(tmp[0]).exists()) {
				loadLocalDoc(tmp[0]);
			} else {
				DialogUtil.createAndShowBalloonToolTip(getShell(), SWT.ICON_ERROR, "Loading Error", "Local folder does not exist anymore", 2, true);
			}
		} else if (tmp.length == 3 || tmp.length == 4) {
			boolean loadPage = (tmp.length == 4 ? true : false);
//			for (int i = 0; i < tmp.length; i++){
//				logger.debug(" split : " + tmp[i]);
//			}
			int docid = Integer.valueOf(tmp[1]);
			int colid = Integer.valueOf(tmp[2]);

			List<TrpDocMetadata> docList;
			try {
				docList = storage.getConnection().findDocuments(colid, docid, "", "", "", "", true, false, 0, 0, null, null);
				if (docList != null && docList.size() > 0) {
					if (loadPage){
						int pageId = Integer.valueOf(tmp[3]);
						loadRemoteDoc(docid, colid, (pageId-1));
					}
					else{
						loadRemoteDoc(docid, colid);
					}
				} else {
					//DialogUtil.createAndShowBalloonToolTip(getShell(), SWT.ICON_ERROR, "Loading Error", "Last used document is not on this server", 2, true);
				}
			} catch (SessionExpiredException | ServerErrorException | ClientErrorException | IllegalArgumentException e) {
				//logger.debug(" exception message " + e.toString() +  " -> " + e.getMessage());
				if(e instanceof SessionExpiredException){
					logger.debug("Exception message " + e.toString() +  " -> " + e.getMessage());
					loginDialog("Session Expired!");
					//retry
					loadRecentDoc(docToLoad);
				}
			}

		}

	}

	public void logout(boolean force, boolean closeOpenDoc) {
		if (!force && !saveTranscriptDialogOrAutosave())
			return;

		logger.debug("Logging out " + storage.getUser());
		storage.logout();

		if (closeOpenDoc && !storage.isLocalDoc()) {
			closeCurrentDocument(true);
		}

		ui.serverWidget.setSelectedCollection(null);
		clearDocList();
		
//		clearHtrModelList();
//		ui.getJobOverviewWidget().refreshPage(true);
		clearThumbs();

		// reloadJobListForDocument();
		// ui.updateLoginInfo(false, getCurrentUserName(), "");
	}

	public String getCurrentUserName() {
		if (storage.getUser() != null)
			return storage.getUser().getUserName();
		else
			return "";
	}

	public void saveDocMetadata() {
		if (!storage.isDocLoaded())
			return;

		final int colId = storage.getCurrentDocumentCollectionId();
		try {
			storage.saveDocMd(colId);
			logger.debug("saved doc-md to collection "+colId);

			// DialogUtil.createAndShowBalloonToolTip(getShell(),
			// SWT.ICON_ERROR, "Success saving doc-metadata", "", 2, true);
//			DialogUtil.showInfoMessageBox(shell, "Success", message);
			
//			DialogUtil.createAndShowBalloonToolTip(getShell(), SWT.ICON_INFORMATION, "Saved document metadata!", "Success", 2, true);
		} catch (Exception e) {
			Display.getDefault().asyncExec(() -> {
				onError("Error saving doc-metadata", e.getMessage(), e, true, true);	
			});
		}
	}
	
	public boolean saveDocMetadata(List<TrpDocMetadata> docs) {
		if (!storage.isLoggedIn()) {
			return false;
		}
		
		if (CoreUtils.isEmpty(docs)) {
			DialogUtil.showErrorMessageBox(getShell(), "No document selected", "Please select a document you want to restore!");
			return false;
		}
		
		int N = docs.size();
		
		List<Integer> listOfDocIdsOfRunningJobs = new ArrayList<>();
		
		try {
			/*
			 * only docs for which no job is currently running will be considered to be deleted or retrieved
			 */
			for (TrpJobStatus job : mw.getStorage().getUnfinishedJobs(true)){
				listOfDocIdsOfRunningJobs.add(job.getDocId());
			}
		} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (N > 1) {
			String msg = "Do you really want to restore " + N + " selected documents?";
			if (DialogUtil.showYesNoDialog(getShell(), "Restore Documents", msg)!=SWT.YES) {
				return false;
			}
		}
		else{
			String msg = "Do you really want to restore document '"+docs.get(0).getTitle()+"'?";
			if (DialogUtil.showYesNoDialog(getShell(), "Restore Document", msg)!=SWT.YES) {
				return false;
			}
		}
		
		final List<String> error = new ArrayList<>();
		try {
		ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
			@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				
				try {
					monitor.beginTask("Restoring documents", docs.size());
					TrpUserLogin user = storage.getUser();
					int i = 0;
					for (TrpDocMetadata d : docs) {
						if (monitor.isCanceled())
							throw new InterruptedException();

						logger.debug("restoring document: "+d);
						
						if (listOfDocIdsOfRunningJobs.contains(d.getDocId())){
							String errorMsg = "Already job running for document id: "+d.getDocId();
							logger.warn(errorMsg);
							error.add(errorMsg);
						}
						else if (!user.isAdmin() && user.getUserId()!=d.getUploaderId()) {
//							DialogUtil.showErrorMessageBox(getShell(), "Unauthorized", "You are not the uploader of this document. " + md.getTitle());
//							return false;
							String errorMsg = "Unauthorized - you are not the uploader of this document: "+d.getTitle()+", id: "+d.getDocId();
							logger.warn(errorMsg);
							error.add(errorMsg);
						} else {
							try {
								d.setDeleted(false);
								storage.updateDocMd(Storage.getInstance().getCollId(), d);
								logger.info("restored document: "+d);
							} catch (SessionExpiredException | TrpClientErrorException | TrpServerErrorException e) {
								logger.warn("Could not restore document: "+d, e);
								error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessageToUser());
							} catch (Throwable e) {
								logger.warn("Could not restore document: "+d, e);
								error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessage());
							}
						}
	
						monitor.worked(++i);
					}

				}
				catch (InterruptedException ie) {
					throw ie;
				} catch (Throwable e) {
					throw new InvocationTargetException(e, e.getMessage());
				}
			}
		}, "Restoring documents", true);
		}
		catch (InterruptedException e) {}
		catch (Throwable e) {
			onError("Unexpected error", e.getMessage(), e);
		}
		
		if (!error.isEmpty() && error.size() == docs.size()) {
			String msg = "Could not restore the following documents:\n";
			for (String u : error) {
				msg += u + "\n";
			}
			msg += "You have to cancel the job if you want to restore the document!";
			if (SWTUtil.isOpen(recycleBinDiag)){
				recycleBinDiag.close();
			}
			mw.onError("Error restoring documents", msg, null);
			try {
				//storage.reloadCollections();
				storage.reloadDocList(storage.getCollId());
				//recycleBinDiag.getDocTableWidget().refreshList(storage.getCollId());
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
					| NoConnectionException e) {
				// TODO Auto-generated catch block
				logger.error("reloading collections not possible after document(s) restoring");
				e.printStackTrace();
			}
			//ui.serverWidget.getDocTableWidget().reloadDocs(false, true);
			return false;
		} else {
			String msg = "";
			if (!error.isEmpty()) {
				msg = error.size() + " doc(s) could not be restored:\n";
				for (String u : error) {
					msg += u + "\n";
				}
				msg += "You have to cancel the job if you want to restore the document!";
			}
			DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully restored "+(docs.size()-error.size())+" documents\n"
					+ msg);
			//clean up GUI
			try {
				//reload necessary in fact the symbolic image has changed - this is done during the delete job
				//storage.reloadCollections();
				storage.reloadDocList(storage.getCollId());
				//recycleBinDiag.getDocTableWidget().refreshList(storage.getCollId());
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
					| NoConnectionException e) {
				// TODO Auto-generated catch block
				logger.error("reloading collections not possible after restoring documents");
				e.printStackTrace();
			}
			//reload necessary to actualize doc list
			//ui.serverWidget.getDocTableWidget().reloadDocs(false, true);
			
			return true;
		}
	}

	/** Reassigns unique id's to the current page file */
	public void updateIDs() {
		try {
			if (!storage.hasTranscript())
				return;

			storage.getTranscript().getPage().updateIDsAccordingToCurrentSorting();
			updatePageRelatedMetadata();

			// reload tree with new IDs:
			refreshStructureView();
		} catch (Throwable th) {
			onError("Could not update IDs", "Unable to update IDs - see error log for more details", th);
		}
	}

	public void saveTranscriptionToNewFile() {
		if (!storage.hasTranscript())
			return;

		logger.debug("saving transcription to file...");
		String fn = DialogUtil.showSaveDialog(getShell(), "Choose a file", null, new String[] { "*.xml" });
		if (fn == null)
			return;
		File f = new File(fn);
		try {
			PageXmlUtils.marshalToFile(storage.getTranscript().getPageData(), f);
		} catch (Exception e1) {
			onError("Saving Error", "Error while saving transcription to " + f.getAbsolutePath(), e1);
		}
		logger.debug("finished writing xml output to " + f.getAbsolutePath());
	}
	
//	public boolean checkLocalSaves(TrpPage page) {
//		List<File> files = autoSaveController.getAutoSavesFiles(page);
//		
//	    if (CoreUtils.isEmpty(files)) {
//	    	logger.debug("No local autosave files found.");
//	    	return false;
//	    }
//	    
//	    logger.debug("Local autosave files found! Comparing timestamps...");	    
//	    File localTranscript = files.get(0);
//	    	    
//	    try {
//	    	/*
//	    	 * getLastChange() Doesn't return correct metadata xml element ???
//	    	 * (getCreator/creationDate work fine)
//	    	 * --> use actual file lastmodified time instead for now	    	
//	    	 */
////			localTimestamp = pcLocal.getMetadata().getLastChange();	    	
//	    	
//			long lLocalTimestamp = localTranscript.lastModified();
//		    GregorianCalendar gc = new GregorianCalendar();
//		    gc.setTimeInMillis(lLocalTimestamp);
//		    XMLGregorianCalendar localTimestamp = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);	
//	    
//			logger.debug("local timestamp: "
//	    		+localTimestamp.getMonth()
//			    + "/" + localTimestamp.getDay()
//			    +"h" + localTimestamp.getHour() 
//			    + "m" + localTimestamp.getMinute() 
//			    + "s" + localTimestamp.getSecond());
//	    
//		    long lRemoteTimestamp = page.getCurrentTranscript().getTimestamp();
//		    gc.setTimeInMillis(lRemoteTimestamp);
//		    XMLGregorianCalendar remoteTimeStamp = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);	  
////		    XMLGregorianCalendar remoteTimeStamp = storage.getTranscript().getPage().getPcGtsType().getMetadata().getLastChange();
//
//		    logger.debug("remote timestamp: "
//	    		+remoteTimeStamp.getMonth()
//			    + "/" + remoteTimeStamp.getDay()
//			    +"h" + remoteTimeStamp.getHour() 
//			    + "m" + remoteTimeStamp.getMinute() 
//			    + "s" + remoteTimeStamp.getSecond());
//	    
//		    //Return false if local autosave transcript is older
//		    if(localTimestamp.compare(remoteTimeStamp)==DatatypeConstants.LESSER 
//		    		||localTimestamp.compare(remoteTimeStamp)==DatatypeConstants.EQUAL ){
//		    	logger.debug("No newer autosave transcript found.");
//		    	return false;
//		    }
//	
//		    logger.debug("Newer autosave transcript found.");
//		    Display.getDefault().syncExec(new Runnable() {
//		        public void run() {
//		        	String diagText = "A newer transcript of this page exists on your computer. Do you want to load it?";
//		        	if(DialogUtil.showYesNoCancelDialog(getShell(),"Newer version found in autosaves",diagText) == SWT.YES){
//		        		logger.debug("loading local transcript into view");	        		
//
//		    			try {
//			        		PcGtsType pcLocal = PageXmlUtils.unmarshal(localTranscript);
//			        		JAXBPageTranscript jxtr = new JAXBPageTranscript();
//			        		jxtr.setPageData(pcLocal);
//			        		storage.getTranscript().setPageData(pcLocal);
//			        		storage.getTranscript().setMd(jxtr.getMd());
//			        		storage.setLatestTranscriptAsCurrent();      		    				
//							loadJAXBTranscriptIntoView(storage.getTranscript());
//						} catch (Exception e) {
//							TrpMainWidget.getInstance().onError("Error when loading transcript into view.", e.getMessage(), e.getCause());
//							e.printStackTrace();
//						}
//		    			ui.taggingWidget.updateAvailableTags();
//		    			updateTranscriptionWidgetsData();
//		    			canvas.getScene().updateSegmentationViewSettings();
//		    			canvas.update();
//		    			
////	    				reloadCurrentPage(true);	        		
//		        	}
//		        }
//		    });
//	    
//	    }catch (Exception e){
//	    	e.printStackTrace();
//	    }
//	    
//		return true;
//	}
	
	
	public boolean saveTranscriptionSilent() {
		try {
			if (!storage.isPageLoaded()) {
//				DialogUtil.showErrorMessageBox(getShell(), "Saving page", "No page loaded!");
				return false;
			}

			final String commitMessage = "";
			logger.debug("commitMessage = " + commitMessage);

			final int colId = storage.getCurrentDocumentCollectionId();

			Runnable saveTask = new Runnable() {
				
				@Override public void run() {
					try {
						storage.saveTranscript(colId, commitMessage);					

						storage.setLatestTranscriptAsCurrent();
					} catch (Exception e) {
						//throw new InvocationTargetException(e, e.getMessage());
					}
					logger.debug("Async save completed.");
				}
			};
			if(asyncSaveThread != null){
				if(asyncSaveThread.isAlive()){
					asyncSaveThread.interrupt();
				}
			}
			asyncSaveThread = new Thread(saveTask, "Async Save Thread");
			asyncSaveThread.start();
			updateToolBars();
			return true;
			
		} catch (Throwable e) {
			onError("Saving Error", "Error while saving transcription", e);
			return false;
		} finally {
			updatePageInfo();
		}
	}

	public boolean saveTranscription(boolean isCommit) {
		// final List<TrpTranscriptMetadata> newTransList = new ArrayList<>();

		try {
			if (!storage.isPageLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Saving page", "No page loaded!");
				return false;
			}

			String commitMessageTmp = null;
			if (isCommit) {
				InputDialog id = new InputDialog(getShell(), "Commit message", "Please enter a commit message: ", "", null);
				id.setBlockOnOpen(true);
				if (id.open() != Window.OK) {
					return false;
				}
				commitMessageTmp = id.getValue();
			}
			final String commitMessage = commitMessageTmp;
			logger.debug("commitMessage = " + commitMessage);

			final int colId = storage.getCurrentDocumentCollectionId();
			
			updateRecentDocItems(Storage.getInstance().getDoc().getMd());			

			RecentDocsPreferences.push(Storage.getInstance().getDoc().getMd().getTitle() + ";;;" + storage.getDocId() + ";;;" + colId + ";;;" + (storage.getPageIndex()+1));
			ui.getServerWidget().updateRecentDocs();
			
			
			// canvas.getScene().selectObject(null, true, false); // security
			// measure due to mysterious bug leading to freeze of progress
			// dialog
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("saving transcription, commitMessage = " + commitMessage);
						monitor.beginTask("Saving transcription", IProgressMonitor.UNKNOWN);
						
						//here we store the current page related md to the transcript
						ui.pageMetadataEditor.handleMdSave();
						storage.saveTranscript(colId, commitMessage);
						// set new transcription list and reload locally:
						logger.debug("Saved file - reloading transcript");

						storage.setLatestTranscriptAsCurrent();
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Saving", false);

//			reloadCurrentTranscript(true, true);
			updateToolBars();
//			updateSelectedTranscription();
			return true;
		} catch (Throwable e) {
			onError("Saving Error", "Error while saving transcription", e);
			return false;
		} finally {
			updatePageInfo();
		}
	}

	private void updateRecentDocItems(TrpDocMetadata md) {
		ListIterator<String> it = RecentDocsPreferences.getItems().listIterator();
		
		//delete all entries starting with title;;;docID;;;colID
		while (it.hasNext()) {
			if (it.next().startsWith(md.getTitle() + ";;;" + md.getDocId() + ";;;" + Storage.getInstance().getCollId())){
				it.remove();
			}
		}
		
	}

	public void updateSegmentationEditStatus() {	
		
		boolean isEditOn = getCanvas().getSettings().isEditingEnabled();

		// ui.getUpdateIDsItem().setEnabled(isEditOn);
		if (isEditOn) {
			canvas.getScene().updateSegmentationViewSettings();
		}

		// updateAddShapeActionButton();
		
		//update visibility of edit buttons
		ui.getCanvasWidget().getToolbar().updateButtonVisibility();
		ui.redraw();
	}

	// /**
	// * Updates enable state of the add shape button in the canvas widget
	// * depending on the selected action and the currently selected element
	// */
	// public void updateAddShapeActionButton() {
	// CanvasToolBar toolbar = ui.getCanvasToolBar();
	// String currentAction = ui.getSelectedAddShapeActionText();
	// ICanvasShape selected = getCanvas().getFirstSelected();
	//
	// if (storage.currentJAXBTranscript == null ||
	// storage.currentJAXBTranscript.getPage() == null)
	// return;
	//
	// if (toolbar.getAddShape()== null || toolbar.getAddShape().isDisposed())
	// return;
	//
	// toolbar.getAddShape().setEnabled(false);
	// if (!getCanvas().getSettings().isEditingEnabled())
	// return;
	//
	// Object data = null;
	// if (selected != null) {
	// data = selected.getData();
	// }
	//
	// boolean enable = false;
	// if (currentAction.equals(SegmentationTypes.TYPE_REGION)) {
	// enable = true;
	// } else if (currentAction.equals(SegmentationTypes.TYPE_PRINTSPACE)) {
	// enable =
	// storage.currentJAXBTranscript.getPageData().getPage().getPrintSpace() ==
	// null;
	// } else if (currentAction.equals(SegmentationTypes.TYPE_LINE)) {
	// enable = (data != null && data instanceof TextRegionType);
	// } else if (currentAction.equals(SegmentationTypes.TYPE_WORD)) {
	// enable = (data != null && data instanceof TextLineType);
	// } else if (currentAction.equals(SegmentationTypes.TYPE_BASELINE)) {
	// enable = data != null && data instanceof TextLineType && ((TextLineType)
	// data).getBaseline() == null;
	// }
	//
	// toolbar.getAddShape().setEnabled(enable);
	// }

	public void updateTranscriptionWidget(TranscriptionLevel type) {
		ATranscriptionWidget aw = null;
		if (type == TranscriptionLevel.WORD_BASED)
			aw = ui.getWordTranscriptionWidget();
		else
			aw = ui.getLineTranscriptionWidget();

		try {
			// update storage data:
			ICanvasShape shape = getCanvas().getFirstSelected();
			storage.updateDataForSelectedShape(shape);

			aw.updateData(storage.getCurrentRegionObject(), storage.getCurrentLineObject(), storage.getCurrentWordObject());
		} catch (Throwable th) {
			onError("Error updating transcription", "Error during the update of the transcription widget (" + type + ")", th);
		}

		logger.debug("finished updating " + type + " based trancription widget");
	}
	
	public void updateSelectedTranscriptionWidgetData() {
		if (ui!=null && ui.getSelectedTranscriptionWidget()!=null) {
			updateTranscriptionWidget(ui.getSelectedTranscriptionWidget().getTranscriptionLevel());	
		}
		
		// do *not* update all transcript widgets...
//		updateLineTranscriptionWidgetData();
//		updateWordTranscriptionWidgetData();
	}

	public void updateWordTranscriptionWidgetData() {
		updateTranscriptionWidget(TranscriptionLevel.WORD_BASED);
	}

	public void updateLineTranscriptionWidgetData() {
		updateTranscriptionWidget(TranscriptionLevel.LINE_BASED);
	}

	public void jumpToPage(int index) {
		if (saveTranscriptDialogOrAutosave()) {
			if (storage.setCurrentPage(index)) {
				reloadCurrentPage(true, () -> {
					if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
						autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
					}					
				}, null);
//				if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
//					autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
//				}
			}
		}
	}

	public void jumpToTranscript(TrpTranscriptMetadata md, boolean reloadSamePage) {
		if (saveTranscriptDialogOrAutosave()) {
			boolean changed = storage.setCurrentTranscript(md);

			if (reloadSamePage || changed) {
				reloadCurrentTranscript(false, true, null, null);
			}
		}
	}

	public void jumpToNextRegion() {
		jumpToRegion(Storage.getInstance().getCurrentRegion() + 1);
	}

	public void jumpToPreviousRegion() {
		jumpToRegion(Storage.getInstance().getCurrentRegion() - 1);
	}

	public void jumpToNextCell(int keycode) {
		TrpTableCellType currentCell = TableUtils.getTableCell(GuiUtil.getCanvasShape(Storage.getInstance().getCurrentRegionObject())); 
		if (currentCell == null) {
			logger.debug("No table found in transcript");
			return;
		}
		TableUtils.selectNeighborCell(getCanvas(), 
				currentCell, 
				TableUtils.parsePositionFromArrowKeyCode(keycode));
	}
	
	public void jumpToRegion(int index) {
		if (storage.jumpToRegion(index)) {
			// get item and select it in canvas, then it will automatically be
			// shown in the transcription widget:
			selectObjectWithData(storage.getCurrentRegionObject(), true, false);
			getCanvas().focusFirstSelected();
		}
	}

	// public ICanvasShape selectObjectWithData(ITrpShapeType trpShape) {
	// return selectObjectWithData(trpShape, true);
	// }

	public ICanvasShape selectObjectWithData(ITrpShapeType trpShape, boolean sendSignal, boolean multiselect) {
		ICanvasShape shape = null;
		if (trpShape != null && trpShape.getData() != null) {
			shape = (ICanvasShape) trpShape.getData();
			getScene().selectObject(shape, sendSignal, multiselect);
		}
		return shape;
	}

	public void nextPage() {
		jumpToPage(storage.getPageIndex() + 1);
	}

	public void prevPage() {
		jumpToPage(storage.getPageIndex() - 1);
	}

	public void firstPage() {
		jumpToPage(0);
	}

	public void lastPage() {
		jumpToPage(storage.getNPages() - 1);
	}

	public void reloadCurrentDocument() {
		if (!storage.isDocLoaded())
			return;

		if (storage.getDoc().isRemoteDoc())
			loadRemoteDoc(storage.getDoc().getId(), storage.getDoc().getCollection().getColId());
		else
			loadLocalDoc(storage.getDoc().getMd().getLocalFolder().getAbsolutePath());
	}

	/** Returns false if user presses cancel */
	public boolean saveTranscriptDialogOrAutosave() {
		if (!storage.isPageLocked() && storage.isTranscriptEdited()) {
			int r = DialogUtil.showYesNoCancelDialog(getShell(), "Unsaved changes",
					"There are unsaved changes in the transcript - do you want to save them first?");
			if (r == SWT.CANCEL)
				return false;
			if (r == SWT.NO){
				/*
				 * if user does not want to keep this transcript the auto saved version should be deleted as well
				 * otherwise the autosaved version will be found and suggested to the user as newer then the version he has stored latest when he came back 
				 * to that page
				 */
				this.autoSaveController.deleteAutoSavedFilesForThisPage(storage.getPage());
				return true;
			}
			if (r == SWT.YES) {
				return this.saveTranscription(false);
			}
		}
		return true;
	}
	
	public void updatePageLock() {
		if (storage.isPageLocked() != isPageLocked) { // page locking changed
			isPageLocked = storage.isPageLocked();
			boolean canTranscribe = Storage.getInstance().getRoleOfUserInCurrentCollection().canTranscribe();
			TrpConfig.getCanvasSettings().setEditingEnabled(!isPageLocked && canTranscribe);

			SWTUtil.setEnabled(ui.getCanvasWidget().getEditingEnabledToolItem(), !isPageLocked);
			
			SWTUtil.setEnabled(ui.getTranscriptionComposite(), !isPageLocked);
			SWTUtil.setEnabled(ui.getSaveDropDown(), !isPageLocked);

			updatePageInfo();
			updateToolBars();
		}
	}

	public void closeCurrentDocument(boolean force) {
		if (force || saveTranscriptDialogOrAutosave()) {
			storage.closeCurrentDocument();

			reloadCurrentPage(false, () -> {
				updatePageInfo();
			}, null);
			clearThumbs();
		}
	}

	/**
	 * (Tries) to determine whether the selection in the current transcription
	 * widget corresponds to the current selection in the canvas. This method is
	 * used e.g. in the class {@link PageMetadataWidgetListener} to distinguish
	 * between tagging based on the transcriptin widget selection and the canvas
	 * selection.
	 */
	public boolean isTextSelectedInTranscriptionWidget() {
		ATranscriptionWidget aw = ui.getSelectedTranscriptionWidget();
		// if (aw == null)
		// return false;

		return aw != null && !aw.isSingleSelection();

		// this was all bullshit:
		// Class<? extends ITrpShapeType> clazz =
		// aw.getTranscriptionUnitClass();
		// // get selected shape data in canvas:
		// List<Object> selectedData = canvas.getScene().getSelectedData();
		// int nSelectedInCanvas = selectedData.size();
		//
		// // if selection range in transcription widget is empty and multiple
		// elements are selected in the canvas, return false:
		// boolean isSelectionEmpty = aw.getText().getSelectionText().isEmpty();
		// if (isSelectionEmpty /*&& nSelectedInCanvas>1*/)
		// return false;

		// // get selected shapes in transcription widget:
		// List<ITrpShapeType> selectedInTw = aw.getSelectedShapes();
		// int nSelectedInTw = selectedInTw.size();
		//
		// // nr of selections in canvas != number of selection in transcription
		// widget -> return false (should be covered by above if however I
		// guess...)
		// if (nSelectedInCanvas != nSelectedInTw)
		// return false;
		//
		// // now check if all elements selected in the canvas correspond with
		// the elements selected in the tw:
		// for (int i=0; i<selectedData.size(); ++i) {
		// Object o = selectedData.get(i);
		// if (!selectedInTw.contains(o))
		// return false;
		// }
		// return true;
	}

	public void updatePageRelatedMetadata() {
		logger.debug("updating page related metadata!");

		int nSel = canvas.getNSelected();
		List<ICanvasShape> selected = canvas.getScene().getSelectedAsNewArray();

		if (!storage.hasTranscript()) {
//			ui.taggingWidget.setSelectedTags(null);
			ui.getStructuralMetadataWidget().updateData(null, null, nSel, null, new ArrayList<CustomTag>());
			return;
		}

		// TEST: update tagging widget:
		// ui.tw3.setInput(storage.getTranscript().getPage());

		// storage.getTranscript().getPage().getTagsMap();

		// get structure type:
		// boolean hasStructure = nSel>=1;
		String structureType = null;
		for (ICanvasShape s : selected) {
			ITrpShapeType st = GuiUtil.getTrpShape(s);
			if (structureType == null) {
				structureType = st.getStructure();
			} else if (!structureType.equals(st.getStructure())) {
				structureType = null;
				break;
			}
		}

		ITrpShapeType st = GuiUtil.getTrpShape(canvas.getFirstSelected());
		if (nSel == 1) {
			structureType = st.getStructure();
		}

		// get tag(s) under cursor:
		List<CustomTag> selectedTags = new ArrayList<>();
		if (getUi().getSelectedTranscriptionWidget() != null) {
			selectedTags = getUi().getSelectedTranscriptionWidget().getCustomTagsForCurrentOffset();
		}

		// List<CustomTag> selectedTags =
		// getUi().getSelectedTranscriptionWidget().getSelectedCommonCustomTags();
		// logger.debug("update metadata, nr of tags = "+selectedTags.size());

		// for (CustomTag t : selectedTags) {
		// if (!(t instanceof TextStyleTag))
		// selectedTagNames.add(t.getTagName());
		// }

//		ui.taggingWidget.setSelectedTags(selectedTags);
		ui.getStructuralMetadataWidget().updateData(storage.getTranscript(), st, nSel, structureType, selectedTags);
		
//		if (ui.getTextStyleWidget()!=null) {
//			ui.getTextStyleWidget().updateData();	
//		}
	}
	
	/**
	 * Change reading order circle width according to image resolution
	 */
	private void adjustReadingOrderDisplayToImageSize() {
		ImageMetadata imgMd = storage.getCurrentImageMetadata();
		if (imgMd == null){
			return;
		}
		double initWidth = readingOrderCircleInitWidth;
		logger.debug("initWidth ro size " + initWidth);
		
		double resizeFactor = 1.0;
		if (imgMd.getxResolution() < 210){
			resizeFactor = 0.5;
		}
		else if(imgMd.getxResolution() > 210 && imgMd.getxResolution() < 390){
			resizeFactor = 1.0;
		}
		double tmpWith = initWidth*resizeFactor;
		logger.debug("set ro in settings " + tmpWith);
		
		//let the valie in the settings as it is
		//canvas.getSettings().setReadingOrderCircleWidth((int) tmpWith);
	}

	public void updateTreeSelectionFromCanvas() {
		if (structTreeListener.isInsideTreeSelectionEvent) {
			// logger.debug("not updating tree!!");
			return;
		}

		List<Object> selData = canvas.getScene().getSelectedData();

		// select lines for baselines in struct view if lines not visible: 
		if (!getTrpSets().isShowLines()) {
			for (int i = 0; i < selData.size(); ++i) {
				Object o = selData.get(i);
				if (o instanceof TrpBaselineType) {
					TrpBaselineType bl = (TrpBaselineType) o;
					selData.set(i, bl.getLine());
				}
			}
		}

		// logger.debug("selected data size = "+selData.size());

		StructuredSelection sel = new StructuredSelection(selData);
		logger.debug("updateTreeSelectionFromCanvas, sel = "+sel);

		// if (!selData.isEmpty()) {
		// //
		// logger.debug("selected data = "+canvas.getScene().getSelectedData());
		// sel = new StructuredSelection(canvas.getScene().getSelectedData());
		// }

		getTreeListener().detach();
		ui.getStructureTreeViewer().setSelection(sel, true);
		getTreeListener().attach();
		
		ui.getStructuralMetadataWidget().getStructTagListWidget().updateTreeSelectionFromCanvas(selData);
	}

	// public void updateDocMetadata() {
	// if (storage.getDoc() != null)
	// ui.getDocMetadataEditor().setMetadata(storage.getDoc().getMd());
	// else
	// ui.getDocMetadataEditor().setMetadata(null);
	// }

	public void reloadCurrentImage() {
		try {
			Storage.getInstance().reloadCurrentImage(TrpMainWidget.getInstance().getSelectedImageFileType());
			updatePageInfo();
		} catch (Throwable e) {
			onError("Image load error", "Error loading main image", e);
		}
	}
	
	/**
	 * Reloads the current page synchronously -> use *only* when you need to have the page loaded after this call  
	 */
	public boolean reloadCurrentPageSync(boolean force, boolean reloadTranscript, CanvasAutoZoomMode zoomMode) {
		return docPageController.reloadCurrentPage(force, reloadTranscript, zoomMode, null, null);
	}

	public Future<PageLoadResult> reloadCurrentPage(boolean force, Runnable onSuccess, Runnable onError) {
		return reloadCurrentPage(force, true, null, onSuccess, onError);
	}

	/**
	 * Reload the current page that is set in {@link #storage}.
	 * @param force Forces a reload of the page without asking to save changes
	 * @param reloadTranscript Also reload the current transcript?
	 * @param zoomMode Specifies the zoom mode this page should be set to, if null the current transformation is kept.
	 * @return True if page was reloaded, false otherwise
	 */
	public Future<PageLoadResult> reloadCurrentPage(boolean force, boolean reloadTranscript, CanvasAutoZoomMode zoomMode, Runnable onSuccess, Runnable onError) {
		if (RELOAD_PAGE_ASYNC) {
			return docPageController.reloadCurrentPageAsync(force, reloadTranscript, zoomMode, onSuccess, onError);
		}
		else {
			boolean success = docPageController.reloadCurrentPage(force, reloadTranscript, zoomMode, onSuccess, onError);
			if (success) { // return a pseudo Future that instantly returns result
				return new Future<PageLoadResult>() {
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public PageLoadResult get() throws InterruptedException, ExecutionException {
						PageLoadResult p = new PageLoadResult();
						p.doc = storage.getDoc();
						p.page = storage.getPage();
						p.image = storage.getCurrentImage();
						p.imgMd = storage.getCurrentImageMetadata();
						p.metadataList = storage.getPage().getTranscripts();
						return p;
					}

					@Override
					public PageLoadResult get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return get();
					}
				};
			}
			else {
				return null;	
			}
		}
		
//		if (!force && !saveTranscriptDialogOrAutosave()) {
//			return false;
//		}
//
//		try {
//			logger.info("loading page: " + storage.getPage());
//			clearCurrentPage();
//
//			final int colId = storage.getCurrentDocumentCollectionId();
//			final String fileType = mw.getSelectedImageFileType();
//			logger.debug("selected img filetype = " + fileType);
//
//			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
//				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					try {
//						logger.debug("Runnable reloads page with index = " + (storage.getPageIndex() + 1));
//						monitor.beginTask("Loading page " + (storage.getPageIndex() + 1), IProgressMonitor.UNKNOWN);
//						storage.reloadCurrentPage(colId, fileType);
//					} catch (Exception e) {
//						throw new InvocationTargetException(e);
//					}
//				}
//			}, "Loading page", false);
//
//			if (storage.isPageLoaded() && storage.getCurrentImage() != null) {
//				getScene().setMainImage(storage.getCurrentImage());
//			}
//			
//			getScene().setCanvasAutoZoomMode(zoomMode);
//
//			if (reloadTranscript && storage.getNTranscripts() > 0) {
//				storage.setLatestTranscriptAsCurrent();
//				reloadCurrentTranscript(false, true);
//				updateVersionStatus();
//			}
//
//			return true;
//		} catch (Throwable th) {
//			String msg = "Could not load page " + (storage.getPageIndex() + 1);
//			onError("Error loading page", msg, th);
//
//			return false;
//		} finally {
//			updatePageLock();
//			ui.getCanvasWidget().updateUiStuff();
//			updateSegmentationEditStatus();
//			getCanvas().updateEditors();
//			updatePageRelatedMetadata();
//			updateToolBars();
//			updatePageInfo();
//		}
	}

	public void createThumbForCurrentPage() {
		// generate thumb for loaded page if local doc:
		if (storage.isLocalDoc() && storage.getPage() != null && storage.getCurrentImage() != null) {
			CreateThumbsService.createThumbForPage(storage.getPage(), storage.getCurrentImage().img, false, null);
		}
	}
	
	public void clearThumbs() {
		//clear and reload only on next click on next selection of DocInfoWidget
		logger.debug("Clearing thumbnailwidget.");
		ui.getThumbnailWidget().clear();
		
		//or force immediate reload if DocInfoWidget is currently shown and a doc is already loaded
		final boolean docInfoWidgetIsShown = ui.getTabWidget().isDocInfoItemSelected();
		final boolean docLoaded = storage.isDocLoaded();
		final boolean forceThumbReloadNow = docInfoWidgetIsShown && docLoaded;
		logger.debug("DocInfoWidget forces thumb reload = {}", forceThumbReloadNow);
		if(forceThumbReloadNow) {
			updateThumbs();
		}
	}
	
	public void updateThumbs() {
		logger.trace("updating thumbs");
		Display.getDefault().asyncExec(updateThumbsWidgetRunnable); // asyncExec needed??
		
		// try {
		// ui.thumbnailWidget.setUrls(storage.getDoc().getThumbUrls(),
		// storage.getDoc().getPageImgNames());
		// } catch (Exception e) {
		// onError("Error loading thumbnails", e.getMessage(), e);
		// }
	}

	void clearCurrentPage() {
		getScene().clear();
		// getScene().selectObject(null);
		ui.getStructureTreeViewer().setInput(null);
		// getTreeViewer().refresh();

		updateToolBars();
	}

//	public void reloadTranscriptsList() {
//		try {
//			int colId = storage.getCurrentDocumentCollectionId();
//			storage.reloadTranscriptsList(colId);
//		} catch (Throwable e) {
//			onError("Error updating transcripts", "Error updating transcripts", e);
//		}
//		updateToolBars();
//	}

	/**
	 * Reloads the current transcrition
	 * 
	 * @param tryLocalReload
	 *            If true, the transcription is reloaded from the locally stored
	 *            object (if it has been loaded already!)
	 */
	public Future<TrpPageType> reloadCurrentTranscript(boolean tryLocalReload, boolean force, Runnable onSuccess, Runnable onError) {
		if (RELOAD_TRANSCRIPT_ASYNC) {
			logger.debug("loading transcript async...");
			return docPageController.reloadCurrentTranscriptAsync(tryLocalReload, force, onSuccess, onError);
		}
		else {
			if (docPageController.reloadCurrentTranscript(tryLocalReload, force, onSuccess, onError)) { // on success of sync execution -> return pseudo Future that instantly returns result
				return new Future<TrpPageType>() {
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public TrpPageType get() throws InterruptedException, ExecutionException {
						return storage.hasTranscript() ? storage.getTranscript().getPage() : null;
					}

					@Override
					public TrpPageType get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return get();
					}
				};
			}
			else {
				return null;
			}
		}
	}

	public void showLocation(TrpLocation l) {
		// if (l.md == null) {
		// DialogUtil.showErrorMessageBox(getShell(),
		// "Error showing custom tag",
		// "Cannot open custom tag - no related metadata found!");
		// return;
		// }

		logger.debug("showing loation: " + l);

		// 1st: load doc & page
		if (!l.hasDoc()) {
			logger.debug("location has no doc specified!");
			return;
		}
		int pageIndex = l.hasPage() ? l.pageNr - 1 : 0;

		boolean wasDocLoaded = false;
		if (!storage.isThisDocLoaded(l.docId, l.localFolder)) {
			wasDocLoaded = true;
			if (l.docId == -1) {
				if (!loadLocalDoc(l.localFolder.getAbsolutePath(), pageIndex))
					return;
			} else {
				if (!loadRemoteDoc(l.docId, l.collId, pageIndex))
					return;
			}
		}

		// 2nd: load page if not loaded by doc anyway:
		if (!l.hasPage()) {
			logger.debug("location has no page specified!");
			return;
		}
		
		logger.debug("wasDocLoaded = {}", wasDocLoaded);
		logger.debug("storage.getPageIndex() = {}", storage.getPageIndex());
		logger.debug("TrpLocation::pageNr = {}", l.pageNr);
		if (!wasDocLoaded && storage.getPageIndex() != l.pageNr - 1) {
			if (!storage.setCurrentPage(l.pageNr - 1)) {
				return;
			}
			logger.info("Document is already loaded. Switching to page {}", l.pageNr);
			
			// FIXME: waiting for the future on async-loading blocks the UI (but only if the page is already loaded)
//			Future<PageLoadResult> future = reloadCurrentPage(true, null, null);
//			if (future == null) {
//				logger.debug("PageLoadResult future is null.");
//				return;
//			}
//			 // wait for page to be loaded!
//			try {
//				logger.debug("Waiting for PageLoadResult...");
//				PageLoadResult result = future.get();
//				logger.debug("Reload done: {}", result);
//			} catch (InterruptedException | ExecutionException e) {
//				logger.error(e.getMessage(), e);
//			}
			
			// this is the safe way to reload the page, although the annoying ProgressBar's will pop up... doesn't matter here!
			if (!reloadCurrentPageSync(true, true, null)) {
				return;
			}
		}

		// 3rd: select region / line / word:
		logger.debug("loading shape region: " + l.shapeId);
		if (l.shapeId == null) {
			logger.info("location has no region / line / word specified!");
			return;
		}
		ICanvasShape s = canvas.getScene().selectObjectWithId(l.shapeId, true, false);
		if (s == null) {
			logger.debug("shape is null!");
			return;
		}
		
		canvas.focusShape(s);
		
		ITrpShapeType st = canvas.getFirstSelectedSt();
		// 4th: select tag in transcription widget; TODO: select word!?
		if (l.t == null) {
			logger.debug("location has no tag specified!");
			return;
		} else {
			// reinforce focus on canvas
			canvas.focusShape(s, true);
		}
		if (st == null) {
			logger.warn("shape type could not be retrieved - should not happen here!");
		}
		
		ATranscriptionWidget tw = ui.getSelectedTranscriptionWidget();
		if (st instanceof TrpTextLineType && tw instanceof LineTranscriptionWidget) {
			logger.debug("selecting custom tag: "+l.t);
			tw.selectCustomTag(l.t);			
		}
		if (st instanceof TrpWordType && tw instanceof WordTranscriptionWidget) {
			// TODO
		} 
		
//		boolean isLine = l.getLowestShapeType() instanceof TrpTextLineType;
//		logger.debug("isLine: "+isLine);
//		boolean isWord = l.getLowestShapeType() instanceof TrpWordType;
//		
//		logger.debug("is lwtw: "+(tw instanceof LineTranscriptionWidget));
//		if (isLine && tw instanceof LineTranscriptionWidget) {
//
//		}
	}

	void clearTranscriptFromView() {
		getUi().getStructureTreeViewer().setInput(null);
		getCanvas().getScene().clearShapes();
		getCanvas().redraw();
	}

	// @SuppressWarnings("rawtypes")
	void loadJAXBTranscriptIntoView(JAXBPageTranscript transcript) {

		// add shapes to canvas:
		getCanvas().getScene().clearShapes();
		for (ITrpShapeType s : transcript.getPage().getAllShapes(false)) {
			shapeFactory.addAllCanvasShapes(s);
		}

		// set input to tree:
		// getUi().getStructureTreeViewer().setInput(transcript.getPage());
		getUi().getStructureTreeViewer().setInput(transcript.getPageData());

		getCanvas().redraw();
		// ui.updateTreeColumnSize();
		ui.getStructureTreeViewer().expandToLevel(3);
		// ui.getTreeViewer().expandToLevel(TreeViewer.ALL_LEVELS);
	}

	// public void updateAvailableTagNamesFromCurrentPage() {
	// ui.taggingWidget.updateAvailableTags();

	// if (true)
	// return;

	// if (!storage.hasTranscript())
	// return;

	// for (String tn : storage.getTranscript().getPage().getTagNames()) {
	// try {
	// logger.debug("1 adding tag: "+tn);
	// CustomTagFactory.addToRegistry(CustomTagFactory.create(tn));
	// } catch (Exception e) {
	// logger.warn(e.getMessage());
	// }
	// }

	// ui.taggingWidget.updateAvailableTags(); // still needed ... why???
	// }

	public CanvasShapeObserver getCanvasShapeObserver() {
		return canvasShapeObserver;
	}

	public CanvasWidget getCanvasWidget() {
		return ui.getCanvasWidget();
	}

	public SWTCanvas getCanvas() {
		return canvas;
	}

	public CanvasScene getScene() {
		return ui.getCanvas().getScene();
	}

//	public void updateSelectedTranscription() {
//		ui.versionsWidget.updateSelectedVersion(storage.getTranscriptMetadata());
//	}

	public void updateToolBars() {
		//boolean canManage = Storage.getInstance().getRoleOfUserInCurrentCollection().canManage();

		boolean canTranscribe = storage.getRoleOfUserInCurrentCollection().canTranscribe() && !storage.isGtDoc();
		boolean isDocLoaded = storage.isDocLoaded();

		int nNPages = storage.getNPages();
		boolean isPageLocked = storage.isPageLocked();

		ui.getPagesPagingToolBar().setToolbarEnabled(nNPages > 0);
		ui.getPagesPagingToolBar().setValues(storage.getPageIndex() + 1, nNPages);

		if (!SWTUtil.isDisposed(ui.getPagesPagingToolBar().getLabelItem())) {
			ui.getPagesPagingToolBar().getLabelItem().setImage(isPageLocked ? Images.LOCK : null);
			ui.getPagesPagingToolBar().getLabelItem().setToolTipText(isPageLocked ? "Page locked" : "");
		}

		SWTUtil.setEnabled(ui.getCloseDocBtn(), isDocLoaded);
		SWTUtil.setEnabled(ui.getSaveDropDown(), isDocLoaded && canTranscribe);
		if (ui.saveOptionsToolItem != null)
			SWTUtil.setEnabled(ui.saveOptionsToolItem.getToolItem(), isDocLoaded && canTranscribe);

		SWTUtil.setEnabled(ui.getReloadDocumentButton(), isDocLoaded);
		SWTUtil.setEnabled(ui.getLoadTranscriptInTextEditor(), isDocLoaded);
		SWTUtil.setEnabled(ui.getStatusCombo(), isDocLoaded && canTranscribe);
		SWTUtil.setEnabled(ui.getExportDocumentButton(), isDocLoaded && canTranscribe);
		
		if (storage.getTranscript() != null && storage.getTranscript().getMd() != null){
			ui.getStatusCombo().setText(storage.getTranscript().getMd().getStatus().getStr());
		}
		
		ui.updateToolBarSize();
		
	}

	public void loginAsTestUser() {

	}

	public boolean loadLocalDoc(String folder) {
		return loadLocalDoc(folder, 0);
	}

	public boolean loadLocalDoc(String folder, int pageIndex) {
		if (!saveTranscriptDialogOrAutosave()) {
			return false;
		}

		try {
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading local document from "+folder, IProgressMonitor.UNKNOWN);
					try {
						storage.loadLocalDoc(folder, monitor);
						logger.debug("loaded local doc "+folder);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
//					} finally {
//						monitor.done();
//					}
				}
			}, "Loading local document", false);
			
			if(storage.getDoc() != null && storage.getDoc().isLocalDoc()) {
				// check if the doc contains known errors and display an error message in that case
				final String problems = storage.getDoc().getImageErrors();
				if(!StringUtils.isEmpty(problems)) {
					mw.onError("The document contains faulty pages!", problems, null);
				}
			}

			final boolean DISABLE_THUMB_CREATION_ON_LOAD = true;
			if (!DISABLE_THUMB_CREATION_ON_LOAD && getTrpSets().isCreateThumbs()) {
				//CreateThumbsService.createThumbForDoc(storage.getDoc(), false, updateThumbsWidgetRunnable);
			}

			storage.setCurrentPage(pageIndex);
			reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
				getCanvas().fitWidth();
			}, null);
			
			//store the path for the local doc
			RecentDocsPreferences.push(folder);
			ui.getServerWidget().updateRecentDocs();
			
			clearThumbs();
//			getCanvas().fitWidth();
			return true;
		} catch (Throwable th) {
			onError("Error loading local document", "Could not load document: " + th.getMessage(), th);
			return false;
		}
	}

	public void loadLocalFolder() {
		logger.debug("loading a local folder...");
		String fn = DialogUtil.showOpenFolderDialog(getShell(), "Choose a folder with images and (optional) PAGE XML files in a subfolder 'page'", lastLocalDocFolder);
		if (fn == null)
			return;

		lastLocalDocFolder = fn;
		loadLocalDoc(fn);
	}

	// public void loadTestDocFromServer() {
	// reloadCurrentDocument();
	// }

//	public boolean loadRemoteDoc(final int docId) {
//		return loadRemoteDoc(docId, 0);
//	}
	
	/**
	 * Tries to load a document just with a document id - first searches for documents with this id, 
	 * then loads the document for the first collection
	 */
	public boolean loadRemoteDoc(final int docId, boolean showMsgIfNotFound) {
		List<TrpDocMetadata> docs;
		try {
			docs = storage.getConnection().findDocuments(0, docId, null, null, null, null, 
					true, true, 0, 0, null, null);
			
			if (!docs.isEmpty()) {
				TrpDocMetadata doc = docs.get(0);
				return TrpMainWidget.getInstance().loadRemoteDoc(doc.getDocId(), doc.getFirstCollectionId());
			} else if (showMsgIfNotFound) {
				DialogUtil.showInfoMessageBox(getShell(), "No such document", "Could not find document with id "+docId+" in any collection!");
			}
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException
				| IllegalArgumentException e1) {
			logger.error(e1.getMessage(), e1);
			TrpMainWidget.getInstance().onError("Error", "Could not load document with id "+docId, e1);
		}
		
		return false;
	}

	public boolean loadRemoteDoc(final int docId, int colId) {
		return loadRemoteDoc(docId, colId, 0);
	}

	/**
	 * Loads a document from the remote server
	 * 
	 * @param docId
	 *            The id of the document to load
	 * @param colId
	 *            The id of the collection to load the document from.
	 *            A colId <= 0 means, the currently selected collection from the
	 *            DocOverViewWidget is taken (if one is selected!)
	 * @return True for success, false otherwise
	 */
	public boolean loadRemoteDoc(final int docId, int colId, int pageIndex) {
		if (!saveTranscriptDialogOrAutosave()) {
			return false;
		}

		try {
			updateSelectedCollection(colId);

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading remote document " + docId, IProgressMonitor.UNKNOWN);
					try {
						// if (true) throw new SessionExpiredException("Yo!");
						storage.loadRemoteDoc(colId, docId);
						logger.debug("loaded remote doc, colIdFinal = " + colId);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Loading document from server", false);

			storage.setCurrentPage(pageIndex);
			reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
				if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
					autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
				}
				getCanvas().fitWidth();
				/*
				 * this way we could automatically adjust the ro circle to the image dimensions
				 * but then the settings get overwritten; better keep user settings
				 */
				//adjustReadingOrderDisplayToImageSize();				
			}, null);
//			if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
//				autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
//			}
			
			//store the recent doc info to the preferences
			if (pageIndex == 0){
				RecentDocsPreferences.push(Storage.getInstance().getDoc().getMd().getTitle() + ";;;" + docId + ";;;" + colId);
			}
			else if (pageIndex > 0){
				RecentDocsPreferences.push(Storage.getInstance().getDoc().getMd().getTitle() + ";;;" + docId + ";;;" + colId + ";;;" + (pageIndex+1));
			}
			ui.getServerWidget().updateRecentDocs();
									
//			getUi().getServerWidget().setSelectedCollection(colId);
			getUi().getServerWidget().getDocTableWidget().loadPage("docId", docId, true);

			clearThumbs();
//			getCanvas().fitWidth();
//			adjustReadingOrderDisplayToImageSize();
			
			tmpCount++;
			return true;
		} catch (Throwable e) {
			onError("Error loading remote document", "Could not load document with id  " + docId, e);
			return false;
		}
		// finally {
		// updatePageInfo();
		// }
	}
	
	/**
	 * Load Model GT pages as document in storage and show in main widget.
	 */
	public boolean loadGroundTruthAsDoc(ModelGtDataSet set, int pageIndex) {
		if (!saveTranscriptDialogOrAutosave()) {
			return false;
		}

		//if this data set is already loaded but another pageIndex was passed then jump to page
		if(storage.isGtDoc() && storage.getDoc().getMd() instanceof TrpModelGtDocMetadata 
				&& ((TrpModelGtDocMetadata)storage.getDoc().getMd()).getDataSet().equals(set)
				&& storage.getPageIndex() != pageIndex) {
			logger.debug("Page switch in HTR GT data set document. pageIndex = " + pageIndex);
			//jump to page
			if (storage.setCurrentPage(pageIndex)) {
				reloadCurrentPage(true, null, null);
			}
			//skip any further loading below
			return true;
		}
		
		try {
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading HTR GT " + set.getDataSetType() + " HTR ID " + set.getId(), IProgressMonitor.UNKNOWN);
					try {
						// if (true) throw new SessionExpiredException("Yo!");
						storage.loadModelGtAsDoc(set, pageIndex);						
						logger.debug("loaded HTR GT of model {}", set.getModel().getName());
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Loading document from server", false);
			reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
				getCanvas().fitWidth();
				//adjustReadingOrderDisplayToImageSize();
			}, null);
			clearThumbs();
//			getCanvas().fitWidth();
//			adjustReadingOrderDisplayToImageSize();
			tmpCount++;
			return true;
		} catch (Throwable e) {
			onError("Error loading HTR GT", "Could not load GT set for HTR with id  " + set.getId(), e);
			return false;
		}
	}
	
	/**
	 * Load HTR GT pages as document in storage and show in main widget.
	 * @deprecated
	 */
	public boolean loadHtrGroundTruth(HtrGtDataSet set, int colId, int pageIndex) {
		if (!saveTranscriptDialogOrAutosave()) {
			return false;
		}

		//if this data set is already loaded but another pageIndex was passed then jump to page
		if(storage.isGtDoc() && storage.getDoc().getMd() instanceof TrpHtrGtDocMetadata 
				&& ((TrpHtrGtDocMetadata)storage.getDoc().getMd()).getDataSet().equals(set)
				&& storage.getPageIndex() != pageIndex) {
			logger.debug("Page switch in HTR GT data set document. pageIndex = " + pageIndex);
			//jump to page
			if (storage.setCurrentPage(pageIndex)) {
				reloadCurrentPage(true, null, null);
			}
			//skip any further loading below
			return true;
		}
		
		try {
			updateSelectedCollection(colId);

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading HTR GT " + set.getDataSetType() + " HTR ID " + set.getId(), IProgressMonitor.UNKNOWN);
					try {
						// if (true) throw new SessionExpiredException("Yo!");
						storage.loadHtrGtAsDoc(colId, set, pageIndex);						
						logger.debug("loaded HTR GT, colId = " + colId);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Loading document from server", false);
			reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
				getCanvas().fitWidth();
				//adjustReadingOrderDisplayToImageSize();
			}, null);
			clearThumbs();
//			getCanvas().fitWidth();
//			adjustReadingOrderDisplayToImageSize();
			tmpCount++;
			return true;
		} catch (Throwable e) {
			onError("Error loading HTR GT", "Could not load GT set for HTR with id  " + set.getId(), e);
			return false;
		}
	}
	
	/**
	 * Checks if the collection with colId is already loaded in storage and, if necessary, switches the collection and reloads the document list.
	 * 
	 * @param colId
	 * @throws Exception
	 */
	private void updateSelectedCollection(int colId) throws Exception {
		boolean collectionChanged = colId != ui.serverWidget.getSelectedCollectionId();
		if (collectionChanged) {
			logger.debug("collection changed - reloading doclist!");
			Future<List<TrpDocMetadata>> fut = reloadDocList(colId);
			if (fut == null)
				throw new Exception("Documents for collection " + colId + " could not be loaded.");
			
			List<TrpDocMetadata> docList = fut.get(); // wait for doclist to be loaded!
			logger.debug("loaded new doclist of size "+docList.size()+" current-collection: "+getSelectedCollection());
			//depending on collection size this log message impacts performance (prod collection 2 doclist size is ~900KB)
			logger.trace("docList = " + docList);
		}
		canvas.getScene().selectObject(null, true, false); // security measure due to mysterious bug leading to freeze of progress dialog

		if (colId <= 0) {
			colId = ui.getServerWidget().getSelectedCollectionId();
			if (colId <= 0)
				throw new Exception("No collection specified to load HTR ground truth data!");
		}
	}

	public void center() {
		ui.center();
	}
	
	public void onError(String title, String message, Throwable th, boolean logStackTrace, boolean showBalloonTooltip) {
		canvas.getMouseListener().reset();
		canvas.setMode(CanvasMode.SELECTION);
		canvas.layout();

		if (th instanceof SessionExpiredException) {
			sessionExpired = true;
			logout(true, false);
			logger.warn("Session expired!");
			loginDialog("Session expired!");
		} else {
			if (th instanceof NullPointerException && StringUtils.isEmpty(message)) {
				message = "NullPointerException";
			}
			
			SWTLog.logError(logger, getShell(), title, message, th, logStackTrace);

			if (!showBalloonTooltip) {
				if (SWTLog.showError(logger, getShell(), title, message, th) == IDialogConstants.HELP_ID) {
					sendBugReport();
				}
			} else
				DialogUtil.createAndShowBalloonToolTip(getShell(), SWT.ICON_ERROR, title, message, 2, true);
		}
	}
	
	public static void onErrorStatic(String title, String message, Throwable th) {
		if (mw != null) {
			mw.onError(title, message, th, true, false);
		}
	}

	/**
	 * Prints an error message and the stack trace of the given throwable to the
	 * error log and pops up an error message box. Also, resets data in some
	 * listeners to recover from the error.
	 */
	public void onError(String title, String message, Throwable th) {
		onError(title, message, th, true, false);
	}

	public void onInterruption(String title, String message, Throwable th) {
		onError(title, message, th, true, true);
	}
	
	private static boolean shouldITrack() {
		
		try {
			try (FileInputStream fis = new FileInputStream(new File("config.properties"))) {
				Properties p = new Properties();
				p.load(fis);
				
				Object tracking = p.get("tracking");
				if (tracking!=null && ((String)tracking).equals("true"))
					return true;
			}
		} catch (Exception e) {
			logger.warn("Could not determine tracking property: "+e.getMessage());
		}
		return false;
	}

	public static void show() {
		logger.debug("cwd = "+System.getProperty("user.dir"));
		ProgramInfo info = new ProgramInfo();
		Display.setAppName(info.getName());
		Display.setAppVersion(info.getVersion());
		
		DeviceData data = new DeviceData();

		data.tracking = shouldITrack();
		logger.info("resource tracking = "+data.tracking);
//		logger.info("classpath = "+CoreUtils.getClassPathString(TrpMainWidget.class));
		
		Display display = new Display(data);

		show(display);
	}

	public static void show(Display givenDisplay) {
		BidiUtils.setBidiSupport(true);
		logger.debug("bidiSupport: "+BidiUtils.getBidiSupport()+ " isBidiPlatform: "+BidiUtil.isBidiPlatform());
		
		GuiUtil.initLogger();
		try {
			// final Display display = Display.getDefault();

			if (givenDisplay != null)
				display = givenDisplay;
			else
				display = new Display();

			final Shell shell = new Shell(display, SWT.SHELL_TRIM);
			setMainShell(shell);

//			Realm.runWithDefault(SWTObservables.getRealm(display), new Runnable() {
			Realm.runWithDefault(DisplayRealm.getRealm(display), new Runnable() {
				@Override public void run() {
					if (USE_SPLASH) {
						final SplashWindow sw = new SplashWindow(display);
						sw.start(new Runnable() {
							@Override public void run() {
								sw.setProgress(10);
								shell.setLayout(new FillLayout());
								mw = new TrpMainWidget(shell);
								shell.setMaximized(true);
								shell.open();
								shell.layout();
								sw.setProgress(50);
								// sw.setProgress(66);
								mw.postInit();
								// if (true) throw new
								// NullPointerException("ajdflkasjdf");

								sw.setProgress(100);
								sw.stop();

							}
						});
					} else {
						shell.setLayout(new FillLayout());
						mw = new TrpMainWidget(shell);
						// TrpMainWidgetView ui = mw.ui;

						// shell.setSize(1400, 1000);
						// mw.center();
						shell.setMaximized(true);
						shell.open();
						shell.layout();
						mw.postInit();
					}
					
//					mw.openChangeLogDialog(getTrpSettings().isShowChangeLog());
					mw.showTrayNotificationOnChangelog(false);
					mw.openJavaVersionDialog(false);						

					// the main display loop:
					logger.debug("entering main event loop");
					
					// while((Display.getCurrent().getShells().length != 0)
					// && !Display.getCurrent().getShells()[0].isDisposed()) {
					while (!shell.isDisposed()) {
						try {
							if (!Display.getCurrent().readAndDispatch()) {
								Display.getCurrent().sleep();
							}
						} catch (Throwable th) {
							logger.error("Unexpected error occured: "+th.getMessage(), th);
						}
					}

				}
			});

			// Display.getCurrent().dispose();
			logger.debug("Program end");

			// while (!ui.isDisposed()) {
			// if (!display.readAndDispatch()) {
			// display.sleep();
			// }
			// }
		} catch (Throwable e) {
			// Display.getCurrent().dispose();
			logger.error("PROGRAM EXIT WITH FATAL ERROR: " + e.getMessage(), e);
		}
	}

	// public void show() {
	// try {
	// Realm.runWithDefault(SWTObservables.getRealm(display),
	// new Runnable() {
	// public void run() {
	// ui.setSize(1400, 1000);
	// center();
	// ui.open();
	// ui.layout();
	//
	// postInit();
	// while (!ui.isDisposed()) {
	// if (!display.readAndDispatch()) {
	// display.sleep();
	// }
	// }
	// }
	// });
	//
	// // while (!ui.isDisposed()) {
	// // if (!display.readAndDispatch()) {
	// // display.sleep();
	// // }
	// // }
	// } catch (Exception e) {
	// logger.error(e);
	// }
	// }

	private static void setMainShell(Shell shell) {
		mainShell = shell;

	}

	public TrpMainWidgetView getUi() {
		return ui;
	}

	public Shell getShell() {
		return ui.getShell();
	}

	public CanvasSettings getCanvasSettings() {
		return ui.getCanvas().getSettings();
	}

	public TrpSettings getTrpSets() {
		return ui.getTrpSets();
	}

	public void redrawCanvas() {
		getCanvas().redraw();
	}

	public void refreshStructureView() {
		ui.getStructureTreeViewer().refresh();
		ui.getStructuralMetadataWidget().getStructTagListWidget().getTreeViewer().refresh();
	}

	// public TreeViewer getStructureTreeViewer() {
	// return ui.getStructureTreeViewer();
	// }

	public StructureTreeListener getTreeListener() {
		return structTreeListener;
	}

	public TrpShapeElementFactory getShapeFactory() {
		return shapeFactory;
	}

	public TranscriptObserver getTranscriptObserver() {
		return transcriptObserver;
	}

	/**
	 * replaced by {@link #uploadDocuments()}
	 */
	@Deprecated public void uploadSingleDocument() {
		try {
			if (!storage.isLoggedIn()) {
				DialogUtil.showErrorMessageBox(getShell(), "Not logged in!", "You have to be logged in to upload a document!");
				return;
			}

			final UploadDialog ud = new UploadDialog(getShell(), ui.getServerWidget().getSelectedCollection());
			int ret = ud.open();

			if (ret == IDialogConstants.OK_ID) {
				final TrpCollection c = ud.getCollection();
				final int cId = (c == null) ? -1 : c.getColId();
				if (c == null || (c.getRole() != null && !c.getRole().canManage())) {
					throw new Exception("Cannot upload to specified collection: " + cId);
				}

				logger.debug(
						"uploading to directory: " + ud.getFolder() + ", title: '" + ud.getTitle() + " collection: " + cId + " viaFtp: " + ud.isUploadViaFtp());
				String type = ud.isUploadViaFtp() ? "FTP" : "HTTP";

				// final int colId =
				// storage.getCollectionId(ui.getDocOverviewWidget().getSelectedCollectionIndex());
				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
					@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							// storage.uploadDocument(4, ud.getFolder(),
							// ud.getTitle(), monitor);// TEST
							boolean uploadViaFTP = ud.isUploadViaFtp();
							logger.debug("uploadViaFTP = " + uploadViaFTP);
							storage.uploadDocument(cId, ud.getFolder(), ud.getTitle(), monitor);
							if (!monitor.isCanceled())
								displaySuccessMessage(
										"Uploaded document!\nNote: the document will be ready after document processing on the server is finished - reload the document list occasionally");
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						}
					}
				}, "Uploading via " + type, true);
			}
		} catch (Throwable e) {
			onError("Error loading uploading document", "Could not upload document", e);
		}
	}

	public void uploadDocuments() {
		try {
			if (!storage.isLoggedIn()) {
				DialogUtil.showErrorMessageBox(getShell(), "Not logged in!", "You have to be logged in to upload a document!");
				return;
			}

//			final UploadFromFtpDialog ud = new UploadFromFtpDialog(getShell(), ui.getServerWidget().getSelectedCollection());
			final UploadDialogUltimate ud = new UploadDialogUltimate(getShell(), ui.getServerWidget().getSelectedCollection());
			if (ud.open() != IDialogConstants.OK_ID)
				return;

			final TrpCollection c = ud.getCollection();
			final int cId = (c == null) ? -1 : c.getColId();
			if (c == null || (c.getRole() != null && !c.getRole().canManage())) {
				throw new Exception("You must be at least an editor to upload to this collection ("+ cId+")");
			}

			if (ud.isSingleDocUpload()) { // single doc upload
				logger.debug("uploading to directory: " + ud.getFolder() + ", title: '" + ud.getTitle() + " collection: " + cId);
				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
					@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						TrpUpload upload = null;
						try {
							upload = storage.uploadDocument(cId, ud.getFolder(), ud.getTitle(), monitor);
							if (!monitor.isCanceled()) {
								displaySuccessMessage(
										"Uploaded document!\nNote: the document will be ready after document processing on the server is finished - reload the document list occasionally");
							}
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						}
					}
				}, "Uploading via HTTPS", true);
			} else if (ud.isMetsUrlUpload()) {
				logger.debug("uploading mets from url " + ud.getMetsUrl() + " to collection: " + cId);
				//test url: http://rosdok.uni-rostock.de/file/rosdok_document_0000007322/rosdok_derivate_0000026952/ppn778418405.dv.mets.xml
				int h = DialogUtil.showInfoMessageBox(getShell(), "Upload Information",
						"Upload document!\nNote: the document will be ready after document processing on the server is finished - this takes a while - reload the document list occasionally");
				try {
					storage.uploadDocumentFromMetsUrl(cId, ud.getMetsUrl());
				} catch (ClientErrorException e) {
					if (e.getMessage().contains("DFG-Viewer Standard")) {
						onError("Error during uploading from Mets URL - reason: ", e.getMessage(), e);
					} else {
						throw e;
					}

				}
			
//				catch (SessionExpiredException | ServerErrorException eo) {
//				// TODO Auto-generated catch block
//				throw eo;
//			}
				// extract images from pdf and upload extracted images
			}else if (ud.isIiifUrlUpload()) {
				// check if Manifest is valid before starting job
				boolean valid = false;
				try {
					URL url = new URL(ud.getIiifUrl());
					IIIFUtils.checkManifestValid(url);
					valid = true;
				} catch(IllegalArgumentException|JsonMappingException | JsonParseException e) {
					DialogUtil.showDetailedErrorMessageBox(getShell(), "Manifest not valid",
							"Upload document from IIIF manifest not possible!\n"
							+ "Note: IIIF manifest is not valid and therefore cannot be parsed.", e.getMessage());
				
				} catch( IOException e) {
					DialogUtil.showDetailedErrorMessageBox(getShell(), "Manifest could not be fetched",
							"Upload document from IIIF manifest not possible!\n"
							+ "Note: IIIF manifest could not be fetched and therefore cannot be parsed.", e.getMessage());
				}
				if(valid) {
					logger.debug("uploading iiif from url " + ud.getIiifUrl() + " to collection: " + cId);
					int h = DialogUtil.showInfoMessageBox(getShell(), "Upload Information",
							"Upload document from IIIF manifest!\nNote: the document will be ready after document processing on the server is finished - this takes a while - reload the document list occasionally");
					storage.uploadDocumentFromIiifUrl(cId, ud.getIiifUrl());
				}
	
			}
			
			
			else if (ud.isUploadFromPdf()) {
				File pdfFolderFile = ud.getPdfFolderFile();
				logger.debug("extracting images from pdf " + ud.getFile() + " to local folder " + pdfFolderFile.getAbsolutePath());
				logger.debug("ingest into collection: " + cId);
				
				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
					@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							storage.uploadDocumentFromPdf(cId, ud.getFile(), pdfFolderFile.getAbsolutePath(), monitor);
							if (!monitor.isCanceled())
								displaySuccessMessage(
										"Uploaded document!\nNote: the document will be ready after document processing on the server is finished"
										+ " - reload the document list occasionally");
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						}
					}
				}, "Uploading PDF via HTTPS", true);

			} else { // private ftp ingest
				final List<TrpDocDir> dirs = ud.getDocDirs();
				if (dirs == null || dirs.isEmpty()) {
					//should not happen. check is already done in Dialog...
					throw new Exception("DocDir list is empty!");
				}
				
				for (final TrpDocDir d : dirs) {
					try {
						storage.uploadDocumentFromPrivateFtp(cId, d.getName(), true);
					} catch (final ClientErrorException ie) {
						if (ie.getResponse().getStatus() == 409) { // conflict! (= duplicate name)
							if (DialogUtil.showYesNoDialog(getShell(), "Duplicate title", ie.getMessage() + "\n\nIngest anyway?") == SWT.YES) {
								storage.uploadDocumentFromPrivateFtp(cId, d.getName(), false);
							}
						}
						else {
							throw ie;
						}
					}
				}
				
				DialogUtil.createAndShowBalloonToolTip(getShell(), SWT.ICON_INFORMATION, "Upload runs as job and can take a while\n"
						+ "- reload the collection to show finished docs", "FTP Upload",
						2, true);
				storage.sendJobListUpdateEvent();
				
//				ui.selectJobListTab();

//				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
//					@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//						try {
//							monitor.beginTask("Starting jobs", dirs.size());
//							int i = 0;
//							for(final TrpDocDir d : dirs) {
//								if(monitor.isCanceled())
//									break;
//								
////								String docTitle = d.getMetadata()==null ? d.getName() : d.getMetadata().getTitle();
//								try {
//									storage.uploadDocumentFromPrivateFtp(cId, d.getName(), true);
//								} catch (final ClientErrorException ie) {
//									
////									if (ie.getResponse().getStatus() == 409) { // conflict! (= duplicate name)
////										Display.getDefault().syncExec(new Runnable() {
////											@Override public void run() {
////												if (DialogUtil.showYesNoDialog(getShell(), "Duplicate title", ie.getMessage()+"\n\nIngest anyway?") == SWT.YES) {
////													storage.uploadDocumentFromPrivateFtp(cId, d.getName(), false);
////												}												
////											}
////										});
////										
////
////									}
//								}
//
//								monitor.worked(++i);
//							}
//							
//							if (!monitor.isCanceled()) {
//								displaySuccessMessage("Ingest jobs started!\nNote: the documents will be ready after document processing on the server is finished - reload the document list occasionally");
//							}
//							monitor.done();
//						} catch (Exception e) {
//							throw new InvocationTargetException(e);
//						}
//					}
//				}, "Ingesting", true);
			}
		} catch (Throwable e) {
			onError("Error uploading document", "Could not upload document", e);
		}
	}

	public void enable(boolean value) {
		canvas.setEnabled(value);
		ui.getTranscriptionComposite().setEnabled(value);
//		ui.getRightTabFolder().setEnabled(value);
	}

	public static void main(String[] args) throws IOException {
		// TEST:
		// TrpGui.getMD5sOfLibs();

		// Dynamically load the correct swt jar depending on OS:

		TrpMainWidget.show();
		
		if(args != null && args.length > 0) {
			File path = new File(args[0]);
			if(!path.canRead()) {
				logger.info("Ignoring unreadable path in first argument: {}", path.getAbsolutePath());
			} else {
				logger.info("Loading local doc on path in first argument: {}", path.getAbsolutePath());
				TrpMainWidget.getInstance().loadLocalDoc(args[0]);
			}
		}
		
		// TrpMainWidget mainWidget = new TrpMainWidget();
	}

//	@Deprecated public void deleteSelectedDocument() {
//		final TrpDocMetadata doc = ui.getServerWidget().getSelectedDocument();
//		try {
//			if (doc == null || !storage.isLoggedIn()) {
//				return;
//			}
//
//			if (DialogUtil.showYesNoDialog(getShell(), "Are you sure?", "Do you really want to delete document " + doc.getDocId()) != SWT.YES) {
//				return;
//			}
//
//			canvas.getScene().selectObject(null, true, false); // security
//																// measure due
//																// to mysterios
//																// bug leading
//																// to freeze of
//																// progress
//																// dialog
//			final int colId = storage.getCurrentDocumentCollectionId();
//			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
//				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					try {
//						logger.debug("deleting document...");
//						monitor.beginTask("Deleting document ", IProgressMonitor.UNKNOWN);
//						logger.debug("Deleting selected document: " + doc);
//						storage.deleteDocument(colId, doc.getDocId());
//						displaySuccessMessage("Deleted document " + doc.getDocId());
//					} catch (Exception e) {
//						throw new InvocationTargetException(e, e.getMessage());
//					}
//				}
//			}, "Exporting", false);
//
//			reloadDocList(ui.getServerWidget().getSelectedCollection());
//		} catch (Throwable e) {
//			onError("Error deleting document", "Could not delete document " + doc.getDocId(), e);
//		}
//	}

	public void displaySuccessMessage(final String message) {
		display.syncExec(new Runnable() {
			@Override public void run() {
				DialogUtil.showInfoMessageBox(getShell(), "Success", message);
			}
		});
	}

	public void displayCancelMessage(final String message) {
		display.syncExec(new Runnable() {
			@Override public void run() {
				DialogUtil.showInfoMessageBox(getShell(), "Cancel", message);
			}
		});
	}
	
	public void addPage(){		
		
		
		if(storage.getDoc() == null){
			DialogUtil.showErrorMessageBox(getShell(), "No remote document loaded", "No remote document loaded");
			return;
		}
		
		final String[] extArr = getAllowedFilenameExtensions();
		
		String filePath = DialogUtil.showOpenFileDialog(mw.getShell(), "Add page", null, extArr);
		logger.debug("Uploading new page from: " + filePath);
		if(filePath == null){
			logger.error("ERROR: Bad filepath");
			return;
		}
		File imgFile = new File(filePath);
		
		logger.debug(Long.toString(imgFile.length()));
		//Set new pageNr
		int pageNr = storage.getNPages()+1;
		int docId = storage.getDocId();
		int colId = storage.getCollId();	
		
		
		try {			
			if (!imgFile.canRead())
				throw new Exception("Can't read file at: " + filePath);
			ProgressBarDialog.open(mw.getShell(), new IRunnableWithProgress(){

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					
					try {
						monitor.beginTask("Uploading image file...colId " + colId + " docId " + docId + " pageNr " + pageNr, 120);
						Storage.getInstance().addPage(colId, docId, pageNr, imgFile, monitor);
					} catch (NoConnectionException e) {
						logger.error(e.toString());
					}					
				}				
			}, "Upload", false);			


			reloadCurrentDocument();

			
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

	}
	
	/**
	 * @return array containing filename extension filters for allowed image filetypes compatible with the file picker dialog
	 */
	private String[] getAllowedFilenameExtensions() {
		List<String> exts = ImgPriority.getAllowedFilenameExtensions();
		final String filterPrefix = "*.";
		String[] extArr = new String[exts.size()];
		for(int i = 0; i < exts.size(); i++) {
			extArr[i] = filterPrefix + exts.get(i);
		}
		logger.debug("Filename filter = " + Arrays.stream(extArr).collect(Collectors.joining(", ", "[", "]")));
		return extArr;
	}
	
	public boolean addSeveralPages2Doc() {
		logger.debug("Open Dialog for adding images");

		final String[] extArr = getAllowedFilenameExtensions();
		final ArrayList<String> imgNames = DialogUtil.showOpenFilesDialog(getShell(), "Select image files to add", null, extArr);
		if (imgNames == null)
			return false;

		try {
			int pageNr = storage.getNPages();
			//check img file
			for (String img : imgNames){
				final File imgFile = new File(img);
				
				pageNr += 1;
				int pageNumber = pageNr;
				int docId = storage.getDocId();
				int colId = storage.getCollId();
				
				if (docId == -2){
					DialogUtil.showInfoMessageBox(mw.getShell(), "No document loaded", "Page(s) can not be added - there is no remote document loaded");
					return false;
				}
				
				if (!imgFile.canRead()) {
					throw new Exception("Can't read file at: " + img);
				}
				
				ProgressBarDialog.open(mw.getShell(), new IRunnableWithProgress(){

					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						
						try {
							monitor.beginTask("Uploading image file...colId " + colId + " docId " + docId + " pageNr " + pageNumber, 120);
							Storage.getInstance().addPage(colId, docId, pageNumber, imgFile, monitor);
						} catch (NoConnectionException e) {
							logger.error(e.toString());
						}					
					}				
				}, "Upload", false);			


				//
			}
			reloadCurrentDocument();
			ui.getServerWidget().getDocTableWidget().reloadDocs(false, true);
			
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;	
	}

	public void deletePage() {
		logger.debug("Open Dialog for deleting page");
		if (!storage.isPageLoaded() || !storage.isRemoteDoc()) {
			DialogUtil.showErrorMessageBox(getShell(), "No remote page loaded", "No remote page loaded");
			return;
		}

		if (DialogUtil.showYesNoDialog(getShell(), "", "Do you really want to delete the current page?") != SWT.YES)
			return;

		try {
			//during deleting a page we don't care if it was edited before
			if (storage.isTranscriptEdited()) {
				storage.setCurrentTranscriptEdited(true);
			}
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("Delete page: " + storage.getPage().getPageNr());

						monitor.beginTask("Deleting page " + storage.getPage().getPageNr(), 1);

						//replace on server
						storage.deleteCurrentPage();

						monitor.worked(1);
						monitor.done();
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Replacing...", true);
			//reload page
			this.reloadCurrentDocument();
		} catch (Throwable e) {
			onError("Error replacing page image", e.getMessage(), e);
		}

	}

	public void replacePageImg() {
		logger.debug("Open Dialog for replacing image");
		if (!storage.isPageLoaded() || !storage.isRemoteDoc()) {
			DialogUtil.showErrorMessageBox(getShell(), "No document loaded", "No document loaded!");
			return;
		}

		//FIXME where to handle which file extensions are allowed?
		final String[] extArr = new String[] { "*.jpg", "*.jpeg", "*.tiff", "*.tif", "*.TIF", "*.TIFF", "*.png" };
		final String fn = DialogUtil.showOpenFileDialog(getShell(), "Select image file", null, extArr);
		if (fn == null)
			return;

		try {
			//check img file
			final File imgFile = new File(fn);
			if (!imgFile.canRead())
				throw new Exception("Can't read file at: " + fn);

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("Replacing file: " + fn);
						monitor.beginTask("Uploading image file", 120);
						//replace on server
						storage.replacePageImgFile(imgFile, monitor);

						for (int i = 1; i <= 2; i++) {
							Thread.sleep(1000);
							monitor.worked(100 + (i * 10));
						}
						monitor.done();
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Replacing...", true);
			//reload page
			this.reloadCurrentPage(false, null, null);
		} catch (Throwable e) {
			onError("Error replacing page image", e.getMessage(), e);
		}
	}

	/**
	 * FIXME <br/>
	 * this is one monster method!<br/>
	 * export-parameter-objects can be used instead of single parameters<br/>
	 * progress bar does not work after transcripts are loaded
	 * 
	 */
	public void unifiedExport() {
		File dir = null;
		String exportFileOrDir = "";
		String exportFormats = "";
		try {

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "No document loaded", "You first have to open a document that shall be exported!");
				return;
			}

			/*
			 * preselect document title for export folder name filter out all
			 * unwanted chars
			 */
			boolean isLocalDoc = storage.isLocalDoc();
			String title = isLocalDoc ? storage.getDoc().getMd().getLocalFolder().getName() : storage.getDoc().getMd().getTitle();
			String adjTitle = ExportUtils.getAdjustedDocTitle(title);

			saveTranscriptDialogOrAutosave();

			String lastExportFolderTmp = TrpGuiPrefs.getLastExportFolder();
			if (lastExportFolderTmp != null && !lastExportFolderTmp.equals("")) {
				lastExportFolder = lastExportFolderTmp;
			}
			
			storage.reloadDocWithAllTranscripts();
			

			
			final CommonExportDialog exportDiag = new CommonExportDialog(getShell(), SWT.NONE, lastExportFolder, adjTitle, storage.getDoc().getPages());
			dir = exportDiag.open();
			if (dir == null){
				return;
			}
			
			/*
			 * if we do not export the latest version
			 * -> reload the doc with all available transcripts to allow export of specific versions
			 * param -1
			 */
			// already done before...
//			if (!exportDiag.getVersionStatus().contains("Latest")){
//				storage.reloadDocWithAllTranscripts();
//			}
			
			String pages = exportDiag.getPagesStr();
			Set<Integer> pageIndices = exportDiag.getPageIndices();
			
			CommonExportPars commonPars = exportDiag.getCommonExportPars();
			TeiExportPars teiPars =  exportDiag.getTeiExportPars();
			PdfExportPars pdfPars = exportDiag.getPdfPars();
			DocxExportPars docxPars = exportDiag.getDocxPars();
			AltoExportPars altoPars = exportDiag.getAltoPars();

			if (exportDiag.isDoServerExport()) {
				String jobId = null;
				List<String> jobIds = new ArrayList<String>();
				List<TrpDocMetadata> docs = Storage.getInstance().getDocList(); 
				
				if (exportDiag.isExportCurrentDocOnServer()) {
					logger.debug("server export, collId = "+storage.getCollId()+", docId = "+storage.getDocId()+", commonPars = "+commonPars+", teiPars = "+teiPars+", pdfPars = "+pdfPars+", docxPars = "+docxPars+", altoPars = "+altoPars);
					jobId = storage.getConnection().exportDocument(storage.getCollId(), storage.getDocId(), 
												commonPars, altoPars, pdfPars, teiPars, docxPars);
					jobIds.add(jobId);
				} else {
					commonPars.setPages(null); // delete pagesStr for multiple document export!
					
					logger.debug("server collection export, collId = "+storage.getCollId()+" dsds = "+CoreUtils.toListString(exportDiag.getDocumentsToExportOnServer()));
					logger.debug("commonPars = "+commonPars+", teiPars = "+teiPars+", pdfPars = "+pdfPars+", docxPars = "+docxPars+", altoPars = "+altoPars);
					
					int pagesCounter = 0;
					List<DocumentSelectionDescriptor> currDescriptors = new ArrayList<DocumentSelectionDescriptor>();
					List<DocumentSelectionDescriptor> docDescriptors = exportDiag.getDocumentsToExportOnServer();
					
					/*
					 * if null the 'Choose documents...' was not opened and all doc descriptors must be set -> otherwise null and we have error
					 * null would work at the API but we want do allow 15.000 pager per export job maximum, so we have to count pages
					 */
					
					if (docDescriptors == null) {
						List<TrpDocMetadata> documents = Storage.getInstance().getDocList();
						docDescriptors = new ArrayList<>();
						for (TrpDocMetadata d : documents) {
							DocumentSelectionDescriptor dsd = new DocumentSelectionDescriptor(d.getDocId());
							docDescriptors.add(dsd);
						}
					}
									
					for (int i = 0; i < docDescriptors.size(); i++) {		
						DocumentSelectionDescriptor descriptor = docDescriptors.get(i);
						if (descriptor.getPages() != null && !descriptor.getPages().isEmpty()) {
							pagesCounter += descriptor.getPages().size();
						}
						else {
							int id = descriptor.getDocId();
							for (TrpDocMetadata doc : docs) {
								if (doc.getDocId() == id) {
									pagesCounter += doc.getNrOfPages();
									break;
								}
							}
						}

						logger.debug("pagesCounter " + pagesCounter);
						currDescriptors.add(descriptor);
						//start export every 15 000 pages?
						if(pagesCounter>15000 || (i+1)==docDescriptors.size()) {
							//teiPars are null if user wants the xslt export from Dario, set if it is the same TEI export at the server site as used at the 'Client export'
							jobId = storage.getConnection().exportDocuments(storage.getCollId(), currDescriptors, commonPars, altoPars, pdfPars, teiPars, docxPars);
							
							logger.debug("start new export job with " + pagesCounter + " pages!");
							jobIds.add(jobId);
							currDescriptors.clear();
							pagesCounter = 0;
						}
					}
				}

				if (!jobIds.isEmpty()) {
					logger.debug("started job(s) with id = "+jobIds);
								
//					mw.registerJobToUpdate(jobId); // do not register job as you get an email anyway...
					
					storage.sendJobListUpdateEvent();
					mw.updatePageLock();
					
					String titleMessage = (jobIds.size() == 1) ? "Export job started" : "Several export jobs started"; 
					String message = (jobIds.size() == 1) ? "Started export job with id = " : "Several export jobs started for this collection with ids = "; 
					
					DialogUtil.showInfoMessageBox(mw.getShell(), titleMessage, message+jobIds+"\n After completion, you will receive a download link via mail");
				}
				return;
			}
			
			logger.debug("after server export");

			if (!dir.exists()) {
				dir.mkdir();
			}
			
			exportFileOrDir = dir.getAbsolutePath();
			boolean doZipExport = false;

			boolean doMetsExport = false;
			boolean doPdfExport = false;
			boolean doDocxExport = false;
			boolean doTxtExport = false;
			/*
			 * tei export only available as server export because it is implemented as xslt transformation page -> tei
			 * New: client tei export available again as it was before and hence differs from the server export
			 */
			boolean doTeiExport = false;
			boolean doXlsxExport = false;
			boolean doTableExport = false;
			boolean doPageMdExport = false;

			String tempDir = null;

			String metsExportDirString = dir.getAbsolutePath() + "/" + dir.getName();
			File metsExportDir = new File(metsExportDirString);

			String pdfExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + ".pdf";
			File pdfExportFile = new File(pdfExportFileOrDir);

			String teiExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + "_tei.xml";
			File teiExportFile = new File(teiExportFileOrDir);

			String docxExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + ".docx";
			File docxExportFile = new File(docxExportFileOrDir);
			
			String txtExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + ".txt";
			File txtExportFile = new File(txtExportFileOrDir);

			String xlsxExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + ".xlsx";
			File xlsxExportFile = new File(xlsxExportFileOrDir);
			
			String tableExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + "_tables.xlsx";
			File tableExportFile = new File(tableExportFileOrDir);
			
			String pageMdExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + "_pageMd.xlsx";
			File pageMdExportFile = new File(pageMdExportFileOrDir);

			String zipExportFileOrDir = dir.getAbsolutePath() + "/" + dir.getName() + ".zip";
			File zipExportFile = new File(zipExportFileOrDir);

			/*
			 * only check export path if it is not ZIP because than we check just the ZIP location
			 */
			if (!exportDiag.isZipExport()) {
				doMetsExport = (exportDiag.isMetsExport() && exportDiag.getExportPathComp().checkExportFile(metsExportDir, null, getShell()));

				doPdfExport = (exportDiag.isPdfExport() && exportDiag.getExportPathComp().checkExportFile(pdfExportFile, ".pdf", getShell()));

				doTeiExport = (exportDiag.isTeiExport() && exportDiag.getExportPathComp().checkExportFile(teiExportFile, ".xml", getShell()));

				doDocxExport = (exportDiag.isDocxExport() && exportDiag.getExportPathComp().checkExportFile(docxExportFile, ".docx", getShell()));
				
				doTxtExport = (exportDiag.isTxtExport() && exportDiag.getExportPathComp().checkExportFile(txtExportFile, ".txt", getShell()));

				doXlsxExport = (exportDiag.isTagXlsxExport() && exportDiag.getExportPathComp().checkExportFile(xlsxExportFile, ".xlsx", getShell()));
				
				doTableExport = (exportDiag.isTableXlsxExport() && exportDiag.getExportPathComp().checkExportFile(tableExportFile, ".xlsx", getShell()));
				
				doPageMdExport = (exportDiag.isPageMdXlsxExport() && exportDiag.getExportPathComp().checkExportFile(pageMdExportFile, ".xlsx", getShell()));
			}

			doZipExport = (exportDiag.isZipExport() && exportDiag.getExportPathComp().checkExportFile(zipExportFile, ".zip", getShell()));

			if (doZipExport) {
				tempDir = System.getProperty("java.io.tmpdir");
				//logger.debug("temp dir is ..." + tempDir);
			}

			if (!doMetsExport && !doTeiExport && !doPdfExport && !doDocxExport && !doTxtExport && !doXlsxExport && !doZipExport && !doTableExport && !doPageMdExport) {
				/*
				 * if the export file exists and the user wants not to overwrite it then the 
				 * export dialog shows up again with the possibility to choose another location
				 * --> comment out if export should close instead
				 */
				unifiedExport();
				return;
			}

			if (exportDiag.isPageableExport() && pageIndices == null) {
				DialogUtil.showErrorMessageBox(getShell(), "Error parsing page ranges", "Error parsing page ranges");
				return;
			}

//			logger.debug("loading transcripts..." + copyOfPageIndices.size());
			
			ExportCache cache = new ExportCache();
			cache.setSelectedTagnames(exportDiag.getSelectedTagsList());
			
			if (!commonPars.exportImagesOnly()){

				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
					@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
	
							//logger.debug("loading transcripts...");
							monitor.beginTask("Loading transcripts...", pageIndices.size());						
						
							//unmarshal the page transcript only once for all different export, don't do this if only images are exported
							cache.storePageTranscripts4Export(storage.getDoc(), pageIndices, monitor, exportDiag.getVersionStatus(),
									storage.getPageIndex(), storage.getTranscript().getMd());
							
							monitor.done();
	
						} catch (Exception e) {
							throw new InvocationTargetException(e, e.getMessage());
						}
					}
				}, "Loading of transcripts: ", true);
	
				logger.debug("transcripts loaded");
				
				/*
				 * this way the pages we wish to export were updated -> not all but e.g. only GT pages; pageIndices was already updated during filling the cache
				 * we could remove the 'pageIndices' parameter in the methods later on
				 */
				commonPars.setPages(CoreUtils.getRangeListStrFromSet(pageIndices));
	
				if (exportDiag.isTagableExportChosen()) {	
	
					ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
						@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								//logger.debug("loading transcripts...");
								monitor.beginTask("Loading tags...", pageIndices.size());
								cache.storeCustomTagMapForDoc(storage.getDoc(), exportDiag.isWordBased(), pageIndices, monitor,
										exportDiag.isDoBlackening());
								
								/*
								 * new behavior: if null -> all tags in document will be exported
								 * we skip this from now on
								 */
//								if (cache.getSelectedTags() == null) {
//									DialogUtil.showErrorMessageBox(getShell(), "Error while reading selected tag names", "Error while reading selected tag names");
//									return;
//								}
								monitor.done();
							} catch (Exception e) {
								throw new InvocationTargetException(e, e.getMessage());
							}
						}
					}, "Loading of tags: ", true);
	
					logger.debug("tags loaded");
	
				}
			
			}

			boolean wordBased = exportDiag.isWordBased();
			boolean doBlackening = exportDiag.isDoBlackening();
			boolean createTitle = exportDiag.isCreateTitlePage();
			
			if (doZipExport) {

				if (tempDir == null)
					return;

				String tempZipDirParent = tempDir + "/" + dir.getName();
				File tempZipDirParentFile = new File(tempZipDirParent);

				if (tempZipDirParentFile.exists()) {
					Random randomGenerator = new Random();
					int randomInt = randomGenerator.nextInt(1000);
					tempZipDirParent = tempZipDirParent.concat(Integer.toString(randomInt));
					tempZipDirParentFile = new File(tempZipDirParent);
				}

				String tempZipDir = tempZipDirParent + "/" + dir.getName();
				File tempZipFileDir = new File(tempZipDir);
				FileUtils.forceMkdir(tempZipFileDir);

				if (exportDiag.isMetsExport())
					exportDocument(tempZipFileDir, commonPars, cache);
				if (exportDiag.isPdfExport())
					exportPdf(new File(tempZipDirParent + "/" + dir.getName() + ".pdf"), pageIndices, cache, commonPars, pdfPars);
				if (exportDiag.isTeiExport())
					exportTei(new File(tempZipDirParent + "/" + dir.getName() + ".xml"), exportDiag, cache);
				if (exportDiag.isDocxExport())
					exportDocx(new File(tempZipDirParent + "/" + dir.getName() + ".docx"), pageIndices, commonPars, docxPars, cache);
				if (exportDiag.isTxtExport()) {
					if (commonPars.isDoSplitTxt() && !StringUtils.containsWhitespace(commonPars.getStart_tagname_for_split()) && !StringUtils.containsWhitespace(commonPars.getEnd_tagname_for_split())) {
						exportTxt(new File(tempZipDirParent + "/textfiles/"), pageIndices, createTitle, wordBased, true, cache, commonPars);
					}
					else {
						exportTxt(new File(tempZipDirParent + "/" + dir.getName() + ".txt"), pageIndices, createTitle, wordBased, true, cache, commonPars);
					}
				}
				if (exportDiag.isTagXlsxExport())
					exportXlsx(new File(tempZipDirParent + "/" + dir.getName() + ".xlsx"), pageIndices, exportDiag.isWordBased(), exportDiag.isDocxTagExport(), cache);
				if (exportDiag.isPageMdXlsxExport())
					exportXlsx(new File(tempZipDirParent + "/" + dir.getName() + ".xlsx"), pageIndices, exportDiag.isWordBased(), exportDiag.isDocxTagExport(), cache);
				if (exportDiag.isTableXlsxExport())
					exportTableXlsx(new File(tempZipDirParent + "/" + dir.getName() + "_tables.xlsx"), pageIndices, cache);

				//createZipFromFolder(tempZipDirParentFile.getAbsolutePath(), dir.getParentFile().getAbsolutePath() + "/" + dir.getName() + ".zip");
				ZipUtils.createZipFromFolder(tempZipDirParentFile.getAbsolutePath(), zipExportFile.getAbsolutePath(), false);

				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "ZIP";

				for (File f : tempZipDirParentFile.listFiles()) {
					f.delete();
				}

				lastExportFolder = dir.getParentFile().getAbsolutePath();
				logger.debug("last export folder: " + lastExportFolder);

				TrpGuiPrefs.storeLastExportFolder(lastExportFolder);

				//delete the temp folder for making the ZIP
				FileDeleteStrategy.FORCE.delete(tempZipDirParentFile);

				if (exportFormats != "") {
					displaySuccessMessage("Sucessfully written " + exportFormats + " to " + exportFileOrDir);
				}

				//export was done via ZIP and is completed now
				return;

			}

			if (doMetsExport) {

				//exportDocument(metsExportDir, pageIndices, exportDiag.isImgExport(), exportDiag.isPageExport(), exportDiag.isAltoExport(), exportDiag.isSplitUpWords(), exportDiag.isWordBased(), commonPars.getFileNamePattern(), commonPars.getRemoteImgQuality(), cache);
				exportDocument(metsExportDir, commonPars, cache);
				if (exportDiag.isPageExport()) {
					if (exportFormats != "") {
						exportFormats += " and ";
					}
					exportFormats += "METS/PAGE";
				}

				if (exportDiag.isAltoExport()) {
					if (exportFormats != "") {
						exportFormats += " and ";
					}
					exportFormats += "METS/ALTO";
				}
				
				if (exportDiag.isImgExport()){
					if (exportFormats != "") {
						exportFormats += " and ";
					}
					exportFormats += "IMAGES";
				}

			}

			if (doPdfExport) {

				exportPdf(pdfExportFile, pageIndices, cache, commonPars, pdfPars);
				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "PDF";

			}

			if (doTeiExport) {

				exportTei(teiExportFile, exportDiag, cache);
				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "TEI";

			}

			if (doDocxExport) {

//				exportDocx(docxExportFile, pageIndices, wordBased, exportDiag.isDocxTagExport(), doBlackening, createTitle,
//						exportDiag.isMarkUnclearWords(), exportDiag.isExpandAbbrevs(), exportDiag.isSubstituteAbbreviations(),
//						exportDiag.isPreserveLinebreaks(), exportDiag.isForcePagebreaks(), exportDiag.isShowSuppliedWithBrackets(), 
//						exportDiag.isIgnoreSupplied(), cache);
				exportDocx(docxExportFile, pageIndices, commonPars, docxPars, cache);
				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "DOCX";

			}
			
			if (doTxtExport) {
				if (commonPars.isDoSplitTxt() && !StringUtils.containsWhitespace(commonPars.getStart_tagname_for_split()) && !StringUtils.containsWhitespace(commonPars.getEnd_tagname_for_split())) {
					exportTxt(new File(dir.getAbsolutePath() + "/textfiles/"), pageIndices, createTitle, wordBased, true, cache, commonPars);
				}
				else {
					//last param keeps the line breaks by default 
					exportTxt(txtExportFile, pageIndices, createTitle, wordBased, true, cache, commonPars);
				}

				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "TXT";

			}

			if (doXlsxExport) {

				if (exportXlsx(xlsxExportFile, pageIndices, exportDiag.isWordBased(), exportDiag.isDocxTagExport(), cache)){
					if (exportFormats != "") {
						exportFormats += " and ";
					}
					exportFormats += "XLSX";
				}

			}
			
			if (doTableExport) {

				exportTableXlsx(tableExportFile, pageIndices, cache);
				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "TABLES_XLSX";

			}
			
			if (doPageMdExport) {
				
				exportPageMdXlsx(pageMdExportFile, pageIndices, cache);
				if (exportFormats != "") {
					exportFormats += " and ";
				}
				exportFormats += "PAGEMD_XLSX";
			}



		} catch (Throwable e) {
			if (e instanceof InterruptedException) {
				DialogUtil.showInfoMessageBox(getShell(), "Export canceled", "Export was canceled");
			}
			else if (e instanceof SessionExpiredException) {
				sessionExpired = true;
				logout(true, false);
				logger.warn("Session expired!");
				loginDialog("Session expired!");
				//unifiedExport();
			} else {
				onError("Export error", e.getMessage(), e);
			}
		} finally {
			
			if (exportFormats != "") {
				displaySuccessMessage("Sucessfully written " + exportFormats + " to " + exportFileOrDir);
			}
						
			if (dir != null) {
				lastExportFolder = dir.getParentFile().getAbsolutePath();
				TrpGuiPrefs.storeLastExportFolder(lastExportFolder);
			}
		}

	}
	
	public void exportDocument(final File dir, CommonExportPars commonPars, ExportCache cache) throws Throwable {
		try {

			if (dir == null)
				return;

			String what = "Images" + (commonPars.isDoExportPageXml() ? ", PAGE" : "") + (commonPars.isDoExportAltoXml() ? ", ALTO" : "");
			lastExportFolder = dir.getParentFile().getAbsolutePath();
			commonPars.setDir(dir.getAbsolutePath());
			commonPars.setUseOcrMasterDir(false);
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("exporting document...");
						final String path = storage.exportDocument(commonPars, monitor, cache);
						monitor.done();
						// displaySuccessMessage("Written export to "+path);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting document files: " + what, false);
		} catch (Throwable e) {
			onError("Export error", "Error during export of document", e);
			throw e;
		}
	}

	/*
	 * use the commonPars instead
	 */
	@Deprecated
	public void exportDocument(final File dir, final Set<Integer> pageIndices, final boolean exportImg, final boolean exportPage, final boolean exportAlto,
			final boolean splitIntoWordsInAlto, final boolean useWordLayer, final String fileNamePattern, final ImgType imgType, ExportCache cache) throws Throwable {
		try {

			if (dir == null)
				return;

			String what = "Images" + (exportPage ? ", PAGE" : "") + (exportAlto ? ", ALTO" : "");
			lastExportFolder = dir.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("exporting document...");
						final String path = storage.exportDocument(dir, pageIndices, exportImg, exportPage, exportAlto, splitIntoWordsInAlto, useWordLayer, fileNamePattern,
								imgType, monitor, cache);
						monitor.done();
						// displaySuccessMessage("Written export to "+path);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting document files: " + what, false);
		} catch (Throwable e) {
			onError("Export error", "Error during export of document", e);
			throw e;
		}
	}

	// public void exportRtf() {
	// try {
	// if (!storage.isDocLoaded()) {
	// DialogUtil.showErrorMessageBox(getShell(), "No document loaded",
	// "You first have to open a document!");
	// return;
	// }
	//
	// String adjTitle = getAdjustedDocTitle();
	//
	// logger.debug("lastExportRtfFn = "+lastExportRtfFn);
	// final RtfExportDialog exportDiag = new RtfExportDialog(
	// getShell(), SWT.NONE, lastExportRtfFn, storage.getDoc().getNPages(),
	// adjTitle
	// );
	// final File file = exportDiag.open();
	// if (file == null)
	// return;
	// final Integer startPage = exportDiag.getStartPage();
	// final Integer endPage = exportDiag.getEndPage();
	// final boolean isWordBased = exportDiag.isWordBased();
	//
	// logger.info("PDF export. pages " + startPage + "-" +
	// endPage+", isWordBased: "+isWordBased);
	//
	// lastExportRtfFn = file.getAbsolutePath();
	// ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
	// @Override public void run(IProgressMonitor monitor) throws
	// InvocationTargetException, InterruptedException {
	// try {
	// logger.debug("creating RTF document...");
	// TrpRtfBuilder.writeRtfForDoc(storage.getDoc(), isWordBased, file,
	// startPage, endPage, monitor);
	// monitor.done();
	// displaySuccessMessage("Written RTF file to "+lastExportRtfFn);
	// } catch (Exception e) {
	// throw new InvocationTargetException(e, e.getMessage());
	// }
	// }
	// }, "Exporting", true);
	// } catch (Throwable e) {
	// onError("Export error", "Error during RTF export of document", e);
	// }
	// }

	public void exportRtf(final File file, final Set<Integer> pageIndices, final boolean isWordBased, final boolean isTagExport, final boolean doBlackening,
			ExportCache cache) throws Throwable {
		try {

			if (file == null)
				return;

			logger.info("RTF export. pages " + pageIndices + ", isWordBased: " + isWordBased);

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating RTF document...");
						TrpRtfBuilder.writeRtfForDoc(storage.getDoc(), isWordBased, isTagExport, doBlackening, file, pageIndices, monitor, cache);
						monitor.done();
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
			onError("Export error", "Error during RTF export of document", e);
			throw e;
		}
	}

	public void exportTxt(final File file, final Set<Integer> pageIndices, final boolean createTitle, final boolean isWordBased, final boolean preserveLineBreaks, ExportCache cache, CommonExportPars commonPars) throws Throwable {
		try {

			if (file == null)
				return;

			logger.info("Txt export. pages " + pageIndices + ", isWordBased: " + isWordBased);

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating txt file...");
						TrpTxtBuilder txtBuilder = new TrpTxtBuilder();
						logger.debug("start tag: " + commonPars.getStart_tagname_for_split());
						if (commonPars.isDoSplitTxt() && !StringUtils.containsWhitespace(commonPars.getStart_tagname_for_split()) && !StringUtils.containsWhitespace(commonPars.getEnd_tagname_for_split())) {
							txtBuilder.writeTxtForCustomTags(storage.getDoc(), isWordBased, preserveLineBreaks, file, pageIndices, monitor, cache, commonPars.getStart_tagname_for_split(), commonPars.getEnd_tagname_for_split(), commonPars.getAttributes_for_split_name(), commonPars.getTags_to_ignore_for_split());
						}
						else {
							txtBuilder.writeTxtForDoc(storage.getDoc(), createTitle, isWordBased, preserveLineBreaks, file, pageIndices, monitor, cache);
						}
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
			if (!(e instanceof InterruptedException)) {
				onError("Export error", "Error during Txt export of document", e);
			}
			throw e;
		}
	}
	
	/*
	 * old: 	public void exportDocx(final File file, final Set<Integer> pageIndices, final boolean isWordBased, final boolean isTagExport, final boolean doBlackening,
			final boolean createTitle, final boolean markUnclearWords, final boolean expandAbbrevs,
			final boolean substituteAbbrevs, final boolean preserveLineBreaks, final boolean forcePageBreaks, final boolean suppliedWithBrackets, final boolean ignoreSupplied, ExportCache cache) throws Throwable {
			new: use the common parameters and docx parameters class
	 */
	public void exportDocx(final File file, final Set<Integer> pageIndices, CommonExportPars commonPars, DocxExportPars docxPars, ExportCache cache) throws Throwable {
		try {

			if (file == null)
				return;

			logger.info("Docx export. pages " + pageIndices + ", isWordBased: " + commonPars.isWriteTextOnWordLevel());

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating Docx document...");
						DocxBuilder docxBuilder = new DocxBuilder();
						docxBuilder.writeDocxForDoc(storage.getDoc(), file, pageIndices, monitor, cache, commonPars, docxPars);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
			if (!(e instanceof InterruptedException)) {
				onError("Export error", "Error during Docx export of document", e);
			}
			throw e;
		}
	}

	public boolean exportXlsx(final File file, final Set<Integer> pageIndices, final boolean isWordBased, final boolean isTagExport, ExportCache cache) throws Throwable {
		try {

			if (cache.getCustomTagMapForDoc().isEmpty()) {
				logger.info("No tags to store -> Xlsx export cancelled");
				displayCancelMessage("No custom tags in document to store -> Xlsx export cancelled");
				return false;
				//throw new Exception("No tags to store -> Xlsx export cancelled");
			}
			
			logger.debug("tags " + cache.getCustomTagMapForDoc().size());
			
			//logger.debug("lastExportXlsxFn = " + lastExportXlsxFn);

			if (file == null)
				return false;

			logger.info("Excel export. pages " + pageIndices + ", isWordBased: " + isWordBased);

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating Excel document...");
						TrpXlsxBuilder xlsxBuilder = new TrpXlsxBuilder();
						xlsxBuilder.writeXlsxForDoc(storage.getDoc(), isWordBased, file, pageIndices, monitor, cache);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
//			if (!(e instanceof InterruptedException))
//				onError("Export error", "Error during Xlsx export of document", e);
			throw e;
		}
		return true;
	}
	
	public void exportTableXlsx(final File file, final Set<Integer> pageIndices, ExportCache cache) throws Throwable {
		try {

			if (file == null)
				return;

			logger.info("Excel table export. pages " + pageIndices);

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating Excel document...");
						TrpXlsxTableBuilder xlsxTableBuilder = new TrpXlsxTableBuilder();
						xlsxTableBuilder.writeXlsxForTables(storage.getDoc(), file, pageIndices, monitor, cache);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
//			if (!(e instanceof InterruptedException))
//				onError("Export error", "Error during Xlsx export of document", e);
			throw e;
		}
	}
	
	public void exportPageMdXlsx(final File file, final Set<Integer> pageIndices, ExportCache cache) throws Throwable {
		try {

			if (file == null)
				return;

			logger.info("Excel page related md export. pages " + pageIndices);

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating Excel document...");
						TrpXlsxBuilder xlsxBuilder = new TrpXlsxBuilder();
						xlsxBuilder.writeXlsxForPageRelatedMetadata(storage.getDoc(), file, pageIndices, monitor, cache);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
//			if (!(e instanceof InterruptedException))
//				onError("Export error", "Error during Xlsx export of document", e);
			throw e;
		}
	}

	// public void exportPdf() {
	// try {
	// if (!storage.isDocLoaded()) {
	// DialogUtil.showErrorMessageBox(getShell(), "No document loaded",
	// "You first have to open a document that shall be exported as PDF!");
	// return;
	// }
	//
	// String adjTitle = getAdjustedDocTitle();
	//
	// final PdfExportDialog exportDiag = new PdfExportDialog(
	// getShell(), SWT.NONE, lastExportPdfFn, storage.getDoc().getNPages(),
	// adjTitle
	// );
	// final File dir = exportDiag.open();
	// if (dir == null)
	// return;
	// final Integer startPage = exportDiag.getStartPage();
	// final Integer endPage = exportDiag.getEndPage();
	//
	// logger.info("PDF export. pages " + startPage + "-" + endPage);
	//
	// lastExportPdfFn = dir.getAbsolutePath();
	// ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
	// @Override public void run(IProgressMonitor monitor) throws
	// InvocationTargetException, InterruptedException {
	// try {
	// logger.debug("creating PDF document...");
	// int totalWork = endPage+1 - startPage;
	// monitor.beginTask("Creating PDF document" , totalWork);
	//
	// final String path = storage.exportPdf(dir, startPage, endPage, monitor);
	// monitor.done();
	// displaySuccessMessage("Written PDF file to "+path);
	// } catch (Exception e) {
	// throw new InvocationTargetException(e, e.getMessage());
	// }
	// }
	// }, "Exporting", false);
	// } catch (Throwable e) {
	// onError("Export error", "Error during export of document", e);
	// }
	// }
	
	public void exportPdf(final File dir, final Set<Integer> pageIndices, ExportCache cache, final CommonExportPars commonPars, final PdfExportPars pars)
			throws Throwable {
		try {
			if (dir == null)
				return;

			// logger.info("PDF export. pages " + startPage + "-" + endPage);
			// logger.info("PDF dir " + dir.getAbsolutePath());
			// logger.info("PDF parent dir " +
			// dir.getParentFile().getAbsolutePath());

			lastExportFolder = dir.getParentFile().getAbsolutePath();
			final Shell shell = getShell();
			ProgressBarDialog.open(shell, new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						storage.exportPdf(dir, pageIndices, monitor, cache, commonPars, pars);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
			if (!(e instanceof InterruptedException))
				onError("Export error", "Error during export of document", e);
			throw e;
		}
	}

	//unused
	public void exportPdf(final File dir, final Set<Integer> pageIndices, final boolean extraTextPages, final boolean imagesOnly,
			final boolean highlightTags, final boolean highlightArticles, final boolean wordBased, final boolean doBlackening, final boolean createTitle, ExportCache cache, final String exportFontname, final ImgType imgType)
			throws Throwable {
		try {
			if (dir == null)
				return;

			// logger.info("PDF export. pages " + startPage + "-" + endPage);
			// logger.info("PDF dir " + dir.getAbsolutePath());
			// logger.info("PDF parent dir " +
			// dir.getParentFile().getAbsolutePath());

			lastExportFolder = dir.getParentFile().getAbsolutePath();
			final Shell shell = getShell();
			ProgressBarDialog.open(shell, new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						storage.exportPdf(dir, pageIndices, monitor, extraTextPages, imagesOnly, highlightTags, highlightArticles, wordBased, doBlackening, createTitle, cache, exportFontname, imgType);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
			if (!(e instanceof InterruptedException))
				onError("Export error", "Error during export of document", e);
			throw e;
		}
	}

	// public void exportTei() {
	// try {
	// if (!storage.isDocLoaded()) {
	// DialogUtil.showErrorMessageBox(getShell(), "No document loaded",
	// "You first have to open a document that shall be exported as PDF!");
	// return;
	// }
	//
	// String adjTitle = getAdjustedDocTitle();
	//
	// final TeiExportDialog exportDiag = new TeiExportDialog(
	// getShell(), SWT.NONE, lastExportTeiFn, storage.getDoc().getNPages(),
	// adjTitle
	// );
	// final File dir = exportDiag.open();
	// if (dir == null)
	// return;
	// final int mode = exportDiag.getMode();
	// logger.info("TEI export. Mode = " + mode);
	//
	// lastExportTeiFn = dir.getAbsolutePath();
	// ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
	// @Override public void run(IProgressMonitor monitor) throws
	// InvocationTargetException, InterruptedException {
	// try {
	// logger.debug("creating TEI document...");
	// monitor.beginTask("Creating TEI document" , IProgressMonitor.UNKNOWN);
	//
	// final String path = storage.exportTei(dir, mode);
	// monitor.done();
	// displaySuccessMessage("Written TEI file to "+path);
	// } catch (Exception e) {
	// throw new InvocationTargetException(e, e.getMessage());
	// }
	// }
	// }, "Exporting", false);
	// } catch (Throwable e) {
	// onError("Export error", "Error during export of document", e);
	// }
	// }

	public void exportTei(final File file, final CommonExportDialog exportDiag, ExportCache cache) throws Throwable {
		try {
			TeiExportPars pars = exportDiag.getTeiExportPars();
			CommonExportPars commonPars = exportDiag.getCommonExportPars();

			if (file == null)
				return;

			logger.info("TEI export, pars = "+pars+" commonPars = "+commonPars);

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating TEI document, pars: " + pars);

						storage.exportTei(file, commonPars, pars, monitor, cache);
						monitor.done();
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", true);
		} catch (Throwable e) {
			if (!(e instanceof InterruptedException))
				onError("Export error", "Error during export of TEI", e);
			throw e;
		}
	}

	public void exportAlto(final File file) throws Throwable {
		try {

			if (file == null)
				return;

			logger.info("Alto export.");

			lastExportFolder = file.getParentFile().getAbsolutePath();
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("creating Alto document...");
						monitor.beginTask("Creating Alto document", IProgressMonitor.UNKNOWN);

						storage.exportAlto(file, monitor);
						monitor.done();
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Exporting", false);
		} catch (Throwable e) {
			onError("Export error", "Error during export of Alto", e);
			throw e;
		}
	}

	public void sendBugReport() {
		try {

			if(bugDialog == null){
				bugDialog = new BugDialog(getShell(), SWT.NONE);
				bugDialog.open();
			}else{
				bugDialog.setActive();
			}
			

		} catch (Throwable e) {
			onError("Fatal bug report error", "Fatal error sending bug report / feature request", e);
		}
	}

	public void selectTranscriptionWidgetOnSelectedShape(ICanvasShape selected) {
		if (selected == null || selected.getData() == null)
			return;

		if (selected.getData() instanceof TrpWordType) {
			ui.changeToTranscriptionWidget(TranscriptionLevel.WORD_BASED);
		} else if (selected.getData() instanceof TrpTextLineType || selected.getData() instanceof TrpBaselineType) {
			ui.changeToTranscriptionWidget(TranscriptionLevel.LINE_BASED);
		}
	}

	public void checkForUpdates() {
		ProgramUpdaterDialog.checkForUpdatesDialog(ui.getShell(), VERSION, info.getTimestamp(), false, false);
	}

	public void notifyOnRequiredUpdate(final String errorMessage) {
		final String msg = errorMessage + "\n" +
				"Please update if you want to use the server-based tools.";
		showUpdateDialog("Update", msg);
	}
	
	public void installSpecificVersion() {
		showUpdateDialog(null, null);
	}
	
	public void showUpdateDialog(final String title, final String customMessage) {
		if(updateDialog != null && !updateDialog.isDisposed()) {
			updateDialog.closeDialog();
			updateDialog = null;
		}
		updateDialog = new InstallSpecificVersionDialog(ui.getShell(), title, customMessage, SWT.NONE);
		int answer = updateDialog.open();
		if (answer == 0 || answer == 1) {
			ProgramPackageFile f = updateDialog.getSelectedFile();
			boolean downloadAll = updateDialog.isDownloadAll();
			logger.debug("installing selected file: " + f + " downloadAll: " + downloadAll);
			if (f == null)
				return;

			boolean keepConfigFiles = answer == 0;
			boolean isNewVersion = !f.getVersion().equals(VERSION);

			try {
				ProgramUpdaterDialog.downloadAndInstall(ui.getShell(), f, isNewVersion, keepConfigFiles, downloadAll);
			} catch (InterruptedException ie) {
				logger.debug("Interrupted: " + ie.getMessage());
			} catch (IOException e) {
				if (!e.getMessage().equals("stream is closed")) {
					TrpMainWidget.getInstance().onError("IO-Error during update", "Error during update: \n\n" + e.getMessage(), e);
				} else {
					logger.error("Program update could not be downloaded or installed.", e);
				}
			} catch (Throwable e) {
				TrpMainWidget.getInstance().onError("Error during update", "Error during update: \n\n" + e.getMessage(), e);
			} finally {
				if (!ProgramUpdaterDialog.TEST_ONLY_DOWNLOAD) {
					ProgramUpdaterDialog.removeUpdateZip();
				}
				updateDialog = null;
			}
		}
	}

//	public void analyzePageStructure(final boolean detectPageNumbers, final boolean detectRunningTitles, final boolean detectFootnotes) {
//		try {
//			if (!storage.isPageLoaded()) {
//				DialogUtil.showErrorMessageBox(getShell(), "Analyze Page Structure", "No page loaded!");
//				return;
//			}
//			if (!storage.isLoggedIn()) {
//				DialogUtil.showErrorMessageBox(getShell(), "Analyze Page Structure", "You are not logged in!");
//				return;
//			}
//
//			final int colId = storage.getCurrentDocumentCollectionId();
//			final int docId = storage.getDoc().getId();
//			final int pageNr = storage.getPage().getPageNr();
//
//			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
//				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					try {
//						logger.debug("analyzing structure...");
//						monitor.beginTask("Analyzing structure", IProgressMonitor.UNKNOWN);
//						storage.analyzeStructure(colId, docId, pageNr, detectPageNumbers, detectRunningTitles, detectFootnotes);
//						monitor.done();
//						// displaySuccessMessage("Written TEI file to "+path);
//					} catch (Exception e) {
//						throw new InvocationTargetException(e, e.getMessage());
//					}
//				}
//			}, "Exporting", false);
//
//			reloadCurrentTranscript(true, true, () -> {
//				storage.setCurrentTranscriptEdited(true);
////				ui.selectStructureTab();
//				updatePageInfo();				
//			}, null);
//		} catch (Throwable e) {
//			onError("Analyze Page Structure", e.getMessage(), e);
//		}
//	}
	
	public void updateParentRelationshipAccordingToGeometricOverlap() {
		if (!storage.hasTranscript())
			return;

		try {
			logger.debug("updating parent relationship according to geometric overlap");
			IStructuredSelection sel = (IStructuredSelection) ui.getStructureTreeViewer().getSelection();
			Iterator<?> it = sel.iterator();
			
			int cTotal=0;
			while (it.hasNext()) {
				Object o = it.next();
				int c=0;
				
				if (o instanceof TrpPageType) {
					TrpPageType page = (TrpPageType) o;
					c = CanvasShapeUtil.assignToShapesGeometrically(page.getTextRegions(false), page.getLines());
					if (c > 0) {
						TrpShapeTypeUtils.applyReadingOrderFromCoordinates(page.getTextRegionOrImageRegionOrLineDrawingRegion(), false, true, false);
					}
				} else if (o instanceof TrpTextRegionType) {
					TrpTextRegionType textRegion = (TrpTextRegionType) o;
					c = CanvasShapeUtil.assignToParentIfOverlapping(textRegion, textRegion.getPage().getLines(), 0.9d);
					if (c > 0) {
						TrpShapeTypeUtils.applyReadingOrderFromCoordinates(textRegion.getTrpTextLine(), false, false, false);
					}
				} else if (o instanceof TrpTextLineType) {
					TrpTextLineType textLine = (TrpTextLineType) o;
					c = CanvasShapeUtil.assignToParentIfOverlapping(textLine, textLine.getPage().getLines(), 0.9d);
					if (c > 0) {
						TrpShapeTypeUtils.applyReadingOrderFromCoordinates(textLine.getTrpWord(), false, false, false);
					}
				}
				// TODO: tables???? --> most probably not relevant for this functionality...
				cTotal += c;
			}
			logger.debug("reassigned nr of shapes: "+cTotal);
			
			if (cTotal > 0) {
				JAXBPageTranscript tr = storage.getTranscript();
				if (tr != null) {
					tr.getPage().sortContent();
				}
				ui.getStructureTreeViewer().refresh();
			}
		} catch (Throwable e) {
			onError("Error updating parent relationship", e.getMessage(), e);
		}
	}

	public void updateReadingOrderAccordingToCoordinates(boolean deleteReadingOrder, boolean recursive) {
		if (!storage.hasTranscript())
			return;

		logger.debug("applying reading order according to coordinates");
		IStructuredSelection sel = (IStructuredSelection) ui.getStructureTreeViewer().getSelection();
		Iterator<?> it = sel.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof TrpPageType) {
				TrpShapeTypeUtils.applyReadingOrderFromCoordinates(((TrpPageType) o).getTextRegionOrImageRegionOrLineDrawingRegion(), false,
						true, recursive);
			} else if (o instanceof TrpTextRegionType) {
				TrpShapeTypeUtils.applyReadingOrderFromCoordinates(((TrpTextRegionType) o).getTrpTextLine(), false, deleteReadingOrder, recursive);
			} else if (o instanceof TrpTextLineType) {
				TrpShapeTypeUtils.applyReadingOrderFromCoordinates(((TrpTextLineType) o).getTrpWord(), false, deleteReadingOrder, recursive);
			} else if (o instanceof TrpTableRegionType) {
				TrpShapeTypeUtils.applyReadingOrderFromCoordinates(((TrpTableRegionType) o).getTrpTableCell(), false, true, recursive);
			}
		}

		JAXBPageTranscript tr = storage.getTranscript();

		tr.getPage().sortContent();
		ui.getStructureTreeViewer().refresh();
	}
	
	public void updateReadingOrderAccordingToCoordinates_YOverlap() {
		if (!storage.hasTranscript())
			return;

		logger.debug("applying reading order according to coordinates");
		IStructuredSelection sel = (IStructuredSelection) ui.getStructureTreeViewer().getSelection();
		Iterator<?> it = sel.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof TrpPageType) {
				TrpShapeTypeUtils.sortShapesByCoordinates_XIfYOverlaps_OtherwiseY(((TrpPageType) o).getTextRegionOrImageRegionOrLineDrawingRegion(),true);
			} else if (o instanceof TrpTextRegionType) {
				TrpShapeTypeUtils.sortShapesByCoordinates_XIfYOverlaps_OtherwiseY(((TrpTextRegionType) o).getTrpTextLine(),true);
			} else if (o instanceof TrpTextLineType) {
				TrpShapeTypeUtils.sortShapesByCoordinates_XIfYOverlaps_OtherwiseY(((TrpTextLineType) o).getTrpWord(),true);
			} else if (o instanceof TrpTableRegionType) {
				TrpShapeTypeUtils.sortShapesByCoordinates_XIfYOverlaps_OtherwiseY(((TrpTableRegionType) o).getTrpTableCell(),true);
			}
		}

		JAXBPageTranscript tr = storage.getTranscript();

		tr.getPage().sortContent();
		ui.getStructureTreeViewer().refresh();
	}

	public void reloadCollections() {
		try {
			logger.debug("reloading collections!");
			if (!storage.isLoggedIn()) {
				// DialogUtil.showErrorMessageBox(getShell(), "Not logged in",
				// "You have to log in to reload the collections list");
				return;
			}

			storage.reloadCollections();
		} catch (Throwable e) {
			onError("Error", "Error reload of collections: " + e.getMessage(), e);
		}
	}

	public String getSelectedImageFileType() {
		String fileType = "view";
		MenuItem mi = getCanvasWidget().getToolbar().getImageVersionDropdown().getSelected();
		if (mi != null) {
			fileType = (String) mi.getData("data");
		}
		
		return fileType;
	}

	private boolean checkExportFile(File file, String extension) {

		String fTxt = file.getAbsolutePath();
		if (extension != null && !fTxt.toLowerCase().endsWith(extension)) {
			fTxt = fTxt + extension;
			file = new File(fTxt);
		}

		if (!file.getParentFile().exists()) {
			DialogUtil.showErrorMessageBox(getShell(), "Error trying to export",
					"The export destination folder does not exist - select an existing base folder!");
			return false;
		}

		if (file.exists() && extension != null) {
			int a = DialogUtil.showYesNoDialog(getShell(), "File exists", "The specified file " + file.getAbsolutePath() + " exists - overwrite?");
			if (a == SWT.YES)
				return true;
			else
				return false;
		} else if (file.exists()) {
			int a = DialogUtil.showYesNoDialog(getShell(), "Folder exists", "The specified document folder " + file.getAbsolutePath() + " exists - overwrite?");
			FileUtils.deleteQuietly(file);
			if (a == SWT.YES)
				return true;
			else
				return false;
		}

		return true;

	}

	public void loadLocalPageXmlFile() {
		if (!storage.isPageLoaded()) {
			onError("No page loaded", "No page is loaded currently!", null);
			return;
		}

		logger.debug("loading a local page xml file...");
		String fn = DialogUtil.showOpenFileDialog(getShell(), "Select xml file to load", null, new String[] { "*.xml" });
		if (fn == null){
			logger.debug("fn is null");
			return;
		}

		try {
			PcGtsType p = PageXmlUtils.unmarshal(new File(fn));
			storage.getTranscript().setPageData(p);
			storage.setCurrentTranscriptEdited(true);
			//saveTranscription(false);
			reloadCurrentTranscript(true, true, null, null);
		} catch (Exception e) {
			onError("Error loading page XML", e.getMessage(), e);
		}
	}

	public void openSearchDialog() {
		checkSession(true);
		if (!storage.isLoggedIn()) {
			logger.debug("not logged in!");
			return;
		}
		
		if (searchDiag != null) {
			if(searchDiag.getShell() != null ){

				if(searchDiag.getShell().getMinimized()){
					searchDiag.getShell().setMinimized(false);
					searchDiag.getShell().forceActive();
				}else{
					searchDiag.getShell().forceActive();
				}
			}else{
				searchDiag.open();
			}
		} 
		else{		
			searchDiag = new SearchDialog(getShell());
			searchDiag.open();
		}
		searchDiag.selectTab(SearchType.FULLTEXT);
	}
	
	public void openDocumentManager() {
		checkSession(true);
		if (!storage.isLoggedIn()) {
			return;
		}
		
		if (docManDiag!=null && !SWTUtil.isDisposed(docManDiag.getShell())) {
			docManDiag.getShell().setVisible(true);
			SWTUtil.centerShell(docManDiag.getShell());
		} else {
			docManDiag = new DocumentManager(getShell());
			docManDiag.open();
		}
	}
	
	public void reloadDocumentManager() {
		if (docManDiag != null && !SWTUtil.isDisposed(docManDiag.getShell()) && docManDiag.getShell().isVisible()){
			docManDiag.totalReload(ui.getServerWidget().getSelectedCollectionId());
		}		
	}
	
	public void closeDocumentManager() {
		if (docManDiag != null && !SWTUtil.isDisposed(docManDiag.getShell()) && docManDiag.getShell().isVisible()) {
			docManDiag.getShell().close();
		}
	}
	
	public SearchDialog getSearchDialog(){
		return searchDiag;
	}


//	//update visibility of reading order
//	public void updateReadingOrderVisibility() {
//
//		for (ICanvasShape s : getScene().getShapes()) {
//
//			if (s.hasDataType(TrpTextRegionType.class)) {
//				s.showReadingOrder(getTrpSets().isShowReadingOrderRegions());
//			}
//			if (s.hasDataType(TrpTextLineType.class)) {
//				s.showReadingOrder(getTrpSets().isShowReadingOrderLines());
//			}
//			if (s.hasDataType(TrpWordType.class)) {
//				s.showReadingOrder(getTrpSets().isShowReadingOrderWords());
//			}
//		}
//	}

	public void showEventMessages() {
		try {
			List<TrpEvent> events = storage.getEvents();
			for (TrpEvent ev : events) {
				final String msg = CoreUtils.newDateFormatUserFriendly().format(ev.getDate()) + ": " + ev.getTitle() + "\n\n" + ev.getMessage();
				MessageDialogWithToggle eventDialog = new MessageDialogStyledWithToggle(getShell(), "Notification", null, msg, MessageDialog.INFORMATION, 
						new String[] { "OK" }, 0, "Do not show this message again", false);
				int ret = eventDialog.open();
				logger.debug("User choice was button: {}", ret);
				boolean doNotShowAgain = eventDialog.getToggleState();
				logger.debug("Do not show again = " + doNotShowAgain);
				if (doNotShowAgain) {
					storage.markEventAsRead(ev.getId());
				}
			}
		} catch (IOException ioe) {
			logger.info("Could not write events.txt file!", ioe);
		} catch (Exception e) {
			logger.info("Could not load events from server!", e);
		}
	}

	public void setLocale(Locale l) {
		logger.debug("setting new locale: " + l);

		if (l == null || Msgs.getLocale().equals(l))
			return;

		Msgs.setLocale(l);
		TrpConfig.getTrpSettings().setLocale(l);
//		TrpConfig.save(TrpSettings.LOCALE_PROPERTY);

		DialogUtil.showInfoMessageBox(ui.getShell(), Msgs.get2("language_changed") + ": " + l.getDisplayName(), Msgs.get2("restart"));
	}

	public void batchReplaceImagesForDoc() {
		try {
			logger.debug("batch replacing images!");

			if (!storage.isLoggedIn() || !storage.isRemoteDoc())
				throw new IOException("No remote document loaded!");

			String fn = DialogUtil.showOpenFolderDialog(getShell(), "Choose a folder with image files", lastLocalDocFolder);
			if (fn == null)
				return;

			// store current location 
			lastLocalDocFolder = fn;
			
			File inputDir = new File(fn);
			List<File> imgFiles = Arrays.asList(inputDir.listFiles(new ImgFileFilter()));
			Collections.sort(imgFiles);

			final BatchImageReplaceDialog d = new BatchImageReplaceDialog(getShell(), storage.getDoc(), imgFiles);
			if (d.open() != Dialog.OK) {
				return;
			}

			logger.debug("checked pages: ");
			for (TrpPage p : d.getCheckedPages()) {
				logger.debug("" + p);
			}
			logger.debug("checkesd urls: ");
			for (URL u : d.getCheckedUrls()) {
				logger.debug("" + u);
			}

			if (d.getCheckedPages().size() != d.getCheckedUrls().size()) {
				throw new Exception("The nr. of checked pages must equals nr. of checked images!");
			}

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						storage.batchReplaceImages(d.getCheckedPages(), d.getCheckedUrls(), monitor);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Batch replacing images", true);

			if (d.getCheckedPages().contains(storage.getPage())) {
				reloadCurrentPage(false, null, null);
			}

		} catch (Throwable e) {
			onError("Error", "Error during batch replace of images", e);
		}
	}

	public void selectProfile(String name) {
		try {
			TrpConfig.loadProfile(name);
//			ui.updateProfiles();
		} catch (Exception e) {
			onError("Error loading profile!", e.getMessage(), e, true, false);
		}
	}

	public void saveNewProfile() {

		try {
			InputDialog dlg = new InputDialog(getShell(), "Save current settings as profile", "Profile name: ", "", new IInputValidator() {
				@Override public String isValid(String newText) {
					if (StringUtils.isEmpty(newText) || !newText.matches(TrpConfig.PROFILE_NAME_REGEX))
						return "Invalid profile name - only alphanumeric characters and underscores allowed!";

					return null;
				}
			});
			if (dlg.open() != Window.OK)
				return;

			String profileName = dlg.getValue();
			logger.debug("profileName = " + profileName);

			File profileFile = null;
			try {
				profileFile = TrpConfig.saveProfile(profileName, false);
				ui.updateProfiles(true);
			} catch (FileExistsException e) {
				if (DialogUtil.showYesNoDialog(getShell(), "Profile already exists!", "Do want to overwrite the existing one?") == SWT.YES) {
					profileFile = TrpConfig.saveProfile(profileName, true);
				}
			}
			if (profileFile != null)
				DialogUtil.showMessageBox(getShell(), "Success", "Written profile to: \n\n" + profileFile.getAbsolutePath(), SWT.ICON_INFORMATION);

		} catch (Exception e) {
			onError("Error saving profile!", e.getMessage(), e, true, false);
		}

	}

	public void createThumbs(TrpDoc doc) {
		// TODO Auto-generated method stub

		try {
			logger.debug("creating thumbnails for document: " + doc);
			if (doc == null) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document given");
				return;
			}

			if (!doc.isLocalDoc()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "This is not a local document");
				return;
			}

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						/*
						int N = 1000;
						monitor.beginTask("task!", N);
						for (int i=0; i<N; ++i) {
							monitor.worked(i+1);
							monitor.subTask("done: "+(i+1)+"/"+N);
							if (monitor.isCanceled())
								return;
						}
						*/

						//CreateThumbsService.createThumbForDoc(doc, true, null);

						SWTUtil.createThumbsForDoc(doc, false, monitor);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Creating thumbs for local document", true);
		} catch (Throwable e) {
			onError("Error", "Error during batch replace of images", e);
		}
		updateThumbs();
	}
	
	public void insertTextOnSelectedTranscriptionWidget(Character c) {
		if (c == null)
			return;

		ATranscriptionWidget tw = ui.getSelectedTranscriptionWidget();
		if (tw == null)
			return;

		tw.insertTextIfFocused("" + c);
	}
	
	public void openVkDialog() {
		logger.debug("opening vk dialog");
		
		if (SWTUtil.isOpen(vkDiag)) {
			vkDiag.getShell().setVisible(true);
		} else {
			vkDiag = new TrpVirtualKeyboardsDialog(getShell());
			vkDiag.create();
			vkDiag.getVkTabWidget().addListener(new ITrpVirtualKeyboardsTabWidgetListener() {
				@Override public void onVirtualKeyPressed(TrpVirtualKeyboardsTabWidget w, char c, String description) {
					TrpMainWidget.this.insertTextOnSelectedTranscriptionWidget(c);
				}
			});			
			vkDiag.open();
		}
	}
	
	public void openJobsDialog() {
		logger.debug("opening jobs dialog");
		if (SWTUtil.isOpen(jobsDiag)) {
			jobsDiag.getShell().setVisible(true);
		} else {
			jobsDiag = new JobsDialog(getShell());
			jobsDiag.open();
		}
	}
		
	public void openActivityDialog() {
		logger.debug("opening cm dialog");
		if (SWTUtil.isOpen(ad)) {
			ad.getShell().setVisible(true);
		} else {
			int col = (storage.getCollection(storage.getCollId()) != null ? storage.getCollId() : -1);
			ad = new ActivityDialog(getShell(), col);
			ad.open();
		}
	}
	
	public void openCollectionUsersDialog(TrpCollection c) {
		if (SWTUtil.isOpen(collUsersDiag)) {
			collUsersDiag.getShell().setVisible(true);
		} else {
			collUsersDiag = new CollectionUsersDialog(getShell(), c);
			collUsersDiag.open();
		}
	}
	
	public void openStrayDocsDialog() {
		if (SWTUtil.isOpen(strayDocsDialog)) {
			strayDocsDialog.getShell().setVisible(true);
		} else {
			strayDocsDialog = new StrayDocsDialog(getShell());
			strayDocsDialog.open();
		}
	}
	
	public void openCollectionManagerDialog() {
		logger.debug("opening cm dialog");
		
		if (cm!=null && !SWTUtil.isDisposed(cm.getShell())) {
			cm.getShell().setVisible(true);
		} else {
			cm = new CollectionManagerDialog(getShell(), SWT.NONE, ui.getServerWidget());
			cm.open();
		}
	}
	
	public void openEditDeclManagerDialog() {
		if(!storage.isDocLoaded()) {
			return;
		}
		
		if (edDiag!=null && !SWTUtil.isDisposed(edDiag.getShell())) {
			edDiag.getShell().setVisible(true);
		} else {
			if(storage.getRoleOfUserInCurrentCollection().getValue() < TrpRole.Editor.getValue()){
				edDiag = new EditDeclViewerDialog(getShell(), SWT.NONE);
			} else {
				edDiag = new EditDeclManagerDialog(getShell(), SWT.NONE);
			}
			edDiag.open();
		}
	}
	
//	public boolean isEditDeclManagerOpen() {
//		return edm != null && edm.getShell() != null && !edm.getShell().isDisposed();
//	}
	
	public void openDebugDialog() {
		logger.debug("opening debug dialog");
		if (SWTUtil.isOpen(debugDiag)) {
			debugDiag.getShell().setVisible(true);
		} else {
			debugDiag = new DebuggerDialog(getShell());
			debugDiag.open();
		}
	}
	
	public void openVersionsCompareDialog(String diffText) throws NullValueException, JAXBException {
		logger.debug("opening compare dialog");
		if (SWTUtil.isOpen(browserDiag)) {
			browserDiag.refreshText(this.getTextDifferenceOfVersions(browserDiag.isShowLineNrs()));
			browserDiag.getShell().setVisible(true);
		} else {
			browserDiag = new VersionsDiffBrowserDialog(getShell(), diffText);
			browserDiag.open();
		}
	}
	
		
	public void openVersionsDialog() {
		logger.debug("opening versions dialog");
		if (SWTUtil.isOpen(versionsDiag)) {
			versionsDiag.getShell().setVisible(true);
		} else {
			versionsDiag = new TranscriptsDialog(getShell());
			versionsDiag.open();
		}
	}
	
	public void openRecycleBin() {
		logger.debug("opening recycle bin - "+Storage.getInstance().getCollId());
		if (SWTUtil.isOpen(recycleBinDiag)) {
			recycleBinDiag.getShell().setVisible(true);
		} else {
			recycleBinDiag = new RecycleBinDialog(getShell(), Storage.getInstance().getCollId());
			recycleBinDiag.open();
		}
	}
	
	public void openViewSetsDialog() {
		
		logger.debug("opening view sets dialog");
		if (viewSetsDiag!=null && !SWTUtil.isDisposed(viewSetsDiag.getShell())) {
			viewSetsDiag.getShell().setVisible(true);
		} else {
			viewSetsDiag = new SettingsDialog(getShell(), /*SWT.PRIMARY_MODAL|*/ SWT.DIALOG_TRIM, getCanvas().getSettings(), getTrpSets());
			viewSetsDiag.open();
		}
	}
	
	public void openProxySetsDialog() {
		logger.debug("opening proxy sets dialog");
		if (proxyDiag!=null && !SWTUtil.isDisposed(proxyDiag.getShell())) {
			proxyDiag.getShell().setVisible(true);
		} else {
			proxyDiag = new ProxySettingsDialog(getShell(), /*SWT.PRIMARY_MODAL|*/ SWT.DIALOG_TRIM);
			proxyDiag.open();
			Storage.getInstance().updateProxySettings();
		}
	}
	
	public void openPreferencesDialog() {
		logger.debug("opening preferences dialog");
		if (preferencesDiag!=null && !SWTUtil.isDisposed(preferencesDiag.getShell())) {
			preferencesDiag.getShell().setVisible(true);
		} else {
			preferencesDiag = new PreferencesDialog(getShell());
			preferencesDiag.open();
			Storage.getInstance().updateProxySettings();
		}
	}
	
	
	public void openAutoSaveSetsDialog() {
		logger.debug("opening autosave sets dialog");
		if (autoSaveDiag!=null && !SWTUtil.isDisposed(autoSaveDiag.getShell())) {
			autoSaveDiag.getShell().setVisible(true);
		} else {
			autoSaveDiag = new AutoSaveDialog(getShell(), getTrpSets());
			autoSaveDiag.open();
		}
	}

	public void openAboutDialog() {
		String msg = ui.HELP_TEXT;
		if(javaInfo != null) {
			msg += "\n\nInstallation details:\n" + javaInfo.toPrettyString();
		}
		int res = DialogUtil.showMessageDialog(getShell(), ui.APP_NAME, msg, null, MessageDialog.INFORMATION, 
				new String[] {"OK", "Report bug / feature request"}, 0);
		
		if (res == 1) {
			ui.getTrpMenuBar().getBugReportItem().notifyListeners(SWT.Selection, new Event());
		}		
	}
	
	public Point getLocationOnTitleBarAfterMenuButton() {
		Rectangle r = ui.menuButton.getBounds();
		Point p = new Point(r.x, r.y);
		p = ui.toDisplay(new Point(r.x, r.y));
		p.x += r.width;
		p.y += r.height;	
		
		return p;
	}
	
	public void showTrayNotificationOnChangelog(boolean forceShow) {
		if (forceShow || getTrpSets().isShowChangeLog()) {
			Point p = getLocationOnTitleBarAfterMenuButton();
			
			ToolTip tip = DialogUtil.createBallonToolTip(ui.getShell(), SWT.ICON_INFORMATION, "New version "+VERSION, "Find out what's new in this version!", 
					p.x, p.y);
			SWTUtil.onSelectionEvent(tip, e -> {
				openChangeLogDialog(true);
			});
			tip.setAutoHide(true);
			tip.setVisible(true);
			
			getTrpSets().setShowChangeLog(false);
		}
	}

	public void openChangeLogDialog(boolean show) {
		if (changelogDialog == null) {
			changelogDialog = new ChangeLogDialog(getShell(), SWT.NONE);
			changelogDialog.setShowOnStartup(getTrpSets().isShowChangeLog());
		}
		
		if (show) {
			changelogDialog.open();
//			getTrpSets().setShowChangeLog(changelogDialog.isShowOnStartup());
			getTrpSets().setShowChangeLog(false); // set property to false after every close -> property is set to true automatically after the user updates
		}

	}
	
	public void openJavaVersionDialog(boolean force) {
		boolean isJaveInstallationBroken = javaVersionDialog == null && (!javaInfo.getSystemArch().equals(javaInfo.getJavaArch())) || javaInfo.getVersion().startsWith("1.10") 
				|| !javaInfo.getFileEnc().startsWith("UTF-8");
		
		if (force || isJaveInstallationBroken) {
			javaVersionDialog = new JavaVersionDialog(getShell(), SWT.NONE, javaInfo);
			javaVersionDialog.open();
		}
	}
	
	public void openPAGEXmlViewer() {
		try {
			logger.debug("loading transcript source");
			if (storage.isPageLoaded() && storage.getTranscriptMetadata() != null) {
				URL url = Storage.getInstance().getTranscriptMetadata().getUrl();

				PAGEXmlViewer xmlviewer = new PAGEXmlViewer(ui.getShell(), SWT.MODELESS);
				xmlviewer.open(url);
			}
		} catch (Exception e1) {
			onError("Could not open XML", "Could not open XML", e1);
		}			
	}

	public void changeProfileFromUi() {
		int i = ui.getProfilesToolItem().getLastSelectedIndex();
		logger.debug("changing profile from ui, selected index = "+i);
		
		if (i>=0 && i < ui.getProfilesToolItem().getItemCount()-1) { // profile selected
			if (!SWTUtil.isDisposed(ui.getProfilesToolItem().getSelected()) && ui.getProfilesToolItem().getSelected().getData() instanceof String) {				
				String name = (String) ui.getProfilesToolItem().getSelected().getData();
				logger.info("selecting profile: "+name);
				mw.selectProfile(name);
									
				boolean mode = (name.contains("Transcription")? true : false);
				canvas.getScene().setTranscriptionMode(mode);
			}
		} else if (i == ui.getProfilesToolItem().getItemCount()-1) {
			logger.info("opening save profile dialog...");
			mw.saveNewProfile();
			canvas.getScene().setTranscriptionMode(false);
		}
	}
	
	public int getSelectedCollectionId() {
		return ui.getServerWidget().getSelectedCollectionId();
	}
	
	public TrpCollection getSelectedCollection() {
		return ui.getServerWidget().getSelectedCollection();
	}
	
	public void openHowToGuides() {
		Desktop d = Desktop.getDesktop();
		try {
			d.browse(new URI("https://transkribus.eu/wiki/index.php/How_to_Guides"));
		} catch (IOException | URISyntaxException e) {
			logger.debug(e.getMessage());
		}		
	}

	public void openCanvasHelpDialog() {
		String ht = ""
//				+ "Canvas shortcut operations:\n"
				+ "- esc: set selection mode\n"
				+ "- hold down either the left mouse button on an empty image area or the right mouse button to move the image\n"
				+ "- shift + drag-on-bounding-box: resize shape on bounding box\n"
				+ "- shift + drag shape: move shape including all its child shapes\n"
				+ "- ctrl/cmd + selecting shapes: select multiple shapes\n"
				+ "- ctrl/cmd + dragging shape(s): move multiple selected shapes\n"
				+ "- right click on a shape: context menu with additional operations\n"
				+ "  (note: on mac touchpads, right-clicks are performed using two fingers simultaneously)\n"
				+ " \n\nText operations:\n"
				+ "- Backspace: moves text one line up\n"
				+ "- Ctrl + Return: moves text one line down"
				;
		
		int res = DialogUtil.showMessageDialog(getShell(), "Canvas shortcut operations", ht, null, MessageDialog.INFORMATION, 
				new String[] {"OK"}, 0);
		
	}
	
	/**
	 * Sleak is a memory tracking utility for SWT
	 */
	public void openSleak() {
//		if (true) // FIXME
//			return;
		
		logger.debug("opening sleak...");

		if (!SWTUtil.isDisposed(sleakDiag)) {
			sleakDiag.getShell().setVisible(true);
		} else {
//			DeviceData data = new DeviceData();
//			data.tracking = true;
//			Display display = new Display(data);
//			
//			display.getDeviceData().tracking = true;
			
			Sleak sleak = new Sleak();
			sleakDiag = new Shell(getShell(), SWT.RESIZE | SWT.CLOSE | SWT.MODELESS);
			sleakDiag.setText("S-Leak");
			Point size = sleakDiag.getSize();
			sleakDiag.setSize(size.x / 2, size.y / 2);
			sleak.create(sleakDiag);
			sleakDiag.open();
		}
	}
	
	public void openSearchForTagsDialog() {
		if(Storage.getInstance().getDoc() == null){
			DialogUtil.showErrorMessageBox(mw.getShell(), "Error", "No document loaded.");
			return;
		}
		
		openSearchDialog();
		getSearchDialog().selectTab(SearchType.TAGS);
	}
	
	public void searchCurrentDoc(){		
		if (!storage.isRemoteDoc()) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Error", "No remote doc loaded!");
			return;
		}
		
		openSearchDialog();
		getSearchDialog().selectTab(SearchType.FULLTEXT);
		
		FullTextSearchComposite ftComp = getSearchDialog().getFulltextComposite();		
		ftComp.searchCurrentDoc(true);							
		ftComp.setSearchText(ui.getQuickSearchText().getText());			
		ftComp.findText();
	}
	
	/**
	 * Opens the search dialog, selects the tag-search tab and searches for the tag with given tagName in the current remote collection / document
	 */
	public void searchTags(boolean collLevel, String tagName) {
		if (!storage.isRemoteDoc()) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Error", "No remote doc loaded!");
			return;
		}
		
		openSearchDialog();
		getSearchDialog().selectTab(SearchType.TAGS);
		
		TagSearchComposite tagSearchComp = getSearchDialog().getTagSearchComposite();
		tagSearchComp.searchForTag(false, tagName);
	}
	
	public boolean duplicateDocuments(int srcColId, List<TrpDocMetadata> docs) {
		if (!storage.isLoggedIn())
			return false;
		
		if (CoreUtils.isEmpty(docs)) {
			DialogUtil.showErrorMessageBox(getShell(), "No document selected", "Please select documents you want to duplicate!");
			return false;
		}
		
		logger.debug("duplicating document, srcColId = "+srcColId+", nDocs = "+docs.size());
		
//		if (srcColId <= 0) {
//			DialogUtil.showErrorMessageBox(getShell(), "Error", "No source collection specified!");
//			return;
//		}
				
		if (!StorageUtil.canDuplicate(srcColId)) {
			DialogUtil.showErrorMessageBox(getShell(), "Insufficient rights", "You must be either at least editor of the collection!");
			return false;
		}
								
		ChooseCollectionDialog diag = new ChooseCollectionDialog(getShell(), "Choose a destination collection", storage.getCollection(srcColId)) {
			@Override protected Control createDialogArea(Composite parent) {
				super.showOptions(true);
				Composite container = (Composite) super.createDialogArea(parent);
				return container;
			}
		};
		
		if (diag.open() != Dialog.OK)
			return false;
		
		TrpCollection c = diag.getSelectedCollection();
		if (c==null) {
			DialogUtil.showErrorMessageBox(getShell(), "No collection selected", "Please select a collection to duplicate the document to!");
			return false;
		}
		int toColId = c.getColId();
		
		final String title_suffix = "_duplicated";
		
		String nameTmp = null;
		if (docs.size() == 1 || diag.isCopyIntoOne()) {
			InputDialog dlg = new InputDialog(getShell(), "New name", "Enter the new name of the document", docs.get(0).getTitle()+title_suffix, null);
			if (dlg.open() != Window.OK)
				return false;
		
			nameTmp = dlg.getValue();
		}
		final String newNameForSingleDoc = nameTmp;
		
		final List<String> error = new ArrayList<>();
		try {
		ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
			@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				
				try {
					monitor.beginTask("Duplicating documents", docs.size());
//						TrpUserLogin user = storage.getUser();
					int i = 0;
					List<DocumentSelectionDescriptor> dsdList = new ArrayList<DocumentSelectionDescriptor>();
					
					JobParameters params = new JobParameters();
					String status = diag.isCopyStatus()? diag.getEditStatus() : null;
					logger.debug("status for duplicating: " + status);
					params.setJobImpl(JobImpl.CopyJob.toString());
					//params.getParams().addParameter(JobConst.PROP_DOC_DESCS, d.getDocId());
					params.getParams().addIntParam(JobConst.PROP_COLLECTION_ID, toColId);
					params.getParams().addBoolParam("copyStatus", diag.isCopyStatus());
					params.getParams().addBoolParam("copyAll", diag.isCopyAllTranscripts());
					if(status != null) {
						params.getParams().addParameter("status", status);
					}
					
					for (TrpDocMetadata d : docs) {
						if (monitor.isCanceled())
							throw new InterruptedException();

						try {
							// the name of the duplicated document is either the name the user has input into the dialog for a single document, or, for multiple
							// documents, the document title + title_suffix
							String name = (docs.size() == 1 || diag.isCopyIntoOne()) && !StringUtils.isEmpty(newNameForSingleDoc) ? newNameForSingleDoc : d.getTitle()+title_suffix;
							logger.debug("duplicating document: "+d+" name: "+name+", toColId: "+toColId);
													
							
							/*
							 * the normal old call if the user wants to duplicate as before
							 * else should duplicate the document but with only the GT pages or with all available transcripts
							 * 		selection of pages is done during the job
							 */
							if (!diag.isCopyStatus() && !diag.isCopyAllTranscripts() && !diag.isCopyIntoOne()) {
								storage.duplicateDocument(srcColId, d.getDocId(), name, toColId <= 0 ? null : toColId);
							}
							else {
					
								
								//TrpDoc doc = storage.getConnection().getTrpDoc(srcColId, d.getDocId(), -1);
								DocumentSelectionDescriptor dsd = new DocumentSelectionDescriptor(d.getDocId());
//								TrpDoc doc2 = new TrpDoc(doc);
//								
//								doc2.filterPagesByPagesStrAndEditStatus("1-"+doc.getNPages(), EditStatus.GT, true);
//								doc2.getPages();
//								for (TrpPage page : doc2.getPages()) {
//									PageDescriptor pd = new PageDescriptor(page.getPageId(), page.getCurrentTranscript().getTsId());
//									dsd.addPage(pd);
//								}
								
								/*
								 * in that case each document should become a new document as well
								 * if the parameter is true -> all selected document become one new document
								 */
								if (!diag.isCopyIntoOne()) {
									dsdList = new ArrayList<DocumentSelectionDescriptor>();
									dsdList.add(dsd);
									params.setDocs(dsdList);
									params.getParams().addParameter(JobConst.PROP_TITLE, name);		
									storage.getConnection().duplicateDocument(srcColId, params);
									params = new JobParameters();
								}
								//use the existent list and add the further document
								else {
									params.getParams().addParameter(JobConst.PROP_TITLE, name);
									dsdList.add(dsd);
								}
								

							}
//							storage.duplicateDocument(srcColId, d.getDocId(), null, toColId <= 0 ? null : toColId); // TEST: null as name, did formerly result in NPE on server
						} catch (Throwable e) {
							logger.warn("Could not duplicate document: "+d);
							error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessage());
						}
													
						monitor.worked(++i);
					}
					
					if (diag.isCopyIntoOne()) {
						params.setDocs(dsdList);
						storage.getConnection().duplicateDocument(srcColId, params);
					}
				}
				catch (InterruptedException ie) {
					throw ie;
				} catch (Throwable e) {
					throw new InvocationTargetException(e, e.getMessage());
				}
			}
		}, "Duplicating documents", true);
		}
		catch (InterruptedException e) {}
		catch (Throwable e) {
			onError("Unexpected error", e.getMessage(), e);
		}
		
		if (!error.isEmpty()) {
			String msg = "Could not duplicate the following documents:\n";
			for (String u : error) {
				msg += u + "\n";
			}
			
			mw.onError("Error duplicating documents", msg, null);
			return false;
		} else {
			DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully duplicated "+docs.size()+" documents\nGo to the jobs view to check the status of duplication!");
			return true;
		}
	}
		
	public boolean deleteDocuments(List<TrpDocMetadata> docs, boolean reallyDelete) {
		if (!storage.isLoggedIn()) {
			return false;
		}
		
		if (CoreUtils.isEmpty(docs)) {
			DialogUtil.showErrorMessageBox(getShell(), "No document selected", "Please select a document you want to delete!");
			return false;
		}
		
		List<Integer> listOfDocIdsOfRunningJobs = new ArrayList<>();
		
		try {
			/*
			 * only docs for which no job is currently running will be considered to be deleted or retrieved
			 */
			for (TrpJobStatus job : mw.getStorage().getUnfinishedJobs(true)){
				listOfDocIdsOfRunningJobs.add(job.getDocId());
			}
		} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e1) {
			logger.error("Could not query unfinished jobs.", e1);
		}
		
		int N = docs.size();
		
		if (N > 1) {
			String msg = reallyDelete ? "Do you really want to delete " + N + " selected documents irreversible? " : "Do you really want to delete " + N + " selected documents?\n" 
					+ "After deletion you can find your documents in the recycle bin.\n"
					+ "Documents in the recycle bin are automatically discarded after two weeks.";
			if (DialogUtil.showYesNoDialog(getShell(), "Delete Documents", msg)!=SWT.YES) {
				return false;
			}
		}
		else{
			String msg = reallyDelete ? "Do you really want to delete document '"+docs.get(0).getTitle()+"' irreversible?" : "Do you really want to delete document '"+docs.get(0).getTitle()+"'?\n"
					+ "After deletion you can find your document in the recycle bin!\n"
					+ "Documents in the recycle bin are automatically discarded after two weeks.";
			if (DialogUtil.showYesNoDialog(getShell(), "Delete Document", msg)!=SWT.YES) {
				return false;
			}
		}
		
		final List<String> error = new ArrayList<>();
		try {
		ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
			@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				
				try {
					monitor.beginTask("Deleting documents", docs.size());
					TrpUserLogin user = storage.getUser();
					int i = 0;
					for (TrpDocMetadata d : docs) {
						if (monitor.isCanceled())
							throw new InterruptedException();

						logger.debug("deleting document: "+d);
						
						if (listOfDocIdsOfRunningJobs.contains(d.getDocId())){
							String errorMsg = "A job is currently running on document: "+d.getDocId();
							logger.warn(errorMsg);
							error.add(errorMsg);
						}
						else if (!user.isAdmin() && user.getUserId()!=d.getUploaderId()) {
//							DialogUtil.showErrorMessageBox(getShell(), "Unauthorized", "You are not the uploader of this document. " + md.getTitle());
//							return false;
							String errorMsg = "Unauthorized - you are not the uploader of this document: "+d.getTitle()+", id: "+d.getDocId();
							logger.warn(errorMsg);
							error.add(errorMsg);
						} 					
						else {
							try {
								storage.deleteDocument(storage.getCollId(), d.getDocId(), reallyDelete);
								logger.info("Create delete document job: "+d);
							} catch (SessionExpiredException | TrpClientErrorException | TrpServerErrorException e) {
								logger.warn("Could not delete document: "+d, e);
								error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessageToUser());
							} catch (Throwable e) {
								logger.warn("Could not delete document: "+d, e);
								error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessage());
							}
						}
	
						monitor.worked(++i);
					}

				}
				catch (InterruptedException ie) {
					throw ie;
				} catch (Throwable e) {
					throw new InvocationTargetException(e, e.getMessage());
				}
			}
		}, "Deleting documents", true);
		}
		catch (InterruptedException e) {}
		catch (Throwable e) {
			onError("Unexpected error", e.getMessage(), e);
		}
		
		if (!error.isEmpty() && error.size() == docs.size()) {
			String msg = "Could not delete the following documents:\n";
			for (String u : error) {
				msg += u + "\n";
			}
			if (SWTUtil.isOpen(recycleBinDiag)){
				recycleBinDiag.close();
			}
			mw.onError("Error deleting documents", msg, null);
			try {
				storage.reloadCollections();
				storage.reloadDocList(storage.getCollId());
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
					| NoConnectionException e) {
				// TODO Auto-generated catch block
				logger.error("reloading doc list not possible after document(s) deletion");
				e.printStackTrace();
			}
			ui.serverWidget.getDocTableWidget().reloadDocs(false, true);
			return false;
		} else {
			String msg = "";
			if (!error.isEmpty()) {
				msg = error.size() + " doc(s) could not be deleted:\n";
				for (String u : error) {
					msg += u + "\n";
				}
			}
			if (reallyDelete){
				DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully created "+(docs.size()-error.size())+" delete document jobs!\n"
						+ "The deleted documents will only disappear after the delete jobs have been finished!\n"
						+  msg);
			}
			else{
				DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully added "+(docs.size()-error.size())+" document(s) to the recycle bin\n"
						+  msg);
			}
			//clean up GUI
			try {
				//recycleBinDiag.close();
				storage.reloadCollections();
				storage.reloadDocList(storage.getCollId());
				if (SWTUtil.isOpen(strayDocsDialog)){
					storage.reloadUserDocs();
				}
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
					| NoConnectionException e) {
				// TODO Auto-generated catch block
				logger.error("recycle bin");
				e.printStackTrace();
			}
			//reload necessary to actualize doc list
			ui.serverWidget.getDocTableWidget().reloadDocs(false, true);
			for (TrpDocMetadata d : docs) {
				if (storage.isThisDocLoaded(d.getDocId(), null)) {
					//if deleted doc was loaded in GUI - close it
					logger.debug("deleted doc loaded in GUI - close it");
					storage.closeCurrentDocument();
					clearCurrentPage();
				}
				//update the recent docs list
				updateRecentDocItems(d);
				ui.getServerWidget().updateRecentDocs();
				
				//Todo
				//reload recycle bin if open??
				//Todo
//				if (SWTUtil.isOpen(recycleBinDiag)){
//					recycleBinDiag.refresh();
//				}
			}
			
			return true;
		}
	}
	
	public boolean addDocumentsToCollection(int srcColId, Collection<TrpDocMetadata> docs, boolean strayDoc) {
		if (!storage.isLoggedIn() || docs==null || docs.isEmpty()) {
			return false;
		}
		
		//cannot be checked for stray documents
		if (!strayDoc){
			TrpCollection coll = storage.getCollection(srcColId);
			if (coll == null) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "Could not determine collection for selected documents!");
			}
			
			final TrpUserLogin user = storage.getUser();
			
			if (!user.isAdmin() && !StorageUtil.isOwnerOfCollection(coll) && !StorageUtil.isUploader(user, docs)) {
				DialogUtil.showErrorMessageBox(getShell(), "Unauthorized", "You are not the owner in this collection or uploader of the document(s)!");
				return false;
			}
		}
		
		ChooseCollectionDialog diag = new ChooseCollectionDialog(getShell(), "Choose a collection where the documents should be added to", storage.getCurrentDocumentCollection()) {
			@Override protected Control createDialogArea(Composite parent) {
				super.showOptions(false);
				Composite container = (Composite) super.createDialogArea(parent);
				
				Label infoLabel = new Label(container, 0);
				infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 2));
				String lableTxt = (strayDoc ? "" : "Note: documents are only linked into the collection, i.e. a soft copy is created.\nThey also remain linked to the current collection but you can also unlink them!");
				infoLabel.setText(lableTxt);
				
				Fonts.setItalicFont(infoLabel);
				
				return container;
			}
			
			@Override protected Point getInitialSize() {
				return new Point(550, 200);
			}
		};
		if (diag.open() != Dialog.OK)
			return false;
		
		TrpCollection c = diag.getSelectedCollection();
		if (c==null) {
			DialogUtil.showErrorMessageBox(getShell(), "No collection selected", "Please select a collection to add the document to!");
			return false;
		}
		logger.debug("selected collection is: "+c);		
		
		TrpServerConn conn = storage.getConnection();
				
		final List<String> error = new ArrayList<>();
		try {
		ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
			@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask("Adding documents to collection", docs.size());
					
					int i = 0;
					for (TrpDocMetadata d : docs) {
						if (monitor.isCanceled())
							throw new InterruptedException();
						
						logger.debug("adding document: "+d+" to collection: "+c.getColId());				
						try {					
							conn.addDocToCollection(c.getColId(), d.getDocId());
							logger.info("added document: "+d);
						} catch (Throwable e) {
							logger.warn("Could not add document: "+d);
							error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessage());
						}
						
						monitor.worked(++i);
					}
				}
				catch (InterruptedException ie) {
					throw ie;
				} catch (Throwable e) {
					throw new InvocationTargetException(e, e.getMessage());
				}
			}
		}, "Adding documents to collection", true);
		}
		catch (InterruptedException e) {}
		catch (Throwable e) {
			onError("Unexpected error", e.getMessage(), e);
		}
		
		if (!error.isEmpty() && error.size() == docs.size()) {
			String msg = "Could not add the following documents:\n";
			for (String u : error) {
				msg += u + "\n";
			}
			
			mw.onError("Error adding documents", msg, null);
			return false;
		} else {
			String msg = "";
			if (!error.isEmpty()){
				msg = "Could not add the following documents:\n";
				for (String u : error) {
					msg += u + "\n";
				}
			}
			DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully added "+docs.size()+" documents\n" + msg);
			if (strayDoc){
				storage.reloadUserDocs();
			}
			return true;
		}
	}

	public boolean removeDocumentsFromCollection(int selColId, List<TrpDocMetadata> docs) {
		if (!storage.isLoggedIn() || CoreUtils.isEmpty(docs)) {
			return false;
		}
		
		if (DialogUtil.showYesNoDialog(getShell(), "", "Do you really want to unlink "+docs.size()+" document(s) from this collection?") != SWT.YES)
			return false;
		
		// check rights first:
		TrpCollection coll = storage.getCollection(selColId);
		if (coll == null) {
			DialogUtil.showErrorMessageBox(getShell(), "Error", "Could not determin collection for selected documents!");
		}
		
		TrpUserLogin user = storage.getUser();
		if (!user.isAdmin() && !StorageUtil.isOwnerOfCollection(coll) && !StorageUtil.isUploader(user, docs)) {
			DialogUtil.showErrorMessageBox(getShell(), "Unauthorized", "You are not the owner in this collection or uploader of the document(s)!");
			return false;
		}
		
		logger.debug("selected collection is: "+coll);		
		
		List<String> error = new ArrayList<>();
		try {
		ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
			@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask("Unlink document(s) from collection", docs.size());

					TrpServerConn conn = storage.getConnection();
					int i = 0;
					for (TrpDocMetadata d : docs) {
						if (monitor.isCanceled())
							throw new InterruptedException();
						
						logger.debug("Unlink document: "+d+" from collection: "+coll.getColId());				
						try {
							conn.removeDocFromCollection(coll.getColId(), d.getDocId());
							logger.info("unlinked document: "+d);
						} catch (Throwable e) {
							logger.warn("Could not unlink document: "+d);
							error.add(d.getTitle()+", ID = "+d.getDocId()+", Reason = "+e.getMessage());
						}
						
						monitor.worked(++i);
					}
				}
				catch (InterruptedException ie) {
					throw ie;
				}
				catch (Throwable e) {
					throw new InvocationTargetException(e, e.getMessage());
				}
			}
		}, "Unlink document(s) from collection", true);
		}
		catch (InterruptedException e) {}
		catch (Throwable e) {
			onError("Unexpected error", e.getMessage(), e);
		}	
				
		if (!error.isEmpty() && error.size() == docs.size()) {
			String msg = "Could not unlink the following documents:\nPlease note the difference between 'Unlink' and 'Delete'!\n";
			for (String u : error) {
				msg += u + "\n";
			}
			mw.onError("Error unlinking documents", msg, null);
			//clean up GUI
			try {
				//reload necessary in fact the symbolic image has changed
				storage.reloadCollections();
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
					| NoConnectionException e) {
				// TODO Auto-generated catch block
				logger.error("reloading collections not possible after documents deletion");
				e.printStackTrace();
			}
			ui.serverWidget.getDocTableWidget().reloadDocs(false, true);
			return false;
		} else {
			String msg = "";
			if (!error.isEmpty()){
				msg = "Could not unlink the following documents:\nPlease note the difference between 'Unlink' and 'Delete'!\n";
				for (String u : error) {
					msg += u + "\n";
				}
			}
			DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully unlinked "+docs.size()+" documents\n" + msg);
			//clean up GUI
			try {
				//reload necessary in fact the symbolic image has changed
				storage.reloadCollections();
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
					| NoConnectionException e) {
				// TODO Auto-generated catch block
				logger.error("reloading collections not possible after unlinking documents");
				e.printStackTrace();
			}
			//reload necessary to actualize doc list
			ui.serverWidget.getDocTableWidget().reloadDocs(false, true);
			for (TrpDocMetadata d : docs) {
				if (storage.isThisDocLoaded(d.getDocId(), null)) {
					//if deleted doc was loaded in GUI - close it
					logger.debug("unlinked doc loaded in GUI - close it");
					storage.closeCurrentDocument();
					clearCurrentPage();
				}
			}
			
			
			return true;
		}
	}
	
	public int createCollection() {
		logger.debug("creating collection...");
		
		InputDialog dlg = new InputDialog(getShell(),
	            "Create collection", "Enter the name of the new collection (min. 3 characters)", "", new IInputValidator() {
					@Override public String isValid(String newText) {
						if (StringUtils.length(newText) >= 3)
							return null;
						else
							return "Too short";
					}
				});
		if (dlg.open() != Window.OK)
			return 0;
				
		String collName = dlg.getValue();
		try {
			int collId = storage.addCollection(dlg.getValue());
			logger.debug("created new collection '"+collName+"' - now reloading available collections!");
			storage.reloadCollections();
			
			return collId;
		} catch (Throwable th) {
			mw.onError("Error", "Error creating collection '"+collName+"': "+th.getMessage(), th);
			return 0;
		}
	}
	
	public void deleteCollection(TrpCollection c) {
		if (c== null || !storage.isLoggedIn())
			return;
		
		TrpServerConn conn = storage.getConnection();
		logger.debug("deleting collection: "+c.getColId()+" name: "+c.getColName());
					
		if(!storage.getUser().isAdmin() && !AuthUtils.isOwner(c.getRole())) {
			DialogUtil.showErrorMessageBox(getShell(), "Unauthorized", "You are not the owner of this collection.");
			return;
		}
		
		if (DialogUtil.showYesNoDialog(getShell(), "Are you sure?", "Do you really want to delete the collection \"" 
				+ c.getColName() + "\"?\n\n"
				+ "Note: documents are not deleted, only their reference to the collection is removed - "
				+ "use the delete document button to completely remove documents from the server!\n"
				+ "After collection is deleted the docs can be deleted or reassigned via the 'Stray Docs Dialog'!",
				SWT.ICON_WARNING)!=SWT.YES) {
			return;
		}
		
		try {
			conn.deleteCollection(c.getColId());
//			DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully deleted collection!");
			
			logger.info("deleted collection "+c.getColId()+" name: "+c.getColName());
			storage.reloadCollections();
		} catch (Throwable th) {
			mw.onError("Error", "Error deleting collection '"+c.getColName()+"': "+th.getMessage(), th);
		}
	}

	public void modifyCollection(TrpCollection c) {
		if (c== null || !storage.isLoggedIn())
			return;
		
		try {
			logger.debug("Role in collection: " + c.getRole());
			if (!AuthUtils.canManage(c.getRole())) {
				DialogUtil.showErrorMessageBox(getShell(), "Unauthorized", "You are not allowed to modify this collection!");
				return;
			}
			
			storage.reloadCollections();
			
			CollectionEditorDialog ced = new CollectionEditorDialog(getShell(), c);
//			if(c.isCrowdsourcing()) {
//				ced.getCollection().setCrowdProject(storage.loadCrowdProject(ced.getCollection().getColId()));
//			}
			if (ced.open() != IDialogConstants.OK_ID) {
				/*
				 * user clicked cancel: milestones and messages without project id (this is how we know the
				 * added milestones and messages from this session) get deleted because this seems to be his 
				 * intention by clicking Cancel 
				 */
//				if (ced.isCrowdMdChanged()){
//					storage.getConnection().deleteCrowdProjectMilestones(ced.getCollection().getColId());
//					storage.getConnection().deleteCrowdProjectMessages(ced.getCollection().getColId());
//					storage.reloadCollections();
//					
//				}
				return;
			}
			
			if(!ced.isMdChanged()) {
				logger.debug("Metadata was not altered.");
				//return;
			}
			else{
				TrpCollection newMd = ced.getCollection();
				storage.getConnection().updateCollectionMd(newMd);
				storage.reloadCollections();
			}
			
//			if (ced.isCrowdMdChanged()){
//				TrpCollection newMd = ced.getCollection();
//				logger.debug("crowd metadata has changed");
//				storage.getConnection().postCrowdProject(newMd.getColId(), newMd.getCrowdProject());
//				for (TrpCrowdProjectMilestone mst : newMd.getCrowdProject().getCrowdProjectMilestones()){
//					storage.getConnection().postCrowdProjectMilestone(newMd.getCrowdProject().getColId(), mst);
//				}
//				for (TrpCrowdProjectMessage msg : newMd.getCrowdProject().getCrowdProjectMessages()){
//					storage.getConnection().postCrowdProjectMessage(newMd.getCrowdProject().getColId(), msg);
//				}
//				
//			}
			
//			DialogUtil.showInfoMessageBox(getShell(), "Success", "Successfully modified the colleciton!");
		} catch (Exception e) {
			mw.onError("Error modifying collection", e.getMessage(), e);
		}
	}
	
	public void deleteTags(CustomTag... tags) {
		if (tags != null) {
			deleteTags(Arrays.asList(tags));
		}
	}

	public void deleteTags(List<CustomTag> tags) {
		try {
			for (CustomTag t : tags) {
				logger.trace("deleting tag: "+t+" ctl: "+t.getCustomTagList());
				if (t==null || t.getCustomTagList()==null)
					continue;
				
				t.getCustomTagList().deleteTagAndContinuations(t);
			}
	
			updatePageRelatedMetadata();
			getUi().getLineTranscriptionWidget().redrawText(true, false, false);
			getUi().getWordTranscriptionWidget().redrawText(true, false, false);
			refreshStructureView();
			
//			getUi().getTaggingWidget().getTagListWidget().refreshTable();
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}
	}
	
	public void handleStructureTagsInMonitor(Set<Integer> pageIndices, String chosenStructureType, String renameStructureType, boolean doAnnotate, boolean doDelete, boolean doRename) {
		try {
			logger.debug("handleStructureTagsInMonitor!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}

			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nTypesHandled=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("structure types batch processing");
						TrpDoc doc = storage.getDoc();
						int worked=0;
						int N = pageIndices==null ? doc.getNPages() : pageIndices.size();
						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Structural metadata processing", N);
						for (int i=0; i<doc.getNPages(); ++i) {
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
							
							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();

							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();
							
							int nChanged = processStructureTags(p, chosenStructureType, renameStructureType, doAnnotate, doDelete, doRename);
							res.nTypesHandled += nChanged;
							logger.debug("nTypesHandled = "+nChanged);
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (nChanged > 0) {
								++res.nPagesChanged;
								String msg = "Handled "+nChanged+" structure types in this page";
								
								res.affectedPageIndices.add(i);

							}
							
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "Handled "+res.nTypesHandled+" structure types in "+res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Handle structure types", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Structure types: ", res.msg+"\nAffected pages: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));
			
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}	
		
		try {
			storage.reloadDocWithAllTranscripts();
			refreshStructureView();
			reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
				if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
					autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
				}
				getCanvas().fitWidth();
				//adjustReadingOrderDisplayToImageSize();				
			}, null);

		} catch (ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SessionExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public int processStructureTags(TrpPage page, String chosenStructureType, String renameStructureType, boolean doAnnotate, boolean doDelete, boolean doRename) {
		
		int changed = 0;
		try {

			//get doc to have all transcripts for all pages available to change
//			Storage storage = Storage.getInstance();
//			TrpDoc currDoc = storage.getDoc();
			String note = "structure type '" + chosenStructureType;
			TrpTranscriptMetadata md = page.getCurrentTranscript();
			int parentId = md.getTsId();
							
			PcGtsType pcGtsType =  md.unmarshallTranscript();
			
			for (TrpRegionType currRegion : pcGtsType.getPage().getTextRegionOrImageRegionOrLineDrawingRegion()) {
				
				if (currRegion != null && currRegion instanceof TrpTextRegionType) { 
					StructureTag t = currRegion.getCustomTagList().getNonIndexedTag(StructureTag.TAG_NAME);
					if (t != null) {
						String structType = (String) t.getType();
						if (structType.contentEquals(chosenStructureType)) {
							if (doDelete) {
								logger.debug("Found struct type to delete = " + structType);
								note += "' deleted";
								setStructure(currRegion, null);
								changed++;
							}
							else if (doRename) {
								logger.debug("Found struct type to rename = " + structType);
								note += "' renamed into ' " + renameStructureType + "'";
								setStructure(currRegion, renameStructureType);
								changed++;
							}
						}
					}
					else {
						//struct type not found
						if (doAnnotate) {
							logger.debug("struct type not found, assign = " + chosenStructureType);
							note += "' assigned to unstructured regions";
							setStructure(currRegion, chosenStructureType);
							changed++;
						}
					}
				}
			}
			
			//store if it has changed
			if (changed > 0) {
				storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md.getDocId(), md.getPageNr(), EditStatus.IN_PROGRESS, pcGtsType, parentId, "StructTagSpecWidget", note);
			}
			
		} catch (ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SessionExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return changed;
	}
	
	private void setStructure(TrpRegionType currRegion, String newStructureType) {
		
		/*
		 * delete should work as well if 'newStructureType' is set to null
		 */
		currRegion.setStructure(newStructureType, false, this);
		//
		//currRegion.getCustomTagList().addOrMergeTag(new StructureTag(newStructureType),null);
		CustomTagUtil.writeCustomTagListToCustomTag(currRegion); 
		
	}

	public void deleteTagsForCurrentSelection() {
		try {
			logger.debug("clearing tags from selection!");
			ATranscriptionWidget aw = ui.getSelectedTranscriptionWidget();
			if (aw==null) {
				logger.debug("no transcription widget selected - doing nothing!");
				return;
			}
			
			List<Pair<ITrpShapeType, IntRange>> ranges = aw.getSelectedShapesAndRanges();
			for (Pair<ITrpShapeType, IntRange> p : ranges) {
				ITrpShapeType s = p.getLeft();
				IntRange r = p.getRight();
				s.getCustomTagList().deleteTagsInRange(r.getOffset(), r.getLength(), true);
				s.setTextStyle(null); // delete also text styles from range!
			}
			
			updatePageRelatedMetadata();
			getUi().getLineTranscriptionWidget().redrawText(true, false, false);
			getUi().getWordTranscriptionWidget().redrawText(true, false, false);
			refreshStructureView();
		} catch (Exception e) {
			onError("Unexpected error deleting tags", e.getMessage(), e);
		}	
	}
	
	public void addTagForSelection(CustomTag t, String addOnlyThisProperty) {
		addTagForSelection(t.getTagName(), t.getAttributeNamesValuesMap(), addOnlyThisProperty);
	}

	public void addTagForSelection(String tagName, Map<String, Object> attributes, String addOnlyThisProperty) {
		try {
			logger.debug("addTagForSelection, tagName = "+tagName+", attributes = "+attributes+", addOnlyThisProperty = "+addOnlyThisProperty);
			
			boolean isTextSelectedInTranscriptionWidget = isTextSelectedInTranscriptionWidget();
			
	//		ATranscriptionWidget aw = mainWidget.getUi().getSelectedTranscriptionWidget();
	//		boolean isSingleSelection = aw!=null && aw.isSingleSelection();
			
			CustomTag protoTag = CustomTagFactory.getTagObjectFromRegistry(tagName, false);
			boolean canBeEmpty = protoTag!=null && protoTag.canBeEmpty();
			logger.debug("protoTag = "+protoTag+" canBeEmtpy = "+canBeEmpty);
			logger.debug("isTextSelectedInTranscriptionWidget = "+isTextSelectedInTranscriptionWidget);		
			
			if (!isTextSelectedInTranscriptionWidget && !canBeEmpty) {
				logger.debug("applying tag to all selected in canvas: "+tagName);
				List<? extends ITrpShapeType> selData = canvas.getScene().getSelectedData(ITrpShapeType.class);
				logger.debug("selData = "+selData.size());
				for (ITrpShapeType sel : selData) {
					if (sel instanceof TrpTextLineType || sel instanceof TrpWordType) { // tags only for words and lines!
						try {
							CustomTag t = CustomTagFactory.create(tagName, 0, sel.getUnicodeText().length(), attributes);						
							sel.getCustomTagList().addOrMergeTag(t, addOnlyThisProperty);
							logger.debug("created tag: "+t);
						} catch (Exception e) {
							logger.error("Error creating tag: "+e.getMessage(), e);
						}
					}
				}
			} else {
				logger.debug("applying tag to all selected in transcription widget: "+tagName);
				List<Pair<ITrpShapeType, CustomTag>> tags4Shapes = TaggingWidgetUtils.constructTagsFromSelectionInTranscriptionWidget(ui, tagName, attributes);
	//			List<Pair<ITrpShapeType, CustomTag>> tags4Shapes = TaggingWidgetUtils.constructTagsFromSelectionInTranscriptionWidget(ui, tagName, null);
				for (Pair<ITrpShapeType, CustomTag> p : tags4Shapes) {
					CustomTag tag = p.getRight();
					if (tag != null) {
						tag.setContinued(tags4Shapes.size()>1);
						p.getLeft().getCustomTagList().addOrMergeTag(tag, addOnlyThisProperty);
					}
				}		
			}
			
			updatePageRelatedMetadata();
			getUi().getLineTranscriptionWidget().redrawText(true, false, false);
			getUi().getWordTranscriptionWidget().redrawText(true, false, false);
			refreshStructureView();
			getUi().getTaggingWidget().refreshTagsFromStorageAndCurrentSelection();
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}
	}
	
	public void updateVersionStatus(){
		if (storage.hasTranscript()) {
			ui.getStatusCombo().setText(storage.getTranscriptMetadata().getStatus().getStr());
			ui.getStatusCombo().redraw();			
		}
	}
	
	public void changeVersionStatus(String text, TrpPage page) {
		
		/*
		 * new strategy for status change
		 * 1) change the status if it makes sense and is allowed
		 * 2) if so - save as new version with the new status
		 */
		
		//is the page the one currently loaded in Transkribus
		boolean isLoaded = (page.getPageId() == Storage.getInstance().getPage().getPageId());
        //then only the latest transcript can be changed -> page.getCurrentTranscript() gives the latest of this page
		boolean isLatestTranscript = (page.getCurrentTranscript().getTsId() == Storage.getInstance().getTranscriptMetadata().getTsId());
		
		TrpTranscriptMetadata trMd = Storage.getInstance().getTranscript().getMd();
		if (trMd.getStatus().equals(EditStatus.fromString(text))){
			DialogUtil.showInfoMessageBox(getShell(), "Status stays the same", "The chosen status " + EditStatus.fromString(text) + " is the same as the old!");
			return;
		}
			
		if (isLoaded && !isLatestTranscript){
			DialogUtil.showInfoMessageBox(getShell(), "Status change not allowed", "Status change is only allowed for the latest transcript. Load the latest transcript or save the current transcript.");
			ui.getStatusCombo().setText(storage.getTranscriptMetadata().getStatus().getStr());
			ui.getStatusCombo().redraw();
			return;
			//logger.debug("page is loaded with transcript ID " + Storage.getInstance().getTranscriptMetadata().getTsId());
		}
		
		if (!AuthUtils.canTranscribe(storage.getRoleOfUserInCurrentCollection())) { // not allowed
			DialogUtil.showInfoMessageBox(getShell(), "No authorization to change status", "No authorization to change status - keep current status");
			ui.getStatusCombo().setText(storage.getTranscriptMetadata().getStatus().getStr());
			ui.getStatusCombo().redraw();
			return;
		}
		
		if(EditStatus.fromString(text).getValue() >= EditStatus.FINAL.getValue() && !AuthUtils.canManage(storage.getRoleOfUserInCurrentCollection())){
			DialogUtil.showInfoMessageBox(getShell(), "No authorization to change status", "Status 'Final' and 'GT' can only be set by editors or the owner - keep current status");
			ui.getStatusCombo().setText(storage.getTranscriptMetadata().getStatus().getStr());
			ui.getStatusCombo().redraw();
			return;
		}
		
		if (EditStatus.fromString(text).equals(EditStatus.NEW)){
			//New is only allowed for the first transcript
			DialogUtil.showInfoMessageBox(getShell(), "Status 'New' reserved for first transcript", "Only the first transcript can be 'New' - please use another status!");
			ui.getStatusCombo().setText(storage.getTranscriptMetadata().getStatus().getStr());
			ui.getStatusCombo().redraw();
			return;
		}
		
		//set new status and save as new version (includes all transcript changes as well)
		trMd.setStatus(EditStatus.fromString(text));
		ui.getThumbnailWidget().reload(true);
		saveTranscription(false);
		
		//this would show another request to the user if he wants to save - too much in my opinion
		//saveTranscriptDialogOrAutosave();
			
		

		/*
		 * 
		 * old solution: directly change the status in the database
		 * no check if the transcript has changed. Therefore the version before the transcript was saved got e.g. status GT
		 */
		
/*		int colId = Storage.getInstance().getCollId();
		//is the page the one currently loaded in Transkribus
		boolean isLoaded = (page.getPageId() == Storage.getInstance().getPage().getPageId());
        //then only the latest transcript can be changed -> page.getCurrentTranscript() gives the latest of this page
		boolean isLatestTranscript = (page.getCurrentTranscript().getTsId() == Storage.getInstance().getTranscriptMetadata().getTsId());
        
		if (isLoaded && !isLatestTranscript){
			DialogUtil.showInfoMessageBox(getShell(), "Status change not allowed", "Status change is only allowed for the latest transcript. Load the latest transcript via the 'Versions' button.");
			return;
			//logger.debug("page is loaded with transcript ID " + Storage.getInstance().getTranscriptMetadata().getTsId());
		}
		
		int pageNr = page.getPageNr();
		int docId = page.getDocId();
		
		int transcriptId = 0;
		if ((pageNr - 1) >= 0) {
			transcriptId = page.getCurrentTranscript().getTsId();
		}
		
		try {
			storage.getConnection().updatePageStatus(colId, docId, pageNr, transcriptId,
					EditStatus.fromString(text), "");

		
			if (isLoaded && isLatestTranscript){
				storage.getTranscript().getMd().setStatus(EditStatus.fromString(text));
				Storage.getInstance().reloadTranscriptsList(colId);
				if (Storage.getInstance().setLatestTranscriptAsCurrent()){
					logger.debug("latest transcript is current");
				}
				else{
					logger.debug("setting of latest transcript to current fails");
				}
				
				TrpTranscriptMetadata trMd = Storage.getInstance().getTranscript().getMd();
				
				if (trMd != null){
					//ui.getStatusCombo().add(storage.getTranscriptMetadata().getStatus().getStr());
	//					ui.getStatusCombo().add(arg0);
	//					ui.getStatusCombo().remove(arg0);
					ui.getStatusCombo().setText(trMd.getStatus().getStr());
					ui.getStatusCombo().redraw();
					logger.debug("Status: " + trMd.getStatus().getStr() + " tsid = " + trMd.getTsId());
					//SWTUtil.select(ui.getStatusCombo(), EnumUtils.indexOf(storage.getTranscriptMetadata().getStatus()));
				}
				
			}
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}

	public boolean changeVersionStatus(String text, List<TrpPage> pageList) {
		
		int r = DialogUtil.showYesNoCancelDialog(getShell(), "Change status of pages", "Do you want to update the status of the selected pages of this document with the chosen status: " + text);
		if (r != SWT.YES) {
			return false;
		}
		
		if (EditStatus.fromString(text).equals(EditStatus.NEW)){
			//New is only allowed for the first transcript
			DialogUtil.showInfoMessageBox(getShell(), "Status 'New' reserved for first transcript", "Only the first transcript can be 'New', all others must be at least 'InProgress'");
			return false;
		}
		
		Storage storage = Storage.getInstance();
		
		int colId = Storage.getInstance().getCollId();
		if (!pageList.isEmpty()) {
			
			try {
				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
					@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try{

								monitor.beginTask("Change status to "+EditStatus.fromString(text), pageList.size());
								int c=0;
								
								for (TrpPage page : pageList) {
									
									if (monitor.isCanceled()){
										storage.reloadDocWithAllTranscripts();
										return;
									}

									int pageNr = page.getPageNr();
									int docId = page.getDocId();
									
									int transcriptId = 0;
									if ((pageNr - 1) >= 0) {
										transcriptId = page.getCurrentTranscript().getTsId();
									}
									
									storage.getConnection().updatePageStatus(colId, docId, pageNr, transcriptId,
											EditStatus.fromString(text), "");
									
									monitor.subTask("Page " + ++c + "/" + pageList.size() );
									monitor.worked(c);
																	
									/*
									 * TODO: we break after first change because otherwise too slow for a batch
									 * Try to fasten this on the server side
									 */
									//break;
									// logger.debug("status is changed to : " +
									// storage.getDoc().getPages().get(pageNr-1).getCurrentTranscript().getStatus());
								}
								

//								ui.getThumbnailWidget().setTranscripts(storage.getTranscriptsSortedByDate(true, 1));
//								ui.getThumbnailWidget().updateItems();
								
								
							} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
//							} catch (NoConnectionException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

					}
				}, "Updating page status", true);
			} catch (Throwable e) {
				TrpMainWidget.getInstance().onError("Error updating page status", e.getMessage(), e, true, false);
			}
			finally{
				for (TrpPage page : pageList) {
					//reload the page in the GUI if status has changed
					if (page.getPageId() == Storage.getInstance().getPage().getPageId()) {
						reloadCurrentPage(true, null, null);
						break;
					}
				}
				//logger.debug("load remote doc thumbsddd");
				try {
					storage.reloadDocWithAllTranscripts();
				} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ui.getThumbnailWidget().reload(true);
				//storage.loadRemoteDoc(storage.getDocId(), storage.getCollId());
				updateVersionStatus();
			}
		}
		return true;

	}
	
	public boolean setVersionsAsLatest(String text, List<TrpPage> pageList) {
		
		final boolean replaceWithPreviousVersion = text.contentEquals("previous version");
		String question = replaceWithPreviousVersion? "Do you want to store previous version(s) as latest?": "Do you want to store all transcript versions with the chosen status or toolname '" + text + "' of this document as the latest versions?";
		int r = DialogUtil.showYesNoCancelDialog(getShell(), "Save versions as latest", question);
		if (r != SWT.YES) {
			return false;
		}
		
		Storage storage = Storage.getInstance();
		
		//otherwise this toolname is not unique for the complete doc
		
		final boolean searchWithPartOfToolname = text.startsWith("TRP: Synced from local plaintext file:");
		final String textPart = searchWithPartOfToolname ? "TRP: Synced from local plaintext file:" : null;
		
		int colId = Storage.getInstance().getCollId();
		if (!pageList.isEmpty()) {
			List<String> msgsNotFound = new ArrayList<String>();
			List<String> msgsIsLatest = new ArrayList<String>();
			try {
				ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
					@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try{
								String msg = replaceWithPreviousVersion? "Store previous version(s) as latest." : "Store version(s) with status or toolname "+text+" as latest.";
								monitor.beginTask(msg, pageList.size());
								int c=0;
								
								for (TrpPage page : pageList) {
									
									if (monitor.isCanceled()){
										storage.reloadDocWithAllTranscripts();
										return;
									}

									int pageNr = page.getPageNr();
									int docId = page.getDocId();
									
									TrpTranscriptMetadata transcript = null;
									if ((pageNr - 1) >= 0) {
										if (replaceWithPreviousVersion) {
											//with 1 we get the previous of the current transcript
											transcript = page.getTranscriptInCreationHistoryAtPosition(1);
										}
										else if (searchWithPartOfToolname) {
											transcript = page.getTranscriptWichContainsStringInToolnameOrNull(textPart);
										}
										else {
											transcript = page.getTranscriptWithStatusOrNull(text);
										}
										
										if (transcript == null){
											logger.debug("For page " + page.getPageNr() + " the transcript was not found" + System.lineSeparator());
											if (msgsNotFound.isEmpty()) {
												msgsNotFound.add(""+ page.getPageNr()); 
											}
											else {
												msgsNotFound.add(", " + page.getPageNr());
											}
											
											continue;
										}
										
										if (page.getCurrentTranscript().equals(transcript)) {
											logger.debug("For page " + page.getPageNr() + " the transcript with this status/toolname is already the latest" + System.lineSeparator());
											//Todo: show user message dialog with this info
											if (msgsIsLatest.isEmpty()) {
												msgsIsLatest.add(""+ page.getPageNr()); 
											}
											else {
												msgsIsLatest.add(", " + page.getPageNr());
											}
											continue;
										}
											
									}
									
									storage.getConnection().updateTranscript(colId, docId, pageNr, transcript.getStatus(), transcript.unmarshallTranscript(), transcript.getParentTsId(), transcript.getToolName());
									
									monitor.subTask("Page " + ++c + "/" + pageList.size() );
									monitor.worked(c);
																	
								}
								
								storage.reloadDocWithAllTranscripts();
								
							} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
//							} catch (NoConnectionException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

					}
				}, "Storing transcripts as latest", true);
			} catch (Throwable e) {
				TrpMainWidget.getInstance().onError("Error storing transcripts as latest", e.getMessage(), e, true, false);
			}
			finally{
				for (TrpPage page : pageList) {
					//reload the page in the GUI if status has changed
					if (page.getPageId() == Storage.getInstance().getPage().getPageId()) {
						reloadCurrentPage(true, null, null);
						break;
					}
				}
				updateVersionStatus();
			}
			if (!msgsNotFound.isEmpty() || !msgsIsLatest.isEmpty()) {
				String pageString1 = "";
				String pageString2 = "";
				
				for (String msgNotFound : msgsNotFound) {
					pageString1 += msgNotFound;
				}
				for (String msgIsLatest : msgsIsLatest) {
					pageString2 += msgIsLatest;
				}
				String info1 = "For following pages there is no transcript with the chosen status/toolname' " + text + "': " + pageString1 + System.lineSeparator();
				String info2 = "For following pages the transcript with the chosen status/toolname '" + text + "' is already the latest: " + pageString2 + System.lineSeparator();
				DialogUtil.showInfoMessageBox(getShell(), "Summary of storing transcripts as latest", info2+info1);
			}
			return true;
		}
			
		else {
			return false;
		}
		

	}
	
	public void undoJob(TrpJobStatus job) {
			
		try {
			
			String pagesStr = job.getPages();
			logger.debug("undo job for pages " + pagesStr);
			
			if (DialogUtil.showYesNoDialog(mw.getShell(), "Undo job", "Do you really want to undo this job and reset these pages: " + pagesStr )!=SWT.YES) {
				return;
			}

			Set<Integer> noReset = new HashSet<Integer>();
			Set<Integer> pageIndices = CoreUtils.parseRangeListStr(pagesStr, Integer.MAX_VALUE);
			//get doc to have all transcripts for all pages available to change
			Storage storage = Storage.getInstance();
			//to get all transcripts
			TrpDoc currDoc = storage.getRemoteDoc(storage.getCurrentDocumentCollectionId(), job.getDocId(), -1);

//			for (Integer i : pageIndices) {
//				logger.debug(" page as Integer " + i);
//			}
			
			Set<String> debugMessages = new HashSet<String>();
			
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Undo the job: affected pages: " + pageIndices.size(), pageIndices.size());
						int i = 1;
						for (TrpPage page : currDoc.getPages()) {
							monitor.subTask("Undo page " + page.getPageNr());

							int pageIdx = -1;
							if (page.getPageNr() > 0) {
								pageIdx = page.getPageNr()-1;
							}

							if (!pageIndices.contains(pageIdx)) {
								logger.debug("this page nr " + pageIdx + " is not contained in job pages!");
								continue;
							}
							else{
								logger.debug("this page nr " + pageIdx + " is contained in job pages!");
							}
											
							TrpTranscriptMetadata jobTranscript = page.getTranscriptByJobId(job.getJobIdAsInt());
							TrpTranscriptMetadata latestTranscript = page.getCurrentTranscript();
							TrpTranscriptMetadata parentTranscript = null;
							if (jobTranscript != null && latestTranscript != null && (jobTranscript.getTsId() == latestTranscript.getTsId()) ) {
								//for 'OcrModule' job no parent available - in that case use the previous transcript
								if (job.getModuleName().equals("OcrModule")) {
									//is second youngest transcript
									parentTranscript = page.getTranscriptInCreationHistoryAtPosition(1);
								}
								else {
									parentTranscript = page.getTranscriptById(jobTranscript.getParentTsId());
								}
							}
							else {
								logger.debug("No reset possible: probably no jobId in transcript or the transcript of the job is not the latest!");
								debugMessages.add("Page: " + page.getPageNr() + " - no reset possible: probably no jobId in transcript or the transcript of the job is not the latest!" + System.lineSeparator());
								noReset.add(page.getPageNr());
								continue;
							}

							//if there is a toolname != null we can revert the job of that tool
							if(parentTranscript != null){
								logger.debug("parent exists - set current version to version of parent!");
								debugMessages.add("Page: " + page.getPageNr() + " - parent exists, set current version to version of parent!" + System.lineSeparator());
								JAXBPageTranscript tr = new JAXBPageTranscript(parentTranscript);
								tr.build();
								storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), parentTranscript.getDocId(), parentTranscript.getPageNr(), parentTranscript.getStatus(), tr.getPageData(), parentTranscript.getParentTsId(), "job undone: job ID " + job.getJobId());
								
							}
							else {
								if (parentTranscript == null) {
									logger.debug("no parent/predecessor found - no reset!!");
									debugMessages.add("Page: " + page.getPageNr() + " - no parent/predecessor found, no reset!" + System.lineSeparator());
									noReset.add(page.getPageNr());
									continue;
								}
							}
							
							//for loaded page do a reload: does not work since because it runs in a separate thread and there are some resource conflicts
//							if (storage.getPage().getPageId() == page.getPageId()){
//								logger.debug("page id = " + storage.getPage().getPageId());
//								Storage.getInstance().setLatestTranscriptAsCurrent();
//								mw.reloadCurrentPage(true);
//							}

							if (monitor.isCanceled())
								throw new InterruptedException();

							monitor.worked(i + 1);
							++i;
						}
					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Undo selected job", true);
			
			String message = noReset.size() > 0? "Not all pages of the job (" + pageIndices.size() + " pages) could be undone" : "All pages of the job were undone";
			String detailMessage = noReset.size() > 0? "The following pages of the job could not be undone: " + CoreUtils.getRangeListStrFromSet(noReset) : "All pages of the job were undone - in total " + pageIndices.size() + " pages.";
			//TrpMessageDialog.showInfoDialog(getShell(), "Undo feedback", message, detailMessage, null);
			detailMessage = detailMessage.concat(System.lineSeparator());
			
			Iterator it = debugMessages.iterator();
			while (it.hasNext()) {
				detailMessage += it.next();
			}
			
//			for (int i = 0; i < 1000; i++) {
//				detailMessage += "test " +i + System.lineSeparator();
//			}
			
			TrpMessageDialog.showInfoDialog(getShell(), "Undo feedback", message, detailMessage, null);
			
			try {
				storage.reloadDocWithAllTranscripts();
				reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
					if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
						autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
					}
					getCanvas().fitWidth();
					//adjustReadingOrderDisplayToImageSize();				
				}, null);

			} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//DialogUtil.showInfoMessageBox(getShell(), "Undo feedback", detailMessage);
			//DialogUtil.showCustomMessageDialog(getShell(), "Undo feedback", detailMessage, null, SWT.ICON_INFORMATION | SWT.V_SCROLL, new String[] {"OK"}, 0, null);
			
			//DialogUtil.showInfoMessageBox(getShell(), "Undo feedback", detailMessage);
			
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{

		}
	}

	public void revertVersions() {
		
		try {
			//get doc to have all transcripts for all pages available to change
			Storage storage = Storage.getInstance();
			TrpDoc currDoc = Storage.getInstance().getRemoteDoc(storage.getCurrentDocumentCollectionId(), storage.getDocId(), -1);
			for (TrpPage page : currDoc.getPages()) {
				
				TrpTranscriptMetadata currTranscript = page.getCurrentTranscript();
				TrpTranscriptMetadata parentTranscript = page.getTranscriptById(currTranscript.getParentTsId());
				
				/*
				 * workaround as long t2i does not save the parent transcript in the transcript metadata
				 */
				if(currTranscript.getToolName() != null && currTranscript.getToolName().equals("T2I")){
					parentTranscript = page.getTranscriptWithStatus("New");
					logger.debug("parent transcript for T2I found: " + parentTranscript.getTsId());
				}

				//logger.debug("currTranscript.getToolName(): " + currTranscript.getToolName());
				
				//if there is a toolname != null we can revert the job of that tool
				if(parentTranscript != null && currTranscript.getToolName() != null){
					logger.debug("parent exists and transcript stems from tool/job - revert version");
					JAXBPageTranscript tr = new JAXBPageTranscript(parentTranscript);
					tr.build();
					storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), parentTranscript.getDocId(), parentTranscript.getPageNr(), parentTranscript.getStatus(), tr.getPageData(), parentTranscript.getParentTsId(), "resetted as current");
				}
				
				//for loaded page do a reload: does not work since because it runs in a separate thread and there are some resource conflicts
//				if (storage.getPage().getPageId() == page.getPageId()){
//					logger.debug("page id = " + storage.getPage().getPageId());
//					Storage.getInstance().setLatestTranscriptAsCurrent();
//					mw.reloadCurrentPage(true);
//				}
			}
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void syncTextRegionTextWithLines() {
		
		try {
			
			logger.debug("sync text on region level with lines - match only if nr. of lines in image corresponds with number of lines in text!");
			
			if (DialogUtil.showYesNoDialog(mw.getShell(), "Sync text", "Do you really want to sync text on region level with recogniced lines?")!=SWT.YES) {
				return;
			}

			Storage storage = Storage.getInstance();
			TrpDoc currDoc = storage.getDoc();	
			List<TrpPage> pages = currDoc.getPages();

			Set<String> debugMessages = new HashSet<String>();
			Set<String> mainMessage = new HashSet<String>();
			
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Sync region text with lines: " + pages.size(), pages.size());
						int i = 1;
						
						int matchedLines = 0;
						int totalLines = 0;
						int matchedLinesDoc = 0;
						int totalLinesDoc = 0;
						
						for (TrpPage page : pages) {
							
							TrpTranscriptMetadata md = page.getCurrentTranscript();
							int parentId = md.getTsId();
											
							PcGtsType pcGtsType =  md.unmarshallTranscript();
							
							for (TrpRegionType currRegion : pcGtsType.getPage().getTextRegionOrImageRegionOrLineDrawingRegion()) {
								if (currRegion instanceof TrpTextRegionType) { 
									matchedLines += PageXmlUtils.applyTextToLinesForRegion(currRegion, currRegion.getUnicodeText());
									String lines[] = currRegion.getUnicodeText().split("\\r?\\n");
									totalLines += lines.length;
									
								}
							}
							
							storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md.getDocId(), md.getPageNr(), EditStatus.IN_PROGRESS, pcGtsType, parentId, "sync region text to lines", "match only similar nr of lines");

							if (monitor.isCanceled())
								throw new InterruptedException();
							
							debugMessages.add("Pagenumber: " + page.getPageNr() + ": From " + totalLines + " total lines the following amount was matched: " +  matchedLines);

							monitor.worked(i + 1);
							++i;
							
							matchedLinesDoc += matchedLines;
							totalLinesDoc += totalLines;
							matchedLines = 0;
							totalLines = 0;
						}
						
						mainMessage.add("For this doc we could match " + matchedLinesDoc + " lines from the total amount of " + totalLinesDoc);
						

					} catch (InterruptedException ie) {
						throw ie;
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Sync region text with lines", true);
			
			String message = "Here is the sync statistic for this document";
			message += System.lineSeparator();
			message += mainMessage.iterator().next();
			String detailMessage = "--------Statistic for each page--------";
			detailMessage = detailMessage.concat(System.lineSeparator());
			
			Iterator it = debugMessages.iterator();
			while (it.hasNext()) {
				detailMessage += it.next();
				detailMessage += System.lineSeparator();
			}
			
//			for (int i = 0; i < 1000; i++) {
//				detailMessage += "test " +i + System.lineSeparator();
//			}
			
			TrpMessageDialog.showInfoDialog(getShell(), "Sync feedback", message, detailMessage, null);
			
			try {
				storage.reloadDocWithAllTranscripts();
				reloadCurrentPage(true, true, CanvasAutoZoomMode.FIT_WIDTH, () -> {
					if (getTrpSets().getAutoSaveEnabled() && getTrpSets().isCheckForNewerAutosaveFile()) {
						autoSaveController.checkForNewerAutoSavedPage(storage.getPage());
					}
					getCanvas().fitWidth();
					//adjustReadingOrderDisplayToImageSize();				
				}, null);

			} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//DialogUtil.showInfoMessageBox(getShell(), "Undo feedback", detailMessage);
			//DialogUtil.showCustomMessageDialog(getShell(), "Undo feedback", detailMessage, null, SWT.ICON_INFORMATION | SWT.V_SCROLL, new String[] {"OK"}, 0, null);
			
			//DialogUtil.showInfoMessageBox(getShell(), "Undo feedback", detailMessage);
			
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{

		}
	}
	
	public void syncTextRegionTextWithLinesSimple(){
		
		try {
			//get doc to have all transcripts for all pages available to change
			Storage storage = Storage.getInstance();
			TrpDoc currDoc = storage.getDoc();

			for (TrpPage page : currDoc.getPages()) {
				
				TrpTranscriptMetadata md = page.getCurrentTranscript();
				int parentId = md.getTsId();
								
				PcGtsType pcGtsType =  md.unmarshallTranscript();
				
				for (TrpRegionType currRegion : pcGtsType.getPage().getTextRegionOrImageRegionOrLineDrawingRegion()) {
					if (currRegion instanceof TrpTextRegionType) { 
						PageXmlUtils.applyTextToLinesForRegion(currRegion, currRegion.getUnicodeText());
					}
				}
				
				storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md.getDocId(), md.getPageNr(), EditStatus.IN_PROGRESS, pcGtsType, parentId, "sync region text to lines", "match only similar nr of lines");
				
			}
		} catch (ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SessionExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

	public void searchReplace(){
		
		try {
			String lastTxtFileSyncFolder = null;
			logger.debug("replace chars with new chars");
			
			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}

			String fn = DialogUtil.showOpenFileDialog(getShell(), "Choose the folder with the mappings", lastTxtFileSyncFolder, new String[]{"*.txt","*.csv"});
			if (fn == null) {
				return;
			}
			
			//dialog page chooser
			PageSelectorDialog d = new PageSelectorDialog(getShell(), false);
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			Set<Integer> pageIndices = d.getPageIndices();
			
			// store current location 
			lastTxtFileSyncFolder = fn;
			
			File mappings = new File(fn);
			//get doc to have all transcripts for all pages available to change
			Storage storage = Storage.getInstance();
			TrpDoc currDoc = storage.getDoc();

			for (TrpPage page : currDoc.getPages()) {
				
				if (!pageIndices.contains(page.getPageNr()-1)) {
					continue;
				}
				
				logger.debug("replace strings on page: " + page.getPageNr());
				
				TrpTranscriptMetadata md = page.getCurrentTranscript();
				int parentId = md.getTsId();
								
				PcGtsType pcGtsType =  md.unmarshallTranscript();
				TrpPageType trpPage = (TrpPageType) pcGtsType.getPage();
				for (TrpTextLineType l : trpPage.getLines()) {
					//was a experiment for Hertziana -> was not really successful
					//TrpShapeTypeUtils.replaceWordEndings(l, mappings);
					
					/*
					 * now we replace chars or strings with replacements
					 */
					TrpShapeTypeUtils.replaceChars(l, mappings);
				}
				
				storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md.getDocId(), md.getPageNr(), md.getStatus(), pcGtsType, parentId, "search_replace");
				storage.getTranscript().setPageData(pcGtsType);
				mw.reloadCurrentPage(true, null, null);		
				
			}
		} catch (ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SessionExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/*
	 * helper method to take over diffs in two versions like e.g. custom attribute
	 * 
	 * --not integrated into GUI yet
	 */
	public void convertDiffsIntoCustomTags() {
		try {

			logger.debug("convert text diffs into abbrev tag!");
			
			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}

			//dialog page chooser
			PageSelectorDialog d = new PageSelectorDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			Set<Integer> pageIndices = d.getPageIndices();
			
			Storage storage = Storage.getInstance();
			storage.reloadDocWithAllTranscripts();
			TrpDoc currDoc = storage.getDoc();
			
			for (TrpPage page : currDoc.getPages()) {
				
				if (!pageIndices.contains(page.getPageNr()-1)) {
					continue;
				}
				
				logger.debug("convert text diffs on page: " + page.getPageNr());
				
				TrpTranscriptMetadata md1 = page.getTranscriptWithStatus(EditStatus.GT);
				TrpTranscriptMetadata md2 = page.getTranscriptWichContainsStringInToolnameOrNull("Noscemus");
				int parentId = md1.getTsId();
								
				PcGtsType pcGtsType1 =  md1.unmarshallTranscript();
				PcGtsType pcGtsType2 =  md2.unmarshallTranscript();
				
				TrpPageType trpPage1 = (TrpPageType) pcGtsType1.getPage();
				TrpPageType trpPage2 = (TrpPageType) pcGtsType2.getPage();
				
				List<TrpTextLineType> lines1 = trpPage1.getLines();
				List<TrpTextLineType> lines2 = trpPage2.getLines();
				
				for (int i = 0; i<trpPage1.getLines().size(); i++) {
					
					TrpTextLineType l1 = lines1.get(i);
					TrpTextLineType l2 = lines2.get(i);
					
					TrpShapeTypeUtils.convertDifferences(l1, l2);
					
					//trpPage1.setEdited(true);
				}
				
				TrpPageType trpPage3 = (TrpPageType) pcGtsType1.getPage();
				
				
				for (TrpTextLineType l : trpPage3.getLines() ) {
					logger.debug("line: " + l.getCustomTagList());
				}
				
				storage.getTranscript().setPageData(pcGtsType1);
				storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md1.getDocId(), md1.getPageNr(), EditStatus.IN_PROGRESS, pcGtsType1, parentId, "");
//				
//				
//				//
//				storage.reloadDocWithAllTranscripts();
				mw.reloadCurrentPage(true, null, null);		
				
			}
		} catch (ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	/*
	 * helper method to get a new sorting of lines: 
	 * 	sorting is done with YX coordinates except for lines on the same horizontal line
	 * 	or columnwise: means if there are several columns sorting is done first column 1-n, second column n-m, ....
	 * 
	 * --integrated in GUI via TrpMenuBar (main menu - Document)
	 * ToDo: monitor to observe the progress
	 */
	public void applyNewReadingOrder(boolean columnwise) {
		try {

			logger.debug("Sort lines according to Y coordinate except lines on the same horizontal line - sort with X coordinate!");
			
			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}

			//dialog page chooser
			PageSelectorDialog d = new PageSelectorDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			Set<Integer> pageIndices = d.getPageIndices();
			
			boolean rtl = d.getRtl();
			boolean doRegions = d.isDoRegions();
			
			Storage storage = Storage.getInstance();
			storage.reloadDocWithAllTranscripts();
			TrpDoc currDoc = storage.getDoc();
			
			final List<String> error = new ArrayList<>();
			try {
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					
					try {
						monitor.beginTask("New sorting of shapes", pageIndices.size());
						int i = 0;
						for (TrpPage page : currDoc.getPages()) {
							
							if (monitor.isCanceled())
								throw new InterruptedException();

								if (!pageIndices.contains(page.getPageNr()-1)) {
									continue;
								}
								
								logger.debug("sorting on page: "+page.getPageNr());
								
								logger.debug("apply new reading order to page: " + page.getPageNr());
								
								TrpTranscriptMetadata md1 = page.getCurrentTranscript();
								int parentId = md1.getTsId();
												
								PcGtsType pcGtsType1 =  md1.unmarshallTranscript();
								
								TrpPageType trpPage1 = (TrpPageType) pcGtsType1.getPage();
								
								List<TrpTextRegionType> regions = trpPage1.getTextRegions(false);
								
								if (doRegions) {
									if (columnwise) {
										logger.debug("do regions columnwise: " + page.getPageNr());
										//printRegionOrdering(trpPage1.getTextRegionOrImageRegionOrLineDrawingRegion());
										TrpShapeTypeUtils.sortShapesByCoordinates_columnwise(trpPage1.getTextRegionOrImageRegionOrLineDrawingRegion(),!rtl);
										//printRegionOrdering(trpPage1.getTextRegionOrImageRegionOrLineDrawingRegion());
									}
									else {
										TrpShapeTypeUtils.sortShapesByCoordinates_XIfYOverlaps_OtherwiseY(trpPage1.getTextRegionOrImageRegionOrLineDrawingRegion(),!rtl);
									}
									trpPage1.setRoAccordingToListOrder();
									
								}
								else {
									for (int j = 0; j<regions.size(); j++) {
									
										TrpRegionType r1 = regions.get(j);
										if (columnwise) {
											//TrpShapeTypeUtils.sortShapesByCoordinates_columnwise(((TrpPageType) trpPage1).getTextRegionOrImageRegionOrLineDrawingRegion(),!rtl);
											TrpShapeTypeUtils.sortShapesByCoordinates_columnwise(((TrpTextRegionType) r1).getTrpTextLine(),!rtl);
										}
										else {
											TrpShapeTypeUtils.sortShapesByCoordinates_XIfYOverlaps_OtherwiseY(((TrpTextRegionType) r1).getTrpTextLine(),!rtl);
										}
										((TrpTextRegionType) r1).setRoAccordingToListOrder();
										r1.reInsertIntoParent();
										
									}
									
								}
								trpPage1.setMd(md1);
								pcGtsType1.setPage(trpPage1);
//								storage.getTranscript().setPageData(pcGtsType1);
								//storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md1.getDocId(), md1.getPageNr(), EditStatus.IN_PROGRESS, pcGtsType1, parentId, "new line sorting");
								storage.saveTranscript(storage.getCurrentDocumentCollectionId(), trpPage1, EditStatus.IN_PROGRESS, parentId, "new line sorting");
								monitor.worked(++i);
							}

					}
					catch (InterruptedException ie) {
						throw ie;
					} catch (Throwable e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}

				private void printRegionOrdering(List<TrpRegionType> textRegionOrImageRegionOrLineDrawingRegion) {
						for (TrpRegionType r : textRegionOrImageRegionOrLineDrawingRegion) {
							logger.debug("get region ID: " + r.getId());
						}
				}
			}, "Resorting shapes", true);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e) {}
			catch (Throwable e) {
				onError("Unexpected error", e.getMessage(), e);
			}
			
			mw.reloadCurrentPage(true, null, null);
		

		} catch (ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	 * use case Bandkatalog
	 * custom tags: structures were destroyed
	 * 	-> copy them from an older version into the newest one
	 */
	public void copyCustomTagsFromLineIntoLatestVersion() {
		try {

			logger.debug("copyCustomTagsFromLineIntoLatestVersion()!");
			
			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}

			//dialog page chooser
			PageSelectorDialog d = new PageSelectorDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			Set<Integer> pageIndices = d.getPageIndices();
			
			Storage storage = Storage.getInstance();
			
			//to be able to use the versions history
			storage.reloadDocWithAllTranscripts();
			TrpDoc currDoc = storage.getDoc();

			for (TrpPage page : currDoc.getPages()) {
				
				if (!pageIndices.contains(page.getPageNr()-1)) {
					continue;
				}
				
				logger.debug("copy custom: " + page.getPageNr());
				
				
				TrpTranscriptMetadata md1 = page.getCurrentTranscript();
				TrpTranscriptMetadata md2 = page.getTranscriptWichContainsStringInToolnameOrNull("ULB_Bandkatalog");
				
				if (md1 == null || md2 == null) {
					logger.debug("md is null????");
					System.in.read();
				}
				
				int parentId = md1.getTsId();
								
				PcGtsType pcGtsType1 =  md1.unmarshallTranscript();
				PcGtsType pcGtsType2 =  md2.unmarshallTranscript();
				
				TrpPageType trpPage1 = (TrpPageType) pcGtsType1.getPage();
				TrpPageType trpPage2 = (TrpPageType) pcGtsType2.getPage();
				
				List<TrpTextLineType> lines1 = trpPage1.getLines();
				List<TrpTextLineType> lines2 = trpPage2.getLines();
				
				for (int i = 0; i<trpPage1.getLines().size(); i++) {
					
					TrpTextLineType l1 = lines1.get(i);
					TrpTextLineType l2 = lines2.get(i);
					
					TrpShapeTypeUtils.copyCustomTags(l1, l2);
					
					//trpPage1.setEdited(true);
				}
				
				storage.getTranscript().setPageData(pcGtsType1);
				storage.getConnection().updateTranscript(storage.getCurrentDocumentCollectionId(), md1.getDocId(), md1.getPageNr(), md1.getStatus(), pcGtsType1, parentId, "");
//				
//				
//				//
//				storage.reloadDocWithAllTranscripts();
				//mw.reloadCurrentPage(true, null, null);		
				
			}
		} catch (ServerErrorException | ClientErrorException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void setStructureTypeOfSelected(String structType, boolean recursive) {
		List<ICanvasShape> selected = getCanvas().getScene().getSelectedAsNewArray();
		logger.debug("applying structure type to selected, n = "+selected.size()+" structType: "+structType);
//		TextTypeSimpleType struct = EnumUtils.fromValue(TextTypeSimpleType.class, mw.getRegionTypeCombo().getText());
//		String struct = mw.getStructureType();	
		for (ICanvasShape sel : selected) {
			ITrpShapeType st = GuiUtil.getTrpShape(sel);
			logger.debug("updating struct type for " + sel+" type = "+structType+", TrpShapeType = "+st);
			
			if (st != null) {
				st.setStructure(structType, recursive, mw);
			}
		}
		
		refreshStructureView();
		redrawCanvas();
	}
	
	/*
	 * In case the StructureTag also contains e.g. article ID this method is needed to keep that attribute instead of 
	 * the above method
	 */
	public void setStructureTypeOfSelected(StructureTag st, boolean recursive) {
		List<ICanvasShape> selected = getCanvas().getScene().getSelectedAsNewArray();
		logger.debug("applying structure type to selected, n = "+selected.size()+" structType: "+st.getType());	
		for (ICanvasShape sel : selected) {
			ITrpShapeType shape = GuiUtil.getTrpShape(sel);
			
			if (!(shape instanceof TrpTextLineType)){
				continue;
			}
			logger.debug("updating struct type for " + sel+" type = "+st.getType()+", TrpShapeType = "+shape);
			
			if (shape != null && shape.getCustomTagList() != null) {
				shape.getCustomTagList().addOrMergeTag(st, null);
			}
			
		}
		
		refreshStructureView();
		redrawCanvas();
		
	}

	public JavaInfo getJavaInfo() {
		return javaInfo;
	}

	public String getTextDifferenceOfVersions(boolean withLineNrs) throws NullValueException, JAXBException {
		TrpTranscriptMetadata ref = (TrpTranscriptMetadata) ui.getToolsWidget().getCorrectText();
		TrpTranscriptMetadata hyp = (TrpTranscriptMetadata) ui.getToolsWidget().getHpothesisText();

		ArrayList<String> refText = new ArrayList<String>();
		ArrayList<String> hypText = new ArrayList<String>();

		if (ref != null && hyp != null) {
			TrpPageType refPage = (TrpPageType) ref.unmarshallTranscript().getPage();
			TrpPageType hypPage = (TrpPageType) hyp.unmarshallTranscript().getPage();			
			
			int i = 1;
			int j = 1;
			boolean containsLines = false;

			for (TrpRegionType region : refPage.getRegions()) {
				if (region instanceof TrpTextRegionType) {
					List<TextLineType> lines = ((TrpTextRegionType) region).getTextLine();
					if (lines == null || lines.size() == 0){
						String lineString = (withLineNrs ? "<i>" + j + "-" + i + " #</i> " : "") + "";
						refText.add(lineString);
						i++;
					}
					for (TextLineType line : lines) {
						//containsLines = true;
						String lineString = (withLineNrs ? "<i>" + j + "-" + i + " #</i> " : "") + ((TrpTextLineType) line).getUnicodeText();
						refText.add(lineString);
						// refText = refText.concat(region.getUnicodeText());
						i++;
					}
					j++;
//					if (containsLines){
//						j++;
//						containsLines = false;
//					}
				}

				if (region instanceof TrpTableRegionType) {
					for (TableCellType cell : ((TrpTableRegionType) region).getTableCell()) {
						for (TextLineType line : cell.getTextLine()) {
							refText.add(((TrpTextLineType) line).getUnicodeText());
						}
					}
				}
				i = 1;
			}
			i = 1;
			j = 1;
			containsLines = false;
			
			for (TrpRegionType region : hypPage.getRegions()) {
				if (region instanceof TrpTextRegionType) {
					List<TextLineType> lines = ((TrpTextRegionType) region).getTextLine();
					if (lines == null || lines.size() == 0){
						String lineString = (withLineNrs ? "<i>" + j + "-" + i + " #</i> " : "") + "";
						hypText.add(lineString);
						i++;
					}
					for (TextLineType line : lines) {
						//containsLines = true;
						String lineString = (withLineNrs ? "<i>" + j + "-" + i + " #</i> " : "") + ((TrpTextLineType) line).getUnicodeText();
						hypText.add(lineString);
						// hypText = hypText.concat(region.getUnicodeText());
						i++;
					}
					j++;
//					if (containsLines){
//						j++;
//						containsLines = false;
//					}
				}
				if (region instanceof TrpTableRegionType) {
					for (TableCellType cell : ((TrpTableRegionType) region).getTableCell()) {
						for (TextLineType line : cell.getTextLine()) {
							hypText.add(((TrpTextLineType) line).getUnicodeText());
						}
					}
				}
				i = 1;
			}

			DiffCompareTool diff = new DiffCompareTool(mw.getShell().getDisplay(), hypText, refText);
			return diff.getResult();
		}
		return "";
	}

	public void checkSession(boolean showLoginDialogOnSessionExpiration) {
		try {
			if (storage.isLoggedIn()) {
				storage.getConnection().checkSession();
			}
		} catch (SessionExpiredException e) {
			storage.logout();
			if (showLoginDialogOnSessionExpiration) {
				loginDialog("Session Expired!");
			}
		}
		
	}
	
//	public void trainP2PaLAModel() {
//		try {
//			logger.debug("p2palaTrainBtn pressed...");
//			if (!storage.getConnection().isUserAllowedForJob(JobImpl.P2PaLATrainJob.toString())) {
//				DialogUtil.showErrorMessageBox(getShell(), "Not allowed!", "You are not allowed to start a P2PaLA training.\n If you are interested, please apply at email@transkribus.eu");
//				return;
//			}
//			P2PaLATrainDialog d = new P2PaLATrainDialog(getShell());
//			if (d.open() == IDialogConstants.OK_ID) {
//				P2PaLATrainUiConf conf = d.getConf();
//				if (conf==null) {
//					return;
//				}
//				P2PaLATrainJobPars jobPars = conf.toP2PaLATrainJobPars();
//				String jobId = storage.getConnection().trainP2PaLAModel(storage.getCurrentDocumentCollectionId(), jobPars);
//				logger.info("Started P2PaLA training job "+jobId);
//				registerJobStatusUpdateAndShowSuccessMessage(jobId);
//			}
//		} catch (Exception e) {
//			mw.onError("Error starting P2PaLA training", e.getMessage(), e);
//		}
//	}

	public void registerJobStatusUpdateAndShowSuccessMessage(String... jobIds) {
		if (!CoreUtils.isEmpty(jobIds)) {
			logger.debug("started " + jobIds.length + " jobs");
			String jobIdsStr = mw.registerJobsToUpdate(jobIds);
			storage.sendJobListUpdateEvent();
			mw.updatePageLock();

			String jobsStr = jobIds.length > 1 ? "jobs" : "job";
			final String title = jobIds.length + " " + jobsStr + " started!";
			final String msg = "IDs:\n " + jobIdsStr;
			
			//Dialog may block the GUI. 
//			DialogUtil.showInfoMessageBox(tw.getShell(), title, msg);
			
			//show balloon tip on jobs button instead
			DialogUtil.showBalloonToolTip(mw.getUi().getJobsButton(), null, title, msg);
		}
	}

	public void duplicateGtToDocument(int sourceColId, TrpCollection targetCol, GroundTruthSelectionDescriptor desc, String title) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		List<GroundTruthSelectionDescriptor> descList = new ArrayList<>(1);
		descList.add(desc);
		String jobId = storage.getConnection().duplicateGtToDocument(sourceColId, targetCol.getColId(), descList, title, null);
		registerJobStatusUpdateAndShowSuccessMessage(jobId);
	}
	
	public List<String> getSelectedRegionIds() {
		List<String> rids = new ArrayList<>();
		for (ICanvasShape s : canvas.getScene().getSelectedAsNewArray()) {
			ITrpShapeType st = GuiUtil.getTrpShape(s);
			if (st == null || !(st instanceof TrpTextRegionType)) {
				continue;
			}
			rids.add(st.getId());
		}
		return rids;
	}
	
	public void openCreditManager(TrpCollection collection) {
		if (creditManagerDialog != null) {
			creditManagerDialog.setVisible();
		} else {
			creditManagerDialog = new CreditManagerDialog(mw.getShell(), collection, storage.isAdminLoggedIn());
			if (creditManagerDialog.open() == IDialogConstants.OK_ID) {
				//we don't need feedback here. do nothing
			}
			creditManagerDialog = null;
		}
	}


}
