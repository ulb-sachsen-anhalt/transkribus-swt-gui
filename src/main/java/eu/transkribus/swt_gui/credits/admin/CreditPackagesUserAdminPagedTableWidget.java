package eu.transkribus.swt_gui.credits.admin;

import java.util.ArrayList;

import javax.ws.rs.ServerErrorException;

import org.eclipse.nebula.widgets.pagination.table.PageableTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.core.model.beans.auth.TrpUser;
import eu.transkribus.core.model.beans.rest.TrpCreditPackageList;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt_gui.credits.OverallBalanceComposite;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.pagination_tables.CreditPackagesUserPagedTableWidget;

public class CreditPackagesUserAdminPagedTableWidget extends CreditPackagesUserPagedTableWidget {
	
	public static final String PACKAGE_ORDER_ID_COL = "Shop Order ID";
	
	//Admin has option to create packages
	Button createBtn, detailsBtn;
	//Admin can view packages of arbitrary userIDs
	Integer  userId;
	public CreditPackagesUserAdminPagedTableWidget(Composite parent, int style) {
		super(parent, style);
	}
	
	public void setUser(TrpUser user) {
		setUserId(user == null ? null : user.getUserId());
	}
	
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	
	public Integer getUserId() {
		return userId;
	}
	
	public Button getCreatePackageBtn() {
		return createBtn;
	}
	
	public Button getDetailsBtn() {
		return detailsBtn;
	}
	
	@Override
	protected void createOverallBalanceComposite(PageableTable pageableTable) {
		// Create the composite in the bottom right of the table widget
		Composite parent = pageableTable.getCompositeBottom();
		int layoutColsIncrement = 3;
		
		createBtn = new Button(parent, SWT.PUSH);
		createBtn.setImage(Images.ADD);
		createBtn.setToolTipText("Create a credit package...");
		
		detailsBtn = new Button(parent, SWT.PUSH);
		detailsBtn.setImage(Images.INFO);
		detailsBtn.setToolTipText("Show package details...");
		
		overallBalanceComp = new OverallBalanceComposite(parent, SWT.NONE);
		overallBalanceComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		//adjust layout of bottom
		GridLayout layout = (GridLayout) parent.getLayout();
		layout.numColumns += layoutColsIncrement;
		parent.pack();
	}
	
	@Override
	protected void createColumns() {
		super.createColumns(true);
		createColumn(PACKAGE_ORDER_ID_COL, 80, "orderId", new PackageColumnLabelProvider(p -> "" + p.getOrderId()));
	}
	
	@Override
	protected RemotePageLoaderSingleRequest<TrpCreditPackageList, TrpCreditPackage> createPageLoader() {
		IPageLoadMethod<TrpCreditPackageList, TrpCreditPackage> plm = new IPageLoadMethod<TrpCreditPackageList, TrpCreditPackage>() {

			@Override
			public TrpCreditPackageList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				if(getUserId() == null) {
					return new TrpCreditPackageList(new ArrayList<>(), 0.0d, 0, 0, 0, null, null);
				}
				Storage store = Storage.getInstance();
				if (store.isLoggedIn()) {
					try {
						Boolean isShared = null, shareable = null;
						return store.getConnection().getCreditCalls().getCreditPackagesByUser(getUserId(), 
								!getShowDisabledFilterValue(), getShowExpiredFilterValue(), getMinBalanceFilterValue(),
								isShared, shareable,
								fromIndex, toIndex - fromIndex, sortPropertyName, sortDirection);
					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
						TrpMainWidget.getInstance().onError("Error loading Credit Packages", e.getMessage(), e);
					}
				}
				return new TrpCreditPackageList(new ArrayList<>(), 0.0d, 0, 0, 0, null, null);
			}
		};
		return new RemotePageLoaderSingleRequest<>(pageableTable.getController(), plm);
	}
}