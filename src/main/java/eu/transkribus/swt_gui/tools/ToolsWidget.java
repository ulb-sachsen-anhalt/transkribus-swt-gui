package eu.transkribus.swt_gui.tools;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.internal.SWTEventListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.dialogs.ChooseTranscriptDialog;
import eu.transkribus.swt_gui.htr.ModelTrainingComposite;
import eu.transkribus.swt_gui.htr.TextRecognitionComposite;
import eu.transkribus.swt_gui.la.LayoutAnalysisComposite;
import eu.transkribus.swt_gui.la.Text2ImageSimplifiedConfComposite.Text2ImageConf;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrCurrentDocPagesSelector;

public class ToolsWidget extends Composite {
	private final static Logger logger = LoggerFactory.getLogger(ToolsWidget.class);
	
	Composite mdGroup;
	SWTEventListener listener=null;
	
	LayoutAnalysisComposite laComp;
	Button startLaBtn;
	
	TextRecognitionComposite trComp;
	ModelTrainingComposite modelTrComp;
	
	Button polygon2baselinesBtn, baseline2PolygonBtn, p2palaBtn, p2palaTrainBtn, duButton;
	CurrentTranscriptOrCurrentDocPagesSelector otherToolsPagesSelector;
	
	Button t2iBtn;
		
//	Image ncsrIcon = Images.getOrLoad("/NCSR_icon.png");
//	Label ncsrIconLbl;

	TranscriptVersionChooser refVersionChooser, hypVersionChooser;
	
	Button computeWerBtn,computeAdvancedBtn,compareSamplesBtn;
	Button compareVersionsBtn;
	Composite werGroup;
	ExpandableComposite werExp, laToolsExp, expRecog, expOther, modelTrainingExp;
	
	Composite container; // this is the base container, where all expandable composite are put into
	Composite otherToolsContainer, p2palaContainer, t2iContainer, duContainer, otherOtherToolsGroup;
	ScrolledComposite sc;
	
	/*
	 * This can be safely removed when Error Rate tool integration is done.
	 */
	public final static boolean IS_LEGACY_WER_GROUP = false;
	
	public static class TranscriptVersionChooser extends Composite {
		public Button useCurrentBtn;
		public Button chooseVersionBtn;
		
		public TrpTranscriptMetadata selectedMd;

		public TranscriptVersionChooser(String label, Composite parent, int style) {
			super(parent, style);
			
			this.setLayout(new GridLayout(4, false));

			Label l = new Label(this, 0);
			l.setText(label);
			
			chooseVersionBtn = new Button(this, SWT.PUSH);
			chooseVersionBtn.setText("Choose...");
			chooseVersionBtn.setToolTipText("Click to choose another transcript version...");
			
			useCurrentBtn = new Button(this, SWT.PUSH);
			useCurrentBtn.setText("Use current");
			useCurrentBtn.setToolTipText("Click to use the currently opened transcript version");
			
			useCurrentBtn.addSelectionListener(new SelectionAdapter() {
				@Override public void widgetSelected(SelectionEvent e) {
					setToCurrent();
				}
			});
			
			chooseVersionBtn.addSelectionListener(new SelectionAdapter() {
				@Override public void widgetSelected(SelectionEvent e) {
					chooseTranscriptVersion();
				}
			});
			
			updateSelectedVersion();
		}
		
		public void updateSelectedVersion() {
			String l = selectedMd == null ? "Choose..." : getTranscriptLabel(selectedMd);
			
			if(!SWTUtil.isDisposed(chooseVersionBtn)){
				chooseVersionBtn.setText(l);
				chooseVersionBtn.pack();
				layout();
			}
		}
		
		public void chooseTranscriptVersion() {
			ChooseTranscriptDialog d = new ChooseTranscriptDialog(getShell());
			if (d.open() != Dialog.OK)
				return;
			
			TrpTranscriptMetadata md = d.getTranscript();
			if (md == null) {
				logger.debug("selected version was null...");
				return;
			}
			
			selectedMd = md;
			updateSelectedVersion();
		}

