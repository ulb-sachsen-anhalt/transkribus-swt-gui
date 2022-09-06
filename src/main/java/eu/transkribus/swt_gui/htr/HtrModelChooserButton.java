package eu.transkribus.swt_gui.htr;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;

/**
 * @deprecated use ModelChooserButton
 *
 */
public class HtrModelChooserButton extends Composite {
	
	Button button;
	Label label;
	
	/**
	 * Button opening the HTR models dialog for selecting a HTR or for display purposes.
	 * 
	 * @param parent
	 * @param doubleClickSelectionEnabled if true, then double-clicking a table element confirms and saves the selection and closes the dialog.
	 */
	public HtrModelChooserButton(Composite parent, final boolean doubleClickSelectionEnabled, final String providerFilter, String labelText) {
		super(parent, 0);
		
		boolean withLabel = !StringUtils.isEmpty(labelText);
		
		this.setLayout(SWTUtil.createGridLayout(withLabel ? 2 : 1, false, 0, 0));
		
		if (withLabel) {
			label = new Label(this, 0);
			label.setText(labelText);
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		}
		
		button = new Button(this, 0);
		button.setLayoutData(new GridData(GridData.FILL_BOTH));
		updateModelText();
		
		SWTUtil.onSelectionEvent(button, (e) -> {
			HtrModelsDialog diag = new HtrModelsDialog(getShell(), doubleClickSelectionEnabled, providerFilter);
			if (diag.open() == Dialog.OK) {
				if (providerFilter != null && providerFilter.equals(ModelUtil.PROVIDER_PYLAIA)) {
					int res = DialogUtil.showYesNoDialog(getShell(), "Do you really want to use a base model?",
							"Training with base models for PyLaia requires the exact same character set.\n"
							+ "Elsewise, the training will produce an error or a model that outputs only the characters from the base model and is unable to use a language model.\n"
							+ "Only use base models if you are really sure that the training data contains the exact same characters as the base model."
							);
					if (res != SWT.YES) {
						setModel(null);
					}
					else {
						setModel(diag.getSelectedHtr());
					}
				}
				else {
					setModel(diag.getSelectedHtr());
				}
			}
		});
	}
	
	private void updateModelText() {
		if (getModel() == null) {
			button.setText("Choose...");
		} else {
			button.setText(getModel().getName());
		}
	}
	
	public void setModel(TrpHtr htr) {
		button.setData(htr);
		updateModelText();
	}
	
	public TrpHtr getModel() {
		return (TrpHtr) button.getData();
	}
	
	public Button getButton() {
		return button;
	}
	
	public Label getLabel() {
		return label;
	}

	public void setText(String string) {
		button.setText(string);
	}
	
	public void setImage(Image image) {
		button.setImage(image);
	}

}
