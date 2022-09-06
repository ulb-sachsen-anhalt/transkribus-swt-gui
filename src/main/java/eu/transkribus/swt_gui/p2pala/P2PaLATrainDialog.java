package eu.transkribus.swt_gui.p2pala;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
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
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpP2PaLA;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.model.beans.rest.P2PaLATrainJobPars;
import eu.transkribus.core.rest.JobConst;
import eu.transkribus.core.rest.JobConstP2PaLA;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.EnumUtils;
import eu.transkribus.core.util.GsonUtil;
import eu.transkribus.core.util.MonitorUtil;
import eu.transkribus.core.util.StructTypesAnal;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.ComboInputDialog;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.LabeledCombo;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.DocsSelectorBtn;

public class P2PaLATrainDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(P2PaLATrainDialog.class);
	
	// FIXME: use P2PaLATrainJobPars instead of this helper class already inside this dialog class...
	public static class P2PaLATrainUiConf {
		public String name=null;
		public String description=null;
		public int numEpochs=150;
		public String structTypes;
		public String mergedStructTypes;
		public String outMode=TrpP2PaLA.OUT_MODE_REGIONS_ONLY;
		
		public EditStatus editStatus;
		public boolean skipPagesWithMissingStatus=false;
		
		public List<DocSelection> trainDocs=new ArrayList<>(), valDocs=new ArrayList<>(), testDocs=new ArrayList<>();
//		public double[] fracs;
		public String fracs;
		
		public void setStructTypes(String text) {
			if (!StringUtils.isEmpty(text)) {
				this.structTypes = StringUtils.join(Arrays.asList(text.split("\\s+")).stream().distinct().sorted().collect(Collectors.toList()), " ");
			}
		}
		
		public boolean isStruct(String s) {
			return StringUtils.isEmpty(structTypes) ? false : Arrays.asList(structTypes.split("\\s+")).contains(s);
		}		
		
		public void setMergedStructTypes(String text) throws Exception {
			if (StringUtils.isEmpty(text.trim())) {
				mergedStructTypes = null;
				return;
			}
			
			mergedStructTypes = "";
			for (String s : text.trim().split("\\s+")) {
				logger.trace("s: "+s);
				s = s.trim();
				String[] s12 = s.split(":");
				if (s12.length != 2) {
					throw new Exception("Not a valid merged structure type specificiation: "+s);
				}
//				for (int i=0; i<2; ++i) {
//					if (!isStruct(s12[i])) {
//						logger.debug("Invalid merged structure types - struct not specified: "+s12[i]);
//						throw new Exception("Invalid merged structure types - struct not specified: "+s12[i]);
//					}
//				}
				if (StringUtils.equals(s12[0], s12[1])) {
					throw new Exception("Cannot merge equal structures: '"+s+"'");
				}
				
				if (!mergedStructTypes.contains(s)) {
					mergedStructTypes += s+" ";
				}
			}
			mergedStructTypes = mergedStructTypes.trim();
		}
		
		public boolean withBaselines() {
			return StringUtils.contains(outMode, "L");
		}
		
		public boolean withRegions() {
			return StringUtils.contains(outMode, "R");
		}
		
		public String getTrainingModeVerbose() {
			return StringUtils.removeEnd(((withBaselines() ? "Baselines, " : "") + (withRegions() ? "Regions" : "")).trim(), ",");
		}
		
		public P2PaLATrainJobPars toP2PaLATrainJobPars() {
			P2PaLATrainJobPars pars = new P2PaLATrainJobPars();
			pars.setTrainDocs(trainDocs);
			pars.setValDocs(valDocs);
			pars.setTestDocs(testDocs);
			
			pars.getParams().addParameter(JobConst.PROP_MODELNAME, name);
			if (!StringUtils.isEmpty(description)) {
				pars.getParams().addParameter(JobConst.PROP_DESCRIPTION, description);
			}
			if (numEpochs > 0) {
				pars.getParams().addIntParam(JobConst.PROP_NUM_EPOCHS, numEpochs);
			}
			if (!StringUtils.isEmpty(structTypes)) {
				pars.getParams().addParameter(JobConstP2PaLA.PROP_REGIONS, structTypes);
			}
			if (!StringUtils.isEmpty(mergedStructTypes)) {
				pars.getParams().addParameter(JobConstP2PaLA.PROP_MERGED_REGIONS, mergedStructTypes);
			}
			
			pars.getParams().addParameter(JobConstP2PaLA.PROP_OUT_MODE, outMode);
			pars.getParams().addParameter(JobConst.PROP_EDIT_STATUS, editStatus);
			pars.getParams().addBoolParam(JobConst.PROP_SKIP_PAGES_WITH_MISSING_STATUS, skipPagesWithMissingStatus);
			
			if (!StringUtils.isEmpty(fracs)) {
				pars.getParams().addParameter(JobConstP2PaLA.PROP_SPLIT_FRACTIONS, fracs);
			}
			
			return pars;
		}
		
		@Override
		public String toString() {
			return "P2PaLATrainUiConf [name=" + name + ", description=" + description + ", numEpochs=" + numEpochs
					+ ", structTypes=" + structTypes + ", mergedStructTypes=" + mergedStructTypes + ", outMode="
					+ outMode + ", editStatus=" + editStatus + ", skipPagesWithMissingStatus="
					+ skipPagesWithMissingStatus + ", trainDocs=" + trainDocs + ", valDocs=" + valDocs + ", testDocs="
					+ testDocs + ", fracs=" + fracs + "]";
		}


	}
	
	LabeledText modelNameText, descriptionText, numEpochsText, structureTypesText, mergedStructureTypesText; 
