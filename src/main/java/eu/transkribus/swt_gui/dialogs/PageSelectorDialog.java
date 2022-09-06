package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import eu.transkribus.swt_gui.util.CurrentTranscriptOrCurrentDocPagesSelector;

public class PageSelectorDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(PageSelectorDialog.class);
	
	CurrentTranscriptOrCurrentDocPagesSelector pagesSelector;

	Button rtlBtn, sortRegions;
	public boolean rtl, regions;
	private boolean showOptions = true;
	
	Set<Integer> pageIndices=null;

	public PageSelectorDialog(Shell parentShell) {
		this(parentShell, true);
	}
	
	public PageSelectorDialog(Shell parentShell, boolean showOptions) {
		super(parentShell);
		this.showOptions = showOptions;
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
		
		pagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(cont, SWT.NONE, true,true);
		pagesSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (showOptions) {
			rtlBtn = new Button(cont, SWT.CHECK);
			rtlBtn.setText("RTL: Sort from right to left");
			sortRegions = new Button(cont, SWT.CHECK);
			sortRegions.setText("Sort regions...");
			
			rtlBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected( SelectionEvent event )
				{
					setRtl(rtlBtn.getSelection());
				}
			});
			
			sortRegions.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected( SelectionEvent event )
				{
					setRegions(sortRegions.getSelection());
				}
			});
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

		logger.debug("pageIndices = "+pageIndices);

		super.okPressed();
	}

	public Set<Integer> getPageIndices() {
		return pageIndices;
	}

	public boolean getRtl() {
		return rtl;
	}
	
	public void setRtl(boolean rtl) {
		this.rtl = rtl;
	}

	public boolean isDoRegions() {
		return regions;
	}

	public void setRegions(boolean regions) {
		this.regions = regions;
	}

}
