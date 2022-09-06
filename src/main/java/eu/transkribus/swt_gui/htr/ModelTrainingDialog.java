package eu.transkribus.swt_gui.htr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.model.beans.CITlabLaTrainConfig;
import eu.transkribus.core.model.beans.CitLabHtrTrainConfig;
import eu.transkribus.core.model.beans.CitLabSemiSupervisedHtrTrainConfig;
import eu.transkribus.core.model.beans.HtrTrainConfig;
import eu.transkribus.core.model.beans.PyLaiaHtrTrainConfig;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.core.model.beans.rest.ParameterMap;
import eu.transkribus.core.util.BaselineTrainDataSelector;
import eu.transkribus.core.util.HtrTrainDataSelector;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.htr.treeviewer.DataSetSelectionController.DataSetSelection;
import eu.transkribus.swt_gui.htr.treeviewer.DataSetSelectionSashForm;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * Configuration dialog for HTR training.<br/>
 * @author philip
 *
 */
public class ModelTrainingDialog extends Dialog implements PropertyChangeListener{
	private static final Logger logger = LoggerFactory.getLogger(ModelTrainingDialog.class);

	private final int colId;

	private CTabFolder paramTabFolder;
	private CTabItem citlabHtrTrainingTabItem;
	private CTabItem citlabHtrPlusTrainingTabItem;
	private CTabItem citlabT2ITabItem;
	private CTabItem pylaiaTrainTabItem;
	private CTabItem citlabLaTrainTabItem;
	
	private Text2ImageConfComposite t2iConfComp;
	private CITlabHtrTrainingConfComposite citlabHtrParamCont;
	private CITlabHtrPlusTrainingConfComposite citlabHtrPlusParamCont;
	private PyLaiaTrainingConfComposite pyLaiaTrainingConfComp;
	private CITlabLATrainingConfComposite citlabLaTrainingConfComp;
	
	private DataSetSelectionSashForm treeViewerSelector;

//	private Text modelNameTxt, descTxt, langTxt;
	private Text modelNameTxt, descTxt;
	private Label langLbl;
	private IsoLanguageEditComposite langEditor;
//	private MultiCheckSelectionCombo langSelection;

	private CitLabHtrTrainConfig citlabTrainConfig;
	private CitLabSemiSupervisedHtrTrainConfig citlabT2IConf;
	private PyLaiaHtrTrainConfig pyLaiaConf;
	private CITlabLaTrainConfig citlabLaConf;

	private Storage store = Storage.getInstance();

	private List<TrpDocMetadata> docList;
//	private List<TrpHtr> htrList;
	
	private final List<JobImpl> trainJobImpls;
	
	private List<TrainMethodUITab> tabList;
	
	public static final boolean ENABLE_T2I = false;

	private boolean enableDebugDialog = false; 

