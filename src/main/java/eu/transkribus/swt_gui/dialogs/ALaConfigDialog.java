package eu.transkribus.swt_gui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.rest.ParameterMap;
import eu.transkribus.swt.util.DialogUtil;

public abstract class ALaConfigDialog extends Dialog {

	protected ParameterMap parameters;

	public ALaConfigDialog(Shell parent, ParameterMap parameters) {
		super(parent);
		if(parameters == null) {
			this.parameters = new ParameterMap();
		} else {
			this.parameters = parameters;
		}
	}
	
	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}
	
	@Override
	protected void okPressed() {
		try {
			storeSelectionInParameterMap();
			super.okPressed();
		}
		catch (IllegalArgumentException e) {
			DialogUtil.showErrorMessageBox(getShell(), "Invalid parameter settings", "You have to specify a trained model using the model chooser button!");
		}
		catch (Exception e) {
			DialogUtil.showErrorMessageBox(getShell(), "Unexcepted error", "An unexpected error occurred: "+e.getMessage());
		}
	}
	
	/**
	 * Read out dialog-specific settings and store them in the parameter map using the respective parameter names used in the backend
	 */
	protected abstract void storeSelectionInParameterMap();
	/**
	 * Set dialog-specific fields according to the values given in the parameter map 
	 */
	protected abstract void applyParameterMapToDialog();
	
	public ParameterMap getParameters() {
		return parameters;
	}

	public abstract String getConfigInfoString();
}