		public void setToCurrent() {
			if (Storage.getInstance().hasTranscript()) {
				selectedMd = Storage.getInstance().getTranscriptMetadata();
				updateSelectedVersion();
			}			
		}
		public void setToGT() {
			List<TrpTranscriptMetadata> transcripts = Storage.getInstance().getTranscriptsSortedByDate(true, -1);
			if (Storage.getInstance().hasTranscript()) {	
				for (TrpTranscriptMetadata version : transcripts) {
					if (version.getStatus() == EditStatus.GT) {
						selectedMd = version;
						updateSelectedVersion();
						return;
					}
				}
				if(transcripts.size() >= 2) {
					selectedMd = transcripts.get(1);
				}else{
					selectedMd = Storage.getInstance().getTranscriptMetadata();
				}
				updateSelectedVersion();				
			}		
		}
	}	
	
	public ToolsWidget(Composite parent, int style) {
		super(parent, style);
		this.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		
		sc = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(new GridData(GridData.FILL_BOTH));
		sc.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		
		container = new Composite(sc, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));		
		
//		Label ncsrIconL = new Label(SWTUtil.dummyShell, 0);
//		ncsrIconL.setImage(ncsrIcon);
		
		initModelTrainingTools(container);
		initLayoutAnalysisTools(container);
		initRecogTools(container);
		
		if(IS_LEGACY_WER_GROUP) {
			initLegacyWerGroup(container);
		} else {
			initWerGroup(container);
		}
		
		initOtherTools(container);
		
