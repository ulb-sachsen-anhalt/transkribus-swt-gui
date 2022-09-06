package eu.transkribus.swt_gui.htr;

import java.util.Properties;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.core.rest.JobConst;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.util.TextRecognitionConfig;

public class PyLaiaRecognitionConfComposite extends Composite implements SelectionListener {
	private final static Logger logger = LoggerFactory.getLogger(PyLaiaRecognitionConfComposite.class);
	
	Button doLinePolygonSimplificationBtn;
//	Button clearLinesBtn;
	Button doWordSegBtn;
	LabeledText batchSizeText;
	
	Button useExistingLinePolygonsBtn;
	Button useComputedLinePolygonsBtn;
	StructureTagComposite structureTagComp;
	RecentModelsCombo recentModelsCombo;
	
	Composite kwsComposite;
	Button writeKwsIndexFiles;
	LabeledText nBest;

	HtrTextRecognitionDialog htrRecognitionDialog;

	public PyLaiaRecognitionConfComposite(Composite parent, HtrTextRecognitionDialog htrRecognitionDialog) {
		super(parent, 0);
		this.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		
		this.htrRecognitionDialog = htrRecognitionDialog;
		
		useComputedLinePolygonsBtn= new Button(this, SWT.RADIO);
		useComputedLinePolygonsBtn.setText("Compute line polygons");
		useComputedLinePolygonsBtn.setToolTipText("Line polygons are automatically computed from baselines - the existing ones are deleted!");
		useComputedLinePolygonsBtn.setSelection(true);
		useComputedLinePolygonsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		SWTUtil.onSelectionEvent(useComputedLinePolygonsBtn, e -> updateUi());
		
		useExistingLinePolygonsBtn = new Button(this, SWT.RADIO);
		useExistingLinePolygonsBtn.setText("Use existing line polygons");
		useExistingLinePolygonsBtn.setToolTipText("Do *not* perform a baseline to polygon computation but use the existing line polygons.\nUse this if you have exact line polygons e.g. from an OCR engine.");
		useExistingLinePolygonsBtn.setSelection(false);
		useExistingLinePolygonsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		SWTUtil.onSelectionEvent(useExistingLinePolygonsBtn, e -> updateUi());
		
		doLinePolygonSimplificationBtn = new Button(this, SWT.CHECK);
		doLinePolygonSimplificationBtn.setText("Do polygon simplification");
		doLinePolygonSimplificationBtn.setToolTipText("Perform a line polygon simplification after the recognition process to reduce the number of points");
		doLinePolygonSimplificationBtn.setSelection(true);
		doLinePolygonSimplificationBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
//		clearLinesBtn = new Button(this, SWT.CHECK);
//		clearLinesBtn.setText("Clear lines");
//		clearLinesBtn.setToolTipText("Clear existing transcriptions before recognition");
//		clearLinesBtn.setSelection(true);
//		clearLinesBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));	
		
		doWordSegBtn = new Button(this, SWT.CHECK);
		doWordSegBtn.setText("Add estimated word coordinates");
		doWordSegBtn.setToolTipText("Adds approximate bounding boxes for the recognized words inside the lines");
		doWordSegBtn.setSelection(true);
		doWordSegBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		kwsComposite = new Composite(this, 0);
		kwsComposite.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		kwsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		writeKwsIndexFiles = new Button(kwsComposite, SWT.CHECK);
		writeKwsIndexFiles.setText("Enable Keyword Spotting (experimental) ");
		writeKwsIndexFiles.setToolTipText("The internal recognition result respresentation, needed for keyword spotting, will be stored in addition to the transcription.");
		writeKwsIndexFiles.setSelection(false);
		SWTUtil.onSelectionEvent(writeKwsIndexFiles, e -> {
			nBest.setEnabled(writeKwsIndexFiles.getSelection());
		});
//		writeKwsIndexFiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		nBest = new LabeledText(kwsComposite, "N-Best: ");
		nBest.setText(""+75);
		nBest.setEnabled(writeKwsIndexFiles.getSelection());
		nBest.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		nBest.setToolTipText("How many best paths should be taken into account? The more, the higher the processing time and space requirement...");
		
		batchSizeText = new LabeledText(this, "Batch size: ");
		batchSizeText.setText(""+10);
		batchSizeText.setToolTipText("Number of lines that are simultaneously decoded - if you get a memory error, decrease this value");
		batchSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));	
		
		structureTagComp = new StructureTagComposite(this);
		structureTagComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		recentModelsCombo = new RecentModelsCombo(this);
		recentModelsCombo.getCombo().addSelectionListener(htrRecognitionDialog);
		recentModelsCombo.getCombo().addSelectionListener(this);
		
		updateUi();
	}
	
	void updateUi() {
		if (useExistingLinePolygonsBtn.getSelection()) {
			doLinePolygonSimplificationBtn.setSelection(false);
		}
		else {
			doLinePolygonSimplificationBtn.setSelection(true);
		}
		boolean isAdminLoggedIn = Storage.getInstance()!=null && Storage.getInstance().isAdminLoggedIn();
		if (!isAdminLoggedIn) {
			batchSizeText.setParent(SWTUtil.dummyShell);
		}
		else {
			batchSizeText.setParent(this);
			batchSizeText.moveBelow(kwsComposite);
		}
		
		boolean isUserAllowedForKws = Storage.i().isUserAllowedForJob(JobImpl.PyLaiaKwsDecodingJob.toString(), false);
		boolean hasSelectedModelLM = htrRecognitionDialog.getConfig()!=null && htrRecognitionDialog.getConfig().getLanguageModel()!=null;		
		if (!isUserAllowedForKws || !hasSelectedModelLM) {
			kwsComposite.setParent(SWTUtil.dummyShell);
			writeKwsIndexFiles.setSelection(false);
		}
		else {
			kwsComposite.setParent(this);
			kwsComposite.moveAbove(doWordSegBtn);
		}
		
	}

	public RecentModelsCombo getRecentModelsCombo() {
		return recentModelsCombo;
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
	}

	@Override
	public void widgetSelected(SelectionEvent arg0) {
		Properties props = recentModelsCombo.getJobProperties();
		
		boolean useExistentLines = Boolean.valueOf(props.getProperty(JobConst.PROP_USE_EXISTING_LINE_POLYGONS));
		
		logger.debug("selection event handled in Pylaia recognition");
		logger.debug("isDoLinePolygonSimplification "+Boolean.valueOf(props.getProperty(JobConst.PROP_DO_LINE_POLYGON_SIMPLIFICATION)));
		logger.debug("useExistingLinePolygonsBtn "+useExistentLines);
		logger.debug("useComputedLinePolygonsBtn "+!useExistentLines);
		
		doLinePolygonSimplificationBtn.setSelection(Boolean.valueOf(props.getProperty(JobConst.PROP_DO_LINE_POLYGON_SIMPLIFICATION)));
		useExistingLinePolygonsBtn.setSelection(useExistentLines);
		useComputedLinePolygonsBtn.setSelection(!useExistentLines);
		
		
	}

}
