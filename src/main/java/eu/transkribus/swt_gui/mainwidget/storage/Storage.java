package eu.transkribus.swt_gui.mainwidget.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.security.auth.login.LoginException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLPropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dea.fimagestore.core.beans.ImageMetadata;
import org.dea.fimgstoreclient.FimgStoreGetClient;
import org.dea.fimgstoreclient.beans.ImgType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.itextpdf.text.DocumentException;
import eu.transkribus.client.connection.TrpServerConn;
import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.exceptions.NullValueException;
import eu.transkribus.core.io.DocExporter;
import eu.transkribus.core.io.LocalDocConst;
import eu.transkribus.core.io.LocalDocWriter;
import eu.transkribus.core.io.UnsupportedFormatException;
import eu.transkribus.core.io.util.ExtensionFileFilter;
import eu.transkribus.core.model.beans.CITlabLaTrainConfig;
import eu.transkribus.core.model.beans.CitLabHtrTrainConfig;
import eu.transkribus.core.model.beans.CitLabSemiSupervisedHtrTrainConfig;
import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.DocumentSelectionDescriptor;
import eu.transkribus.core.model.beans.DocumentSelectionDescriptor.PageDescriptor;
import eu.transkribus.core.model.beans.EdFeature;
import eu.transkribus.core.model.beans.EdOption;
import eu.transkribus.core.model.beans.JAXBPageTranscript;
import eu.transkribus.core.model.beans.PageLock;
import eu.transkribus.core.model.beans.PyLaiaHtrTrainConfig;
import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TrpAction;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpCreditCosts;
import eu.transkribus.core.model.beans.TrpCrowdProject;
import eu.transkribus.core.model.beans.TrpCrowdProjectMessage;
import eu.transkribus.core.model.beans.TrpCrowdProjectMilestone;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocDir;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpEntityAttribute;
import eu.transkribus.core.model.beans.TrpErrorRateResult;
import eu.transkribus.core.model.beans.TrpEvent;
import eu.transkribus.core.model.beans.TrpGroundTruthPage;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.TrpUpload;
import eu.transkribus.core.model.beans.auth.TrpRole;
import eu.transkribus.core.model.beans.auth.TrpUserLogin;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory;
import eu.transkribus.core.model.beans.customtags.CustomTagUtil;
import eu.transkribus.core.model.beans.customtags.StructureTag;
import eu.transkribus.core.model.beans.enums.DataSetType;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.model.beans.enums.SearchType;
import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.core.model.beans.job.enums.JobTask;
import eu.transkribus.core.model.beans.pagecontent.TextTypeSimpleType;
import eu.transkribus.core.model.beans.pagecontent.TranskribusMetadataType;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPageType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.model.beans.rest.ParameterMap;
import eu.transkribus.core.model.beans.searchresult.FulltextSearchResult;
import eu.transkribus.core.model.builder.CommonExportPars;
import eu.transkribus.core.model.builder.ExportCache;
import eu.transkribus.core.model.builder.alto.AltoExporter;
import eu.transkribus.core.model.builder.pdf.PdfExportPars;
import eu.transkribus.core.model.builder.pdf.PdfExporter;
import eu.transkribus.core.model.builder.tei.ATeiBuilder;
import eu.transkribus.core.model.builder.tei.TeiExportPars;
import eu.transkribus.core.model.builder.tei.TrpTeiStringBuilder;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.DescriptorUtils;
import eu.transkribus.core.util.Event;
import eu.transkribus.core.util.MonitorUtil;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.core.util.ProxyUtils;
import eu.transkribus.core.util.SebisStopWatch;
import eu.transkribus.core.util.UnicodeList;
import eu.transkribus.core.util.SebisStopWatch.SSW;
import eu.transkribus.swt.util.AsyncExecutor;
import eu.transkribus.swt.util.AsyncExecutor.AsyncCallback;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.TrpGuiPrefs;
import eu.transkribus.swt_gui.TrpGuiPrefs.ProxyPrefs;
import eu.transkribus.swt_gui.canvas.CanvasImage;
import eu.transkribus.swt_gui.canvas.shapes.ICanvasShape;
import eu.transkribus.swt_gui.htr.treeviewer.HtrGroundTruthContentProvider.HtrGtDataSet;
import eu.transkribus.swt_gui.htr.treeviewer.HtrGroundTruthContentProvider.TrpHtrGtDocMetadata;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.ModelGtDataSet;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.TrpModelGtDocMetadata;
import eu.transkribus.swt_gui.mainwidget.ImageDataDacheFactory;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettings;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.BeforeTranscriptSaveEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.CollectionsLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.DocListLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.DocLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.DocMetadataUpdateEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.GroundTruthLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.JobUpdateEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.LoginOrLogoutEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.MainImageLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.PageLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.StructTagSpecsChangedEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.TagSpecsChangedEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.TranscriptListLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.TranscriptLoadEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.TranscriptSaveEvent;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener.UserDocListLoadEvent;
import eu.transkribus.swt_gui.metadata.CustomTagSpec;
import eu.transkribus.swt_gui.metadata.CustomTagSpecDBUtil;
import eu.transkribus.swt_gui.metadata.CustomTagSpecUtil;
import eu.transkribus.swt_gui.metadata.StructCustomTagSpec;
import eu.transkribus.swt_gui.metadata.TaggingWidgetUtils;
import eu.transkribus.util.CheckedConsumer;
import eu.transkribus.util.DataCache;
import eu.transkribus.util.DataCacheFactory;
import eu.transkribus.util.MathUtil;
import eu.transkribus.util.OcrConfig;
import eu.transkribus.util.RecognitionPreferences;
import eu.transkribus.util.TextRecognitionConfig;
import eu.transkribus.util.Utils;

// ULB start
import de.ulb.gtscribus.core.io.LocalDocReader;
import de.ulb.gtscribus.core.io.LocalDocReader.DocLoadConfig;
import de.ulb.gtscribus.swt_gui.mainwidget.storage.ULBLocalDocExporter;
// ULB end


/** Singleton class that contains all data related to loading a transcription */
public class Storage {
	private final static Logger logger = LoggerFactory.getLogger(Storage.class);	
	
	public static final File VK_XML = new File("virtualKeyboards.xml");
	public static final String VK_SHORTCUT_PROP_PREFIX = "__Shortcut.";
	private XMLPropertiesConfiguration vkConf;

	final int N_IMAGES_TO_PRELOAD_PREVIOUS = 0;
	final int N_IMAGES_TO_PRELOAD_NEXT = 0;
	
//	final int N_IMAGES_TO_PRELOAD_PREVIOUS = 1;
//	final int N_IMAGES_TO_PRELOAD_NEXT = 1;	

	// public final static String LOGIN_OR_LOGOUT_EVENT =
	// "LOGIN_OR_LOGOUT_EVENT";

	private static Storage storage = null;

	// private int currentTranscriptIndex = 0;

	private List<TrpDocMetadata> docList = Collections.synchronizedList(new ArrayList<>());
	private List<TrpDocMetadata> deletedDocList = Collections.synchronizedList(new ArrayList<>());
	private List<TrpDocMetadata> userDocList = Collections.synchronizedList(new ArrayList<>());
	
	private List<CustomTagSpec> customTagSpecs = new ArrayList<>();
	private List<CustomTagSpec> collectionSpecificTagSpecs = new ArrayList<>();
	private List<StructCustomTagSpec> structCustomTagSpecs = new ArrayList<>();
	private Map<String, Pair<Integer, String>> virtualKeysShortCuts = new HashMap<>();
	
	private int collId;

	private TrpDoc doc = null;
	private TrpPage page = null;
	private boolean isPageLocked = false;

	private JAXBPageTranscript transcript = new JAXBPageTranscript();
	private TrpTextRegionType regionObject = null;
	private TrpTextLineType lineObject = null;
	private TrpWordType wordObject = null;

	// TextLineType currentLineObject = null;

	// TrpTranscriptMetadata currentTranscriptMetadata = null;

	private CanvasImage currentImg;
	String[][] wgMatrix = new String[][] {};

	private TrpServerConn conn = null;
	private TrpUserLogin user = null;
	
	private List<TrpCollection> collections = Collections.synchronizedList(new ArrayList<>());

//	private static DocJobUpdater docUpdater;
	private DataCache<URL, CanvasImage> imCache;
	
	public static final boolean USE_TRANSCRIPT_CACHE = false;
	private DataCache<TrpTranscriptMetadata, JAXBPageTranscript> transcriptCache;
	
	ImageMetadata imgMd;
	
//	private int currentColId = -1;
	
	Set<IStorageListener> listener = new HashSet<>();
	
	// just for debugging purposes:
	private static int reloadDocListCounter=0;
	private static int reloadHtrListCounter=0;
	
//	private List<TrpP2PaLA> p2palaModels = new ArrayList<>();
//	private List<TrpHtr> htrList = new ArrayList<>();
	
	private List<TrpCreditCosts> creditCostsList;
	
	private AsyncExecutor loadPageAsyncExecutor = new AsyncExecutor();
	private AsyncExecutor loadTranscriptAsyncExecutor = new AsyncExecutor();

	public Storage(String someString) {}
	
	public static class StorageException extends Exception {
		private static final long serialVersionUID = -2215354890031208420L;

		public StorageException(String message) {
			super(message);
		}
		
		public StorageException(String message, Exception cause) {
			super(message, cause);
		}
	}

	private Storage() {
		initImCache();
		initTranscriptCache();
		readTagSpecsFromLocalSettings();
		readStructTagSpecsFromLocalSettings();
		try {
			readVirtualKeyboardConf();
		} catch (ConfigurationException e) {
			logger.error("Could not read virtual keyboard configuration: "+e.getMessage(), e);
		}
	}
	
	public TrpPageType getOrBuildPage(TrpTranscriptMetadata md, boolean keepAlways) throws Exception {
		if (USE_TRANSCRIPT_CACHE) {
			return transcriptCache.getOrPut(md, keepAlways, null).getPage();
		} else {
			JAXBPageTranscript tr = new JAXBPageTranscript(md);
			tr.build();
			if(currentImg != null && currentImg.getTransformation() != null) {
				PageXmlUtils.checkAndFixXmlOrientation(currentImg.getTransformation(), tr.getPageData());
				//enable this if user is to be asked to save on orientation fix.
//				setCurrentTranscriptEdited(true);
			}
			return tr.getPage();
		}
		
	}
	
	private void initTranscriptCache() {
		int cacheSize = TrpConfig.getTrpSettings().getImageCacheSize();
		if (cacheSize < 1)
			cacheSize = 1;
		
		logger.info("setting transcript cache size to " + cacheSize);
		transcriptCache = new DataCache<TrpTranscriptMetadata, JAXBPageTranscript>(cacheSize, new DataCacheFactory<TrpTranscriptMetadata, JAXBPageTranscript>() {
			@Override public JAXBPageTranscript createFromKey(TrpTranscriptMetadata key, Object opts) throws Exception {
//				JAXBPageTranscript tr = TrpPageTranscriptBuilder.build(key);
				JAXBPageTranscript tr = new JAXBPageTranscript(key);
				tr.build();
				return tr;
			}
			@Override public void dispose(JAXBPageTranscript element) {
			}
		});
		
	}

	private void initImCache() {
		int imCacheSize = TrpConfig.getTrpSettings().getImageCacheSize();
		if (imCacheSize < 1)
			imCacheSize = 1;

		logger.info("setting image cache size to " + imCacheSize);
		imCache = new DataCache<URL, CanvasImage>(imCacheSize, new ImageDataDacheFactory());
	}

//	private static void initDocUpdater() {
//		docUpdater = new DocJobUpdater() {
//			@Override public void onUpdate(final TrpJobStatus job) {
//				// Display.getDefault().asyncExec(new Runnable() {
//				// @Override public void run() {
//				storage.sendEvent(new JobUpdateEvent(this, job));
//				// }
//				// });
//			}
//		};
//	}
//	
//	public void startOrResumeJobThread() {
//		docUpdater.startOrResumeJobThread();
//	}
//
//	@Override public void finalize() {
//		logger.debug("Storage finalize - stopping job update thread!");
//		docUpdater.stopJobThread();
//	}
	
	public static Storage i() {
		return getInstance();
	}

	public static Storage getInstance() {
		if (storage == null) {
			storage = new Storage();
//			initDocUpdater();
		}
		return storage;
	}

	private static TrpTranscriptMetadata findTranscriptWithTimeStamp(List<TrpTranscriptMetadata> transcripts, TrpTranscriptMetadata md) {
		// logger.debug("timestamp to find: "+md.getTimestamp());
		for (TrpTranscriptMetadata pmd : transcripts) {
			if (pmd.getTimestamp() == md.getTimestamp()) {
				// logger.debug("returning ts: "+pmd.getTimestamp());
				return pmd;
			}
		}
		return null;
	}

	public List<TrpTranscriptMetadata> getTranscriptsSortedByDate(boolean includeCurrent, int max) {
		if (page == null)
			return new ArrayList<TrpTranscriptMetadata>();

		List<TrpTranscriptMetadata> trlist = page.getTranscripts();

		if (max > 0 && trlist.size() > max) {
			trlist = new ArrayList<>(trlist.subList(0, max));
		}

		if (includeCurrent && hasTranscriptMetadata() && findTranscriptWithTimeStamp(trlist, transcript.getMd()) == null) {
			logger.debug("adding transcription to list: {}", transcript.getMd());
			if(!StorageUtil.isTranscriptMdConsistent(transcript)) {
				logger.error("Blocked attempt to add a transcript of page with nr. {} to page with nr. {}", 
						transcript.getMd().getPageNr() , page.getPageNr());
				throw new IllegalStateException("Inconsistent state in storage. Reload page to continue.");
			}
			trlist.add(transcript.getMd());
		}

		Collections.sort(trlist, Collections.reverseOrder());

		return trlist;
	}	

	public boolean hasPageIndex(int index) {
		return (doc != null && index >= 0 && index < getNPages());
	}

	// public boolean hasCurrentPage() { return currentPageObject!=null; }

	// public boolean hasTranscript(int index) {
	//
	// return (page != null && index >= 0 && index < getNTranscripts());
	// }

	public boolean hasTranscriptMetadata() {
		return transcript != null && transcript.getMd() != null;
	}

	public boolean hasTranscript() {
		return hasTranscriptMetadata() && transcript.getPageData() != null;
	}

	public int getNTranscripts() {
		if (page != null) {
			return page.getTranscripts().size();
		} else {
			return 0;
		}
	}

	public boolean hasTextRegion(int index) {
		return (hasTranscript() && index >= 0 && index < getNTextRegions());
	}

	public int getNPages() {
		if (doc != null) {
			return doc.getNPages();
		} else
			return 0;
	}

	public int getNTextRegions() {
		return getTextRegions().size();
	}

	public List<TrpTextRegionType> getTextRegions() {
		if (hasTranscript()) {
			return transcript.getPage().getTextRegions(true);
		} else
			return new ArrayList<>();
	}

//	public List<TrpJobStatus> getJobs() {
//		return jobs;
//	}

	// public List<TrpJobStatus> getJobsForCurrentDocument() {
	// // reloadJobs();
	// List<TrpJobStatus> jobs4Doc = new ArrayList<>();
	// if (!isDocLoaded())
	// return jobs4Doc;
	//
	// for (TrpJobStatus j : jobs) {
	// if (j.getDocId() == doc.getMd().getDocId())
	// jobs4Doc.add(j);
	// }
	// return jobs4Doc;
	// }
	
//	public int getNUnfinishedJobs(boolean filterByUser) throws NumberFormatException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
//		if (!isLoggedIn())
//			return 0;
//		
//		return conn.countJobs(filterByUser, TrpJobStatus.UNFINISHED, null);
//	}

	public List<TrpJobStatus> getUnfinishedJobs(boolean filterByUser) throws SessionExpiredException, ServerErrorException, IllegalArgumentException {
		List<TrpJobStatus> unfinished = new ArrayList<>();
		if (!isLoggedIn())
			return unfinished;
		
		return conn.getJobs(filterByUser, TrpJobStatus.UNFINISHED, null, null, 0, 0, null, null);
		
//		for (TrpJobStatus j : getJobs()) {
//			if (!j.isFinished())
//				unfinished.add(j);
//		}
//		return unfinished;
	}

	public List<TrpJobStatus> getUnfinishedJobsForCurrentPage() {
		List<TrpJobStatus> unfinished = new ArrayList<>();
		// TODO
//		if (doc == null || page == null)
//			return unfinished;
//
//		for (TrpJobStatus j : getUnfinishedJobs()) {
//			if (j.getDocId() == doc.getId() && j.getPageNr() == page.getPageNr())
//				unfinished.add(j);
//		}
		return unfinished;
	}

	// public boolean hasUnfinishedJobForCurrentPage() {
	// if (doc==null || page==null)
	// return false;
	//
	// for (TrpJobStatus j : getUnfinishedJobs()) {
	// if (j.getDocId()==doc.getId() && j.getPageNr() == page.getPageNr())
	// return true;
	// }
	// return false;
	// }
	
	public String getUserName() {
		return user == null ? null : user.getUserName();
	}

	public TrpUserLogin getUser() {
		return user;
	}
	
	public int getUserId() {
		return user!=null ? user.getUserId() : -1;
	}

	public boolean isLoggedIn() {
		return (conn != null && user != null);
	}
	
	public boolean isAdminLoggedIn() {
		return (conn != null && user != null && user.isAdmin());
	}

	public boolean isPageLoaded() {
		return (doc != null && page != null);
	}

	public TrpServerConn getConnection() {
		return conn;
	}

	public String getCurrentServer() {
		return isLoggedIn() ? conn.getServerUri().toString() : null;
	}
	
