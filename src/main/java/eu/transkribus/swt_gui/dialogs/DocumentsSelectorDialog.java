package eu.transkribus.swt_gui.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import eu.transkribus.core.model.beans.DocumentSelectionDescriptor;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.swt_gui.util.DocumentsSelector;

public class DocumentsSelectorDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(DocumentsSelectorDialog.class);

	DocumentsSelector ds;
	List<TrpDocMetadata> docs;
	List<DocSelection> previousSelection;
	
	List<DocumentSelectionDescriptor> checkedDescriptors;
	List<DocSelection> checkedDocSelections;
	Map<TrpDocMetadata, DocSelection> checkedDocSelectionDetails;
	
	String title;
	private boolean withPagesSelector;
	private boolean isForTextRecognition;
	
	public DocumentsSelectorDialog(Shell parent, final String title, List<TrpDocMetadata> docs) {
		this(parent, title, docs, false);
	}

	public DocumentsSelectorDialog(Shell parent, final String title, List<TrpDocMetadata> docs, boolean withPagesSelector) {
		this(parent, title, docs, withPagesSelector, null);
	}
	
	public DocumentsSelectorDialog(Shell parent, final String title, List<TrpDocMetadata> docs, boolean withPagesSelector, boolean isForTextRecognition) {
		this(parent, title, docs, withPagesSelector, null, isForTextRecognition);
	}
	
	public DocumentsSelectorDialog(Shell parent, final String title, List<TrpDocMetadata> docs, boolean withPagesSelector, List<DocSelection> previousSelection) {
		this(parent, title, docs, withPagesSelector, null, false);
	}	
	
	public DocumentsSelectorDialog(Shell parent, final String title, List<TrpDocMetadata> docs, boolean withPagesSelector, List<DocSelection> previousSelection, boolean isForTextRecognition) {
		super(parent);
		this.title = title;
		this.docs = docs;
		this.withPagesSelector = withPagesSelector;
		this.previousSelection = previousSelection;
		this.isForTextRecognition = isForTextRecognition;
		this.checkedDocSelectionDetails = new HashMap<>();
	}
	
//	public void setWithPagesSelector(boolean withPagesSelector) {
//		this.withPagesSelector = withPagesSelector;
//	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		
		ds = new DocumentsSelector(container, 0, false, true, withPagesSelector, isForTextRecognition);
		ds.setLayoutData(new GridData(GridData.FILL_BOTH));
		ds.setDataList(docs);
		if (previousSelection!=null) {
			ds.setPreviousSelection(previousSelection);
		}
		
		return container;
	}
	
	@Override
	protected void okPressed() {
		checkedDescriptors = ds.getCheckedDocumentDescriptors();
		checkedDocSelections = ds.getCheckedDocSelections();
		checkedDocSelectionDetails = ds.getCheckedDocumentsDetails();
		super.okPressed();
	}
	
	public List<DocSelection> getCheckedDocSelections() {
		return checkedDocSelections;
	}
	
	public List<TrpDocMetadata> getCheckedDocs() {
		return new ArrayList<>(checkedDocSelectionDetails.keySet());
	}
	
	public Map<TrpDocMetadata, DocSelection> getCheckedDocSelectionDetails() {
		return checkedDocSelectionDetails;
	}
	
	/*
	 * get the documentSelectionDescriptor list including the page descriptors
	 */
	public List<DocumentSelectionDescriptor> getCheckedDocumentDescriptors(){
		return checkedDescriptors;
		
//		try{
//			List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
//			for (TrpDocMetadata d : getCheckedDocs()) {
//				DocumentSelectionDescriptor currDescr = new DocumentSelectionDescriptor(d.getDocId());
//				
//				/*
//				 * if no PageDescriptor given the job will make all pages - hopefully!
//				 */
////				TrpDoc currDoc;
////	
////					currDoc = Storage.getInstance().getConnection().getTrpDoc(Storage.getInstance().getCollId(), d.getDocId(), 1);
////				
////				List<TrpPage> currPages = currDoc.getPages();
////				for (int i = 0; i < d.getNrOfPages(); i++){		
////					currDescr.addPage(new PageDescriptor(currPages.get(i).getPageId()));
////				}
//				dsds.add(currDescr);	
//			}
//			return dsds;
//		} catch (ClientErrorException | IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
	}
	
	@Override protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
//		newShell.setMinimumSize(400, 400);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 800);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}

}

