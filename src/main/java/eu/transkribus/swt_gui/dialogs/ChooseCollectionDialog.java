package eu.transkribus.swt_gui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.util.EnumUtils;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt_gui.collection_comboviewer.CollectionSelectorWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class ChooseCollectionDialog extends Dialog {
		
	Storage store = Storage.getInstance();
		
	TrpCollection initColl=null;
	
	TrpCollection selectedCollection=null;
	CollectionSelectorWidget collSelector;
	
	Button copyStatusBtn, copyAllBtn, copyIntoOneBtn;
	Combo statusCombo;
	boolean copyStatus = false;
	boolean copyAllTranscripts = false;
	boolean copyIntoOne = false;
	boolean showOptions = false;
	
	String title, editStatus;
		
	public ChooseCollectionDialog(Shell parentShell) {
		this(parentShell, "Choose a collection");
	}

	public ChooseCollectionDialog(Shell parentShell, String title) {
		this(parentShell, title, null);
	}
	
	public ChooseCollectionDialog(Shell parentShell, String title, TrpCollection initColl) {
		super(parentShell);
		this.title = title;
		this.initColl = initColl;
	}
	
	@Override protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(title);
	}
	
	@Override protected boolean isResizable() {
	    return true;
	}
	
	@Override protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, false));
				
		Label l = new Label(container, 0);
		l.setText("Selected collection: ");
		Fonts.setBoldFont(l);
		
		collSelector = new CollectionSelectorWidget(container, 0, false, null);
		collSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		collSelector.setSelectedCollection(initColl);
		
		if (showOptions) {
		
			copyStatusBtn = new Button(container, SWT.CHECK);
			copyStatusBtn.setText("Copy pages with status");
			copyStatusBtn.setToolTipText("If checked, only pages with the chosen status get copied to the new document!");
			copyStatusBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			copyStatusBtn.setSelection(false);
			
			copyStatusBtn.addSelectionListener(new SelectionAdapter() {
				@Override public void widgetSelected(SelectionEvent e) {
					copyStatus = copyStatusBtn.getSelection();
					if (copyStatus) {
						if (copyAllBtn != null) {
							copyAllBtn.setSelection(false);
							setCopyAllTranscripts(false);
						}
						statusCombo.setEnabled(true);
					}
					else {
						statusCombo.setEnabled(false);
					}
				}
			});
			
			statusCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			statusCombo.setToolTipText("Only pages with the chosen status get copied to the new document!");
			statusCombo.setItems(EnumUtils.stringsArray(EditStatus.class));
			statusCombo.setEnabled(false);
			//select the GT version as default
			statusCombo.select(EnumUtils.stringsArray(EditStatus.class).length-1);
			setEditStatus(statusCombo.getText());
			
			statusCombo.addSelectionListener(new SelectionAdapter() {
				@Override public void widgetSelected(SelectionEvent e) {
					setEditStatus(statusCombo.getText());
				}
			});
			
			if (Storage.getInstance().isAdminLoggedIn()) {
				copyAllBtn = new Button(container, SWT.CHECK);
				copyAllBtn.setText("Copy all available transcripts");
				copyAllBtn.setToolTipText("If checked, all transcripts of each single page get copied to the new document, otherwise only the latest");
				copyAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				copyAllBtn.setSelection(false);
				
				copyAllBtn.addSelectionListener(new SelectionAdapter() {
					@Override public void widgetSelected(SelectionEvent e) {
						copyAllTranscripts = copyAllBtn.getSelection();
						if (copyAllTranscripts) {
							copyStatusBtn.setSelection(false);
							setCopyStatus(false);
							statusCombo.setEnabled(false);
						}
					}
				});
			}
			
			copyIntoOneBtn = new Button(container, SWT.CHECK);
			copyIntoOneBtn.setText("Copy all selected into one doc");
			copyIntoOneBtn.setToolTipText("Copy all selected documents into one new document");
			copyIntoOneBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
			copyIntoOneBtn.setSelection(false);
			
			copyIntoOneBtn.addSelectionListener(new SelectionAdapter() {
				@Override public void widgetSelected(SelectionEvent e) {
					copyIntoOne = copyIntoOneBtn.getSelection();
				}
			});
		}
		
		return container;
	}
	
	@Override protected void okPressed() {
		selectedCollection = collSelector.getSelectedCollection();
		
		super.okPressed();
	}
	
	public TrpCollection getSelectedCollection() { return selectedCollection; }

	public boolean isCopyStatus() {
		return copyStatus;
	}

	public void setCopyStatus(boolean copyStatus) {
		this.copyStatus = copyStatus;
	}

	public boolean isCopyAllTranscripts() {
		return copyAllTranscripts;
	}

	public void setCopyAllTranscripts(boolean copyAllTranscripts) {
		this.copyAllTranscripts = copyAllTranscripts;
	}

	public boolean isCopyIntoOne() {
		return copyIntoOne;
	}

	public void setCopyIntoOne(boolean copyIntoOne) {
		this.copyIntoOne = copyIntoOne;
	}

	public String getEditStatus() {
		return editStatus;
	}

	public void setEditStatus(String editStatus) {
		this.editStatus = editStatus;
	}

	@Override protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override protected Point getInitialSize() {
		return new Point(550, 200);
	}
	
	private static Combo initComboWithLabel(Composite parent, String label, int comboStyle) {

		Label l = new Label(parent, SWT.LEFT);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		l.setText(label);

		Combo combo = new Combo(parent, comboStyle);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));

		return combo;
	}

	public void showOptions(boolean b) {
		this.showOptions = b;
		
	}

}
