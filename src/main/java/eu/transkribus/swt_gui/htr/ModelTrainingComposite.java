package eu.transkribus.swt_gui.htr;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.models.ModelChooserButton;

public class ModelTrainingComposite extends Composite {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModelTrainingComposite.class);
	
	ModelChooserButton modelsBtn;
	Button trainBtn;

	public ModelTrainingComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		
		modelsBtn = new ModelChooserButton(this, false) {
			@Override public void setModel(TrpModelMetadata htr) {}
		};
		modelsBtn.setText("View models...");
		modelsBtn.setImage(Images.MODEL_ICON);
		modelsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		trainBtn = new Button(this, 0);
		trainBtn.setText("Train a new model...");
		trainBtn.setImage(Images.TRAIN);
		trainBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Storage.i().addListener(new IStorageListener() {
			public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
				updateGui();
			}
		});
		
		updateGui();
	}
	
	public Button getTrainBtn() {
		return trainBtn;
	}
	
	public ModelChooserButton getModelsBtn() {
		return modelsBtn;
	}
	
	public void updateGui() {	
		JobImpl[] htrTrainingJobImpls = {};
		if(Storage.i() != null && Storage.i().isLoggedIn()) {
			try {
				htrTrainingJobImpls = Storage.i().getHtrTrainingJobImpls();
			} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
				logger.debug("An exception occurred while querying job acl. Training is off.", e);
			}
		}
		
		setBtnVisibility(htrTrainingJobImpls.length > 0);
	}

	private void setBtnVisibility(boolean withTrainBtn) {
		if (withTrainBtn) {
			trainBtn.setParent(this);
			trainBtn.moveBelow(modelsBtn);
		} else {
			trainBtn.setParent(SWTUtil.dummyShell);
		}

		this.pack();
		this.redraw();
		super.layout(true, true);
		
		logger.trace("parent: "+getParent());
		getParent().layout();
	}
}
