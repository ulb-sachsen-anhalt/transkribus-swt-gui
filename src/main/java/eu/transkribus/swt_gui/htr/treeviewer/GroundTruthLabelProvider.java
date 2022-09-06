package eu.transkribus.swt_gui.htr.treeviewer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TrpGroundTruthPage;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.util.DescriptorUtils.AGtDataSet;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.AGtDataSetElement;
import eu.transkribus.swt_gui.models.ModelPagedTableWidget;

public class GroundTruthLabelProvider extends LabelProvider {	
	@Override
	public Image getImage(Object element) {
		if(element instanceof AGtDataSetElement<?>) {
			return Images.IMAGE;
		} else if (element instanceof AGtDataSet<?>) {
			return Images.FOLDER;
		} else if (element instanceof TrpHtr) {
			if(((TrpHtr)element).getReleaseLevelValue() > 0) {
				return Images.MODEL_SHARED_ICON;
			}
			return Images.MODEL_ICON;
		} else if (element instanceof TrpModelMetadata) {
			TrpModelMetadata model = (TrpModelMetadata) element;
			ReleaseLevel release = ReleaseLevel.fromString(model.getReleaseLevel());
			if (release.getValue() > 0) {
				if(model.isFeatured() != null && model.isFeatured()) {
					return Images.MODEL_FEATURED_ICON;
				}
				return Images.MODEL_SHARED_ICON;
			}
			return Images.MODEL_ICON;
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if(element instanceof TrpHtr) {
			TrpHtr htr = (TrpHtr) element;
			//be consistent with the documents view and add the ID as long as this is not a treeviewer with table columns
			return htr.getHtrId() + " - " + htr.getName();
		} else if(element instanceof TrpModelMetadata) {
			TrpModelMetadata model = (TrpModelMetadata) element;
			//be consistent with the documents view and add the ID as long as this is not a treeviewer with table columns
			return model.getModelId() + " - " + model.getName();
		} else if (element instanceof AGtDataSet<?>) {
			return getText((AGtDataSet<?>)element) ;
		} else if (element instanceof AGtDataSetElement<?>) {
			return getText((AGtDataSetElement<?>)element);
		}
		return null;
	}

	protected String getText(AGtDataSet<?> s) {
		final String nrOfPages = "(" + s.getSize() + " pages)";
		return StringUtils.rightPad(s.getDataSetType().getLabel(), 15) + nrOfPages;
	}

	protected String getText(AGtDataSetElement<?> element) {
		TrpGroundTruthPage p = element.getGroundTruthPage();
		return "Page " + StringUtils.rightPad("" + p.getPageNr(), 5) 
				+ "(" + p.getNrOfLines() + " lines, " + p.getNrOfWordsInLines() + " words)";
	}
}
