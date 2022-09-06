package eu.transkribus.swt_gui.credits.admin;

import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TrpNumberTextComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.core.model.beans.TrpCreditProduct;
import eu.transkribus.core.model.beans.auth.TrpUser;
import eu.transkribus.core.model.beans.job.enums.JobType;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.credits.CreditPackagePropsAdminWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.pagination_tables.CreditProductsPagedTableWidget;

public class CreateCreditPackageDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(CreateCreditPackageDialog.class);
	
	protected Composite dialogArea;
	
	protected CTabFolder tabFolder;
	protected CTabItem productsTableTabItem;
	protected CTabItem newProductFormTabItem;
	
	protected CreditProductsPagedTableWidget productsTable;
	
	protected Composite newProductFormWidget;
	protected Text labelTxt;
	protected TrpNumberTextComposite nrOfCreditsTxt;
	protected Button shareableChk;
	
	private CreditPackagePropsAdminWidget propsEditor;
	//store final outcome here
	private TrpCreditPackage packageToCreate;
	
	/**
	 * Create dialog with current user as initial owner. Search button allows to find and set another package owner.
	 */
	public CreateCreditPackageDialog(Shell parent) {
		this(parent, null);
	}
	
	/**
	 * Create dialog with given user as initial owner. Search button is not shown.
	 */
	public CreateCreditPackageDialog(Shell parent, TrpUser selectedOwner) {
		super(parent);
		this.packageToCreate = new TrpCreditPackage();
		
		if(selectedOwner != null) {
			setOwner(selectedOwner);
		} else {
			//init owner field with logged-in user
			setOwner(Storage.getInstance().getUser());
		}
		//new package shall be enabled by default
		packageToCreate.setActive(true);
	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		dialogArea = (Composite) super.createDialogArea(parent);
		dialogArea.setLayout(new GridLayout(1, false));
		
		tabFolder = new CTabFolder(dialogArea, SWT.BORDER | SWT.FLAT);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		productsTableTabItem = new CTabItem(tabFolder, SWT.NONE);
		
		productsTable = new CreditProductsPagedTableWidget(tabFolder, SWT.NONE);
		productsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		productsTableTabItem.setText("Existing Products");
		productsTableTabItem.setControl(productsTable);
		
		newProductFormTabItem = new CTabItem(tabFolder, SWT.NONE);
		newProductFormWidget = createNewProductFormWidget(tabFolder, SWT.NONE);
		newProductFormTabItem.setText("Create Product");
		newProductFormTabItem.setControl(newProductFormWidget);

		Group pkgPropsGrp = new Group(dialogArea, SWT.BORDER);
		pkgPropsGrp.setText("Credit Package Properties");
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(pkgPropsGrp);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pkgPropsGrp);
		
		propsEditor = new CreditPackagePropsAdminWidget(pkgPropsGrp, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(propsEditor);
		propsEditor.setPackage(packageToCreate);
		
		tabFolder.setSelection(productsTableTabItem);
		
		dialogArea.pack();
		//init both tabs and not only the visible one. 
		//not resetting the tables to first page initially will lead to messed up pagination display.
		updateProductsTable(true);
		
		return dialogArea;
	}
	
	private void setOwner(TrpUser trpUser) {
		packageToCreate.setUserId(trpUser.getUserId());
		packageToCreate.setOrigUserId(trpUser.getUserId());
		packageToCreate.setUserName(trpUser.getUserName());
	}

	private Composite createNewProductFormWidget(Composite parent, int style) {
		Composite c = new Composite(parent, style);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setLayout(new GridLayout(2, false));
		Label labelLbl = new Label(c, SWT.NONE);
		labelLbl.setText("Label:");
		labelTxt = new Text(c, SWT.BORDER);
		labelTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label nrOfCreditsLbl = new Label(c, SWT.NONE);
		nrOfCreditsLbl.setText("Nr. of Credits:");
		nrOfCreditsTxt = new TrpNumberTextComposite(c, SWT.NONE);
		nrOfCreditsTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		//do not show decimal places here and use intValue when getting it from the slider composite
		nrOfCreditsTxt.setNumberFormat(new DecimalFormat("0"));
		nrOfCreditsTxt.setValue(100);
		nrOfCreditsTxt.setMaximum(1000000);
		
		new Label(c, SWT.NONE);
		shareableChk = new Button(c, SWT.CHECK);
		shareableChk.setText("Shareable");
		
		return c;
	}
	
	@Override
	protected void okPressed() {
		TrpCreditProduct product = null;
		if(tabFolder.getSelection().equals(productsTableTabItem)) {
			product = productsTable.getFirstSelected();
			if(product == null) {
				DialogUtil.showErrorMessageBox(this.getShell(), "Incomplete data", "No product is selected.");
				return;
			}			
		} else {
			String errorMsg = "";
			String label = labelTxt.getText();
			if(label == null || label.length() < 5) {
				errorMsg += "Enter a label with at least 5 letters\n";
			}
			Double value = nrOfCreditsTxt.getValue();
			if(value == null || value <= 0.0) {
				errorMsg += "Nr of credits must be a positive number\n";
			}
			boolean shareable = shareableChk.getSelection();
						
			if(!StringUtils.isEmpty(errorMsg)) {
				DialogUtil.showErrorMessageBox(this.getShell(), "Incomplete data", errorMsg);
				return;
			}
			
			//the propsEditor operates on a reference of packageToCreate so this update might not be necessary
			packageToCreate = propsEditor.getPackage();
			product = new TrpCreditProduct();
			product.setLabel(label);
			//value must be an int!
			product.setNrOfCredits(value.intValue());
			product.setShareable(shareable);
			product.setCreditType("" + JobType.recognition);
		}
		packageToCreate.setProduct(product);
		
		String msg =  "Are you sure you want to create the following credit package?\n";
		msg += "\nProduct name: " + product.getLabel();
		msg += "\nOwner: " + packageToCreate.getUserName();
		msg += "\nNr. of Credits: " + product.getNrOfCredits();
		msg += "\nShareable: " + product.getShareable();
		msg += "\nExpires: " + (packageToCreate.getExpirationDate() == null ? "Never" : packageToCreate.getExpirationDate());
		msg += "\nFree of Charge: " + (packageToCreate.isFreeOfCharge() ? "Yes" : "No");
		int answer = DialogUtil.showYesNoDialog(this.getParentShell(), "Please confirm your selection", msg);
		if(answer != SWT.YES) {
			logger.debug("Admin declined creating credit package: {}", packageToCreate);
			return;
		} else {
			super.okPressed();
		}
	}
	
	protected TrpCreditPackage getPackageToCreate() {
		return packageToCreate;
	}
	
	/**
	 * Refreshes the tables in the visible tab.
	 * 
	 * @param resetTablesToFirstPage
	 */
	protected void updateUI(boolean resetTablesToFirstPage) {
		CTabItem selection = tabFolder.getSelection();
		if(selection.equals(productsTableTabItem)) {
			updateProductsTable(resetTablesToFirstPage);
		} else {
			//clear form fields?
		}
	}
	
	protected void updateProductsTable(boolean resetTablesToFirstPage) {
		productsTable.refreshPage(resetTablesToFirstPage);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Credit Manager");
		newShell.setMinimumSize(640, 865);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(640, 865);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}
}