		sc.setContent(container);
	    sc.setExpandHorizontal(true);
	    sc.setExpandVertical(true);
//	    sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//	    sc.setMinSize(container.computeSize(0, SWT.DEFAULT));
	    layoutContainer();
	}
	
	private void layoutContainer() {
		container.layout();
	    sc.setMinSize(0, container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y-1);
//	    sc.setMinSize(container.computeSize(0, SWT.DEFAULT));
	}
	
	public String getSelectedLaMethod() {
		return laComp.getSelectedMethod();
	}
	
	private void initModelTrainingTools(Composite container) {
		modelTrainingExp = new ExpandableComposite(container, ExpandableComposite.COMPACT);
		modelTrainingExp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Composite grp = new Composite(modelTrainingExp, SWT.SHADOW_ETCHED_IN);
		grp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		grp.setLayout(new GridLayout(2, false));
		
		modelTrComp = new ModelTrainingComposite(grp, 0);
		modelTrComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		modelTrainingExp.setClient(grp);
		modelTrainingExp.setText("Model Training");
		modelTrainingExp.setExpanded(true);
		Fonts.setBoldFont(modelTrainingExp);
		modelTrainingExp.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				layoutContainer();
			}
		});
		
	}
	
	private void initLayoutAnalysisTools(Composite container) {
		laToolsExp = new ExpandableComposite(container, ExpandableComposite.COMPACT);
		laToolsExp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Composite laToolsGroup = new Composite(laToolsExp, SWT.SHADOW_ETCHED_IN);
		laToolsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
//		metadatagroup.setText("Document metadata");
		laToolsGroup.setLayout(new GridLayout(2, false));
		
		laComp = new LayoutAnalysisComposite(laToolsGroup, 0);
		laComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		startLaBtn = new Button(laToolsGroup, 0);
		startLaBtn.setText("Run");
		startLaBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startLaBtn.setImage(Images.ARROW_RIGHT);
		
		if (false) {
//		laMethodCombo = new LabeledCombo(laToolsGroup, "Method: ");
//		laMethodCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
//		laMethodCombo.combo.setItems(LayoutAnalysisComposite.getMethods(false).toArray(new String[0]));
//		laMethodCombo.combo.select(0);
//		Storage.getInstance().addListener(new IStorageListener() {
//			public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
//				if (arg.login) {
//					laMethodCombo.combo.setItems(LayoutAnalysisComposite.getMethods(false).toArray(new String[0]));
//					laMethodCombo.combo.select(0);
//				}
//			}
//		});
//		
//		laMethodCombo.combo.addModifyListener(new ModifyListener() {
//			@Override public void modifyText(ModifyEvent e) {
//				updateLaGui();
//			}
//		});
//		
//		regAndLineSegBtn = new Button(laToolsGroup, SWT.PUSH);
//		regAndLineSegBtn.setText("Detect regions, lines and baselines");
//		regAndLineSegBtn.setToolTipText("Detects regions, lines and baselines in this page - warning: current regions, lines and baselines will be lost!");
//		regAndLineSegBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
//		
////		Button aboutRegAndLineSegBtn = new Button(laToolsGroup, SWT.PUSH);
////		aboutRegAndLineSegBtn.setImage(Images.getOrLoad("/icons/information.png"));
//		
//		lineSegBtn = new Button(laToolsGroup, SWT.PUSH);
//		lineSegBtn.setText("Detect lines and baselines");
//		lineSegBtn.setToolTipText("Detects lines and baselines in all selected regions (or in all regions if no region is selected) - warning: current lines and baselines in selected regions will be lost!");
//		lineSegBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
//		
//		batchLaBtn = new Button(laToolsGroup, SWT.PUSH);
//		batchLaBtn.setText("Batch job...");
//		batchLaBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//		batchLaBtn.setToolTipText("Configure and start a batch job for layout analysis");
//		
//		Button aboutLaBtn = new Button(laToolsGroup, SWT.PUSH);
//		aboutLaBtn.setImage(Images.getOrLoad("/icons/information.png"));
//		aboutLaBtn.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				String title = "About: Analyze Layout";
//				String msg = "Status\n"
//						+ "\t-Experimental\n"
//						+ "\t-Needs enhancement\n"
//						+ "Behaviour\n"
//						+ "\t-Text regions and lines are detected\n"
//						+ "\t-Already available text regions and/or lines are deleted\n"
//						+ "Background\n"
//						+ "\t-HTR processing needs correctly detected text regions and baselines\n"
//						+ "\t-In the future it is planned to have integrated solutions available where\n"
//						+ "\t text regions and baselines are detected in one process"
//						+ "Provider\n"
//						+ "\t-National Centre for Scientific Research (NCSR) – Demokritos in\n"
//						+ "\t Greece/Athens.\n"
//						+ "Contact\n"
//						+ "\t https://www.iit.demokritos.gr/cil";
//				
//				DialogUtil.showMessageDialog(getShell(), title, msg, null, ncsrIcon, new String[] { "Close" }, 0);
//			}
//		});
		
		}
		
//		Button aboutLineSegBtn = new Button(laToolsGroup, SWT.PUSH);
//		aboutLineSegBtn.setImage(Images.getOrLoad("/icons/information.png"));
//		aboutLineSegBtn.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				String title = "About: Detect Lines and Baselines";
//				String msg = "Status\n"
//						+ "\t-Beta version\n"
//						+ "\t-Can be used for productive work\n"
//						+ "Behaviour\n"
//						+ "\t-Detects lines and baselines in text regions.\n"
//						+ "\t-Note: For HTR purposes only baselines are necessary, therefore no need\n"
//						+ "\t to correct lines.\n"
//						+ "Background\n"
//						+ "\t-The PAGE format which is used internally in TRANSKRIBUS requires that\n"
//						+ "\t each baseline is part of a line region. Therefore the tool needs to produce\n"
//						+ "\t line regions although the line regions are not used for further processing\n"
//						+ "\t (and can therefore be ignored in the correction process).\n"
//						+ "Provider\n"
//						+ "\t-National Centre for Scientific Research (NCSR) – Demokritos in\n\tGreece/Athens.\n"
//						+ "Contact\n"
//						+ "\t https://www.iit.demokritos.gr/cil/";
//				
//				DialogUtil.showMessageDialog(getShell(), title, msg, null, ncsrIcon, new String[] { "Close" }, 0);
//			}
//		});		
				
//		baselineBtn = new Button(laToolsGroup, SWT.PUSH);
//		baselineBtn.setText("Detect baselines");
//		baselineBtn.setToolTipText("Detects baselines in all lines of the selected regions (or of all regions if no region is selected) - warning: current baselines of affected lines will be lost!");
//		baselineBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
//		
//		Button aboutBaselineBtn = new Button(laToolsGroup, SWT.PUSH);
//		aboutBaselineBtn.setImage(Images.getOrLoad("/icons/information.png"));
//		aboutBaselineBtn.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				String title = "About: Detect baselines";
//				String msg = "Status\n"
//						+ "\t-Beta version\n"
//						+ "\t-Can be used for productive work\n"
//						+ "Behaviour\n"
//						+ "\t-Note: This is a tool with a very special purpose: If line regions are\n"
//						+ "\t already available the tool will detect corresponding baselines\n"
//						+ "\t-Needs correct line regions as input\n"
//						+ "\t-Detects baselines within line regions\n"
//						+ "Background\n"
//						+ "\t-In some rare cases researchers may have correct line regions\n"
//						+ "\t available, these line regions can be enriched with baselines.\n"
//						+ "Provider\n"
//						+ "\t-National Centre for Scientific Research (NCSR) – Demokritos in\n"
//						+ "\t Greece/Athens.\n"
//						+ "Contact\n"
//						+ "\t https://www.iit.demokritos.gr/cil/";
//				
//				DialogUtil.showMessageDialog(getShell(), title, msg, null, ncsrIcon, new String[] { "Close" }, 0);				
//			}
//		});		
//		
//		wordSegBtn = new Button(laToolsGroup, SWT.PUSH);
//		wordSegBtn.setText("Detect words");
//		wordSegBtn.setToolTipText("Detects words in all lines of the selected regions (or of all regions if no region is selected) - warning: current baselines of affected lines will be lost!");
//		wordSegBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
//		
//		Button aboutWordBtn = new Button(laToolsGroup, SWT.PUSH);
//		aboutWordBtn.setImage(Images.getOrLoad("/icons/information.png"));
//		aboutWordBtn.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				String title = "About: Detect baselines";
//				String msg = "Status\n"
//						+ "\t-Beta version\n"
//						+ "\t-Can be used for productive work\n"
//						+ "Behaviour\n"
//						+ "\t-Note: This is a tool with a very special purpose: If line regions are\n"
//						+ "\t already available the tool will detect corresponding baselines\n"
//						+ "\t-Needs correct line regions as input\n"
//						+ "\t-Detects baselines within line regions\n"
//						+ "Background\n"
//						+ "\t-In some rare cases researchers may have correct line regions\n"
//						+ "\t available, these line regions can be enriched with baselines.\n"
//						+ "Provider\n"
//						+ "\t-National Centre for Scientific Research (NCSR) – Demokritos in\n"
//						+ "\t Greece/Athens.\n"
//						+ "Contact\n"
//						+ "\t https://www.iit.demokritos.gr/cil/";
//				
//				DialogUtil.showMessageDialog(getShell(), title, msg, null, ncsrIcon, new String[] { "Close" }, 0);				
//			}
//		});		
		
//		Button aboutBtn = new Button(laToolsGroup, SWT.PUSH);
//		aboutBtn.setText("About NCSR...");
//		aboutBtn.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				Shell s = new Shell();
//				s.setLayout(new RowLayout());
//				Label iconL = new Label(s, 0);
//				iconL.setImage(ncsrIcon);
//				
//				Label aboutL = new Label(s, 0);
//				aboutL.setText("Computational Intelligence Laboratory, Institute of Informatics and Telecommunications\nNational Center for Scientific Research “Demokritos”, GR-153 10 Agia Paraskevi, Athens, Greece");
//				s.pack();
//				s.setText("About: Layout analysis tools");
//				SWTUtil.centerShell(s);
//				s.open();
//			}
//		});
		
		laToolsExp.setClient(laToolsGroup);
		laToolsExp.setText("Layout Analysis");
		laToolsExp.setExpanded(true);
		Fonts.setBoldFont(laToolsExp);
		laToolsExp.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				layoutContainer();
			}
		});
		
