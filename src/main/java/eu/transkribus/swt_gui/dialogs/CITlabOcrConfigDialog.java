package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrDocPagesOrCollectionSelector;

public class CITlabOcrConfigDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(OcrDialog.class);
	
	private boolean docsSelected = false;
	private List<DocSelection> selectedDocSelections;
	private Map<TrpDocMetadata, DocSelection> selectedDocSelectionDetails = new HashMap<>();
	private CurrentTranscriptOrDocPagesOrCollectionSelector dps;
	
	private Storage store = Storage.getInstance();
	private String pages;

	
	public CITlabOcrConfigDialog(Shell parent) {
		super(parent);
	}
    
	public void setVisible() {
		if(super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(3, false));
		
		dps = new CurrentTranscriptOrDocPagesOrCollectionSelector(cont, SWT.NONE, true, true, true);		
		dps.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 3, 1));

		return cont;
	}
	
	@Override
	protected void okPressed() {
		
		if(dps.isCurrentTranscript()) {
			pages = ""+store.getPage().getPageNr();
		} else if(!dps.isDocsSelection()) {
			pages = dps.getPagesStr();
			if(pages == null || pages.isEmpty()) {
				DialogUtil.showErrorMessageBox(this.getParentShell(), "Error", "Please specify pages for recognition.");
				return;
			}
			
			try {
				CoreUtils.parseRangeListStr(pages, store.getDoc().getNPages());
			} catch (IOException e) {
				DialogUtil.showErrorMessageBox(this.getParentShell(), "Error", "Page selection is invalid.");
				return;
			}
		} else {
			docsSelected = dps.isDocsSelection();
//			selectedDocDescriptors = dps.getDocumentsSelected();
			selectedDocSelections = dps.getDocSelections();
			selectedDocSelectionDetails = dps.getDocSelectionDetails();
			if(CollectionUtils.isEmpty(selectedDocSelections)) {
				DialogUtil.showErrorMessageBox(this.getParentShell(), "Error", "No documents selected for recognition.");
				return;
			}
		}
		
		super.okPressed();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Optical Character Recognition");
		newShell.setMinimumSize(600, 300);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 300);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
	
//	public OcrConfig getConfig() {
//		return this.config;
//	}
	
	public boolean isDocsSelection(){
		return docsSelected;
	}
	
	public List<DocSelection> getDocs() {
		return selectedDocSelections;
	}
	
	public Map<TrpDocMetadata, DocSelection> getDocSelectionDetails() {
		return selectedDocSelectionDetails;
	}
	
	public String getPages() {
		return this.pages;
	}
}
