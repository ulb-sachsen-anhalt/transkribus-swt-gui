package eu.transkribus.swt_gui.credits;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpCreditHistoryEntry;
import eu.transkribus.core.model.beans.auth.TrpUser;
import eu.transkribus.core.model.beans.rest.TrpCreditHistoryList;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.dialogs.FindUserDialog;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.pagination_tables.CreditHistoryCollectionPagedTableWidget;
import eu.transkribus.swt_gui.pagination_tables.CreditHistoryUserPagedTableWidget;

public class CreditHistoryWidget extends SashForm {
	private static final Logger logger = LoggerFactory.getLogger(CreditHistoryWidget.class);
	
	protected Text userIdTxt;
	protected CreditHistoryUserPagedTableWidget userHistoryTable;
	protected Group collectionCreditGroup;
	protected CreditHistoryCollectionPagedTableWidget collectionHistoryTable;
	
	protected Button addToCollectionBtn, removeFromCollectionBtn, transferToUserBtn;
	private Text amountTxt;
	private TrpCollection collection;
	
	public CreditHistoryWidget(Composite parent, int style) {
		super(parent, SWT.HORIZONTAL | style);
		this.setLayout(SWTUtil.createGridLayout(3, false, 0, 0));
		
		Group userCreditGroup = new Group(this, SWT.BORDER);
//		if(Storage.getInstance().isAdminLoggedIn()) {
//			userIdTxt = new Text(userCreditGroup, SWT.BORDER);
//			userIdTxt.setMessage("Enter userId");
//			userIdTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			
//		}
		userCreditGroup.setLayout(new GridLayout(1, true));
		userCreditGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		userCreditGroup.setText("My Credits");
		userHistoryTable = new  CreditHistoryUserPagedTableWidget(userCreditGroup, SWT.NONE);
		userHistoryTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite buttonComp = new Composite(this, SWT.NONE);
		buttonComp.setLayout(new GridLayout(1, true));
		buttonComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label space = new Label(buttonComp, SWT.NONE);
		space.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		
		amountTxt = new Text(buttonComp, SWT.BORDER);
		amountTxt.setMessage("Amount");
		amountTxt.setToolTipText("Enter amount to transfer");
		amountTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addToCollectionBtn = new Button(buttonComp, SWT.PUSH);
		addToCollectionBtn.setImage(Images.ARROW_RIGHT);
		addToCollectionBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addToCollectionBtn.setToolTipText("Transfer credits to this collection");
		removeFromCollectionBtn = new Button(buttonComp, SWT.PUSH);
		removeFromCollectionBtn.setImage(Images.ARROW_LEFT);
		removeFromCollectionBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		removeFromCollectionBtn.setToolTipText("Remove credits from this collection");
		transferToUserBtn = new Button(buttonComp, SWT.PUSH);
		transferToUserBtn.setImage(Images.USER_GO_ICON);
		transferToUserBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		transferToUserBtn.setToolTipText("Transfer credits to another user account");
		
		Label space2 = new Label(buttonComp, SWT.NONE);
		space2.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
		
		collectionCreditGroup = new Group(this, SWT.BORDER);
		collectionCreditGroup.setLayout(new GridLayout(1, true));
		collectionCreditGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		//group's title text is updated when data is loaded
		collectionHistoryTable = new CreditHistoryCollectionPagedTableWidget(collectionCreditGroup, SWT.NONE);
		collectionHistoryTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Menu menu = new Menu(userHistoryTable.getTableViewer().getTable());
		userHistoryTable.getTableViewer().getTable().setMenu(menu);
		
		final int buttonWeight = 6;
		this.setWeights(new int[] { 47, buttonWeight, 47 });
		
		addListeners();
	}

	public void updateCollectionCreditGroupText(TrpCollection collection) {
		String text = "Credits in Collection";
		if(collection != null) {
			text += " '" + collection.getColName() + "'";
		}
		collectionCreditGroup.setText(text);
	}

	public void setCollection(TrpCollection collection, boolean resetTablesToFirstPage) {
		this.collection = collection;
		updateCollectionCreditGroupText(collection);
		collectionHistoryTable.setCollection(collection);
		userHistoryTable.refreshPage(resetTablesToFirstPage);
		collectionHistoryTable.refreshPage(resetTablesToFirstPage);
	}
	
