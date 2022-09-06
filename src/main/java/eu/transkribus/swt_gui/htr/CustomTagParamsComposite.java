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

import eu.transkribus.core.model.beans.CustomTagTrainingParams;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.ComboInputDialog;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class CustomTagParamsComposite extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(CustomTagParamsComposite.class);
	
	/*
	 * short form and train properties are true and false as default -> can be used later on
	 */
	Button trainTags;
	//Button shortForm;
	Button trainProps;
	LabeledText tags4training;
	Button addTagBtn;
	
	public CustomTagParamsComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(SWTUtil.createGridLayout(5, false, 0, 0));
		
		trainTags = new Button(this, SWT.CHECK);
		trainTags.setText("Train Tags");
		trainTags.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		trainTags.setToolTipText("Train the annotated tags so that the model will generate the tags during recognition, most helpful for e.g. learn expansions of abbrevs or learn textstyles like italic, bold, ...");
		
		trainProps = new Button(this, SWT.CHECK);
		trainProps.setText("Include Properties");
		trainProps.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		trainProps.setToolTipText("Include the set properties in the training. E.g. bold, italic or expansion etc. can be trained if they were marked in the document.");
		
		/*
		 * short form means that each property value is replaced by a pair of special character -> text were not successful -> omit this
		 */
//		shortForm = new Button(this, SWT.CHECK);
//		shortForm.setText("Short Notation");
//		shortForm.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
//		shortForm.setToolTipText("Properties get replaced with labels instead of appended in long form, these labels are trained and then mapped to the former properties.");
		
		tags4training = new LabeledText(this, "Tag choice: ");
		tags4training.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tags4training.setToolTipText("List of tags that should be trained (space or comma separated)");
		
		addTagBtn = new Button(this, 0);
		addTagBtn.setImage(Images.ADD);
		addTagBtn.setToolTipText("Add a tag");
		
		SWTUtil.onSelectionEvent(addTagBtn, e -> {
			logger.debug("custom tag size: " + Storage.i().getCustomTagSpecsStrings().size());
			String[] items = Storage.i().getCustomTagSpecsStrings().toArray(new String[0]);
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
				tags4training.setText((tags4training.getText()+" "+d.getSelectedText()).trim());
			}
		});
		
		SWTUtil.onSelectionEvent(trainTags, e -> {
			updateUi();
		});
		updateUi();
	}
	
	public CustomTagTrainingParams getParams() {
		return new CustomTagTrainingParams(trainTags.getSelection(), CoreUtils.parseStringListOnCommasAndSpaces(tags4training.getText()), trainProps.getSelection(), false);
	}
	
	private void updateUi() {
		tags4training.setEnabled(trainTags.getSelection());
		addTagBtn.setEnabled(trainTags.getSelection());
	}

	public void setDefaults() {
		//shortForm.setSelection(false);
		trainTags.setSelection(false);
		trainProps.setSelection(false);
		//shortForm.setSelection(false);
		tags4training.setText("");
		updateUi();
	}

}
