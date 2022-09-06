package eu.transkribus.swt_gui.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.gvt.event.SelectionAdapter;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.model.beans.rest.TrpModelMetadataList;
import eu.transkribus.core.model.beans.searchresult.FacetRepresentation;
import eu.transkribus.core.model.beans.searchresult.FacetRepresentation.FacetValueRepresentation;
import eu.transkribus.core.util.IsoLangUtils;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.SWTUtil;

public class ModelFilterWithProviderWidget extends ModelFilterWidget {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ModelFilterWithProviderWidget.class);
	
	protected Combo typeCombo;
	protected Combo providerCombo;
	protected Combo langFilterCombo;
	
	final private String htrProviderFilterValue;
	final private String typeFilterValue;
	
	public static final String ALL_TYPES = "all";
	
	public interface IModelFilterWithProviderListener {
		void comboFiltersChanged(Combo c);
	}
	
	private List<IModelFilterWithProviderListener> listener = new ArrayList<>();
	
	public ModelFilterWithProviderWidget(Composite parent, StructuredViewer viewer, final String typeFilterValue, final String htrProviderFilterValue, int style) {
		super(parent, viewer, style);
		
		this.typeFilterValue =  typeFilterValue;
		this.htrProviderFilterValue = htrProviderFilterValue;	
		
		typeCombo = new Combo(this, SWT.READ_ONLY);
		typeCombo.setToolTipText("The model output type");
		if (this.typeFilterValue == null) {
//			typeCombo.add(ALL_TYPES); // filtering for all types currently not supported!
			for (String type : ModelUtil.ALL_TYPES) {
				typeCombo.add(type);
			}
		}
		else {
			typeCombo.add(typeFilterValue);
			typeCombo.setEnabled(false);
		}
		typeCombo.select(0);
		SWTUtil.onSelectionEvent(typeCombo, e -> {
			updateUi();
		});		
		
		providerCombo = new Combo(this, SWT.READ_ONLY);
		providerCombo.setToolTipText("The name of the engine provider");
		//filtering by provider is done in Storage and that's why the listener is attached in the outer Composite
//		providerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		langFilterCombo = new Combo(this, SWT.READ_ONLY);
		langFilterCombo.setToolTipText("The language of the model");
//		providerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		SWTUtil.onSelectionEvent(linkageFilterCombo, e -> triggerFilterChanged(linkageFilterCombo));
		SWTUtil.onSelectionEvent(typeCombo, e -> triggerFilterChanged(typeCombo));
		SWTUtil.onSelectionEvent(providerCombo, e -> triggerFilterChanged(providerCombo));
		SWTUtil.onSelectionEvent(langFilterCombo, e -> triggerFilterChanged(langFilterCombo));
		
		updateUi();
	}
	
	private void triggerFilterChanged(Combo c) {
		listener.stream().forEach(l -> l.comboFiltersChanged(c));
	}
	
	public void addListener(IModelFilterWithProviderListener l) {
		listener.add(l);
	}
	
	public void removeListener(IModelFilterWithProviderListener l) {
		listener.remove(l);
	}	
	
	private void updateUi() {
		providerCombo.removeAll();
		if(htrProviderFilterValue == null) {
			addProviderFilter(providerCombo, "All engines", null);
			for (String p : ModelUtil.getProviderForType(getSelectedType())) {
				addProviderFilter(providerCombo, ModelViewerLabelProvider.getLabelForHtrProvider(p), p);
			}
		} else {
			addProviderFilter(providerCombo, ModelViewerLabelProvider.getLabelForHtrProvider(htrProviderFilterValue), htrProviderFilterValue);
			//lock the combo as no choice is allowed
			providerCombo.setEnabled(false);
		}
		providerCombo.select(0);
		
		langFilterCombo.setVisible(ModelUtil.hasTypeLanguage(getSelectedType()));
	}
	
	@Override
	public void addListener(int eventType, Listener listener) {
		super.addListener(eventType, listener);
		providerCombo.addListener(eventType, listener);
	}
	
	public String getLangSelectionLabel() {
		return (String) langFilterCombo.getData(langFilterCombo.getText());
	}
	
	/**
	 * set two additional columns to hold the provider combo filter
	 */
	@Override
	protected Layout createLayout() {
		return new GridLayout(5, false);
	}
	
	private void addProviderFilter(Combo providerCombo, String label, String data) {
		providerCombo.add(label);
		providerCombo.setData(label, data);
	}
	
	public void resetProviderFilter() {
		if(providerCombo != null) {
			providerCombo.select(0);
		}
	}
	
	public Combo getProviderCombo() {
		return providerCombo;
	}
	
	public Combo getLangFilterCombo() {
		return langFilterCombo;
	}
	
	public Text getFilterText() {
		return filterTxt;
	}
	
	public String getSelectedType() {
		return typeCombo.getText();
	}
	
	public void updateLanguages(TrpModelMetadataList l) {
		String langSelectionLbl = getLangSelectionLabel();
		langFilterCombo.removeAll();
		final String allLangs = "All Languages";
		langFilterCombo.add(allLangs);
		langFilterCombo.setData(allLangs, null);
			
		if(l!=null && l.getFacets() != null) {		
			for(FacetRepresentation f : l.getFacets()) {
				if(f.getName().equals("isoLanguages")) {
					int langSelectionIndex = 0;
					for(int i = 0; i < f.getValues().size(); i++) {
						FacetValueRepresentation v = f.getValues().get(i);
						String label = String.format("%s (%d)", 
								IsoLangUtils.resolveLabelFromCode(v.getValue()), v.getCount());
						String data = v.getValue();
						langFilterCombo.add(label);
						langFilterCombo.setData(label, data);
						if(data.equals(langSelectionLbl)) {
							langSelectionIndex = i+1;
						}
					}
					langFilterCombo.select(langSelectionIndex);
				}
			}
			
		}
		langFilterCombo.layout();
		
		this.layout();
	}
}
