package eu.transkribus.swt_gui.pagination_tables;

import java.util.ArrayList;

import javax.ws.rs.ServerErrorException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.ibm.icu.math.BigDecimal;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpCreditHistoryEntry;
import eu.transkribus.core.model.beans.rest.TrpCreditHistoryList;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt_gui.credits.ABalanceComposite;
import eu.transkribus.swt_gui.credits.CollectionBalanceComposite;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * Page loader will retrieve credit packages
 *
 */
public class CreditHistoryCollectionPagedTableWidget extends CreditHistoryUserPagedTableWidget {
	
	TrpCollection collection;
	
	public CreditHistoryCollectionPagedTableWidget(Composite parent, int style) {
		super(parent, style);
	}
	
	public void setCollection(TrpCollection collection) {
		this.collection = collection;
	}

	public TrpCollection getCollection() {
		return collection;
	}
	
	protected RemotePageLoaderSingleRequest<TrpCreditHistoryList, TrpCreditHistoryEntry> createPageLoader() {
		IPageLoadMethod<TrpCreditHistoryList, TrpCreditHistoryEntry> plm = new IPageLoadMethod<TrpCreditHistoryList, TrpCreditHistoryEntry>() {

			@Override
			public TrpCreditHistoryList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				if(sortPropertyName == null) {
					sortPropertyName = "time";
					sortDirection = "DESC";
				}
				Storage store = Storage.getInstance();
				if (store.isLoggedIn() && collection != null) {
					try {
						return store.getConnection().getCreditCalls().getCreditHistoryByCollection(
								collection.getColId(),
								fromIndex, toIndex - fromIndex, sortPropertyName, sortDirection);
					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
						TrpMainWidget.getInstance().onError("Error loading Credit Packages", e.getMessage(), e);
					}
				}
				return new TrpCreditHistoryList(new ArrayList<>(), 0, 0.0d, 0, 0, null, null);
			}
		};
		return new RemotePageLoaderSingleRequest<>(pageableTable.getController(), plm);
	}
	
	@Override
	protected ABalanceComposite createBalanceComposite(Composite parent) {
		return new CollectionBalanceComposite(parent, SWT.NONE);
	}
	
	@Override
	protected void createColumns() {
		createColumns(true);
	}
	
	protected String buildCreditValue(TrpCreditHistoryEntry e) {
		double value = e.getCreditValue();
		if(collection != null 
				&& e.getSourceCollectionId() != null 
				&& e.getSourceCollectionId().intValue() == collection.getColId()) {
			value = BigDecimal.valueOf(value).negate().doubleValue();
		}
		return numberFormat.format(value);
	}
}