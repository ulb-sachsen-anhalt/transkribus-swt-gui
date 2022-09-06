package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrCurrentDocPagesSelector;

public class HandleTableConfDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(HandleTableConfDialog.class);
	
	CurrentTranscriptOrCurrentDocPagesSelector pagesSelector;
	Button keepBordersBtn;
	
	Set<Integer> pageIndices=null;
	boolean keepBorders=false;
	private boolean showOption = false;

	public HandleTableConfDialog(Shell parentShell, boolean showOptions) {
		super(parentShell);
		this.showOption = showOptions;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(400, getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
	}
	
	@Override
	protected void setShellStyle(int newShellStyle) {           
	    super.setShellStyle(SWT.CLOSE | SWT.MODELESS| SWT.BORDER | SWT.TITLE);
	    setBlockOnOpen(false);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Choose pages for this task");
	}
	
	@Override
	protected boolean isResizable() {
	    return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
		
		pagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(cont, SWT.NONE, true, true);
		pagesSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pagesSelector.setToolTipText("Current page will be ignored automatically!");
		
		Composite threshContainer = new Composite(cont, 0);
		threshContainer.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		threshContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (showOption) {
			keepBordersBtn = new Button(cont, SWT.CHECK);
			keepBordersBtn.setText("Keep the borders");
			keepBordersBtn.setToolTipText("The borders of the regions are taken over in the table cells!");
			keepBordersBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			keepBordersBtn.setSelection(keepBorders);
		}
		
		return cont;
	}
	
	@Override
	protected void okPressed() {
		try {
			pageIndices = pagesSelector.getSelectedPageIndices();
		} catch (IOException e) {
			pageIndices = null;
			DialogUtil.showErrorMessageBox(getShell(), "Invalid value", "Could not parse selected pages");
			logger.error("Could not parse page indices: "+e.getMessage(), e);
			return;
		}

		if (keepBordersBtn != null) {
			keepBorders = keepBordersBtn.getSelection();
		}
		
		logger.debug("pageIndices = "+pageIndices);
		logger.debug("keepBorders = "+keepBorders);
		
		super.okPressed();
	}

	public Set<Integer> getPageIndices() {
		return pageIndices;
	}

	public boolean isKeepBorders() {
		return keepBorders;
	}
}
