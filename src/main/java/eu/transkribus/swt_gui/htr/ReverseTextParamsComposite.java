package eu.transkribus.swt_gui.htr;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.ReverseTextParams;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.ComboInputDialog;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class ReverseTextParamsComposite extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(ReverseTextParamsComposite.class);
	
	Button reverseText, excludeDigits;
	LabeledText tagExceptions;
	Button addTagBtn;
	
	public ReverseTextParamsComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(SWTUtil.createGridLayout(4, false, 0, 0));
		
		reverseText = new Button(this, SWT.CHECK);
		reverseText.setText("Reverse Text");
		reverseText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		reverseText.setToolTipText("Reverse the text during training, e.g. if text was written right-to-left and transcribed left-to-right");
		
		excludeDigits = new Button(this, SWT.CHECK);
		excludeDigits.setText("Exclude digits");
		excludeDigits.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		excludeDigits.setToolTipText("Exclude digits when reversing text");
		excludeDigits.setSelection(true);
		
		tagExceptions = new LabeledText(this, "Tag exceptions for reversion: ");
		tagExceptions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tagExceptions.setToolTipText("List of tags that indicate that a text should be excluded from reversion (space or comma separated)");
		
		addTagBtn = new Button(this, 0);
		addTagBtn.setImage(Images.ADD);
		addTagBtn.setToolTipText("Add a tag");
		SWTUtil.onSelectionEvent(addTagBtn, e -> {
			String[] items = Storage.i().getStructCustomTagSpecsTypeStrings().toArray(new String[0]);
			ComboInputDialog d = new ComboInputDialog(getShell(), "Specify a tag: ", items, SWT.DROP_DOWN, true);
			d.setValidator(new IInputValidator() {
				@Override public String isValid(String arg) {
					if (StringUtils.containsWhitespace(arg)) {
						return "No spaces allowed in structure types!";
					}
					return null;
				}
			});
			if (d.open() == Dialog.OK) {
				tagExceptions.setText((tagExceptions.getText()+" "+d.getSelectedText()).trim());
			}
		});
		
		SWTUtil.onSelectionEvent(reverseText, e -> {
			updateUi();
		});
		updateUi();
	}
	
	public ReverseTextParams getParams() {
		return new ReverseTextParams(reverseText.getSelection(), excludeDigits.getSelection(), CoreUtils.parseStringListOnCommasAndSpaces(tagExceptions.getText()));
	}
	
	private void updateUi() {
		excludeDigits.setEnabled(reverseText.getSelection());
		tagExceptions.setEnabled(reverseText.getSelection());
		addTagBtn.setEnabled(reverseText.getSelection());
	}

	public void setDefaults() {
		reverseText.setSelection(false);
		excludeDigits.setSelection(true);
		tagExceptions.setText("");
		updateUi();
	}

}
