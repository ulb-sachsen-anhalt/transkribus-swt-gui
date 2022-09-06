package eu.transkribus.swt.pagination_table;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.nebula.widgets.pagination.IPageLoaderHandler;
import org.eclipse.nebula.widgets.pagination.MyPageSizeComboRenderer;
import org.eclipse.nebula.widgets.pagination.PageableController;
import org.eclipse.nebula.widgets.pagination.collections.PageResult;
import org.eclipse.nebula.widgets.pagination.collections.PageResultContentProvider;
import org.eclipse.nebula.widgets.pagination.table.PageableTable;
import org.eclipse.nebula.widgets.pagination.table.SortTableColumnSelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.TableViewerUtils;

public abstract class ATableWidgetPagination<T> extends Composite {
	private final static Logger logger = LoggerFactory.getLogger(ATableWidgetPagination.class);

	static int DEFAULT_INITIAL_PAGE_SIZE = 50;
	
	protected PageableTable pageableTable;
		
	protected TableViewer tv;
	
	protected IPageLoadMethods<T> methods = null;
	
	protected int initialPageSize = DEFAULT_INITIAL_PAGE_SIZE;
	
	protected LoadingComposite loadingComposite;
	
	protected Text filter;
	protected boolean withFilter;
	
	protected boolean isRecycleBin = false;
	
//	T itemToSelect=null;
	
//	public ATableWidgetPagination(Composite parent, int style) {
//		super(parent, style);
//		this.setLayout(new GridLayout(1, false));
//		this.methods = null;
//		
//		createTable();
//	}
	
	public ATableWidgetPagination(Composite parent, int tableStyle, int initialPageSize) {
		this(parent, tableStyle, initialPageSize, null, false, false);
	}

	public ATableWidgetPagination(Composite parent, int tableStyle, int initialPageSize, IPageLoadMethods<T> methods) {
		this(parent, tableStyle, initialPageSize, methods, false, false);
	}
	
	public ATableWidgetPagination(Composite parent, int tableStyle, int initialPageSize, IPageLoadMethods<T> methods, boolean withFilter) {
		this(parent, tableStyle, initialPageSize, methods, withFilter, false);
	}

	public ATableWidgetPagination(Composite parent, int tableStyle, int initialPageSize, IPageLoadMethods<T> methods, boolean withFilter, boolean recycleBin) {
		super(parent, 0);
		this.setLayout(new GridLayout(1, false));
		
		this.isRecycleBin = recycleBin;
		this.withFilter = withFilter;
		this.initialPageSize = initialPageSize;
		this.methods = methods;
		
		createTable(tableStyle);
	}
	
	public Text getFilter() {
		return filter;
	}
	
	public String getSortPropertyName() {
		return pageableTable.getController().getSortPropertyName();
	}
	
	public String getSortDirection() {
		return pageableTable.getController().getSortDirection()==SWT.UP ? "desc" : "asc";
	}
	
	public PageableTable getPageableTable() { return pageableTable; }
	
	private static <T> T findItem(List<T> items, String propertyName, Object value) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (items == null)
			return null;
		
