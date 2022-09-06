package eu.transkribus.swt_gui.htr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.model.beans.CITlabLaTrainConfig;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;

public class CITlabLATrainingConfComposite extends Composite {
	
	private static final boolean SHOW_MODEL_TYPE = false;
	
	private Text numEpochsTxt;
	private Text learningReateTxt;
	private Combo modelTypeCombo;
	
	public static final String[] MODEL_TYPE_ITEMS =  {"u", "ru", "aru", "laru"};

	public CITlabLATrainingConfComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));
		
		Label numEpochsLbl = new Label(this, SWT.NONE);
		numEpochsLbl.setText("Nr. of Epochs:");
		numEpochsTxt = new Text(this, SWT.BORDER);
		numEpochsTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		numEpochsTxt.setToolTipText("The number of epochs");
		
		Label learningRateLbl = new Label(this, SWT.NONE);
		learningRateLbl.setText("Learning rate:");		
		learningReateTxt = new Text(this, SWT.BORDER);
		learningReateTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		learningReateTxt.setToolTipText("The learning rate");
		
		if (false) {
			Label modelTypeLbl = new Label(this, SWT.NONE);
			modelTypeLbl.setText("Model type:");			
			modelTypeCombo = new Combo(this, SWT.READ_ONLY);
			modelTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			modelTypeCombo.setToolTipText("The model type - cf. paper: https://arxiv.org/pdf/1802.03345v2.pdf");
			modelTypeCombo.setItems(MODEL_TYPE_ITEMS);
		}
		
//		Button paperLinkBtn = new Button(this, 0);
//		paperLinkBtn.setText("Link to paper...");
//		paperLinkBtn.setImage(Images.SCRIPT_ICON);
//		SWTUtil.onSelectionEvent(paperLinkBtn, e -> {
//			org.eclipse.swt.program.Program.launch("https://arxiv.org/pdf/1802.03345v2.pdf");
//		});
		
//		Button codeLinkBtn = new Button(this, 0);
//		codeLinkBtn.setText("Link to GitHub repo...");
//		codeLinkBtn.setImage(Images.SCRIPT_CODE_ICON);
//		SWTUtil.onSelectionEvent(codeLinkBtn, e -> {
//			org.eclipse.swt.program.Program.launch("https://github.com/TobiasGruening/ARU-Net");
//		});		
		
		setDefault();
	}
	
	public void setDefault() {
		CITlabLaTrainConfig c = new CITlabLaTrainConfig();
		SWTUtil.set(numEpochsTxt, ""+c.getNumEpochs());
		SWTUtil.set(learningReateTxt, ""+c.getLearningRate());
		if (modelTypeCombo!=null) {
			modelTypeCombo.select(Arrays.asList(MODEL_TYPE_ITEMS).indexOf(c.getModelType()));
		}
	}
	
	public String getProvider() {
		return ModelUtil.PROVIDER_CITLAB_BASELINES;
	}

	public List<String> validateParameters(ArrayList<String> errorList) {
		if(errorList == null) {
			errorList = new ArrayList<>();
		}
		if (!StringUtils.isNumeric(numEpochsTxt.getText())) {
			errorList.add("Number of Epochs must be a number!");
		}
		
		return errorList;
	}

	public CITlabLaTrainConfig addParameters(CITlabLaTrainConfig conf) {
		conf.getModelMetadata().setProvider(getProvider());
		conf.setNumEpochs(Integer.parseInt(numEpochsTxt.getText()));
		if (modelTypeCombo!=null) {
			conf.setModelType(modelTypeCombo.getText());
		}
		
		return conf;
	}

}
