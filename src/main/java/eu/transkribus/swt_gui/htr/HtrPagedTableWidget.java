package eu.transkribus.swt_gui.htr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.nebula.widgets.pagination.IPageLoader;
import org.eclipse.nebula.widgets.pagination.collections.PageResult;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.rest.TrpHtrList;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.DelayedTask;

public class HtrPagedTableWidget extends ATableWidgetPagination<TrpHtr> {
	private static final Logger logger = LoggerFactory.getLogger(HtrPagedTableWidget.class);
	
	public final static String[] providerValues = ModelUtil.ALL_PROVIDER;	
	
	public static final String HTR_NAME_COL = "Name";
	public static final String HTR_LANG_COL = "Language";
	public static final String HTR_CREATOR_COL = "Curator";
	public static final String HTR_TECH_COL = "Technology";
	public static final String HTR_DATE_COL = "Created";
	public static final String HTR_ID_COL = "ID";
	public static final String HTR_WORDS_COL = "nrOfWords";
	public static final String HTR_CER_TRAIN_COL = "CER Train";
	public static final String HTR_CER_VAL_COL = "CER Validation ";
	public static final String HTR_RATING_COL = "Rating";
	
	// filter:
	HtrFilterWithProviderWidget filterComposite;
	private final String providerFilter;
	
	public HtrPagedTableWidget(Composite parent, int style, String providerFilter) {
		super(parent, style, 25, null, true);
		
		if(providerFilter != null && !Arrays.stream(providerValues).anyMatch(s -> s.equals(providerFilter))) {
			throw new IllegalArgumentException("Invalid providerFilter value");
		}
		
		this.providerFilter = providerFilter;
		this.setLayout(new GridLayout(1, false));
		
		addFilter();
	}
	
	@Override
	public void addListener(int eventType, Listener listener) {
		super.addListener(eventType, listener);
		filterComposite.addListener(eventType, listener);
	}
	