	public boolean isLoggedInAtTestServer() {
		if (getCurrentServer()==null) {
			return false;
		}
		return getCurrentServer().startsWith(TrpServerConn.TEST_SERVER_URI);
	}

	public boolean isLocalDoc() {
		return isLocalDoc(doc);
	}

	public boolean isRemoteDoc() {
		return isRemoteDoc(doc);
	}
	
	public static boolean isLocalDoc(TrpDoc doc) {
		return doc != null && doc.isLocalDoc();
	}
	
	public static boolean isRemoteDoc(TrpDoc doc) {
		return doc != null && doc.isRemoteDoc();
	}
	
	public boolean isGtDoc() {
		return isGtDoc(doc);
	}
	
	/**
	 * GT is loaded as remote doc with docId < 0 but is not available via collections API.<br>
	 * This methods checks  if the document is not null but no local folder and no docId is set.
	 * 
	 * @param doc
	 * @return
	 */
	public static boolean isGtDoc(TrpDoc doc) {
		return doc != null && doc.isRemoteDoc() && doc.isGtDoc();
	}
	
	public void closeCurrentDocument() {
		clearDocContent();
		clearPageContent();
		
		tryReleaseLocks();
		
		sendEvent(new DocLoadEvent(this, null));
	}

	private void clearDocContent() {
		doc = null;
		page = null;
	}

	private void clearPageContent() {
		// currentTranscriptIndex = 0;
		currentImg = null;
		clearTranscriptContent();
		regionObject = null;
		lineObject = null;
		wordObject = null;
	}
	
	public void checkDocLoaded() throws StorageException {
		if (!isDocLoaded()) {
			throw new StorageException("No document loaded!");
		}
	}
	
	public void checkRemoteDocLoaded() throws StorageException {
		if (!isRemoteDoc()) {
			throw new StorageException("No remote document loaded!");
		}
	}
	
	public void checkLocalDocLoaded() throws StorageException {
		if (!isRemoteDoc()) {
			throw new StorageException("No local document loaded!");
		}
	}
	
	public void checkLoggedIn() throws StorageException {
		if (!isLoggedIn()) {
			throw new StorageException("You are not logged in!");
		}
	}
	
	public void checkPageLoaded() throws StorageException {
		if (!isPageLoaded()) {
			throw new StorageException("No page loaded!");
		}
	}
	

	private void clearTranscriptContent() {
		transcript.clear();
	}

	private void preloadSurroundingImages(String fileType) {
		if (!isPageLoaded()) {
			return;
		}

		logger.debug("preloading surrounding images - n-previous = " + N_IMAGES_TO_PRELOAD_PREVIOUS + " n-next = " + N_IMAGES_TO_PRELOAD_NEXT);
		ArrayList<URL> preload = new ArrayList<URL>();

		int notLoadedCounter = 0;
		int currentPageIndex = getPageIndex();

		for (int i = 1; i <= N_IMAGES_TO_PRELOAD_PREVIOUS; ++i) {
			
			if (hasPageIndex(currentPageIndex - i)) {
				String urlStr = doc.getPages().get(currentPageIndex - i).getUrl().toString();
				urlStr = UriBuilder.fromUri(urlStr).replaceQueryParam("fileType", fileType).toString();
				
				try {
					preload.add(new URL(urlStr));
				} catch (MalformedURLException e) {
					logger.error(e.getMessage(),e );
				}
			} else
				notLoadedCounter++;
		}
		for (int i = 1; i <= (N_IMAGES_TO_PRELOAD_NEXT + notLoadedCounter); ++i) {
			if (hasPageIndex(currentPageIndex + i)) {
//				preload.add(doc.getPages().get(currentPageIndex + i).getUrl());
				String urlStr = doc.getPages().get(currentPageIndex + i).getUrl().toString();
				urlStr = UriBuilder.fromUri(urlStr).replaceQueryParam("fileType", fileType).toString();				
				
				try {
					preload.add(new URL(urlStr));
				} catch (MalformedURLException e) {
					logger.error(e.getMessage(),e );
				}
			}
		}

		imCache.preload(preload, fileType);
	}

	/**
	 * Sets the current page to page with the given index. Note that for
	 * actually loading the page (which loads the corresponding image and a list
	 * of transcriptions) a call to {@link #reloadCurrentPage()} is needed.
	 * 
	 * @param pageIndex
	 *            The 0 based index of the page
	 * @return True if the page exists and was set, false elsewise
	 */
	public boolean setCurrentPage(int pageIndex) {
		if (hasPageIndex(pageIndex)) {
			page = doc.getPages().get(pageIndex);
			return true;
		}
		return false;
	}

	public boolean setCurrentTranscript(TrpTranscriptMetadata md) {
		if (doc == null || page == null)
			return false;

		if (transcript.getMd()!=null && transcript.getMd().equals(md))
			return false;
		
		//FIXME only md is set but not the pageData! https://github.com/Transkribus/TranskribusSwtGui/issues/310
		transcript.setMd(md);
		return true;
	}

	public boolean setLatestTranscriptAsCurrent() {
		logger.trace("Page: {}", page);
		if (doc == null || page == null || CoreUtils.isEmpty(page.getTranscripts()))
			return false;

		List<TrpTranscriptMetadata> trs = getTranscriptsSortedByDate(false, 0);
		if (!trs.isEmpty())
			return setCurrentTranscript(trs.get(0));
		else
			return false;
	}

	public boolean jumpToRegion(int index) {
		if (doc == null || page == null || !hasTranscript())
			return false;

		logger.debug("Jumping to region " + index);
		if (hasTextRegion(index)) {
			regionObject = getTextRegions().get(index);
			return true;
		} else
			return false;

	}

	// public int getTranscriptIndex() {
	// return currentTranscriptIndex;
	// }

	public JAXBPageTranscript getTranscript() {
		return transcript;
	}

	public TrpTranscriptMetadata getTranscriptMetadata() {
		return transcript == null ? null : transcript.getMd();
	}
	
	public boolean isCurrentTranscript(TrpTranscriptMetadata metadata) {
		logger.debug("isCurrentTranscript, transcript = "+transcript+", md = "+transcript.getMd());
		return transcript!=null && transcript.getMd()!=null && transcript.getMd().equals(metadata);
	}
	
	public int getCurrentRegion() {
		if (regionObject != null)
			return regionObject.getIndex();
		else
			return -1;
	}

	public TrpPage getPage() {
		return page;
	}
	
	public boolean isCurrentPage(TrpPage other) {
		return page!=null && page.equals(other);
	}

	public int getPageIndex() {
		return isPageLoaded() ? doc.getPageIndex(page) : -1;
	}

	public boolean isPageLocked() {
		if (page == null)
			return false;

		return isPageLocked || !getUnfinishedJobsForCurrentPage().isEmpty();
	}

	public boolean isDocLoaded() {
		return doc != null;
	}
	
	public boolean isThisDocLoaded(int docId, File localFolder) {
		if (doc == null)
			return false;
		
		if (docId == -1)
			return doc.getMd().getLocalFolder().equals(localFolder);
		else
			return docId == doc.getMd().getDocId();
	}

	public void updateDataForSelectedShape(ICanvasShape shape) {
		// update storage data:
		regionObject = null;
		lineObject = null;
		wordObject = null;

		if (shape != null && shape.getData() != null) {
			if (shape.getData() instanceof TrpTextRegionType) {
				regionObject = (TrpTextRegionType) shape.getData();
			} else if (shape.getData() instanceof TrpTextLineType) {
				TrpTextLineType tl = (TrpTextLineType) shape.getData();
				regionObject = tl.getRegion();
				lineObject = tl;
			} else if (shape.getData() instanceof TrpBaselineType) {
				TrpTextLineType tl = ((TrpBaselineType) shape.getData()).getLine();
				regionObject = tl.getRegion();
				lineObject = tl;
			} else if (shape.getData() instanceof TrpWordType) {
				TrpWordType word = (TrpWordType) shape.getData();
				TrpTextLineType tl = word.getLine();
				regionObject = tl.getRegion();
				lineObject = tl;
				wordObject = word;
			}
		}
	}

	public boolean isTranscriptEdited() {
		return (hasTranscript() && transcript.getPage().isEdited());
	}

	public CanvasImage getCurrentImage() {
		return currentImg;
	}

	public TrpTextRegionType getCurrentRegionObject() {
		return regionObject;
	}

	public TrpTextLineType getCurrentLineObject() {
		return lineObject;
	}

	public TrpWordType getCurrentWordObject() {
		return wordObject;
	}
	
	public String getServerUri() {
		return conn==null ? "" : conn.getServerUri();
	}

	public TrpDoc getDoc() {
		return doc;
	}
	
	/** Returns the current document id. A value of -2 means no doc is loaded, while -1 means a local doc is loaded! */
	public int getDocId() {
		return doc==null ? -2 : doc.getId();
	}
	
//	public CanvasImage getCurrentImg() {
//		return currentImg;
//	}
	
	public boolean addListener(IStorageListener l) {
		return listener.add(l);
	}
	
	public boolean removeListener(IStorageListener l) {
		return listener.remove(l);
	}
	
	public void sendJobListUpdateEvent() {
		sendEvent(new JobUpdateEvent(this, null));
	}
	
	public void sendJobUpdateEvent(TrpJobStatus job) {
		sendEvent(new JobUpdateEvent(this, job));
	}	

	public void sendEvent(final Event event) {
		if (Thread.currentThread() == Display.getDefault().getThread()) {
			for (IStorageListener l : listener) {
				l.handleEvent(event);
			}
		} else {
			Display.getDefault().asyncExec(() -> {
				for (IStorageListener l : listener) {
					l.handleEvent(event);
				}
			});
		}

		// setChanged();
		// notifyObservers(event);
	}

	// //////////// METHODS THAT THROW EXCEPTIONS: /////////////////////////	
	public TrpCollection getCurrentDocumentCollection() {
		return isRemoteDoc() ? doc.getCollection() : null;
	}
	
	public int getCurrentDocumentCollectionId() {
		TrpCollection c = getCurrentDocumentCollection();
		return c==null ? 0 : c.getColId();
	}
		
	public void reloadUserDocs() {
		logger.debug("reloading docs by user!");
		
		if (user != null) {
			conn.getAllStrayDocsByUserAsync(0, 0, null, null, new InvocationCallback<List<TrpDocMetadata>>() {
				@Override public void failed(Throwable throwable) {
					logger.error("Error loading documents by user "+user+" - "+throwable.getMessage(), throwable);
				}
				
				@Override public void completed(List<TrpDocMetadata> response) {
					logger.debug("loaded docs by user "+user+" - "+response.size()+" thread: "+Thread.currentThread().getName());
					synchronized (this) {
						userDocList.clear();
						userDocList.addAll(response);
						
						sendEvent(new UserDocListLoadEvent(this, userDocList));
					}
				}
			});
		} else {
			synchronized (this) {
				userDocList.clear();				
				sendEvent(new UserDocListLoadEvent(this, userDocList));
			}
		}
	}
	
	public List<TrpDocMetadata> getUserDocList() {
		return userDocList;
	}

	public Future<List<TrpDocMetadata>> reloadDocList(int colId) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException{
		checkConnection(true);
		if (colId == 0)
			return null;
		

		//Just as a work around to get SessionExpiredException, which is catched and the login dialog can be called 
		getConnection().checkSession();

		logger.debug("reloading doclist for collection: "+colId+" reloadDocListCounter = "+(++reloadDocListCounter));
		
		SebisStopWatch.SW.start();
		
		Future<List<TrpDocMetadata>> fut = 
//			conn.getAllDocsAsync(colId, 0, 0, null, null, new InvocationCallback<List<TrpDocMetadata>>() {
			conn.getAllDocsAsync(colId, 0, 0, "docId", "desc", false, new InvocationCallback<List<TrpDocMetadata>>() {
			@Override
			public void completed(List<TrpDocMetadata> docs) {				
				synchronized (this) {
					docList.clear();
					docList.addAll(docs);
				}
				
				/* 
				 * Some actions triggered by DocListLoadEvent are only needed if the collection changed.
				 * Capture this and send it with the event.
				 */
				boolean isCollectionChange = Storage.this.collId != colId;
				logger.debug("Collection has changed ({} -> {}) ? {}", Storage.this.collId, colId, isCollectionChange);
				
				Storage.this.collId = colId;
				
				logger.debug("async loaded "+docList.size()+" nr of docs of collection "+collId+" thread: "+Thread.currentThread().getName());
				SebisStopWatch.SW.stop(true, "load time: ", logger);
				
				sendEvent(new DocListLoadEvent(this, colId, docList, isCollectionChange));
			}

			@Override public void failed(Throwable throwable) {
				
				
				
//				TrpMainWidget.getInstance().onError(title, message, th);
			}
		});
		
		//load deleted docs list as well
		Future<List<TrpDocMetadata>> fut2 = 
//				conn.getAllDocsAsync(colId, 0, 0, null, null, new InvocationCallback<List<TrpDocMetadata>>() {
				conn.getAllDocsAsync(colId, 0, 0, "docId", "desc", true, new InvocationCallback<List<TrpDocMetadata>>() {
				@Override
				public void completed(List<TrpDocMetadata> docs) {				
					synchronized (this) {
						deletedDocList.clear();
						deletedDocList.addAll(docs);
					}
										
					logger.debug("async loaded "+deletedDocList.size()+" nr of deleted docs in collection "+colId+" thread: "+Thread.currentThread().getName());

				}

				@Override public void failed(Throwable throwable) {

				}
			});
		
		return fut;
	}
	
	public int getCollId() {
		return collId;
	}
		
	public boolean hasRemoteDoc(int index) {
		return (docList != null && index >= 0 && index < docList.size());
	}
	
	public List<TrpDocMetadata> getDocList() {
		return docList;
	}

	public void invalidateSession() throws SessionExpiredException, ServerErrorException, Exception {
		checkConnection(true);
		conn.invalidate();
	}
	
	/**
	 * @deprecated not tested and used yet
	 */
	public void loginAsync(String serverUri, String username, String password, AsyncCallback<Object> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					login(serverUri, username, password);
					callback.onSuccess(null);
				} catch (Throwable e) {
					callback.onError(e);
				}
			}
		}).start();
	}

	public void login(String serverUri, String username, String password) throws ClientErrorException, LoginException {
		logger.debug("Logging in as user: " + username);
		if (conn != null)
			conn.close();

		conn = new TrpServerConn(serverUri);
		
		final boolean logHttp = TrpMainWidget.getTrpSettings().isLogHttp();
		if(logHttp) {
			conn.enableDebugLogging();
		} else {
			//gzip encoding and debug logging do not go well together...
			conn.enableGzipEncoding();
		}
		
		user = conn.login(username, password);
		logger.debug("Logged in as user: " + user + " connection: " + conn);
		
		if(user.isAdmin() && TrpMainWidget.getInstance()!=null) {
			logger.info(user + " is admin.");
			TrpMainWidget.getTrpSettings().setServerSelectionEnabled(user.isAdmin());
		}
		onLogin();
		sendEvent(new LoginOrLogoutEvent(this, true, user, conn.getServerUri()));
	}
	
	protected void onLogin() {
//		reloadP2PaLAModels();
	}
	
	public void logout() {
		try {
			if (conn != null)
				conn.close();
		} catch (Throwable th) {
			logger.error("Error logging out: " + th.getMessage(), th);
		} finally {
			clearCollections();
//			htrList = new ArrayList<>(0);
//			clearP2PaLAModels();
			conn = null;
			user = null;
//			clearDocList();
//			jobs = new ArrayList<>();
			sendEvent(new LoginOrLogoutEvent(this, false, null, null));
		}
	}

//	public void reloadJobs(boolean filterByUser) throws SessionExpiredException, ServerErrorException, IllegalArgumentException {
//		logger.debug("reloading jobs ");
//		if (conn != null && !isLocalDoc()) {
//			jobs = conn.getJobs(filterByUser, null, 0, 0);
//			// sort by creation date:
//			Comparator<TrpJobStatus> comp = new Comparator<TrpJobStatus>() {
//				@Override public int compare(TrpJobStatus o1, TrpJobStatus o2) {
//					return Long.compare(o1.getCreateTime(), o2.getCreateTime());
//				}
//			};
//			Comparator<TrpJobStatus> reverseComp = Collections.reverseOrder(comp);
//			Collections.sort(jobs, reverseComp);
//			startOrResumeJobThread();
//		}
//		if (jobs == null) // ensure that jobs array is never null!
//			jobs = new ArrayList<>();
//
//		sendEvent(new JobUpdateEvent(this, null));
//	}
	
	public void cancelJob(String jobId) throws SessionExpiredException, ServerErrorException, IllegalArgumentException {
		if (conn != null && jobId != null) {
			conn.killJob(jobId);
		}
	}

//	public TrpJobStatus loadJob(String jobId) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
//		// FIXME: direct access to job table not "clean" here...
//		List<TrpJobStatus> jobs = (List<TrpJobStatus>) TrpMainWidget.getInstance().getUi().getJobOverviewWidget().getTableViewer().getInput();
//		if (jobs == null) // should not happen!
//			return null;
//		
//		synchronized (jobs) {
//			checkConnection(true);
//			TrpJobStatus job = conn.getJob(jobId);
//			// update job in jobs array if there
//			for (int i = 0; i < jobs.size(); ++i) {
//				if (jobs.get(i).getJobId().equals(job.getJobId())) {
//					//logger.debug("UPDATING JOB: "+job.getJobId()+" new status: "+job.getState());
//					jobs.get(i).copy(job); // do not set new instance, s.t. table-viewer does not get confused!
//					
//					return jobs.get(i);
//					
////					jobs.set(i, job);
////					break;
//				}
//			}
////			return null; // orig
//			return job; // return "original" job from connection here if not found in table (can be possible since introduction of paginated widgets!!)
//		}
//	}

