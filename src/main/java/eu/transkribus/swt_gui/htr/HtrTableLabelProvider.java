package eu.transkribus.swt_gui.htr;

import java.text.DateFormat;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.HtrCITlabUtils;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.Images;

public class HtrTableLabelProvider implements ITableLabelProvider, ITableFontProvider {

	private final static String NOT_AVAILABLE_LABEL = "N/A";

	Table table;
	TableViewer tableViewer;
	DateFormat createDateFormat;

	public HtrTableLabelProvider(TableViewer tableViewer) {
		this.tableViewer = tableViewer;
		this.table = tableViewer.getTable();
		this.createDateFormat = CoreUtils.newDateFormatddMMYY();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void dispose() {

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {

		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {

	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		TableColumn column = table.getColumn(columnIndex);
		String ct = column.getText();
		if (element instanceof TrpHtr) {
			switch (ct) {
			case HtrTableWidget.HTR_NAME_COL:
				if(((TrpHtr)element).getReleaseLevelValue() > 0) {
					return Images.MODEL_SHARED_ICON;
				}
				return Images.MODEL_ICON;
			default:
				return null;
			}
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof TrpHtr) {
			TrpHtr htr = (TrpHtr) element;

			TableColumn column = table.getColumn(columnIndex);
			String ct = column.getText();
			
			return getColumnText(htr, ct);
		} else {
			return NOT_AVAILABLE_LABEL;
		}
	}
	
	public String getColumnText(TrpHtr htr, String columnName) {
		switch (columnName) {
		case HtrPagedTableWidget.HTR_NAME_COL:
			return htr.getName();
		case HtrPagedTableWidget.HTR_LANG_COL:
//			return htr.getLanguage();
			return htr.getResolvedLanguageString();
		case HtrPagedTableWidget.HTR_ID_COL:
			return "" + htr.getHtrId();
		case HtrPagedTableWidget.HTR_CREATOR_COL:
			return htr.getUserName() == null ? "Unknown" : htr.getUserName();
		case HtrPagedTableWidget.HTR_TECH_COL:
			return getLabelForHtrProvider(htr.getProvider());
		case HtrPagedTableWidget.HTR_DATE_COL:
			return createDateFormat.format(htr.getCreated());
		case HtrPagedTableWidget.HTR_WORDS_COL:
			return createDateFormat.format(htr.getNrOfWords());
		case HtrPagedTableWidget.HTR_CER_TRAIN_COL:
			return htr.getFinalCer() == null ? "N/A" : "" + HtrCITlabUtils.formatCerVal(htr.getFinalCer());
		case HtrPagedTableWidget.HTR_CER_VAL_COL:
			return htr.getFinalCerTest() == null ? "N/A" : "" + HtrCITlabUtils.formatCerVal(htr.getFinalCerTest());
		case HtrPagedTableWidget.HTR_RATING_COL:
			return htr.getInternalRating() == null ? "N/A" : "" + htr.getInternalRating();	
		default:
			return NOT_AVAILABLE_LABEL;
		}
	}

	public static String getLabelForHtrProvider(String provider) {
		String lbl = ModelUtil.getLabelForModelProvider(provider);
		return lbl == null ? NOT_AVAILABLE_LABEL : lbl;
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}
}