		for (T i : items) {
			Object v = PropertyUtils.getProperty(i, propertyName);
			
			logger.trace("prop value: "+v+" values classes: "+(value.getClass())+" / "+(v.getClass())+" values: "+value+" / "+v);
			if (value.equals(v)) {
				return i;
			}
		}
		return null;
	}
	
	/**
	 * Loads the page that contains the specified property / value pair; if found, the element is selected
	 */
	public synchronized void loadPage(String propertyName, Object value, boolean refreshFirst) {
		if (propertyName == null || value == null) {
			logger.error("propertyName or value is null - doing nothing!");
			return;
		}
		PageableController c = pageableTable.getController();
		if (refreshFirst) {
			logger.debug("refreshing first...");
			pageableTable.refreshPage(true);
		}
		
		logger.debug("loading page, propertyName = "+propertyName+" value = "+value+" currentPage = "+c.getCurrentPage());

		try {			
			// 1st: check if object is present at locally loaded dataset:
			List<T> items = (List<T>) pageableTable.getViewer().getInput();
			List<T> itemsCopy = CoreUtils.copyList(items);
					
			T item = findItem(itemsCopy, propertyName, value);
			if (item != null) {
				logger.debug("found item in current page!");
				selectElement(item);
				return;
			}
			// 2nd: search pages one by one:
			else {
				int currentPage = c.getCurrentPage();
				logger.debug("total elements = "+c.getTotalElements());
				
				PageableController c1 = new PageableController();
				c1.setPageSize(c.getPageSize());
				c1.setTotalElements(c.getTotalElements());
				c1.setSort(c.getSortPropertyName(), c.getSortDirection());
				c1.setCurrentPage(c.getCurrentPage());
				
				for (int i=0; i<c1.getTotalPages(); ++i) {
					if (i == currentPage) // already checked!
						continue;

					c1.setCurrentPage(i);
					PageResult<T> res = (PageResult<T>) pageableTable.getPageLoader().loadPage(c1);
					
//					Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
//					    public void uncaughtException(Thread th, Throwable ex) {
//					        System.out.println("Uncaught exception: " + ex);
//					        ex.printStackTrace();
//					    }
//					};
//					Thread t = new Thread() {
//					    public void run() {
//					        System.out.println("Sleeping ...");
//					        try {
//					            Thread.sleep(1000);
//					        } catch (InterruptedException e) {
//					            System.out.println("Interrupted.");
//					        }
//					        System.out.println("Throwing exception ...");
//					        throw new RuntimeException();
//					    }
//					};
//					t.setUncaughtExceptionHandler(h);
//					t.start();

					
//					PageResult<T> res = PagingUtils.loadPage(methods, c);
					
					c.setCurrentPage(i);
					items = (List<T>) pageableTable.getViewer().getInput();
					itemsCopy = CoreUtils.copyList(items);
					
					//items = res.getContent();
					//

					item = findItem(itemsCopy, propertyName, value);
					
					if (item != null) {
						logger.debug("found item in page "+i);
						logger.debug("item found "+ item);
						
						selectElement(item);
						
						return;
					}
				}
			}
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	
	/**
	 * Loads the page that contains the specified property / value pair; if found, the element is selected
	 * use binary search to make it faster
	 * TODO: this information could come directly from the database
	 * 
	 */
	public synchronized void loadPage_useBinarySearch(String propertyName, Object value, boolean refreshFirst) {
		if (propertyName == null || value == null) {
			logger.error("propertyName or value is null - doing nothing!");
			return;
		}
		PageableController c = pageableTable.getController();
		if (refreshFirst) {
			logger.debug("refreshing first...");
			pageableTable.refreshPage(true);
		}
		
		logger.debug("loading page, propertyName = "+propertyName+" value = "+value+" currentPage = "+c.getCurrentPage());

		try {			
			// 1st: check if object is present at locally loaded dataset:
			List<T> items = (List<T>) pageableTable.getViewer().getInput();
			List<T> itemsCopy = CoreUtils.copyList(items);
					
			T item = findItem(itemsCopy, propertyName, value);
			if (item != null) {
				logger.debug("found item in current page!");
				selectElement(item);
				return;
			}
			// 2nd: search pages one by one:
			else {
				List<T> itemsOfAlreadySearchedPage = itemsCopy;
				int currentPage = c.getCurrentPage();
				logger.debug("total elements = "+c.getTotalElements());
				
				PageableController c1 = new PageableController();
				c1.setPageSize(c.getPageSize());
				c1.setTotalElements(c.getTotalElements());
				c1.setSort(c.getSortPropertyName(), c.getSortDirection());
				c1.setCurrentPage(c.getCurrentPage());
				
				int l = 0;
				int r = c1.getTotalPages()-1;
				if(r==0) {
					return;
				}
				
			    while (l <= r) { 
			        // find index of middle element 
			        int m = (l+r)/2; 
			        
			        // already checked
					if (true && m == currentPage) {
						logger.debug("already checked this page: "+m);
						items = itemsOfAlreadySearchedPage;
//						continue; // never use continue in a while loop -> can easily lead to an endless loop...
					}
					// not checked -> retrieve items for this page from server
					else {
						logger.debug("page to look at: " + m);
						c1.setCurrentPage(m);
						c.setCurrentPage(m);
						items = (List<T>) pageableTable.getViewer().getInput();
					}
					
					itemsCopy = CoreUtils.copyList(items);
					//items = res.getContent();
					//
			  
					Integer firstValueInList = (Integer) PropertyUtils.getProperty(itemsCopy.get(0), propertyName);
					Integer lastValueInList  = (Integer) PropertyUtils.getProperty(itemsCopy.get(itemsCopy.size()-1), propertyName);
					
//					logger.debug("firstValueInList for this page: " + firstValueInList);
//					logger.debug("lastValueInList for this page: " + lastValueInList);
					
					Integer searchValue = (Integer) value;
					
					if (searchValue <= firstValueInList && searchValue >= lastValueInList) {
						item = findItem(itemsCopy, propertyName, value);
						if (item != null) {
							logger.debug("found item in page "+m);
							logger.debug("item found "+ item);
							
							selectElement(item);
							
							return;
						}
					}
					
			        // If x greater, ignore left half 
			        if (firstValueInList > searchValue) {
			        	l = m + 1; 
			        }
			        // If x is smaller, ignore right half 
			        else{
			        	r = m - 1;
			        }
			        
			        logger.debug("new l is " + l);
			        logger.debug("new r is " + r);
			    } 
			    // not found --> select first page
			    c.setCurrentPage(0);
			
			}
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			logger.error(e.getMessage(), e);
		}
		
	}

	void createTable(int style) {
		int tableStyle = SWT.BORDER | SWT.MULTI  | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL;
		
		if ((style & SWT.SINGLE) != 0) {
			tableStyle |= SWT.SINGLE;
		}
				
//		if (singleSelection)
//			tableStyle |= SWT.SINGLE;
		
		pageableTable = new PageableTable(this, SWT.BORDER, tableStyle, initialPageSize
				, PageResultContentProvider.getInstance(),
				PagingToolBarNavigationRendererFactory.getFactory(),
				PageableTable.getDefaultPageRendererBottomFactory()
				) {
			@Override
			public void refreshPage() {
				super.refreshPage();
				tv.getTable().redraw(); // have to redraw table after page refresh -> bug in MacOS
			}
			
//			@Override protected Composite createCompositeTop(Composite parent) {
//				final PageableController c = pageableTable.getController();
//				
//				Composite container = new Composite(parent, 0);
//				container.setLayout(new FillLayout());
//				
//				PagingToolBar pagingToolBar = new PagingToolBar("", true, false, container, SWT.NONE);
//					...
//				
//				container.setLayoutData(new GridData(
//						GridData.FILL_HORIZONTAL));
//				return container;
//			}
			
			@Override
			protected Composite createCompositeBottom(Composite parent) {
//				Composite bottom = new LoadingComposite(parent);
//				bottom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//				return bottom;
				
				Composite bottom = new Composite(parent, 0);
				bottom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				bottom.setLayout(new GridLayout(withFilter ? 3 : 2, false));
				
//				PageSizeComboRenderer pageSizeComboDecorator = new PageSizeComboRenderer(
//						bottom, SWT.NONE, getController(), new Integer[] { 5, 10, 25, 50, 75, 100, 200 }) {
//					
//					public void widgetSelected(SelectionEvent e) {						
//						pageableTable.refreshPage(true); // needed to refresh pagination control -> bug in original code!						
//						super.widgetSelected(e);
//					}
//				};
				MyPageSizeComboRenderer pageSizeComboDecorator = new MyPageSizeComboRenderer(
						bottom, SWT.NONE, getController(), new Integer[] { 5, 10, 25, 50, 75, 100, 200 }) {
					
					public void widgetSelected(SelectionEvent e) {						
						pageableTable.refreshPage(true); // needed to refresh pagination control -> bug in original code!						
						super.widgetSelected(e);
					}
				};				
				pageSizeComboDecorator.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
				pageSizeComboDecorator.pageSizeChanged(initialPageSize, initialPageSize, getController());
				
				if (ATableWidgetPagination.this.withFilter) {
//					Label filterLabel = new Label(bottom, 0);
//					filterLabel.setText("Filter: ");
					
					filter = new Text(bottom, SWT.BORDER | SWT.SEARCH);
					filter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					filter.setToolTipText("Filter this list with a keyword");
					filter.setMessage("Filter");
//					filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 2));
				}
				
				loadingComposite = new LoadingComposite(bottom, false);
				loadingComposite.reload.addSelectionListener(new SelectionAdapter() {
					@Override public void widgetSelected(SelectionEvent e) {
						onReloadButtonPressed();
					}
				});
				loadingComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
								
				return bottom;
			}
		};
		
//		pageableTable.getPageRendererTopFactory()
	
//		loadingComposite = (LoadingComposite) pageableTable.getCompositeBottom();
//		loadingComposite.reload.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				pageableTable.refreshPage(true);
//			}
//		});

		pageableTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
//		PageSizeComboRenderer pageSizeComboDecorator = new PageSizeComboRenderer(
//				this, SWT.NONE, pageableTable.getController(), new Integer[] { 5, 10, 25, 50, 75, 100, 200 }) {
//			
//			public void widgetSelected(SelectionEvent e) {
//				super.widgetSelected(e);
////				pageableTable.setCurrentPage(0);
//				pageableTable.refreshPage(true); // needed to refresh pagination control -> bug!
//			}
//		};
//		pageSizeComboDecorator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		pageSizeComboDecorator.pageSizeChanged(initialPageSize, initialPageSize, pageableTable.getController());
		
		tv = pageableTable.getViewer();
		tv.setContentProvider(new ArrayContentProvider());
		tv.setLabelProvider(new LabelProvider());
//		documentsTv.setLabelProvider(new DocTableLabelProvider(this));
		
		Table table = tv.getTable();
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createColumns();
		setPageLoader();
		
		pageableTable.setPageLoaderHandler(new IPageLoaderHandler<PageableController>() {
			long time = 0;
			
			public void onBeforePageLoad(PageableController controller) {
//				logger.debug("onBeforePageLoad");
				
				time = System.currentTimeMillis();
				String text = "Loading...";
				logger.trace(text);
				loadingComposite.setText(text);
			}

			public boolean onAfterPageLoad(
					PageableController controller, Throwable e) {
//				logger.debug("onAfterPageLoad");
				long diff = System.currentTimeMillis() - time;
				logger.trace("after page reload: "+diff);
				String text = "Loaded in "+ diff + "(ms) ";
				logger.trace(text);
				loadingComposite.setText(text);
				
//				if (itemToSelect != null) {
//					logger.debug("itemToSelect = "+itemToSelect);
//					selectElement(itemToSelect);
//					itemToSelect = null;
//				}
				
				return true;
			}
		});
	}
	
	protected void onReloadButtonPressed() {
		pageableTable.refreshPage();
	}
	
	protected abstract void setPageLoader();
	
	public void refreshPage(boolean resetToFirstPage) {
		pageableTable.refreshPage(resetToFirstPage);
	}
	
	public Button getReloadButton() {
		return loadingComposite.reload;
	}
	
	public boolean isRecycleBin() {
		return isRecycleBin;
	}

	public void setRecycleBin(boolean isRecycleBin) {
		this.isRecycleBin = isRecycleBin;
	}
	
	public TableViewer getTableViewer() { return tv; }
	
	public void selectElement(T el) {
		if (el != null) {
			getTableViewer().setSelection(new StructuredSelection(el), true);
		}
	}
	
	public void clearSelection() {
		getTableViewer().setSelection(null);
	}
	
	public T getFirstSelected() {
		if(tv == null) {
			return null;
		}
		IStructuredSelection sel = (IStructuredSelection) tv.getSelection();		
		return (T) sel.getFirstElement();
	}
	
	public List<T> getSelected() {
		if(tv == null) {
			return new ArrayList<>(0);
		}
		return ((IStructuredSelection) tv.getSelection()).toList();
	}
	
	public IStructuredSelection getSelectedAsIStructuredSelection() {
		return ((IStructuredSelection) tv.getSelection());
	}
	
	public List<T> getItemsOfCurrentPage() {
		return (List<T>) pageableTable.getViewer().getInput();
	}
	
	protected abstract void createColumns();

	protected TableViewerColumn createColumn(String columnName, int colSize, String sortPropertyName, CellLabelProvider lp) {
		TableViewerColumn col = TableViewerUtils.createTableViewerColumn(tv, 0, columnName, colSize);
		col.setLabelProvider(lp);
		if (sortPropertyName != null)
			col.getColumn().addSelectionListener(new SortTableColumnSelectionListener(sortPropertyName));
		return col;
	}
	
	protected TableViewerColumn createDefaultColumn(String columnName, int colSize, String propertyName, boolean sortable) {
		return createColumn(columnName, colSize, sortable ? propertyName : null, new TableColumnBeanLabelProvider(propertyName));
	}
	
	protected TableViewerColumn createDefaultColumn(String columnName, int colSize, String labelPropertyName, String sortPropertyName) {
		return createColumn(columnName, colSize, sortPropertyName, new TableColumnBeanLabelProvider(labelPropertyName));
	}

}