//	/**
//	 * @deprecated
//	 * FIXME page index is now set but Storage state is inconsistent due to missing page reload Causes
//	 * https://github.com/Transkribus/TranskribusSwtGui/issues/310
//	 * 
//	 * Solution: remove and use TrpMainWidget::reloadCurrentDocument instead!
//	 */
//	private void reloadCurrentDocument(int colId) throws SessionExpiredException, IllegalArgumentException, NoConnectionException, UnsupportedFormatException,
//			IOException, NullValueException {
//		if (doc != null) {
//			if (isLocalDoc()) {
//				loadLocalDoc(doc.getMd().getLocalFolder().getAbsolutePath(), null);
//			} else {
//				loadRemoteDoc(colId, doc.getMd().getDocId());
//			}
//
//			logger.debug("nr of pages: " + getNPages());
//			setCurrentPage(0);
//
//		}
//	}

	/**
	 * @deprecated Wordgraph support is removed from the server. this method returns an empty String matrix
	 */
	public String[][] getWordgraphMatrix(boolean fromCache, final int docId, final int pageNr, final String lineId) throws IOException {
		return new String[][] {};
	}
	
	public void reloadCurrentImage(final String fileType) throws MalformedURLException, Exception {
		if (!isPageLoaded())
			return;
		
		String urlStr = page.getUrl().toString();
		UriBuilder ub = UriBuilder.fromUri(urlStr);
		
		ub = ub.replaceQueryParam("fileType", null); // remove existing fileType par
		
		logger.debug("img uri: "+ub.toString());
		
		if (ub.toString().startsWith("file:") || new File(ub.toString()).exists()) {
			logger.debug("this is a local image file!");
			urlStr = ub.toString();
		} else {
			logger.debug("this is a remote image file - adding fileType parameter for fileType="+fileType);
			urlStr = UriBuilder.fromUri(urlStr).replaceQueryParam("fileType", fileType).toString();
		}
					
		logger.debug("Loading image from url: " + urlStr);
		final boolean FORCE_RELOAD = false;
		
		// always reload original image if asked for it
		currentImg = imCache.getOrPut(new URL(urlStr), true, fileType, (fileType == "orig") || FORCE_RELOAD);
		logger.trace("loaded image!");
		
		setCurrentImageMetadata();
		
		sendEvent(new MainImageLoadEvent(this, currentImg));
	}
	
	private static CanvasImage loadCanvasImage(DataCache<URL, CanvasImage> imCache, TrpDoc doc, TrpPage page, String fileType) throws MalformedURLException, Exception {
		if (doc==null || page==null)
			return null;
		
		String urlStr = page.getUrl().toString();
		UriBuilder ub = UriBuilder.fromUri(urlStr);
		
		ub = ub.replaceQueryParam("fileType", null); // remove existing fileType par
		
		logger.debug("img uri: "+ub.toString());
		
		if (ub.toString().startsWith("file:") || new File(ub.toString()).exists()) {
			logger.debug("this is a local image file!");
			urlStr = ub.toString();
		} else {
			logger.debug("this is a remote image file - adding fileType parameter for fileType="+fileType);
			urlStr = UriBuilder.fromUri(urlStr).replaceQueryParam("fileType", fileType).toString();
		}
					
		logger.debug("Loading image from url: " + urlStr);
		final boolean FORCE_RELOAD = false;

		CanvasImage img;
		try {
			// always reload original image if asked for it
			img = imCache.getOrPut(new URL(urlStr), true, fileType, (fileType == "orig") || FORCE_RELOAD);
		} catch (Exception e) {
			urlStr = LocalDocConst.getDummyImageUrl().toString();
			img = imCache.getOrPut(new URL(urlStr), true, fileType, false);
		}
		logger.trace("loaded image!");
		
		return img;
	}

	/**
	 * Reloads the current page, i.e. its corresponding image and a list of
	 * transcription belonging to this page
	 * 
	 * @throws IllegalArgumentException
	 * @throws ServerErrorException
	 * @throws SessionExpiredException
	 * @throws Exception
	 */
	public void reloadCurrentPage(int colId, String fileType) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (!isPageLoaded())
			return;
		if (isRemoteDoc())
			checkConnection(true);

		clearPageContent();

		// load image
		reloadCurrentImage(fileType);

		reloadTranscriptsList(colId);
		setLatestTranscriptAsCurrent();
		logger.debug("nr of transcripts: " + getNTranscripts());
		logger.debug("image filename: " + page.getUrl());
		
		if (isRemoteDoc() && this.getRoleOfUserInCurrentCollection().getValue() > TrpRole.Reader.getValue())
			lockPage(getCurrentDocumentCollectionId(), page);
		
		if (TrpConfig.getTrpSettings().isPreloadImages())
			preloadSurroundingImages(fileType);
		else
			logger.debug("preloading images is turned off!");

		sendEvent(new PageLoadEvent(this, doc, page));
	}
	
	public Future<PageLoadResult> reloadCurrentPageAsync(int colId, String fileType, AsyncCallback<PageLoadResult> callback) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (!isPageLoaded())
			return null;
		if (isRemoteDoc())
			checkConnection(true);

		clearPageContent();

		// FIXME: is this really necessary?
		final TrpDoc doc = this.doc;
		final TrpPage page = this.page;
		
		Future<PageLoadResult> future = loadPageAsyncExecutor.runAsync("Load page "+page.getPageNr(), () -> {
			logger.debug("reloadCurrentPageAsync");
			PageLoadResult res = new PageLoadResult();
			res.doc = doc;
			res.page = page;
			
			logger.debug("loading image...");
			res.image = loadCanvasImage(imCache, doc, page, fileType);
			logger.debug("loading image metadata...");
			res.imgMd = getImageMetadata(doc, page);
			logger.debug("loading page transcript list...");
			res.metadataList = loadTranscriptsList(conn, doc, page, colId);
			logger.debug("Returning PageLoadResult: {}", res);
			return res;
		},
		new AsyncCallback<PageLoadResult>() {
			@Override
			public void onError(Throwable error) {
				if (!isCurrentPage(page)) {
					logger.debug("current page already switched - not forwarding error!");
					return;
				}
				
				AsyncExecutor.onError(callback, error);
			}

			@Override
			public void onSuccess(PageLoadResult result) {
				if (!isCurrentPage(page)) {
					logger.debug("current page already switched - not forwarding success!");
					return;
				}				
				
				logger.debug("setting current image: "+result.image+", disposed = "+result.image.isDisposed());
				currentImg = result.image;
				imgMd = result.imgMd;
				//metadata result.metadataList will be null for GT and local docs! Do not overwrite the given transcripts list then.
				if (result.metadataList != null)  {
					Storage.this.page.setTranscripts(result.metadataList);
					setLatestTranscriptAsCurrent();
				}
				
				sendEvent(new MainImageLoadEvent(this, result.image));
				sendEvent(new TranscriptListLoadEvent(this, result.doc, result.page, isPageLoaded() ? result.page.getTranscripts() : null));
				
				logger.debug("nr of transcripts: " + getNTranscripts());
				logger.debug("image filename: " + page.getUrl());
				
				if (isRemoteDoc() && getRoleOfUserInCurrentCollection().getValue() > TrpRole.Reader.getValue()) {
					try {
						lockPage(getCurrentDocumentCollectionId(), page);
					} catch (SessionExpiredException | ServerErrorException | NoConnectionException e) {
						onError(e);
					}
				}
				
				if (TrpConfig.getTrpSettings().isPreloadImages())
					preloadSurroundingImages(fileType);
				else
					logger.debug("preloading images is turned off!");

				sendEvent(new PageLoadEvent(this, doc, page));	
				
				AsyncExecutor.onSuccess(callback, result);
			}
		});
		return future;
		
		// load image
//		reloadCurrentImage(fileType);
//		reloadTranscriptsList(colId);
//		setLatestTranscriptAsCurrent();
//		logger.debug("nr of transcripts: " + getNTranscripts());
//		logger.debug("image filename: " + page.getUrl());
//		
//		if (isRemoteDoc() && this.getRoleOfUserInCurrentCollection().getValue() > TrpRole.Reader.getValue())
//			lockPage(getCurrentDocumentCollectionId(), page);
//		
//		if (TrpConfig.getTrpSettings().isPreloadImages())
//			preloadSurroundingImages(fileType);
//		else
//			logger.debug("preloading images is turned off!");
//
//		sendEvent(new PageLoadEvent(this, doc, page));
	}	
	
	public List<PageLock> listPageLocks(int colId, int docId, int pageNr) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		
		return conn.listPageLocks(colId, docId, pageNr);
	}
	
	public List<TrpAction> listAllActions(int colId, int docId, int nValues) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		final Integer[] typeIds = {1, 3}; // = Save | Status Change | Access Document = 4 (no need)
		return conn.listActions(typeIds, colId, docId, nValues);
	}
	
	
	
	public void lockPage(int colId, TrpPage page) throws NoConnectionException, SessionExpiredException, ServerErrorException {
		if (page.getDocId() > 0) {
			checkConnection(true);
			logger.debug("locking page: colId = "+colId+" docId = "+page.getDocId()+" pageNr = "+page.getPageNr());
			conn.lockPage(colId, page.getDocId(), page.getPageNr());
		} else {
			logger.debug("This is a local doc... locking no page!");
		}
		
	}
	
	public void releaseLocks() throws NoConnectionException, SessionExpiredException, ServerErrorException {
		checkConnection(true);
		
		conn.unlockPage();
	}
	
	public void tryReleaseLocks() {
		try {
			if (isLoggedIn()) {
				releaseLocks();
			}
		} catch (SessionExpiredException | ServerErrorException | NoConnectionException e) {
			logger.error("Error releasing locks: "+e.getMessage(), e);
		}
	}
	
	public void getLocks() {
		
		
		
		
		
	}

	public void syncTextOfDocFromWordsToLinesAndRegions(final int colId, IProgressMonitor monitor) throws JAXBException, IOException, InterruptedException, Exception {
		monitor.beginTask("Applying text from words", doc.getPages().size());
		int i = 1;
		for (TrpPage p : doc.getPages()) {
			monitor.subTask("Applying text of page " + i);

			List<TrpTranscriptMetadata> trlist = p.getTranscripts();
			Collections.sort(trlist);
			if (!trlist.isEmpty()) {
				TrpTranscriptMetadata md = trlist.get(trlist.size() - 1);
//				JAXBPageTranscript tr = TrpPageTranscriptBuilder.build(md);
				JAXBPageTranscript tr = new JAXBPageTranscript(md);
				tr.build();
				for (TrpTextRegionType region : tr.getPage().getTextRegions(true))
					region.applyTextFromWords();

//				saveTranscript(colId, p, tr, EditStatus.IN_PROGRESS);
				saveTranscript(colId, tr.getPage(), EditStatus.IN_PROGRESS, md.getTsId(), "Synced text from word to line regions");
			}

			if (monitor.isCanceled())
				throw new InterruptedException();

			monitor.worked(i + 1);
			++i;
		}
	}

	public void reloadTranscriptsList(int colId) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		// if (!isPageLoaded())
		// return;

		if (isRemoteDoc() && !isGtDoc()) {
			checkConnection(true);
			
			//int nValues = 10; // 0 for all!
			int nValues = 0;
			List<TrpTranscriptMetadata> list = conn.getTranscriptMdList(colId, doc.getMd().getDocId(), getPageIndex() + 1, 0, nValues, null, null);
			logger.debug("got transcripts: " + list.size());
			page.setTranscripts(list);
		}

		sendEvent(new TranscriptListLoadEvent(this, doc, page, isPageLoaded() ? page.getTranscripts() : null));
	}
	
	private static List<TrpTranscriptMetadata> loadTranscriptsList(TrpServerConn conn, TrpDoc doc, TrpPage page, int colId) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (doc==null || page==null || !doc.isRemoteDoc() || doc.isGtDoc()) {
			return null;
		}

//		if (doc!=null && page!=null && doc.isRemoteDoc() && !doc.isGtDoc()) {
			checkConnection(conn, true);
			
			int nValues = 0;//int nValues = 10; // 0 for all!
			List<TrpTranscriptMetadata> list = conn.getTranscriptMdList(colId, doc.getMd().getDocId(), doc.getPageIndex(page) + 1, 0, nValues, null, null);
			logger.debug("got transcripts: " + list.size());
			page.setTranscripts(list);
			return list;
//		}
	}

	/**
	 * Reloads the current transcrition
	 * 
	 * @param tryLocalReload
	 *            If true, the transcription is reloaded from the locally stored
	 *            object (if it has been loaded already!)
	 * @throws IOException
	 *             , Exception
	 * @throws JAXBException
	 */
	public void reloadTranscript() throws JAXBException, IOException, Exception {
		if (!isLocalDoc() && !isLoggedIn())
			throw new Exception("No connection");
		else if (!isPageLoaded())
			throw new Exception("No page loaded");
		
		TrpTranscriptMetadata trMd = getTranscriptMetadata();
		if (trMd == null)
			throw new Exception("Transcript metadata is null -> should not happen...");

		logger.debug("reloading transcript 2: " + trMd);
		clearTranscriptContent();
//		transcript = TrpPageTranscriptBuilder.build(trMd); // OLD
		
		// NEW:
		TrpPageType p = getOrBuildPage(trMd, true);
		transcript.setMd(trMd);
		transcript.setPageData(p.getPcGtsType());
		
		setCurrentTranscriptEdited(false);
		if (!isLocalDoc()) {
			// FIXME:
			// PcGtsType pc = conn.getTranscript(doc.getId(), trMd.getPageNr());
			// isPageLocked = conn.isPageLocked(doc.getId(), trMd.getPageNr());

			// transcript = new JAXBPageTranscript(trMd, pc);
		}
		// TEST:
		// isPageLocked = true;
		
		// add foreign tags from this transcript:
		addForeignStructTagSpecsFromTranscript();

		sendEvent(new TranscriptLoadEvent(this, doc, page, transcript));
		logger.debug("loaded JAXB, regions: " + getNTextRegions());
	}
	
	public Future<TrpPageType> reloadTranscriptAsync(AsyncCallback<TrpPageType> callback) throws Exception  {
		if (!isLocalDoc() && !isLoggedIn())
			throw new Exception("No connection");
		else if (!isPageLoaded())
			throw new Exception("No page loaded");
		
		TrpTranscriptMetadata trMd = getTranscriptMetadata();
		if (trMd == null)
			throw new Exception("Transcript metadata is null -> should not happen...");
		
//		clearTranscriptContent(); // here or in onSuccess? I guess its safe to do it here...
		
		return loadTranscriptAsyncExecutor.runAsync("Load transcript "+trMd.getKey()==null ? trMd.getXmlFileName() : trMd.getKey(), () -> {
			logger.debug("reloading transcript async: " + trMd);
			SebisStopWatch sw = new SebisStopWatch();
			TrpPageType p = getOrBuildPage(trMd, true);
			sw.stop(true, "time for unmarshalling page: ", logger);
			
			if (false) { // test: throw exception after some delay to test how exceptions are handled 
				Thread.currentThread().sleep(1000);
				throw new Exception("I am error "+System.currentTimeMillis());
			}
			
			return p;
		}, new AsyncCallback<TrpPageType>() {
			@Override
			public void onError(Throwable error) {
				if (!isCurrentTranscript(trMd)) {
					logger.debug("current transcript already switched - not forwarding error!");
					return;
				}
				clearTranscriptContent();
				AsyncExecutor.onError(callback, error);
			}

			@Override
			public void onSuccess(TrpPageType result) {
				if (!isCurrentTranscript(trMd)) {
					logger.debug("current transcript already switched - not forwarding success!");
					return;
				}
				
				logger.debug("onSuccess: "+result);
				
				clearTranscriptContent();
				transcript.setMd(trMd);
				transcript.setPageData(result.getPcGtsType());
				
				setCurrentTranscriptEdited(false);
				
				// add foreign tags from this transcript:
				addForeignStructTagSpecsFromTranscript();

				sendEvent(new TranscriptLoadEvent(this, doc, page, transcript));
				logger.debug("loaded JAXB, regions: " + getNTextRegions());
				
				AsyncExecutor.onSuccess(callback, result);
			}
		});
	}
	
//	public void analyzeStructure(int colId, int docId, int pageNr, boolean detectPageNumbers, boolean detectRunningTitles, boolean detectFootnotes) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException, JAXBException {
//		checkConnection(true);
//		
//		PcGtsType pcGts = conn.analyzePageStructure(colId, docId, pageNr, detectPageNumbers, detectRunningTitles, detectFootnotes);
//		
//		transcript.setPageData(pcGts);
//	}

	public void loadLocalDoc(String folder, IProgressMonitor monitor) throws UnsupportedFormatException, IOException, NullValueException {
		tryReleaseLocks();
		
		DocLoadConfig config = new DocLoadConfig();
		doc = LocalDocReader.load(folder, config, monitor);
		
		setCurrentPage(0);

		if (doc == null)
			throw new NullValueException(folder + " is null...");

		sendEvent(new DocLoadEvent(this, doc));

		logger.info("loaded local document, path = " + doc.getMd().getLocalFolder().getAbsolutePath() + ", title = " + doc.getMd().getTitle() + ", nPages = "
				+ doc.getPages().size());
	}

	public void loadRemoteDoc(int colId, int docId) throws SessionExpiredException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);

		doc = conn.getTrpDoc(colId, docId, 1);
