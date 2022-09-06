package eu.transkribus.swt_gui.credits;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.pagination_tables.CreditTransactionsByJobPagedTableWidget;
import eu.transkribus.swt_gui.pagination_tables.JobTableWidgetPagination;

/**
 * A SashForm with a {@link JobTableWidgetPagination} (left) and a {@link CreditTransactionsByJobPagedTableWidget} (right).
 * A TraverseListener will load the respective credit transaction when a job is selected.
 */
public class JobTransactionSashForm extends SashForm {
	Logger logger = LoggerFactory.getLogger(JobTransactionSashForm.class);
	protected JobTableWidgetPagination jobsTable;
	protected CreditTransactionsByJobPagedTableWidget transactionsTable;
	
	public JobTransactionSashForm(Composite parent, int style) {
		super(parent, style);
		
		this.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		this.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group jobsGroup = new Group(this, SWT.BORDER);
		jobsGroup.setLayout(new GridLayout(1, true));
		jobsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		jobsGroup.setText("Jobs");
		jobsTable = new JobTableWidgetPagination(jobsGroup, SWT.NONE, 20);
		jobsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group jobTransactionGroup = new Group(this, SWT.BORDER);
		jobTransactionGroup.setLayout(new GridLayout(1, true));
		jobTransactionGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		jobTransactionGroup.setText("Transactions of Job");
		transactionsTable = new CreditTransactionsByJobPagedTableWidget(jobTransactionGroup, SWT.NONE);
		transactionsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		addListeners();
		updateUI(true);
	}

	public void updateUI(boolean resetTablesToFirstPage) {
		jobsTable.refreshPage(resetTablesToFirstPage);
		transactionsTable.refreshPage(resetTablesToFirstPage);
	}
	
	protected void addListeners() {
		jobsTable.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				BusyIndicator.showWhile(JobTransactionSashForm.this.getDisplay(), new Runnable() {
					@Override
					public void run() {
						List<TrpJobStatus> jobs = jobsTable.getSelected();
						if(CollectionUtils.isEmpty(jobs)) {
							logger.debug("No job selected");
							return;
						}
						transactionsTable.setJobId(jobs.get(0).getJobIdAsInt());
					}
				});
			}
		});
	}
}