	private void addListeners() {
		addToCollectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Integer amount = getTransferAmount();
				if(amount == null) {
					return;
				}
				if(collection == null) {
					return;
				}
				TrpCreditHistoryEntry entry = new TrpCreditHistoryEntry();
				entry.setCreditValue(amount);
				entry.setSourceUserId(Storage.getInstance().getUserId());
				entry.setTargetCollectionId(collection.getColId());
				TrpCreditHistoryList result = requestTransfer(entry);
				logger.debug("Transfer result: {}", result);
				userHistoryTable.refreshPage(true);
				collectionHistoryTable.refreshPage(true);
			}
		});
		removeFromCollectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Integer amount = getTransferAmount();
				if(amount == null) {
					return;
				}
				if(collection == null) {
					return;
				}
				TrpCreditHistoryEntry entry = new TrpCreditHistoryEntry();
				entry.setCreditValue(amount);
				entry.setSourceCollectionId(collection.getColId());
				entry.setTargetUserId(Storage.getInstance().getUserId());
				TrpCreditHistoryList result = requestTransfer(entry);
				logger.debug("Transfer result: {}", result);
				userHistoryTable.refreshPage(true);
				collectionHistoryTable.refreshPage(true);
			}
		});
		transferToUserBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Integer amount = getTransferAmount();
				if(amount == null) {
					return;
				}
				if(collection == null) {
					return;
				}
				TrpCreditHistoryEntry entry = new TrpCreditHistoryEntry();
				entry.setCreditValue(amount);
				FindUserDialog fud = new FindUserDialog(CreditHistoryWidget.this.getShell());
				int returnCode = fud.open();
				if(returnCode != IDialogConstants.OK_ID || fud.getSelectedUsers().isEmpty()) {
					logger.debug("FindUserDialog exited with returnCode = {}", returnCode);
				}
				TrpUser user = fud.getSelectedUsers().get(0);
				logger.debug("Credit transfer user selection: {}", user);
				entry.setTargetUserId(user.getUserId());
				int answer = DialogUtil.showYesNoDialog(getShell(), "Are you sure?", 
						"Please confirm the transfer of " + amount + " credits to " + user.getEmail());
				if (SWT.YES == answer) {
					TrpCreditHistoryList result = requestTransfer(entry);
					logger.debug("Transfer result: {}", result);
					userHistoryTable.refreshPage(true);
					collectionHistoryTable.refreshPage(true);
				}
			}
		});
		userHistoryTable.getShowBalanceDetailsButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreditPackageDetailsDialog d = 
						new CreditPackageDetailsDialog(getShell(), userHistoryTable.getCurrentBalance());
				d.open();
			}
		});
		collectionHistoryTable.getShowBalanceDetailsButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreditPackageDetailsDialog d = 
						new CreditPackageDetailsDialog(getShell(), collectionHistoryTable.getCurrentBalance());
				d.open();
			}
		});
	}
	
	protected TrpCreditHistoryList requestTransfer(TrpCreditHistoryEntry entry) {
		try {
			return Storage.getInstance().getConnection().getCreditCalls().requestTransfer(entry);
		} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e1) {
			DialogUtil.showBalloonToolTip(amountTxt, null, "Transfer failed", e1.getMessage());
			return null;
		}
	}

	public Integer getTransferAmount() {
		String str = amountTxt.getText();
		if(str.trim().isEmpty()) {
			DialogUtil.showBalloonToolTip(amountTxt, null, "Illegal value", "Please enter a positive amount to transfer");
			return null;
		}
		final int amount;
		try {
			amount = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			DialogUtil.showBalloonToolTip(amountTxt, null, "Illegal value", "Please enter a positive amount to transfer");
			return null;
//			throw new IllegalArgumentException("Transfer amount is not a number: " + str, e);
		}
		if(amount <= 0) {
			DialogUtil.showBalloonToolTip(amountTxt, null, "Illegal value", "Please enter a positive amount to transfer");
			return null;
		}
		return amount;
	}
}