//		currentColId  = colId;
		setCurrentPage(0);

		sendEvent(new DocLoadEvent(this, doc));

		logger.info("loaded remote document, docId = " + doc.getId() + ", title = " 
				+ doc.getMd().getTitle() + ", nPages = " + doc.getPages().size() + ", pageId = " 
				+ doc.getMd().getPageId());
	}
	
	public boolean isUserAllowedForJob(String jobImpl, boolean defaultValue) {
		if (isLoggedIn()) {
			try {
				return conn.isUserAllowedForJob(jobImpl);
			} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
				return defaultValue;
			}	
		}
		else {
			return defaultValue;
		}
	}
	
	/**
	 * ReleaseLevel of the HTR may imply that the dataset is not visible to current user.<br>
	 * None = model is obviously linked to collection. Otherwise it wouldn't be visible.<br>
	 * DisclosedDataSet = Handle like "None".<br>
	 * UndisclosedDataSet = only show children if current user is curator OR the model is linked to this collection.<br>
	 */
	public boolean isUserAllowedToViewDataSets(TrpHtr h) {
		return isUserAllowedToViewDataSets(this.getCollId(), h);
	}
	
	/**
	 * ReleaseLevel of the HTR may imply that the dataset is not visible to current user.<br>
	 * None = model is obviously linked to collection. Otherwise it wouldn't be visible.<br>
	 * DisclosedDataSet = Handle like "None".<br>
	 * UndisclosedDataSet = only show children if current user is curator OR the model is linked to this collection.<br>
	 */
	public boolean isUserAllowedToViewDataSets(TrpModelMetadata h) {
		try {
			return getConnection().getModelCalls().getModel(h).isGtAccessible();
		} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
			return false;
		}
	}
	
	/**
	 * @deprecated TrpModelMetadata has a property for this set on server-side
	 * 
	 * ReleaseLevel of the HTR may imply that the dataset is not visible to current user.<br>
	 * None = model is obviously linked to collection. Otherwise it wouldn't be visible.<br>
	 * DisclosedDataSet = Handle like "None".<br>
	 * UndisclosedDataSet = only show children if current user is curator OR the model is linked to this collection.<br>
	 */
	public boolean isUserAllowedToViewDataSets(int colId, TrpHtr h) {
		if(h == null) {
			logger.warn("HTR argument is null!");
			return false;
		}
		if(isAdminLoggedIn()) {
			logger.debug("Admin is allowed to view datasets of HTR '{}'", h.getName());
			return true;
		}
		logger.debug("Checking HTR ReleaseLevel: {}", h.toShortString());
		
		//check if this is a private data set
		boolean isAllowed = !ReleaseLevel.isPrivateDataSet(h.getReleaseLevel());
		logger.debug("isAllowed based on release level: {}", isAllowed);
		
		//check for direct collection link which will allow the user to see the set
		isAllowed |=  h.getCollectionIdLink() != null && h.getCollectionIdLink() == colId;
		logger.debug("isAllowed based on collectionIdLink: {}", isAllowed);
		
		//curator may always see the sets even if no explicit link is set to this collection
		isAllowed |= h.getUserId() == getUserId();
		logger.debug("isAllowed based on userId: {}", isAllowed);		
		
		return isAllowed;
	}
	
	/**
	 * Loads a HTR ground truth set from the server, transforms it into a document object and sets it as document in this Storage.
	 * 
	 * @param colId
	 * @param set
	 * @throws SessionExpiredException
	 * @throws ClientErrorException
	 * @throws IllegalArgumentException
	 * @throws NoConnectionException
	 */
	public void loadModelGtAsDoc(ModelGtDataSet set, int pageIndex) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		this.doc = getModelDataSetAsDoc(set);
		setCurrentPage(pageIndex < 0 ? 0 : pageIndex);
		
		sendEvent(new GroundTruthLoadEvent(this, doc));

		logger.info("loaded HTR GT " + set.getDataSetType().getLabel() + " htrId = " + set.getId() + ", title = " 
				+ doc.getMd().getTitle() + ", nPages = " + doc.getPages().size());
	}

	public TrpDoc getModelDataSetAsDoc(TrpModelMetadata model, DataSetType dataSetType) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		return getModelDataSetAsDoc(new ModelGtDataSet(model, dataSetType));
	}
	
	private TrpDoc getModelDataSetAsDoc(ModelGtDataSet set) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		List<TrpGroundTruthPage> gt = getModelDataSet(set.getId(), set.getDataSetType());
		
		TrpDocMetadata md = new TrpModelGtDocMetadata(set);
		TrpDoc gtDoc = new TrpDoc();
		gtDoc.setMd(md);
		for(TrpGroundTruthPage g : gt) {
			gtDoc.getPages().add(g.toTrpPage());
		}
		return gtDoc;
	}

	public List<TrpGroundTruthPage> getModelDataSet(int id, final DataSetType dataSetType) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		List<TrpGroundTruthPage> gt;
		switch(dataSetType) {
		case VALIDATION: 
			gt = conn.getModelCalls().getHtrValidationData(id);
			break;
		default:
			gt = conn.getModelCalls().getHtrTrainData(id);
			break;
		}
		return gt;
	}
	
	public TrpModelMetadata getModelDetails(TrpModelMetadata modelBrief) throws NoConnectionException, TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		checkConnection(true);
		return conn.getModelCalls().getModel(modelBrief);
	}
	
	/**
	 * Loads a HTR ground truth set from the server, transforms it into a document object and sets it as document in this Storage.
	 * 
	 * @param colId
	 * @param set
	 * @throws SessionExpiredException
	 * @throws ClientErrorException
	 * @throws IllegalArgumentException
	 * @throws NoConnectionException
	 */
	@Deprecated
	public void loadHtrGtAsDoc(int colId, HtrGtDataSet set, int pageIndex) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		this.doc = getHtrDataSetAsDoc(colId, set);
		setCurrentPage(pageIndex < 0 ? 0 : pageIndex);
		
		sendEvent(new GroundTruthLoadEvent(this, doc));

		logger.info("loaded HTR GT " + set.getDataSetType().getLabel() + " htrId = " + set.getId() + ", title = " 
				+ doc.getMd().getTitle() + ", nPages = " + doc.getPages().size());
	}

	@Deprecated
	public TrpDoc getHtrDataSetAsDoc(int colId, TrpHtr htr, DataSetType dataSetType) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		return getHtrDataSetAsDoc(colId, new HtrGtDataSet(htr, dataSetType));
	}

	@Deprecated
	private TrpDoc getHtrDataSetAsDoc(int colId, HtrGtDataSet set) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		List<TrpGroundTruthPage> gt = getHtrDataSet(colId, set.getId(), set.getDataSetType());
		
		TrpDocMetadata md = new TrpHtrGtDocMetadata(set);
		TrpDoc gtDoc = new TrpDoc();
		gtDoc.setMd(md);
		for(TrpGroundTruthPage g : gt) {
			gtDoc.getPages().add(g.toTrpPage());
		}
		return gtDoc;
	}

	@Deprecated
	public List<TrpGroundTruthPage> getHtrDataSet(int colId, int id, final DataSetType dataSetType) throws SessionExpiredException, ClientErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		List<TrpGroundTruthPage> gt;
		switch(dataSetType) {
		case VALIDATION: 
			gt = conn.getHtrValidationData(colId, id);
			break;
		default:
			gt = conn.getHtrTrainData(colId, id);
			break;
		}
		return gt;
	}

	public TrpDoc getRemoteDoc(int colId, int docId, int nrOfTranscripts) throws SessionExpiredException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		return conn.getTrpDoc(colId, docId, nrOfTranscripts);
	}
	
	/**
	 * Saves all transcripts in transcriptsMap.
	 * @param transcriptsMap A map of the transcripts to save. The map's key is the page-id, its value is a pair of the collection-id and the 
	 * corresponding TrpPageType object to save as newest version.
	 * @param monitor A progress monitor that can also be null is no GUI status update is needed.
	 */
	public void saveTranscriptsMap(Map<Integer, Pair<Integer, TrpPageType>> transcriptsMap, IProgressMonitor monitor)
			throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (monitor != null)
			monitor.beginTask("Saving affected transcripts", transcriptsMap.size());

		int c = 0;
		for (Pair<Integer, TrpPageType> ptPair : transcriptsMap.values()) {
			if (monitor != null && monitor.isCanceled())
				return;
			
			saveTranscript(ptPair.getLeft(), ptPair.getRight(), null, ptPair.getRight().getMd().getTsId(), "Tagged from text");

			if (monitor != null)
				monitor.worked(c++);

			++c;
		}
	}
	
	public void saveTranscript(int colId, String commitMessage) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (!isLocalDoc() && !isLoggedIn())
			throw new Exception("No connection");
		if (!isPageLoaded())
			throw new Exception("No page loaded");
		if (!hasTranscript()) {
			throw new Exception("No transcript loaded");
		}

		transcript.getPage().removeDeadLinks();
		
		logger.debug("saving transcription " + (getPageIndex() + 1) + " for doc " + doc.getMd().getDocId());
//		saveTranscript(colId, page, transcript, EditStatus.IN_PROGRESS);
		if(!StorageUtil.isTranscriptMdConsistent(transcript)) {
			logger.error("Inconsistent storage state!");
			logger.error("Transcript md: {}", transcript.getMd());
			logger.error("PageData transcript md: {}", transcript.getPage().getMd());
//			throw new IllegalStateException("Transcript metadata is not consistent! Reload document to continue.");
		}
		saveTranscript(colId, transcript.getPage(), transcript.getMd().getStatus(), transcript.getMd().getTsId(), commitMessage);
	}
	
	public void saveTranscript(int colId, JAXBPageTranscript transcript, String commitMessage) 
			throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (transcript == null)
			throw new Exception("No page or metadata given");
		if (transcript.getMd() == null)
			throw new Exception("No page metadata set");
		if (transcript.getPage() == null)
			throw new Exception("No page transcript set");
		if(!StorageUtil.isTranscriptMdConsistent(transcript)) {
			//FIXME saveTranscript will use the pageNr embedded in TrpPageType. 
			logger.error("Transcript md: {}", transcript.getMd());
			logger.error("PageData md: {}", transcript.getPage().getMd());
//			throw new IllegalStateException("Transcript metadata is not consistent!");
		}
		saveTranscript(colId, transcript.getPage(), transcript.getMd().getStatus(), transcript.getMd().getTsId(), commitMessage);
	}	
	
	/**
	 * @param colId
	 * @param page
	 * @param status
	 * @param parentId
	 * @param commitMessage
	 * @throws SessionExpiredException
	 * @throws ServerErrorException
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	public void saveTranscript(int colId, TrpPageType page, EditStatus status, int parentId, String commitMessage) 
			throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (page == null)
			throw new Exception("No page or metadata given");
		if (page.getMd() == null)
			throw new Exception("No page metadata set");
		
		if (page.getMd().getTsId() != parentId) {
			logger.warn("Current tsId = {} is not equal to parentTsId argument = {}", 
					page.getMd().getTsId(), parentId);
			// the save might affect the wrong page. See https://github.com/Transkribus/TranskribusPersistence/issues/21
//			throw new IllegalStateException("Inconsistent state in Storage. Saving is not possible.");
		}
		
		if (status == null || status.equals(EditStatus.NEW)){
			status = EditStatus.IN_PROGRESS;
		}
		
		int docId = page.getMd().getDocId();
		
		//the save will use the pageNr embedded in page argument
		final int pageNrSave = page.getMd().getPageNr();
		//reloadTranscriptsList(colId) will load the transcripts for storage.page field value. Check consistency
		final int pageNrTranscriptReload = getPageIndex() + 1;
		if(pageNrSave != pageNrTranscriptReload) {
			logger.error("PageNr differs in page data ({}) and storage.page field ({})!", pageNrSave, pageNrTranscriptReload);
//			throw new IllegalStateException("Inconsistent state in Storage. Saving is not possible.");
		}
		
		if (docId != -1) {
			checkConnection(true);	
			//info level to make iit show up in release builds' log files 
			logger.info("Saving transcript: docId = {}, pageNr = {}, status = {}, parentId = {}", docId, page.getMd().getPageNr(), status, parentId);
			page.writeCustomTagsToPage();
			TrpTranscriptMetadata res = conn.updateTranscript(colId, docId, page.getMd().getPageNr(), status, page.getPcGtsType(), parentId, commitMessage);
		} else {
			//status must be set for local docs into local PAGE XMLs if transcript gets saved
			if (page.getPcGtsType() != null && page.getPcGtsType().getMetadata() != null && page.getPcGtsType().getMetadata().getTranskribusMetadata() != null) {
				page.getPcGtsType().getMetadata().getTranskribusMetadata().setStatus(status.toString());
			}
			else {
				if (page.getPcGtsType() != null && page.getPcGtsType().getMetadata() != null && page.getPcGtsType().getMetadata().getTranskribusMetadata() == null) {
					TranskribusMetadataType trpMd = new TranskribusMetadataType();
					trpMd.setDocId(getDocId());
					trpMd.setPageId(getPage().getPageId());
					trpMd.setPageNr(getPage().getPageNr());
					trpMd.setStatus(status.toString());
					page.getPcGtsType().getMetadata().setTranskribusMetadata(trpMd);
				}
			}
			
			LocalDocWriter.updateTrpPageXml(new JAXBPageTranscript(page.getMd(), page.getPcGtsType()));
			
			//write the page status into the doc.xml so that it gets loaded for the local doc
			TrpDoc doc = Storage.getInstance().getDoc();
			final File inputDir = doc.getMd().getLocalFolder();
			logger.debug("input dir: " + inputDir.getAbsolutePath());
			final File docXml = new File(inputDir.getAbsolutePath() + File.separator + LocalDocConst.DOC_XML_FILENAME);

			LocalDocWriter.writeDocXml(doc, docXml);
			
		}
		page.setEdited(false);
		
		sendEvent(new TranscriptSaveEvent(this, colId, page.getMd()));
		reloadTranscriptsList(colId);
	}

//	@Deprecated
//	// Too much fuzz
//	private void saveTranscript(int colId, TrpPage page, JAXBPageTranscript transcript, EditStatus status) 
//			throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
//		if (page == null)
//			throw new Exception("No page given");
//		int docId = page.getDocId();
//		
//		if (docId != -1) {
//			checkConnection(true);	
//		}
//		if (transcript == null) {
//			throw new Exception("No transcript given");
//		}
//		
//		if (status == null)
//			status = EditStatus.IN_PROGRESS;
//		
//		if (page == null || transcript == null || status == null)
//			throw new Exception("Null values in saveTransript not allowed!");
//
//		if (docId != -1) {
//			transcript.getPage().writeCustomTagsToPage();
//			TrpTranscriptMetadata res = conn.updateTranscript(colId, docId, page.getPageNr(), EditStatus.IN_PROGRESS,
//					transcript.getPageData());
//		} else {
//			LocalDocWriter.updateTrpPageXml(transcript);
//		}
//		transcript.getPage().setEdited(false);
//		reloadTranscriptsList(colId);
//	}
	
	public void applyFunctionToTranscriptsAndSave(Collection<Integer> pageIndices, IProgressMonitor monitor, Consumer<JAXBPageTranscript> f, String commitMsg) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (!isDocLoaded())
			throw new IOException("No document loaded!");
		
		int N = pageIndices == null ? doc.getNPages() : pageIndices.size();
		
		if (monitor != null)
			monitor.beginTask("Applying function", N);
				
		int worked=0;
		for (int i=0; i<doc.getNPages(); ++i) {
			TrpPage p = doc.getPages().get(i);
			if (pageIndices != null && !pageIndices.contains(i))
				continue;
			
			if (monitor != null)
				monitor.subTask("Processing page "+(worked+1)+" / "+N);
			
			if (monitor != null && monitor.isCanceled())
				return;
			
			// unmarshal page:
			TrpTranscriptMetadata md = p.getCurrentTranscript();
			JAXBPageTranscript tr = new JAXBPageTranscript(md);
			tr.build();
			
			f.accept(tr);
			
			saveTranscript(getCurrentDocumentCollectionId(), tr, commitMsg);
			
			if (monitor != null)
				monitor.worked(++worked);
		}

	}
	
	public void applyAffineTransformation(Collection<Integer> pageIndices, double tx, double ty, double sx, double sy, double rot, IProgressMonitor monitor) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, Exception {
		if (!isDocLoaded())
			throw new IOException("No document loaded!");
		
		int N = pageIndices == null ? doc.getNPages() : pageIndices.size();
		
		if (monitor != null)
			monitor.beginTask("Applying transformation", N);
		
		String trTxt = "tx="+tx+", ty="+ty+", sx="+sx+", sy="+sy+", rot="+rot;
		logger.info("affine transform is: "+trTxt);
		double rotRad = MathUtil.degToRad(rot);
		logger.debug("rotation in radiants = "+rotRad);
		
		int worked=0;
		
		for (int i=0; i<doc.getNPages(); ++i) {
			TrpPage p = doc.getPages().get(i);
			if (pageIndices != null && !pageIndices.contains(i))
				continue;
			
			if (monitor != null)
				monitor.subTask("Processing page "+(worked+1)+" / "+N);
			
			if (monitor != null && monitor.isCanceled())
				return;
			
			// unmarshal page:
			TrpTranscriptMetadata md = p.getCurrentTranscript();
			JAXBPageTranscript tr = new JAXBPageTranscript(md);
			tr.build();
			
			// apply transformation:
			PageXmlUtils.applyAffineTransformation(tr.getPage(), tx, ty, sx, sy, rotRad);
			String msg = "Applied affine transformation: "+trTxt;
			
			saveTranscript(getCurrentDocumentCollectionId(), tr, msg);
			
			if (monitor != null)
				monitor.worked(++worked);
		}

	}
	
	/**
	 * Synchronize local PAGE xml files for current document on server.
	 * If the number of local and remote pages do not match, file name matching is 
	 * checked and documents are synced according to filename. 
	 * If filenames do not match, the current document page is not touched 
	 * and a warning is passed issued.
	 * 
	 * @param pages local document pages
	 * @param checked indices of pages to sync
	 * @param monitor status of progress
	 * @throws IOException
	 * @throws SessionExpiredException
	 * @throws ServerErrorException
	 * @throws IllegalArgumentException
	 * @throws NoConnectionException
	 * @throws NullValueException
	 * @throws JAXBException
	 * 
	 * @deprecated old and outdated; use generic method {@link #syncFilesWithDoc(List, CheckedConsumer, String, IProgressMonitor)}
	 */
	public void syncDocPages(List<TrpPage> pages, List<Boolean> checked, IProgressMonitor monitor) 
			throws IOException, SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException, NullValueException, JAXBException {
//		if (!localDoc.isLocalDoc())
//			throw new IOException("No local document given!");
		
		checkConnection(true);
		
		// alert if number of local and selected pages mismatch
		if (checked != null && pages.size() != checked.size()) {
			throw new IOException("Nr of checked list is unequal to nr of pages: "+checked.size()+"/"+pages.size());
		}
		
		// alert if no remote document (i.e. target doc) is loaded
		if (!isRemoteDoc())
			throw new IOException("No remote document loaded!");
		
		// calculate correct number of pages to sync for progress monitor output
		int nToSync = checked == null ? pages.size() : Utils.countTrue(checked);

		if (monitor != null)
			monitor.beginTask("Syncing doc pages with local pages", nToSync);

		// retrieve names of files in current doc
		List<String> remoteImgNames = doc.getPageImgNames();
		
		List<Integer> remoteIndices = new ArrayList<Integer>();
		List<Integer> syncIndices = new ArrayList<Integer>();
		
		// retrieve matching server page according to filename
		for (int i=0; i<pages.size(); ++i) {
			// loop until match is found in remote doc
			for (int j=0; j<remoteImgNames.size(); j++) {

				// check whether image filenames match (and incoming images are selected)
				if (StringUtils.contains(remoteImgNames.get(j), FilenameUtils.getBaseName(pages.get(i).getImgFileName()))
						&& (checked == null || checked.get(i))) {
					remoteIndices.add(j);
					syncIndices.add(i);
					logger.debug("Found remote match at position" + j + ": "+pages.get(i).getImgFileName());
					continue;
				}
			}
		}	

		// adopt nToSync to actual number
		nToSync = syncIndices.size();
		
		logger.debug("Synching "+nToSync+" pages " + remoteIndices);
		
		// TODO:FIXME decide what to do then !!! Until then: ignore :-) 
		// This case should occur if one or more
		// of the selected local images do not have 
		// matching images on the server 
		if (nToSync != pages.size()) {
			logger.warn("Found " + remoteIndices.size() +" pages on server, you gave me " + pages.size());
		}

		if (monitor != null)
			monitor.subTask("Found "+nToSync+ " images on server, will start syncing these now");
		
		// workflow to sync by filename
		int worked=0;
		for (int i=0; i<nToSync; ++i) {
			// metadata of entry of local document 
			TrpTranscriptMetadata tmd = pages.get(syncIndices.get(i)).getCurrentTranscript();

			logger.debug("syncing page "+(worked+1) + ": " + tmd.getUrl().getFile());
			
			if (monitor != null)
				monitor.subTask("Syncing page "+(worked+1)+" / "+nToSync + ": " + tmd.getUrl().getFile());
			
			if (monitor != null && monitor.isCanceled())
				return;
			
			conn.updateTranscript(getCurrentDocumentCollectionId(), doc.getMd().getDocId(), 
					(remoteIndices.get(i)+1), EditStatus.DONE,
					tmd.unmarshallTranscript(), tmd.getTsId(), "TRP: external source");

			if (monitor != null)
				monitor.worked(++worked);
		}
	}
	
	public List<Pair<TrpPage, File>> syncFilesWithDoc(List<Pair<TrpPage, File>> matches, CheckedConsumer<Pair<TrpPage,File>> c, String typeOfFiles, IProgressMonitor monitor) throws NoConnectionException {
		if (isRemoteDoc()) {
			checkConnection(true);
		}
		
		Objects.requireNonNull(c);

		MonitorUtil.beginTask(monitor, "Syncing "+typeOfFiles+" files with document", matches.size());
		
		int worked=0;
		List<Pair<TrpPage, File>> errors = new ArrayList<>();
		for (Pair<TrpPage, File> match : matches) {
			try {
				MonitorUtil.subTask(monitor, "Syncing "+(worked+1)+" / "+matches.size()+": "+match.getRight().getName());
				logger.debug((worked+1)+"/"+matches.size()+" Matching file '"+match.getRight().getAbsolutePath()+"' to page: "+match.getLeft());
				
				c.accept(match);
			}
			catch (Exception e) {
				logger.error("Could not match file: "+e.getMessage(), e);
				errors.add(match);
			}
			finally {
				if (MonitorUtil.isCanceled(monitor)) {
					return null;
				}
				
				MonitorUtil.worked(monitor, ++worked);
			}
		}
		return errors;
	}

	public void saveDocMd(int colId) throws SessionExpiredException, IllegalArgumentException, Exception {
		if (!isDocLoaded())
			throw new Exception("No document loaded");
	
		logger.debug("saving metadata for doc " + doc.getMd());
		if (isLocalDoc()) {
			LocalDocWriter.updateTrpDocMetadata(doc);
		} else {
			if (!isLoggedIn())
				throw new Exception("No connection");

			conn.updateDocMd(colId, doc.getMd().getDocId(), doc.getMd());
		}
		sendEvent(new DocMetadataUpdateEvent(this, doc, doc.getMd()));
	}
	
