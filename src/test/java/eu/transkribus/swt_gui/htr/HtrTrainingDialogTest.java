package eu.transkribus.swt_gui.htr;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.connection.TrpServerConn;
import eu.transkribus.core.io.util.TrpProperties;
import eu.transkribus.core.model.beans.CitLabHtrTrainConfig;
import eu.transkribus.core.model.beans.CitLabSemiSupervisedHtrTrainConfig;
import eu.transkribus.core.model.beans.PyLaiaHtrTrainConfig;
import eu.transkribus.core.util.JaxbUtils;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.TestApplicationWindow;

public class HtrTrainingDialogTest {
	private static final Logger logger = LoggerFactory.getLogger(HtrTrainingDialogTest.class);
	
	public static void main(String[] args) {

		boolean displayJsonDescriptorOnChange = true;
		
//		final int colId = 2; //575 = CITlab GT collection
		final int colId = 2; //575 = CITlab GT collection
		
		boolean startTrainingOnServer=false;
		
		TrpProperties creds = new TrpProperties("testCreds.properties");
		new TestApplicationWindow("https://transkribus.eu/TrpServerTesting", creds.getString("username"), creds.getString("password"), colId) {
//		new TestApplicationWindow(TrpServerConn.TEST_SERVER_URI, args[0], args[1], colId) {
			@Override
			protected void createTestContents(Composite parent) throws Exception {
				getShell().setSize(500, 700);
//				getStorage().reloadHtrs();
				parent.pack();
				ModelTrainingDialog htd = new ModelTrainingDialog(
						parent.getShell(),
						getStorage().getHtrTrainingJobImpls()
						);
				
				//show/update debug dialog with JSON descriptor on each change in data set
				htd.enableDebugDialog(displayJsonDescriptorOnChange);
				
				if (htd.open() == IDialogConstants.OK_ID) {
					// new: check here if user wants to store or not
					// if (!mw.saveTranscriptDialogOrAutosave()) {
					// //if user canceled this
					// return;
					// }
					String jobId = null;
					if (htd.getCitlabTrainConfig() != null) {
						CitLabHtrTrainConfig config = htd.getCitlabTrainConfig();
						logger.info("config = " + config);
						logger.info("JSON =\n{}", JaxbUtils.marshalToJsonString(config, true));
					} else if (htd.getCitlabT2IConfig() != null) {
						CitLabSemiSupervisedHtrTrainConfig config = htd.getCitlabT2IConfig();
						logger.info("conig = " + config);
					}
					else if (htd.getPyLaiaConfig() != null) {
						try {
							PyLaiaHtrTrainConfig config = htd.getPyLaiaConfig();
							logger.info("config = " + config);
							logger.info("JSON =\n{}", JaxbUtils.marshalToJsonString(config, true));
							
//							if (startTrainingOnServer) {
//								jobId = getStorage().runPyLaiaTraining(config);
//								DialogUtil.showInfoMessageBox(getShell(), "Success", "Started the PyLaia training, jobId="+jobId);								
//							}
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							DialogUtil.showErrorMessageBox(getShell(), "Error starting PyLaia training", e.getMessage());
						}
					}
				}
			}
		}.show();
	}
}
