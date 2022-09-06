package eu.transkribus.swt_gui.models;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;

public class ModelChooserButton extends Composite {
	
	Button baseModelBtn;
	Label label;
	boolean doubleClickSelectionEnabled;
	String typeFilter, providerFilter;
	
	
	public ModelChooserButton(Composite parent, final boolean doubleClickSelectionEnabled, final String typeFilter, final String providerFilter) {
		this(parent, doubleClickSelectionEnabled, typeFilter, providerFilter, null);
	}
	
	/**
	 * Button opening the HTR models dialog for selecting a HTR or for display purposes.
	 * 
	 * @param parent
	 * @param doubleClickSelectionEnabled if true, then double-clicking a table element confirms and saves the selection and closes the dialog.
	 */
	public ModelChooserButton(Composite parent, final boolean doubleClickSelectionEnabled, final String typeFilter, final String providerFilter, String labelText) {
		super(parent, 0);
		
		this.doubleClickSelectionEnabled = doubleClickSelectionEnabled;
		this.typeFilter = typeFilter;
		this.providerFilter = providerFilter;
		
		boolean withLabel = !StringUtils.isEmpty(labelText);
		
		
		this.setLayout(SWTUtil.createGridLayout(withLabel ? 2 : 1, false, 0, 0));
		
		if (withLabel) {
			label = new Label(this, 0);
			label.setText(labelText);
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		}
		
		baseModelBtn = new Button(this, 0);
		baseModelBtn.setLayoutData(new GridData(GridData.FILL_BOTH));
		updateModelText();
		
		SWTUtil.onSelectionEvent(baseModelBtn, (e) -> {
			openModelChooserDialog();
		});
	}
	
	/**
	 * Button opening the HTR models dialog for selecting a HTR or for display purposes.
	 * 
	 * @param parent
	 * @param doubleClickSelectionEnabled if true, then double-clicking a table element confirms and saves the selection and closes the dialog.
	 */
	public ModelChooserButton(Composite parent, final boolean doubleClickSelectionEnabled) {
		this(parent, doubleClickSelectionEnabled, null, null);
	}
	
	public void openModelChooserDialog() {
		ModelsDialog diag = new ModelsDialog(getShell(), doubleClickSelectionEnabled, typeFilter, providerFilter);
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
					setModel(diag.getSelectedModel());
				}
			}
			else {
				setModel(diag.getSelectedModel());
			}
		}
	}
	

	
	private void updateModelText() {
		if (getModel() == null) {
			baseModelBtn.setText("Choose...");
		} else {
			baseModelBtn.setText(getModel().getName());
		}
	}
	
	public void setModel(TrpModelMetadata model) {
		baseModelBtn.setData(model);
		updateModelText();
		onModelSelectionChanged(getModel());
	}
	
	protected void onModelSelectionChanged(TrpModelMetadata selectedModel) {
	}

	public TrpModelMetadata getModel() {
		return (TrpModelMetadata) baseModelBtn.getData();
	}
	
	public Button getButton() {
		return baseModelBtn;
	}
	
	public Label getLabel() {
		return label;
	}

	public void setText(String string) {
		baseModelBtn.setText(string);
	}
	
	public void setImage(Image image) {
		baseModelBtn.setImage(image);
	}

}
