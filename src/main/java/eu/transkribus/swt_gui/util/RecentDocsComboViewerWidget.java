package eu.transkribus.swt_gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.util.RecentDocsPreferences;

public class RecentDocsComboViewerWidget extends Composite implements Observer {
	private final static Logger logger = LoggerFactory.getLogger(RecentDocsComboViewerWidget.class);
	
	public ComboViewer lastDocsComboViewer;
	public Combo lastDocsCombo;
	
//	Storage storage = Storage.getInstance();
	
	public final String label = "Recent Documents...";
	
	boolean isBeingSelected=false;
		
	public RecentDocsComboViewerWidget(Composite parent, int style) {
		super(parent, style);
				
//		Composite collsContainer = new Composite(this, 0);
//		collsContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		this.setLayout(new GridLayout(2, false));
		this.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		
//		lastDocsCombo = new Combo(this, /*SWT.READ_ONLY |*/ SWT.DROP_DOWN);
		lastDocsCombo = new Combo(this, SWT.READ_ONLY | SWT.DROP_DOWN);
		
//		lastDocsCombo.setBackground(Colors.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		
		lastDocsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		lastDocsCombo.addKeyListener(new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				e.doit = false;
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				e.doit = false;		
			}
		});
		
		// draw label "artificially"
		lastDocsCombo.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {	
				if (isBeingSelected)
					return;
				
				Point txtExtent = e.gc.textExtent(label);
				Rectangle r = lastDocsCombo.getBounds();
				e.gc.setBackground(lastDocsCombo.getBackground());
				e.gc.drawString(label, r.x+10, r.y+(r.height-txtExtent.y)/2, true);
			}
		});
		
		lastDocsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isBeingSelected = true;
			}
		});

		lastDocsComboViewer = new ComboViewer(lastDocsCombo);
				
		lastDocsComboViewer.setLabelProvider(new LabelProvider() {
			@Override public String getText(Object element) {
				if (element instanceof String) {
					return ((String) element);
				}
				else return "i am error";
			}
		});
		
		lastDocsComboViewer.setContentProvider(new ArrayContentProvider());

		setRecentDocs(true);
	}
	
	public String getSelectedDoc() {		
		IStructuredSelection sel = (IStructuredSelection) lastDocsComboViewer.getSelection();
		if (!sel.isEmpty())
			return (String) sel.getFirstElement();
		
		return null;
	}

	public void setRecentDocs(boolean sendSelectionEvent) {
		lastDocsComboViewer.refresh();
		
		List<String> items = new ArrayList<>();
		items.addAll(RecentDocsPreferences.getItems());
		
		lastDocsComboViewer.setInput(items);
		lastDocsComboViewer.refresh(false);
		//lastDocsComboViewer.getCombo().setText(label);
				
		if (sendSelectionEvent)
			sendComboSelectionEvent();
		
		isBeingSelected = false;
	}	
	
	void sendComboSelectionEvent() {
		Event event = new Event(); 
		event.type = SWT.Selection;
		event.widget = lastDocsCombo;
//		event.widget = this;
		lastDocsCombo.notifyListeners(SWT.Selection, event);
	}

	@Override public void update(Observable o, Object arg) {
	}
	
	

}
