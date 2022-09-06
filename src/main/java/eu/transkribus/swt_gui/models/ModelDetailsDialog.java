package eu.transkribus.swt_gui.models;

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

import eu.transkribus.core.model.beans.TrpModelMetadata;

public class ModelDetailsDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(ModelDetailsDialog.class);
	
	private ModelDetailsWidget mdw;
	private TrpModelMetadata model;

	public ModelDetailsDialog(Shell parent, TrpModelMetadata model) {
		super(parent);
		this.model = model;
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

		mdw = new ModelDetailsWidget(cont, SWT.VERTICAL);
		mdw.setLayoutData(new GridData(GridData.FILL_BOTH));
		mdw.setLayout(new GridLayout(1, false));

		updateDetails(model);
		
		cont.layout();
		return cont;
	}

	public void setModel(TrpModelMetadata model) {
		this.model = model;
		updateDetails(model);
	}

	private void updateDetails(TrpModelMetadata model) {
		if (model == null) {
			// don't clear the fields here.
			return;
		}
		mdw.checkForUnsavedChanges();
		this.getShell().setText("Model '" + model.getName() + "'");
		mdw.updateDetails(model);
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
		mdw.checkForUnsavedChanges();
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
		return mdw == null || mdw.isDisposed();
	}
}
