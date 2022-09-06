package eu.transkribus.swt_gui.htr.treeviewer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.core.io.RemoteDocConst;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpGroundTruthPage;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.enums.DataSetType;
import eu.transkribus.core.util.DescriptorUtils.AGtDataSet;
import eu.transkribus.swt.util.ACollectionBoundStructuredContentProvider;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.AGtDataSetElement;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;

@Deprecated
public class HtrGroundTruthContentProvider extends ACollectionBoundStructuredContentProvider implements ITreeContentProvider, IStorageListener {
	private static final Logger logger = LoggerFactory.getLogger(HtrGroundTruthContentProvider.class);
	List<TrpHtr> htrs;
	
	/**
	 * Omit constant API queries for data sets as those are static anyway.
	 */
	private final static Map<HtrGtDataSet, List<HtrGtDataSetElement>> DATA_SET_CACHE = Collections.synchronizedMap(new HashMap<>());
	
	public HtrGroundTruthContentProvider(final Integer colId) {
		super(colId);
		store.addListener(this);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {		
		if (newInput instanceof List<?>) {
			this.htrs = ((List<TrpHtr>) newInput);
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {

		if (inputElement instanceof List<?>) {
			return ((List<TrpHtr>) inputElement).toArray();			
		} else if (inputElement instanceof TrpHtr) {
			return getChildren((TrpHtr) inputElement);
		} else if (inputElement instanceof HtrGtDataSet) {
			return getChildren((HtrGtDataSet) inputElement);
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TrpHtr) {
			return getChildren((TrpHtr) parentElement);
		} else if (parentElement instanceof HtrGtDataSet) {
			return getChildren((HtrGtDataSet) parentElement);
		}
		return null;
	}
	
	HtrGtDataSet[] getChildren(TrpHtr htr) {
		HtrGtDataSet trainSet = null;
		HtrGtDataSet valSet = null;
		if(htr.hasTrainGt()) {
			trainSet = new HtrGtDataSet(htr, DataSetType.TRAIN);
		}
		if(htr.hasValidationGt()) {
			valSet = new HtrGtDataSet(htr, DataSetType.VALIDATION);
		}
		if(trainSet != null && valSet != null) {
			return new HtrGtDataSet[] { trainSet, valSet };
		} else if (trainSet != null) {
			return new HtrGtDataSet[] { trainSet };
		}
		return null;
	}
	
	HtrGtDataSetElement[] getChildren(HtrGtDataSet gt) {
		List<TrpGroundTruthPage> gtList = null;
		
		if(!store.isUserAllowedToViewDataSets(this.getCollId(), gt.getModel())) {
			return null;
		}
		
		if(DATA_SET_CACHE.containsKey(gt)) {
			logger.debug("Returning GT data set cache entry");
			List<HtrGtDataSetElement> elements =  DATA_SET_CACHE.get(gt);
			return elements.toArray(new HtrGtDataSetElement[elements.size()]);
		}
		
		switch(gt.getDataSetType()) {
		case TRAIN:
			try {
				gtList = store.getConnection().getHtrTrainData(super.getCollId(), gt.getId());
			} catch (TrpClientErrorException e) {
				logger.warn("Could not retrieve GT: {}", e.getMessageToUser());
			} catch (SessionExpiredException | IllegalArgumentException e) {
				logger.error("Could not retrieve HTR train data set for HTR = " + gt.getId(), e);
			}
			break;
		case VALIDATION:
			try {
				gtList = store.getConnection().getHtrValidationData(super.getCollId(), gt.getId());
			} catch (TrpClientErrorException e) {
				logger.warn("Could not retrieve GT: {}", e.getMessageToUser());
			} catch (SessionExpiredException | IllegalArgumentException e) {
				logger.error("Could not retrieve HTR validation data set for HTR = " + gt.getId(), e);
			}
			break;
		}
		if(gtList == null) {
			return null;
		} else {
			List<HtrGtDataSetElement> children = gtList.stream()
					.map(g -> new HtrGtDataSetElement(gt, g))
					.collect(Collectors.toList());
			synchronized(DATA_SET_CACHE) {
				DATA_SET_CACHE.put(gt, children);
			}
			return children.toArray(new HtrGtDataSetElement[children.size()]);
		}
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof List<?>) {
			return null;
		} else if (element instanceof TrpHtr) {
			return htrs;
		} else if (element instanceof HtrGtDataSet) {
			return((HtrGtDataSet) element).getModel();
		} else if (element instanceof HtrGtDataSetElement) {
			return ((HtrGtDataSetElement) element).getParentGtDataSet();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof List<?> && ((List<?>) element).size() > 0) {
			return true;
		} else if (element instanceof TrpHtr) {
			return ((TrpHtr) element).hasTrainGt();
		} else if (element instanceof HtrGtDataSet) {
			final HtrGtDataSet s = ((HtrGtDataSet) element);
			TrpHtr h = ((HtrGtDataSet) element).getModel();
			if(!store.isUserAllowedToViewDataSets(this.getCollId(), h)) {
				return false;
			}
			
			switch (s.getDataSetType()) {
			case TRAIN:
				return h.hasTrainGt();
			case VALIDATION:
				return h.hasValidationGt();
			}
		}
		return false;
	}

	@Override
	public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
		synchronized(DATA_SET_CACHE) {
			logger.debug("Clearing HtrGroundTruth data set cache.");
			DATA_SET_CACHE.clear();
		}
	}
	
	/**
	 * An instance of this type represents an HTR GroundTruth data set (e.g. train or validation set) and is used for 
	 * displaying the ground truth set level in a HTR treeviewer.
	 */
	public static class HtrGtDataSet extends AGtDataSet<TrpHtr> {
		public HtrGtDataSet(TrpHtr htr, DataSetType dataSetType) {
			super(htr, dataSetType);
			switch(dataSetType) {
			case TRAIN:
				if(htr.getNrOfTrainGtPages() != null) {
					super.size = htr.getNrOfTrainGtPages();
				}
				break;
			case VALIDATION:
				if(htr.getNrOfValidationGtPages() != null) {
					super.size = htr.getNrOfValidationGtPages();
				}
				break;
			}
		}
		@Override
		public int getId() {
			return getModel().getHtrId();
		}
		@Override
		public String getName() {
			return getModel().getName();
		}
	}
	
	/**
	 * TrpDocMetadata type that decorates a HtrGtDataSet. This type is needed to determine the origin of a GT doc that is loaded in Storage. 
	 */
	public static class TrpHtrGtDocMetadata extends TrpDocMetadata {
		private static final long serialVersionUID = -3302933027729222456L;
		private final HtrGtDataSet dataSet;
		public TrpHtrGtDocMetadata(HtrGtDataSet dataSet) {
			this.dataSet = dataSet;
			this.setTitle("HTR '" + dataSet.getModel().getName() + "' " + dataSet.getDataSetType().getLabel());
		}
		public HtrGtDataSet getDataSet() {
			return dataSet;
		}
		@Override
		public Integer getStatus() {
			return RemoteDocConst.STATUS_GROUND_TRUTH_DOC;
		}
	}
	
	/**
	 * An instance of this type represents an HTR GroundTruth data set element (e.g. a page from train or validation set) and is used for 
	 * displaying the ground truth page level in a HTR treeviewer. It wraps a TrpGroundTruthPage and has a reference to its parent HtrGtDataSet.
	 * <br><br>
	 * Using plain TrpGroundTruthPage objects would not allow to determine the original parent as a GroundTruthPage may be linked in 
	 * several HTRs (with different pageNr though). This is a problem when a single page is added to the selection and {@link HtrGroundTruthContentProvider#getParent(Object)} is called.
	 */
	public static class HtrGtDataSetElement extends AGtDataSetElement<HtrGtDataSet> {
		public HtrGtDataSetElement(HtrGtDataSet parentHtrGtDataSet, TrpGroundTruthPage gtPage) {
			super(parentHtrGtDataSet, gtPage);
		}
		@Deprecated
		public HtrGtDataSet getParentHtrGtDataSet() {
			return super.getParentGtDataSet();
		}
	}
}
