package eu.transkribus.swt_gui.credits;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DatePickerComposite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.core.model.beans.auth.TrpUser;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.dialogs.FindUserDialog;

public class CreditPackagePropsAdminWidget extends Composite {
	Logger logger = LoggerFactory.getLogger(CreditPackagePropsAdminWidget.class);
	
	/**
	 * The default state for the "Enabled" checkbox if not package is set with {@link #setPackage(TrpCreditPackage)}
	 */
	private final static boolean IS_ACTIVE_DEFAULT_STATE = true;
	
	private Text idTxt;
	private DatePickerComposite datePicker;
	private Button freeOfChargeBtn, isActiveBtn, searchUserBtn;
	private Text ownerTxt;
	
	//the package to edit
	private TrpCreditPackage creditPkg;
	
	public CreditPackagePropsAdminWidget(Composite parent, int style) {
		super(parent, style);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).applyTo(this);
		
		Label idLbl = new Label(this, SWT.NONE);
		idLbl.setText("Package ID");
		idTxt = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		idTxt.setEnabled(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(idTxt);
		
		Label ownerLbl = new Label(this, SWT.NONE);
		ownerLbl.setText("Owner");
		
		Composite ownerComp = new Composite(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).applyTo(ownerComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(ownerComp);
		
		ownerTxt = new Text(ownerComp, SWT.BORDER | SWT.READ_ONLY);
		ownerTxt.setEnabled(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(ownerTxt);
		searchUserBtn = new Button(ownerComp, SWT.PUSH);
		searchUserBtn.setImage(Images.FIND);
		
		Label expirationDateLbl = new Label(this, SWT.NONE);
		expirationDateLbl.setText("Expiration Date");
		datePicker = new DatePickerComposite(this, null, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(datePicker);
		
		Label freeOfChargeLbl = new Label(this, SWT.NONE);
		freeOfChargeLbl.setText("Free of Charge");
		freeOfChargeBtn = new Button(this, SWT.CHECK);
		final String freeOfChargeToolTipMsg = "Mark packages that were given to the user free of charge. "
				+ "This property is taken into account when generating Transkribus usage reports.";
		freeOfChargeLbl.setToolTipText(freeOfChargeToolTipMsg);
		freeOfChargeBtn.setToolTipText(freeOfChargeToolTipMsg);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(freeOfChargeBtn);
		
		Label isActiveLbl = new Label(this, SWT.NONE);
		isActiveLbl.setText("Enabled");
		isActiveBtn = new Button(this, SWT.CHECK);
		isActiveBtn.setSelection(IS_ACTIVE_DEFAULT_STATE);
		final String isActiveToolTipMsg = "Untick the box to virtually delete a package. The package will be invisible to the user and is no longer taken into account when starting a job.";
		isActiveLbl.setToolTipText(isActiveToolTipMsg);
		isActiveBtn.setToolTipText(isActiveToolTipMsg);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(isActiveBtn);
		
		creditPkg = new TrpCreditPackage();
		addListeners();
	}
	
	private void addListeners() {
		SWTUtil.onSelectionEvent(searchUserBtn, e -> {
			FindUserDialog fud = new FindUserDialog(CreditPackagePropsAdminWidget.this.getShell());
			if(fud.open() == IDialogConstants.OK_ID) {
				List<TrpUser> selection = fud.getSelectedUsers();
				if(!CollectionUtils.isEmpty(selection)) {
					creditPkg.setUserId(selection.get(0).getUserId());
					creditPkg.setUserName(selection.get(0).getUserName());
					updateUI();
				}
			}
		});
		SWTUtil.onSelectionEvent(freeOfChargeBtn, e -> {
			creditPkg.setFreeOfCharge(freeOfChargeBtn.getSelection());
			updateUI();
		});
		datePicker.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				logger.debug("Date was changed to {} - event = {}", datePicker.getDate(), e);
				creditPkg.setExpirationDate(datePicker.getDate());
				//the datePicker updates itself. Don't updateUI()
			}
		});
		SWTUtil.onSelectionEvent(isActiveBtn, e -> {
			creditPkg.setActive(isActiveBtn.getSelection());
			updateUI();
		});
	}

	/**
	 * Set the given package for editing. All editing is immediately set in the package object.
	 * 
	 * @param pkg
	 */
	public void setPackage(TrpCreditPackage pkg) {
		//do not alter the given package? it comes from a table view which needs to be refreshed anyway. Use copy constructor for now.
		this.creditPkg = pkg;
		updateUI();
	}
	
	public TrpCreditPackage getPackage() {
		return creditPkg;
	}
	
	private void updateUI() {
		final String idStr = creditPkg.getPackageId() < 1 ? "N/A" : "" + creditPkg.getPackageId();
		idTxt.setText(idStr);
		ownerTxt.setText(creditPkg.getUserName());
		freeOfChargeBtn.setSelection(creditPkg.isFreeOfCharge());
		isActiveBtn.setSelection(creditPkg.isActive());
		datePicker.setDate(creditPkg.getExpirationDate());
	}
}
