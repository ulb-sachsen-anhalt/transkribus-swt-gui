package eu.transkribus.swt_gui.credits;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.pagination_tables.CreditPackagesCollectionPagedTableWidget;
import eu.transkribus.swt_gui.pagination_tables.CreditPackagesUserPagedTableWidget;

public class CreditPackageWidget extends SashForm {
	protected CreditPackagesUserPagedTableWidget userCreditsTable;
	protected Group collectionCreditGroup;
	protected CreditPackagesCollectionPagedTableWidget collectionCreditsTable;
	
	protected MenuItem splitUserPackageItem, showUserPackageDetailsItem;
	
	protected Button addToCollectionBtn, removeFromCollectionBtn;
	
	public CreditPackageWidget(Composite parent, int style) {
		super(parent, SWT.HORIZONTAL | style);
		this.setLayout(SWTUtil.createGridLayout(3, false, 0, 0));
//		sf.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group userCreditGroup = new Group(this, SWT.BORDER);
		userCreditGroup.setLayout(new GridLayout(1, true));
		userCreditGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		userCreditGroup.setText("My Credit Packages");
		userCreditsTable = new CreditPackagesUserPagedTableWidget(userCreditGroup, SWT.NONE);
		userCreditsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite buttonComp = new Composite(this, SWT.NONE);
		buttonComp.setLayout(new GridLayout(1, true));
		buttonComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label space = new Label(buttonComp, SWT.NONE);
		space.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		
		addToCollectionBtn = new Button(buttonComp, SWT.PUSH);
		addToCollectionBtn.setImage(Images.ARROW_RIGHT);
//		addToCollectionBtn.setText("Assign");
		addToCollectionBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		removeFromCollectionBtn = new Button(buttonComp, SWT.PUSH);
		removeFromCollectionBtn.setImage(Images.ARROW_LEFT);
//		removeFromCollectionBtn.setText("Remove");
		removeFromCollectionBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label space2 = new Label(buttonComp, SWT.NONE);
		space2.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
		
		collectionCreditGroup = new Group(this, SWT.BORDER);
		collectionCreditGroup.setLayout(new GridLayout(1, true));
		collectionCreditGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		//group's title text is updated when data is loaded
		collectionCreditsTable = new CreditPackagesCollectionPagedTableWidget(collectionCreditGroup, SWT.NONE);
		collectionCreditsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		//TODO add menu to collectionCreditsTable too with same listeners
		Menu menu = new Menu(userCreditsTable.getTableViewer().getTable());
		userCreditsTable.getTableViewer().getTable().setMenu(menu);

		showUserPackageDetailsItem = new MenuItem(menu, SWT.NONE);
		showUserPackageDetailsItem.setText("Show details...");
		splitUserPackageItem = new MenuItem(menu, SWT.NONE);
		splitUserPackageItem.setText("Split package...");
		
		final int buttonWeight = 6;
		this.setWeights(new int[] { 47, buttonWeight, 47 });
	}
	
	public void updateCollectionCreditGroupText(TrpCollection collection) {
		String text = "Credit Packages in Collection";
		if(collection != null) {
			text += " '" + collection.getColName() + "'";
		}
		collectionCreditGroup.setText(text);
	}

	public void setCollection(TrpCollection collection, boolean resetTablesToFirstPage) {
		updateCollectionCreditGroupText(collection);
		collectionCreditsTable.setCollection(collection);
		userCreditsTable.refreshPage(resetTablesToFirstPage);
		collectionCreditsTable.refreshPage(resetTablesToFirstPage);
	}
}
