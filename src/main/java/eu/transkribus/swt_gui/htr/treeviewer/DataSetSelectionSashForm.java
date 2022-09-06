package eu.transkribus.swt_gui.htr.treeviewer;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.DescriptorUtils.AGtDataSet;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.ImgLoader;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.TrpViewerFilterWidget;
import eu.transkribus.swt_gui.collection_treeviewer.CollectionContentProvider;
import eu.transkribus.swt_gui.collection_treeviewer.CollectionLabelProvider;
import eu.transkribus.swt_gui.htr.DataSetMetadata;
import eu.transkribus.swt_gui.htr.DataSetTableWidget;
import eu.transkribus.swt_gui.htr.treeviewer.DataSetSelectionController.DataSetSelection;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.AGtDataSetElement;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * Sashform UI element for selecting datasets from document and ground truth data.
 * 
 * @see DataSetSelectionController
 */
public class DataSetSelectionSashForm extends SashForm implements IStorageListener {
	private static final Logger logger = LoggerFactory.getLogger(DataSetSelectionSashForm.class);
	
//	private static final RGB BLUE_RGB = new RGB(0, 0, 140);
//	private static final RGB LIGHT_BLUE_RGB = new RGB(0, 140, 255);
//	private static final RGB GREEN_RGB = new RGB(0, 140, 0);
//	private static final RGB LIGHT_GREEN_RGB = new RGB(0, 255, 0);
//	private static final RGB CYAN_RGB = new RGB(85, 240, 240);
	
	private static final RGB BLUE_RGB = new RGB(0, 83, 138);
	private static final RGB LIGHT_BLUE_RGB = new RGB(115, 161, 191);
	private static final RGB GREEN_RGB = new RGB(0, 105, 66);
	private static final RGB LIGHT_GREEN_RGB = new RGB(69, 145, 117);
	private static final RGB CYAN_RGB = new RGB(0, 150, 240);
//	private static final RGB VERY_LIGHT_BLUE_RGB = new RGB(191, 212, 225);
//	private static final RGB VERY_LIGHT_GREEN_RGB = new RGB(153, 195, 179);
	
	static final Color BLUE = Colors.createColor(BLUE_RGB);
	static final Color LIGHT_BLUE = Colors.createColor(LIGHT_BLUE_RGB);
//	static final Color VERY_LIGHT_BLUE = Colors.createColor(VERY_LIGHT_BLUE_RGB);
	static final Color GREEN = Colors.createColor(GREEN_RGB);
	static final Color LIGHT_GREEN = Colors.createColor(LIGHT_GREEN_RGB);
//	static final Color VERY_LIGHT_GREEN = Colors.createColor(VERY_LIGHT_GREEN_RGB);
	static final Color CYAN = Colors.createColor(CYAN_RGB);
	static final Color WHITE = Colors.getSystemColor(SWT.COLOR_WHITE);
	static final Color BLACK = Colors.getSystemColor(SWT.COLOR_BLACK);
	
	//show pages with no transcribed lines in gray
	public static final Color GRAY = Colors.getSystemColor(SWT.COLOR_GRAY);
	
	Composite docTabComp, dataTabComp;
	TreeViewer docTv, groundTruthTv;
	TrpViewerFilterWidget docFilterWidget, gtFilterWidget;
	
	Combo versionCombo;
	Button addToTrainSetBtn, addToValSetBtn, removeFromTrainSetBtn, removeFromValSetBtn;
	Button automaticValSet_2Percent, automaticValSet_5Percent, automaticValSet_10Percent;

	Label infoLbl;
	DataSetTableWidget<IDataSelectionEntry<?, ?>> valSetOverviewTable, trainSetOverviewTable;
	CTabFolder dataTabFolder;
	CTabItem documentsTabItem;
	CTabItem gtTabItem;
	private Label previewLbl;
	private URL currentThumbUrl = null;
	
	private DataSetSelectionController dataSetSelectionController;

	//the input to select data from
	private List<TrpDocMetadata> docList;
	private GroundTruthPagedTreeWidget gtPagedTree;
	private final int colId;

