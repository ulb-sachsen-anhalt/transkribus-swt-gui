package eu.transkribus.swt_gui.collection_treeviewer;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.swt.util.Images;

public class CollectionLabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		if(element instanceof TrpPage) {
			return Images.IMAGE;
		} else if (element instanceof TrpDocMetadata) {
			return Images.FOLDER;
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if(element instanceof TrpDocMetadata) {
			TrpDocMetadata d = (TrpDocMetadata)element;
			return d.getDocId() + " - " + d.getTitle() + " (" + d.getNrOfPages() + " pages)";
		} else if (element instanceof TrpPage) {
			TrpPage p = (TrpPage)element;
			return "Page " + p.getPageNr() + " (" + p.getCurrentTranscript().getNrOfTranscribedLines() + " lines)";
		}
		return null;
	}
}
