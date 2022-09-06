package eu.transkribus.swt_gui.pagination_tables;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.pagination.IPageLoader;
import org.eclipse.nebula.widgets.pagination.collections.PageResult;
import org.eclipse.nebula.widgets.pagination.collections.PageResultLoaderList;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.util.SebisStopWatch.SSW;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethods;
import eu.transkribus.swt.pagination_table.RemotePageLoader;
import eu.transkribus.swt.pagination_table.TableColumnBeanLabelProvider;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.DelayedTask;

public class CollectionsTableWidgetPagination extends ATableWidgetPagination<TrpCollection> {
	private final static Logger logger = LoggerFactory.getLogger(CollectionsTableWidgetPagination.class);
	
	static String ID_COL = "ID";
	static String NAME_COL = "Name";
	static String ROLE_COL = "Role";
	static String DESC_COL = "Description";
	static String LABEL_COL = "Label";
	
	static final boolean USE_LIST_LOADER = true;
	
	List<TrpCollection> collections = new ArrayList<>();
	PageResultLoaderList<TrpCollection> listLoader;
	
	ViewerFilter viewerFilter;
	protected ModifyListener filterModifyListener;
	static String[] filterProperties = { "colId", "colName" };
	
	Predicate<TrpCollection> collectionPredicate;
		
	public CollectionsTableWidgetPagination(Composite parent, int style, int initialPageSize, Predicate<TrpCollection> collectionPredicate, IPageLoadMethods<TrpCollection> methods, TrpCollection initColl) {
		super(parent, style, initialPageSize, methods, true);
		this.collectionPredicate = collectionPredicate;
		
		initFilter();
		initListener();
		
		logger.debug("initColl = "+initColl);
		if (initColl != null) {
			loadPage("colId", initColl.getColId(), false);
		}
	}
	
//	public CollectionsTableWidgetPagination(Composite parent, int style, int initialPageSize) {
//		super(parent, style, initialPageSize);
//		initFilter();
//		initListener();
//	}
	
	void initListener() {
		if (USE_LIST_LOADER) {
			Storage.getInstance().addListener(new IStorageListener() {
				@Override public void handleCollectionsLoadEvent(CollectionsLoadEvent cle) {
					if (SWTUtil.isDisposed(CollectionsTableWidgetPagination.this) || SWTUtil.isDisposed(getShell()))
							return;
					
					setCollections(Storage.getInstance().getCollections());
				}
			});
			
			setCollections(Storage.getInstance().getCollections());
		}
	}
	
	void initFilter() {
		viewerFilter = new ViewerFilter() {
			@Override public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (SWTUtil.isDisposed(filter)) {
					return true;
				}
				
				logger.trace("filter, select: "+element);

				String ft = filter.getText();
				logger.trace("ft = "+ft);
				if (StringUtils.isEmpty(ft))
					return true;
				
				ft = Pattern.quote(ft);
				
				String reg = "(?i)(.*"+ft+".*)";
				logger.trace("reg = "+reg);
				
//				TrpDocMetadata d = (TrpDocMetadata) element;
				
				for (String property : filterProperties) {
					try {
						String propValue = BeanUtils.getSimpleProperty(element, property);
						logger.trace("property: "+property+" value: "+propValue);
						
						if (propValue.matches(reg)) {
							return true;
						}
					} catch (Exception e) {
						logger.error("Error getting filter property '"+property+"': "+e.getMessage());
					}
				}

				return false;
				
//				boolean matches = element.toString().matches(reg);
//				logger.debug("matches = "+matches);
//				return matches;
			}
		};
		
