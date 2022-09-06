package eu.transkribus.swt_gui.comments_widget;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import eu.transkribus.core.model.beans.TrpDbTag;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.customtags.CommentTag;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagList;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.swt.mytableviewer.ColumnConfig;
import eu.transkribus.swt.mytableviewer.MyTableViewer;
import eu.transkribus.swt.util.DefaultTableColumnViewerSorter;

public class CommentsTable extends MyTableViewer {
	
	public static final String PAGE_COL = "Page";
	public static final String COMMENT_COL = "Comment";
	public static final String OFFSET_COL = "Offset";
	public static final String LENGTH_COL = "Length";
	public static final String LINES_COL = "Line(s)";
	
	public final ColumnConfig[] COLS = new ColumnConfig[] {
//			new ColumnConfig(PAGE_COL, 50, false, DefaultTableColumnViewerSorter.ASC),
			new ColumnConfig(LINES_COL, 55, false, DefaultTableColumnViewerSorter.ASC),
			new ColumnConfig(COMMENT_COL, 200, false, DefaultTableColumnViewerSorter.ASC),

//			new ColumnConfig(OFFSET_COL, 50, false, DefaultTableColumnViewerSorter.ASC),
//			new ColumnConfig(LENGTH_COL, 50, false, DefaultTableColumnViewerSorter.ASC),
		};	

	public CommentsTable(Composite parent, int style) {
		super(parent, style | SWT.SINGLE | SWT.FULL_SELECTION);
		
		getTable().setHeaderVisible(true);
		
		setContentProvider(new ArrayContentProvider());
		setLabelProvider(new ITableLabelProvider() {			
			@Override public void removeListener(ILabelProviderListener listener) {
			}
			@Override public boolean isLabelProperty(Object element, String property) {
				return false;
			}
			@Override public void dispose() {
			}
			@Override public void addListener(ILabelProviderListener listener) {
			}
			@Override public String getColumnText(Object element, int columnIndex) {
				TableColumn column = getTable().getColumn(columnIndex);
				String ct = column.getText();
				
				if (element instanceof TrpDbTag) { // not used currently...
					TrpDbTag comment = (TrpDbTag) element;
					if (ct.equals(PAGE_COL)) {
						return ""+comment.getPagenr();
					} else if (ct.equals(COMMENT_COL)) {
						return comment.getValue();
					} 
					else if (ct.equals(OFFSET_COL)) {				
						return ""+comment.getOffset();
					} else if (ct.equals(LENGTH_COL)) {
						return ""+comment.getLength();
					} else if (ct.equals(LINES_COL)) {
						return comment.getRegionid();
					}					
				}
				else if (element instanceof CommentTag) {
					CommentTag comment = (CommentTag) element;
					CustomTagList tl = comment.getCustomTagList();
					TrpTextLineType line = null;
					if (tl.getShape() instanceof TrpTextLineType) {
						line = (TrpTextLineType) tl.getShape();
					}

					TrpTranscriptMetadata md = tl.getShape().getPage().getMd();					
						
					if (ct.equals(PAGE_COL)) {
						return md!=null ? ""+md.getPageNr() : ""+-1;
					} else if (ct.equals(COMMENT_COL)) {
						return comment.getComment();
					} 
					else if (ct.equals(OFFSET_COL)) {				
						return ""+comment.getOffset();
					} else if (ct.equals(LENGTH_COL)) {
						return ""+comment.getLength();
					} else if (ct.equals(LINES_COL)) {
						String lStr = ""+(line.getIndex()+1);
						for (CustomTag t : comment.continuations) {
							if ( t.getCustomTagList().getShape() instanceof TrpTextLineType) {
								TrpTextLineType l0 = (TrpTextLineType) t.getCustomTagList().getShape();
								lStr += ", "+(l0.getIndex()+1);
							}
						}
						return lStr;
					}
				}

				return "i am error";
			}
			
			@Override public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
		} );
		
		addColumns(COLS);
	}

}