//		updateLaGui();
	}
	
//	private void updateLaGui() {
//		if (regAndLineSegBtn == null || lineSegBtn == null)
//			return;
//		
//		String method = getSelectedLaMethod();
//		regAndLineSegBtn.setEnabled(true);
//		lineSegBtn.setEnabled(true);
//		
//		if (method.equals(LayoutAnalysisComposite.METHOD_NCSR)) {
//			regAndLineSegBtn.setEnabled(false);
//		}
//		else if (method.equals(LayoutAnalysisComposite.METHOD_CVL)) {
//			lineSegBtn.setEnabled(false);
//		}
//	}
	
	private void initRecogTools(Composite container) {
		expRecog = new ExpandableComposite(container, ExpandableComposite.COMPACT);
		expRecog.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		trComp = new TextRecognitionComposite(expRecog, 0);
		trComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		expRecog.setClient(trComp);
		expRecog.setText("Text Recognition");
		Fonts.setBoldFont(expRecog);
		expRecog.setExpanded(true);
		expRecog.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				layoutContainer();
			}
		});
	}

	
	private void initOtherTools(Composite container) {
		expOther = new ExpandableComposite(container, ExpandableComposite.COMPACT);
		expOther.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		otherToolsContainer = new Composite(expOther, SWT.SHADOW_ETCHED_IN);
		otherToolsContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		otherToolsContainer.setLayout(new GridLayout(1, true));
		
		p2palaContainer = new Composite(otherToolsContainer, 0);
		p2palaContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		p2palaContainer.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		
		p2palaBtn = new Button(p2palaContainer, SWT.PUSH);
		p2palaBtn.setText("P2PaLA...");
		p2palaBtn.setToolTipText("Creates regions with structure tags and baselines from pre-trained models");
		p2palaBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (false) { // for now, put train button into P2PaLAConfDialog
		p2palaTrainBtn = new Button(p2palaContainer, SWT.PUSH);
		p2palaTrainBtn.setText("P2PaLA training...");
		p2palaTrainBtn.setToolTipText("Train a new model for P2PaLA");
		p2palaTrainBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		
		t2iContainer = new Composite(otherToolsContainer, 0);
		t2iContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		t2iContainer.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		
		t2iBtn = new Button(t2iContainer, SWT.PUSH);
		t2iBtn.setText("Text2Image...");
		t2iBtn.setToolTipText("Tries to match the text contained in the transcriptions to a line segmentation");
		t2iBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		t2iBtn.setData(new Text2ImageConf());

		duContainer = new Composite(otherToolsContainer, 0);
		duContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		duContainer.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		duButton = new Button(duContainer, SWT.PUSH);
		duButton.setText("Document Understanding...");
		duButton.setToolTipText("Automatically adds annotations.");
		duButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		
		otherOtherToolsGroup = new Group(otherToolsContainer, 0);
		otherOtherToolsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		otherOtherToolsGroup.setLayout(new GridLayout(1, true));
		
		otherToolsPagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(otherOtherToolsGroup, SWT.NONE, true,true);
		otherToolsPagesSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		
		polygon2baselinesBtn = new Button(otherOtherToolsGroup, SWT.PUSH);
		polygon2baselinesBtn.setText("Add Baselines to Polygons");
		polygon2baselinesBtn.setToolTipText("Creates baselines for all surrounding polygons - warning: existing baselines will be lost (text is retained however!)");
		polygon2baselinesBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		baseline2PolygonBtn = new Button(otherOtherToolsGroup, SWT.PUSH);
		baseline2PolygonBtn.setText("Add Polygons to Baselines");
		baseline2PolygonBtn.setToolTipText("Creates polygons for all baselines - warning: existing polygons will be lost (text is retained however!)");
		baseline2PolygonBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		
		expOther.setClient(otherToolsContainer);
		new Label(otherToolsContainer, SWT.NONE);
		expOther.setText("Other Tools");
		Fonts.setBoldFont(expOther);
		expOther.setExpanded(true);
		expOther.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				layoutContainer();
			}
		});
	
	}
	
	public void setDuVisible(boolean visible) {
		logger.debug("setting du visible: "+visible);
		if (visible) {
			duContainer.setParent(otherToolsContainer);
			duContainer.moveBelow(t2iContainer);
		} else {
			duContainer.setParent(SWTUtil.dummyShell);
		}
		layoutContainer();
	}
	
