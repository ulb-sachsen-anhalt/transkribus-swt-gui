package eu.transkribus.swt_gui.htr;

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

public class HtrFilterWidget extends TrpViewerFilterWidget {
	
	Combo linkageFilterCombo;
	
	private final static String LINK_FILTER_ALL = "All";
	private final static String LINK_FILTER_COLLECTION = "In Collection";
	private final static String LINK_FILTER_PUBLIC = "Public Models";
	
	public HtrFilterWidget(Composite parent, StructuredViewer viewer, int style) {
		super(parent, viewer, false, TrpHtr.class, "htrId", "name", "language", "userName");
	}
	
	@Override
	protected void createCompositeArea(boolean withFilterLbl) {
		super.createCompositeArea(withFilterLbl);
		linkageFilterCombo = new Combo(this, SWT.READ_ONLY);
		linkageFilterCombo.add(LINK_FILTER_ALL);
		//FIXME collectionId is not set for admins in current HtrDao and filtering by this is not possible.
		linkageFilterCombo.add(LINK_FILTER_COLLECTION);
		linkageFilterCombo.add(LINK_FILTER_PUBLIC);
		linkageFilterCombo.select(0);
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

	public void setLinkageFilterCombo(Combo linkageFilterCombo) {
		this.linkageFilterCombo = linkageFilterCombo;
	}
}