	public DataSetSelectionSashForm(Composite parent, int style, final int colId, List<TrpDocMetadata> docList) {
		super(parent, style);
		this.docList = docList;
		this.colId = colId;
		dataSetSelectionController = new DataSetSelectionController(colId, this);
		
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.setLayout(new GridLayout(1, false));
		
		dataTabFolder = new CTabFolder(this, SWT.BORDER | SWT.FLAT);
		dataTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		GridLayout tabCompLayout = new GridLayout(1, true);
		//remove margins to make it consistents with documentsTab
		tabCompLayout.marginHeight = tabCompLayout.marginWidth = 0;
		GridData tabCompLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		
		docTabComp = new Composite(dataTabFolder, SWT.NONE);
		docTabComp.setLayout(tabCompLayout);
		docTabComp.setLayoutData(tabCompLayoutData);
		
		documentsTabItem = new CTabItem(dataTabFolder, SWT.NONE);
		documentsTabItem.setText("Documents");
		docTv = createDocumentTreeViewer(docTabComp);
		docFilterWidget = new TrpViewerFilterWidget(docTabComp, docTv, false, TrpDocMetadata.class, "docId", "title");
		documentsTabItem.setControl(docTabComp);

		dataTabComp = new Composite(dataTabFolder, SWT.NONE);
		dataTabComp.setLayout(tabCompLayout);
		dataTabComp.setLayoutData(tabCompLayoutData);
		
		gtPagedTree = createGroundTruthTreeViewer(dataTabComp);
		groundTruthTv = gtPagedTree.getTreeViewer();
		
		Composite buttonComp = new Composite(this, SWT.NONE);
		buttonComp.setLayout(new GridLayout(1, true));

		previewLbl = new Label(buttonComp, SWT.NONE);
		GridData previewLblGd = new GridData(SWT.CENTER, SWT.TOP, true, true);
		previewLblGd.heightHint = 120;
		previewLblGd.widthHint = 100;
		previewLbl.setLayoutData(previewLblGd);
		
		infoLbl = new Label(buttonComp, SWT.WRAP);
		GridData infoLblGd = new GridData(SWT.FILL, SWT.BOTTOM, true, true);
		infoLbl.setLayoutData(infoLblGd);
		
		addToTrainSetBtn = new Button(buttonComp, SWT.PUSH);
		addToTrainSetBtn.setImage(Images.ADD);
		addToTrainSetBtn.setText("Training");
		addToTrainSetBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		addToValSetBtn = new Button(buttonComp, SWT.PUSH);
		addToValSetBtn.setImage(Images.ADD);
		addToValSetBtn.setText("Validation");
		addToValSetBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		Label tmp = new Label(buttonComp, SWT.WRAP);
		GridData tmpLblGd = new GridData(SWT.FILL, SWT.TOP, true, false);
		tmp.setLayoutData(tmpLblGd);
		tmp.setText("automatic selection of validation set");
		
		automaticValSet_2Percent = new Button(buttonComp, SWT.CHECK);
		automaticValSet_2Percent.setText("2% from train");
		automaticValSet_2Percent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		automaticValSet_2Percent.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		    	if (automaticValSet_2Percent.getSelection()) {
		    		automaticValSet_5Percent.setSelection(false);
		    		automaticValSet_10Percent.setSelection(false);
		    	}
		    }
		  });
		
		automaticValSet_5Percent = new Button(buttonComp, SWT.CHECK);
		automaticValSet_5Percent.setText("5% from train");
		automaticValSet_5Percent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		automaticValSet_5Percent.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		    	if (automaticValSet_5Percent.getSelection()) {
		    		automaticValSet_2Percent.setSelection(false);
		    		automaticValSet_10Percent.setSelection(false);
		    	}
		    }
		  });
		
		automaticValSet_10Percent = new Button(buttonComp, SWT.CHECK);
		automaticValSet_10Percent.setText("10% from train");
		automaticValSet_10Percent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		automaticValSet_10Percent.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		    	if (automaticValSet_10Percent.getSelection()) {
		    		automaticValSet_2Percent.setSelection(false);
		    		automaticValSet_5Percent.setSelection(false);
		    	}
		    }
		  });

		Group trainOverviewCont = new Group(this, SWT.NONE);
		trainOverviewCont.setText("Overview");
