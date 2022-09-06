package eu.transkribus.swt_gui.htr;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.transkribus.core.model.beans.TrpPreprocPars;
import eu.transkribus.swt.util.LabeledCombo;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class PyLaiaTrpPreprocComposite extends Composite {
	TrpPreprocPars trpPreprocPars = new TrpPreprocPars();
	
	Label note;
	LabeledCombo dewarpMethodCombo;
	Button deleteBackgroundCheck;
	Button doSauvolaCheck;
	LabeledText lineHeightText;
	LabeledText paddingText;
	LabeledText scalingFactorText;
	
	public PyLaiaTrpPreprocComposite(Composite parent, TrpPreprocPars trpPreprocPars) {
		super(parent, 0);
		this.setLayout(new GridLayout(1, false));
		this.trpPreprocPars = trpPreprocPars == null ? new TrpPreprocPars() : trpPreprocPars;
		createUi();
	}
	
	private void createUi() {
		note = new Label(this, 0);
		note.setText("May be better for a straight script (no slanting) with curved lines");
		note.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		dewarpMethodCombo = new LabeledCombo(this, "Dewarping method");
		dewarpMethodCombo.setItems(new String[] {"dewarp", "rotate", "none"});
		dewarpMethodCombo.getCombo().select(1);
		dewarpMethodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dewarpMethodCombo.setToolTipText("Correction method for non-horizontal lines.\nDewarp - if (some) input lines are curved\nRotate - if input lines are just rotated by some degree but elsewise straight\nNone - if input lines are all perfectly horizontal");
		
		deleteBackgroundCheck = new Button(this, SWT.CHECK);
		deleteBackgroundCheck.setText("Delete background");
		deleteBackgroundCheck.setToolTipText("Remove the background of the line polygon?");
		deleteBackgroundCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		doSauvolaCheck = new Button(this, SWT.CHECK);
		doSauvolaCheck.setText("Binarize");
		doSauvolaCheck.setToolTipText("Binarize all lines images using Sauvola binarization");
		doSauvolaCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		lineHeightText = new LabeledText(this, "Line height: ");
		lineHeightText.setToolTipText("Normalized height of extracted lines. Set to 0 for no normalization.");
		lineHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		paddingText = new LabeledText(this, "Left/right padding: ");
		paddingText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
		scalingFactorText = new LabeledText(this, "Scaling factor: ");
		scalingFactorText.setToolTipText("Scaling factor - determines quality and size of line images.\nThe higher the value, the slower the preprocessing\n0.5 is usually good enough!");
		scalingFactorText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// hide some more advanced parameters from non-admins
		if (Storage.getInstance()!=null && !Storage.getInstance().isAdminLoggedIn()) {
			paddingText.setVisible(false);
			scalingFactorText.setVisible(false);
		}
	}

	public TrpPreprocPars getTrpPreprocPars() {
		trpPreprocPars.setDewarp_method(dewarpMethodCombo.getCombo().getText());
		trpPreprocPars.setDelete_background(deleteBackgroundCheck.getSelection());
		trpPreprocPars.setDo_sauvola(doSauvolaCheck.getSelection());
		trpPreprocPars.setLine_height(lineHeightText.toIntVal(trpPreprocPars.getLine_height()));
		if (paddingText != null) {
			trpPreprocPars.setPadding(paddingText.toIntVal(trpPreprocPars.getPadding()));	
		}
		if (scalingFactorText != null) {
			trpPreprocPars.setScaling_factor((float) scalingFactorText.toDoubleVal(trpPreprocPars.getScaling_factor()));	
		}
		
		return trpPreprocPars;
	}
	
	public void updateUi() {
		if (StringUtils.equals("dewarp", trpPreprocPars.getDewarp_method())) {
			dewarpMethodCombo.getCombo().select(0);
		}
		else if (StringUtils.equals("rotate", trpPreprocPars.getDewarp_method())) {
			dewarpMethodCombo.getCombo().select(1);
		}
		else {
			dewarpMethodCombo.getCombo().select(2);
		}
		
		deleteBackgroundCheck.setSelection(trpPreprocPars.isDelete_background());
		doSauvolaCheck.setSelection(trpPreprocPars.isDo_sauvola());
		lineHeightText.setText(""+trpPreprocPars.getLine_height());
		if (paddingText!=null) {
			paddingText.setText(""+trpPreprocPars.getPadding());	
		}
		if (scalingFactorText != null) {
			scalingFactorText.setText(""+trpPreprocPars.getScaling_factor());	
		}
	}

}