//	public void setP2PaLAModels(List<TrpP2PaLAModel> models) {
//		logger.debug("setting p2pala models, N = "+CoreUtils.size(models));
//		if (models==null || models.isEmpty()) {
//			p2palaModelCombo.setItems(new String[] {});
//		}
//		
//		List<String> items = new ArrayList<>();
//		int i=0;
//		for (TrpP2PaLAModel m : models) {
//			items.add(m.getName());
//			p2palaModelCombo.setData(""+i, m);
//			++i;
//		}
//		p2palaModelCombo.setItems(items.toArray(new String[0]));
//		p2palaModelCombo.select(0);
//	}
//	
//	public TrpP2PaLAModel getSelectedP2PaLAModel() {
//		int i = p2palaModelCombo.getSelectionIndex();
//		if (i>=0 && i<p2palaModelCombo.getItemCount()) {
//			try {
//				return (TrpP2PaLAModel) p2palaModelCombo.getData(""+i);
//			} catch (Exception e) {
//				logger.error("Error casting selected P2PaLAModel: "+e.getMessage(), e);
//			}
//		}
//		return null;
//	}

	private void initLegacyWerGroup(Composite container) {
		werExp = new ExpandableComposite(container, ExpandableComposite.COMPACT);
		werExp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		werGroup = new Composite(werExp, SWT.SHADOW_ETCHED_IN);
		werGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		werGroup.setLayout(new GridLayout(2, false));
		
		refVersionChooser = new TranscriptVersionChooser("Reference:\n(Correct Text) ", werGroup, 0);
		refVersionChooser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		hypVersionChooser = new TranscriptVersionChooser("Hypothesis:\n(HTR Text) ", werGroup, 0);
		hypVersionChooser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));		
				
		computeWerBtn = new Button(werGroup, SWT.PUSH);
		computeWerBtn.setText("Compare");
		computeWerBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 0, 1));
		computeWerBtn.setToolTipText("Compares the two selected transcripts and computes word error rate and character error rate.");
		
		compareVersionsBtn = new Button(werGroup, SWT.PUSH);
		compareVersionsBtn.setText("Compare Text Versions");
		compareVersionsBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1));
		compareVersionsBtn.setToolTipText("Shows the difference of the two selected versions");
			
		werExp.setClient(werGroup);
		werExp.setText("Compute Accuracy");
		Fonts.setBoldFont(werExp);
		werExp.setExpanded(true);
		werExp.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				layoutContainer();
			}
		});
	}
	
	private void initWerGroup(Composite container) {
		werExp = new ExpandableComposite(container, ExpandableComposite.COMPACT);
		werExp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		werGroup = new Composite(werExp, SWT.SHADOW_ETCHED_IN);
		werGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
//		metadatagroup.setText("Document metadata");
		werGroup.setLayout(new GridLayout(2, false));
		
		refVersionChooser = new TranscriptVersionChooser("Reference:\n(Correct Text) ", werGroup, 0);
		refVersionChooser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		hypVersionChooser = new TranscriptVersionChooser("Hypothesis:\n(HTR Text) ", werGroup, 0);
		hypVersionChooser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));		
		
