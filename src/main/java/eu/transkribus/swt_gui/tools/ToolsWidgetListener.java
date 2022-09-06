package eu.transkribus.swt_gui.tools;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.model.beans.CitLabHtrTrainConfig;
import eu.transkribus.core.model.beans.CitLabSemiSupervisedHtrTrainConfig;
import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.DocumentSelectionDescriptor;
import eu.transkribus.core.model.beans.PyLaiaHtrTrainConfig;
import eu.transkribus.core.model.beans.TrpErrorRateResult;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.TrpP2PaLA;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.job.enums.JobImpl;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.rest.ParameterMap;
import eu.transkribus.core.rest.JobConst;
import eu.transkribus.core.rest.JobConstP2PaLA;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.swt.util.DesktopUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.canvas.SWTCanvas;
import eu.transkribus.swt_gui.credits.CostEstimateMessageBuilder;
import eu.transkribus.swt_gui.dialogs.CITlabAdvancedLaConfigDialog;
import eu.transkribus.swt_gui.dialogs.CITlabOcrConfigDialog;
import eu.transkribus.swt_gui.dialogs.ErrorRateAdvancedDialog;
import eu.transkribus.swt_gui.dialogs.OcrDialog;
import eu.transkribus.swt_gui.dialogs.SamplesCompareDialog;
import eu.transkribus.swt_gui.htr.DUDecodeDialog;
import eu.transkribus.swt_gui.htr.HtrTextRecognitionDialog;
import eu.transkribus.swt_gui.htr.ModelTrainingDialog;
import eu.transkribus.swt_gui.la.Text2ImageSimplifiedConfComposite.Text2ImageConf;
import eu.transkribus.swt_gui.la.Text2ImageSimplifiedDialog;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.mainwidget.storage.Storage.StorageException;
import eu.transkribus.swt_gui.p2pala.P2PaLAConfDialog;
import eu.transkribus.swt_gui.p2pala.P2PaLAConfDialog.P2PaLARecogUiConf;
import eu.transkribus.util.OcrConfig;
import eu.transkribus.util.TextRecognitionConfig;

public class ToolsWidgetListener implements SelectionListener, IStorageListener {
	private final static Logger logger = LoggerFactory.getLogger(ToolsWidgetListener.class);

	TrpMainWidget mw;
	ToolsWidget tw;
	SWTCanvas canvas;
	Storage store = Storage.getInstance();

	ModelTrainingDialog htd;
	OcrDialog od;
	HtrTextRecognitionDialog trd2;

	public ToolsWidgetListener(TrpMainWidget mainWidget) {
		this.mw = mainWidget;
		this.canvas = mainWidget.getCanvas();
		this.tw = mainWidget.getUi().getToolsWidget();

		addListener();
	}

	private void addListener() {
		SWTUtil.addSelectionListener(tw.trComp.getRunBtn(), this);

		SWTUtil.onSelectionEvent(tw.modelTrComp.getTrainBtn(), (e) -> {
			startHtrTrainingDialog();
		});

		SWTUtil.addSelectionListener(tw.startLaBtn, this);
		
		if(!ToolsWidget.IS_LEGACY_WER_GROUP) {
			SWTUtil.addSelectionListener(tw.computeWerBtn, this);
			SWTUtil.addSelectionListener(tw.computeAdvancedBtn, this);
		}
		
		SWTUtil.addSelectionListener(tw.compareVersionsBtn, this);
		SWTUtil.addSelectionListener(tw.compareSamplesBtn, this);
		SWTUtil.addSelectionListener(tw.polygon2baselinesBtn, this);
		SWTUtil.addSelectionListener(tw.baseline2PolygonBtn, this);
		SWTUtil.addSelectionListener(tw.p2palaBtn, this);
		SWTUtil.addSelectionListener(tw.p2palaTrainBtn, this);
		SWTUtil.addSelectionListener(tw.t2iBtn, this);
		SWTUtil.addSelectionListener(tw.duButton, this);
		
		Storage.getInstance().addListener(this);
	}

//	List<String> getSelectedRegionIds() {
//		List<String> rids = new ArrayList<>();
//		for (ICanvasShape s : canvas.getScene().getSelectedAsNewArray()) {
//			ITrpShapeType st = GuiUtil.getTrpShape(s);
//			if (st == null || !(st instanceof TrpTextRegionType)) {
//				continue;
//			}
//			rids.add(st.getId());
//		}
//		return rids;
//	}

	boolean isLayoutAnalysis(Object s) {
		return s == tw.startLaBtn || s == tw.polygon2baselinesBtn || s == tw.baseline2PolygonBtn || s==tw.p2palaBtn || s==tw.p2palaTrainBtn || s==tw.t2iBtn;
		// return (s == tw.batchLaBtn || s == tw.regAndLineSegBtn || s == tw.lineSegBtn
		// || s == tw.baselineBtn || s == tw.polygon2baselinesBtn);
	}

	boolean needsRegions(PcGtsType pageData, Object s) {
		if (pageData==null) {
			return false;
		}
		if (PageXmlUtils.hasRegions(pageData)) {
			return false;
		}

		return (s == tw.startLaBtn && !tw.laComp.isDoBlockSeg() && tw.laComp.isDoLineSeg())
				|| s == tw.polygon2baselinesBtn || s == tw.baseline2PolygonBtn;
	}
	
