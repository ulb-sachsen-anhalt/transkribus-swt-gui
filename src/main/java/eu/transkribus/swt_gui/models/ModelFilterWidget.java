package eu.transkribus.swt_gui.models;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.swt.util.TrpViewerFilter;
import eu.transkribus.swt.util.TrpViewerFilterWidget;

public class ModelFilterWidget extends TrpViewerFilterWidget {
	
	Combo linkageFilterCombo;
	
	public final static String LINK_FILTER_ALL = "All";
	public final static String LINK_FILTER_COLLECTION = "In Collection";
	public final static String LINK_FILTER_PUBLIC = "Public Models";
	public final static String LINK_FILTER_MY_MODELS = "My Models";
	
	public ModelFilterWidget(Composite parent, StructuredViewer viewer, int style) {
		super(parent, viewer, false, TrpHtr.class, "htrId", "name", "language", "userName");
	}
	
	@Override
	protected void createCompositeArea(boolean withFilterLbl) {
		super.createCompositeArea(withFilterLbl);
		linkageFilterCombo = new Combo(this, SWT.READ_ONLY);
		linkageFilterCombo.add(LINK_FILTER_ALL);
		linkageFilterCombo.add(LINK_FILTER_COLLECTION);
		linkageFilterCombo.add(LINK_FILTER_MY_MODELS);
		linkageFilterCombo.add(LINK_FILTER_PUBLIC);
		linkageFilterCombo.select(0);
		linkageFilterCombo.setToolTipText("The model visibility");
	}
	
	/**
	 * set two additional columns to hold the provider combo filter
	 */
	@Override
	protected Layout createLayout() {
		return new GridLayout(3, false);
	}
	
	@Override
	protected TrpViewerFilter newTrpViewerFilter(Text filterTxt, Class<?> targetClass, String[] fieldNames) {
		return new TrpViewerFilter(filterTxt, targetClass, fieldNames) {
			
			@Override
			protected void updateView() {
				refreshViewer();
			}
			
			@Override
			protected void addListeners() {
				super.addListeners();
				ModifyListener comboModListener = new FilterModifyListener(linkageFilterCombo);
				linkageFilterCombo.addModifyListener(comboModListener);
			}
			
			/**
			 * @deprecated (most probably)
			 * This method is never called by the paged table widgets where filtering is done on server-side?
			 */
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				boolean isSelectedByTxtFilter = super.select(viewer, parentElement, element);
				
				boolean isSelectedByLinkageFilter = true;
				
				if(element instanceof TrpHtr) {
					TrpHtr htr = (TrpHtr) element;
					switch(linkageFilterCombo.getText()) {
					case LINK_FILTER_COLLECTION:
						/*
						 * FIXME collectionIdLink is NOT set for admins! Otherwise it's current collection's ID
						 * => This filter must not be shown for admins
						 */
						isSelectedByLinkageFilter = htr.getCollectionIdLink() != null;
						break;
					case LINK_FILTER_PUBLIC:
						isSelectedByLinkageFilter = htr.getReleaseLevelValue() > ReleaseLevel.None.getValue();
						break;
					case LINK_FILTER_ALL:
					default:
						isSelectedByLinkageFilter = true;
					}
				}
				
				return isSelectedByTxtFilter && isSelectedByLinkageFilter;
			}
		};
	}
	
	@Override
	public void reset() {
		super.reset();
		linkageFilterCombo.select(0);
	}

	public String getLinkageFilterComboText() {
		return linkageFilterCombo.getText();
	}
}
