package eu.transkribus.swt_gui.htr.treeviewer;

import java.text.DateFormat;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TrpGroundTruthPage;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.ModelGtDataSet;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.ModelGtDataSetElement;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.structure_tree.StructureTreeWidget.ColConfig;

/**
 * CellLabelProvider that sets a bold font on the GT pages loaded in the MainWidget/Storage 
 */
public class ModelGroundTruthTableLabelAndFontProvider extends CellLabelProvider implements ITableLabelProvider, IFontProvider {
	private static final Logger logger = LoggerFactory.getLogger(ModelGroundTruthTableLabelAndFontProvider.class);
	
	protected final Font boldFont;
	protected Storage store;
	DateFormat createDateFormat;
	
	GroundTruthLabelAndFontProvider delegate;
	
	public ModelGroundTruthTableLabelAndFontProvider(Font defaultFont) {
		super();
		this.store = Storage.getInstance();
		this.boldFont = Fonts.createBoldFont(defaultFont);
		this.createDateFormat = CoreUtils.newDateFormatddMMYY();
		this.delegate = new GroundTruthLabelAndFontProvider(defaultFont);
	}
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		ColConfig col = GroundTruthPagedTreeWidget.COLUMNS[columnIndex];
		if(!GroundTruthPagedTreeWidget.NAME_COL.equals(col)) {
			return null;
		}
		return this.delegate.getImage(element);
	}
	
	@Override
	public String getColumnText(Object element, int columnIndex) {
		ColConfig col = GroundTruthPagedTreeWidget.COLUMNS[columnIndex];
			
		if(element instanceof TrpModelMetadata) {
			TrpModelMetadata m = (TrpModelMetadata)element;
			if(GroundTruthPagedTreeWidget.ID_COL.equals(col)) {
				return "" + m.getModelId();
			} else if(GroundTruthPagedTreeWidget.NAME_COL.equals(col)) {
				return m.getName();
			} else if(GroundTruthPagedTreeWidget.CURATOR_COL.equals(col)) {
				return ((TrpModelMetadata)element).getUserName();
			} else if(GroundTruthPagedTreeWidget.DATE_COL.equals(col)) {
				return createDateFormat.format(((TrpModelMetadata)element).getCreated());
			} else if(GroundTruthPagedTreeWidget.WORD_COL.equals(col)) {
				return Integer.toString(((TrpModelMetadata)element).getNrOfWords());
			}
		} else if(element instanceof ModelGtDataSet) {
			ModelGtDataSet set = (ModelGtDataSet) element;
			if(GroundTruthPagedTreeWidget.NAME_COL.equals(col)) {
				String suffix = "";
				ReleaseLevel release = ReleaseLevel.fromString(set.getModel().getReleaseLevel());
				if(release.getValue() > 0 
						&& ReleaseLevel.isPrivateDataSet(release)) {
					suffix = " (private)";
				}
				return set.getDataSetType().getLabel() + suffix;
			} else if(GroundTruthPagedTreeWidget.SIZE_COL.equals(col)) {
				return set.getSize() + " pages" ;
			}
		} else if (element instanceof ModelGtDataSetElement) {
			ModelGtDataSetElement e = (ModelGtDataSetElement) element;
			if(GroundTruthPagedTreeWidget.NAME_COL.equals(col)) {
				return "Page " + e.getGroundTruthPage().getPageNr();
			} else if(GroundTruthPagedTreeWidget.SIZE_COL.equals(col)) {
				TrpGroundTruthPage p = e.getGroundTruthPage();
				return p.getNrOfLines() + " lines, " + p.getNrOfWordsInLines() + " words";
			}
		}
		return null;
	}
	
	@Override
	public Font getFont(Object element) {
		return this.delegate.getFont(element);
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getViewerRow().getElement();
		cell.setText(getColumnText(element, cell.getColumnIndex()));
		cell.setImage(getColumnImage(element, cell.getColumnIndex()));
		cell.setFont(getFont(element));
	}
}
