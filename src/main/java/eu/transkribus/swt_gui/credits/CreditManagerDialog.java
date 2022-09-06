package eu.transkribus.swt_gui.credits;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.swt_gui.credits.admin.CreditAdminSashForm;

public class CreditManagerDialog extends Dialog {
	
	/**
	 * A Job's transactions can now be displayed from the original job overview Dialog. 
	 * The job transactions tab is therefore disabled here.
	 */
	private final static boolean SHOW_JOB_TRANSACTIONS_TAB = false;
	
	TrpCollection collection;
	
	protected Composite dialogArea;
	
	protected CTabFolder tabFolder;
	protected CTabItem collectionTabItem, historyTabItem;
	protected CTabItem jobTabItem;
	
	private boolean showAdminTab;
	protected CTabItem adminTabItem;
	private CreditAdminSashForm creditAdminWidget;
	private CreditPackageWidget collectionCreditWidget;
	private CreditHistoryWidget creditHistoryWidget;
	private JobTransactionSashForm jobTransactionWidget;

	public CreditManagerDialog(Shell parent, TrpCollection collection, boolean showAdminTab) {
		super(parent);
		this.showAdminTab = showAdminTab;
		this.collection = collection;
	}
	
	/**
	 * Dialog is now modal. Update on collection change to be tested yet.
	 */
	@SuppressWarnings("unused")
	private void setCollection(TrpCollection collection) {
		this.collection = collection;
		updateCreditsTabUI(true);
		updateHistoryTabUI(true);
	}

	public TrpCollection getCollection() {
		return collection;
	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		dialogArea = (Composite) super.createDialogArea(parent);

		tabFolder = new CTabFolder(dialogArea, SWT.BORDER | SWT.FLAT);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		historyTabItem = new CTabItem(tabFolder, SWT.NONE);
		creditHistoryWidget = createCreditHistoryWidget(tabFolder, SWT.NONE);
		historyTabItem.setText("Account History");
		historyTabItem.setControl(creditHistoryWidget);
		
		collectionTabItem = new CTabItem(tabFolder, SWT.NONE);
		collectionCreditWidget = createCollectionCreditWidget(tabFolder, SWT.NONE);
		collectionTabItem.setText("Credit Packages (Deprecated)");
		collectionTabItem.setControl(collectionCreditWidget);
		
		if(SHOW_JOB_TRANSACTIONS_TAB) {
			createJobTransactionTab(tabFolder);
		}
		
		if(showAdminTab) {
			adminTabItem = new CTabItem(tabFolder, SWT.NONE);
			creditAdminWidget = new CreditAdminSashForm(tabFolder, SWT.NONE);
			adminTabItem.setText("Admin");
			adminTabItem.setControl(creditAdminWidget);
			creditAdminWidget.refresh(true);
		}
		
		tabFolder.setSelection(historyTabItem);		
		dialogArea.pack();
		//init both tabs and not only the visible one. 
		//not resetting the tables to first page initially will lead to messed up pagination display.
		updateCreditsTabUI(true);
		updateHistoryTabUI(true);
		new CreditManagerListener(this);
		
		return dialogArea;
	}

	/**
	 * @deprecated see {@link #SHOW_JOB_TRANSACTIONS_TAB}
	 */
	private void createJobTransactionTab(CTabFolder tabFolder) {
		jobTabItem = new CTabItem(tabFolder, SWT.NONE);
		jobTransactionWidget = new JobTransactionSashForm(tabFolder, SWT.HORIZONTAL);
		jobTabItem.setText("Transactions");
		jobTabItem.setControl(jobTransactionWidget);
	}

	private CreditPackageWidget createCollectionCreditWidget(Composite parent, int style) {
		CreditPackageWidget sf = new CreditPackageWidget(parent, style);
		sf.setLayoutData(new GridData(GridData.FILL_BOTH));
		return sf;
	}
	
	private CreditHistoryWidget createCreditHistoryWidget(Composite parent, int style) {
		CreditHistoryWidget sf = new CreditHistoryWidget(parent, style);
		sf.setLayoutData(new GridData(GridData.FILL_BOTH));
		return sf;
	}
	
	protected CreditPackageWidget getCollectionCreditWidget() {
		return collectionCreditWidget;
	}
	
	/**
	 * Refreshes the tables in the visible tab.
	 * 
	 * @param resetTablesToFirstPage
	 */
	protected void updateUI(boolean resetTablesToFirstPage) {
		CTabItem selection = tabFolder.getSelection();
		if(selection.equals(collectionTabItem)) {
			updateCreditsTabUI(resetTablesToFirstPage);
		} else if(selection.equals(historyTabItem)) {
			updateHistoryTabUI(resetTablesToFirstPage);
		} else {
			updateJobsTabUI(resetTablesToFirstPage);
		}
	}
	
	/**
	 * @deprecated see {@link #SHOW_JOB_TRANSACTIONS_TAB}
	 */
	protected void updateJobsTabUI(boolean resetTablesToFirstPage) {
		if(jobTransactionWidget != null) {
			jobTransactionWidget.updateUI(resetTablesToFirstPage);
		}
	}
	
	protected void updateCreditsTabUI(boolean resetTablesToFirstPage) {
		collectionCreditWidget.setCollection(this.getCollection(), resetTablesToFirstPage);
	}
	
	protected void updateHistoryTabUI(boolean resetTablesToFirstPage) {
		creditHistoryWidget.setCollection(this.getCollection(), resetTablesToFirstPage);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Credit Manager");
		newShell.setMinimumSize(1024, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1200, 768);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.APPLICATION_MODAL | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		//only show OK button (close dialog on press), labeled as close. defaultButton = false => hitting enter will not trigger close
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, false);
	}
}
