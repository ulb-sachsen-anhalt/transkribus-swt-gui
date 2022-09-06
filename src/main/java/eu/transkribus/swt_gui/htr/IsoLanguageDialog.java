package eu.transkribus.swt_gui.htr;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.swt.util.SWTUtil;

public class IsoLanguageDialog extends Dialog {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IsoLanguageDialog.class);
	
	IsoLanguageTable t;
	String languageString;
	
	public IsoLanguageDialog(Shell parent, String languageString) {
		super(parent);
		
		this.languageString = languageString;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
		cont.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		t = new IsoLanguageTable(cont, 0, languageString);
		t.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		cont.layout();
		return cont;
	}
	
	@Override
	public boolean close() {
		this.languageString = t.getLanguageString();
		return super.close();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setMinimumSize(640, 500);
		SWTUtil.centerShell(newShell);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(640, 500);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}

	public String getLanguageString() {
		return languageString;
	}	

}
