package eu.transkribus.swt_gui.la;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.util.EnumUtils;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.models.ModelChooserButton;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrDocPagesOrCollectionSelector;

public class Text2ImageSimplifiedConfComposite extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(Text2ImageSimplifiedConfComposite.class);
	
	Button removeLineBreaksBtn;
	Button performLaBtn;
	ModelChooserButton baseModelBtn;
//	LabeledText thresholdText;
	Label thresholdLabel;
	Combo thresholdCombo;
	Combo editStatusCombo;
//	Combo thresholdText;
//	CurrentTranscriptOrCurrentDocPagesSelector pagesSelector;
	CurrentTranscriptOrDocPagesOrCollectionSelector pagesSelector;
	Button allowIgnoringTranscriptsBtn, allowSkippingBaselinesBtn, allowIgnoringReadingOrderBtn;
	Button useHyphenBtn;
	
	public static class Text2ImageConf {
		public TrpModelMetadata model=null;
		public boolean performLa=true;
		public boolean removeLineBreaks=false;
//		public String versionsStatus=null;
		public EditStatus editStatus=null;
		public double threshold=0.0d;
		
		public Double skip_word=null;
		public Double skip_bl=null;
		public Double jump_bl=null;
		public Double hyphen=null;
		
		public boolean currentTranscript=true;
		public String pagesStr=null;
		
		public boolean isDocsSelection=false;
		public List<DocSelection> docsSelected = null;
		
		
		public Text2ImageConf() {}
		public Text2ImageConf(TrpModelMetadata model, boolean performLa, boolean removeLineBreaks, double threshold) {
			super();
			this.model = model;
			this.performLa = performLa;
			this.removeLineBreaks = removeLineBreaks;
			this.threshold = threshold;
		}
		@Override
		public String toString() {
			return "Text2ImageConf [model=" + model + ", performLa=" + performLa + ", removeLineBreaks="
					+ removeLineBreaks + ", editStatus=" + editStatus + ", threshold=" + threshold
					+ ", skip_word=" + skip_word
					+ ", skip_bl=" + skip_bl + ", jump_bl=" + jump_bl + ", hyphen="+hyphen+", currentTranscript=" + currentTranscript
					+ ", pagesStr=" + pagesStr + "]";
		}
	}
	
	public Text2ImageSimplifiedConfComposite(Composite parent, int flags, Text2ImageConf conf) {
		super(parent, flags);
		int nCols = 2;
		this.setLayout(new GridLayout(nCols, false));
		
		
//		Label modelLabel = new Label(this, 0);
//		modelLabel.setText("Base model");
//		modelLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		
		/*
		 * old one without batch mode (chosing several docs from collection)
		 */
//		pagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(this, SWT.NONE, true,true);
//		pagesSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));	
		
		pagesSelector = new CurrentTranscriptOrDocPagesOrCollectionSelector(this, SWT.NONE, false, true, true);		
		pagesSelector.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));
		
		baseModelBtn = new ModelChooserButton(this, true, ModelUtil.TYPE_TEXT, ModelUtil.PROVIDER_CITLAB_PLUS, "Base model: ");
		baseModelBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		
		performLaBtn = new Button(this, SWT.CHECK);
		performLaBtn.setText("Perform Layout Analysis");
		performLaBtn.setToolTipText("Perform a new layout analysis for text alignment - uncheck to use the existing layout");
		performLaBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		
		removeLineBreaksBtn = new Button(this, SWT.CHECK);
		removeLineBreaksBtn.setText("Remove Line Breaks");
		removeLineBreaksBtn.setToolTipText("Check to disrespect linebreaks of the input text during text alignment");
		removeLineBreaksBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		
		Composite editStatusComp = new Composite(this, 0);
		editStatusComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		editStatusComp.setLayout(SWTUtil.createGridLayout(nCols, false, 0, 0));
		
		Label editStatusLbl = new Label(editStatusComp, 0);
		editStatusLbl.setText("Use versions with edit status: ");
		
		editStatusCombo = new Combo(editStatusComp, SWT.DROP_DOWN);
		
		List<String> stati = EnumUtils.stringsList(EditStatus.class);
		stati.add(0, "");
		editStatusCombo.setItems(stati.toArray(new String[0]));
		editStatusCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		editStatusCombo.setToolTipText("Use versions with this edit status for matching.\nIf empty, current version is used");
		editStatusCombo.setText("");
		
