package eu.transkribus.swt_gui.htr;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrDocPagesOrCollectionSelector;
import eu.transkribus.util.TextRecognitionConfig;
import eu.transkribus.util.TextRecognitionConfig.Mode;

public class HtrTextRecognitionDialog extends Dialog implements SelectionListener {
	private static final Logger logger = LoggerFactory.getLogger(HtrTextRecognitionDialog.class);
	
	private HtrTextRecognitionConfigDialog trcd = null;

	private CurrentTranscriptOrDocPagesOrCollectionSelector dps;
	private boolean docsSelected = false;
	private List<DocSelection> selectedDocSelections;
	private Map<TrpDocMetadata, DocSelection> selectedDocSelectionDetails;
	
	PyLaiaRecognitionConfComposite pylaiaConfComp;
	CitlabRecognitionConfComposite citlabConfComp;
	
	private Storage store = Storage.getInstance();
	
	private TextRecognitionConfig config;
	private String pages;
	
	private Composite container;
	private Text configTxt;
	private Button configBtn;
	
	public HtrTextRecognitionDialog(Shell parent) {
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
		
		container = cont;
		
		//FIXME the document selection is not initialized before the selection dialog is opened once
		//with this selector jobs can be started for complete collections
		dps = new CurrentTranscriptOrDocPagesOrCollectionSelector(cont, SWT.NONE, true, true, true, true);		
		dps.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 3, 1));
		
		citlabConfComp = new CitlabRecognitionConfComposite(cont, this);
		citlabConfComp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 3, 1));
		
		pylaiaConfComp = new PyLaiaRecognitionConfComposite(SWTUtil.dummyShell, this);
		pylaiaConfComp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 3, 1));
		
		configTxt = new Text(cont, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
		configTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 6));
				
		configBtn = new Button(cont, SWT.PUSH);
		configBtn.setText("Select HTR model...");
		configBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		configBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(trcd == null) {
					trcd = new HtrTextRecognitionConfigDialog(parent.getShell(), config);
					if(trcd.open() == IDialogConstants.OK_ID) {
						logger.info("OK pressed");
						config = trcd.getConfig();
						store.saveTextRecognitionConfig(config);	
						updateUi();
					}
					trcd = null;
				} else {
					trcd.setVisible();
				}
			}
		});
		
		config = store.loadTextRecognitionConfig();
		logger.debug("Config loaded:" + config);
		
		updateUi();
		return cont;
	}
	
	public boolean isDocsSelection(){
		return docsSelected;
	}
	