	private void startHtrTrainingDialog() {
		try {
			store.checkLoggedIn();

			if (htd != null) {
				logger.debug("set the training dialog visible");
				htd.setVisible();
			} else {
				logger.debug("new training dialog");
				htd = new ModelTrainingDialog(mw.getShell(), store.getHtrTrainingJobImpls());
				
				if (htd.open() == IDialogConstants.OK_ID) {
					// new: check here if user wants to store or not
					// if (!mw.saveTranscriptDialogOrAutosave()) {
					// //if user canceled this
					// return;
					// }
					String jobId = null;
					if (htd.getCitlabTrainConfig() != null) {
						CitLabHtrTrainConfig config = htd.getCitlabTrainConfig();
						jobId = store.runHtrTraining(config);
						showSuccessMessage(jobId);
					} else if (htd.getCitlabT2IConfig() != null) {
						CitLabSemiSupervisedHtrTrainConfig config = htd.getCitlabT2IConfig();
						jobId = store.runCitLabText2Image(config);
						showSuccessMessage(jobId);
					} else if (htd.getPyLaiaConfig() != null) {
						PyLaiaHtrTrainConfig conf = htd.getPyLaiaConfig();
						jobId = store.runPyLaiaTraining(conf);
						showSuccessMessage(jobId);
					} else if (htd.getCitlabLaConfig() != null) {
						jobId = store.runCITlabLATraining(htd.getCitlabLaConfig());
						showSuccessMessage(jobId);
					}
				}
				htd = null;
			}
		} catch (StorageException e) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Error", e.getMessage());
			htd = null;
		} catch (TrpClientErrorException e) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Could not Start Training", e.getMessageToUser());
			logger.error("Failed to start training: {}", e.getMessageToUser(), e);
			htd = null;
		} catch (Exception e) {
			mw.onError("Error while starting training job: " + e.getMessage(), e.getMessage(), e);
			htd = null;
		}
	}

	private void showSuccessMessage(List<String> jobIds) {
		mw.registerJobStatusUpdateAndShowSuccessMessage(jobIds.toArray(new String[0]));
//		showSuccessMessage(jobIds.toArray(new String[0]));
	}

	private void showSuccessMessage(String... jobIds) {
		mw.registerJobStatusUpdateAndShowSuccessMessage(jobIds);
		
//		if (!CoreUtils.isEmpty(jobIds)) {
//			logger.debug("started " + jobIds.length + " jobs");
//			String jobIdsStr = mw.registerJobsToUpdate(jobIds);
//			store.sendJobListUpdateEvent();
//			mw.updatePageLock();
//
//			String jobsStr = jobIds.length > 1 ? "jobs" : "job";
//			final String title = jobIds.length + " " + jobsStr + " started!";
//			final String msg = "IDs:\n " + jobIdsStr;
//			
//			//Dialog may block the GUI. 
////			DialogUtil.showInfoMessageBox(tw.getShell(), title, msg);
//			
//			//show balloon tip on jobs button instead
//			DialogUtil.showBallonToolTip(mw.getUi().getJobsButton(), null, title, msg);
//		}
	}
	
	private boolean isDocLoadedNeeded(Object s) {
		if (s == tw.p2palaTrainBtn || s==tw.p2palaBtn) {
			return false;
		}
		
		return true;
	}
	
	private boolean isPageLoadedNeeded(Object s) {
		if (s == tw.p2palaTrainBtn || s==tw.p2palaBtn) {
			return false;
		}
		
		return true;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		Object s = e.getSource();

		if (isDocLoadedNeeded(s) && !store.isDocLoaded()) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Not available", "No document loaded!");
			return;
		}

		if (isPageLoadedNeeded(s) && !store.isPageLoaded()) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Not available", "No page loaded!");
			return;
		} else if (!store.isLoggedIn()) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Not available", "You are not logged in!");
			return;
		} else if (isDocLoadedNeeded(s) && store.isLocalDoc()) {
			DialogUtil.showErrorMessageBox(mw.getShell(), "Not available",
					"The tools are only available for remote documents!");
			return;
		}

		try {
			PcGtsType pageData = store.getTranscript().getPageData();
			List<String> jobIds = new ArrayList<>();

			int colId = store.getCurrentDocumentCollectionId();

			if (needsRegions(pageData, s)) {
				DialogUtil.showErrorMessageBox(mw.getShell(), "Error", "You have to define text regions first!");
				return;
			}

			if (isLayoutAnalysis(s) && store.isTranscriptEdited()) {
				mw.saveTranscription(false);
			}

			// new: check here if user wants to store or not: e.g layout corrected and HTR
			// started but not saved before
			if (!mw.saveTranscriptDialogOrAutosave()) {
				// if user canceled this
				return;
			}

			if (s == tw.startLaBtn) {
				logger.debug("PARAMETERS = " + tw.laComp.getParameters());
				String pageStr = (!tw.laComp.isCurrentTranscript() ? tw.laComp.getPages() : Integer.toString(store.getPage().getPageNr()));
				
//				logger.debug("docs selection " + tw.laComp.isDocsSelection());
//				logger.debug(" tw.laComp.getDocs() != null " +  (tw.laComp.getDocs() != null));
//				logger.debug("Storage.getInstance().isAdminLoggedIn() " +  (Storage.getInstance().isAdminLoggedIn()));
				
				String msg = (tw.laComp.isDocsSelection() && tw.laComp.getDocs() != null) ? "Do you really want to start the LA for "+ tw.laComp.getDocs().size() + " docs in this collection?" : "Do you really want to start the LA for page(s) " + pageStr + "  ?";
				
				List<String> rids = mw.getSelectedRegionIds();
				String configInfoStr = null;
				//get information on config for configurable methods
				if(tw.laComp.isSelectedMethodConfigurable()) {
					configInfoStr = new CITlabAdvancedLaConfigDialog(mw.getShell(), tw.laComp.getParameters(), tw.laComp.getJobImpl()).getConfigInfoString();
				}
				if (tw.laComp.isCurrentTranscript() && !CoreUtils.isEmpty(rids)) {
					configInfoStr+="\nSelected regions: "+rids;
				}
				
				if(configInfoStr != null) {
					msg += "\n\nSettings:\n" + configInfoStr;
				}
				
				if (DialogUtil.showYesNoDialog(mw.getShell(), "Layout recognition", msg)!=SWT.YES) {
					return;
				}
				
				JobImpl jobImpl = tw.laComp.getJobImpl();
				ParameterMap params = tw.laComp.getParameters();
				if (JobImpl.FinereaderSepJob.equals(jobImpl)) {
					String ocrType = tw.laComp.isEnrichOldTranscript()? "Sep_enrich" : "Sep";
					params.addParameter("ocrType", ocrType);
				}
				if(logger.isDebugEnabled()) {
					params.getParamMap().entrySet().stream().forEach(en -> logger.debug("{} -> {}", en.getKey(), en.getValue()));
				}
				
				if (tw.laComp.isDocsSelection() && tw.laComp.getDocs() != null){
					if(JobImpl.FinereaderLaJob.equals(jobImpl)) {
						//OCR is another endpoint and it can't yet handle descriptors...
						for (DocSelection docSel : tw.laComp.getDocs()){
							logger.debug("Start printed block detection for doc {}, pages = {}", docSel.getDocId(), docSel.getPages());
							String jobIdStr = store.getConnection().runTypewrittenBlockSegmentation(colId, docSel.getDocId(), docSel.getPages());
							jobIds.add(jobIdStr);
						}
					} else if(JobImpl.FinereaderSepJob.equals(jobImpl)) {
						//OCR is another endpoint and it can't yet handle descriptors...
						for (DocSelection docSel : tw.laComp.getDocs()){
							logger.debug("Start separator detection for doc {}, pages = {}", docSel.getDocId(), docSel.getPages());
							String jobIdStr = store.getConnection().runSeparatorSegmentation(colId, docSel.getDocId(), docSel.getPages(), params.getParameterValue("ocrType"));
							jobIds.add(jobIdStr);
						}
					} else {
						// NEW: use DocSelection objects
						for (DocSelection docSel : tw.laComp.getDocs()){
							logger.debug("start LA for docs: " + docSel.getDocId());
							List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
							// as LA call does not support specifying a docId & pagesStr (TODO!) we have to get the pageIds from the server to construct a DSD object
							DocumentSelectionDescriptor dsd = store.getDocumentSelectionDescriptor(colId, docSel);
							logger.debug("nr of pages in la descriptor: "+dsd.getPages().size());
							dsds.add(dsd);
							List<String> tmp = store.analyzeLayoutOnDocumentSelectionDescriptor(
									dsds, tw.laComp.isDoBlockSeg(), tw.laComp.isDoLineSeg(), tw.laComp.isDoWordSeg(), 
									false, false, tw.laComp.getJobImpl().toString(), params
									);
							jobIds.addAll(tmp);
						}
					}
					
					// OLD
//					/*
//					 * ToDo: we could start LA for all docs at once in a single job instead of starting it for each doc separately
//					 * this way the jobs are parallelized automatically, results will be finsished earlier
//					 * but job list will be much longer
//					 */
//					for (DocumentSelectionDescriptor docDescr : tw.laComp.getDocs()){
//						logger.debug("start LA for docs: " + docDescr.getDocId());
//						List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
//						dsds.add(docDescr);
//						List<String> tmp = store.analyzeLayoutOnDocumentSelectionDescriptor(
//								dsds, tw.laComp.isDoBlockSeg(), tw.laComp.isDoLineSeg(), tw.laComp.isDoWordSeg(), 
//								false, false, tw.laComp.getJobImpl().toString(), tw.laComp.getParameters()
//								);
//						jobIds.addAll(tmp);
//						
//					}
				}

				else if (!tw.laComp.isCurrentTranscript()) {
					logger.debug("running la on pages: " + tw.laComp.getPages());
					jobIds = store.analyzeLayoutOnLatestTranscriptOfPages(tw.laComp.getPages(),
							tw.laComp.isDoBlockSeg(), tw.laComp.isDoLineSeg(), tw.laComp.isDoWordSeg(), false, false,
							tw.laComp.getJobImpl().toString(), params);
				} else {
					logger.debug("running la on current transcript and selected rids: " + CoreUtils.join(rids));
					jobIds = store.analyzeLayoutOnCurrentTranscript(rids, tw.laComp.isDoBlockSeg(),
							tw.laComp.isDoLineSeg(), tw.laComp.isDoWordSeg(), false, false,
							tw.laComp.getJobImpl().toString(), params);
				}
			}
			else if (s == tw.polygon2baselinesBtn || s == tw.baseline2PolygonBtn) {
				boolean isPolygon2Baseline = s == tw.polygon2baselinesBtn;
				String jobImpl = isPolygon2Baseline ? JobImpl.NcsrOldLaJob.toString() : JobImpl.UpvlcLaJob.toString();
				String btnName = isPolygon2Baseline ? "polygon2baselinesBtn" : "baseline2PolygonBtn";

				if (!tw.otherToolsPagesSelector.isCurrentTranscript()) {
					logger.debug(btnName + " on pages: " + tw.otherToolsPagesSelector.getPagesStr());
					jobIds = store.analyzeLayoutOnLatestTranscriptOfPages(tw.otherToolsPagesSelector.getPagesStr(),
							false, false, false, isPolygon2Baseline, !isPolygon2Baseline, jobImpl, null);
				} else {
					logger.debug(btnName + " on current transcript");
					List<String> rids = mw.getSelectedRegionIds();

					jobIds = store.analyzeLayoutOnCurrentTranscript(rids, false, false, false, isPolygon2Baseline,
							!isPolygon2Baseline, jobImpl, null);
				}
			}
//			else if (s == tw.p2palaTrainBtn) {
//				logger.debug("p2palaTrainBtn pressed...");
//				if (!store.getConnection().isUserAllowedForJob(JobImpl.P2PaLATrainJob.toString())) {
//					DialogUtil.showErrorMessageBox(tw.getShell(), "Not allowed!", "You are not allowed to start a P2PaLA training.\n If you are interested, please apply at email@transkribus.eu");
//					return;
//				}
//				P2PaLATrainDialog d = new P2PaLATrainDialog(tw.getShell());
//				if (d.open() == IDialogConstants.OK_ID) {
//					P2PaLATrainUiConf conf = d.getConf();
//					if (conf==null) {
//						return;
//					}
//					P2PaLATrainJobPars jobPars = conf.toP2PaLATrainJobPars();
//					String jobId = store.getConnection().trainP2PaLAModel(colId, jobPars);
//					jobIds.add(jobId);
//					logger.info("Started P2PaLA training job "+jobId);
//				}
//			}
			else if (s == tw.p2palaBtn) {
				P2PaLAConfDialog diag = new P2PaLAConfDialog(tw.getShell()/*, Storage.getInstance().getP2PaLAModels()*/);
				if (diag.open()==IDialogConstants.OK_ID) {
					P2PaLARecogUiConf conf = diag.getConf();
					if (conf != null) {
						String jobImpl = JobImpl.P2PaLAJob.toString();
						TrpP2PaLA model = conf.model;
						if (model == null) {
							DialogUtil.showErrorMessageBox(tw.getShell(), "No model selected", "Please select a P2PaLA model");
							return;
						}
						logger.debug("Selected P2PaLA model: "+model);
						
						String msg = (diag.isDocsSelected() && diag.getDocs() != null) ? "Do you really want to start P2PaLA for "+ diag.getDocs().size() + " docs in this collection?" : "Do you really want to start P2PaLA for all selected page(s)?";
						
						if (DialogUtil.showYesNoDialog(mw.getShell(), "P2PaLA", msg)!=SWT.YES) {
							return;
						}
						
						ParameterMap pm = new ParameterMap();
						pm.addIntParam(JobConst.PROP_MODEL_ID, model.getModelId());
						pm.addParameter(JobConst.PROP_MODELNAME, model.getName());
						if (conf.minArea!=null) {
							pm.addParameter(JobConstP2PaLA.MIN_AREA_PAR, conf.minArea);	
						}
						pm.addParameter(JobConstP2PaLA.RECTIFY_REGIONS_PAR, conf.rectifyRegions);
						pm.addParameter(JobConstP2PaLA.ENRICH_EXISTING_TRANSCRIPTIONS_PAR, conf.enrichExistingTranscriptions);
						pm.addParameter(JobConstP2PaLA.LABEL_REGIONS_PAR, conf.labelRegions);
						pm.addParameter(JobConstP2PaLA.LABEL_LINES_PAR, conf.labelLines);
						pm.addParameter(JobConstP2PaLA.LABEL_WORDS_PAR, conf.labelWords);
						
						if (diag.isDocsSelected() && diag.getDocs() != null){
							// NEW
							for (DocSelection docSel : diag.getDocs()){
								logger.debug("start p2pala for doc: " + docSel.getDocId());
								List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
								// as LA call does not support specifying a docId & pagesStr (TODO!) we have to get the pageIds from the server to construct a DSD object
								DocumentSelectionDescriptor dsd = store.getDocumentSelectionDescriptor(colId, docSel);
								logger.debug("nr of pages in p2pala descriptor: "+dsd.getPages().size());
								dsds.add(dsd);
								List<String> tmp = store.analyzeLayoutOnDocumentSelectionDescriptor(dsds, true, true, false, false, false, jobImpl, pm);
								jobIds.addAll(tmp);							
							}							
							// OLD
//							for (DocumentSelectionDescriptor docDescr : diag.getDocs()) {
//								logger.debug("start p2pala for doc: " + docDescr.getDocId());
//								List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
//								dsds.add(docDescr);
//								List<String> tmp = store.analyzeLayoutOnDocumentSelectionDescriptor(dsds, true, true, false, false, false, jobImpl, pm);
//								jobIds.addAll(tmp);								
//							}
						}
						else if (!conf.currentTranscript) {
							logger.debug("p2palaBtn on pages: " + tw.otherToolsPagesSelector.getPagesStr());
							jobIds = store.analyzeLayoutOnLatestTranscriptOfPages(conf.pagesStr,
									true, true, false, false, false, jobImpl, pm);
						} else {
							logger.debug("p2palaBtn on current transcript");
//							List<String> rids = getSelectedRegionIds();
							jobIds = store.analyzeLayoutOnCurrentTranscript(null, true, true, false, false, false, jobImpl, pm);
						}
					}
					else {
						DialogUtil.showErrorMessageBox(tw.getShell(), "No configuration", "Please select a P2PaLA model");
						return;
					}
				}
				//// -------------- old code:
//				String jobImpl = JobImpl.P2PaLAJob.toString();
//				TrpP2PaLAModel model = tw.getSelectedP2PaLAModel();
//				if (model == null) {
//					DialogUtil.showErrorMessageBox(tw.getShell(), "No model selected", "Please select a P2PaLA model");
//					return;
//				}
//				logger.debug("Selected P2PaLA model: "+model);
//				ParameterMap pm = new ParameterMap();
//				pm.addIntParam(JobConst.PROP_MODEL_ID, model.getId());
//				pm.addParameter(JobConst.PROP_MODELNAME, model.getName());
//				
//				if (!tw.otherToolsPagesSelector.isCurrentTranscript()) {
//					logger.debug("p2palaBtn on pages: " + tw.otherToolsPagesSelector.getPagesStr());
//					jobIds = store.analyzeLayoutOnLatestTranscriptOfPages(tw.otherToolsPagesSelector.getPagesStr(),
//							true, true, false, false, false, jobImpl, pm);
//				} else {
//					logger.debug("p2palaBtn on current transcript");
////					List<String> rids = getSelectedRegionIds();
//					jobIds = store.analyzeLayoutOnCurrentTranscript(null, true, true, false, false, false, jobImpl, pm);
//				}
			}
			else if (s == tw.t2iBtn) {
				Text2ImageConf conf = (Text2ImageConf) tw.t2iBtn.getData();
				Text2ImageSimplifiedDialog diag = new Text2ImageSimplifiedDialog(tw.getShell(), conf);
				if (diag.open()==IDialogConstants.OK_ID) {
					conf = diag.getConfig();
					logger.debug("setting t2i conf to: "+conf);
					tw.t2iBtn.setData(conf);
					
					// now run T2I:
					String jobImpl = JobImpl.T2IJob.toString();
					
//					Text2ImageConf conf = (Text2ImageConf) tw.t2iConfBtn.getData();
					logger.debug("starting t2i - conf = "+conf);
					
					TrpModelMetadata htr = conf.model;
					if (htr == null) {
						DialogUtil.showErrorMessageBox(tw.getShell(), "No model selected", "Please select a base model for Text2Image");
						return;
					}
					
					String msg = (conf.isDocsSelection && conf.docsSelected != null) ? "Do you really want to start t2i for "+ conf.docsSelected.size() + " docs in this collection?" : "Do you really want to start t2i for all selected page(s)?";
					
					if (DialogUtil.showYesNoDialog(mw.getShell(), "t2i confirmation message", msg)!=SWT.YES) {
						return;	}
					
					
					ParameterMap pm = new ParameterMap();
					pm.addIntParam(JobConst.PROP_MODEL_ID, htr.getModelId());
					pm.addBoolParam(JobConst.PROP_PERFORM_LAYOUT_ANALYSIS, conf.performLa);
					pm.addBoolParam(JobConst.PROP_REMOVE_LINE_BREAKS, conf.removeLineBreaks);
					pm.addDoubleParam(JobConst.PROP_THRESHOLD, conf.threshold);
					
					if (conf.skip_word!=null) {
						pm.addDoubleParam(JobConst.PROP_T2I_SKIP_WORD, conf.skip_word);
					}
					if (conf.skip_bl!=null) {
						pm.addDoubleParam(JobConst.PROP_T2I_SKIP_BASELINE, conf.skip_bl);
					}
					if (conf.jump_bl!=null) {
						logger.debug("setting jump_bl = "+conf.jump_bl);
						pm.addDoubleParam(JobConst.PROP_T2I_JUMP_BASELINE, conf.jump_bl);
					}
					if (conf.hyphen!=null) {
						logger.debug("setting hyphen = "+conf.hyphen);
						pm.addDoubleParam(JobConst.PROP_T2I_HYPHEN, conf.hyphen);						
					}
					if (conf.editStatus!=null) {
						pm.addParameter(JobConst.PROP_EDIT_STATUS, conf.editStatus.getStr());
					}
					
					if (conf.isDocsSelection && conf.docsSelected != null){
						// NEW
						for (DocSelection docSel : conf.docsSelected){
							logger.debug("start ti2 for doc: " + docSel.getDocId());
							List<DocumentSelectionDescriptor> dsds = new ArrayList<>();
							// as t2i call does not support specifying a docId & pagesStr (TODO!) we have to get the pageIds from the server to construct a DSD object
							DocumentSelectionDescriptor dsd = store.getDocumentSelectionDescriptor(colId, docSel);
							logger.debug("nr of pages in t2i descriptor: "+dsd.getPages().size());
							dsds.add(dsd);
							//TODO: check if it starts t2i properly
							List<String> tmp = store.analyzeLayoutOnDocumentSelectionDescriptor(dsds, true, true, false, false, false, jobImpl, pm);
							jobIds.addAll(tmp);							
						}	
					}
					
					else if (!conf.currentTranscript) {
						logger.debug("t2i on pages: " + conf.pagesStr);
						jobIds = store.analyzeLayoutOnLatestTranscriptOfPages(conf.pagesStr, true, true, false, false, false, jobImpl, pm);
					} else {
						logger.debug("t2i on current transcript");
//						List<String> rids = getSelectedRegionIds();
						jobIds = store.analyzeLayoutOnCurrentTranscript(null, true, true, false, false, false, jobImpl, pm);
					}
				}
				// OLD
//				String jobImpl = JobImpl.T2IJob.toString();
//				
//				Text2ImageConf conf = (Text2ImageConf) tw.t2iConfBtn.getData();
//				logger.debug("starting t2i - conf = "+conf);
//				
//				TrpHtr htr = conf.model;
//				if (htr == null) {
//					DialogUtil.showErrorMessageBox(tw.getShell(), "No model selected", "Please select a base model for Text2Image");
//					return;
//				}
//				ParameterMap pm = new ParameterMap();
//				pm.addIntParam(JobConst.PROP_MODEL_ID, htr.getHtrId());
//				pm.addBoolParam(JobConst.PROP_PERFORM_LAYOUT_ANALYSIS, conf.performLa);
//				pm.addBoolParam(JobConst.PROP_REMOVE_LINE_BREAKS, conf.removeLineBreaks);
//				pm.addDoubleParam(JobConst.PROP_THRESHOLD, conf.threshold);
//				
//				if (!tw.otherToolsPagesSelector.isCurrentTranscript()) {
//					logger.debug("t2i on pages: " + tw.otherToolsPagesSelector.getPagesStr());
//					jobIds = store.analyzeLayoutOnLatestTranscriptOfPages(tw.otherToolsPagesSelector.getPagesStr(),
//							true, true, false, false, false, jobImpl, pm);
//				} else {
//					logger.debug("t2i on current transcript");
////					List<String> rids = getSelectedRegionIds();
//					jobIds = store.analyzeLayoutOnCurrentTranscript(null, true, true, false, false, false, jobImpl, pm);
//				}
			}

			// struct analysis:
			// else if (s == tw.polygon2baselinesBtn) {
			// mw.analyzePageStructure(tw.detectPageNumbers.getSelection(),
			// tw.detectRunningTitles.getSelection(),
			// tw.detectFootnotesCheck.getSelection());
			// }

//			else if (s == tw.computeWerBtn) {
//
//				TrpTranscriptMetadata ref = (TrpTranscriptMetadata) tw.refVersionChooser.selectedMd;
//				TrpTranscriptMetadata hyp = (TrpTranscriptMetadata) tw.hypVersionChooser.selectedMd;
//
//				if (ref != null && hyp != null) {
//					
//					if(ToolsWidget.IS_LEGACY_WER_GROUP) {
//						logger.debug("Computing WER: " + ref.getKey() + " - " + hyp.getKey());
//						final String result = store.computeWer(ref, hyp);
//						MessageBox mb = new MessageBox(TrpMainWidget.getInstance().getShell(), SWT.ICON_INFORMATION | SWT.OK);
//						mb.setText("Result");
//						mb.setMessage(result);
//						mb.open();
//					} else {					
//						logger.debug("Computing WER: " + ref.getKey() + " - " + hyp.getKey());
//	
//						TrpErrorRateResult resultErr = store.computeErrorRate(ref, hyp);
//						logger.debug("resultError was calculated : "+resultErr.getCer());
//						ErrorRateDialog dialog = new ErrorRateDialog(mw.getShell(), resultErr);
//						dialog.open();
//
//					}
//				}
//				
//			}
			else if (s == tw.computeWerBtn) {

				TrpTranscriptMetadata ref = (TrpTranscriptMetadata) tw.refVersionChooser.selectedMd;
				TrpTranscriptMetadata hyp = (TrpTranscriptMetadata) tw.hypVersionChooser.selectedMd;

				if (ref != null && hyp != null) {
					
					logger.debug("Computing WER: " + ref.getKey() + " - " + hyp.getKey());
					TrpErrorRateResult result = store.computeErrorRate(ref, hyp);
					final String resultText = "Word Error Rate:\n"+ result.getWerDouble()+"\n Character Error Rate:\n"+result.getCerDouble();
					MessageBox mb = new MessageBox(TrpMainWidget.getInstance().getShell(), SWT.ICON_INFORMATION | SWT.OK);	
					mb.setText("Result");
					mb.setMessage(resultText);
					mb.open();
					
				}
				
			} else if (s == tw.computeAdvancedBtn) {
				
				ErrorRateAdvancedDialog dialog = new ErrorRateAdvancedDialog(mw.getShell());
				dialog.open();
				
				
			} else if (s == tw.compareSamplesBtn) {
				SamplesCompareDialog dialog = new SamplesCompareDialog(mw.getShell());
				dialog.open();
				
				
			}else if (s == tw.compareVersionsBtn) {
				
				String diffText = mw.getTextDifferenceOfVersions(false);
				mw.openVersionsCompareDialog(diffText);
				
			}
			// else if (tw.trComp.isHtr() && s == tw.trComp.getTrainBtn()) {
			// if(htd != null) {
			// htd.setVisible();
			// } else {
			// htd = new HtrTrainingDialog(mw.getShell());
			// if(htd.open() == IDialogConstants.OK_ID) {
			// CitLabHtrTrainConfig config = htd.getConfig();
			// String jobId = store.runHtrTraining(config);
			// jobIds.add(jobId);
			// }
			// htd = null;
			// }
			// }
			else if (tw.trComp.isHtr() && s == tw.trComp.getRunBtn()) {
				if (trd2 != null) {
					logger.debug("htr diag set visible");
					trd2.setVisible();
				} else {
					trd2 = new HtrTextRecognitionDialog(mw.getShell());
					if (trd2.open() == IDialogConstants.OK_ID) {
						
						final String pages;
						TextRecognitionConfig config = trd2.getConfig();
						String msg;
						try {
							CostEstimateMessageBuilder messageBuilder = new CostEstimateMessageBuilder();
							final boolean isDocsSelection = trd2.isDocsSelection() && trd2.getDocs() != null;
							if (isDocsSelection) {
								pages = null;
								msg = "Do you really want to start the HTR for " + trd2.getDocs().size() + " docs in this collection?";
								msg += "\n" + messageBuilder.buildHtrCostEstimateMessage(colId, trd2.getDocSelectionDetails(), config);
							} else {
								pages = trd2.getPages();
								msg = "Do you really want to start the HTR for page(s) " + pages + " ?";
								msg += "\n" + messageBuilder.buildHtrCostEstimateMessage(colId, store.getDoc().getMd(), pages, config);
							}
							
							if (DialogUtil.showYesNoDialog(mw.getShell(), "Handwritten Text Recognition", msg)!=SWT.YES) {
								return;
							}

							if (isDocsSelection){
								// NEW: use DocSelection here, as they contain the pages string for each doc:
								for (DocSelection docSel : trd2.getDocs()) {
									DocumentSelectionDescriptor dsd = store.getDocumentSelectionDescriptor(colId, docSel);
									logger.debug("dsd = "+dsd);
									String jobId = store.runHtr(dsd, config);
									jobIds.add(jobId);

									// OLD: call does not work when docSel.getPages() is null!
//									logger.debug("sel: "+docSel+" docId: "+docSel.getDocId()+" pages: "+docSel.getPages());
//									String jobId = store.runHtr(docSel.getDocId(), docSel.getPages(), config);
								}
								// OLD: use DocumentSelectionDescriptor which do *not* contain individual pages
//								for (DocumentSelectionDescriptor docDescr : trd2.getDocs()){
//									logger.debug("start HTR for all pages with docId = {}", docDescr.getDocId());
//
//									String tmp = store.runHtr(docDescr, config);
//									jobIds.add(tmp);
//								}
							} else {
								String jobId = store.runHtr(pages, config);
								jobIds.add(jobId);
							}
						} finally {
							trd2 = null;
						}
					}
					trd2 = null;
				}
			}
			else if (tw.trComp.isTranskribusOcr() && s == tw.trComp.getRunBtn()) {
				CITlabOcrConfigDialog d = new CITlabOcrConfigDialog(mw.getShell());
				if (d.open() == IDialogConstants.OK_ID) {
					final String pages;
					String ocrType = "Transkribus";
					
					String msg;
					final boolean isDocsSelection = d.isDocsSelection() && d.getDocs() != null;
					CostEstimateMessageBuilder messageBuilder = new CostEstimateMessageBuilder();
					if (isDocsSelection) {
						pages = null;
						msg = "Do you really want to start the Transkribus-OCR-Job for " + d.getDocs().size() + " docs in this collection?";
						msg += "\n" + messageBuilder.buildOcrCostEstimateMessage(colId, d.getDocSelectionDetails(), ocrType, false);
					} else {
						pages = d.getPages();
						msg = "Do you really want to start the Transkribus-OCR-Job for page(s) " + pages + " ?";
						msg += "\n" + messageBuilder.buildOcrCostEstimateMessage(colId, store.getDoc().getMd(), pages, ocrType, false);
					}

					if (DialogUtil.showYesNoDialog(mw.getShell(), "Transkribus OCR", msg) != SWT.YES) {
						return;
					}
					
					if (isDocsSelection){
						// NEW: use DocSelection here, as they contain the pages string for each doc:
						for (DocSelection docSel : d.getDocs()) {
							logger.info("starting transkribus-ocr for doc " + docSel.getDocId() + ", pages " + docSel.getPages() + " and col "
									+ colId);
							String jobId = store.runOcr(colId, docSel.getDocId(), docSel.getPages(), null, ocrType);
							jobIds.add(jobId);
						}

					} else {
						logger.info("starting transkribus-ocr for doc " + store.getDocId() + ", pages " + pages + " and col "
								+ colId);
						String jobId = store.runOcr(colId, store.getDocId(), pages, null, ocrType);
						jobIds.add(jobId);
					}
				}
			}
			else if (tw.trComp.isOcr() && s == tw.trComp.getRunBtn()) {
				if (od != null) {
					od.setVisible();
				} else {
					od = new OcrDialog(mw.getShell());
					int ret = od.open();

					if (ret == IDialogConstants.OK_ID) {
						final String pages;
						final OcrConfig config = od.getConfig();
						String ocrType = tw.trComp.isTranskribusOcr() ? "Transkribus" : "Legacy";
						logger.debug("ocrType = "+ocrType);
						String msg;						
						
						final boolean isDocsSelection = od.isDocsSelection() && od.getDocs() != null;
						if (isDocsSelection) {
							pages = null;
							msg = "Do you really want to start the OCR for "+ od.getDocs().size() + " docs in this collection?";
						} else {
							pages = od.getPages();
							msg = "Do you really want to start the HTR for page(s) " + pages + " ?";
						}

						if (DialogUtil.showYesNoDialog(mw.getShell(), "Optical Character Recognition", msg)!=SWT.YES) {
							od = null;
							return;
						}
						
						if (isDocsSelection){
							// NEW: use DocSelection here, as they contain the pages string for each doc:
							for (DocSelection docSel : od.getDocs()) {
								//DocumentSelectionDescriptor dsd = store.getDocumentSelectionDescriptor(colId, docSel);
								//logger.debug("dsd = "+dsd);								
								logger.info("starting ocr for doc " + docSel.getDocId() + ", pages " + docSel.getPages() + " and col "
										+ colId);
								String jobId = store.runOcr(colId, docSel.getDocId(), docSel.getPages(), config, ocrType);
								jobIds.add(jobId);
							}

						} else {
							logger.info("starting ocr for doc " + store.getDocId() + ", pages " + pages + " and col "
									+ colId);
							String jobId = store.runOcr(colId, store.getDocId(), pages, config, ocrType);
							jobIds.add(jobId);
						}
						

					}
					od = null;
				}
			} else if(s == tw.duButton){
				DUDecodeDialog duDecodeDialog = new DUDecodeDialog(mw.getShell());
				int ret = duDecodeDialog.open();

				if (ret == IDialogConstants.OK_ID) {
					String pageString = duDecodeDialog.getPages();
					String jobId = store.runDocUnderstanding(store.getDocId(), pageString, 2);
					logger.debug("started DU job: "+jobId);
					jobIds.add(jobId);
				}


			}

			showSuccessMessage(jobIds);

		} catch (TrpClientErrorException | TrpServerErrorException ee) {
			final int status = ee.getResponse().getStatus();
			if (status == 400) {
				logger.error(ee.getMessage(), ee);
				DialogUtil.showErrorMessageBox(this.mw.getShell(), "Error", ee.getMessageToUser());
			} else if (Status.PAYMENT_REQUIRED.equals(Status.fromStatusCode(status))) {
				logger.warn(ee.getMessage());
				int choice = MessageDialog.open(MessageDialog.INFORMATION, this.mw.getShell(), 
						"Credits Depleted", ee.getMessageToUser(), SWT.NONE, new String[] { "OK", "Visit the shop at readcoop.eu" });
				if(choice == 1) {
					DesktopUtil.browse("https://readcoop.eu", "Could not open system browser.\nPlease visit the shop at https://readcoop.eu", this.mw.getShell());
				} else {
					logger.debug("Insufficient credits dialog user choice was {}", choice);
				}
			} else {
				mw.onError("Error", ee.getMessageToUser(), ee);
			}
		} catch (ClientErrorException cee) {
			final int status = cee.getResponse().getStatus();
			if (status == 400) {
				DialogUtil.showErrorMessageBox(this.mw.getShell(), "Error",
						"A job of this type already exists for this page/document!");
			} else {
				mw.onError("Error", cee.getMessage(), cee);
			}

			// mw.onError("Error", cee.getMessage(), cee);
		} catch (Exception ex) {
			mw.onError("Error", ex.getMessage(), ex);
		} finally {
			// laDiag = null;
		}
		return;
	}

	// private void setButtonsEnabled(boolean isEnabled){
	// lw.getBlocksBtn().setEnabled(isEnabled);
	// lw.getBlocksInPsBtn().setEnabled(isEnabled);
	// lw.getLinesBtn().setEnabled(isEnabled);
	// }

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	// @Override public void keyTraversed(TraverseEvent e) {
	// Object s = e.getSource();
	// if (s == tw.languageCombo && e.detail == SWT.TRAVERSE_RETURN) {
	// logger.debug("enter pressed on language field!");
	//// mw.saveDocMetadata();
	// }
	// }
	
	public void handleTranscriptLoadEvent(TranscriptLoadEvent arg) {
		tw.refVersionChooser.setToGT();
		tw.hypVersionChooser.setToCurrent();
	}
	
	public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
		boolean duVisible = Storage.getInstance().isLoggedInAtTestServer();		
		//tw.setDuVisible(duVisible);
	}
}