//		Composite trainOverviewCont = new Composite(this, SWT.NONE);
		trainOverviewCont.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		trainOverviewCont.setLayout(new GridLayout(1, false));
		
		Composite versionSelectionComp = new Composite(trainOverviewCont, SWT.NONE);
		versionSelectionComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout versionSelectionCompLayout = new GridLayout(2, false);
		versionSelectionCompLayout.marginHeight = versionSelectionCompLayout.marginWidth = 0;
		versionSelectionComp.setLayout(versionSelectionCompLayout);
		
		Label versionComboLbl = new Label(versionSelectionComp, SWT.NONE);
		versionComboLbl.setText("Transcript version");
		versionCombo = new Combo(versionSelectionComp, SWT.READ_ONLY);
		
		for(VersionComboStatus s : VersionComboStatus.values()) {
			versionCombo.add(s.getLabel());
		}
		versionCombo.select(0);
		versionCombo.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));

		GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
		GridLayout tableGl = new GridLayout(1, true);
		
		SashForm vertSf = new SashForm(trainOverviewCont, SWT.VERTICAL);
		vertSf.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		vertSf.setLayoutData(new GridData(GridData.FILL_BOTH));

		Group trainSetGrp = new Group(vertSf, SWT.NONE);
		trainSetGrp.setText("Training Set");
		trainSetGrp.setLayoutData(tableGd);
		trainSetGrp.setLayout(tableGl);

		trainSetOverviewTable = new DataSetTableWidget<IDataSelectionEntry<?, ?>>(trainSetGrp, SWT.BORDER) {};
		trainSetOverviewTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridData buttonGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		removeFromTrainSetBtn = new Button(trainSetGrp, SWT.PUSH);
		removeFromTrainSetBtn.setLayoutData(buttonGd);
		removeFromTrainSetBtn.setImage(Images.CROSS);
		removeFromTrainSetBtn.setText("Remove selected entries from training set");

		Group valSetGrp = new Group(vertSf, SWT.NONE);
		valSetGrp.setText("Validation Set");
		valSetGrp.setLayoutData(tableGd);
		valSetGrp.setLayout(tableGl);

		valSetOverviewTable = new DataSetTableWidget<IDataSelectionEntry<?, ?>>(valSetGrp, SWT.BORDER) {};
		valSetOverviewTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		removeFromValSetBtn = new Button(valSetGrp, SWT.PUSH);
		removeFromValSetBtn.setLayoutData(buttonGd);
		removeFromValSetBtn.setImage(Images.CROSS);
		removeFromValSetBtn.setText("Remove selected entries from validation set");
		
		vertSf.setWeights(new int[] { 1, 1 });
		this.setWeights(new int[] { 45, 10, 45 });
		dataTabFolder.setSelection(documentsTabItem);
		
		docTv.getTree().pack();
		buttonComp.pack();
		trainOverviewCont.pack();
		trainSetGrp.pack();
		valSetGrp.pack();
		
		Storage.getInstance().addListener(this);
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				Storage.getInstance().removeListener(DataSetSelectionSashForm.this);
			}
		});
		new DataSetSelectionSashFormListener(this, dataSetSelectionController);
		
		SWTUtil.setTabFolderBoldOnItemSelection(this.dataTabFolder);
	}

	public void setGroundTruthSelectionEnabled(boolean enabled) {
		
		
		
		if(enabled) {
			if (gtTabItem == null || gtTabItem.isDisposed()) {
				gtTabItem = new CTabItem(dataTabFolder, SWT.NONE);
				gtTabItem.setText("Model Data");
				gtTabItem.setControl(dataTabComp);
				
			}
			logger.debug("load the first page of htr paged tree in training dialog");
			getHtrPagedTree().loadFirstPage();
			return;
			
		} else {
			if(gtTabItem != null) {
				gtTabItem.dispose();
				gtTabItem = null;
				dataSetSelectionController.removeAllGtFromSelection();
				return;
			}
		}

	}
	

	private TreeViewer createDocumentTreeViewer(Composite parent) {
		TreeViewer tv = new TreeViewer(parent, SWT.BORDER | SWT.MULTI);
		final CollectionContentProvider docContentProvider = new CollectionContentProvider(colId);
		final CollectionLabelProvider docLabelProvider = new CollectionDataSetLabelProvider(dataSetSelectionController);
		tv.setContentProvider(docContentProvider);
		tv.setLabelProvider(docLabelProvider);
		tv.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tv.setInput(this.docList);
		return tv;
	}
	
	private GroundTruthPagedTreeWidget createGroundTruthTreeViewer(Composite parent) {
		GroundTruthPagedTreeWidget widget = new GroundTruthPagedTreeWidget(dataTabComp, SWT.BORDER, null, null, 
				new ModelGroundTruthContentProvider(null), new ModelGroundTruthTableLabelAndFontProvider(getFont()));
		widget.setWithStartPylaiaInContextMenu(true);
		widget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		return widget;
	}

	/**
	 * Show dialog for resolving conflicts with overlapping images.
	 * <ul>
	 * <li>SWT.YES = replace data in selection with gtOverlapByImageId</li>
	 * <li>SWT.NO = discard gtOverlapByImageId and keep previous selection</li>
	 * <li>SWT.CANCEL = do nothing</li>
	 * </ul>
	 * @param docMd
	 * @param gtOverlapByImageId
	 * @return SWT.YES, SWT.NO, SWT.CANCEL
	 */
	int openConflictDialog(TrpDocMetadata docMd, List<TrpPage> gtOverlapByImageId) {
		String title = "Some of the data is already selected";
		String msg = "The images of the following pages are already included in the selection:\n\n";
		String pageStr = CoreUtils.getRangeListStrFromList(gtOverlapByImageId.stream().map(p -> p.getPageNr()-1).collect(Collectors.toList()));
		msg += "Document '" + docMd.getTitle() + "' pages " + pageStr;
		msg += "\n\nDo you want to replace the previous selection with those pages?";
		
		MessageBox messageBox = new MessageBox(this.getShell(), SWT.ICON_QUESTION
	            | SWT.YES | SWT.NO | SWT.CANCEL);
        messageBox.setMessage(msg);
        messageBox.setText(title);
        return messageBox.open();	
	}
	
	/**
	 * Show dialog for resolving conflicts with overlapping images.
	 * <ul>
	 * <li>SWT.YES = replace data in selection with gtOverlapByImageId</li>
	 * <li>SWT.NO = discard gtOverlapByImageId and keep previous selection</li>
	 * <li>SWT.CANCEL = do nothing</li>
	 * </ul>
	 * @param docMd
	 * @param gtOverlapByImageId
	 * @return SWT.YES, SWT.NO, SWT.CANCEL
	 */
	int openConflictDialog(AGtDataSet<?> gtSet, List<AGtDataSetElement<?>> gtOverlapByImageId) {
		String title = "Some of the image data is already included";
		String msg = "The images of the following model data are already included in the selection:\n\n";
		if(gtOverlapByImageId.size() == 1) {
			msg += "HTR " + gtSet.getDataSetType().getLabel() + " '" + gtSet.getName() 
					+ "' page " + gtOverlapByImageId.get(0).getGroundTruthPage().getPageNr();
		} else {
			List<Integer> pageIndices = gtOverlapByImageId.stream()
					.map(g -> (g.getGroundTruthPage().getPageNr() - 1))
					.collect(Collectors.toList());
			String pageStr = CoreUtils.getRangeListStrFromList(pageIndices);
			msg += "HTR " + gtSet.getDataSetType().getLabel() + " '" + gtSet.getName() + "' pages " + pageStr;
		}
		msg += "\n\nDo you want to replace the previous selection with those pages?";
		
		MessageBox messageBox = new MessageBox(this.getShell(), SWT.ICON_QUESTION
	            | SWT.YES | SWT.NO | SWT.CANCEL);
        messageBox.setMessage(msg);
        messageBox.setText(title);
        return messageBox.open();	
	}
	
	public void refreshTreeViewers() {
		groundTruthTv.refresh(true);
		docTv.refresh(true);
	}
	
	/**
	 * Update ground truth treeviewer row colors according to selected data set.
	 * 
	 * For now this will expect train/validation data to be selected to the respective sets!
	 */
	void updateGtTvColors() {
		groundTruthTv.refresh(true);
	}
	
	void updateDocTvColors() {
		docTv.refresh(true);
	}
	
	public void updateThumbnail(URL thumbUrl) {
		logger.debug("Update thumbnail: " + thumbUrl + " | current thumnail: " + currentThumbUrl);
		if(thumbUrl == null) {
			logger.debug("Remove image from view");
			updateThumbnail((Image)null);
			return;
		}
		if(!thumbUrl.equals(currentThumbUrl)) {
			//update thumbnail on URL change only
			Runnable r = new Runnable() {
				@Override
				public void run() {
					updateThumbnail(loadThumbnail(thumbUrl));
					currentThumbUrl = thumbUrl;
				}
			};
			getDisplay().asyncExec(r);
		} else {
			logger.debug("Keeping current thumb as URL has not changed");
		}
	}
	
	/**
	 * Update the thumbnail label with given image.
	 * 
	 * @param image the image or null to clear the label
	 */
	private void updateThumbnail(Image image) {
		if (previewLbl.getImage() != null) {
			previewLbl.getImage().dispose();
		}
		previewLbl.setImage(image);
	}

	private Image loadThumbnail(URL thumbUrl) {
		Image image;
		try {
			image = ImgLoader.load(thumbUrl);
		} catch (IOException e) {
			logger.error("Could not load image", e);
			image = null;
		}
		return image;
	}
	
	/**
	 * Set infoLabelText on info label.
	 * 
	 * @param infoLabelText the text or null to clear the label.
	 */
	void updateInfoLabel(String infoLabelText) {
		if(infoLabelText == null) {
			infoLabelText = "";
		}
		this.infoLbl.setText(infoLabelText);
		this.infoLbl.requestLayout();
	}
	
	public List<DataSetMetadata> getTrainSetMetadata() {
		return dataSetSelectionController.getTrainSetMetadata();
	}
	
	public List<DataSetMetadata> getValSetMetadata() {
		return dataSetSelectionController.getValSetMetadata();
	}
	
	public VersionComboStatus getVersionComboStatus() {
		VersionComboStatus status = VersionComboStatus.Latest;
		String text = versionCombo.getText();
		for(VersionComboStatus s : VersionComboStatus.values()) {
			if(s.getLabel().equals(text)) {
				status = s;
				break;
			}
		}
		logger.debug("Selected VersionComboStatus = {}", status);
		return status;
	}
	
	public DataSetSelectionController getController() {
		return dataSetSelectionController;
	}

	public DataSetSelection getSelection() {
		return dataSetSelectionController.getSelection();
	}
	
	boolean isGtTabActive() {
		return this.gtTabItem.equals(this.dataTabFolder.getSelection());
	}
	boolean isDocumentsTabActive() {
		return this.documentsTabItem.equals(this.dataTabFolder.getSelection());
	}

	public void enableDebugDialog(boolean b) {
		getController().SHOW_DEBUG_DIALOG = b;
	}
	
	public boolean isAutomaticValSetChoiceBtnSet() {
		return automaticValSet_2Percent.getSelection() || automaticValSet_5Percent.getSelection() || automaticValSet_10Percent.getSelection();
	}
	
	public boolean isValSet2Percent() {
		return automaticValSet_2Percent.getSelection();
	}
	
	public boolean isValSet5Percent() {
		return automaticValSet_5Percent.getSelection();
	}
	
	public boolean isValSet10Percent() {
		return automaticValSet_10Percent.getSelection();
	}
	

//	@Override
//	public void handleHtrListLoadEvent(HtrListLoadEvent e) {
//		if(e.collId != this.colId) {
//			logger.debug("Ignoring update of htrList for foreign collection ID = " + e.collId);
//			return;
//		}
//		this.htrList = e.htrs.getList();
//		groundTruthTv.setInput(this.htrList);
//	}
	
	public GroundTruthPagedTreeWidget getHtrPagedTree() {
		return gtPagedTree;
	}

	public static enum VersionComboStatus {
		Latest("Latest transcript", null),
		GT("Ground truth only", EditStatus.GT),
		Initial("Initial transcript", EditStatus.NEW);
		
		private final String label;
		private final EditStatus status;
		private VersionComboStatus(String label, EditStatus status) {
			this.label = label;
			this.status = status;
		}
		public String getLabel() {
			return label;
		}
		public EditStatus getStatus() {
			return status;
		}
	}
}
