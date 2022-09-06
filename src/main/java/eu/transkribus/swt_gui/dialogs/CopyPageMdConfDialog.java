package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrCurrentDocPagesSelector;

public class CopyPageMdConfDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(CopyPageMdConfDialog.class);
	
	CurrentTranscriptOrCurrentDocPagesSelector pagesSelector;

	Set<Integer> pageIndices=null;
	boolean showCurrentPage = false;

	public CopyPageMdConfDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public CopyPageMdConfDialog(Shell parentShell, boolean showCurrentPage) {
		super(parentShell);
		this.showCurrentPage = showCurrentPage;
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
		newShell.setText("Page metadata configuration");
	}
	
	@Override
	protected boolean isResizable() {
	    return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
		
		pagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(cont, SWT.NONE, true, showCurrentPage);
		pagesSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		//pagesSelector.setToolTipText("For Copying - current page will be ignored automatically!");
		
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
		
		logger.debug("pageIndices = "+pageIndices);
		
		super.okPressed();
	}

	public Set<Integer> getPageIndices() {
		return pageIndices;
	}


}
