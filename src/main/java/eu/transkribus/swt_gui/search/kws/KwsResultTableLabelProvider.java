package eu.transkribus.swt_gui.search.kws;

import java.text.DateFormat;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.CoreUtils;

public class KwsResultTableLabelProvider implements ITableLabelProvider, ITableFontProvider {
	private final static Logger logger = LoggerFactory.getLogger(KwsResultTableLabelProvider.class);
	
	Table table;
	TableViewer tableViewer;
	DateFormat dateFormat;

	public KwsResultTableLabelProvider(TableViewer tableViewer) {
		this.tableViewer = tableViewer;
		this.table = tableViewer.getTable();
		this.dateFormat = CoreUtils.newDateFormatUserFriendly();
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
		
		return null;
	}

//	@Override
//	public String getColumnText(Object element, int columnIndex) {
//		if (element instanceof TrpKwsResultTableEntry) {
//			TrpKwsResultTableEntry res = (TrpKwsResultTableEntry) element;
//			
//			TableColumn column = table.getColumn(columnIndex);
//			String ct = column.getText();
//			
//			if (ct.equals(KwsResultTableWidget.KWS_CREATED_COL)) {
//				return dateFormat.format(res.getCreated());
//			} else if (ct.equals(KwsResultTableWidget.KWS_STATUS_COL)) {
//				return res.getStatus();
//			} else if (ct.equals(KwsResultTableWidget.KWS_SCOPE_COL)) {
//				return res.getScope();
//			} else if (ct.equals(KwsResultTableWidget.KWS_DURATION_COL)) {
//				return res.getDuration();
//			} else if (ct.equals(KwsResultTableWidget.KWS_QUERY_COL)) {
//				return res.getQuery();
//			}
//		}
//		if(element instanceof TrpErrorResultTableEntry) {
//			TrpErrorResultTableEntry res = (TrpErrorResultTableEntry) element;
//			
//			TableColumn column = table.getColumn(columnIndex);
//			String ct = column.getText();
//			
//			if (ct.equals(KwsResultTableWidget.KWS_CREATED_COL)) {
//				return dateFormat.format(res.getCreated());
//			} else if (ct.equals(KwsResultTableWidget.KWS_STATUS_COL)) {
//				return res.getStatus();
//			} else if (ct.equals(KwsResultTableWidget.KWS_SCOPE_COL)) {
//				return res.getScope();
//			} else if (ct.equals(KwsResultTableWidget.KWS_DURATION_COL)) {
//				return res.getDuration();
//			} else if (ct.equals(KwsResultTableWidget.KWS_QUERY_COL)) {
//				return res.getQuery();
//			}
//			
//		}
//		if(element instanceof TrpSampleResultTableEntry) {
//			TrpSampleResultTableEntry res = (TrpSampleResultTableEntry) element;
//			
//			TableColumn column = table.getColumn(columnIndex);
//			String ct = column.getText();
//			
//			if (ct.equals(KwsResultTableWidget.KWS_CREATED_COL)) {
//				return dateFormat.format(res.getCreated());
//			} else if (ct.equals(KwsResultTableWidget.KWS_STATUS_COL)) {
//				return res.getStatus();
//			} else if (ct.equals(KwsResultTableWidget.KWS_SCOPE_COL)) {
//				return res.getScope();
//			} else if (ct.equals(KwsResultTableWidget.KWS_DURATION_COL)) {
//				return res.getDuration();
//			} else if (ct.equals(KwsResultTableWidget.KWS_QUERY_COL)) {
//				return res.getQuery();
//			}
//			
//		}
//		
//		return "i am error";
//	}
	
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof AJobResultTableEntry<?>) {
			AJobResultTableEntry<?> res = (AJobResultTableEntry<?>) element;
			TableColumn column = table.getColumn(columnIndex);
			String ct = column.getText();
			
			if (ct.equals(KwsResultTableWidget.KWS_CREATED_COL)) {
				return dateFormat.format(res.getCreated());
			} else if (ct.equals(KwsResultTableWidget.KWS_STATUS_COL)) {
				return res.getStatus();
			} else if (ct.equals(KwsResultTableWidget.KWS_SCOPE_COL)) {
				return res.getScope();
			} else if (ct.equals(KwsResultTableWidget.KWS_DURATION_COL)) {
				return res.getDuration();
			} else if (ct.equals(KwsResultTableWidget.KWS_QUERY_COL)) {
				return res.getQuery();
			}
		}
		return "i am error";
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}
}
