package eu.transkribus.swt_gui.models;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.TrpModelMetadata;

public class ModelsDialog extends Dialog {
	
	ModelsComposite modelsComp;
	TrpModelMetadata selectedModel;
	
	private final String typeFilter;
	private final String providerFilter;
	
	private final boolean doubleClickSelectionEnabled;

	/**
	 * The dialog can be fixated to only show models of a specific provider, e.g. for selecting a base model for the training.
	 *  
	 * @param parentShell
	 * @param providerFilter fixates the HTR provider filter. Pass null to allow the use to filter by that.
	 */
	public ModelsDialog(Shell parentShell, boolean doubleClickSelectionEnabled, final String typeFilter, final String providerFilter) {
		super(parentShell);
		this.typeFilter = typeFilter;
		this.providerFilter = providerFilter;
		this.doubleClickSelectionEnabled = doubleClickSelectionEnabled;
	}

	public ModelsDialog(Shell parentShell, boolean doubleClickSelectionEnabled) {
		this(parentShell, doubleClickSelectionEnabled, null, null);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
	
		modelsComp = new ModelsComposite(cont, typeFilter, providerFilter, 0);
		modelsComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		modelsComp.mtw.getTableViewer().getTable().addKeyListener(new KeyAdapter() {			
			@Override
			public void keyPressed(KeyEvent e) {
				boolean isEnterKey = e.keyCode == SWT.CR
                        || e.keyCode == SWT.KEYPAD_CR;
				
				//don't close dialog on enter key pressed if an editable text field is focused because this sucks hard.
				//TODO show "Do you want to save" dialog instead
				boolean isTxtFieldFocused = modelsComp.mdw.descTxt.isFocusControl() 
						|| modelsComp.mdw.langEditor.getText().isFocusControl()
						|| modelsComp.mdw.nameTxt.isFocusControl();
				
				if (isEnterKey && !isTxtFieldFocused) {
					okPressed();
				}
			}
		});
		
		if(doubleClickSelectionEnabled) {
			modelsComp.mtw.getTableViewer().getTable().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					okPressed();
				}
			});
		}
		
		return cont;
	}
	
	@Override
	protected void okPressed() {
		modelsComp.mdw.checkForUnsavedChanges();
		selectedModel = modelsComp.getSelectedModel();
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed() {
		modelsComp.mdw.checkForUnsavedChanges();
		super.cancelPressed();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Choose a model");
		newShell.setMinimumSize(800, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1280, 900);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.APPLICATION_MODAL | SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
	
	public TrpModelMetadata getSelectedModel() {
		return selectedModel;
	}
}