//		computeWerBtn.pack();
		
		compareVersionsBtn = new Button(werGroup, SWT.PUSH);
		compareVersionsBtn.setText("Compare Text Versions...");
		compareVersionsBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1));
		compareVersionsBtn.setToolTipText("Shows the difference of the two selected versions");
		
//		computeWerBtn = new Button(werGroup, SWT.PUSH);
//		computeWerBtn.setText("Quick Compare");
//		computeWerBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 0, 1));
//		computeWerBtn.setToolTipText("Compares the two selected transcripts and computes word error rate and character error rate.");
		
		computeAdvancedBtn = new Button(werGroup,SWT.PUSH);
		computeAdvancedBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1));
		computeAdvancedBtn.setText("Compare...");
		
		compareSamplesBtn = new Button(werGroup, SWT.PUSH);
		compareSamplesBtn.setText("Compare Samples...");
		compareSamplesBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1));
		compareSamplesBtn.setToolTipText("Shows the difference of the two selected versions");
		
		werExp.setClient(werGroup);
		werExp.setText("Compute Accuracy...");
		Fonts.setBoldFont(werExp);
		werExp.setExpanded(true);
		werExp.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				layoutContainer();
			}
		});
	}

	public Button getCompareVersionsBtn() {
		return compareVersionsBtn;
	}
	
	public void updateVisibility(boolean setEnabled){
		werExp.setExpanded(setEnabled);
		werExp.setEnabled(setEnabled);
		laToolsExp.setExpanded(setEnabled);
		laToolsExp.setEnabled(setEnabled);
		laComp.updateSelectionChooserForLA();
		expOther.setExpanded(setEnabled);
		expOther.setEnabled(setEnabled);
		trComp.getRunBtn().setEnabled(setEnabled);
		layoutContainer();
	}
	
	public TrpTranscriptMetadata getCorrectText(){
		return this.refVersionChooser.selectedMd;
	}
	
	public TrpTranscriptMetadata getHpothesisText(){
		return this.hypVersionChooser.selectedMd;
	}

	public static String getTranscriptLabel(TrpTranscriptMetadata t) {
		final String labelStr = CoreUtils.newDateFormatUserFriendly().format(t.getTime()) 
				+ " - " + t.getUserName() 
				+ " - " + t.getStatus().getStr()
				+ (t.getToolName() != null ? " - " + t.getToolName() : "");
		
		return labelStr;
	}
}