		filterModifyListener = new ModifyListener() {
			DelayedTask dt = new DelayedTask(() -> { 
				refreshList();
				//this way the first collection found by the filter is preselected
				if (!tv.getTable().isDisposed()){
					tv.getTable().select(0);
				}
			}, true);
			@Override public void modifyText(ModifyEvent e) {
				dt.start();
			}
		};
		filter.addModifyListener(filterModifyListener);
//		pageableTable.getViewer().addFilter(viewerFilter); // does not work with pagination -> using viewerFilter explicitly when setting input to listLoader

		
	}
	
	private boolean matchesCollectionPredicate(TrpCollection c) {		
		return collectionPredicate==null || collectionPredicate.test(c);
	}
	
	/**
	 * Sets list of available collections and filters them using the given collectionPredicate (if set)
	 * Not that this call is only necessary when using a list-loader, i.e. when USE_LIST_LOADER=true
	 */
	public void setCollections(List<TrpCollection> collections) {
		if (collectionPredicate != null) {
			this.collections = collections.stream().filter(collectionPredicate).collect(Collectors.toList());
		}
		else {
			this.collections = collections;
		}
		refreshList();
	}
	
	// refreshes the list - if a filter is set, filters the elements according to that
	public synchronized void refreshList() {
		if (!StringUtils.isEmpty(filter.getText())) {
			logger.debug("filtering collections... N = "+collections.size());
			SSW.SW.start();	
			
			if (Storage.getInstance().isLoggedIn()) {
				// Note: this is kind of a hack - instead of filtering collections client side, use the the endpoint including a filter
				// FIXME we have to integrate the collectionPredicate into the endpoint to be able to use it directly in a RemotePageLoaderSingleRequest
				try {
					int nMax = (int) 1e9;
					List<TrpCollection> filtered = Storage.getInstance().getConnection().getCollections(filter.getText(), 0, nMax, null, null).getList();
					if (collectionPredicate!=null) {
						List<TrpCollection> filtered2 = filtered.stream().filter(collectionPredicate).collect(Collectors.toList());
						filtered.clear(); filtered.addAll(filtered2);
					}
					
					Display.getDefault().syncExec(() -> {
						if (USE_LIST_LOADER && listLoader!=null) {
							listLoader.setItems(filtered==null ? new ArrayList<>() : filtered);
						}
						
						refreshPage(true);
					});				
				} catch (SessionExpiredException | ServerErrorException | ClientErrorException
						| IllegalArgumentException e) {
					logger.error(e.getMessage(), e);
				}
			}
			
//			List<TrpCollection> filtered = collections.stream().filter(c -> {
//				return matchesCollectionPredicate(c) && viewerFilter.select(null, null, c);
//			}).collect(Collectors.toList());
//		
//		//		for (TrpCollection c : collections) {
//		//			if (matchesCollectionPredicate(c) && viewerFilter.select(null, null, c)) { 
//		//				filtered.add(c);
//		//			}
//		//		}
//			SSW.SW.stop(true, "filtering time: ", logger);
//			logger.debug("filtered collections, N = "+collections.size());
//			
//			Display.getDefault().syncExec(() -> {
//				if (USE_LIST_LOADER && listLoader!=null) {
//					listLoader.setItems(filtered);
//				}
//				
//				refreshPage(true);
//			});			
		} else {
			Display.getDefault().syncExec(() -> {
				if (USE_LIST_LOADER && listLoader!=null) {
					listLoader.setItems(collections);
				}
				
				refreshPage(true);
			});	
		}

	}
	
	protected void onReloadButtonPressed() {
		if (USE_LIST_LOADER) {
			try {
				logger.debug("re-loading collections...");
				Storage.getInstance().reloadCollections();
			} catch (Exception e) {
				logger.error("Error loading collections: "+e.getMessage(), e);
			}
		} else {
			super.onReloadButtonPressed();	
		}
	}

	@Override protected void setPageLoader() {
		IPageLoader<PageResult<TrpCollection>> loader;
		
		if (USE_LIST_LOADER) {
			List<TrpCollection> collections = Storage.getInstance().getCollections();
			listLoader = new PageResultLoaderList<TrpCollection>(collections);
			loader = listLoader;
		} else {
			if (methods == null) {
				methods = new IPageLoadMethods<TrpCollection>() {
					Storage store = Storage.getInstance();
					
					@Override public int loadTotalSize() {
						List<TrpCollection> collections = Storage.getInstance().getCollections();
						return collections.size();
						
	//					int N = 0;
	//					if (store.isLoggedIn()) {
	//						try {
	//							return store.getConnection().countAllCollections();
	//						} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
	//							TrpMainWidget.getInstance().onError("Error loading documents", e.getMessage(), e);
	//						}
	//					}
	//					return N;
					}
		
					@Override public List<TrpCollection> loadPage(int fromIndex, int toIndex, String sortPropertyName, String sortDirection) {
						List<TrpCollection> collections = Storage.getInstance().getCollections();
						logger.trace("colls loading page, fromIndex = "+fromIndex+", toIndex = "+toIndex);
						return collections.subList(fromIndex, toIndex);
						
	//					List<TrpCollection> list = new ArrayList<>();
	//					if (store.isLoggedIn()) {
	//						try {
	//							list = store.getConnection().getAllCollections(fromIndex, toIndex-fromIndex);
	//						} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
	//							TrpMainWidget.getInstance().onError("Error loading documents", e.getMessage(), e);
	//						}
	//					}
	//					return list;
					}
				};
			}
			
			RemotePageLoader<TrpCollection> pl = new RemotePageLoader<>(pageableTable.getController(), methods);
			loader = pl;
		}

		pageableTable.setPageLoader(loader);
	}

	@Override protected void createColumns() {
		class CollectionsTableColumnLabelProvider extends TableColumnBeanLabelProvider {
			Font boldFont = Fonts.createBoldFont(tv.getControl().getFont());
			
			public CollectionsTableColumnLabelProvider(String colName) {
				super(colName);
			}
            
        	@Override public Font getFont(Object element) {
        		if (element instanceof TrpCollection) {
        			TrpCollection c = (TrpCollection) element;
        			
        			if (c.getColId() == TrpMainWidget.getInstance().getUi().getServerWidget().getSelectedCollectionId())
        				return boldFont;
        		}
        		
        		return null;
        	}
        	
//        	@Override public String getText(Object element) {
//        		if (!colName.equals(ROLE_COL))
//        			super.getText(element);
//        		
//        		if (element instanceof TrpCollection) {
//        			TrpCollection c = (TrpCollection) element;
//        			
//        			return c.getRole() == null ? "" : c.getRole().toString();
//        			
//        		}
//        		return null;
//        		
//        	}
		}
		
		createColumn(ID_COL, 50, "colId", new CollectionsTableColumnLabelProvider("colId"));
		createColumn(NAME_COL, 250, "colName", new CollectionsTableColumnLabelProvider("colName"));
		createColumn(ROLE_COL, 80, "colName", new CollectionsTableColumnLabelProvider("role"));
		createColumn(DESC_COL, 500, "description", new CollectionsTableColumnLabelProvider("description"));
//		createColumn(LABEL_COL, 100, "label", new CollectionsTableColumnLabelProvider("label"));
	}

}
