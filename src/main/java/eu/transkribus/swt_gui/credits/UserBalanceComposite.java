package eu.transkribus.swt_gui.credits;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.model.beans.rest.TrpCreditHistoryList;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;

public class UserBalanceComposite extends ABalanceComposite {
	final static String PERSONAL_TOOLTIP = "Your personal, non-shareable credits.";
	final static String SHAREABLE_TOOLTIP = "Your credits that can be assigned to a collection or transferred to other Transkribus users.";
	
	final Label overallBalanceLbl, personalBalanceLbl, shareableBalanceLbl;
	final Text overallBalanceValueTxt, personalBalanceValueTxt, shareableBalanceValueTxt;
	final Button showDetailsBtn;
	
	public UserBalanceComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new GridLayout(7, false));
		
		personalBalanceLbl = new Label(this, SWT.NONE);
		personalBalanceLbl.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, true));
		personalBalanceLbl.setText("Personal:");
		personalBalanceLbl.setToolTipText(PERSONAL_TOOLTIP);
		
		personalBalanceValueTxt = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		personalBalanceValueTxt.setLayoutData(new GridData(GridData.FILL_BOTH));
		personalBalanceValueTxt.setToolTipText(PERSONAL_TOOLTIP);
		
		shareableBalanceLbl = new Label(this, SWT.NONE);
		shareableBalanceLbl.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, true));
		shareableBalanceLbl.setText("Shareable:");
		shareableBalanceLbl.setToolTipText(SHAREABLE_TOOLTIP);
		
		shareableBalanceValueTxt = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		shareableBalanceValueTxt.setLayoutData(new GridData(GridData.FILL_BOTH));
		shareableBalanceValueTxt.setToolTipText(SHAREABLE_TOOLTIP);
		
		overallBalanceLbl = new Label(this, SWT.NONE);
		overallBalanceLbl.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, true));
		overallBalanceLbl.setText("Overall:");
		Fonts.setBoldFont(overallBalanceLbl);
		
		overallBalanceValueTxt = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		overallBalanceValueTxt.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		showDetailsBtn = new Button(this, SWT.PUSH);
		showDetailsBtn.setImage(Images.getOrLoad("/icons/calculator.png"));
		showDetailsBtn.setLayoutData(new GridData(GridData.END, SWT.CENTER, false, false));
		
		update(null);
	}
	
	public void update(TrpCreditHistoryList currentData) {
		if(currentData == null) {
			updateBalanceValue(overallBalanceValueTxt, null);
			updateBalanceValue(personalBalanceValueTxt, null);
			updateBalanceValue(shareableBalanceValueTxt, null);
		} else {
			updateBalanceValue(overallBalanceValueTxt, currentData.getOverallBalance());
			updateBalanceValue(personalBalanceValueTxt, currentData.getPersonalBalance());
			updateBalanceValue(shareableBalanceValueTxt, currentData.getShareableBalance());
		}
	}

	public Button getShowDetailsBtn() {
		return showDetailsBtn;
	}
}
