package eu.transkribus.swt_gui.util;


import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;


public class TagsSelector extends Composite {
	private final static Logger logger = LoggerFactory.getLogger(TagsSelector.class);
	
	Button exportTagsBtn;
	Button selectTagsBtn;
	Set<String> tagnames;
	Set<String> checkedTagnames;
	String additionalTags = "";
	boolean tagExport=false;


	public TagsSelector(Composite parent, int style, final Set<String> tagnames) {

		super(parent, style);
		this.tagnames = tagnames;
		this.setLayout(new GridLayout(2, false));
						
		selectTagsBtn = new Button(this, SWT.PUSH);
		selectTagsBtn.setText("Select Tags");
		selectTagsBtn.setToolTipText("Select tags you wish to export");
		selectTagsBtn.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, false, false));
		selectTagsBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				final TagsViewer dpv = new TagsViewer(SWTUtil.dummyShell, 0, false, true, false);
				dpv.setTags(tagnames, getSelectedTagnames());
				dpv.setAdditionalTags(additionalTags);

				final MessageDialog d = DialogUtil.createCustomMessageDialog(getShell(), "Select tags", "", null, 0, new String[]{"OK",  "Cancel"}, 0, dpv);
				// gets called when dialog is closed:
				dpv.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent e) {
//						logger.info("return code: "+d.getReturnCode());
//						logger.info("checked list: "+dpv.getCheckedList());
						if (d.getReturnCode() == 0) {
							//logger.info("rs = "+ dpv.getCheckedList());
							setSelectedTagnames(dpv.getSelectedList());
							additionalTags = dpv.getAdditionalTags();
							
						}
					}
				});
				d.open();
			}
		});
	}
	


	public Set<String> getSelectedTagnames() {
		return checkedTagnames;
	}

	public void setSelectedTagnames(Set<String> list) {
		this.checkedTagnames = list;
	}


}
