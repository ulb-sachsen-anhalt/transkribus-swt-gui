package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.util.DocPagesSelector;

public class FixExif6RotationCoordsDialog extends Dialog {

	DocPagesSelector pagesSelector;
	Set<Integer> selectedPages;
	List<TrpPage> pages;
	
	public FixExif6RotationCoordsDialog(Shell parentShell, List<TrpPage> pages) {
		super(parentShell);
		
		this.pages = pages;
	}
	
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Fix coordinates for exif-6 orientation rotated images...");
		shell.setSize(600, 400);
		SWTUtil.centerShell(shell);
	}
	
	@Override protected boolean isResizable() {
	    return true;
	}
	
	@Override protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, true));
		
		pagesSelector = new DocPagesSelector(container, 0, pages);
		pagesSelector.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));

		return container;
	}
	
	@Override protected void okPressed() {
		try {
			selectedPages = pagesSelector.getSelectedPageIndices();
		} catch (IOException e) {
			selectedPages = null;
		}
		
		super.okPressed();
	}

	public Set<Integer> getSelectedPages() {
		return selectedPages;
	}

}