//	CurrentTranscriptOrDocPagesOrCollectionSelector trainSetSelector, valSetSelector, testSetSelector;
	
	DocsSelectorBtn trainSel, valSel, testSel;
	Button autoSplitTrainSetCheck, skipPagesWithMissingStatusCheck, analStructTypesBtn;
	LabeledCombo statusCombo, outModeCombo;
	
	Text structTypeAnalText;
	
	P2PaLATrainUiConf conf = new P2PaLATrainUiConf();
	
//	Label infoLabel;
	Composite fractionsComp;
	LabeledText trainFrac, valFrac, testFrac;
	
	static final String[][] outModes = {
			{"Regions only", TrpP2PaLA.OUT_MODE_REGIONS_ONLY}, 
			{"Lines only", TrpP2PaLA.OUT_MODE_LINES_ONLY},
			{"Region and Lines", TrpP2PaLA.OUT_MODE_LINES_AND_REGIONS},
	};

	public P2PaLATrainDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Point getInitialSize() {
		int minY = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return new Point(700, minY+100);
//		return new Point(500, Math.min(getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).y, 700));
	}
	
	@Override
	protected boolean isResizable() {
	    return true;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
		
		Button helpBtn = createButton(parent, IDialogConstants.HELP_ID, "Help", false);
		helpBtn.setImage(Images.HELP);
		SWTUtil.onSelectionEvent(helpBtn, e -> {
			org.eclipse.swt.program.Program.launch("https://readcoop.eu/transkribus/howto/how-to-use-the-structural-tagging-feature-and-how-to-train-it");
		});
		
//		Button modelDetailsBtn = createButton(parent, IDialogConstants.HELP_ID, "Model info", false);
//		modelDetailsBtn.setImage(Images.INFO);
//		SWTUtil.onSelectionEvent(modelDetailsBtn, e -> {
//			P2PaLAModelDetailsDialog d = new P2PaLAModelDetailsDialog(getShell(), models);
//			d.open();
//		});	
		
	    Button runBtn = createButton(parent, IDialogConstants.OK_ID, "Train", false);
	    runBtn.setImage(Images.ARROW_RIGHT);
	    
//	    onSelectedModelChanged();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("P2PaLA training");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
		
		modelNameText = new LabeledText(cont, "Name: ");
		modelNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		descriptionText = new LabeledText(cont, "Description: (optional)");
		descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		numEpochsText = new LabeledText(cont, "Number of epochs: ");
		numEpochsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		numEpochsText.setText(""+150);
		numEpochsText.setToolTipText("The number of epochs the training should run");

		Composite structsComp = new Composite(cont, 0);
		structsComp.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		structsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		structureTypesText = new LabeledText(structsComp, "Structures: ");
		structureTypesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		structureTypesText.setToolTipText("A list of all structure types to be trained separated by whitespaces\n"
				+ "E.g.: 'paragraph heading footnote page-number'");
		
		Button addStructBtn = new Button(structsComp, 0);
		addStructBtn.setImage(Images.ADD);
		addStructBtn.setToolTipText("Add a structure type");
		SWTUtil.onSelectionEvent(addStructBtn, e -> {
			String[] items = Storage.i().getStructCustomTagSpecsTypeStrings().toArray(new String[0]);
			ComboInputDialog d = new ComboInputDialog(getShell(), "Specify a structure: ", items, SWT.DROP_DOWN, true);
			d.setValidator(new IInputValidator() {
				@Override public String isValid(String arg) {
					if (StringUtils.containsWhitespace(arg)) {
						return "No spaces allowed in structure types!";
					}
					return null;
				}
			});
			if (d.open() == Dialog.OK) {
				structureTypesText.setText((structureTypesText.getText()+" "+d.getSelectedText()).trim());
			}
		});
		
		Composite mergedStructsComp = new Composite(cont, 0);
		mergedStructsComp.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		mergedStructsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		mergedStructureTypesText = new LabeledText(mergedStructsComp, "Merged structures: (optional)");
		mergedStructureTypesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mergedStructureTypesText.setToolTipText("A list of all structure types to be merged into others.\n"
				+ "E.g.: 'footnote:footnote-continued,footer heading:header' means that 'footnote-continue' and 'footnote' are regarded as 'footnote' while 'header' is regarded as 'heading'");
		
		Button addMergedStructBtn = new Button(mergedStructsComp, 0);
		addMergedStructBtn.setImage(Images.ADD);
		addMergedStructBtn.setToolTipText("Add a structure type to merge");
		
		SWTUtil.onSelectionEvent(addMergedStructBtn, e -> {
			String[] types = getCurrentStructTypes();
			P2PaLAAddMergedStructDiag d = new P2PaLAAddMergedStructDiag(getShell(), types);
			if (d.open() == Dialog.OK) {
				mergedStructureTypesText.setText((mergedStructureTypesText.getText()+" "+d.getConfig()).trim());
			}
		});		
		
		outModeCombo = new LabeledCombo(cont, "Training mode: ");
		outModeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		outModeCombo.getCombo().add(outModes[0][0]);
		outModeCombo.getCombo().add(outModes[1][0]);
		outModeCombo.getCombo().add(outModes[2][0]);
		outModeCombo.getCombo().select(0);
		SWTUtil.onSelectionEvent(outModeCombo.getCombo(), e -> updateUi());
		
		Composite statusComp = new Composite(cont, 0);
		statusComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		statusComp.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		
		statusCombo = new LabeledCombo(statusComp, "Edit Status: ");
		List<String> stati = EnumUtils.stringsList(EditStatus.class);
		stati.add(0, "");
		statusCombo.setItems(stati.toArray(new String[0]));
		statusCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		statusCombo.setToolTipText("Use versions with this edit status for matching.\nIf empty, current version is used");
		statusCombo.getCombo().setText("");
		statusCombo.getCombo().select(0);

		skipPagesWithMissingStatusCheck = new Button(statusComp, SWT.CHECK);
		skipPagesWithMissingStatusCheck.setText("Skip pages with missing status");
		skipPagesWithMissingStatusCheck.setToolTipText("If a status is selected and it is not set for any of the pages in the train/val/test set, those page will be skipped");
		skipPagesWithMissingStatusCheck.setSelection(false);
		SWTUtil.onSelectionEvent(skipPagesWithMissingStatusCheck, e -> updateUi());
		
		trainSel = new DocsSelectorBtn(cont, "Training set: ");
		trainSel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		trainSel.setToolTipText("The training used for training this model. This is mandatory.");
		
		valSel = new DocsSelectorBtn(cont, "Validation set: (optional) ");
		valSel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		valSel.setToolTipText("The validation set used *during* each epoch of training to avoid overfitting to the training data.\n"
				+ "Not mandatory but very much recommended!");
		
//		testSel = new DocsSelectorBtn(cont, "Test set: (optional) ");
//		testSel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		testSel.setToolTipText("The test set is used *after* the training to measure the quality of the overall model.\n"
//				+ "This is not mandatory if you are not interested in an overall quality measure of your model.");
		
		autoSplitTrainSetCheck = new Button(cont, SWT.CHECK);
		autoSplitTrainSetCheck.setText("Split train set randomly");
		autoSplitTrainSetCheck.setToolTipText("Perform a random split of the training set into validation and testset according to the given fractions");
		autoSplitTrainSetCheck.setSelection(true);
		SWTUtil.onSelectionEvent(autoSplitTrainSetCheck, e -> updateUi());		
		
		fractionsComp = new Composite(cont, 0);
		fractionsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fractionsComp.setLayout(new GridLayout(3, false));
		trainFrac = new LabeledText(fractionsComp, "Train: ");
		trainFrac.setText("90");
		trainFrac.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		trainFrac.setToolTipText("Percentage of the data that is used as training set");
		
		valFrac = new LabeledText(fractionsComp, "Val: ");
		valFrac.setText("10");
		valFrac.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		valFrac.setToolTipText("Percentage of the data that is used as validation set");
		
//		testFrac = new LabeledText(fractionsComp, "Test: ");
//		testFrac.setText("0");
//		testFrac.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		testFrac.setToolTipText("Percentage of the data that is used as test set");
		
		analStructTypesBtn = new Button(cont, 0);
		analStructTypesBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		analStructTypesBtn.setText("Analyze structure types");
		analStructTypesBtn.setToolTipText("Parses structure type information on the current train- & val- & test-set");
		SWTUtil.onSelectionEvent(analStructTypesBtn, e -> {
			structTypeAnalText.setText("");
			StructTypesAnal anal = parseStrucutreTypesAnal();
			if (anal != null) {
				String analText = "Structure types:\n";
				analText += "\t"+anal.getStructTypesStrForP2PaLA()+"\n";
				analText += "Counts: \n";
				analText += "\t"+anal.getCounts();
				structTypeAnalText.setText(analText);
			}
		});
		
		structTypeAnalText = new Text(cont, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
		structTypeAnalText.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		updateUi();
		
		return cont;
	}
	
	private String[] getCurrentStructTypes() {
		return Arrays.asList(structureTypesText.getText().split("\\s+")).stream().distinct().sorted().toArray(String[]::new);
	}
	
	private String parseOutMode() {
		return outModes[outModeCombo.getCombo().getSelectionIndex()][1];
	}
	
	private EditStatus parseEditStatus() {
		String comboText = statusCombo.getCombo().getText();
		if (!StringUtils.isEmpty(comboText)) {
			return EditStatus.fromString(comboText);
//			try {
//				return EditStatus.fromString(comboText);
//			} catch (Exception e) {
//				DialogUtil.showErrorMessageBox(getShell(), "Invalid Edit Status", "Invalid Edit Status: "+comboText+" - skipping!");
//				editStatus = null;
//			}		
		}
		return null;
	}
	
	private List<DocSelection> getAllDocSelections() {
		List<DocSelection> docs = new ArrayList<>();
		docs.addAll(trainSel.getDocSelection());
		docs.addAll(valSel.getDocSelection());
		if (testSel != null) {
			docs.addAll(testSel.getDocSelection());	
		}
		return docs;
	}
	
	private StructTypesAnal parseStrucutreTypesAnal() {
		Storage store = Storage.getInstance();
		if (!store.isLoggedIn()) {
			DialogUtil.showErrorMessageBox(getShell(), "Not logged in", "Please login to our server to use this feature!");
			return null;
		}
		
		List<DocSelection> docs = getAllDocSelections();
		if (CoreUtils.isEmpty(docs)) {
			DialogUtil.showErrorMessageBox(getShell(), "No documents", "Please selects some documents for train/val/test set!");
			return null;
		}
		
		try {
			StructTypesAnal anal = new StructTypesAnal();
			anal.setWithBaselines(true);
			EditStatus editStatus = parseEditStatus();
			boolean skipPagesWithMissingStatus = skipPagesWithMissingStatusCheck.getSelection();
			
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					MonitorUtil.beginTask(monitor, "Retrieving pages for "+docs.size()+" document selections...", docs.size());
					
					List<TrpPage> pages = new ArrayList<>();
					int i=0;
					for (DocSelection ds : docs) {
						if (MonitorUtil.isCanceled(monitor)) {
							throw new InterruptedException();
						}
						MonitorUtil.subTask(monitor, (i+1)+"/"+docs.size());
						
						try {
							if (true) {
								// NEW CODE
								logger.debug("getting doc "+ds.getDocId()+"pagesStr = "+ds.getPages()+", editStatus = "+editStatus+", skipPagesWithMissingStatus = "+skipPagesWithMissingStatus);
								TrpDoc doc = store.getConnection().getTrpDoc(store.getCollId(), ds.getDocId(), -1);
								doc.filterPagesByPagesStrAndEditStatus(ds.getPages(), editStatus, skipPagesWithMissingStatus);
								pages.addAll(doc.getPages());
							}
							else {
								// OLD CODE
								List<TrpPage> pagesForDocs = store.getConnection().getTrpPagesByPagesStr(store.getCollId(), 
										ds.getDocId(), ds.getPages(), editStatus, skipPagesWithMissingStatus);
								logger.debug("got "+pagesForDocs.size()+" pages for doc "+ds.getDocId());
								pages.addAll(pagesForDocs);
							}
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						}
						
						MonitorUtil.worked(monitor, ++i);
					}
					
					try {
						anal.setPages(pages);
						anal.analyzeStructureTypes(monitor);
						if (MonitorUtil.isCanceled(monitor)) {
							throw new InterruptedException();
						}
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			}, "", true);
			return anal;
		}
		catch (InterruptedException ie) {
			logger.debug("cancelled...");
			return null;
		}
		catch (Throwable e) {
			DialogUtil.showErrorMessageBox2(getShell(), "Error", e.getMessage(), e);
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
	private void updateUi() {
		fractionsComp.setEnabled(autoSplitTrainSetCheck.getSelection());
		SWTUtil.setEnabled(trainFrac, autoSplitTrainSetCheck.getSelection());
		SWTUtil.setEnabled(valFrac, autoSplitTrainSetCheck.getSelection());
		SWTUtil.setEnabled(testFrac, autoSplitTrainSetCheck.getSelection());
		
		SWTUtil.setEnabled(valSel, !autoSplitTrainSetCheck.getSelection());
		SWTUtil.setEnabled(testSel, !autoSplitTrainSetCheck.getSelection());
		
		structureTypesText.setEnabled(outModeCombo.getCombo().getSelectionIndex()!=1);
		mergedStructureTypesText.setEnabled(outModeCombo.getCombo().getSelectionIndex()!=1);
	}
	
	private double parseFraction(String percText) throws NumberFormatException {
		int perc = Integer.parseInt(percText);
		if (perc < 0 || perc > 100) {
			throw new NumberFormatException("Percentage must be between 0 and 100");
		}
		
		return (double)perc/100.0d;
	}
	
	private void storeConf() throws Exception {
		P2PaLATrainUiConf conf = new P2PaLATrainUiConf();
		conf.name = modelNameText.getText();
		if (StringUtils.length(conf.name)<3) {
			throw new Exception("Model name must be at least 3 characters!");
		}
		conf.description = descriptionText.getText();
		try {
			conf.numEpochs = Integer.parseInt(numEpochsText.getText());
		} catch (Exception e) {
			throw new Exception("Could not parse number of epochs: "+e.getMessage(), e);
		}
		
		conf.outMode = parseOutMode();
		logger.debug("outMode = "+conf.outMode);
		
		conf.setStructTypes(structureTypesText.getText().trim());
		if (StringUtils.isEmpty(conf.structTypes) && !conf.outMode.equals(TrpP2PaLA.OUT_MODE_LINES_ONLY)) {
			throw new Exception("No structure types provided for training with regions!");
		}
		
		// parse validity of mergedStructTypes syntax, i.e. s1:s2,s3,s4 s5:s6
		conf.setMergedStructTypes(mergedStructureTypesText.getText());
		
		conf.trainDocs = trainSel.getDocSelection();
		if (conf.trainDocs==null || conf.trainDocs.isEmpty()) {
			throw new Exception("Training set cannot be empty!");
		}
		
		if (autoSplitTrainSetCheck.getSelection()) {
			List<Double> fracList = new ArrayList<>();
			double trainFracD = parseFraction(trainFrac.getText()); 
			fracList.add(trainFracD);
			double valFracD = parseFraction(valFrac.getText());
			if (valFracD<=0.0d) {
				throw new NumberFormatException("Validation fraction must be greater 0!");
			}
			fracList.add(valFracD);
			double testFracD = testFrac == null ? 0 : parseFraction(testFrac.getText());
			if (testFracD > 0) {
				fracList.add(testFracD);
			}
			
//			double[] fracs = new double[3];
//			fracs[0] = parseFraction(trainFrac.getText());
//			fracs[1] = parseFraction(valFrac.getText());
//			fracs[2] = parseFraction(testFrac.getText());
			
			conf.fracs = GsonUtil.toJson(fracList);
			logger.debug("parsed frace = "+conf.fracs);
		} else {
			conf.fracs = null;
			
			conf.valDocs = valSel.getDocSelection();
			if (CoreUtils.isEmpty(conf.valDocs)) {
				if (DialogUtil.showYesNoDialog(getShell(), "Empty validation set", "No validation set selected - do you still want to continue?") == SWT.NO) {
					this.conf = null;
					return;
				}
			}
			if (testSel != null) {
				conf.testDocs = testSel.getDocSelection();	
			}
		}
		
		conf.editStatus = parseEditStatus();
		conf.skipPagesWithMissingStatus = skipPagesWithMissingStatusCheck.getSelection();
		
		this.conf = conf;
		logger.debug("conf = "+this.conf);
	}	
	
	private List<String> splitOnWsSortedDistinct(String s) {
		if (!StringUtils.isEmpty(s)) {
			return Arrays.asList(s.split("\\s+")).stream().distinct().sorted().collect(Collectors.toList());
		}
		else {
			return new ArrayList<>();
		}
	}
	
	@Override
	protected void okPressed() {
		try {
			storeConf();
			if (this.conf != null) {
				String msgDetail="";
				Storage s = Storage.i();
				
				if (s!=null && s.getCollection(s.getCollId())!=null) {
					TrpCollection c = s.getCollection(s.getCollId());
					msgDetail+="\n\tTraining "+conf.getTrainingModeVerbose();
					if (conf.withRegions()) {
						msgDetail+="\n\tStruct types: "+CoreUtils.join(splitOnWsSortedDistinct(conf.structTypes), ", ", "'", "'");
						if (!StringUtils.isEmpty(conf.mergedStructTypes)) {
							msgDetail+="\n\tMerged structs: "+CoreUtils.join(splitOnWsSortedDistinct(conf.mergedStructTypes), ", ", "'", "'");	
						}
					}
					msgDetail+="\n\tCollection: "+c.getColId()+", "+c.getColName();
					msgDetail+="\n\tModel owner: "+s.getUserName();
				}
				
				if (DialogUtil.showYesNoDialog(getShell(), "Starting training...", 
						"Do you really want to start the training job?"+msgDetail) == SWT.YES) {
					super.okPressed();	
				}
				else {
					this.conf = null;
				}
			}
		} catch (Exception e) {
			this.conf = null;
			logger.error(e.getMessage(), e);
			DialogUtil.showErrorMessageBox(getShell(), "Unable to parse configuration", e.getMessage());
			return;
		}
	}

	public P2PaLATrainUiConf getConf() {
		return this.conf;
	}
	
	public P2PaLATrainJobPars getTrainJobPars() {
		return conf != null ? conf.toP2PaLATrainJobPars() : null;
	}

}
