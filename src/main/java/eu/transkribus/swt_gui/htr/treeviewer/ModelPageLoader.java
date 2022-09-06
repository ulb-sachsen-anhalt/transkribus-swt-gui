package eu.transkribus.swt_gui.htr.treeviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ServerErrorException;

import org.eclipse.nebula.widgets.pagination.PageableController;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.rest.TrpModelMetadataList;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.models.ModelFilterWidget;
import eu.transkribus.swt_gui.models.ModelFilterWithProviderWidget;

public class ModelPageLoader extends RemotePageLoaderSingleRequest<TrpModelMetadataList, TrpModelMetadata> {
	private static final Logger logger = LoggerFactory.getLogger(ModelPageLoader.class);

	Storage store = Storage.getInstance();
	
	private final ModelFilterWithProviderWidget filterComposite;
	private final Text filter;

	public ModelPageLoader(PageableController controller, Text filter, ModelFilterWithProviderWidget filterComposite) {
		super(controller, null);
		this.filterComposite = filterComposite;
		this.filter = filter;
	}

	@Override
	protected TrpModelMetadataList loadPage(int fromIndex, int toIndex, String sortPropertyName, String sortDirection) {
		TrpModelMetadataList l = null;
		if (store != null && store.isLoggedIn()) {
			try {
				String htrRelease = ModelFilterWidget.LINK_FILTER_ALL;
				String langSelectionLabel = null;
				List<String> isoLangFilter = null;
				if(filterComposite != null) {
					htrRelease = filterComposite.getLinkageFilterComboText();
					langSelectionLabel = filterComposite.getLangSelectionLabel();
				}
				
				Integer userId = null;
				Integer collId = null;
				Integer releaseLevel = null;
				if (ModelFilterWidget.LINK_FILTER_COLLECTION.equals(htrRelease)) {
					collId = store.getCollId();
					//only show private models in this mode
					releaseLevel = 0;
				} else if(ModelFilterWidget.LINK_FILTER_PUBLIC.equals(htrRelease)) {
					//-1 => all public models, i.e. releaseLevel >= 1
					releaseLevel = -1;
				} else if(ModelFilterWidget.LINK_FILTER_MY_MODELS.equals(htrRelease)) {
					userId = store.getUser().getUserId();
				} else { //if(ModelFilterWidget.LINK_FILTER_ALL.equals(htrRelease))
					//keep defaults = all null => do not filter
				}
			
				if(langSelectionLabel != null) {
					isoLangFilter = Arrays.asList(langSelectionLabel);
				}
				
				String filterTxt = filter.getText();
				String typeFilter = filterComposite.getSelectedType();
				logger.debug("load HTRs from DB with filter: " + filterTxt);
				logger.debug("userIdFilter: {}", userId);
				logger.debug("In collection: {}", collId);
				logger.debug("typeFilter: {}", typeFilter);
				logger.debug("providerFilter: {}", getProviderComboValue());
				logger.debug("linkage filter: {}", htrRelease);
				logger.debug("htr release is : {}", releaseLevel);
				logger.debug("isoLangFilter: {}", isoLangFilter);
				logger.debug("sort: {}, {}", sortPropertyName, sortDirection);
				
				l = store.getConnection().getModelCalls().getModels(typeFilter, collId, userId, releaseLevel, filterTxt, getProviderComboValue(), 
						null, isoLangFilter, null, null, null, null, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
						
						//("text", collId, getProviderComboValue(), filterTxt, releaseLevel, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
				if (l.getList()== null){
					logger.debug("the result list is null - no htr match the search string");
					//if we set not this the old entries persist in the table!!
					l = new TrpModelMetadataList();
					l.setList(new ArrayList<>());
					l.setIndex(0);
					l.setnValues(0);
					l.setTotal(0);
				}
				
				if (filterComposite != null) {
					filterComposite.updateLanguages(l);
				}
			} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
				TrpMainWidget.getInstance().onError("Error loading HTRs", e.getMessage(), e);
			}
		}
		if(l == null) {
			l = new TrpModelMetadataList();
			l.setList(new ArrayList<>());
			l.setIndex(0);
			l.setnValues(0);
			l.setTotal(0);
		}
		return l;
	}
	
	public String getProviderComboValue() {
		if (filterComposite != null) {
			Combo providerCombo = filterComposite.getProviderCombo();
			return (String) providerCombo.getData(providerCombo.getText());
		}
		return null;
	}
	
}
