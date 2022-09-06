package eu.transkribus.swt_gui.dialogs.la;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.la.LayoutAnalysisComposite;
import eu.transkribus.swt_gui.la.LayoutAnalysisDialog;

public class LayoutAnalysisDialogTest {

	public static void main(String[] args) {
		ApplicationWindow aw = new ApplicationWindow(null) {
			@Override
			protected Control createContents(Composite parent) {
				getShell().setSize(300, 200);
				SWTUtil.centerShell(getShell());
				
				LayoutAnalysisComposite.TEST = true;
				
				LayoutAnalysisDialog diag = new LayoutAnalysisDialog(getShell());
				diag.open();

				return parent;
			}
		};
		aw.setBlockOnOpen(true);
		aw.open();

		Display.getCurrent().dispose();
	}

}
