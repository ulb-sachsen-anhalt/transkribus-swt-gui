package eu.transkribus.swt_gui.metadata;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * for testing purposes
 * @author jkloe
 *
 */
public class TaggingWidgetDialog extends Dialog {

	TaggingWidget taggingWidget;

	public TaggingWidgetDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
	
		taggingWidget = new TaggingWidget(cont, 0);
		taggingWidget.setLayoutData(new GridData(GridData.FILL_BOTH));
				
		return cont;
	}
	
	@Override
	protected void okPressed() {
		// do sth. on ok pressed... needed?
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Tagging widget");
		newShell.setMinimumSize(800, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1024, 768);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
	
}
