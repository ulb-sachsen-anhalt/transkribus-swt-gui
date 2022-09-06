package eu.transkribus.swt_gui.la;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.core.model.beans.rest.ParameterMap;
import eu.transkribus.swt.util.LabeledCombo;
import eu.transkribus.swt.util.LabeledComboWithButton;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.TrpGuiPrefs;
import eu.transkribus.swt_gui.dialogs.ALaConfigDialog;
import eu.transkribus.swt_gui.dialogs.CITlabAdvancedLaConfigDialog;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrDocPagesOrCollectionSelector;

public class LayoutAnalysisComposite extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(LayoutAnalysisComposite.class);
	
	public static boolean TEST = false;
	public static final boolean IS_CONFIGURABLE = true;
	
	static private Storage store = TEST ? null : Storage.getInstance();
//	private DocPagesSelector dps;
	private CurrentTranscriptOrDocPagesOrCollectionSelector dps;
	private Button doBlockSegBtn, doLineSegBtn, doWordSegBtn, enrichOldTranscriptBtn;
	private Label selectedModelLabel;
//	private LabeledComboWithButton methodCombo;
	private LabeledCombo methodCombo;
//	private LabeledText customJobImplText;
		
	//public static final String METHOD_NCSR_OLD = "NCSR";
	//public static final String METHOD_NCSR = "NCSR New (experimental)";
	public static final String METHOD_CVL = "CVL (experimental)";
	public static final String METHOD_CITLAB = "CITlab";
	public static final String METHOD_CITLAB_ADVANCED = "CITlab Advanced";
	public static final String METHOD_PRINTED_BLOCKS = "Printed Block Detection";
	public static final String METHOD_SEPARATORS = "Separator Detection";
	public static final String METHOD_TRANSKRIBUS = "Transkribus LA (Alpha)";
	
//	public static final String METHOD_CUSTOM = "Custom";
	
	public static final String[] LA_CITLAB_ALLOWED_USERS = { };
	
	private ParameterMap paramMap;
	private ALaConfigDialog configDialog = null;
	
	Composite mainContainer, tmpContainer;

	public LayoutAnalysisComposite(Composite parent, int style) {
		super(parent, style);
		
		mainContainer = (Composite) this;
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginWidth = 0;
		this.setLayout(gl);
		
//		GridLayout g = (GridLayout)mainContainer.getLayout();
//		g.numColumns = 1;
//		g.makeColumnsEqualWidth = false;
		
		final String labelTxt = "Method: ";
		if(IS_CONFIGURABLE) {
			methodCombo = new LabeledComboWithButton(mainContainer,  labelTxt, "Configure...");
		} else {
			methodCombo = new LabeledCombo(mainContainer,  labelTxt);
		}
		methodCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		updateMethods();
//		methodCombo.combo.setItems(getMethods(true).toArray(new String[0]));
//		methodCombo.combo.select(0);
				
		//with this selector jobs can be started for complete collections
		dps = new CurrentTranscriptOrDocPagesOrCollectionSelector(mainContainer, SWT.NONE, true, true, true, true);		
		dps.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));

//		Group checkGrp = new Group(mainContainer,SWT.NONE);
//		checkGrp.setLayout(new GridLayout(1, false));
//		checkGrp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 2));
		
		tmpContainer = new Composite(mainContainer, SWT.FILL);
		tmpContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tmpContainer.setLayout(new GridLayout(2, false));
		
		doBlockSegBtn = new Button(tmpContainer, SWT.CHECK);
		doBlockSegBtn.setText("Find Text Regions");
		doBlockSegBtn.setSelection(true);
		doBlockSegBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
		
//		unsegmentedBtn = new Button(tmpContainer, SWT.PUSH);
//		unsegmentedBtn.setText("Empty pages only");
//		unsegmentedBtn.setForeground(new Color(Display.getCurrent(),0, 100, 0));
//		unsegmentedBtn.setSelection(false);
//		unsegmentedBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true, 1, 1));
//		unsegmentedBtn.addSelectionListener(new SelectionAdapter() {
//		      public void widgetSelected(SelectionEvent e) {
//		    	  	//pages String
//		    	  if (unsegmentedBtn.getSelection()){
//			    	  String pageString = "";
//			    	  List<Boolean> checked = new ArrayList<>();
//			    	  for (TrpPage page : Storage.getInstance().getDoc().getPages()){
//			    		  TrpTranscriptMetadata ttm = page.getCurrentTranscript();
//			    		  if (ttm.getNrOfLines() != null && ttm.getNrOfLines() > 0){
//			    			  checked.add(false);
//			    		  }
//			    		  else{
//			    			  checked.add(true);
//			    		  }
//			    	  }
//			    	  pageString = CoreUtils.getRangeListStr(checked);
//			    	  logger.debug("pageString with pages containing no lines = "+pageString);
//			    	  
//			    	  dps.setPagesStr(pageString);
//		    	  }
//		    	  else{
//		    		  dps.getPagesSelector().resetLabelAndPagesStr();
//		    	  }
//		      }
//		      });		
		
		doLineSegBtn = new Button(tmpContainer, SWT.CHECK);
		doLineSegBtn.setText("Find Lines in Text Regions");
		doLineSegBtn.setSelection(true);
		
		//can be used as a place filler
		//new Label(tmpContainer, SWT.NONE);

		doWordSegBtn = new Button(tmpContainer, SWT.CHECK);
		doWordSegBtn.setText("Find Words in Lines (experimental!)");
		doWordSegBtn.setVisible(false);
		
		enrichOldTranscriptBtn = new Button(tmpContainer, SWT.CHECK);
		enrichOldTranscriptBtn.setText("Enrich the current PAGE XML");
		enrichOldTranscriptBtn.setVisible(false);
		
		selectedModelLabel = new Label(tmpContainer, 0);
		selectedModelLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

