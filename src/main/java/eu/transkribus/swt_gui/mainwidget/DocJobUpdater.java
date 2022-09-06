package eu.transkribus.swt_gui.mainwidget;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.dialogs.TrpMessageDialog;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * Starts a thread that periodically updates jobs registered via the {@link DocJobUpdater#registerJobToUpdate(String)} method
 * @author sebastian
 */
public class DocJobUpdater {
	private final static Logger logger = LoggerFactory.getLogger(DocJobUpdater.class);
	
	Runnable r;
	Thread t;
	Storage store = Storage.getInstance();
	boolean started=false;
	boolean stop=false;
	
	final public int UPDATE_TIME_MS = 3000;
		
	int nExc=0;
	
	Set<String> jobsToUpdate = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());	
	TrpMainWidget mw;
		
	public DocJobUpdater(TrpMainWidget mw) {
		this.mw = mw;
		
		store.addListener(new IStorageListener() {
			public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
				if (!arg.login) { // on logout, clear all jobsToUpdate
					logger.debug("logout -> clearing jobs to update!");
					jobsToUpdate.clear();
				}
			}
		});
				
		r = new Runnable() {
			@Override
			public void run() {
				started = true;
				logger.debug("starting DocJobUpdater");

				while (true) {
					String jobId = "";
					try {
						Thread.sleep(UPDATE_TIME_MS);
						
						if (stop) {
							logger.debug("stopping DocJobUpdater");
							break;
						}

						if (jobsToUpdate.isEmpty())
							continue;

						logger.trace("jobs to update: " + jobsToUpdate.size());

						store.checkConnection(true);

						// update jobs and remove from list if necessary:
						
						for (Iterator<String> jobIt = jobsToUpdate.iterator(); jobIt.hasNext();) {
							jobId = jobIt.next();
							TrpJobStatus job = store.getConnection().getJob(jobId);

							store.sendJobUpdateEvent(job);

							if (job.isFinished()) {
								logger.debug("removing finished job " + jobId + ", nr of unfinished jobs: "
										+ jobsToUpdate.size());
								jobsToUpdate.remove(jobId);
								checkIfFinishedJobAffectsOpenedPage(job);
								//Added for batch jobs - after the job has finished we need to reload the transcripts in the storage
								store.reloadDocWithAllTranscripts();
							}
						}

						nExc = 0;
					} catch (SessionExpiredException | NoConnectionException ex) {
						logger.debug("Session expired or no connection - clearing jobsToUpdate");
						jobsToUpdate.clear();
					}
					catch (Exception ex) {
						logger.error(ex.getMessage(), ex);
						++nExc;
						logger.debug("nr of subsequent exceptions: " + nExc+" jobId: "+jobId);
						if (nExc > 3 && !StringUtils.isEmpty(jobId)) {
							logger.debug("removing job from jobsToUpdate: "+jobId);
							jobsToUpdate.remove(jobId);
							nExc = 0;
						}
					}
				}
			}
		};
		
		t = new Thread(r);
		t.start();
	}
	
	private void checkIfFinishedJobAffectsOpenedPage(TrpJobStatus job) {
		
		boolean isThisDocOpen = store.isDocLoaded() && store.getDoc().getId()==job.getDocId();
		// reload current page if page job for this page is finished:
		// (only ask question to reload page!!)
//		logger.debug("the page nr of the job was " + job.getPageid());
//		logger.debug("job pages " + job.getPages());
		if (isThisDocOpen && job.isFinished()) {
			Display.getDefault().asyncExec(() -> {
				if (job.isFailed()) {
					logger.error("A job for the current document failed: "+job);

					TrpMessageDialog.showErrorDialog(mw.getShell(), "A job for this document failed", job.getDescription(), job.getStackTrace(), null);
					// TODO: show stacktrace of error... job.getStackTrace()
//					DialogUtil.showErrorMessageBox(mw.getShell(), "A job for this page failed", job.getDescription());
				}
				//pageNr is deprecated but maybe still used by some jobs 
				else if (store.getPageIndex() == (job.getPageNr()-1)) {
//					if (job.getJobImpl().equals(JobImpl.DocExportJob.toString())) {
//						ShowServerExportLinkDialog linkDiag = new ShowServerExportLinkDialog(mw.getShell(), job.getResult());
//						linkDiag.open();
//						return;
//					}
					// reload page if doc and page is open:					
					if (DialogUtil.showYesNoDialog(mw.getShell(), "A job for this page finished", "Do you want to reload the current page? By saying 'No' you can save your latest changes first!") == SWT.YES) {
						logger.debug("reloading page!");
						mw.reloadCurrentPage(true, null, null);						
					}
				}
				//page string set e.g. 1-30; get list of page indices and check if loaded page is affected
				else if(job.getPages() != null) {
					try {
						for (Integer pageIdx : CoreUtils.parseRangeListStrToList(job.getPages(),store.getDoc().getNPages())){
							if (store.getPageIndex() == pageIdx){
								if (DialogUtil.showYesNoDialog(mw.getShell(), "A job for this page finished", "Do you want to reload the current page? By saying 'No' you can save your latest changes first!") == SWT.YES) {
									logger.debug("reloading page!");
									mw.reloadCurrentPage(true, null, null);						
								}
								break;
							}
							
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			});
		}
	}
	
	public void registerJobToUpdate(String jobId) {
		jobsToUpdate.add(jobId);
	}

}
