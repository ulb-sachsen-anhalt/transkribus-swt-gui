package eu.transkribus.swt_gui.models;

import java.text.DateFormat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.HtrCITlabUtils;
import eu.transkribus.core.util.IsoLangUtils;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.Images;

public class ModelViewerLabelProvider implements ITableLabelProvider, ITableFontProvider, ITreePathLabelProvider {

	private final static String NOT_AVAILABLE_LABEL = "N/A";

	ViewerAdapter viewer;
	DateFormat createDateFormat;

	public ModelViewerLabelProvider(TableViewer tableViewer) {
		this.viewer = new TableViewerAdapter(tableViewer);
		this.createDateFormat = CoreUtils.newDateFormatddMMYY();
	}
	
	public ModelViewerLabelProvider(TreeViewer treeViewer) {
		this.viewer = new TreeViewerAdapter(treeViewer);
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
		Item column = viewer.getColumn(columnIndex);
		String ct = column.getText();
		if (element instanceof TrpModelMetadata) {
			TrpModelMetadata model = (TrpModelMetadata) element;
			ReleaseLevel release = ReleaseLevel.fromString(model.getReleaseLevel());
			switch (ct) {
			case ModelPagedTableWidget.MODEL_NAME_COL:
				if (release.getValue() > 0) {
					if(model.isFeatured() != null && model.isFeatured()) {
						return Images.MODEL_FEATURED_ICON;
					}
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
		if (element instanceof TrpModelMetadata) {
			TrpModelMetadata htr = (TrpModelMetadata) element;

			Item column = viewer.getColumn(columnIndex);
			String ct = column.getText();
			
			return getColumnText(htr, ct);
		} else {
			return NOT_AVAILABLE_LABEL;
		}
	}
	
	public String getColumnText(TrpModelMetadata model, String columnName) {
		switch (columnName) {
		case ModelPagedTableWidget.MODEL_NAME_COL:
			return model.getName();
		case ModelPagedTableWidget.MODEL_LANG_COL:
			return IsoLangUtils.DEFAULT_RESOLVER.getLanguageWithResolvedIsoCodes(model.getLanguage());
		case ModelPagedTableWidget.MODEL_ID_COL:
			return "" + model.getModelId();
		case ModelPagedTableWidget.MODEL_CREATOR_COL:
			return model.getUserName() == null ? "Unknown" : model.getUserName();
		case ModelPagedTableWidget.MODEL_TECH_COL:
			return getLabelForHtrProvider(model.getProvider());
		case ModelPagedTableWidget.MODEL_DATE_COL:
			return createDateFormat.format(model.getCreated());
		case ModelPagedTableWidget.MODEL_WORDS_COL:
			return createDateFormat.format(model.getNrOfWords());
		case ModelPagedTableWidget.MODEL_CER_TRAIN_COL:
			return model.getFinalCer() == null ? "N/A" : "" + HtrCITlabUtils.formatCerVal(model.getFinalCer());
		case ModelPagedTableWidget.MODEL_CER_VAL_COL:
			return model.getFinalValidationCer() == null ? "N/A" : "" + HtrCITlabUtils.formatCerVal(model.getFinalValidationCer());
		case ModelPagedTableWidget.MODEL_RATING_COL:
			return model.getInternalRating() == null ? "N/A" : "" + model.getInternalRating();	
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
		return null;
	}

	@Override
	public void updateLabel(ViewerLabel arg0, TreePath arg1) {}
	
	private static interface ViewerAdapter {
		public Item getColumn(int columnIndex);
	}
	
	private static class TableViewerAdapter implements ViewerAdapter {
		Table table;
		
		public TableViewerAdapter(TableViewer viewer) {
			this.table = viewer.getTable();
		}
		
		@Override
		public Item getColumn(int columnIndex) {
			return table.getColumn(columnIndex);
		}
	}
	
	private static class TreeViewerAdapter implements ViewerAdapter {
		Tree tree;
		
		public TreeViewerAdapter(TreeViewer viewer) {
			this.tree = viewer.getTree();
		}
		
		@Override
		public Item getColumn(int columnIndex) {
			return tree.getColumn(columnIndex);
		}
	}
}
