package eu.transkribus.swt_gui.metadata;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class StructTagConfDialog extends Dialog {
	
	StructTagConfWidget tagConfWidget;

	public StructTagConfDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
	
		tagConfWidget = new StructTagConfWidget(cont, 0);
		tagConfWidget.setLayoutData(new GridData(GridData.FILL_BOTH));
				
		return cont;
	}
	
	@Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "OK", true);
//        createButton(parent, IDialogConstants.CANCEL_ID,
//                IDialogConstants.CANCEL_LABEL, false);
    }
	
	@Override
	protected void okPressed() {		
		super.okPressed();
		// do sth. else on ok pressed... needed?
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Tag configuration");
		newShell.setMinimumSize(400, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 800);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL);
		// setBlockOnOpen(false);
	}
	
}
