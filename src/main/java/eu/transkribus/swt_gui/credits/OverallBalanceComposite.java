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

public class OverallBalanceComposite extends ABalanceComposite {
	final Label overallBalanceLbl;
	final Text overallBalanceValueTxt;
	final Button showDetailsBtn;
	Double balance; 
	
	public OverallBalanceComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new GridLayout(3, false));
		
		overallBalanceLbl = new Label(this, SWT.NONE);
		overallBalanceLbl.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, true));
		overallBalanceLbl.setText("Overall Credits:");
		Fonts.setBoldFont(overallBalanceLbl);
		
		overallBalanceValueTxt = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		overallBalanceValueTxt.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		showDetailsBtn = new Button(this, SWT.PUSH);
		showDetailsBtn.setImage(Images.getOrLoad("/icons/calculator.png"));
		showDetailsBtn.setLayoutData(new GridData(GridData.END, SWT.CENTER, false, false));
		
		update(null);
	}
	
	public void updateBalanceValue(Double balance) {
		this.balance = balance;
		String txt = "N/A";
		if(balance != null) {
			txt = "" + balance;
		}
		overallBalanceValueTxt.setText(txt);
	}
	
	public void update(TrpCreditHistoryList currentData) {
		Double balance = null;
		if(currentData != null && currentData.getOverallBalance() != null) {
			balance = currentData.getOverallBalance();
		}
		updateBalanceValue(balance);
	}

	public Double getBalance() {
		return balance;
	}

	public Button getShowDetailsBtn() {
		return showDetailsBtn;
	}
}