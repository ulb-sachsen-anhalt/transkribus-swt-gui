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
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.io.RemoteDocConst;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpGroundTruthPage;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.enums.DataSetType;
import eu.transkribus.core.util.DescriptorUtils.AGtDataSet;
import eu.transkribus.swt.util.ACollectionBoundStructuredContentProvider;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;

public class ModelGroundTruthContentProvider extends ACollectionBoundStructuredContentProvider implements ITreeContentProvider, IStorageListener {
	private static final Logger logger = LoggerFactory.getLogger(ModelGroundTruthContentProvider.class);
	List<TrpModelMetadata> htrs;
	
	/**
	 * Omit constant API queries for data sets as those are static anyway.
	 */
	private final static Map<ModelGtDataSet, List<ModelGtDataSetElement>> DATA_SET_CACHE = Collections.synchronizedMap(new HashMap<>());
	
	public ModelGroundTruthContentProvider(final Integer colId) {
		super(colId);
		store.addListener(this);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {		
		if (newInput instanceof List<?>) {
			this.htrs = ((List<TrpModelMetadata>) newInput);
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {

		if (inputElement instanceof List<?>) {
			return ((List<TrpModelMetadata>) inputElement).toArray();			
		} else if (inputElement instanceof TrpModelMetadata) {
			return getChildren((TrpModelMetadata) inputElement);
		} else if (inputElement instanceof AGtDataSet<?>) {
			return getChildren((AGtDataSet<?>) inputElement);
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TrpModelMetadata) {
			return getChildren((TrpModelMetadata) parentElement);
		} else if (parentElement instanceof AGtDataSet<?>) {
			return getChildren((ModelGtDataSet) parentElement);
		}
		return null;
	}
	
	AGtDataSet<TrpModelMetadata>[] getChildren(TrpModelMetadata model) {
		ModelGtDataSet trainSet = null;
		ModelGtDataSet valSet = null;
		
		//load details
		TrpModelMetadata details;
		try {
			details = store.getConnection().getModelCalls().getModel(model);
		} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
			logger.error("Could not load model from server!", e);
			return null;
		}
		
		if(details.getNrOfTrainGtPages() != null && details.getNrOfTrainGtPages() > 0) {
			trainSet = new ModelGtDataSet(details, DataSetType.TRAIN);
		}
		if(details.getNrOfValidationGtPages() != null && details.getNrOfValidationGtPages() > 0) {
			valSet = new ModelGtDataSet(details, DataSetType.VALIDATION);
		}
		if(trainSet != null && valSet != null) {
			return new ModelGtDataSet[] { trainSet, valSet };
		} else if (trainSet != null) {
			return new ModelGtDataSet[] { trainSet };
		}
		return null;
	}
	
	AGtDataSetElement<?>[] getChildren(ModelGtDataSet gt) {
		List<TrpGroundTruthPage> gtList = null;
		
		if(!gt.getModel().isGtAccessible()) {
			return null;
		}
		
		if(DATA_SET_CACHE.containsKey(gt)) {
			logger.debug("Returning GT data set cache entry");
			List<ModelGtDataSetElement> elements =  DATA_SET_CACHE.get(gt);
			return elements.toArray(new ModelGtDataSetElement[elements.size()]);
		}
		
		switch(gt.getDataSetType()) {
		case TRAIN:
			try {
				gtList = store.getConnection().getModelCalls().getHtrTrainData(gt.getId());
			} catch (TrpClientErrorException e) {
				logger.warn("Could not retrieve GT: {}", e.getMessageToUser());
			} catch (SessionExpiredException | IllegalArgumentException e) {
				logger.error("Could not retrieve HTR train data set for HTR = " + gt.getId(), e);
			}
			break;
		case VALIDATION:
			try {
				gtList = store.getConnection().getModelCalls().getHtrValidationData(gt.getId());
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
			List<ModelGtDataSetElement> children = gtList.stream()
					.map(g -> new ModelGtDataSetElement(gt, g))
					.collect(Collectors.toList());
			synchronized(DATA_SET_CACHE) {
				DATA_SET_CACHE.put(gt, children);
			}
			return children.toArray(new ModelGtDataSetElement[children.size()]);
		}
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof List<?>) {
			return null;
		} else if (element instanceof TrpModelMetadata) {
			return htrs;
		} else if (element instanceof AGtDataSet<?>) {
			return((AGtDataSet<?>) element).getModel();
		} else if (element instanceof AGtDataSetElement<?>) {
			return ((AGtDataSetElement<?>) element).getParentGtDataSet();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof List<?> && ((List<?>) element).size() > 0) {
			return true;
		} else if (element instanceof TrpModelMetadata) {
			return hasTrainGt(((TrpModelMetadata) element));
		} else if (element instanceof ModelGtDataSet) {
			final ModelGtDataSet s = ((ModelGtDataSet) element);
			TrpModelMetadata h = ((ModelGtDataSet) element).getModel();
			if(!h.isGtAccessible()) {
				return false;
			}
			
			switch (s.getDataSetType()) {
			case TRAIN:
				return hasTrainGt(h);
			case VALIDATION:
				return hasValidationGt(h);
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
	
	private static boolean hasTrainGt(TrpModelMetadata m) {
		return m.getNrOfTrainGtPages() != null && m.getNrOfTrainGtPages() > 0;
	}
	
	private static boolean hasValidationGt(TrpModelMetadata m) {
		return m.getNrOfValidationGtPages() != null && m.getNrOfValidationGtPages() > 0;
	}
	
	/**
	 * An instance of this type represents an HTR GroundTruth data set (e.g. train or validation set) and is used for 
	 * displaying the ground truth set level in a HTR treeviewer.
	 */
	public static class ModelGtDataSet extends AGtDataSet<TrpModelMetadata> {
		public ModelGtDataSet(TrpModelMetadata htr, DataSetType dataSetType) {
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
			return getModel().getModelId();
		}
		@Override
		public String getName() {
			return getModel().getName();
		}
	}
	
	/**
	 * TrpDocMetadata type that decorates a HtrGtDataSet. This type is needed to determine the origin of a GT doc that is loaded in Storage. 
	 */
	public static class TrpModelGtDocMetadata extends TrpDocMetadata {
		private static final long serialVersionUID = -3302933027729222456L;
		private final AGtDataSet<?> dataSet;
		public TrpModelGtDocMetadata(AGtDataSet<?> dataSet) {
			this.dataSet = dataSet;
			this.setTitle("HTR '" + dataSet.getName() + "' " + dataSet.getDataSetType().getLabel());
		}
		public AGtDataSet<?> getDataSet() {
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
	 * several HTRs (with different pageNr though). This is a problem when a single page is added to the selection and {@link ModelGroundTruthContentProvider#getParent(Object)} is called.
	 */
	public static class ModelGtDataSetElement extends AGtDataSetElement<ModelGtDataSet> {
		public ModelGtDataSetElement(ModelGtDataSet parentHtrGtDataSet, TrpGroundTruthPage gtPage) {
			super(parentHtrGtDataSet, gtPage);
		}
	}
	
	public abstract static class AGtDataSetElement<T extends AGtDataSet<?>> {
		private final T parentGtDataSet;
		private final TrpGroundTruthPage gtPage;
		public AGtDataSetElement(T parentGtDataSet, TrpGroundTruthPage gtPage) {
			this.parentGtDataSet = parentGtDataSet;
			this.gtPage = gtPage;
		}
		public T getParentGtDataSet() {
			return parentGtDataSet;
		}
		public TrpGroundTruthPage getGroundTruthPage() {
			return gtPage;
		}
	}
}
