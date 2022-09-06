package eu.transkribus.swt_gui.pagination_tables;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethods;
import eu.transkribus.swt.pagination_table.TableColumnBeanLabelProvider;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.DelayedTask;

public class CollectionsOfUserTableWidgetPagination extends ATableWidgetPagination<TrpCollection> {
	private final static Logger logger = LoggerFactory.getLogger(CollectionsOfUserTableWidgetPagination.class);
	
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
	
	int userId;
	
	Predicate<TrpCollection> collectionPredicate;
		
	public CollectionsOfUserTableWidgetPagination(Composite parent, int style, int initialPageSize, Predicate<TrpCollection> collectionPredicate, IPageLoadMethods<TrpCollection> methods, int userId) {
		super(parent, style, initialPageSize, methods, true);
		this.collectionPredicate = collectionPredicate;
		this.userId = userId;
		
		initFilter();
		initListener();

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
					if (SWTUtil.isDisposed(CollectionsOfUserTableWidgetPagination.this) || SWTUtil.isDisposed(getShell()))
							return;
					
					try {
						refreshList(Storage.getInstance().getConnection().getAllCollectionsForSingleUser(0, 0, null, null, userId));
					} catch (SessionExpiredException | ServerErrorException | ClientErrorException
							| IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			
			try {
				refreshList(Storage.getInstance().getConnection().getAllCollectionsForSingleUser(0, 0, null, null, userId));
			} catch (SessionExpiredException | ServerErrorException | ClientErrorException
					| IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
				refreshList(collections);
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
	
	public synchronized void refreshList(List<TrpCollection> collections) {
		this.collections = new ArrayList<>();
		this.collections.addAll(collections);
		
		List<TrpCollection> filtered = new ArrayList<>();
		for (TrpCollection c : collections) {
			if (matchesCollectionPredicate(c) && viewerFilter.select(null, null, c)) { 
				filtered.add(c);
			}
		}
		
		Display.getDefault().syncExec(() -> {
			if (USE_LIST_LOADER && listLoader!=null) {
				listLoader.setItems(filtered);
			}
			
			refreshPage(true);
		});
	}
	
	protected void onReloadButtonPressed() {
		if (USE_LIST_LOADER) {
			try {
				logger.debug("re-loading collections...");
				//Storage.getInstance().reloadCollections();
			} catch (Exception e) {
				logger.error("Error loading collections: "+e.getMessage(), e);
			}
		} else {
			super.onReloadButtonPressed();	
		}
	}

	@Override protected void setPageLoader() {
		IPageLoader<PageResult<TrpCollection>> loader;
		
		//List<TrpCollection> collections = Storage.getInstance().getCollections();
		listLoader = new PageResultLoaderList<TrpCollection>(collections);
		loader = listLoader;

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
