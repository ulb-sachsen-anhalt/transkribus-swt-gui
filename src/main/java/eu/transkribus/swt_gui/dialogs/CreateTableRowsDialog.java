package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.util.CurrentTranscriptOrCurrentDocPagesSelector;

public class CreateTableRowsDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(CreateTableRowsDialog.class);
	
	CurrentTranscriptOrCurrentDocPagesSelector pagesSelector;
	private Text subjectText;
	private Text lineText;
	
	Button dryRunBtn;
	Button forSelectedBtn;
	
	Button useSeparators;
	
	Integer columnNr;
	double lineWidth;
	
	Set<Integer> pageIndices=null;
	boolean doCurrentPage=true;
	boolean useHorizontalSeps=false;

	public CreateTableRowsDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(400, getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
	}
	
	@Override
	protected void setShellStyle(int newShellStyle) {           
	    super.setShellStyle(SWT.CLOSE | SWT.MODELESS| SWT.BORDER | SWT.TITLE);
	    setBlockOnOpen(false);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create table rows");
	}
	
	@Override
	protected boolean isResizable() {
	    return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(1, false));
		
		pagesSelector = new CurrentTranscriptOrCurrentDocPagesSelector(cont, SWT.NONE, true,true);
		pagesSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite comp = new Composite(cont, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		
		useSeparators = new Button(comp, SWT.CHECK);
		useSeparators.setText("Use horizontal separators");
		useSeparators.setSelection(false);
		GridData gd_sep = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
		useSeparators.setLayoutData(gd_sep);
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText("Column number for split:");
		GridData gd_lbl = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		lbl.setLayoutData(gd_lbl);
		
		subjectText = new Text(comp, SWT.BORDER);
		GridData gd_subjectText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_subjectText.widthHint = 200;
		subjectText.setText("1");
		subjectText.setToolTipText("Enter the column number that is decisive for dividing the table into rows: 1 or 2 or 3 ...");
		subjectText.setLayoutData(gd_subjectText);
		
		Label lbl2 = new Label(comp, SWT.NONE);
		lbl2.setText("Line width considered:");
		GridData gd_lbl2 = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		lbl.setLayoutData(gd_lbl2);
		
		lineText = new Text(comp, SWT.BORDER);
		GridData gd_lineText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_lineText.widthHint = 200;
		lineText.setText("0");
		lineText.setToolTipText("Enter the line width (in percent of column width) to say which lines should be considered: e.g. 0.25, 0.5, 0.75");
		lineText.setLayoutData(gd_lineText);
		
		return cont;
	}
	
	@Override
	protected void okPressed() {
		
		setUseHorizontalSeps(useSeparators.getSelection());
		
		try {
			pageIndices = pagesSelector.getSelectedPageIndices();
			setDoCurrentPage(pagesSelector.isCurrentTranscript());
		} catch (IOException e) {
			pageIndices = null;
			DialogUtil.showErrorMessageBox(getShell(), "Invalid value", "Could not parse selected pages");
			logger.error("Could not parse page indices: "+e.getMessage(), e);
			return;
		}
		
		try {
			columnNr = Integer.valueOf(subjectText.getText());
		} catch (Exception e) {
			DialogUtil.showErrorMessageBox(getShell(), "Invalid value", "Could not parse column number (must be a valid Integer number: 1,2,3,....)");
			logger.error(e.getMessage());
			return;
		}
		
		try {
			lineWidth = Double.valueOf(lineText.getText());
			if (lineWidth <= 0 && lineWidth > 1) {
				DialogUtil.showErrorMessageBox(getShell(), "Invalid value", "Could not parse line width in percent (must be a value between 0 and 1, e.g. 0.7)");
				return;
			}
		} catch (Exception e) {
			DialogUtil.showErrorMessageBox(getShell(), "Invalid value", "Could not parse line width in percent (must be a value between 0 and 1, e.g. 0.7)");
			logger.error(e.getMessage());
			return;
		}
		
		logger.debug("pageIndices = "+pageIndices);
		
		super.okPressed();
	}

	public boolean isDoCurrentPage() {
		return doCurrentPage;
	}

	public boolean isUseHorizontalSeps() {
		return useHorizontalSeps;
	}

	public void setUseHorizontalSeps(boolean useSeps) {
		this.useHorizontalSeps = useSeps;
	}

	public void setDoCurrentPage(boolean doCurrentPage) {
		this.doCurrentPage = doCurrentPage;
	}

	public Set<Integer> getPageIndices() {
		return pageIndices;
	}

	public Integer getColumnNr() {
		return columnNr;
	}

	public void setColumnNr(Integer columnNr) {
		this.columnNr = columnNr;
	}

	public double getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(double lineWidth) {
		this.lineWidth = lineWidth;
	}

}