	public ModelTrainingDialog(Shell parent, JobImpl[] impls) {
		super(parent);
		if(impls == null || impls.length == 0) {
			throw new IllegalStateException("No HTR training jobs defined.");
		}
		this.docList = store.getDocList();
		this.colId = store.getCollId();	
		this.trainJobImpls = Arrays.asList(impls);
		this.tabList = new ArrayList<>(impls.length);
		
		
	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
			//mainly to load first page of paged htrs
			logger.debug("update training dialog UI and set visible");
			updateUI();
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
		
		Button helpBtn = createButton(parent, IDialogConstants.HELP_ID, "Help", false);
		helpBtn.setImage(Images.HELP);
		SWTUtil.onSelectionEvent(helpBtn, e -> {
			org.eclipse.swt.program.Program.launch("https://readcoop.eu/transkribus/howto/how-to-train-a-handwritten-text-recognition-model-in-transkribus");
		});
		
	    Button runBtn = createButton(parent, IDialogConstants.OK_ID, "Train", false);
	    runBtn.setImage(Images.ARROW_RIGHT);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		
		logger.debug("create training dialog");
		Composite cont = (Composite) super.createDialogArea(parent);

		SashForm sash = new SashForm(cont, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sash.setLayout(new GridLayout(2, false));

		Composite paramCont = new Composite(sash, SWT.BORDER);
		paramCont.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramCont.setLayout(new GridLayout(4, false));

		Label modelNameLbl = new Label(paramCont, SWT.FLAT);
		modelNameLbl.setText("Model Name:");
		modelNameTxt = new Text(paramCont, SWT.BORDER);
		modelNameTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		langLbl = new Label(paramCont, SWT.FLAT);
		langLbl.setText("Language:");
		langEditor = new IsoLanguageEditComposite(paramCont, 0);
		langEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label descLbl = new Label(paramCont, SWT.FLAT);
		descLbl.setText("Description:");
		descTxt = new Text(paramCont, SWT.MULTI | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 3;
		// gd.horizontalSpan = 3;
		descTxt.setLayoutData(gd);

		paramTabFolder = new CTabFolder(paramCont, SWT.BORDER | SWT.FLAT);
		paramTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		int i = 0;
		CTabItem selection = null;
		if(trainJobImpls.contains(JobImpl.PyLaiaTrainingJob)) {
			TrainMethodUITab tab = createPylaiaTrainTabItem(i++);
			selection = tab.getTabItem();
			tabList.add(tab);
		}		

		if(trainJobImpls.contains(JobImpl.CITlabHtrPlusTrainingJob)) {
			TrainMethodUITab tab = createCitlabHtrPlusTrainingTab(i++);
			if (selection == null) { // select HTR+ training tab if nothing was selected before (if not authorized for PyLaia)
				selection = tab.getTabItem();
			}			
			tabList.add(tab);
		}
		
		if(trainJobImpls.contains(JobImpl.BaselineTrainingJob)) {
			TrainMethodUITab tab = createCitlabLaTrainTabItem(i++);
			if (selection == null) { // select HTR+ training tab if nothing was selected before (if not authorized for PyLaia)
				selection = tab.getTabItem();
			}			
			tabList.add(tab);
		}		
		
		if(false && trainJobImpls.contains(JobImpl.CITlabHtrTrainingJob)) { // disable old HTR Training...!?
			TrainMethodUITab tab = createCitlabTrainingTab(i++);
			selection = tab.getTabItem();
			tabList.add(tab);
		}		

		if(ENABLE_T2I && trainJobImpls.contains(JobImpl.CITlabSemiSupervisedHtrTrainingJob)) {
			TrainMethodUITab tab = createCitlabT2ITab(i++);
			if(selection == null) {
				//only select t2i if no other method is configured
				selection = tab.getTabItem();
			}
			tabList.add(tab);
		}
		
		if (selection!=null) {
			logger.debug("SELECTION: "+selection);
			paramTabFolder.setSelection(selection);	
		}
		paramCont.pack();
		SWTUtil.onSelectionEvent(paramTabFolder, (e) -> { updateUI(); } );
		SWTUtil.setTabFolderBoldOnItemSelection(paramTabFolder);
		
		treeViewerSelector = new DataSetSelectionSashForm(sash, SWT.HORIZONTAL, colId, docList);
		treeViewerSelector.enableDebugDialog(this.enableDebugDialog);
		treeViewerSelector.getHtrPagedTree().addPropertyChangeListener(this);
		
		GridLayout layout = (GridLayout) treeViewerSelector.getHtrPagedTree().getPageableTree().getCompositeBottom().getLayout();
		layout.numColumns += 1;
		Button retrainBtn = new Button(treeViewerSelector.getHtrPagedTree().getPageableTree().getCompositeBottom(), 0);
		retrainBtn.setText("Retrain selected model...");
		SWTUtil.onSelectionEvent(retrainBtn, e -> {
			if (treeViewerSelector.getHtrPagedTree().getSelectedModel() != null) {
				retrainModel(treeViewerSelector.getHtrPagedTree().getSelectedModel());	
			}
		});
		
		sash.setWeights(new int[] { 40, 60 });
		
		//refreshes the dialog
		cont.layout();
		
		updateUI();
		
		return cont;
	}

	private void updateUI() {
		boolean isT2I = isCitlabT2ISelected();
		logger.debug("update the UI - isT2i" +isT2I );
		descTxt.setEnabled(!isT2I);
		modelNameTxt.setEnabled(!isT2I);
		treeViewerSelector.setGroundTruthSelectionEnabled(!isT2I);
		
		langLbl.setVisible(hasSelectedTabLanguage());
		langEditor.setVisible(hasSelectedTabLanguage());
		
		if (isCitlabLaTrainingSelected()) {
			treeViewerSelector.getController().setSelector(new BaselineTrainDataSelector());
		}
		else {
			treeViewerSelector.getController().setSelector(new HtrTrainDataSelector());
		}
		treeViewerSelector.refreshTreeViewers();
	}
	
	public void setHTRsEnabled() {
		boolean isT2I = isCitlabT2ISelected();
		treeViewerSelector.setGroundTruthSelectionEnabled(!isT2I);
	}
	
	private TrainMethodUITab createCitlabT2ITab(final int tabIndex) {
		citlabT2ITabItem = new CTabItem(paramTabFolder, SWT.NONE);
		citlabT2ITabItem.setText("CITlab T2I");
		
		t2iConfComp = new Text2ImageConfComposite(paramTabFolder, 0);
		citlabT2ITabItem.setControl(t2iConfComp);
		return new TrainMethodUITab(tabIndex, citlabT2ITabItem, t2iConfComp);
	}
	
	private TrainMethodUITab createCitlabTrainingTab(final int tabIndex) {
		citlabHtrTrainingTabItem = new CTabItem(paramTabFolder, SWT.NONE);

		citlabHtrParamCont = new CITlabHtrTrainingConfComposite(paramTabFolder, SWT.NONE);
		final String label = HtrTableLabelProvider.getLabelForHtrProvider(citlabHtrParamCont.getProvider()+ "(Deprecated)");
		citlabHtrTrainingTabItem.setText(label);
		
		citlabHtrTrainingTabItem.setControl(citlabHtrParamCont);
		return new TrainMethodUITab(tabIndex, citlabHtrTrainingTabItem, citlabHtrParamCont);
	}
	
	private TrainMethodUITab createCitlabHtrPlusTrainingTab(final int tabIndex) {
		citlabHtrPlusTrainingTabItem = new CTabItem(paramTabFolder, SWT.NONE);

		citlabHtrPlusParamCont = new CITlabHtrPlusTrainingConfComposite(paramTabFolder, true, SWT.NONE);
		final String label = HtrTableLabelProvider.getLabelForHtrProvider(citlabHtrPlusParamCont.getProvider());
		citlabHtrPlusTrainingTabItem.setText(label);
		
		citlabHtrPlusTrainingTabItem.setControl(citlabHtrPlusParamCont);
		return new TrainMethodUITab(tabIndex, citlabHtrPlusTrainingTabItem, citlabHtrPlusParamCont);
	}
	
	private TrainMethodUITab createPylaiaTrainTabItem(int tabIndex) {
		pylaiaTrainTabItem = new CTabItem(paramTabFolder, SWT.NONE);
		
		pyLaiaTrainingConfComp = new PyLaiaTrainingConfComposite(paramTabFolder, true, SWT.NONE);
		pylaiaTrainTabItem.setText(HtrTableLabelProvider.getLabelForHtrProvider(pyLaiaTrainingConfComp.getProvider()));
		pylaiaTrainTabItem.setControl(pyLaiaTrainingConfComp);
		return new TrainMethodUITab(tabIndex, pylaiaTrainTabItem, pyLaiaTrainingConfComp);		
	}
	
	private TrainMethodUITab createCitlabLaTrainTabItem(int tabIndex) {
		citlabLaTrainTabItem = new CTabItem(paramTabFolder, SWT.NONE);
		
		citlabLaTrainingConfComp = new CITlabLATrainingConfComposite(paramTabFolder, SWT.NONE);
		citlabLaTrainTabItem.setText(HtrTableLabelProvider.getLabelForHtrProvider(citlabLaTrainingConfComp.getProvider()));
		citlabLaTrainTabItem.setControl(citlabLaTrainingConfComp);
		return new TrainMethodUITab(tabIndex, citlabLaTrainTabItem, citlabLaTrainingConfComp);	
	}	
	
	private void setTrainAndValDocsInHtrConfig(HtrTrainConfig config, EditStatus status) throws IOException {
		config.setColId(colId);
		
		DataSetSelection selection = treeViewerSelector.getSelection();
		
		config.setTrain(selection.getTrainDocDescriptorList());
		config.setTest(selection.getValidationDocDescriptorList());
		if(!selection.getTrainGtDescriptorList().isEmpty()) {
			config.setTrainGt(selection.getTrainGtDescriptorList());
		}
		if(!selection.getValidationGtDescriptorList().isEmpty()) {
			config.setTestGt(selection.getValidationGtDescriptorList());
		}
		if (config.getTrain().isEmpty() && CollectionUtils.isEmpty(config.getTrainGt())) {
			throw new IOException("Train set must not be empty!");
		}
		
		if((config.getTest().isEmpty() && CollectionUtils.isEmpty(config.getTestGt())) 
				&& !isCitlabT2ISelected()){
			throw new IOException("Validation set must not be empty! \nAt least one page must be selected to get meaningful error curve."
					+ " Please increase choice of text pages with increasing training pages.");
		}

		if (config.isTestAndTrainOverlapping()) {
			throw new IOException("Train and validation sets must not overlap!");
		}
	}
	
	private <T extends HtrTrainConfig> T createBaseConfig(T configObject, boolean needsLanguage) throws IOException {
		checkBasicConfig(needsLanguage);
		configObject.getModelMetadata().setDescriptions(new HashMap<>());
		configObject.getModelMetadata().getDescriptions().put(Locale.ENGLISH.getLanguage(), descTxt.getText());
		configObject.getModelMetadata().setName(modelNameTxt.getText());
		if (needsLanguage) {
			configObject.getModelMetadata().setLanguage(langEditor.getLanguageString());	
		}
		EditStatus status = treeViewerSelector.getVersionComboStatus().getStatus();
		setTrainAndValDocsInHtrConfig(configObject, status);
		
		//init empty customParams
		ParameterMap customParams = new ParameterMap();
		configObject.setCustomParams(customParams);
		
		return configObject;
	}
	
	private CitLabHtrTrainConfig createCitlabTrainConfig() throws IOException {
		checkCitlabTrainingConfig();
		
		CitLabHtrTrainConfig citlabTrainConf = new CitLabHtrTrainConfig();
		citlabTrainConf = createBaseConfig(citlabTrainConf, true);		
		citlabTrainConf = citlabHtrParamCont.addParameters(citlabTrainConf);		

		return citlabTrainConf;
	}
	
	private CitLabHtrTrainConfig createCitlabHtrPlusTrainConfig() throws IOException {
		checkCitlabPlusTrainingConfig();
		
		CitLabHtrTrainConfig citlabTrainConf = new CitLabHtrTrainConfig();
		citlabTrainConf = createBaseConfig(citlabTrainConf, true);		
		citlabTrainConf = citlabHtrPlusParamCont.addParameters(citlabTrainConf);		

		return citlabTrainConf;
	}
	
	private PyLaiaHtrTrainConfig createPyLaiaTrainConfig() throws IOException {
		checkPyLaiaTrainingConfig();
		
		PyLaiaHtrTrainConfig conf = new PyLaiaHtrTrainConfig();
		conf = createBaseConfig(conf, true);
		conf = pyLaiaTrainingConfComp.addParameters(conf);
		
		return conf;
	}
	
	private CITlabLaTrainConfig createCitlabLaTrainConfig() throws IOException {
		checkCitlabLaTrainingConfig();
		
		CITlabLaTrainConfig conf = new CITlabLaTrainConfig();
		conf = createBaseConfig(conf, false);
		conf = citlabLaTrainingConfComp.addParameters(conf);
		
		return conf;
	}
	
	private CitLabSemiSupervisedHtrTrainConfig createCitlabT2IConfig() throws IOException {
		CitLabSemiSupervisedHtrTrainConfig config = t2iConfComp.getConfig();
		//TODO this part is not tested as t2i is not included here anymore but the checkboxes were changed to a combo in the meantime
		final EditStatus selectedStatus = treeViewerSelector.getVersionComboStatus().getStatus();
		EditStatus status = EditStatus.NEW.equals(selectedStatus) ? EditStatus.NEW : null;
		setTrainAndValDocsInHtrConfig(config, status);
		
		return config;
	}
	
	boolean isCitlabTrainingSelected() {
		return paramTabFolder.getSelection()!=null && paramTabFolder.getSelection().equals(citlabHtrTrainingTabItem);
	}
	
	boolean isCitlabHtrPlusTrainingSelected() {
		return paramTabFolder.getSelection()!=null && paramTabFolder.getSelection().equals(citlabHtrPlusTrainingTabItem);
	}
	
	boolean isPyLaiaTrainingSelected() {
		return paramTabFolder.getSelection()!=null && paramTabFolder.getSelection().equals(pylaiaTrainTabItem);
	}
	
	boolean isCitlabLaTrainingSelected() {
		return paramTabFolder.getSelection()!=null && paramTabFolder.getSelection().equals(citlabLaTrainTabItem);
	}	
	
	boolean isCitlabT2ISelected() {
		return paramTabFolder.getSelection()!=null && paramTabFolder.getSelection().equals(citlabT2ITabItem);
	}
	
	boolean hasSelectedTabLanguage() {
		return isCitlabTrainingSelected() || isCitlabHtrPlusTrainingSelected() || isPyLaiaTrainingSelected();
		
	}

	@Override
	protected void okPressed() {
		citlabTrainConfig = null;
		citlabT2IConf = null;
		pyLaiaConf = null;
		citlabLaConf = null;
		String msg = "";
		try {
			if (isCitlabTrainingSelected()) {
				msg = "You are about to start an HTR Training using CITlab HTR\n\n";
				citlabTrainConfig = createCitlabTrainConfig();
			} else if (isCitlabT2ISelected()) {
				logger.debug("creating citlab t2i config!");
				msg = "You are about to start a Text2Image alignment using CITlab HTR\n\n";
				citlabT2IConf = createCitlabT2IConfig();
			} else if (isCitlabHtrPlusTrainingSelected()){
				msg = "You are about to start an HTR Training using CITlab HTR+\n\n";
				citlabTrainConfig = createCitlabHtrPlusTrainConfig();
			}  else if (isPyLaiaTrainingSelected()) {
				msg = "You are about to start an HTR Training using PyLaia\n\n";
				pyLaiaConf = createPyLaiaTrainConfig();
			}  else if (isCitlabLaTrainingSelected()) {
				msg = "You are about to start a CITlab-LA Training\n\n";
				citlabLaConf = createCitlabLaTrainConfig();
			} 
			else {
				throw new IOException("Invalid method selected - should not happen anyway...");
			}
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			DialogUtil.showErrorMessageBox(getShell(), "Bad configuration", e.getMessage());
			return;
		}
		catch (Exception e) {
			TrpMainWidget.getInstance().onError("Unexpected error", e.getMessage(), e);
			return;
		}

		List<DataSetMetadata> trainSetMd = treeViewerSelector.getTrainSetMetadata();
		List<DataSetMetadata> validationSetMd = treeViewerSelector.getValSetMetadata();
		
		StartTrainingDialog diag = new StartTrainingDialog(this.getShell(), trainSetMd, validationSetMd);
		if (diag.open() == Window.OK) {
			logger.trace("User confirmed dataset selection");
			super.okPressed();
		} else {
			logger.trace("User denied dataset selection");
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Model Training");
		newShell.setMinimumSize(1000, 800);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1400, 1000);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
	
	@Override
	protected void initializeBounds() {
		SWTUtil.centerShell(getShell());
	}

	private void checkCitlabTrainingConfig() throws IOException {
		List<String> errorList = citlabHtrParamCont.validateParameters(new ArrayList<>());
		if (!errorList.isEmpty()) {
			throw new IOException(errorList.stream()
					.collect(Collectors.joining("\n")));
		}
	}
	
	private void checkCitlabPlusTrainingConfig() throws IOException {
		List<String> errorList = citlabHtrPlusParamCont.validateParameters(new ArrayList<>());
		if (!errorList.isEmpty()) {
			throw new IOException(errorList.stream()
					.collect(Collectors.joining("\n")));
		}
	}
	
	private void checkPyLaiaTrainingConfig() throws IOException {
		List<String> errorList = pyLaiaTrainingConfComp.validateParameters(new ArrayList<>());
		if (!errorList.isEmpty()) {
			throw new IOException(errorList.stream()
					.collect(Collectors.joining("\n")));
		}
	}
	
	private void checkCitlabLaTrainingConfig() throws IOException {
		List<String> errorList = citlabLaTrainingConfComp.validateParameters(new ArrayList<>());
		if (!errorList.isEmpty()) {
			throw new IOException(errorList.stream()
					.collect(Collectors.joining("\n")));
		}
	}

	private void checkBasicConfig(boolean needsLanguage) throws IOException {
		List<String> errorList = new ArrayList<>();
		if (StringUtils.isEmpty(modelNameTxt.getText())) {
			errorList.add("Model Name must not be empty!");
		}
		if (StringUtils.isEmpty(descTxt.getText())) {
			errorList.add("Description must not be empty!");
		}
		if (needsLanguage && StringUtils.isEmpty(langEditor.getLanguageString())) {
			errorList.add("Language must not be empty!");
		}
		if (!errorList.isEmpty()) {
			throw new IOException(errorList.stream()
					.collect(Collectors.joining("\n")));
		}
	}

	public CitLabHtrTrainConfig getCitlabTrainConfig() {
		return citlabTrainConfig;
	}
	
	public CitLabSemiSupervisedHtrTrainConfig getCitlabT2IConfig() {
		return citlabT2IConf;
	}
	
	public PyLaiaHtrTrainConfig getPyLaiaConfig() {
		return pyLaiaConf;
	}
	
	public CITlabLaTrainConfig getCitlabLaConfig() {
		return citlabLaConf;
	}	
	
	private class TrainMethodUITab {
		final int tabIndex;
		final CTabItem tabItem;
		final Composite configComposite;
		private TrainMethodUITab(int tabIndex, CTabItem tabItem, Composite configComposite) {
			this.tabIndex = tabIndex;
			this.tabItem = tabItem;
			this.configComposite = configComposite;
		}
		public int getTabIndex() {
			return tabIndex;
		}
		public CTabItem getTabItem() {
			return tabItem;
		}
		public Composite getConfigComposite() {
			return configComposite;
		}
	}

	public void enableDebugDialog(boolean b) {
		this.enableDebugDialog  = b;
		if(treeViewerSelector != null) {
			treeViewerSelector.enableDebugDialog(this.enableDebugDialog);
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		TrpModelMetadata selection = (TrpModelMetadata) evt.getNewValue();
		retrainModel(selection);
	}
	
	private void retrainModel(TrpModelMetadata selection) {
		if (selection == null) {
			return;
		}
		
		TrpModelMetadata model;
		try {
			model = store.getModelDetails(selection);
		} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException
				| NoConnectionException e) {
			DialogUtil.showErrorMessageBox(getShell(), "Could not load model: '" + selection.getName() + "'", e.getMessage());
			return;
		}
		logger.debug("data set selection: pylaia training prepared for start with model = {}", model);
		
//		TreeItem selection = treeViewerSelector.getHtrPagedTree().getTreeViewer().getTree().getSelection()[0];
//		logger.debug("selection " + selection.getText());
//		treeViewerSelector.getHtrPagedTree().getTreeViewer().getTree().update();
//		int selectionIdx = treeViewerSelector.getHtrPagedTree().getTreeViewer().getTree().indexOf(selection);
//		
//		treeViewerSelector.getHtrPagedTree().getTreeViewer().expandToLevel(treeViewerSelector.getHtrPagedTree().getTreeViewer().getTree().getItem(selectionIdx), 2);
//		
//		treeViewerSelector.getHtrPagedTree().getTreeViewer().getTree().getItem(selectionIdx).setExpanded(true);
//		treeViewerSelector.getHtrPagedTree().getTreeViewer().refresh(selection);
//		logger.debug("selectionIdx " + selectionIdx);
		
		treeViewerSelector.getController().removeAllGtFromSelection(); // remove existing GT data!?
		treeViewerSelector.getController().addGtDataSetsOfModel(model);
		modelNameTxt.setText(model.getName());
		descTxt.setText(model.getDescriptions().get(Locale.ENGLISH.getLanguage()));
		langEditor.setLanguageString(model.getLanguage());
	}

}
