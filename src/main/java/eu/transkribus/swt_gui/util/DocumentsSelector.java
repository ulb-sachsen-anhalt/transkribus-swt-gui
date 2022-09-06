package eu.transkribus.swt_gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.ClientErrorException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.DocumentSelectionDescriptor;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.mytableviewer.ColumnConfig;
import eu.transkribus.swt.util.APreviewListViewer;
import eu.transkribus.swt.util.DefaultTableColumnViewerSorter;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class DocumentsSelector extends APreviewListViewer<TrpDocMetadata> {
	private final static Logger logger = LoggerFactory.getLogger(DocumentsSelector.class);
	
	public static final String ID_COL = "ID";
	public static final String TITLE_COL = "Title";
	public static final String N_PAGES_COL = "N-Pages";
	public static final String PAGES_COL = "Pages";
	
	public static final ColumnConfig[] COLS = new ColumnConfig[] {
		new ColumnConfig(ID_COL, 65, true, DefaultTableColumnViewerSorter.DESC),
		new ColumnConfig(TITLE_COL, 150, false, DefaultTableColumnViewerSorter.ASC),
		new ColumnConfig(N_PAGES_COL, 75, false, DefaultTableColumnViewerSorter.ASC),
	};
	
	public static final ColumnConfig[] COLS_WITH_PAGES = new ColumnConfig[] {
			new ColumnConfig(ID_COL, 65, true, DefaultTableColumnViewerSorter.DESC),
			new ColumnConfig(TITLE_COL, 150, false, DefaultTableColumnViewerSorter.ASC),
			new ColumnConfig(N_PAGES_COL, 75, false, DefaultTableColumnViewerSorter.ASC),
			new ColumnConfig(PAGES_COL, 75, false, DefaultTableColumnViewerSorter.ASC),
		};	
	
	boolean withPagesSelector;
	Map<Integer, String> pagesStrs=new HashMap<>(); // holds specific pages string for a document-ID; if null, all pages are selected!
	TranscriptFilterButtonWidget filterBtn;
	Label infoLabel;
	Label fromLbl, toLbl;
	Text fromTxt, toTxt;
	Button apply;
		
	public DocumentsSelector(Composite parent, int style, boolean showUpDownBtns, boolean withCheckboxes) {
		this(parent, style, showUpDownBtns, withCheckboxes, false);
	}
	
	public DocumentsSelector(Composite parent, int style, boolean showUpDownBtns, boolean withCheckboxes, boolean withPagesSelector) {
		this(parent, style, showUpDownBtns, withCheckboxes, false, false);
	}

	public DocumentsSelector(Composite parent, int style, boolean showUpDownBtns, boolean withCheckboxes, boolean withPagesSelector, boolean isForTextRecognition) {
		super(parent, style, withPagesSelector ? COLS_WITH_PAGES : COLS, null, showUpDownBtns, withCheckboxes, false);
		
		if (isForTextRecognition) {
			infoLabel = new Label(this, 0);
			infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			
			filterBtn = new TranscriptFilterButtonWidget(this, 0);
			filterBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			filterBtn.addSelectionListener(e -> {
				modifyPageStringsForAllCheckedDocs();
			});
			
//			Button avoidGtStatus = new Button(this, SWT.PUSH);
//			avoidGtStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//			avoidGtStatus.setText("Exclude GT,FINAL,DONE");
//			avoidGtStatus.setToolTipText("Calculates new page strings for all CHECKED! documents. Unchecked documents from a previous step are ignored!");
//			avoidGtStatus.setForeground(new Color(Display.getCurrent(),0, 100, 0));
//			avoidGtStatus.setSelection(false);
//			avoidGtStatus.addSelectionListener(new SelectionAdapter() {
//				public void widgetSelected(SelectionEvent event) {
//						
//						modifyPageStringsForAllCheckedDocs(false);
//						
//					}
//			});
//			
//			Button withoutTextBtn = new Button(this, SWT.PUSH);
//			withoutTextBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//			withoutTextBtn.setText("Get pages without (HTR) text");
//			withoutTextBtn.setToolTipText("Calculates new page strings for all CHECKED! documents. Unchecked documents from a previous step are ignored!");
//			withoutTextBtn.setForeground(new Color(Display.getCurrent(),0, 100, 0));
//			withoutTextBtn.setSelection(false);
//			withoutTextBtn.addSelectionListener(new SelectionAdapter() {
//				public void widgetSelected(SelectionEvent event) {
//						
//					modifyPageStringsForAllCheckedDocs(true);
//
//				}
//			});
			
			
		}
		
		if (true) {
			
			Composite helper = new Composite(this, SWT.NONE);
			helper.setLayout(new GridLayout(5, false));

		    fromLbl = new Label(helper, SWT.NULL);
		    fromLbl.setText("Select ID from: ");
		    fromLbl.setToolTipText("Select all documents with docID between 'from ID' and 'to ID' (if 'to ID' is empty go to the last docID)");

		    fromTxt = new Text(helper, SWT.SINGLE | SWT.BORDER);

		    toLbl = new Label(helper, SWT.NULL);
		    toLbl.setText("to: ");
		    toLbl.setToolTipText("Select all documents with docID between 'from ID' and 'to ID' (if 'from ID' is empty start from the first document)");

		    toTxt = new Text(helper, SWT.SINGLE | SWT.BORDER);
		    
		    apply = new Button(helper, SWT.NONE);
		    apply.setText("Apply range");
		    apply.addListener(SWT.Selection, new Listener() {
		    	@Override
		    	public void handleEvent(Event e) {
		    		switch (e.type) {
		    		case SWT.Selection:
		    			logger.debug("Apply range : " + fromTxt.getText() + " - " + toTxt.getText());
		    			applyRange(fromTxt.getText(), toTxt.getText());
		    			break;
		    		}
		    	}

				private void applyRange(String from, String to) {
					int fromValue = 0;
					int toValue = Integer.MAX_VALUE;
				      try {
				          fromValue = Integer.parseInt(from);

				        } catch (NumberFormatException e) {
				          fromTxt.setText("0");
				          //show info that 'invalid' range
				        }
				      
				      try {
				          toValue = Integer.parseInt(to);

				        } catch (NumberFormatException e) {
				          toTxt.setText(Integer.toString(Integer.MAX_VALUE));
				          //show info that 'invalid' range
				        }
					
					for (TableItem ti : tv.getTable().getItems()) {
						TrpDocMetadata md = (TrpDocMetadata) ti.getData();
						ti.setChecked(md.getDocId()>=fromValue && md.getDocId()<=toValue);
					}
				}	
		    });
		    
		}
		
		this.setLabelProvider(new ITableLabelProvider() {
			@Override public void removeListener(ILabelProviderListener listener) {
			}
			
			@Override public boolean isLabelProperty(Object element, String property) {
				return true;
			}
			
			@Override public void dispose() {
			}
			
			@Override public void addListener(ILabelProviderListener listener) {
			}
			
			@Override public String getColumnText(Object element, int columnIndex) {
				if (!(element instanceof TrpDocMetadata)) {
					return "i am error";
				}
				
				String cn = columns[columnIndex].name;
				TrpDocMetadata d = (TrpDocMetadata) element;
				
				if (cn.equals(ID_COL)) {
					return ""+d.getDocId();
				}
				else if (cn.equals(TITLE_COL)) {
					return d.getTitle();
				}
				else if (cn.equals(N_PAGES_COL)) {
					return ""+d.getNrOfPages();
				}
				else if (cn.equals(PAGES_COL)) {
					return getPagesStrForDoc(d);
				}
				
				return null;
			}
			
			@Override public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		});
		
		this.withPagesSelector = withPagesSelector;
		if (this.withPagesSelector) {
			attachCellEditors(tv, this);
		}
		SWTUtil.onSelectionEvent(tv.getTable(), e -> {
			if (e.detail == SWT.CHECK) {
				updateInfoLabel();
			}
		});
		
//		modifyPageStringsForAllCheckedDocs();
	}
	
	public String getPagesStrForDoc(TrpDocMetadata md) {
		String pagesStr = pagesStrs.get(md.getDocId());
		return pagesStr==null ? "1-"+md.getNrOfPages() : pagesStr;
	}
	
	public Map<Integer, String> getPagesStrs() {
		return pagesStrs;
	}

	private String setPagesStrForDoc(TrpDocMetadata md, String pagesStr) {
		if (StringUtils.isEmpty(pagesStr)) {
			return pagesStrs.remove(md.getDocId());
		}
		else {
			return pagesStrs.put(md.getDocId(), pagesStr);	
		}
	}
	
	/**
	 * Recover a selection of DocSelection objects
	 */
	public void setPreviousSelection(List<DocSelection> checkedDocSelections) {
		for (TableItem ti : tv.getTable().getItems()) {
			TrpDocMetadata md = (TrpDocMetadata) ti.getData();
			DocSelection ds = checkedDocSelections.stream().filter(d -> d.getDocId()==md.getDocId()).findFirst().orElse(null);
			ti.setChecked(ds!=null);
//			if (ds != null) {
//				logger.debug("docId123 = "+md.getDocId()+" pages = "+ds.getPages());
//			}
			
			if (ds != null && !StringUtils.isEmpty(ds.getPages())) {
				setPagesStrForDoc(md, ds.getPages());
				tv.refresh(md);
			}
		}
	}
	
	TableItem findTableItem(TrpDocMetadata md) {
		if (md == null) {
			return null;
		}
		
		for (TableItem ti : tv.getTable().getItems()) {
			if (ti.getData() == md) {
				return ti;
			}
		}
		return null;
	}
	
	private void attachCellEditors(final TableViewer viewer, Composite parent) {
//		viewer.setUseHashlookup(true);
		
		// create and set column names:
		String[] colNames = new String[columns.length];
		for (int i=0; i<colNames.length; ++i) {
			colNames[i] = columns[i].name;
		}
		viewer.setColumnProperties(colNames);
		
		// create and set cell editors:
		CellEditor[] cellEditors = new CellEditor[columns.length];
		for (int i=0; i<colNames.length; ++i) {
			cellEditors[i] = null;
			if (columns[i].name.equals(PAGES_COL)) {
				cellEditors[i] = new TextCellEditor(viewer.getTable());
			}
		}		
		viewer.setCellEditors(cellEditors);
		
		// create and set the cell modifier:
	    viewer.setCellModifier(new ICellModifier() {
			public boolean canModify(Object element, String property) {
				if (PAGES_COL.equals(property)) {
					TableItem ti = findTableItem((TrpDocMetadata) element);
					if (ti != null && ti.getChecked()) {
						return true;
					}
				}
				return false;
			}

	      public Object getValue(Object element, String property) {
	    	  logger.trace("getValue, element="+element+", property="+property);
	    	  TrpDocMetadata d = (TrpDocMetadata) element;
	    	  if (property.equals(PAGES_COL)) {
	    		  return getPagesStrForDoc(d); 
	    	  }
	    	  else {
	    		  return "i am error";  
	    	  }
	      }

	      public void modify(Object element, String property, Object value) {
	    	  TrpDocMetadata d = (TrpDocMetadata) ((TableItem) element).getData(); // note: here, element is a TableItem!!
	    	  logger.debug("modify, element="+element+", property="+property+", value="+value);
	    	  String pagesStr = value==null ? "" : ""+value;
	    	  
	    	  if (StringUtils.isEmpty(pagesStr) || CoreUtils.isValidRangeListStr(pagesStr, d.getNrOfPages())) {
	    		  setPagesStrForDoc(d, ""+value);
	    		  viewer.refresh(d);
	    	  }
	    	  
	    	  updateInfoLabel();
	      }
	    });
	  }
		
	public List<TrpDocMetadata> getCheckedDocuments() {
		return getCheckedDataList();
	}
	
	public List<DocSelection> getCheckedDocSelections() {
		logger.debug("getCheckedDocSelections, pagesStrs = "+CoreUtils.mapToString(pagesStrs));
		return new ArrayList<>(getCheckedDocumentsDetails().values());
	}
	
	public Map<TrpDocMetadata, DocSelection> getCheckedDocumentsDetails() {
		Map<TrpDocMetadata, DocSelection> selection = new TreeMap<>();
		for(TrpDocMetadata d : getCheckedDocuments()) {
			String pagesStr = pagesStrs.get(d.getDocId());
			logger.debug("pagesStr = "+pagesStr);
			// if all pages selected -> clear pagesStr -> = select all pages!
			if (!StringUtils.isEmpty(pagesStr) && pagesStr.equals("1-"+d.getNrOfPages())) { // necessary !?
				logger.debug("clearing pagesStr as all pages are selected!");
				pagesStr = null;
			}
			selection.put(d, new DocSelection(d.getDocId(), pagesStr, null, null));
		}
		return selection;
	}
	
    private void modifyPageStringsForAllCheckedDocs() {
    	for (TrpDocMetadata d : getCheckedDocuments()) {
    	  	 setPageString(d);
    	}
    	updateInfoLabel();
    }
    
    private void updateInfoLabel() {
    	List<TrpDocMetadata> checkedDocs = getCheckedDataList();
    	List<DocSelection> checkedDocSel = getCheckedDocSelections();
    	
    	try {
        	int nPages=0;
        	for (int i=0; i<checkedDocSel.size(); ++i) {
        		nPages += checkedDocSel.get(i).getNrOfSelectedPages(checkedDocs.get(i).getNrOfPages());
        	}    	
        	if (infoLabel != null)
        		infoLabel.setText("N-Selected-Pages: "+nPages);
    	} catch (Exception e) {
    		logger.error(e.getMessage(), e);
    	}
    }
    
	private void setPageString(TrpDocMetadata doc) {

		List<Boolean> checked = new ArrayList<>();
		if (doc != null) {
			
			TrpDoc currDoc = null;
			try {
				currDoc = Storage.getInstance().getConnection().getTrpDoc(doc.getFirstCollectionId(), doc.getDocId(), -1);
			} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
				TrpMainWidget.getInstance().onError("Error retrieving document", e.getMessage(), e);
				return;
			}
			
			for (TrpPage page : currDoc.getPages()){
				TrpTranscriptMetadata ttm = page.getCurrentTranscript();
				checked.add(filterBtn!=null ? filterBtn.isAccepted(ttm) : true);
				
//				if (emptyLines) {
//					if (ttm != null && (ttm.getNrOfLines() == null || ttm.getNrOfLines() == 0) || (ttm.getNrOfTranscribedLines() != null && ttm.getNrOfTranscribedLines() > 0)){
//						checked.add(false);
//					}
//					else{
//						checked.add(true);
//					}
//				}
//				else {
//					//limit the recognition to In_Progress and New but not GT, Final, Done
//					if(ttm.getStatus() == EditStatus.IN_PROGRESS || ttm.getStatus() == EditStatus.NEW) {
//						checked.add(true);
//					}
//					else {
//						checked.add(false);
//					}
//					
//				}
			}
			
			String pageStr = CoreUtils.getRangeListStr(checked);
			//logger.debug("pageString with pages containing no lines = "+pageStr);
			if (pageStr.isEmpty()) {
				TableItem ti = findTableItem((TrpDocMetadata) doc);
				setPagesStrForDoc(doc, "0"); 
				ti.setChecked(false);
			}
			else {
				setPagesStrForDoc(doc, pageStr); 
				
			}
			tv.refresh(doc);
			//logger.debug("pageString with pages containing no lines = "+pageString);
		}
	}
	
	public List<DocumentSelectionDescriptor> getCheckedDocumentDescriptors() {
		List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
		for (TrpDocMetadata d : getCheckedDocuments()) {
			DocumentSelectionDescriptor dsd = new DocumentSelectionDescriptor(d.getDocId());
//			if (this.withPagesSelector) { // FIXME does not work properly and has not been tested
//				String pagesStr = pagesStrs.get(dsd.getDocId());
//				if (!StringUtils.isEmpty(pagesStr)) {
//					try {
//						TrpDoc doc = Storage.getInstance().getConnection().getTrpDoc(Storage.getInstance().getCollId(), d.getDocId(), 1);
//						dsd = DocumentSelectionDescriptor.fromDocAndPagesStr(doc, pagesStr);
//					} catch (Exception e) {
//						DialogUtil.showErrorMessageBox(getShell(), "Error parsing pages", "Could not parse pages: "+pagesStr+", doc: "+d+"\nShould not happen here...");
//						logger.debug(e.getMessage(), e);
//					}
//				}
//			}
			dsds.add(dsd);
		}
		return dsds;
	}

	@Override
	protected Control createPreviewArea(Composite previewContainer) {
		return null;
	}

	@Override
	protected void reloadPreviewForSelection() {
	}

}