//		useNewVersionsBtn = new Button(this, SWT.CHECK);
//		useNewVersionsBtn.setText("Use 'New' versions");
//		useNewVersionsBtn.setToolTipText("Use versions with status 'New' instead of the current version for matching");
//		useNewVersionsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));		

		Composite thresholdComp = new Composite(this, 0);
		thresholdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		thresholdComp.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		
		thresholdLabel = new Label(thresholdComp, 0);
		thresholdLabel.setText("Threshold: ");
		
		String thresholdToolTip = "Threshold for text alignment. If the confidence of a text-to-image\n" + 
				"alignment is above this threshold, an alignment is done (default = 0.0). A\n" + 
				"good value is between 0.01 and 0.05. Note that the confidence is stored\n" + 
				"in the pageXML anyway, so deleting text alignments with low confidence\n" + 
				"can also be made later.";
		
		thresholdCombo = new Combo(thresholdComp, SWT.DROP_DOWN);
		thresholdCombo.setItems(new String[] {"0.0", "0.01", "0.05"} );
		thresholdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		thresholdCombo.setToolTipText(thresholdToolTip);
		
		allowIgnoringTranscriptsBtn = new Button(this, SWT.CHECK);
		allowIgnoringTranscriptsBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		allowIgnoringTranscriptsBtn.setText("Allow ignoring text");
		allowIgnoringTranscriptsBtn.setToolTipText("Allows the tool to skip a word, for example if a baseline is too short");
		
		allowSkippingBaselinesBtn = new Button(this, SWT.CHECK);
		allowSkippingBaselinesBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		allowSkippingBaselinesBtn.setText("Allow skipping baselines");
		allowSkippingBaselinesBtn.setToolTipText("Allows the tool to skip a baseline of the layout");
		
		allowIgnoringReadingOrderBtn = new Button(this, SWT.CHECK);
		allowIgnoringReadingOrderBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		allowIgnoringReadingOrderBtn.setText("Ignore reading order");
		allowIgnoringReadingOrderBtn.setToolTipText("Allows the tool to ignore the reading order of the layout");
		
		useHyphenBtn = new Button(this, SWT.CHECK);
		useHyphenBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nCols, 1));
		useHyphenBtn.setText("Use hyphens (3x slower)");
		useHyphenBtn.setToolTipText("Use default hyphenation signs ('¬', '-', ':', '=') to split text.\nWarning: T2I will be 3x slower!");
		
		setUiFromGivenConf(conf);
	}
	
	private void setUiFromGivenConf(Text2ImageConf conf) {
		if (conf == null) {
			conf = new Text2ImageConf();
		}
		
		// do not update page selection as a different doc with different nr of pages could be loaded since last call...
//		pagesSelector.getCurrentTranscriptButton().setSelection(conf.currentTranscript);
//		pagesSelector.setPagesStr(conf.pagesStr);
		
		if (conf.editStatus != null) {
			editStatusCombo.setText(conf.editStatus.getStr());	
		}
		
		baseModelBtn.setModel(conf.model);
		performLaBtn.setSelection(conf.performLa);
		removeLineBreaksBtn.setSelection(conf.removeLineBreaks);
//		thresholdText.setText(""+conf.threshold);
		thresholdCombo.setText(""+conf.threshold);
		allowIgnoringTranscriptsBtn.setSelection(conf.skip_word!=null);
		allowSkippingBaselinesBtn.setSelection(conf.skip_bl!=null);
		allowIgnoringReadingOrderBtn.setSelection(conf.jump_bl!=null);
		useHyphenBtn.setSelection(conf.hyphen!=null);
	}
	
	public Text2ImageConf getConfigFromUi() {
		Text2ImageConf conf = new Text2ImageConf();
		
		conf.currentTranscript = pagesSelector.isCurrentTranscript();
		conf.pagesStr = pagesSelector.getPagesStr();
		
		conf.isDocsSelection = pagesSelector.isDocsSelection();
		conf.docsSelected = pagesSelector.getDocSelections();
		
		conf.model = baseModelBtn.getModel();
		conf.performLa = performLaBtn.getSelection();
		conf.removeLineBreaks = removeLineBreaksBtn.getSelection();
		
		conf.editStatus = null;
		if (!StringUtils.isEmpty(editStatusCombo.getText())) {
			try {
				conf.editStatus = EditStatus.fromString(editStatusCombo.getText());
			} catch (Exception e) {
				DialogUtil.showErrorMessageBox(getShell(), "Invalid Edit Status", "Invalid Edit Status: "+editStatusCombo.getText()+" - skipping!");
				conf.editStatus = null;
			}			
		}

		
		try {
			conf.threshold = Double.parseDouble(thresholdCombo.getText());
		} catch (Exception e) {
			DialogUtil.showErrorMessageBox(getShell(), "Invalid threshold", "Invalid threshold value: "+thresholdCombo.getText()+" - setting to 0.0");
			thresholdCombo.setText("0.0");
			conf.threshold = 0.0d;
		}
		
		conf.skip_word = allowIgnoringTranscriptsBtn.getSelection() ? 4.0d : null;
		conf.skip_bl = allowSkippingBaselinesBtn.getSelection() ? 0.2d : null;
		conf.jump_bl = allowIgnoringReadingOrderBtn.getSelection() ? 0.0d : null;
		conf.hyphen = useHyphenBtn.getSelection() ? 4.0d : null;
		
		return conf;
	}
	
	public boolean isDocsSelection() {
		return pagesSelector.isDocsSelection();
	}
	
	public List<DocSelection> getDocs() {
		return pagesSelector.getDocSelections();
	}
	
		

}
