package eu.transkribus.swt_gui.credits;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.swt_gui.pagination_tables.CreditTransactionsByPackagePagedTableWidget;

/**
 * @deprecated unused. Transactions of packages are now shown in CreditPackageDetailsDialog
 *
 */
public class CreditTransactionsByPackageDialog extends Dialog {
	
	protected CreditTransactionsByPackagePagedTableWidget transactionsTable;
	private TrpCreditPackage creditPackage;
		
	public CreditTransactionsByPackageDialog(Shell parent, TrpCreditPackage creditPackage) {
		super(parent);
		this.creditPackage = creditPackage;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		transactionsTable = new CreditTransactionsByPackagePagedTableWidget(dialogArea, SWT.NONE);
		transactionsTable.setLayoutData(new GridData(GridData.FILL_BOTH));	
		dialogArea.pack();
		updateUI(true);
		return dialogArea;
	}
	
	public void setPackage(TrpCreditPackage creditPackage) {
		this.creditPackage = creditPackage;
		transactionsTable.setPackageId(creditPackage.getPackageId());
	}
	
	/**
	 * Refreshes the table.
	 * 
	 * @param resetTablesToFirstPage
	 */
	protected void updateUI(boolean resetTablesToFirstPage) {
		transactionsTable.setPackageId(creditPackage.getPackageId());
		this.getShell().setText("Transactions of Package '" + creditPackage.getProduct().getLabel() + "'");
		transactionsTable.refreshPage(resetTablesToFirstPage);
	}
	
	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Transactions of Package");
		newShell.setMinimumSize(800, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1024, 768);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.APPLICATION_MODAL | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		//don't create buttons
	}
}
