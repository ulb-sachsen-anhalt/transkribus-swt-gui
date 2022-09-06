package eu.transkribus.swt_gui.dialogs;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;

public class ExtendBaselinesDialog extends Dialog {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExtendBaselinesDialog.class);
	
	LabeledText right, left;
	Button extendSelected, extendAll;
	
	public ExtendBaselinesDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, "Close", false);
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
		newShell.setText("Extend baselines");
	}
	
	@Override
	protected boolean isResizable() {
	    return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		cont.setLayout(new GridLayout(2, false));
	
		left = new LabeledText(cont, "Left extension: ", true);
		left.setToolTipText("Nr of pixels that the baselines is extended to the left");
		left.setText("50");
		
		right = new LabeledText(cont, "Right extension: ", true);
		right.setToolTipText("Nr of pixels that the baselines is extended to the right");
		right.setText("50");
		
		extendSelected = new Button(cont, SWT.PUSH);
		extendSelected.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		extendSelected.setText("Extend selected");
		extendSelected.setToolTipText("Extends baselines of selected shapes only");
				
		extendAll = new Button(cont, SWT.PUSH);
		extendAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		extendAll.setText("Extend all");
		extendSelected.setToolTipText("Extends all baselines of the current page");
		
		SWTUtil.onSelectionEvent(extendSelected, e -> {
			Pair<Integer, Integer> v = parseVals(true); 
			if (v==null) {
				return;
			}
			
			logger.debug("extending selected: "+v);
			TrpMainWidget.i().getShapeEditController().extendBaselines(v.getLeft(), v.getRight(), true);
		});
		
		SWTUtil.onSelectionEvent(extendAll, e -> {
			Pair<Integer, Integer> v = parseVals(true); 
			if (v==null) {
				return;
			}
			
			logger.debug("extending all: "+v);
			TrpMainWidget.i().getShapeEditController().extendBaselines(v.getLeft(), v.getRight(), false);
		});
		
		return cont;
	}
	
	private Pair<Integer, Integer> parseVals(boolean showError) {
		try {
			int l = Integer.parseInt(left.getText());
			int r = Integer.parseInt(right.getText());
			
			if (l<0 || r <0) {
				throw new IllegalArgumentException("Negative values are not allowed");
			}
			return Pair.of(l, r);
		} catch (Exception e) {
			if (showError) {
				DialogUtil.showErrorMessageBox(getShell(), "Could not parse values", "Invalid left or right extension values!");
			}
			return null;
		}
	}
	
	@Override
	protected void okPressed() {
	
		super.okPressed();
	}

}
