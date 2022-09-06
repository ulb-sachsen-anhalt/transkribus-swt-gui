package eu.transkribus.swt_gui.htr.treeviewer;

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.graphics.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.DescriptorUtils.AGtDataSet;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.AGtDataSetElement;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.TrpModelGtDocMetadata;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * GroundTruthLabelAndFontProvider that sets a bold font on the GT pages loaded in the MainWidget/Storage 
 */
public class GroundTruthLabelAndFontProvider extends GroundTruthLabelProvider implements IFontProvider {
	private static final Logger logger = LoggerFactory.getLogger(GroundTruthLabelAndFontProvider.class);
	
	protected final Font boldFont;
	protected Storage store;
	public GroundTruthLabelAndFontProvider(Font defaultFont) {
		super();
		this.store = Storage.getInstance();
		this.boldFont = Fonts.createBoldFont(defaultFont);
	}
	@Override
	public Font getFont(Object element) {
		if(!store.isGtDoc()) {
			return null;
		}
		AGtDataSet<?> loadedSet = ((TrpModelGtDocMetadata) store.getDoc().getMd()).getDataSet();

		if(element instanceof AGtDataSet<?>) {
			logger.debug("getFont for " + element + " | loadedSet = " + loadedSet);
			if(((AGtDataSet<?>)element).equals(loadedSet)) {
				return boldFont;
			}
		} else if (element instanceof AGtDataSetElement<?>
				&& ((AGtDataSetElement<?>)element).getParentGtDataSet().equals(loadedSet) 
				&& ((AGtDataSetElement<?>)element).getGroundTruthPage().getPageNr() == store.getPage().getPageNr()) {
			return boldFont;
		}
		return null;
	}
}