//		customJobImplText = new LabeledText(mainContainer, "Custom jobImpl: ");
//		customJobImplText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				
		addListener();
		
		updateGui();
	}
	
	private void updateMethods() {
		methodCombo.combo.setItems(getMethods(true).toArray(new String[0]));
		methodCombo.combo.select(0);
		//load stored params
		paramMap = TrpGuiPrefs.getLaParameters(getJobImpl());
	}
	
	private void updateGui() {
		String method = getSelectedMethod();
				
		doBlockSegBtn.setEnabled(true);
		doLineSegBtn.setEnabled(true);
		doWordSegBtn.setEnabled(true);
		enrichOldTranscriptBtn.setVisible(false);
		
		switch(method) {
//		case METHOD_NCSR:
//			doBlockSegBtn.setEnabled(false);
//			doBlockSegBtn.setSelection(false);
//			break;
//		case METHOD_NCSR_OLD:
//			doWordSegBtn.setSelection(false);
//			doWordSegBtn.setEnabled(false);
//			break;
		case METHOD_CITLAB:
//			doBlockSegBtn.setSelection(true);
//			doBlockSegBtn.setEnabled(false);
			doWordSegBtn.setSelection(false);
			doWordSegBtn.setEnabled(false);
			break;
			/*
			 * next two are bottom-up approaches and thus must have 'detect lines' enabled
			 * for CVL regions and lines are always recogniced for the whole page
			 * for Citlab Advanced it is possible to detect lines just for the selected regions
			 * TODO: merge the unselected regions with the result from the current recognition 
			 */
		case METHOD_CVL:
			doBlockSegBtn.setSelection(true);
			doBlockSegBtn.setEnabled(false);
			doLineSegBtn.setSelection(true);
			doLineSegBtn.setEnabled(false);
			doWordSegBtn.setSelection(false);
			doWordSegBtn.setEnabled(false);
			break;
		case METHOD_CITLAB_ADVANCED:
			doWordSegBtn.setSelection(false);
			doWordSegBtn.setEnabled(false);
			doLineSegBtn.setSelection(true);
			doLineSegBtn.setEnabled(false);
			break;
		case METHOD_PRINTED_BLOCKS:
			doBlockSegBtn.setSelection(true);
			doBlockSegBtn.setEnabled(false);
			doLineSegBtn.setSelection(false);
			doLineSegBtn.setEnabled(false);
			doWordSegBtn.setSelection(false);
			doWordSegBtn.setEnabled(false);
			break;
		case METHOD_SEPARATORS:
			doBlockSegBtn.setSelection(true);
			doBlockSegBtn.setEnabled(false);
			doLineSegBtn.setSelection(false);
			doLineSegBtn.setEnabled(false);
			doWordSegBtn.setSelection(false);
			doWordSegBtn.setEnabled(false);
			enrichOldTranscriptBtn.setVisible(true);
			break;
		case METHOD_TRANSKRIBUS:
			doBlockSegBtn.setSelection(false);
			doBlockSegBtn.setEnabled(false);
			doLineSegBtn.setSelection(true);
			doLineSegBtn.setEnabled(false);
			doWordSegBtn.setSelection(false);
			doWordSegBtn.setEnabled(false);
			break;
		default:
			return;
		}
		
		if (isSelectedMethodConfigurable()) {
			selectedModelLabel.setText(new CITlabAdvancedLaConfigDialog(getShell(), getParameters(), getJobImpl()).getSelectedModelInfoString());
		}
		else {
			selectedModelLabel.setText("");
		}
		
		//enable config button only if method is configurable (only CITlabAdvanced for now)
		if(methodCombo instanceof LabeledComboWithButton) {
			((LabeledComboWithButton)methodCombo).getButton().setEnabled(isSelectedMethodConfigurable());
		}
	}
	
	public static boolean isUserAllowedTranskribusLaJob() {
		return store.isAdminLoggedIn();
	}

	public static boolean isUserAllowedCitlab() {
		if (TEST || store.isAdminLoggedIn())
			return true;
		
		try {
			return store.isUserAllowedForJob(JobImpl.CITlabLaJob.toString(), false);
		} catch (Exception e) {
			logger.error("Could not determine if user is allowed for CITlabLaJob: "+e.getMessage());
			return false;
		}
	}

	public static List<String> getMethods(boolean withCustom) {
		List<String> methods = new ArrayList<>();
		
		methods.add(METHOD_CITLAB_ADVANCED);
		methods.add(METHOD_PRINTED_BLOCKS);
		methods.add(METHOD_SEPARATORS);
		
		if (isUserAllowedTranskribusLaJob()) {
			methods.add(METHOD_TRANSKRIBUS);			
		}
		
//		methods.add(METHOD_NCSR_OLD);
//		methods.add(METHOD_NCSR);
//		methods.add(METHOD_CVL);
				
		if (isUserAllowedCitlab()) {
			methods.add(METHOD_CITLAB);
		}
		
		return methods;
	}
	
	private void addListener() {			
		methodCombo.combo.addModifyListener(new ModifyListener() {
			@Override public void modifyText(ModifyEvent e) {
				logger.trace("method changed: " + getJobImpl());
				paramMap = TrpGuiPrefs.getLaParameters(getJobImpl());
				updateGui();
				if(configDialog != null) {
					configDialog.close();
					configDialog = null;
				}
			}
		});
		if(methodCombo instanceof LabeledComboWithButton) {
			((LabeledComboWithButton)methodCombo).getButton().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if(configDialog != null) {
						configDialog.setVisible();
					} else {
						JobImpl impl = getJobImpl();
						switch(impl) {
						case CITlabAdvancedLaJob:
						case TranskribusLaJob:							
							configDialog = new CITlabAdvancedLaConfigDialog(getShell(), paramMap, impl);
							break;							
						default:
							return;	
						}
						final int ret = configDialog.open();
						logger.debug("Dialog ret = " + ret);
						if(ret == IDialogConstants.OK_ID && configDialog != null) { //may be null if close() is called programmatically (happens when switching methods)
							paramMap = configDialog.getParameters();
							TrpGuiPrefs.storeLaParameters(impl, paramMap);
							updateGui();
						}
						configDialog = null;
					}
				}
			});
		}
		
		if (store != null) {
			store.addListener(new IStorageListener() {
				public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
					updateMethods();
				}
			});
		}
	}
	
	private void setPageSelectionToCurrentPage(){
		if (TEST) {
			dps.getPagesSelector().getPagesText().setText("NA");
		} else {
			dps.getPagesSelector().getPagesText().setText(""+store.getPage().getPageNr());	
		}
	}
	
	public void setPageSelectionToSelectedPages(String pages){
		dps.getPagesSelector().getPagesText().setText(pages);
	}
	
	public Button getDoBlockSegBtn() {
		return doBlockSegBtn;
	}

	public Button getDoLineSegBtn() {
		return doLineSegBtn;
	}
	
	public String getSelectedMethod() {
		if (methodCombo.combo.getSelectionIndex()>=0 && methodCombo.combo.getSelectionIndex()<methodCombo.combo.getItemCount()) {
			return methodCombo.combo.getItems()[methodCombo.combo.getSelectionIndex()];	
		} else {
			return "";
		}
	}
	
	public static JobImpl getJobImplForMethod(String selectedMethod) {
		switch(selectedMethod) {
//		case METHOD_NCSR_OLD:
//			return JobImpl.NcsrOldLaJob;
//		case METHOD_NCSR:
//			return JobImpl.NcsrLaJob;
		case METHOD_CVL:
			return JobImpl.CvlLaJob;
		case METHOD_CITLAB:
			return JobImpl.CITlabLaJob;
		case METHOD_CITLAB_ADVANCED:
			return JobImpl.CITlabAdvancedLaJob;
		case METHOD_PRINTED_BLOCKS:
			return JobImpl.FinereaderLaJob;
		case METHOD_SEPARATORS:
			return JobImpl.FinereaderSepJob;
		case METHOD_TRANSKRIBUS:
			return JobImpl.TranskribusLaJob;
		default:
			return null;
		}
	}
	
	public void updateSelectionChooserForLA(){
		dps.updateGui();
		this.layout(true);
	}
	
	
	public boolean isDoLineSeg(){
		return doLineSegBtn.getSelection();
	}
	
	public boolean isDoBlockSeg(){
		return doBlockSegBtn.getSelection();
	}
	
	public boolean isEnrichOldTranscript() {
		return enrichOldTranscriptBtn.getSelection();
	}

	public boolean isDoWordSeg() {
		return doWordSegBtn.getSelection();
	}
	
	public boolean isCurrentTranscript() {
		return dps.isCurrentTranscript();
	}
	
	public boolean isDocsSelection() {
		return dps.isDocsSelection();
	}
	
	public String getPages(){
		return dps.getPagesStr();
	}
	
	public ParameterMap getParameters() {
		return paramMap;
	}
	
//	public List<DocumentSelectionDescriptor> getDocs() {
//		return dps.getDocumentsSelected();
//	}
	
	public List<DocSelection> getDocs() {
		return dps.getDocSelections();
	}
	
	public JobImpl getJobImpl() {
		String selectedMethod = getSelectedMethod();
		JobImpl jobImpl = getJobImplForMethod(selectedMethod);
		return jobImpl;
	}
	
	public boolean isSelectedMethodConfigurable() {
		return CITlabAdvancedLaConfigDialog.isConfigurable(getJobImpl());
	}
	
}
