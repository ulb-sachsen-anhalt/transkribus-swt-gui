package eu.transkribus.swt_gui.htr;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpHtr;

/**
 * @deprecated use ModelDetailsDialog / ModelDetailsWidget
 */
public class HtrDetailsDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(HtrDetailsDialog.class);
	
	private HtrDetailsWidget hdw;
	private TrpHtr htr;

	public HtrDetailsDialog(Shell parent, TrpHtr htr) {
		super(parent);
		this.htr = htr;
	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
		cont.setLayoutData(new GridData(GridData.FILL_BOTH));

		hdw = new HtrDetailsWidget(cont, SWT.VERTICAL);
		hdw.setLayoutData(new GridData(GridData.FILL_BOTH));
		hdw.setLayout(new GridLayout(1, false));

		updateDetails(htr);
		
		cont.layout();
		return cont;
	}

	public void setHtr(TrpHtr htr) {
		this.htr = htr;
		updateDetails(htr);
	}

	private void updateDetails(TrpHtr htr) {
		if (htr == null) {
			// don't clear the fields here.
			return;
		}
		hdw.checkForUnsavedChanges();
		this.getShell().setText("HTR '" + htr.getName() + "'");
		hdw.updateDetails(htr);
	}

	/**
	 * We don't need the button bar here
	 */
	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		GridLayout layout = (GridLayout) parent.getLayout();
		
		//I don't want to waste space for the button bar. Add a close button when someone complains
		final boolean showButtonBar = false;
		if(showButtonBar) {
			//default marginHeight is 15
			layout.marginHeight = 10;
			createButton(parent, CANCEL, "Nice!", true);
		} else {
			//the empty button bar still has a margin. Remove it.
			layout.marginHeight = 0;
		}
	}
	
	@Override
	public boolean close() {
		hdw.checkForUnsavedChanges();
		return super.close();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setMinimumSize(640, 640);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(640, 640);
	}
	
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = super.getDialogBoundsSettings();
		/* 
		 * On a user's machine with Windows 8.1 the dialog did not start using the value from getInitialSize() but roughly twice the width.
		 * This method returns null on Win 10 and GTK 2.0 and the issue could not be reproduced yet. 
		 */
		logger.debug("Retrieved dialog settings: {}", settings);
		return settings;
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}

	public boolean isDisposed() {
		return hdw == null || hdw.isDisposed();
	}
}
