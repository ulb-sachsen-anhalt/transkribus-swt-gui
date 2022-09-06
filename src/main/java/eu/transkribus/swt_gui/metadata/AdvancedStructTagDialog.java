package eu.transkribus.swt_gui.metadata;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.ComboInputDialog;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrCurrentDocPagesSelector;

public class AdvancedStructTagDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(AdvancedStructTagDialog.class);
	
	IStorageListener storageListener;
	Storage store = Storage.getInstance();
	
	CurrentTranscriptOrCurrentDocPagesSelector pagesSelector;
	Set<Integer> pageIndices=null;
	
	final String INITIAL_TYPE = "no_type_selected";
	
	Button annotateTypeBtn;
	Button deleteTypeBtn;
	Button renameTypeBtn;
	Button addTagBtn;
	String selectedType = INITIAL_TYPE;
	
	String structureType;
	
	boolean annotate, delete, rename;

	public AdvancedStructTagDialog(Shell parent, String currStructureType) {
		super(parent);
		parent.setText("Advanced options for structural metadata");
		this.structureType = currStructureType;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true, 1, 1));
		container.setLayout(new GridLayout(1, false));
		
		pagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(container, SWT.NONE, true,true);
		pagesSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		annotateTypeBtn = new Button(container, SWT.CHECK);
		annotateTypeBtn.setText("Annotate empty regions with '" + structureType +"'");
		annotateTypeBtn.setToolTipText("If checked, all regions without structure type will be assigned with structure type: '" + structureType + "'");
		annotateTypeBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		SWTUtil.onSelectionEvent(annotateTypeBtn, e -> {
			if (annotateTypeBtn.getSelection()) {
				deleteTypeBtn.setSelection(!annotateTypeBtn.getSelection());
				renameTypeBtn.setSelection(!annotateTypeBtn.getSelection());
			}
			setAnnotate(annotateTypeBtn.getSelection());
		});
		
		deleteTypeBtn = new Button(container, SWT.CHECK);
		deleteTypeBtn.setText("Delete (from document!) all these structure types: '" + structureType +"'");
		deleteTypeBtn.setToolTipText("If checked, structure type '" + structureType + "' of all regions will be deleted: ");
		deleteTypeBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		SWTUtil.onSelectionEvent(deleteTypeBtn, e -> {
			if (deleteTypeBtn.getSelection()) {
				annotateTypeBtn.setSelection(!deleteTypeBtn.getSelection());
				renameTypeBtn.setSelection(!deleteTypeBtn.getSelection());
			}
			setDelete(deleteTypeBtn.getSelection());
		});
		
		String renameTextTemplate = "Rename structure type '" + structureType + "' with 'platzhalter2'";
		String renameText = renameTextTemplate.replace("platzhalter2", selectedType);
		renameTypeBtn = new Button(container, SWT.CHECK);
		renameTypeBtn.setText(renameText);
		renameTypeBtn.setToolTipText("If checked, all region structure types will be renamed with the new type: " + selectedType);
		renameTypeBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		SWTUtil.onSelectionEvent(renameTypeBtn, e -> {
			if (renameTypeBtn.getSelection()) {
				annotateTypeBtn.setSelection(!renameTypeBtn.getSelection());
				deleteTypeBtn.setSelection(!renameTypeBtn.getSelection());
			}
			setRename(renameTypeBtn.getSelection());
		});
		
		LabeledText selectedLabel = new LabeledText(container, "Select structure type: ", false, SWT.READ_ONLY);
		selectedLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		selectedLabel.setText(selectedType);
		selectedLabel.setToolTipText("Select structure type for renaming");
		
		addTagBtn = new Button(container, 0);
		addTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		addTagBtn.setImage(Images.ADD);
		addTagBtn.setToolTipText("Select a structure type");
		SWTUtil.onSelectionEvent(addTagBtn, e -> {
			String[] items = Storage.getInstance().getStructCustomTagSpecsTypeStrings().toArray(new String[0]);
			ComboInputDialog d = new ComboInputDialog(this.getShell(), "Choose a tag: ", items, SWT.DROP_DOWN, true);
			if (d.open() == d.OK) {
				String tmp = d.getSelectedText().trim();
				setSelectedType(tmp);
				addTagBtn.setText(tmp);
				renameTypeBtn.setText(renameTextTemplate.replace("platzhalter2", tmp));
				selectedLabel.setText(tmp);
			}
		});

		return container;
	}
	
	@Override protected void okPressed() {
		
		try {
			pageIndices = pagesSelector.getSelectedPageIndices();
		} catch (IOException e) {
			pageIndices = null;
			DialogUtil.showErrorMessageBox(getShell(), "Invalid value", "Could not parse selected pages");
			logger.error("Could not parse page indices: "+e.getMessage(), e);
			return;
		}
		finally {
			super.okPressed();
		}
		
	}

	public boolean isAnnotate() {
		return annotate;
	}

	public boolean isDelete() {
		return delete;
	}

	public boolean isRename() {
		return rename;
	}

	public String getSelectedType() {
		if (selectedType.contentEquals(INITIAL_TYPE)) {
			return null;
		}
		return selectedType;
	}

	public void setSelectedType(String selectedType) {
		this.selectedType = selectedType;
	}

	public void setAnnotate(boolean annotate) {
		this.annotate = annotate;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public void setRename(boolean rename) {
		this.rename = rename;
	}
	
	public Set<Integer> getPageIndices() {
		return pageIndices;
	}

}
