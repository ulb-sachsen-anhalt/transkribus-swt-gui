package org.eclipse.swt.widgets;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.Images;

/**
 * Widget with a read-only text field to show a formatted date string, a clear button that allows to unset the date (null) and a calendar button that opens a date picker.
 * Use {@link #addModifyListener(ModifyListener)} to implement react on changes of the selected date.
 */
public class DatePickerComposite extends Composite {
	Logger logger = LoggerFactory.getLogger(DatePickerComposite.class);
	DateFormat format;
	Date date;
	Text dateTxt;
	Button datePickerBtn, clearBtn;
	
	ModifyListener modifyListener;
	
	public DatePickerComposite(Composite parent, DateFormat format, int style) {
		super(parent, style);
		if(format == null) {
			format = CoreUtils.newDateFormatUserFriendly();
		}
		this.format = format;
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).margins(0, 0).applyTo(this);
		dateTxt = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		dateTxt.addMouseListener(new MouseListener() {
			@Override
			public void mouseDown(MouseEvent arg0) {
				logger.trace("Mouse down");
				openDatePickerDialog();				
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				logger.trace("Mouse double click");
			}
			@Override
			public void mouseUp(MouseEvent arg0) {
				logger.trace("Mouse up");
			}
		});
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dateTxt);
		clearBtn = new Button(this, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(clearBtn);
		clearBtn.setImage(Images.CROSS);
		clearBtn.setToolTipText("Clear the selection.");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDate(null);
				sendModifyEvent();
			}
		});
		
		datePickerBtn = new Button(this, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(datePickerBtn);
		datePickerBtn.setImage(Images.CALENDAR);
		datePickerBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openDatePickerDialog();
			}
		});
	}
	
	private void openDatePickerDialog() {
		Calendar cal = null;
		if(date != null) {
			cal = Calendar.getInstance();
			cal.setTime(date);
		}
		DatePickerDialog d = new DatePickerDialog(getShell(), cal);
		if(IDialogConstants.OK_ID == d.open()) {
			setDate(d.getCalendar().getTime());
			sendModifyEvent();
		}
	}
	
	protected void sendModifyEvent() {
		//or override notifyListeners()?
		if(modifyListener == null) {
			//do nothing
			return;
		}
		Event event = new Event();
		event.widget = this;
		event.data = date;
		modifyListener.modifyText(new ModifyEvent(event));
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
		dateTxt.setText(date == null ? "None" : format.format(date));
	}
	
	public void addModifyListener(ModifyListener listener) {
		this.modifyListener = listener;
	}
	
	/**
	 * Dialog for selecting a date and time using SWT's {@link DateTime} widget.
	 * In contrast to {@link eu.transkribus.swt_gui.dialogs.DatePickerDialog} this is based on JFace's Dialog 
	 * and returns the selection as a calendar instead of a formatted string.
	 */
	public static class DatePickerDialog extends Dialog {
		Calendar cal;
		DateTime datePicker;
		DateTime timePicker;
		protected DatePickerDialog(Shell parentShell) {
			super(parentShell);
		}
		protected DatePickerDialog(Shell parentShell, Calendar selection) {
			super(parentShell);
			if(selection == null) {
				selection = Calendar.getInstance();
			}
			this.cal = selection;
		}
		@Override
		protected void setShellStyle(int newShellStyle) {
			super.setShellStyle(SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL);
		}
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Choose a Date");
		}
		@Override
		protected Composite createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(area);
			datePicker = new DateTime(area, SWT.CALENDAR);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(datePicker);
			//Couldn't make this work in 24 hour format instead of AM/PM. Might be a Linux issue though: https://bugs.eclipse.org/bugs/show_bug.cgi?id=337468
			timePicker = new DateTime(area, SWT.TIME);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(timePicker);
			setSelection(cal);			
			return area;
		}
		
		private void setSelection(Calendar cal) {
			if(cal != null) {
				datePicker.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				timePicker.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
			}
		}
		@Override
		protected void okPressed() {
			if(cal == null) {
				cal = Calendar.getInstance();
			}
			cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDay(), 
					timePicker.getHours(), timePicker.getMinutes(), timePicker.getSeconds());
			super.okPressed();
		}
		
		public Calendar getCalendar() {
			return cal;
		}
	}
}