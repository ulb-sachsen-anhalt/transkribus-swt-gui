package eu.transkribus.swt_gui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.transkribus.swt.util.SWTUtil;

public class DatePickerDialog extends Dialog {
	
	private Shell shell; 
	private String date;

	public DatePickerDialog(Shell parent){
		super(parent);
	    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.CLOSE); 
	    shell.setText("Choose Due Date..."); 
	    shell.setLayout(new GridLayout()); 
	    final DateTime dateTime = new DateTime(shell, SWT.CALENDAR | SWT.BORDER); 
	  
	    shell.addDisposeListener(new DisposeListener() { 
			@Override
			public void widgetDisposed(DisposeEvent e) {
				int month = dateTime.getMonth()+1;
				String monthStr = Integer.toString(month);  
				if (month < 10){
					monthStr = "0"+month;
				}
				int day = dateTime.getDay();
				String dayStr = Integer.toString(day);
				if (day < 10){
					dayStr = "0"+day;
				}
				date = dateTime.getYear() + "-" + monthStr + "-" + dayStr;
				//date = dateTime.toString();
			} 
	    }); 
	}
	
	 public void open() { 
		 final Display display = shell.getDisplay(); 
		 shell.pack(); 
		 shell.open(); 
		 SWTUtil.centerShell(shell);
		 while (!shell.isDisposed()) { 
			 if (!display.readAndDispatch()) { 
				 display.sleep(); 
			 } 
		 } 
	 } 
		 
	 public String getDate() { 
		 return date; 
	 } 
}
