package eu.transkribus.swt_gui.credits;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.swt_gui.pagination_tables.CreditTransactionsByJobPagedTableWidget;

public class CreditTransactionsByJobDialog extends Dialog {
	
	protected CreditTransactionsByJobPagedTableWidget transactionsTable;
	private TrpJobStatus job;
		
	public CreditTransactionsByJobDialog(Shell parent, TrpJobStatus job) {
		super(parent);
		this.job = job;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		transactionsTable = new CreditTransactionsByJobPagedTableWidget(dialogArea, SWT.NONE);
		transactionsTable.setLayoutData(new GridData(GridData.FILL_BOTH));	
		dialogArea.pack();
		updateUI(true);
		return dialogArea;
	}
	
	public void setJob(TrpJobStatus job) {
		this.job = job;
		transactionsTable.setJobId(job.getJobIdAsInt());
	}
	
	/**
	 * Refreshes the table.
	 * 
	 * @param resetTablesToFirstPage
	 */
	protected void updateUI(boolean resetTablesToFirstPage) {
		transactionsTable.setJobId(job.getJobIdAsInt());
		this.getShell().setText("Transactions of " + job.getType());
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
		newShell.setText("Transactions of Job");
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
