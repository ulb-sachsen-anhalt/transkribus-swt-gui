package eu.transkribus.swt_gui.htr;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.util.IsoLangUtils;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;

public class IsoLanguageEditComposite extends Composite {
	String languageString="";
	Text text;
	Button editBtn;
	
	public IsoLanguageEditComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		
		text = new Text(this, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		editBtn = new Button(this, 0);
		editBtn.setImage(Images.PENCIL);
		editBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		SWTUtil.onSelectionEvent(editBtn, e -> {
			IsoLanguageDialog d = new IsoLanguageDialog(getShell(), languageString);
			if (d.open() == IDialogConstants.OK_ID) {
				setLanguageString(d.getLanguageString());
			}
		});
	}
	
	public void setLanguageString(String languageString) {
		this.languageString = languageString;
		text.setText(IsoLangUtils.DEFAULT_RESOLVER.getLanguageWithResolvedIsoCodes(this.languageString));
	}
	
	public Text getText() {
		return text;
	}
	
	public String getLanguageString() {
		return this.languageString;
	}
	
	@Override
	public void setEnabled(boolean enable) {
		text.setEnabled(enable);
		editBtn.setEnabled(enable);
	}
}