	private void addFilter() {
		filterComposite = new HtrFilterWithProviderWidget(this, getTableViewer(), providerFilter, SWT.NONE) {
			@Override
			protected void refreshViewer() {
				logger.debug("refreshing viewer...");
				refreshPage(true);
			}
			@Override
			protected void attachFilter() {
			}
		};
		filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterComposite.getFilterText().setParent(SWTUtil.dummyShell);
		filterComposite.layout();
		filterComposite.moveAbove(null);
		
		filterComposite.getProviderCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				refreshPage(true);
			}
		});
		
		ModifyListener filterModifyListener = new ModifyListener() {
			DelayedTask dt = new DelayedTask(() -> { 
				refreshPage(true);
			}, true);
			@Override public void modifyText(ModifyEvent e) {
				dt.start();
			}
		};
		filter.addModifyListener(filterModifyListener);
		filter.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_RETURN) {
					refreshPage(true);
				}
			}
		});
	}
	
	void resetProviderFilter() {
		filterComposite.resetProviderFilter();
	}
	
	public String getProviderComboValue() {
		Combo providerCombo = filterComposite.getProviderCombo();
		return (String) providerCombo.getData(providerCombo.getText());
	}
	
	public TrpHtr getSelectedHtr() {
		return getFirstSelected();
//		IStructuredSelection sel = (IStructuredSelection) htrTv.getSelection();
//		if (sel.getFirstElement() != null && sel.getFirstElement() instanceof TrpHtr) {
//			return (TrpHtr) sel.getFirstElement();
//		} else
//			return null;

	}

	public void refreshList(List<TrpHtr> htrs) {
		// TODO: htrs are reloaded using the IPageLoadMethod created in setPageLoader method
		// --> no need to set them here
		
		logger.debug("refreshList");
		refreshPage(true);
	}

	public void setSelection(int htrId) {
		// TODO
		
//		List<TrpHtr> htrs = (List<TrpHtr>)htrTv.getInput();
//		TrpHtr htr = null;
//		for(int i = 0; i < htrs.size(); i++){
//			final TrpHtr curr = htrs.get(i);
//			if(curr.getHtrId() == htrId){
//				logger.trace("Found htrId {}", htrId);
//				htr = curr;
//				break;
//			}
//		}
//		logger.trace("Selecting HTR in table viewer: {}", htr);
//		if(htr != null) { //if model has been removed from this collection it is not in the list.
//			htrTv.setSelection(new StructuredSelection(htr), true);
//		} else {
//			htrTv.setSelection(null);
//		}
	}

	@Override
	protected void setPageLoader() {
		IPageLoadMethod<TrpHtrList, TrpHtr> plm = new IPageLoadMethod<TrpHtrList, TrpHtr>() {
			Storage store = Storage.getInstance();
			TrpHtrList l;
			
			private void load(int fromIndex, int toIndex, String sortPropertyName, String sortDirection) {
				if (store != null && store.isLoggedIn()) {
					try {
						Integer collId = store.getCollId();
						String htrRelease = filterComposite.getLinkageFilterComboText();
						Integer releaseLevel = htrRelease.contains("All")? null : htrRelease.contains("Public")? -1 : 0;
						if (store.isAdminLoggedIn() && releaseLevel == null){
							collId = null;
						}
					
						String filterTxt = filter.getText();
						logger.debug("load HTRs from DB with filter: " + filterTxt);
						logger.debug("providerFilter: " + getProviderComboValue());
						logger.debug("linkage filter: " + filterComposite.getLinkageFilterComboText());
						logger.debug("htr release is : " + releaseLevel);
						
						l = store.getConnection().getHtrsSync(collId, getProviderComboValue(), filterTxt, releaseLevel, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
						if (l.getList()== null){
							logger.debug("the result list is null - no htr match the search string");
							//if we set not this the old entries persist in the table!!
							l = new TrpHtrList(new ArrayList<>(), 0, 0, 0, null, null);
						}
						
					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
						TrpMainWidget.getInstance().onError("Error loading HTRs", e.getMessage(), e);
					}
				}
				else {
					l = new TrpHtrList(new ArrayList<>(), 0, 0, 0, null, null);
				}
			}			
			
			private void applyFilter() {
				logger.debug("in filter function");
				if (filterComposite!=null && l!=null && l.getList()!=null) {
					logger.debug("filtering htrs..., N-before = "+l.getList().size());
					l.getList().removeIf(htr -> !filterComposite.getViewerFilter().select(getTableViewer(), null, htr));
					l.setTotal(l.getList().size());
					logger.debug("filtering htrs..., N-after = "+l.getList().size());
				}
			}
			
			@Override
			public TrpHtrList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				//pageableTable.refreshPage();
				load(fromIndex, toIndex, sortPropertyName, sortDirection);
				//applyFilter();
				return l;
			}
		};
		final IPageLoader<PageResult<TrpHtr>> pl = new RemotePageLoaderSingleRequest<>(pageableTable.getController(), plm);
		pageableTable.setPageLoader(pl);	
		

	}

	@Override
	protected void createColumns() {
		HtrTableLabelProvider lp = new HtrTableLabelProvider(tv);
		//createDefaultColumn(HTR_NAME_COL, 210, "name", true);
		createColumn(HTR_NAME_COL, 210, "name", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setImage(lp.getColumnImage((TrpHtr)cell.getElement(), cell.getColumnIndex()));
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_NAME_COL));	
				}
			}
		});
		
		
//		createDefaultColumn(HTR_LANG_COL, 100, "language", true);
		
		createColumn(HTR_LANG_COL, 100, "language", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setImage(lp.getColumnImage((TrpHtr)cell.getElement(), cell.getColumnIndex()));
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_LANG_COL));	
				}
			}
		});
		
		createColumn(HTR_CREATOR_COL, 120, "userName", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_CREATOR_COL));	
				}
			}
		});
		createColumn(HTR_TECH_COL, 80, "provider", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_TECH_COL));	
				}
			}
		});
		createColumn(HTR_DATE_COL, 60, "created", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_DATE_COL));
				}
			}
		});
		createDefaultColumn(HTR_WORDS_COL, 80, "nrOfWords", true);

		//DefaultColumn using BeanUtils will set some Double values in scientific notation. Use formatting as in HtrDetailsWidget
		createColumn(HTR_CER_TRAIN_COL, 80, "finalCer", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_CER_TRAIN_COL));
				}
			}
		});
		createColumn(HTR_CER_VAL_COL, 80, "finalCerTest", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpHtr) {
					cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_CER_VAL_COL));
				}
			}
		});
		if (Storage.getInstance()!=null && Storage.getInstance().isAdminLoggedIn()) {
			TableViewerColumn tc = createColumn(HTR_RATING_COL, 80, "internalRating", new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					if (cell.getElement() instanceof TrpHtr) {
						cell.setText(lp.getColumnText((TrpHtr)cell.getElement(), HTR_RATING_COL));
					}
				}
			});
		}
		createDefaultColumn(HTR_ID_COL, 50, "htrId", true);
	}	
}