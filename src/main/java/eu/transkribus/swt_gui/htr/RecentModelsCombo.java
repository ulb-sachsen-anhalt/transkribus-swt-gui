package eu.transkribus.swt_gui.htr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.core.model.beans.rest.TrpHtrList;
import eu.transkribus.core.rest.JobConst;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.util.TextRecognitionConfig;
import eu.transkribus.util.TextRecognitionConfig.Mode;

public class RecentModelsCombo{
	private final static Logger logger = LoggerFactory.getLogger(RecentModelsCombo.class);
	
	private Storage store = Storage.getInstance();
	private Combo combo;
	private Composite theParent;
	
	Map<Integer, TrpHtr> htrs = new HashMap<Integer, TrpHtr>();
	Map<Integer, TrpJobStatus> recentRecognitionJobs = null;
	
	Properties props = null;
	

	public Properties getJobProperties() {
		return props;
	}

	public RecentModelsCombo(Composite parent) {
		theParent = parent;
		
		Color lightGreen = new Color(parent.getDisplay(), new RGB( 255, 255, 200 ) );

		Label recentModelLbl = new Label(parent, SWT.FILL);
		recentModelLbl.setText("Recently used HTR models");
		recentModelLbl.setBackground(lightGreen);
		
		combo = new Combo(parent, SWT.READ_ONLY);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		combo.setBackground(lightGreen);
		
		findRecentHtrs();
		
//		combo.addSelectionListener(new SelectionAdapter() {
//			 @Override
//			 public void widgetSelected(SelectionEvent e) {
//				 
//				 loadRecentHtr(combo.getItem(combo.getSelectionIndex()));
//					
//			 }
//		});
		
		
		//combo.addSelectionListener(A);
//		
//		this.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		
		
		//setMultiCombo(new MultiCheckSelectionCombo(this, SWT.FILL,"Restrict on structure tags", 1, 200, 300 ));
		//getMultiCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		
		
	}
	
	protected void findRecentHtrs() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	logger.debug("recently used recognition models:");
				List<TrpJobStatus> recJobs = new ArrayList<>();
				List<TrpJobStatus> pylaiaJobs = new ArrayList<>();
				recentRecognitionJobs = new HashMap<Integer, TrpJobStatus>();
				
				try {
					recJobs = store.getConnection().getJobs(true, TrpJobStatus.FINISHED, "CITlab Handwritten Text Recognition", null,
							0, 50, null, null);
				
					pylaiaJobs = store.getConnection().getJobs(true, TrpJobStatus.FINISHED, "PyLaia Decoding", null,
							0, 50, null, null);
					
					recJobs.addAll(pylaiaJobs);
					
					TrpHtrList l = store.getConnection().getHtrsSync(store.getCollId(), null, null, null, 0, -1, null, null);
					for (TrpJobStatus job : recJobs) {
						
						if (!recentRecognitionJobs.containsKey(job.getModelId())) {
							recentRecognitionJobs.put(job.getModelId(), job);
						}
						
					}
					
					logger.debug( "nr of recognition jobs in history: " + recJobs.size());				
					logger.debug( "available htrs in collection (incl. public models) " + l.getTotal());
//					for (TrpHtr htr : l.getList()) {
//						logger.debug( "(htr name) " + htr.getName() + " (htr id) " + htr.getHtrId());
//					}
					
					//recentModelIds.forEach((K,V) -> logger.debug( K + " => " + V.getModelId() ));
			
					int i = 0;
					for (Integer key : recentRecognitionJobs.keySet()) {
						//TrpJobStatus currJob = recentModelIds.get(key);
						for (TrpHtr htr : l.getList()) {
							if (Integer.valueOf(htr.getHtrId()).equals(key)) {
								logger.trace( " => (recent model found) " + htr.getName() );
			
								htrs.put(htr.getHtrId(), htr);
								combo.add(htr.getHtrId() + ": " + htr.getName() + " (\t"  + htr.getProvider() + ")");
								
							}
						}
					}
				} catch (SessionExpiredException | ServerErrorException | ClientErrorException | IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		});
	}

	protected void loadRecentHtr(String recentHtr) {
		TextRecognitionConfig config = null;
		try {
			
			String id = recentHtr.substring(0, recentHtr.indexOf(":"));
			
			TrpHtr htr = htrs.get(Integer.valueOf(id));
			TrpJobStatus job = recentRecognitionJobs.get(Integer.valueOf(id));
			
			
			if (htr != null) {
				Mode mode = getModeForProvider(htr.getProvider());
				if (mode == null) {
					DialogUtil.showErrorMessageBox(theParent.getShell(), "Error parsing mode from provider", "Unknown model provider: "+htr.getProvider());
					return;
				}
				config = new TextRecognitionConfig(mode);
				
				/*
				 * this is set in the recognition job -> so we can get them from the job
				 */
//				props.setProperty(JobConst.PROP_DICTNAME, dictName);
//				validateDictNameValue(dictName);
//				props.setProperty(JobConst.PROP_DO_LINE_POLYGON_SIMPLIFICATION, ""+doLinePolygonSimplification);
//				props.setProperty(JobConst.PROP_KEEP_ORIGINAL_LINE_POLYGONS, ""+keepOriginalLinePolygons);
//				props.setProperty(JobConst.PROP_DO_STORE_CONFMATS, ""+doStoreConfMats);
				
				props = job.getJobDataProps().getProperties();

				config.setDictionary(props.getProperty(JobConst.PROP_DICTNAME));
				config.setLanguageModel(JobConst.PROP_TRAIN_DATA_LM_VALUE);
				config.setHtrId(htr.getHtrId());
				config.setHtrName(htr.getName());
				config.setLanguage(htr.getLanguage());				
			}
			else {
				logger.debug("model was probably deleted - setting config to null!");
				config = null;
			}
		} catch (Exception e) {
			logger.error("Error while setting HTR: "+e.getMessage(), e);
		}
		finally {
			
			store.saveTextRecognitionConfig(config);
			
		}
	}
	
	public Combo getCombo() {
		return combo;
	}
	
	private Mode getModeForProvider(String provider) {
				
		if (ModelUtil.PROVIDER_CITLAB.equals(provider) || ModelUtil.PROVIDER_CITLAB_PLUS.equals(provider)) {
			return Mode.CITlab;
		}
		if (ModelUtil.PROVIDER_PYLAIA.equals(provider)) {
			return Mode.UPVLC;
		}
		
		return null;
	}

}