//	public List<DocumentSelectionDescriptor> getDocs(){
//		return selectedDocDescriptors;
//	}
	
	public List<DocSelection> getDocs() {
		return selectedDocSelections;
	}
	
	public Map<TrpDocMetadata, DocSelection> getDocSelectionDetails() {
		return selectedDocSelectionDetails;
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
		
		if (config == null) {
			DialogUtil.showErrorMessageBox(getShell(), "Bad Configuration", "Please define a configuration.");
			return;
		}
		
		if (config != null) {
//			boolean isCitlab = config.getMode()==Mode.CITlab;
			if (config.getMode()==Mode.CITlab) {
				logger.debug("structs = "+Arrays.toString(citlabConfComp.structureTagComp.getMultiCombo().getSelections()));
				config.setStructures(Arrays.asList(citlabConfComp.structureTagComp.getMultiCombo().getSelections()));
				config.setKeepOriginalLinePolygons(citlabConfComp.keepOriginalLinePolygonsBtn.getSelection());
				config.setDoLinePolygonSimplification(citlabConfComp.doLinePolygonSimplificationBtn.getSelection());
				config.setDoStoreConfMats(citlabConfComp.doStoreConfMatsBtn.getSelection());			
			}
			else if (config.getMode()==Mode.UPVLC) {
				logger.debug("structs = "+Arrays.toString(pylaiaConfComp.structureTagComp.getMultiCombo().getSelections()));
				config.setStructures(Arrays.asList(pylaiaConfComp.structureTagComp.getMultiCombo().getSelections()));				
				config.setUseExistingLinePolygons(pylaiaConfComp.useExistingLinePolygonsBtn.getSelection());
				config.setDoLinePolygonSimplification(pylaiaConfComp.doLinePolygonSimplificationBtn.getSelection());
				config.setDoWordSeg(pylaiaConfComp.doWordSegBtn.getSelection());
				boolean writeKwsIndexFiles = config!=null && config.getLanguageModel()!=null && pylaiaConfComp.writeKwsIndexFiles.getSelection();
				config.setWriteKwsIndexFiles(writeKwsIndexFiles);
				if (config.isWriteKwsIndexFiles()) {
					try {
						int nBest = Integer.parseInt(pylaiaConfComp.nBest.getText());
						config.setNBest(nBest);
					}
					catch (Exception e) {
						DialogUtil.showErrorMessageBox(getShell(), "Error parsing nBest", "Invalid nBest: "+pylaiaConfComp.nBest.getText());
						return;
					}					
				}
				try {
					int batchSize = Integer.parseInt(pylaiaConfComp.batchSizeText.getText());
					config.setBatchSize(batchSize);
				}
				catch (Exception e) {
					DialogUtil.showErrorMessageBox(getShell(), "Error parsing batch size", "Invalid batch size: "+pylaiaConfComp.batchSizeText.getText());
					return;
				}
			}
			else  {
				DialogUtil.showErrorMessageBox(getShell(), "Bad Configuration", "No Citlab or UPVLC model selected - should not happen...");
				return;
			}
			//let server decide on strategy
//			config.setCreditSelectionStrategy(CreditSelectionStrategy.AUTO);
		}
		
		super.okPressed();
	}
	
	private void updateUi() {
//		boolean isCitlab = config!=null && config.getMode()==Mode.CITlab;
		configTxt.setText(config!=null ? config.toString() : "");
		if (config == null) {
			citlabConfComp.setParent(SWTUtil.dummyShell);
			pylaiaConfComp.setParent(SWTUtil.dummyShell);
		}
		else if (config.getMode()==Mode.CITlab) {
			citlabConfComp.setParent(container);
			citlabConfComp.moveAbove(configTxt);
			pylaiaConfComp.setParent(SWTUtil.dummyShell);
		}
		else if (config.getMode()==Mode.UPVLC) {
			pylaiaConfComp.setParent(container);
			pylaiaConfComp.moveAbove(configTxt);
			pylaiaConfComp.updateUi();
			citlabConfComp.setParent(SWTUtil.dummyShell);
		}
		container.layout(true);		
		container.redraw();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Text Recognition");
		if (Storage.getInstance().isAdminLoggedIn())
			newShell.setMinimumSize(300, 470);
		else{
			newShell.setMinimumSize(300, 430);
		}
	}

	@Override
	protected Point getInitialSize() {
		return SWTUtil.getPreferredOrMinSize(getShell(), 300, 470);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
	
	public TextRecognitionConfig getConfig() {
		return this.config;
	}
	
	public String getPages() {
		return this.pages;
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void widgetSelected(SelectionEvent arg0) {
		int index = 0;
		if (citlabConfComp.getParent() == container) {
			index = citlabConfComp.getRecentModelsCombo().getCombo().getSelectionIndex();
			citlabConfComp.getRecentModelsCombo().getCombo().layout();
			citlabConfComp.getRecentModelsCombo().loadRecentHtr(citlabConfComp.getRecentModelsCombo().getCombo().getItem(index));
			
		}
		else if (pylaiaConfComp.getParent() == container) {
			index = pylaiaConfComp.getRecentModelsCombo().getCombo().getSelectionIndex();
			pylaiaConfComp.getRecentModelsCombo().getCombo().layout();
			pylaiaConfComp.getRecentModelsCombo().loadRecentHtr(pylaiaConfComp.getRecentModelsCombo().getCombo().getItem(index));
			
		}
		config = store.loadTextRecognitionConfig();
		configTxt.requestLayout();
		updateUi();
		
		if (citlabConfComp.getParent() == container) {
			citlabConfComp.getRecentModelsCombo().getCombo().select(index);
			
		}
		else if (pylaiaConfComp.getParent() == container) {
			pylaiaConfComp.getRecentModelsCombo().getCombo().select(index);
		}
		
		
		
		
	}
	
	

}
