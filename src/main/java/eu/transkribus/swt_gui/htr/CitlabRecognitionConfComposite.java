package eu.transkribus.swt_gui.htr;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.rest.JobConst;
import eu.transkribus.swt.util.SWTUtil;

public class CitlabRecognitionConfComposite extends Composite implements SelectionListener {
	private final static Logger logger = LoggerFactory.getLogger(CitlabRecognitionConfComposite.class);
	
	Button doLinePolygonSimplificationBtn, keepOriginalLinePolygonsBtn, doStoreConfMatsBtn;
	StructureTagComposite structureTagComp;
	RecentModelsCombo recentModelsCombo;
	List<String> selectionArray = new ArrayList<>();

	public CitlabRecognitionConfComposite(Composite parent, HtrTextRecognitionDialog htrRecognition) {
		super(parent, 0);
		this.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		
		doLinePolygonSimplificationBtn = new Button(this, SWT.CHECK);
		doLinePolygonSimplificationBtn.setText("Do polygon simplification");
		doLinePolygonSimplificationBtn.setToolTipText("Perform a line polygon simplification after the recognition process to reduce the number of points");
		doLinePolygonSimplificationBtn.setSelection(true);
		doLinePolygonSimplificationBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		keepOriginalLinePolygonsBtn = new Button(this, SWT.CHECK);
		keepOriginalLinePolygonsBtn.setText("Keep original line polygons");
		keepOriginalLinePolygonsBtn.setToolTipText("Keep the original line polygons after the recognition process, e.g. if they have been already corrected");
		keepOriginalLinePolygonsBtn.setSelection(false);
		keepOriginalLinePolygonsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		doStoreConfMatsBtn = new Button(this, SWT.CHECK);
		doStoreConfMatsBtn.setText("Enable Keyword Spotting");
		doStoreConfMatsBtn.setToolTipText("The internal recognition result respresentation, needed for keyword spotting, will be stored in addition to the transcription.");
		doStoreConfMatsBtn.setSelection(false);
		doStoreConfMatsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		SWTUtil.onSelectionEvent(keepOriginalLinePolygonsBtn, e -> {
			doLinePolygonSimplificationBtn.setEnabled(!keepOriginalLinePolygonsBtn.getSelection());
		});
		doLinePolygonSimplificationBtn.setEnabled(!keepOriginalLinePolygonsBtn.getSelection());		
		
		structureTagComp = new StructureTagComposite(this);
		structureTagComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		recentModelsCombo = new RecentModelsCombo(this);
		recentModelsCombo.getCombo().addSelectionListener(htrRecognition);
		recentModelsCombo.getCombo().addSelectionListener(this);
		
	}
	
	public RecentModelsCombo getRecentModelsCombo() {
		return recentModelsCombo;
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void widgetSelected(SelectionEvent arg0) {
		Properties props = recentModelsCombo.getJobProperties();
		
		logger.debug("isDoLinePolygonSimplification "+Boolean.valueOf(props.getProperty(JobConst.PROP_DO_LINE_POLYGON_SIMPLIFICATION)));
		logger.debug("isKeepOriginalLinePolygons "+Boolean.valueOf(props.getProperty(JobConst.PROP_KEEP_ORIGINAL_LINE_POLYGONS)));
		logger.debug("isDoStoreConfMats "+Boolean.valueOf(props.getProperty(JobConst.PROP_DO_STORE_CONFMATS)));
		
		doLinePolygonSimplificationBtn.setSelection(Boolean.valueOf(props.getProperty(JobConst.PROP_DO_LINE_POLYGON_SIMPLIFICATION)));
		doStoreConfMatsBtn.setSelection(Boolean.valueOf(props.getProperty(JobConst.PROP_DO_STORE_CONFMATS)));
		keepOriginalLinePolygonsBtn.setSelection(Boolean.valueOf(props.getProperty(JobConst.PROP_KEEP_ORIGINAL_LINE_POLYGONS)));
		
	}

}
