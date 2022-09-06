package eu.transkribus.swt_gui.pagination_tables;

import java.util.ArrayList;

import javax.ws.rs.ServerErrorException;

import org.eclipse.nebula.widgets.pagination.IPageLoader;
import org.eclipse.nebula.widgets.pagination.collections.PageResult;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCreditTransaction;
import eu.transkribus.core.model.beans.rest.TrpCreditTransactionList;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class CreditTransactionsByPackagePagedTableWidget extends ACreditTransactionsPagedTableWidget {
	private static final Logger logger = LoggerFactory.getLogger(CreditTransactionsByPackagePagedTableWidget.class);
	
	public static final String TA_JOB_ID_COL = "Job ID";
	
	private Integer packageId;
	
	public CreditTransactionsByPackagePagedTableWidget(Composite parent, int style) {
		super(parent, style);
		packageId = null;
	}
	
	public void setPackageId(Integer packageId) {
		this.packageId = packageId;
		this.refreshPage(true);
	}
	
	@Override
	protected void createColumns() {
		super.createColumns();
		createDefaultColumn(TA_JOB_ID_COL, 50, "jobId", true);
	}
	
	@Override
	protected void setPageLoader() {
		IPageLoadMethod<TrpCreditTransactionList, TrpCreditTransaction> plm = new IPageLoadMethod<TrpCreditTransactionList, TrpCreditTransaction>() {
			@Override
			public TrpCreditTransactionList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				Storage store = Storage.getInstance();
				TrpCreditTransactionList l = new TrpCreditTransactionList(new ArrayList<>(), 0, 0, 0, null, null);
				if(packageId == null) {
					logger.debug("No packageId set => not loading transactions.");
					return l;
				}
				if(!store.isLoggedIn()) {
					logger.debug("Not logged in.");
					return l;
				}
				try {
					l = store.getConnection().getCreditCalls().getTransactionsByPackage(packageId, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
				} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
					TrpMainWidget.getInstance().onError("Error loading HTRs", e.getMessage(), e);
				}
				return l;
			}
		};
		final IPageLoader<PageResult<TrpCreditTransaction>> pl = new RemotePageLoaderSingleRequest<>(pageableTable.getController(), plm);
		pageableTable.setPageLoader(pl);		
	}
}