//	public void saveDocMdAttributes(int colId, List<TrpEntityAttribute> attributes) throws SessionExpiredException, IllegalArgumentException, Exception {
//		if (!isDocLoaded())
//			throw new Exception("No document loaded");
//	
//		logger.debug("saving metadata attributes for doc " + doc.getId());
//
//		if (!isLoggedIn())
//			throw new Exception("No connection -  md attributes can only be set for remote documents");
//
//		conn.updateDocMdAttributes(colId, doc.getMd().getDocId(),);
//		
//		sendEvent(new DocMetadataUpdateEvent(this, doc, doc.getMd()));
//	}
	
	/*
	 * 
	 */
	public void updateDocMd(int colId, TrpDocMetadata docMd) throws SessionExpiredException, IllegalArgumentException, Exception {
	
		logger.debug("saving metadata " + docMd);

		if (!isLoggedIn())
			throw new Exception("No connection");

		conn.updateDocMd(colId, docMd.getDocId(), docMd);
		
	}
	
	public TrpUpload uploadDocument(int colId, String folder, String title, IProgressMonitor monitor) throws IOException, Exception {
		if (!isLoggedIn())
			throw new Exception("Not logged in!");

		DocLoadConfig config = new DocLoadConfig();
		
		File inputDir = new File(folder);
		LocalDocReader.checkInputDir(inputDir);
		
		/*
		 * Check if an import of non-PAGE files is needed.
		 * Check for existence of a non-empty page dir
		 */
		final File pageDir = LocalDocReader.getPageXmlInputDir(inputDir);
		final boolean hasNonEmptyPageDir = pageDir.isDirectory() 
				&& pageDir.listFiles(ExtensionFileFilter.getXmlFileFilter()).length > 0;
		
		//if there is no page dir with files, then check if other files exist that should be converted
		if(!hasNonEmptyPageDir && 
				(LocalDocReader.getOcrXmlInputDir(inputDir).isDirectory()
				|| LocalDocReader.getAltoXmlInputDir(inputDir).isDirectory()
				|| LocalDocReader.getTxtInputDir(inputDir).isDirectory())) {
			//force page XML creation for importing existing text files
			config.setForceCreatePageXml(true);
		}
		
		TrpDoc doc = LocalDocReader.load(folder, config, monitor);
		if (title != null && !title.isEmpty()) {
			doc.getMd().setTitle(title);
		}

		return conn.uploadTrpDoc(colId, doc, monitor);
	}
	
	/**
	 * Upload pdf by extracting images first and uploading them as a new document
	 * @param colId ID of new collection
	 * @param file path of pdf file
	 * @param dirName name of directory
	 * @param monitor
	 * @throws IOException
	 * @throws Exception
	 */
	public void uploadDocumentFromPdf(int colId, String file, String dirName, final IProgressMonitor monitor) 
			throws IOException, Exception {
		if (!isLoggedIn())
			throw new Exception("Not logged in!");

		Observer o = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if(arg instanceof String) {
					monitor.subTask((String) arg);
				}
			}
		};
		
		// extract images from pdf and load images into Trp document
		TrpDoc doc = LocalDocReader.loadPdf(file, dirName, o);
		logger.debug("Extracted and loaded pdf " + file);

		conn.uploadTrpDoc(colId, doc, monitor);
	}
	
	public boolean checkDocumentOnPrivateFtp(String dirName) throws Exception {
		if (!isLoggedIn())
			throw new Exception("Not logged in!");
		
		return conn.checkDirOnFtp(dirName);
	}
	
	public void uploadDocumentFromPrivateFtp(int cId, String dirName, boolean checkForDuplicateTitle) throws Exception {
		if (!isLoggedIn())
			throw new Exception("Not logged in!");
		
		conn.ingestDocFromFtp(cId, dirName, checkForDuplicateTitle);
	}
	
	public void uploadDocumentFromMetsUrl(int cId, String metsUrlStr) throws SessionExpiredException, ServerErrorException, ClientErrorException, NoConnectionException, MalformedURLException, IOException{
//		if (!isLoggedIn())
//			throw new Exception("Not logged in!");
		checkConnection(true);
		URL metsUrl = new URL(metsUrlStr);
		if (metsUrl.getProtocol().startsWith("file")){
			conn.ingestDocFromLocalMetsUrl(cId, metsUrl);
		}
		else{
			conn.ingestDocFromUrl(cId, metsUrl);
		}
	}
	
	public void uploadDocumentFromIiifUrl(int colId, String iiifUrl) throws NoConnectionException, TrpServerErrorException, TrpClientErrorException, SessionExpiredException, UnsupportedEncodingException {
		checkConnection(true);
		logger.debug("Reached upload Doc from IIIF in storage ");
		conn.ingestDocFromIiifUrl(colId, iiifUrl);
	}
	
	public List<String> analyzeLayoutOnCurrentTranscript(List<String> regIds, boolean doBlockSeg, boolean doLineSeg, boolean doWordSeg, boolean doPolygonToBaseline, boolean doBaselineToPolygon, String jobImpl, ParameterMap pars) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException, NoConnectionException, IOException {
		checkConnection(true);
		
		if (!isRemoteDoc()) {
			throw new IOException("No remote doc loaded!");
		}
		int colId = getCurrentDocumentCollectionId();
		
		List<String> jobids = new ArrayList<>();
		if(JobImpl.FinereaderLaJob.toString().equals(jobImpl)) {
			String jobIdStr = conn.runTypewrittenBlockSegmentation(colId, getDoc().getId(), ""+getPage().getPageNr());
			jobids.add(jobIdStr);
		} else if(JobImpl.FinereaderSepJob.toString().equals(jobImpl)) {
			logger.debug("separator job");
			String jobIdStr = conn.runSeparatorSegmentation(colId, getDoc().getId(), ""+getPage().getPageNr(), pars.getParameterValue("ocrType"));
			jobids.add(jobIdStr);
		} else {
		
			DocumentSelectionDescriptor dd = new DocumentSelectionDescriptor(getDocId());
			PageDescriptor pd = dd.addPage(getPage().getPageId());
			if (regIds != null && !regIds.isEmpty()) {
				pd.getRegionIds().addAll(regIds);
			}
			pd.setTsId(getTranscriptMetadata().getTsId());
			
			List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
			dsds.add(dd);
			
			List<TrpJobStatus> jobs = conn.analyzeLayout(colId, dsds, doBlockSeg, doLineSeg, doWordSeg, doPolygonToBaseline, doBaselineToPolygon, jobImpl, pars);
			for (TrpJobStatus j : jobs) {
				jobids.add(j.getJobId());
			}
		}
				
		return jobids;
	}
	
	
	/**
	 * Wrapper method which takes a pages range string of the currently loaded document
	 */
	public List<String> analyzeLayoutOnLatestTranscriptOfPages(String pageStr, boolean doBlockSeg, boolean doLineSeg, boolean doWordSeg, boolean doPolygonToBaseline, boolean doBaselineToPolygon, String jobImpl, ParameterMap pars) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException, NoConnectionException, IOException {
		checkConnection(true);
		
		if (!isRemoteDoc()) {
			throw new IOException("No remote doc loaded!");
		}
		int colId = getCurrentDocumentCollectionId();
		
		List<String> jobids = new ArrayList<>();
		if(JobImpl.FinereaderLaJob.toString().equals(jobImpl)) {
			String jobIdStr = conn.runTypewrittenBlockSegmentation(colId, getDoc().getId(), pageStr);
			jobids.add(jobIdStr);
		} else if(JobImpl.FinereaderSepJob.toString().equals(jobImpl)) {
			String jobIdStr = conn.runSeparatorSegmentation(colId, getDoc().getId(), pageStr, pars.getParameterValue("ocrType"));
			jobids.add(jobIdStr);
		} else {
			DocumentSelectionDescriptor dd = getDoc().getDocSelectionDescriptorForPagesString(pageStr);
			List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
			dsds.add(dd);

			List<TrpJobStatus> jobs = conn.analyzeLayout(colId, dsds, doBlockSeg, doLineSeg, doWordSeg, doPolygonToBaseline, doBaselineToPolygon, jobImpl, pars);
			for (TrpJobStatus j : jobs) {
				jobids.add(j.getJobId());
			}
		}
		return jobids;
	}
	
	/*
	 *	used to start LA for several docs of the collection: admin feature 
	 */
	public List<String> analyzeLayoutOnDocumentSelectionDescriptor(List<DocumentSelectionDescriptor> dsds, boolean doBlockSeg, boolean doLineSeg, boolean doWordSeg, boolean doPolygonToBaseline, boolean doBaselineToPolygon, String jobImpl, ParameterMap pars) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException, NoConnectionException, IOException {
		checkConnection(true);
		
		int colId = getCollId();
		
		List<String> jobids = new ArrayList<>();

		List<TrpJobStatus> jobs = conn.analyzeLayout(colId, dsds, doBlockSeg, doLineSeg, doWordSeg, doPolygonToBaseline, doBaselineToPolygon, jobImpl, pars);
		for (TrpJobStatus j : jobs) {
			jobids.add(j.getJobId());
		}
		
		return jobids;
	}
	
	public String runOcr(int colId, int docId, String pageStr, OcrConfig config, String ocrType) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		return conn.runOcr(colId, docId, pageStr, config!=null ? config.getTypeFace() : null, config != null ? config.getLanguageString() : null, ocrType);
	}

	public void deleteDocument(int colId, int docId, boolean reallyDelete) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);

		conn.deleteDoc(colId, docId, reallyDelete);
	}
	
	public void deleteCurrentPage() throws NoConnectionException, SessionExpiredException, IllegalArgumentException {
		checkConnection(true);
		
		if (!isPageLoaded() || !isRemoteDoc())
			throw new IllegalArgumentException("No remote page loaded!");
		
		
		int colId = storage.getCurrentDocumentCollectionId();
		int docId = getDocId();
		int pageNr = getPage().getPageNr();
		
		deletePage(colId, docId, pageNr);
	}
	
	public void deletePage(int colId, int docId, int pageNr) throws NoConnectionException, SessionExpiredException, IllegalArgumentException {
		checkConnection(true);		
		conn.deletePage(colId, docId, pageNr);
	}
	
	public void addPage(final int colId, final int docId, final int pageNr, File imgFile, IProgressMonitor monitor) throws NoConnectionException{
		checkConnection(true);
		conn.addPage(colId, docId, pageNr, imgFile, monitor);
	}
	
	public String exportDocument(CommonExportPars pars, final IProgressMonitor monitor, ExportCache cache) throws SessionExpiredException, ServerErrorException, IllegalArgumentException,
			NoConnectionException, Exception {
		// ULB Sachsen-Anhalt Start
		if (this.getConnection() != null) {
			throw new RuntimeException("No remote connection supported!");
		}
		// ULB Sachsen-Anhalt End
		if (!isDocLoaded())
			throw new Exception("No document is loaded!");
		
		FileUtils.forceMkdir(new File(pars.getDir()));

		Set<Integer> pageIndices = CoreUtils.parseRangeListStr(pars.getPages(), Integer.MAX_VALUE);
		final int totalWork = pageIndices==null ? doc.getNPages() : pageIndices.size();		
		monitor.beginTask("Exporting document", totalWork);

		String path = pars.getDir();
		logger.debug("Trying to export document to " + path);
			
		Observer o = new Observer() {
			int c=0;
			@Override public void update(Observable o, Object arg) {
				if (monitor != null && monitor.isCanceled()) {
					return;
				}
				if (arg instanceof Integer) {
					++c;
					monitor.subTask("Processed page " + c +" / "+totalWork);
					monitor.worked(c);
				}
			}
		};
		// ULB Sachsen-Anhalt Start
		if (conn != null) {
			DocExporter de = new DocExporter(conn.newFImagestoreGetClient(), cache);
			de.exportDoc(doc, pars);
		} else {
			logger.warn("no login detected - use custom LocalDocExporter");
			ULBLocalDocExporter de = new ULBLocalDocExporter();
			de.exportDoc(doc, pars);
		}
		// ULB Sachsen-Anhalt End

		return path;
	}

	public String exportDocument(File dir, Set<Integer> pageIndices, boolean exportImg, 
			boolean exportPage, boolean exportAlto, boolean splitIntoWordsInAlto, boolean useWordLayer,
			final String fileNamePattern, final ImgType imgType, final IProgressMonitor monitor, ExportCache cache) throws SessionExpiredException, ServerErrorException, IllegalArgumentException,
			NoConnectionException, Exception {
		if (!isDocLoaded())
			throw new Exception("No document is loaded!");
		
		FileUtils.forceMkdir(dir);

		// FileUtils.forceMkdir(new File(baseDir));

		// String dirName = isLocalDoc() ?
		// doc.getMd().getLocalFolder().getName() : "trp_doc_"+doc.getId();
		// String path = baseDir + "/" + dirName;
		// File destDir = new File(path);

		// if (destDir.exists())
		// throw new
		// Exception("Export directory already exists: "+destDir.getAbsolutePath());
		
		final int totalWork = pageIndices==null ? doc.getNPages() : pageIndices.size();		
		monitor.beginTask("Exporting document", totalWork);

		String path = dir.getAbsolutePath();
		logger.debug("Trying to export document to " + path);
			
		Observer o = new Observer() {
			int c=0;
			@Override public void update(Observable o, Object arg) {
				if (monitor != null && monitor.isCanceled()) {
					return;
				}
				if (arg instanceof Integer) {
					++c;
					monitor.subTask("Processed page " + c +" / "+totalWork);
					monitor.worked(c);
				}
			}
		};
		DocExporter de = new DocExporter(conn.newFImagestoreGetClient(), cache);
		de.addObserver(o);
		de.writeRawDoc(doc, path, true, pageIndices, exportImg, exportPage, exportAlto, splitIntoWordsInAlto, useWordLayer, fileNamePattern, imgType);

		return path;
	}
	
	public String exportPdf(File pdf, Set<Integer> pageIndices, IProgressMonitor monitor, ExportCache cache, CommonExportPars commonPars,
			PdfExportPars pdfPars) throws MalformedURLException, DocumentException, IOException, JAXBException, InterruptedException, Exception {
		if (!isDocLoaded())
			throw new Exception("No document is loaded!");
		if (pdf.isDirectory()) {
			throw new IOException(pdf.getAbsolutePath() + " is not a valid file name!");
		}
		if (!pdf.getParentFile().exists()) {
			throw new IOException("Directory " + pdf.getParent() + " does not exist!");
		}

		logger.debug("Trying to export PDF file to " + pdf.getAbsolutePath());
		
		final int totalWork = pageIndices==null ? doc.getNPages() : pageIndices.size();
		monitor.beginTask("Creating PDF document" , totalWork);

		final PdfExporter pdfExp = new PdfExporter();
		Observer o = new Observer() {
			int c=0;
			@Override public void update(Observable o, Object arg) {
				if (monitor != null && monitor.isCanceled()) {
					pdfExp.cancel = true;
//					return;
				}
				
				if (arg instanceof Integer) {
					++c;
					monitor.setTaskName("Exported page " + c);
					monitor.worked(c);
					
					
				} else if (arg instanceof String) {
					monitor.setTaskName((String) arg);
				}
			}
		};
		pdfExp.addObserver(o);
		
		pdf = pdfExp.export(doc, pdf.getAbsolutePath(), pageIndices, cache, commonPars, pdfPars);

		return pdf.getAbsolutePath();
		
	}

	//old method
	public String exportPdf(File pdf, Set<Integer> pageIndices, final IProgressMonitor monitor, final boolean extraTextPages, final boolean imagesOnly, final boolean highlightTags, final boolean highlightArticles, final boolean wordBased, final boolean doBlackening, boolean createTitle, ExportCache cache, String exportFontname, ImgType imgType) throws MalformedURLException, DocumentException,
			IOException, JAXBException, InterruptedException, Exception {
		
		PdfExportPars params = new PdfExportPars(imagesOnly, !imagesOnly, extraTextPages, highlightTags, highlightArticles, false);
		params.setPdfImgQuality(imgType);
		
		CommonExportPars commonPars = new CommonExportPars();
		commonPars.setDoBlackening(doBlackening);
		commonPars.setDoCreateTitle(createTitle);
		commonPars.setFont(exportFontname);
		commonPars.setWriteTextOnWordLevel(wordBased);
		
		return exportPdf(pdf, pageIndices, monitor, cache, commonPars, params);
		
//		if (!isDocLoaded())
//			throw new Exception("No document is loaded!");
//		if (pdf.isDirectory()) {
//			throw new IOException(pdf.getAbsolutePath() + " is not a valid file name!");
//		}
//		if (!pdf.getParentFile().exists()) {
//			throw new IOException("Directory " + pdf.getParent() + " does not exist!");
//		}
//
//		logger.debug("Trying to export PDF file to " + pdf.getAbsolutePath());
//		
//		final int totalWork = pageIndices==null ? doc.getNPages() : pageIndices.size();
//		monitor.beginTask("Creating PDF document" , totalWork);
//
//		final PdfExporter pdfExp = new PdfExporter();
//		Observer o = new Observer() {
//			int c=0;
//			@Override public void update(Observable o, Object arg) {
//				if (monitor != null && monitor.isCanceled()) {
//					pdfExp.cancel = true;
////					return;
//				}
//				
//				if (arg instanceof Integer) {
//					++c;
//					monitor.setTaskName("Exported page " + c);
//					monitor.worked(c);
//					
//					
//				} else if (arg instanceof String) {
//					monitor.setTaskName((String) arg);
//				}
//			}
//		};
//		pdfExp.addObserver(o);
//		
//		pdf = pdfExp.export(doc, pdf.getAbsolutePath(), pageIndices, wordBased, extraTextPages, imagesOnly, highlightTags, highlightArticles, doBlackening, createTitle, cache, exportFontname, imgType);
//
//		return pdf.getAbsolutePath();
	}

	public String exportTei(File tei, CommonExportPars commonPars, TeiExportPars pars, IProgressMonitor monitor, ExportCache cache) throws IOException, Exception {
		if (!isDocLoaded())
			throw new Exception("No document is loaded!");
		if (tei.isDirectory()) {
			throw new IOException(tei.getAbsolutePath() + " is not a valid file name!");
		}
		if (!tei.getParentFile().exists()) {
			throw new IOException("Directory " + tei.getParent() + " does not exist!");
		}

		logger.debug("Trying to export TEI XML file to " + tei.getAbsolutePath());
		
//		TrpTeiDomBuilder builder = new TrpTeiDomBuilder(doc, mode, monitor, pageIndices);
		ATeiBuilder builder = new TrpTeiStringBuilder(doc, commonPars, pars, monitor);
		builder.addTranscriptsFromCache(cache);
		builder.buildTei();
		
		if (monitor != null)
			monitor.setTaskName("Writing TEI XML file");
		
		builder.writeTeiXml(tei);

		return tei.getAbsolutePath();
	}
	
	public String exportAlto(File altoDir, final IProgressMonitor monitor) throws IOException, Exception {
		
		FileUtils.forceMkdir(altoDir);
		
		if (!isDocLoaded())
			throw new Exception("No document is loaded!");

		if (!altoDir.exists()) {
			throw new IOException("Directory " + altoDir + " does not exist!");
		}

		logger.debug("Trying to export Alto XML file to " + altoDir.getAbsolutePath());
		AltoExporter altoExp = new AltoExporter();
		Observer o = new Observer() {
			@Override public void update(Observable o, Object arg) {
				if (arg instanceof Integer) {
					int i = (Integer) arg;
					monitor.setTaskName("Exporting page " + i);
					monitor.worked(i);
				} else if (arg instanceof String) {
					monitor.setTaskName((String) arg);
				}
			}
		};
		altoExp.addObserver(o);
		altoExp.export(doc, altoDir.getAbsolutePath());

		return altoDir.getAbsolutePath();
	}
	
	public List<TrpCollection> getCollections() { return collections; }
	
	public List<TrpDocMetadata> getDeletedDocList() {
		return deletedDocList;
	}

	public TrpCollection getCollection(int colId) {
		for (TrpCollection c : collections) {
			//logger.debug("the collections in the storage: " + c.getColId());
			if (c.getColId() == colId) {
				return c;
			}
		}
		return null;
	}
	
	public List<TrpCollection> getCollectionsCanManage() {
		List<TrpCollection> ccm = new ArrayList<>();
		for (TrpCollection c : collections) {
			 if (c.getRole()==null || c.getRole().canManage()) { // if role==null -> admin!
				 ccm.add(c);
			 }
		}
		
		return ccm;
	}
	
	public synchronized void clearCollections() {
		collections.clear();
		//reset selected collId to detect change on next login
		collId = 0;
		sendEvent(new CollectionsLoadEvent(this, user, collections));
	}
	
	public void reloadCollections() throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		
		logger.debug("loading collections from server");
		SSW.SW.start();
		List<TrpCollection> collectionsFromServer = conn.getAllCollections(0, 0, null, null);
		logger.debug("got collections: "+(collectionsFromServer==null?"null":collectionsFromServer.size()));
		SSW.SW.stop(true, "time for loading collections: ", logger);
		
		collections.clear();
		collections.addAll(collectionsFromServer);

		sendEvent(new CollectionsLoadEvent(this, user, collections));
	}
	
	public int addCollection(String name) throws NoConnectionException, SessionExpiredException, ServerErrorException {
		checkConnection(true);
		
		return conn.createCollection(name);
	}
	

	public static void checkConnection(TrpServerConn conn, boolean checkLoggedIn) throws NoConnectionException {
		if (conn == null || (checkLoggedIn && conn.getUserLogin() == null)) {
			throw new NoConnectionException("No connection to the server!");
		}
	}
		
	public void checkConnection(boolean checkLoggedIn) throws NoConnectionException {
		checkConnection(conn, checkLoggedIn);
	}
	
