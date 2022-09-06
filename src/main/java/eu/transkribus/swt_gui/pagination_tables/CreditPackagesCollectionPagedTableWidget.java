package eu.transkribus.swt_gui.pagination_tables;

import java.util.ArrayList;

import javax.ws.rs.ServerErrorException;

import org.eclipse.swt.widgets.Composite;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.core.model.beans.rest.TrpCreditPackageList;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class CreditPackagesCollectionPagedTableWidget extends CreditPackagesUserPagedTableWidget {
	
	TrpCollection collection;
	
	public CreditPackagesCollectionPagedTableWidget(Composite parent, int style) {
		super(parent, style);
	}
	
	public void setCollection(TrpCollection collection) {
		this.collection = collection;
	}

	public TrpCollection getCollection() {
		return collection;
	}
	
	@Override
	protected RemotePageLoaderSingleRequest<TrpCreditPackageList, TrpCreditPackage> createPageLoader() {
		IPageLoadMethod<TrpCreditPackageList, TrpCreditPackage> plm = new IPageLoadMethod<TrpCreditPackageList, TrpCreditPackage>() {

			@Override
			public TrpCreditPackageList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				Storage store = Storage.getInstance();
				if (store.isLoggedIn() && collection != null) {
					try {
						return store.getConnection().getCreditCalls().getCreditPackagesByCollection(collection.getColId(), 
								!getShowDisabledFilterValue(), getShowExpiredFilterValue(), getMinBalanceFilterValue(),
								fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
						TrpMainWidget.getInstance().onError("Error loading Credit Packages", e.getMessage(), e);
					}
				}
				return new TrpCreditPackageList(new ArrayList<>(), 0.0d, 0, 0, 0, null, null);
			}
		};
		return new RemotePageLoaderSingleRequest<>(pageableTable.getController(), plm);
	}
	
	@Override
	protected void createColumns() {
		createColumns(true);
	}
}