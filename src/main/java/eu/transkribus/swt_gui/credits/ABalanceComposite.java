package eu.transkribus.swt_gui.credits;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.model.beans.rest.TrpCreditHistoryList;

public abstract class ABalanceComposite extends Composite {
	public ABalanceComposite(Composite parent, int style) {
		super(parent, style);
	}
	public abstract void update(TrpCreditHistoryList currentData);
	public abstract Button getShowDetailsBtn();
	protected void updateBalanceValue(Text txt, Double balance) {
		String str = "N/A";
		if(balance != null) {
			str = "" + balance;
		}
		txt.setText(str);
	}
}