//	public List<EdFeature> getEditDeclFeatures(TrpDoc doc) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
////		checkConnection(true);
//		List<EdFeature> features;
//		if(isLoggedIn() && isRemoteDoc(doc)) {
//			if (doc.getCollection() == null)
//				throw new IllegalArgumentException("Collection not set for remote doc: "+doc);
//			
//			features = conn.getEditDeclByDoc(doc.getCollection().getColId(), doc.getId());
//		} else {
//			File editDecl = new File(doc.getMd().getLocalFolder() + File.separator + "editorialDeclaration.xml");
//			if(editDecl.isFile()){
//				try {
//					JaxbList<EdFeature> list = JaxbUtils.unmarshal(editDecl, JaxbList.class, EdFeature.class, EdOption.class);
//					features = list.getList();
//				} catch (FileNotFoundException | JAXBException e) {
//					features = new ArrayList<EdFeature>(0);
//					e.printStackTrace();
//				}
//			} else {
//				features = new ArrayList<EdFeature>(0);
//			}
//		}
//		return features;
//		
//	}
	
	public List<EdFeature> getAvailFeatures() throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
//		checkConnection(true);
		List<EdFeature> features;
		if(isLoggedIn() && isRemoteDoc()){
			features = conn.getEditDeclFeatures(getCurrentDocumentCollectionId());
		} else {
			try {
				features = new TrpServerConn(TrpServerConn.PROD_SERVER_URI).getEditDeclFeatures(0);
			} catch (LoginException e) {
				//is only thrown if uriStr is null or empty
				e.printStackTrace();
				features = new ArrayList<>(0);
			}
		}
		return features;
	}
	
	public List<EdFeature> getEditDeclFeatures() throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
