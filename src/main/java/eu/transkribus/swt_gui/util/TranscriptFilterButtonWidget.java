package eu.transkribus.swt_gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.ToolBox;

public class TranscriptFilterButtonWidget extends Composite {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TranscriptFilterButtonWidget.class);
	
	List<Button> statusBtns = new ArrayList<>();
	Button statusFilterBtn, withoutTextBtn, withTextBtn, withoutLaBtn, withLaBtn;

	public TranscriptFilterButtonWidget(Composite parent, int style) {
		super(parent, style);
		this.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));

		statusFilterBtn = new Button(this, SWT.PUSH);
		statusFilterBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		statusFilterBtn.setText("Exclude GT,FINAL,DONE");
		statusFilterBtn.setForeground(Colors.createColor(new RGB(0, 100, 0)));
		statusFilterBtn.setSelection(false);
		
		Button configExclusionStatiBtn = new Button(this, SWT.PUSH);
		configExclusionStatiBtn.setText("...");
		ToolBox tb = new ToolBox(getShell(), true, "Filter transcripts by...");
		statusBtns = new ArrayList<>();
		for (EditStatus e : EditStatus.values()) {
			Button b = tb.addButton(e.toString(), null, SWT.CHECK);	
			b.setData(e);
			b.setSelection(e != EditStatus.GT && e != EditStatus.FINAL && e != EditStatus.DONE); // default selection
			statusBtns.add(b);
			SWTUtil.onSelectionEvent(b, se -> {
				updateUi();
			});
		}
		
		withoutLaBtn = tb.addButton("Without baselines", null, SWT.CHECK);
		SWTUtil.onSelectionEvent(withoutLaBtn, se -> {
			if (withoutLaBtn.getSelection()) {
				withLaBtn.setSelection(false);
			}				
			updateUi();
		});	
		withLaBtn = tb.addButton("With baselines", null, SWT.CHECK);
		SWTUtil.onSelectionEvent(withLaBtn, se -> {
			if (withLaBtn.getSelection()) {
				withoutLaBtn.setSelection(false);
			}				
			updateUi();
		});		
		
		withoutTextBtn = tb.addButton("Without text", null, SWT.CHECK);
		SWTUtil.onSelectionEvent(withoutTextBtn, se -> {
			updateUi();
			if (withoutTextBtn.getSelection()) {
				withTextBtn.setSelection(false);
			}			
		});	
		withTextBtn = tb.addButton("With text", null, SWT.CHECK);
		SWTUtil.onSelectionEvent(withTextBtn, se -> {
			if (withTextBtn.getSelection()) {
				withoutTextBtn.setSelection(false);
			}			
			updateUi();
		});	
		
		tb.addTriggerWidget(configExclusionStatiBtn);
		updateUi();
	}
	
	private List<EditStatus> getSelectedTranscriptStatus() {
		return statusBtns.stream().filter(b -> b.getSelection()).map(b -> ((EditStatus)b.getData())).collect(Collectors.toList());
	}
	
	private List<String> getSelectedTranscriptStatusAsString() {
		return getSelectedTranscriptStatus().stream().map(b -> b.toString()).collect(Collectors.toList());
	}	
	
	private void updateUi() {
		List<String> filterLabels = getSelectedTranscriptStatusAsString();
		if (this.withoutLaBtn.getSelection()) {
			filterLabels.add("w/o baselines");
		}
		if (this.withLaBtn.getSelection()) {
			filterLabels.add("with baselines");
		}
		if (this.withoutTextBtn.getSelection()) {
			filterLabels.add("w/o text");
		}
		if (this.withTextBtn.getSelection()) {
			filterLabels.add("with text");
		}
		
		statusFilterBtn.setText("Filter by: "+CoreUtils.join(filterLabels));
	}
	
	public void addSelectionListener(SelectionListener l) {
		statusFilterBtn.addSelectionListener(l);
	}
	
	public void addSelectionListener(Consumer<SelectionEvent> c) {
		statusFilterBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				c.accept(e);
			}
		});
	}
	
	public boolean isAccepted(TrpTranscriptMetadata tm) {
		if (tm==null) {
			return false;
		}
		
		boolean statusCheck = getSelectedTranscriptStatus().contains(tm.getStatus());

		boolean laCheck = true;
		if (withoutLaBtn.getSelection() || withLaBtn.getSelection()) {
			boolean isEmpty = tm.getNrOfLines() == null || tm.getNrOfLines() == 0;
			laCheck = withoutLaBtn.getSelection() ? isEmpty : !isEmpty;
		}
		
		boolean textCheck = true;
		if (withoutTextBtn.getSelection() || withTextBtn.getSelection()) {
			// FIXME? if tm.getNrOfTranscribedLines()==null -> expects text to be present (should only happen for older transcripts)
			boolean isEmpty = tm.getNrOfTranscribedLines() != null && tm.getNrOfTranscribedLines() == 0;
//			logger.debug(tm.getPageNr()+", tm.getNrOfTranscribedLines() = "+tm.getNrOfTranscribedLines()+" isEmpty = "+isEmpty);
			textCheck = withoutTextBtn.getSelection() ? isEmpty : !isEmpty;
		}
		
		return statusCheck && laCheck && textCheck;
	}
}
