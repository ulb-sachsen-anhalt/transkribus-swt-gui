package eu.transkribus.swt_gui.doc_overview;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ServerErrorException;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.pagination.collections.PageResultLoaderList;
import org.eclipse.nebula.widgets.pagination.table.SortTableColumnSelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethods;
import eu.transkribus.swt.pagination_table.RemotePageLoader;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.TableViewerUtils;
import eu.transkribus.swt.util.TrpViewerFilter;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class DocTableWidgetPagination extends ATableWidgetPagination<TrpDocMetadata> {
	private final static Logger logger = LoggerFactory.getLogger(DocTableWidgetPagination.class);
	
	public static final String DOC_NR_COL = "NR";
	public static final String DOC_ID_COL = "ID";
	public static final String DOCS_TITLE_COL = "Title";
	public static final String DOC_NPAGES_COL = "Pages";
	public static final String DOC_UPLOADER_COL = "Uploader";
	public static final String DOC_UPLOADED_COL = "Uploaded";
	public static final String DOC_COLLECTIONS_COL = "Collections";
	public static final String DOC_DEL_TIME = "Deleted on";
	
	public static final boolean LOAD_ALL_DOCS_ONCE = true;
	protected int collectionId=Integer.MIN_VALUE;
	
	List<TrpDocMetadata> docs = new ArrayList<>();
	PageResultLoaderList<TrpDocMetadata> listLoader;
	public static final boolean USE_LIST_LOADER = true;
	
	ViewerFilter viewerFilter;
	static String[] filterProperties = { "docId", "title", "uploader" }; // those are the properties of the TrpDocMetadata bean that are used for filtering
		
	public DocTableWidgetPagination(Composite parent, int style, int initialPageSize) {		
		this(parent, style, initialPageSize, false, null);
	}
	
	public DocTableWidgetPagination(Composite parent, int style, int initialPageSize, boolean isRecycleBin) {		
		this(parent, style, initialPageSize, isRecycleBin, null);
	}	
	
	public DocTableWidgetPagination(Composite parent, int style, int initialPageSize, boolean isRecycleBin, IPageLoadMethods<TrpDocMetadata> methods) {
		super(parent, style, initialPageSize, methods, true, isRecycleBin);
				
		viewerFilter = new TrpViewerFilter(filter, filterProperties) {

			@Override
			protected void updateView() {
				reloadDocs(true, false); 
			}
		};
	
//		pageableTable.getViewer().addFilter(viewerFilter); // does not work with pagination -> using viewerFilter explicitly when setting input to listLoader	
	}
	
	public String getFilterText() {
		if (SWTUtil.isDisposed(filter))
			return "";
		else
			return filter.getText();
	}
	
	private void setCollectionId(int collectionId) {
//		if (collectionId != this.collectionId)
//			reloadDocs=true;
			
		this.collectionId = collectionId;
	}
	
	@Override protected void onReloadButtonPressed() {
		refreshList(this.collectionId, false, true);
	}
	
	//empty the filter text
	public void clearFilter() {
		filter.setText("");
	}
		
//	public void refreshList(boolean resetPage) {
//		this.refreshList(this.collectionId, resetPage, false);
//	}
	
	public void refreshList(int collectionId, boolean resetPage, boolean forceServerReload) {
		logger.debug("old coll-id: "+this.collectionId+" new coll-id: "+collectionId);
		setCollectionId(collectionId);
		/*
		 * reloadDocs() will check if the collectionId set here matches the one in Storage 
		 * and force a reload if necessary or if forceServerReload argument is true.
		 */
		logger.debug("refreshing doc table, collectionId="+collectionId+" resetPage="+resetPage+" forceServerReload="+forceServerReload);
		reloadDocs(resetPage, forceServerReload);
	}
	
	/*
	 * for recycle bin - collectionId is the current one
	 */
	public void refreshList(int collectionId) {
		refreshList(collectionId, true, false);
	}
	
	private void setDocList(List<TrpDocMetadata> newDocs, boolean resetPage) {
		synchronized (this) {
			logger.debug("setDocList, N = "+newDocs.size());
			
			Display.getDefault().asyncExec(() -> {
				this.docs = new ArrayList<>();
				// filter
				for (TrpDocMetadata d : newDocs) {
					if (viewerFilter.select(null, null, d)) {
						this.docs.add(d);
					}
				}
				
				listLoader.setItems(docs);
				refreshPage(resetPage);	
			});
		}
	}
	
	public void reloadDocs(boolean resetPage, boolean forceReload) {
		//case if user is logged out
		if (collectionId == 0) {
			logger.debug("collectionId=0");
			setDocList(new ArrayList<>(), resetPage);
			return;
		}
		
		Storage store = Storage.getInstance();
		//use collectionId == -1 for the stray documents widget
		if (collectionId == -1) {
			logger.debug("collectionId=-1");
			setDocList(store.getUserDocList(), resetPage);
			return;
		}

		
		if (forceReload || collectionId != store.getCollId()) { // have to reload doclist
//			store.getConnection().getAllDocsAsync(collectionId, 0, 0, null, null, new InvocationCallback<List<TrpDocMetadata>>() {
			logger.debug("collection id differs from storage - reloading from server! this="+collectionId+" != storage="+store.getCollId());
			TrpMainWidget.getInstance().reloadDocList(collectionId);
		} else {
			logger.debug("setting docs from storage: "+ (isRecycleBin ? store.getDeletedDocList().size() : store.getDocList().size()));
			//List<TrpDocMetadata> docList = 
			setDocList(isRecycleBin ? store.getDeletedDocList() : store.getDocList(), resetPage);
		}
	}
	
	protected void createColumns() {
		// generic label provider constructed with the bean property used for this column
		class DocTableColumnLabelProvider extends ColumnLabelProvider {
			String colName;
			Font boldFont = Fonts.createBoldFont(tv.getControl().getFont());
			
			public DocTableColumnLabelProvider(String colName) {
				this.colName = colName;
			}
			
			@Override
			public String getToolTipText(Object element) {
				
				TrpDocMetadata docMd = (TrpDocMetadata) element;
				return "ID=" + docMd.getDocId() + " / Title=" + docMd.getTitle() + " / N-Pages=" + docMd.getNrOfPages() + " / Uploader=" + docMd.getUploader() + " / Uploaded=" + docMd.getUploadTime().toString() + " / Collections=" + docMd.getColString();
			}
			
            @Override public String getText(Object element) {
            	try {
					return BeanUtils.getSimpleProperty(element, colName);
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					return "i am error " + e.getMessage();
				}
            }
            
        	@Override public Font getFont(Object element) {
        		if (element instanceof TrpDocMetadata) {
        			TrpDocMetadata md = (TrpDocMetadata) element;
        			if (md.getDocId() == Storage.getInstance().getDocId())
        				return boldFont;
        		}
        		
        		return null;
        	}
		}
		
		TableViewerColumn col = TableViewerUtils.createTableViewerColumn(tv, 0, DOC_ID_COL, 50);
		col.setLabelProvider(new DocTableColumnLabelProvider("docId"));
		col.getColumn().addSelectionListener(new SortTableColumnSelectionListener("docId"));
		
//		tv.getTable().setSortColumn(col.getColumn());
//		tv.getTable().setSortDirection(SWT.DOWN);
		
		col = TableViewerUtils.createTableViewerColumn(tv, 0, DOCS_TITLE_COL, 225);
		col.setLabelProvider(new DocTableColumnLabelProvider("title"));
		col.getColumn().addSelectionListener(new SortTableColumnSelectionListener("title"));
		
		col = TableViewerUtils.createTableViewerColumn(tv, 0, DOC_NPAGES_COL, 50);
		col.setLabelProvider(new DocTableColumnLabelProvider("nrOfPages"));
		col.getColumn().addSelectionListener(new SortTableColumnSelectionListener("nrOfPages"));

		col = TableViewerUtils.createTableViewerColumn(tv, 0, DOC_UPLOADER_COL, 100);
		col.setLabelProvider(new DocTableColumnLabelProvider("uploader"));
		col.getColumn().addSelectionListener(new SortTableColumnSelectionListener("uploader"));
		
		col = TableViewerUtils.createTableViewerColumn(tv, 0, DOC_UPLOADED_COL, 100);
		col.setLabelProvider(new DocTableColumnLabelProvider("uploadTime"));
		col.getColumn().addSelectionListener(new SortTableColumnSelectionListener("uploadTimestamp"));		
		
		col = TableViewerUtils.createTableViewerColumn(tv, 0, DOC_COLLECTIONS_COL, 100);	
		col.setLabelProvider(new DocTableColumnLabelProvider("colString"));
//		col.getColumn().addSelectionListener(new SortTableColumnSelectionListener("colString"));

		if(isRecycleBin){
			col = TableViewerUtils.createTableViewerColumn(tv, 0, DOC_DEL_TIME, 100);	
			col.setLabelProvider(new DocTableColumnLabelProvider("deletedOnDate"));
		}
		
		

	}

	@Override protected void setPageLoader() {
		if (USE_LIST_LOADER && methods==null) {
			listLoader = new PageResultLoaderList<TrpDocMetadata>(docs);
			pageableTable.setPageLoader(listLoader);
		} else {
			if (methods == null) { // if not set from outside -> set default one that loads docs from current collection!
				methods = new IPageLoadMethods<TrpDocMetadata>() {
					Storage store = Storage.getInstance();
		
					@Override public int loadTotalSize() {
						
						if (!store.isLoggedIn() || collectionId <= 0)
							return 0;
						
						int totalSize = 0;
						try {
							totalSize = store.getConnection().countDocs(collectionId);
						} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
							TrpMainWidget.getInstance().onError("Error loading documents", e.getMessage(), e);
						}
						return totalSize;
					}
		
					@Override public List<TrpDocMetadata> loadPage(int fromIndex, int toIndex, String sortPropertyName, String sortDirection) {
						if (!store.isLoggedIn() || collectionId <= 0)
							return new ArrayList<>();
						
						List<TrpDocMetadata> docs = new ArrayList<>();
						try {
							logger.debug("loading docs, sortDirection = "+sortDirection+" collectionId = "+collectionId+" fromIndex = "+fromIndex+" toIndex = "+toIndex);
							if (!isRecycleBin){
								docs = store.getConnection().getAllDocs(collectionId, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection, false);
							}
							else{
								docs = store.getConnection().getAllDocs(collectionId, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection, true);
							}
						} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
							TrpMainWidget.getInstance().onError("Error loading documents", e.getMessage(), e);
						}
						return docs;
					}
				};
			}
			
			RemotePageLoader<TrpDocMetadata> pl = new RemotePageLoader<>(pageableTable.getController(), methods);
			pageableTable.setPageLoader(pl);
		}
	} // end setPageLoader()



}