//		checkConnection(true);
		List<EdFeature> features;
		if(isLoggedIn() && isRemoteDoc()){
			features = conn.getEditDeclByDoc(getCurrentDocumentCollectionId(), doc.getId());
		} else {
			features = LocalDocReader.readEditDeclFeatures(getDoc().getMd().getLocalFolder());
		}
		return features;
	}
	
	public void saveEditDecl(List<EdFeature> feats) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException, FileNotFoundException, JAXBException {
		if(isLoggedIn() && isRemoteDoc()){
			saveEditDecl(doc.getId(), feats);
		} else {
			LocalDocWriter.writeEditDeclFeatures(feats, getDoc().getMd().getLocalFolder());
		}
		
		doc.setEdDeclList(feats);
	}
	
	public void saveEditDecl(final int docId, List<EdFeature> feats) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		conn.postEditDecl(getCurrentDocumentCollectionId(), docId, feats);
	}

	public void storeEdFeature(EdFeature feat, boolean isCollectionFeature) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		if(isCollectionFeature){
			feat.setColId(getCurrentDocumentCollectionId());
		} else {
			feat.setColId(null);
		}
		conn.postEditDeclFeature(getCurrentDocumentCollectionId(), feat);
	}

	public void deleteEdFeature(EdFeature feat) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		conn.deleteEditDeclFeature(getCurrentDocumentCollectionId(), feat);
	}
	
	public void storeCrowdProject(TrpCrowdProject project, int collId) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		conn.postCrowdProject(collId, project);
	}
	
	public int storeCrowdProjectMilestone(TrpCrowdProjectMilestone milestone, int collId) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		return conn.postCrowdProjectMilestone(collId, milestone);
	}
	
	public int storeCrowdProjectMessage(TrpCrowdProjectMessage message, int collId) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		return conn.postCrowdProjectMessage(collId, message);
	}
	
	public void storeEdOption(EdOption opt) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		conn.postEditDeclOption(getCurrentDocumentCollectionId(), opt);
	}

	public void deleteEdOption(EdOption opt) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		conn.deleteEditDeclOption(getCurrentDocumentCollectionId(), opt);
	}

	public void deleteTranscript(TrpTranscriptMetadata tMd) throws NoConnectionException, SessionExpiredException, ServerErrorException, IllegalArgumentException {
		checkConnection(true);
		conn.deleteTranscript(this.getCurrentDocumentCollectionId(), this.getDoc().getId(), 
				this.getPage().getPageNr(), tMd.getKey());
	}
	
	public TrpRole getRoleOfUserInCurrentCollection() {
		return StorageUtil.getRoleOfUserInCurrentCollection();
	}
	
	public String createSample(Map<TrpDocMetadata, List<TrpPage>> sampleDocMap, int nrOfLines, String sampleName, String sampleDescription, Double blLenThresh, boolean keepText, Integer maxLinesPerPage) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		List<DocumentSelectionDescriptor> descList = null;
		try {
			descList = DescriptorUtils.buildCompleteSelectionDescriptorList(sampleDocMap, null);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not build selection descriptor list");
		}
		return conn.createSampleJob(collId,descList,nrOfLines, sampleName, sampleDescription, blLenThresh, keepText, maxLinesPerPage);
	}
	
	public String createSamplePages(Map<TrpDocMetadata, List<TrpPage>> sampleDocMap, int nrOfPages, String sampleName, String sampleDescription, String option) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		List<DocumentSelectionDescriptor> descList = null;
		try {
			descList = DescriptorUtils.buildCompleteSelectionDescriptorList(sampleDocMap, null);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not build selection descriptor list");
		}
		return conn.createSamplePagesJob(collId, descList, nrOfPages, sampleName, sampleDescription, option);
	}
	
	public TrpJobStatus computeSampleRate(int docId, ParameterMap params) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		return conn.computeSampleJob(docId,params);
	}

	
	public TrpJobStatus computeErrorRate(int docId, final String pageStr, ParameterMap params) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		//TODO
		return conn.computeErrorRateWithJob(docId, pageStr, params);
		
	}
	
	public TrpErrorRateResult computeErrorRate(TrpTranscriptMetadata ref, TrpTranscriptMetadata hyp) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		if(ref == null || hyp == null){
			throw new IllegalArgumentException("A parameter is null!");
		}	
		return conn.computeErrorRate(ref.getKey(), hyp.getKey());
	}
	
	@Deprecated
	public String computeWer(TrpTranscriptMetadata ref, TrpTranscriptMetadata hyp) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		if(ref == null || hyp == null){
			throw new IllegalArgumentException("A parameter is null!");
		}	
		String result = conn.computeWer(ref.getKey(), hyp.getKey());
		result = result.replace("WER", "Word Error Rate:");
		result = result.replace("CER", "Character Error Rate:");
		return result;
	}
	
	public ImageMetadata getCurrentImageMetadata() {
		return imgMd;
	}
	
	private void setCurrentImageMetadata() throws IOException {
		imgMd = getImageMetadata(doc, getPage());
		
//		TrpPage page = getPage();
//		if (isLocalDoc() || page == null) {
//			imgMd = null;
//			return;
//		}
//		
//		try (FimgStoreGetClient getter = new FimgStoreGetClient(page.getUrl())) {
//			imgMd = (ImageMetadata)getter.getFileMd(page.getKey());
//		} catch (Exception e) {
//			logger.error("Couldn't read metadata for file: "+page.getUrl());
//			imgMd = null;
//		}
	}
	
	private static ImageMetadata getImageMetadata(TrpDoc doc, TrpPage page) throws IOException{
//		TrpPage page = getPage();
		if (doc.isLocalDoc() || page == null) {
			return null;
		}
		
		try (FimgStoreGetClient getter = new FimgStoreGetClient(page.getUrl())) {
			return (ImageMetadata)getter.getFileMd(page.getKey());
		} catch (Exception e) {
			logger.error("Couldn't read metadata for file: "+page.getUrl());
			return null;
		}
	}

	public void replacePageImgFile(File imgFile, IProgressMonitor monitor) throws Exception {
		checkConnection(true);
		TrpPage newPage = conn.replacePageImage(this.getCurrentDocumentCollectionId(), this.getDocId(), 
				this.getPage().getPageNr(), imgFile, monitor);
		this.doc.getPages().set(getPageIndex(), newPage);
		this.page = newPage;
	}
	
	public FulltextSearchResult searchFulltext(String query, SearchType type, Integer start, Integer rows, List<String> filters) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException {
		checkConnection(true);
		return conn.getSearchCalls().searchFulltext(query, type, start, rows, filters);
	}
	
	public void movePage(final int colId, final int docId, final int pageNr, final int toPageNr) throws SessionExpiredException, ServerErrorException, ClientErrorException, NoConnectionException{
		checkConnection(true);
		conn.movePage(colId, docId, pageNr, toPageNr);
	}

	public List<TrpDocDir> listDocDirsOnFtp() throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		return conn.listDocsOnFtp();
	}

	public List<TrpEvent> getEvents() throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		TrpSettings sets = TrpConfig.getTrpSettings();
		List<TrpEvent> events = conn.getNextEvents(sets.getShowEventsMaxDays());
		Collections.sort(events);
		List<String> lines;
		try{
			lines = FileUtils.readLines(new File(sets.getEventsTxtFileName()));
		} catch (Exception e){
			logger.debug("No " + sets.getEventsTxtFileName() + " found. Show all events...");
			lines = new ArrayList<String>(0);
		}
		for(String l : lines){
			int id;
			try{
				id = Integer.parseInt(l);
			} catch (Exception e){
				continue;
			}
			Iterator<TrpEvent> it = events.iterator();
			while(it.hasNext()){
				TrpEvent ev = it.next();
				if(ev.getId() == id){
					it.remove();
//					break;
				}
			}
		}
		return events;
	}
	public void markEventAsRead(int id) throws IOException{
		TrpSettings sets = TrpConfig.getTrpSettings();
		File eventsTxt = new File(sets.getEventsTxtFileName());
		ArrayList<String> lines = new ArrayList<>(1);
		lines.add(""+id);
		FileUtils.writeLines(eventsTxt, lines, true);
	}

	public String duplicateDocument(int colId, int docId, String newName, Integer toColId) throws SessionExpiredException, ServerErrorException, IllegalArgumentException, NoConnectionException {
		checkConnection(true);
		
		return conn.duplicateDocument(colId, docId, newName, toColId);		
	}

	public void batchReplaceImages(List<TrpPage> checkedPages, List<URL> checkedUrls, IProgressMonitor monitor) throws Exception {
		logger.info("batch replacing "+checkedPages.size()+" images!");

		checkConnection(true);
		
		if (checkedPages.size() != checkedUrls.size()) {
			throw new IOException("Nr of checked pages is unequal to nr of images: "+checkedPages.size()+"/"+checkedUrls.size());
		}
		if (!isRemoteDoc())
			throw new IOException("No remote document loaded!");
		
		int N = checkedPages.size();
		
		if (monitor != null)
			monitor.beginTask("Replacing images", N);

		for (int i=0; i<checkedPages.size(); ++i) {
			if (monitor.isCanceled())
				break;
			
			if (monitor != null)
				monitor.subTask("Replacing page "+(i+1)+" / "+N);			
			
			TrpPage p = checkedPages.get(i);
			int pageIndex = doc.getPageIndex(p);
			
			URL u = checkedUrls.get(i);
			
			if (!CoreUtils.isLocalFile(u))
				throw new Exception("Not a local file: "+u);
			
			File imgFile = FileUtils.toFile(u);
			
			TrpPage newPage = conn.replacePageImage(getCurrentDocumentCollectionId(), getDocId(), p.getPageNr(), imgFile, null);
			doc.getPages().set(pageIndex, newPage);
			
			if (monitor != null)
				monitor.worked(i+1);
		}
	}

	public void updateProxySettings() {
		ProxyPrefs p = TrpGuiPrefs.getProxyPrefs();
		if(p.isEnabled()) {
			logger.debug("PROXY IS ENABLED");
			ProxyUtils.setProxy(p);
		} else {
			logger.debug("PROXY IS DISABLED");
			ProxyUtils.unsetProxy();
		}
		ProxyUtils.logProxySettings();
	}
	
	/*
	 * HTR stuff
	 */

	@Deprecated
	public void deleteHtr(TrpHtr htr) throws NoConnectionException, TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		checkConnection(true);
		if(htr == null) {
			logger.debug("htr argument is null in deleteHtr. Doing nothing.");
			return;
		}
		conn.deleteHtr(getCollId(), htr.getHtrId());
	}
	
	@Deprecated
	public TrpHtr updateHtrMetadata(TrpHtr htr) throws NoConnectionException, TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		checkConnection(true);
		if(htr == null) {
			logger.debug("htr argument is null in deleteHtr. Doing nothing.");
			return null;
		}
		return conn.updateHtrMetadata(getCollId(), htr);
		//FIXME next line is commented out, but data in tables is now outdated!
		//reloadHtrs();
	}
	
	@Deprecated
	public void addHtrToCollection(TrpHtr htr, TrpCollection col) throws SessionExpiredException, ServerErrorException, ClientErrorException, NoConnectionException {
		checkConnection(true);
		conn.addHtrToCollection(htr.getHtrId(), this.collId, col.getColId());		
	}
	
	@Deprecated
	public void removeHtrFromCollection(TrpHtr htr) throws SessionExpiredException, ServerErrorException, ClientErrorException, NoConnectionException {
		checkConnection(true);
		conn.removeHtrFromCollection(htr.getHtrId(), this.collId);		
	}
	
	public void deleteModel(TrpModelMetadata model) throws NoConnectionException, TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		checkConnection(true);
		if(model == null) {
			logger.debug("htr argument is null in deleteHtr. Doing nothing.");
			return;
		}
		conn.getModelCalls().setModelDeleted(model);
	}
	
	public TrpModelMetadata updateModelMetadata(TrpModelMetadata model) throws NoConnectionException, TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		checkConnection(true);
		if(model == null) {
			logger.debug("htr argument is null in deleteHtr. Doing nothing.");
			return null;
		}
		return conn.getModelCalls().updateModel(model);
		//FIXME next line is commented out, but data in tables is now outdated!
		//reloadHtrs();
	}
	
	public void addModelToCollection(TrpModelMetadata model, TrpCollection col) throws SessionExpiredException, ServerErrorException, ClientErrorException, NoConnectionException {
		checkConnection(true);
		conn.getModelCalls().addModelToCollection(model, col.getColId());		
	}
	
	public void removeModelFromCurrentCollection(TrpModelMetadata model) throws SessionExpiredException, ServerErrorException, ClientErrorException, NoConnectionException {
		checkConnection(true);
		conn.getModelCalls().removeModelFromCollection(model, this.collId);		
	}
	
	public String runHtr(String pages, TextRecognitionConfig config) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException {
		checkConnection(true);
		switch(config.getMode()) {
		case CITlab:
			return conn.runCitLabHtr(getCurrentDocumentCollectionId(), getDocId(), pages, 
					config.getHtrId(), config.getDictionary(), config.isDoLinePolygonSimplification(), config.isKeepOriginalLinePolygons(), 
					config.isDoStoreConfMats(), config.getStructures(), config.getCreditSelectionStrategy());
		case UPVLC:
			return conn.getPyLaiaCalls().runPyLaiaHtrDecode(getCurrentDocumentCollectionId(), getDocId(), pages, 
					config.getHtrId(), config.getLanguageModel(),
					config.isUseExistingLinePolygons(), config.isDoLinePolygonSimplification(), config.isClearLines(), config.isKeepOriginalLinePolygons(), config.isDoWordSeg(),
					config.isWriteKwsIndexFiles(), config.getNBest(), config.getBatchSize(),
					config.getStructures(), 
					config.getCreditSelectionStrategy());			
		default:
			return null;
		}
	}
	
	public String runHtr(DocumentSelectionDescriptor descriptor, TextRecognitionConfig config) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException {
		checkConnection(true);
		switch(config.getMode()) {
		case CITlab:
			return conn.runCitLabHtr(getCurrentDocumentCollectionId(), descriptor, config.getHtrId(), 
					config.getDictionary(), config.isDoLinePolygonSimplification(), config.isKeepOriginalLinePolygons(), 
					config.isDoStoreConfMats(), config.getStructures(), config.getCreditSelectionStrategy()
					);
		case UPVLC:
			return conn.getPyLaiaCalls().runPyLaiaHtrDecode(getCurrentDocumentCollectionId(), descriptor, config.getHtrId(), config.getLanguageModel(), 
					config.isUseExistingLinePolygons(), config.isDoLinePolygonSimplification(), config.isClearLines(), config.isKeepOriginalLinePolygons(), config.isDoWordSeg(),
					config.isWriteKwsIndexFiles(), config.getNBest(), config.getBatchSize(),
					config.getStructures(),
					config.getCreditSelectionStrategy()
					);
		default:
			return null;
		}
	}
	
	public String runHtr(int docId, String pages, TextRecognitionConfig config) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException {
		checkConnection(true);
		switch(config.getMode()) {
		case CITlab:
			return conn.runCitLabHtr(getCurrentDocumentCollectionId(), docId, pages, 
					config.getHtrId(), config.getDictionary(), config.isDoLinePolygonSimplification(), config.isKeepOriginalLinePolygons(), config.isDoStoreConfMats(), 
					config.getStructures(), config.getCreditSelectionStrategy());
		case UPVLC:
			return conn.getPyLaiaCalls().runPyLaiaHtrDecode(getCurrentDocumentCollectionId(), docId, pages, 
					config.getHtrId(), config.getLanguageModel(), 
					config.isUseExistingLinePolygons(), config.isDoLinePolygonSimplification(), config.isClearLines(), config.isKeepOriginalLinePolygons(), config.isDoWordSeg(),
					config.isWriteKwsIndexFiles(), config.getNBest(), config.getBatchSize(),
					config.getStructures(),
					config.getCreditSelectionStrategy());
		default:
			return null;
		}
	}
	
	public String runHtrTraining(CitLabHtrTrainConfig config) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		if(config == null) {
			throw new IllegalArgumentException("Config is null!");
		}
		return conn.runCitLabHtrTraining(config);
	}
	
	public String runPyLaiaTraining(PyLaiaHtrTrainConfig config) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		if(config == null) {
			throw new IllegalArgumentException("Config is null!");
		}
		return conn.getPyLaiaCalls().runPyLaiaTraining(config);
	}	
	
	public String runCITlabLATraining(CITlabLaTrainConfig config) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		if(config == null) {
			throw new IllegalArgumentException("Config is null!");
		}
		
		return conn.runCITlabLATraining(config);
	}
	
	public String runCitLabText2Image(CitLabSemiSupervisedHtrTrainConfig config) throws SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException {
		if(config == null) {
			throw new IllegalArgumentException("Config is null!");
		}
		return conn.runCitLabText2Image(config);
	}

	public String runDocUnderstanding(int docId, String pages, int model) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException {
		return conn.getDuCalls().runDocUnderstandingDecode(getCurrentDocumentCollectionId(), docId, pages, model);
	}

	public void saveTextRecognitionConfig(TextRecognitionConfig config) {
		RecognitionPreferences.save(collId, this.conn.getServerUri(), config);		
	}
	
	public TextRecognitionConfig loadTextRecognitionConfig() {
		return RecognitionPreferences.getHtrConfig(collId, this.conn.getServerUri());
	}
	
	public OcrConfig loadOcrConfig() {
		return RecognitionPreferences.getOcrConfig(collId,  this.conn.getServerUri());
	}
	
	public void saveOcrConfig(OcrConfig config) {
		RecognitionPreferences.save(collId,  this.conn.getServerUri(), config);
	}
	
	public List<String> getHtrDicts() throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException, IllegalArgumentException{
		checkConnection(true);
		List<String> sortedDictList = conn.getHtrDictListText();
		Collections.sort(sortedDictList);
		return sortedDictList;
	}

	public TrpCrowdProject loadCrowdProject(int colId) throws NoConnectionException, SessionExpiredException, ServerErrorException, ClientErrorException {
		checkConnection(true);
		return conn.getCrowdProject(colId);		
	}

	public boolean reloadDocWithAllTranscripts() throws SessionExpiredException, ClientErrorException, IllegalArgumentException {
		if (isRemoteDoc()) {
			doc = conn.getTrpDoc(this.collId, doc.getMd().getDocId(), -1);
			return true;
		}
		return false;
	}
	
	
	// CUSTOM TAG SPEC STUFF:
	
	public List<CustomTagSpec> getCustomTagSpecs() {
		return customTagSpecs;
	}
	
	public CustomTagSpec getCustomTagSpec(String tagname) {
		for (CustomTagSpec spec : customTagSpecs){
			if (spec.getCustomTag().getTagName().contentEquals(tagname)) {
				return spec;
			}
			
		}
		return null;
	}
	
	public List<String> getCustomTagSpecsStrings() {
		return customTagSpecs.stream().map(t -> t.getCustomTag().getTagName()).collect(Collectors.toList());
	}
	
	public List<StructCustomTagSpec> getStructCustomTagSpecs() {
		return structCustomTagSpecs;
	}
	
	public List<String> getStructCustomTagSpecsTypeStrings() {
		return structCustomTagSpecs.stream().map(t -> t.getCustomTag().getType()).collect(Collectors.toList());
	}
	
	public void addCustomTagSpec(CustomTagSpec tagSpec) {
		
			customTagSpecs.add(tagSpec);
			CustomTagSpecUtil.checkTagSpecsConsistency(customTagSpecs);
			sendEvent(new TagSpecsChangedEvent(this, customTagSpecs));
		
	
		try {
			storeCustomTagSpecsForCurrentCollection();
		} catch (ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void removeCustomTagSpec(CustomTagSpec tagDef, boolean collectionSpecific) {
		if (collectionSpecific){
			collectionSpecificTagSpecs.remove(tagDef);
			sendEvent(new TagSpecsChangedEvent(this, collectionSpecificTagSpecs));
			return;
		}
		
		customTagSpecs.remove(tagDef);
		CustomTagSpecUtil.checkTagSpecsConsistency(customTagSpecs);
		sendEvent(new TagSpecsChangedEvent(this, customTagSpecs));
		
		try {
			storeCustomTagSpecsForCurrentCollection();
		} catch (ClientErrorException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void addStructCustomTagSpec(StructCustomTagSpec tagSpec) {
		structCustomTagSpecs.add(tagSpec);
		CustomTagSpecUtil.checkTagSpecsConsistency(structCustomTagSpecs);
		sendEvent(new StructTagSpecsChangedEvent(this, structCustomTagSpecs));
	
		storeStructCustomTagSpecsForCurrentCollection();
	}
	
	public String getNewStructCustomTagColor() {
		return CustomTagSpecUtil.getNewStructSpecColor(structCustomTagSpecs);
	}
	
	public void removeStructCustomTagSpec(CustomTagSpec tagDef) {
		structCustomTagSpecs.remove(tagDef);
		CustomTagSpecUtil.checkTagSpecsConsistency(structCustomTagSpecs);
		sendEvent(new StructTagSpecsChangedEvent(this, structCustomTagSpecs));
		
		storeStructCustomTagSpecsForCurrentCollection();
	}
	
	public void signalCustomTagSpecsChanged() {
		CustomTagSpecUtil.checkTagSpecsConsistency(customTagSpecs);
		sendEvent(new TagSpecsChangedEvent(this, customTagSpecs));
		try {
			storeCustomTagSpecsForCurrentCollection();
		} catch (ClientErrorException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void signalStructCustomTagSpecsChanged() {
		CustomTagSpecUtil.checkTagSpecsConsistency(structCustomTagSpecs);
		sendEvent(new StructTagSpecsChangedEvent(this, structCustomTagSpecs));
		storeStructCustomTagSpecsForCurrentCollection();
	}
	
	public CustomTagSpec getCustomTagSpecWithShortCut(String shortCut) {
		return CustomTagSpecUtil.getCustomTagSpecWithShortCut(customTagSpecs, shortCut);
	}
	
	public StructCustomTagSpec getStructCustomTagSpecWithShortCut(String shortCut) {
		return CustomTagSpecUtil.getCustomTagSpecWithShortCut(structCustomTagSpecs, shortCut);
	}
	
	private void storeCustomTagSpecsForCurrentCollection() {
		logger.debug("updating custom tag specs for local mode, customTagSpecs: "+customTagSpecs);
		CustomTagSpecUtil.writeCustomTagSpecsToSettings(customTagSpecs);
	}
	
	public void storeStructCustomTagSpecsForCurrentCollection() {
		logger.debug("updating struct custom tag specs for local mode, structCustomTagSpecs: "+structCustomTagSpecs);
		CustomTagSpecUtil.writeStructCustomTagSpecsToSettings(structCustomTagSpecs);
	}
	
	private void readTagSpecsFromLocalSettings() {
		customTagSpecs.clear();
		customTagSpecs.addAll(CustomTagSpecUtil.readCustomTagSpecsFromSettings());
		
		sendEvent(new TagSpecsChangedEvent(this, customTagSpecs));
	}
	
	public void readUserTagSpecsFromDB() {
		customTagSpecs.clear();
		try {
			if (conn != null){
				logger.debug("tag Defs user = " + conn.getTagDefsUser());
				
				List<CustomTagSpec> tagSpecs = CustomTagSpecUtil.readCustomTagSpecsFromJsonString(conn.getTagDefsUser());

				if (tagSpecs != null){
					customTagSpecs.addAll(tagSpecs);
				}
			}
			else {
				logger.debug("conn is null = ");
			}
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//sendEvent(new TagDefsChangedEvent(this, collectionSpecificTagSpecs));
	}
	
//	public void readCollectionTagSpecsFromDB() {
//		collectionSpecificTagSpecs.clear();
//		try {
//			if (conn != null){
//				logger.debug("tag Defs collection = " + conn.getTagDefsCollection(collId));
//				//List<CustomTagSpec> tagDefs = CustomTagSpecUtil.readCustomTagSpecsFromJsonString(conn.getTagDefsUser());
//				
//				List<String> tagNames = CustomTagSpecDBUtil.readCustomTagSpecsFromJsonString(conn.getTagDefsCollection(collId));
//				List<CustomTagSpec> tagDefs = new ArrayList<CustomTagSpec>();
//
//				for (String tn : tagNames){
//					CustomTag ct = CustomTagFactory.getTagObjectFromRegistry(tn, true);
//					//not null if tagname from DB is already known in the client
//					if (ct != null){
//						CustomTagSpec cts = new CustomTagSpec(ct);
//						tagDefs.add(cts);
//					}
//					else{
//						CustomTagFactory.addToRegistry(ct, CustomTagFactory.getTagColor(tn), CustomTagFactory.is, false, true);
//						//TODO: add custom tag from DB to object registry because it is it known for this user
//					}
//				}
//
//				if (tagDefs != null){
//					collectionSpecificTagSpecs.addAll(tagDefs);
//				}
//			}
//		} catch (SessionExpiredException | ServerErrorException | ClientErrorException | IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		//sendEvent(new TagDefsChangedEvent(this, collectionSpecificTagSpecs));
//	}

	private void readStructTagSpecsFromLocalSettings() {
		structCustomTagSpecs.clear();
		structCustomTagSpecs.addAll(CustomTagSpecUtil.readStructCustomTagSpecsFromSettings());
		
		if (structCustomTagSpecs.isEmpty()) {
			structCustomTagSpecs.addAll(getDefaultStructCustomTagSpecs());
		}
		
		sendEvent(new StructTagSpecsChangedEvent(this, structCustomTagSpecs));
	}
	
	private void addForeignStructTagSpecsFromTranscript() {
		if (transcript != null) {
			int sizeBefore = structCustomTagSpecs.size();
			for (ITrpShapeType st : transcript.getPage().getAllShapes(true)) {
				String structType = CustomTagUtil.getStructure(st);	

				if (!StringUtils.isEmpty(structType)) {
					
					StructCustomTagSpec spec = getStructCustomTagSpec(structType);		
					if (spec == null) { // tag not found --> create new one and add it to the list with a new color!
						spec = new StructCustomTagSpec(new StructureTag(structType), getNewStructCustomTagColor());
						//logger.debug("adding foreign page from transcript: "+spec);
						structCustomTagSpecs.add(spec);
					}

				}
			}
			if (sizeBefore != structCustomTagSpecs.size()) {
				logger.debug("added "+(structCustomTagSpecs.size()-sizeBefore)+" foreign tags!");
				sendEvent(new StructTagSpecsChangedEvent(this, structCustomTagSpecs));
			}
		}
	}
	
	public void restoreDefaultStructCustomTagSpecs() {
		structCustomTagSpecs.clear();
		structCustomTagSpecs.addAll(getDefaultStructCustomTagSpecs());
		
		sendEvent(new StructTagSpecsChangedEvent(this, structCustomTagSpecs));
	}
	
	public static List<StructCustomTagSpec> getDefaultStructCustomTagSpecs() {
		List<StructCustomTagSpec> specs = new ArrayList<>();
		
		int i=0;
		for (TextTypeSimpleType tt : TextTypeSimpleType.values()) {
			
			String colorCode = TaggingWidgetUtils.getColorCodeForIndex(i++);
			RGB rgb = Colors.toRGB(colorCode);
			if (rgb.equals(StructCustomTagSpec.DEFAULT_COLOR)) { // get next color if this was the default color!
				colorCode = TaggingWidgetUtils.getColorCodeForIndex(i++);
			}
			
			StructCustomTagSpec spec = new StructCustomTagSpec(new StructureTag(tt.value()), colorCode);
			specs.add(spec);
		}
		
		return specs;
	}
	
	public boolean hasStructCustomTagSpec(String type) {
		return getStructCustomTagSpec(type) != null;
	}
	
	public StructCustomTagSpec getStructCustomTagSpec(String type) {
//		for (StructCustomTagSpec spec : structCustomTagSpecs){
//			logger.debug("spec " + spec.getCustomTag().getType());
//		}
		return structCustomTagSpecs.stream().filter(c1 -> c1.getCustomTag().getType().equals(type)).findFirst().orElse(null);
	}
	
	public Color getStructureTypeColor(String type) {
		StructCustomTagSpec c = getStructCustomTagSpec(type);
		if (c!=null && c.getRGB()!=null) {
			return Colors.createColor(c.getRGB());
		}
		else {
			return Colors.createColor(StructCustomTagSpec.DEFAULT_COLOR);
		}
	}
	// END OF CUSTOM TAG SPECS STUFF
	
	// virtual keys shortcuts:
	public XMLPropertiesConfiguration readVirtualKeyboardConf() throws ConfigurationException {
		logger.debug("loading virtual keyboards from file: "+VK_XML.getAbsolutePath()+", file exits: "+VK_XML.exists());
		vkConf = new XMLPropertiesConfiguration();
		vkConf.setEncoding("UTF-8");
		vkConf.setFile(VK_XML);
		vkConf.load();
		logger.debug("actual file: "+vkConf.getFile().getAbsolutePath());
		vkConf.setAutoSave(true);
		
		// parse and set shortcuts:
		List<Pair<String, Pair<Integer, String>>> scs = loadVirtalKeyboardsShortCuts(vkConf);
		clearVirtualKeyShortCuts();
		for (Pair<String, Pair<Integer, String>> sc : scs) {
			setVirtualKeyShortCut(sc.getKey(), sc.getValue());
		}
		return vkConf;
	}
	
	private static List<Pair<String, Pair<Integer, String>>> loadVirtalKeyboardsShortCuts(XMLPropertiesConfiguration conf) {
		List<Pair<String, Pair<Integer, String>>> scs = new ArrayList<>();
		Iterator<String> it=conf.getKeys();
		while (it.hasNext()) {
			String key = it.next();
			logger.debug("key: "+key);
			if (key.startsWith(VK_SHORTCUT_PROP_PREFIX) && key.length()>VK_SHORTCUT_PROP_PREFIX.length()) {
				String scKey = key.substring(VK_SHORTCUT_PROP_PREFIX.length());				
				String value = conf.getString(key);
				
				logger.debug("found shortcut - key = "+scKey+" value = "+value);

				Pair<String, Pair<Integer, String>> sc;
				try {
					sc = Pair.of(scKey, UnicodeList.parseUnicodeString(value));
					scs.add(sc);
				} catch (IOException e) {
					logger.error("Could not parse shortcut "+scKey+": "+e.getMessage());
				}
			}
		}
		
		return scs;
	}	
	
	public XMLPropertiesConfiguration getVirtualKeyboardConf() {
		return vkConf;
	}
	
	public Pair<Integer, String> getVirtualKeyShortCutValue(String key) {
		return virtualKeysShortCuts.get(key);
	}
	
	public String getVirtualKeyShortCutKey(Pair<Integer, String> vk) {
		for (String key : virtualKeysShortCuts.keySet()) {
			if (virtualKeysShortCuts.get(key).equals(vk)) {
				return key;
			}
		}
		return null;
	}
	
	public Pair<Integer, String> removeVirtualKeyShortCut(String key) {
		return virtualKeysShortCuts.remove(key);
	}
	
	public Pair<Integer, String> setVirtualKeyShortCut(String newKey, Pair<Integer, String> vk) {
		Iterator<String> it = virtualKeysShortCuts.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			Pair<Integer, String> value = getVirtualKeyShortCutValue(key);
			if (value.equals(vk)) {
				logger.debug("removing old shorcut, key = "+key+", value = "+value);
				virtualKeysShortCuts.remove(key);
			}
		}
		
		return virtualKeysShortCuts.put(newKey, vk);
	}
	
	public boolean isValidVirtualKeyShortCutKey(String key) {
		return key.equals("0") || key.equals("1") || key.equals("2") || key.equals("3") || key.equals("4") || key.equals("5") || 
				key.equals("6") || key.equals("7") || key.equals("8") || key.equals("9");
	}
	
	public void clearVirtualKeyShortCuts() {
		virtualKeysShortCuts.clear();
	}
	
	public Map<String, Pair<Integer, String>> getVirtualKeysShortCuts() {
		return virtualKeysShortCuts;
	}
	// END OF CUSTOM TAG SPECS STUFF
	
	// START OF COLLECTION-SPECIFIC TAG STUFF
	
	public List<CustomTagSpec> getCustomTagSpecsForCurrentCollection(){
		
		//TODO: "load custom tag defs from DB for loaded collection"
		logger.debug("load custom tag defs from DB for this collection");
		return collectionSpecificTagSpecs;
		
//		//List<CustomTagSpec> tagSpecs = 
//		try {
//			return CustomTagSpecUtil.readCustomTagSpecsFromJsonString(conn.getTagDefsCollection(collId));
//		} catch (SessionExpiredException | ServerErrorException | ClientErrorException | IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;

//		currList.add(getCustomTagSpecs().get(0));
//		
//		CustomTag tag;
//		try {
//			tag = CustomTagFactory.create("person");
//			CustomTagSpec tagDef = new CustomTagSpec(tag);
//			
//			currList.add(tagDef);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return currList;
		
//		if (!storage.isLoggedIn()) {
//			logger.debug("updating custom tag defs for local mode, customTagDefs: "+customTagDefs);
//			CustomTagDefUtil.writeCustomTagDefsToSettings(customTagDefs);
//		} else {
//			// TODO: write to server for current collection if logged in!
//		}
	}
	
	public void updateCustomTagSpecsForCurrentCollectionInDB(){
		
		List<CustomTagSpec> collectionTagSpecs = new ArrayList<CustomTagSpec>();

		//List<CustomTag> cts = CustomTagFactory.getCustomTagListFromProperties(tagNamesProp);
		Collection<CustomTag> cts = CustomTagFactory.getRegisteredCollectionTags();
		for (CustomTag ct : cts){
			String color = CustomTagFactory.getCollectionTagColor(ct.getTagName());
			String label = CustomTagFactory.getCollectionTagLabel(ct.getTagName());
			String extra = CustomTagFactory.getCollectionTagExtra(ct.getTagName());
			Integer icon = CustomTagFactory.getCollectionTagIcon(ct.getTagName());
			CustomTagSpec spec = new CustomTagSpec(ct);
			spec.setColor(color);
			spec.setLabel(label);
			spec.setExtras(extra);
			spec.setIcon(icon);
			collectionTagSpecs.add(spec);
		}
		
		String collectionTags;
		try {
			collectionTags = storage.getConnection().getTagDefsCollection(storage.getCollId());
			JsonArray oldtagSpecString = CustomTagSpecDBUtil.getCollectionTagSpecsAsJsonString(collectionTagSpecs);
			
			logger.debug("old collection defined tags " + oldtagSpecString.toString());
			
			String newTagDefString = CustomTagFactory.createCollectionDBTagsString(collectionTags, oldtagSpecString);

			logger.debug("collection defined tags - newTagDefString " + newTagDefString);

			checkConnection(true);
			//String s = (String) tagSpecString.toString();
			conn.updateTagDefsCollection(collId, newTagDefString);
			
		} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		if (!storage.isLoggedIn()) {
//			logger.debug("updating custom tag defs for local mode, customTagDefs: "+customTagDefs);
//			CustomTagDefUtil.writeCustomTagDefsToSettings(customTagDefs);
//		} else {
//			// TODO: write to server for current collection if logged in!
//		}
	}
	
	public void updateCustomTagSpecsForUserInDB(){
		
		/*
		 * first of all get the object registry (contains all custom tags )
		 * get tag definitions which are not predefined
		 * get them with tagname from object registry
		 * add them to a customtagspec list and import in DB
		 */
		// init predifined tags:
		//String tagNamesProp = TrpConfig.getTrpSettings().getTagNames();
		List<CustomTagSpec> userTagSpecs = new ArrayList<CustomTagSpec>();

		//List<CustomTag> cts = CustomTagFactory.getCustomTagListFromProperties(tagNamesProp);
		Collection<CustomTag> cts = CustomTagFactory.getRegisteredCustomTags();
		for (CustomTag ct : cts){
			String color = CustomTagFactory.getTagColor(ct.getTagName());
			CustomTagSpec spec = new CustomTagSpec(ct);
			spec.setColor(color);
			userTagSpecs.add(spec);
		}

		JsonArray tagSpecString = CustomTagSpecDBUtil.getCollectionTagSpecsAsJsonString(userTagSpecs);
		logger.debug("user defined tags " + tagSpecString.toString());
		try {
			checkConnection(true);
			String s = (String) tagSpecString.toString();
			conn.updateTagDefsUser(s);
		} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	// END OF COLLECTION-SPECIFIC TAG STUFF
	public void saveTagDefinitions() {
		String tagNamesProp = CustomTagFactory.createTagDefPropertyForConfigFile();
		logger.debug("storing tag defs, tagNamesProp: "+tagNamesProp);
		TrpConfig.getTrpSettings().setTagNames(tagNamesProp);
	}

	public void setCurrentTranscriptEdited(boolean edited) {
		if (transcript==null || transcript.getPage()==null) {
			return;
		}
		
		transcript.getPage().setEdited(edited);
	}

	public JobImpl[] getHtrTrainingJobImpls() throws SessionExpiredException, ServerErrorException, ClientErrorException {
		final Predicate<JobImpl> htrTrainingJobImplFilter = j -> j.getTask().equals(JobTask.HtrTraining);
		List<JobImpl> list = getConnection().getJobImplAcl(htrTrainingJobImplFilter);
		return list.toArray(new JobImpl[list.size()]);
	}

	/**
	 * @deprecated with the TrpHtr.gtAccessible field
	 */
	public boolean isGtDataAccessible(TrpHtr htr) {
		if(isAdminLoggedIn()) {
			return true;
		}
		//is model unpublished and linked to this collection?
		if(ReleaseLevel.None.equals(htr.getReleaseLevel()) 
				&& htr.getCollectionIdLink() == getCollId()) {
			return true;
		}
		//is model published including the data sets?
		if(!ReleaseLevel.isPrivateDataSet(htr.getReleaseLevel())) {
			return true;
		}
		return false;
	}
	
	public DocumentSelectionDescriptor getDocumentSelectionDescriptor(int colId, DocSelection docSel) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException, StorageException {
		checkLoggedIn();
		
		DocumentSelectionDescriptor dsd = new DocumentSelectionDescriptor(docSel.getDocId());
		if (!StringUtils.isEmpty(docSel.getPages())) {
			List<Integer> pids = conn.getPageIdsByPagesStr(colId, docSel.getDocId(), docSel.getPages());
			dsd.addPages(pids);					
		}
		else {
			logger.debug("pagesStr is empty for DocSelection -> leaving pages empty s.t. all pages get processed!");
		}
		return dsd;
	}

	public List<TrpCreditCosts> getCreditCosts(Date time, boolean forceRefresh) {
		try {
			if(time != null) {
				//return time-specific costs, do not cache
				return conn.getCreditCalls().getCreditCosts(time);
			}
			if(creditCostsList == null || forceRefresh) {
				creditCostsList = conn.getCreditCalls().getCreditCosts(null);
			}
		} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
			logger.error("Could not load cost model from server.", e);
			creditCostsList = new ArrayList<>(0);
		}
		logger.debug("Returning credit costs list with {} entries", creditCostsList.size());
		return creditCostsList;
	}

//	public void updateDocMdAttributes(GenericEntity<List<TrpEntityAttribute>> genericList) {
//		try {
//			logger.debug("collId: " + collId );
//			logger.debug("doc ID : " + doc.getId() );
//			//logger.debug("atts size: " + attributesList. );
//			//conn.updateDocMdAttributes(collId, doc.getId(), genericList);
//		} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
	/*
	 * for the doc md attributes
	 */
	public void updateDocMdAttributes(List<TrpEntityAttribute> genericList) {
		try {
			logger.debug("collId: " + collId );
			logger.debug("doc ID : " + doc.getId() );
			logger.debug("atts size: " + genericList.size() );
			TrpDoc doc = getDoc();
			doc.getMd().setAttributes(genericList);
			conn.updateDocMd(collId, doc.getId(), doc.getMd());
		} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
//	public void reloadP2PaLAModels() {
//		reloadP2PaLAModels(true, isAdminLoggedIn(), null, null, null);
//	}
//	
//	public void reloadP2PaLAModels(boolean onlyActive, boolean allModels, Integer colId, Integer userId, Integer releaseLevel) {
//		if (!isLoggedIn()) {
//			return;
//		}
//		
//		try {
//			List<TrpP2PaLA> models = conn.getP2PaLAModels(onlyActive, isAdminLoggedIn(), colId, userId, releaseLevel);
//			if (CoreUtils.size(models)>0) {
//				p2palaModels = models;
//			}
//		} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
//			logger.error("Error loading P2PaLA models: "+e.getMessage(), e);
//		}
//			
//	}
//	
//	public void clearP2PaLAModels() {
//		p2palaModels = new ArrayList<>();
//	}
//	
//	public List<TrpP2PaLA> getP2PaLAModels() {
//		return p2palaModels;
//	}	
}
