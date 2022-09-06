package eu.transkribus.swt_gui.structure_tree;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPageType;

public class StructureTreeContentProvider implements ITreeContentProvider {
	private static final Logger logger = LoggerFactory.getLogger(StructureTreeContentProvider.class);
	
//	public static boolean DISPLAY_ROOT_ELEMENT=true;
//	TrpPageType page;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {		
//		if (newInput instanceof TrpPageType) {
//			this.page = (TrpPageType) newInput;
//		}
	}

	@Override
	public Object[] getElements(Object inputElement) {

		if (inputElement instanceof TrpPageType) {
			return ((TrpPageType) inputElement).getAllShapes(false).toArray();			
		} else if (inputElement instanceof PcGtsType) {
			return new Object[] { ((PcGtsType) inputElement).getPage() };
		}
		return null;
	}
	
//	public List<Object> getSubElements(Object parent) {
//		List<Object> subElements = new ArrayList<Object>();
//		if (hasChildren(parent)) {
//			for (Object c : getChildren(parent)) {
//				for (Object c1 : getSubElements(c)) {
//					subElements.add(c1);
//				}
//				subElements.add(c);
//			}
//		}
//		return subElements;
//	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TrpPageType) {
			logger.trace("getting children elements for: "+parentElement);
//			return getElements(parentElement);
			return ((TrpPageType) parentElement).getAllShapes(false).toArray();
		}
//		else if (parentElement instanceof TrpTableRegionType) {
//			logger.debug("is TrpTableRegionType type");
//			return ((ITrpShapeType)parentElement).getChildren(true).toArray();
//		}
//		else if (parentElement instanceof TableRegionType) {
//			logger.debug("is TableRegionType type");
//			return ((ITrpShapeType)parentElement).getChildren(true).toArray();
//		}
//		else if (parentElement instanceof TrpRegionType) {
//			logger.debug("is TrpRegionType type");
//			return ((ITrpShapeType)parentElement).getChildren(true).toArray();
//		}
		else if (parentElement instanceof ITrpShapeType) {
			logger.trace("name of shape " + ((ITrpShapeType) parentElement).getName());
			logger.trace("shape " + parentElement);
			return ((ITrpShapeType)parentElement).getChildren(false).toArray();
		}
		else{
			logger.trace("(else) name of shape " + ((ITrpShapeType) parentElement).getName());
		}
//		else if (parentElement instanceof TrpTextRegionType) {
//			return ((TrpTextRegionType)parentElement).getTextLine().toArray();
//		}
//		else if (parentElement instanceof TrpTextLineType) {
//			TrpTextLineType line = (TrpTextLineType) parentElement;
//			Object[] elements = new Object[line.getBaseline()==null ? line.getWord().size() : line.getWord().size()+1];
//			int i=0;
//			if (line.getBaseline()!=null)
//				elements[i++] = line.getBaseline();
//			for (WordType w : line.getWord()) {
//				elements[i++] = w;
//			}
//			
//			return elements;
//		}
//		else if (parentElement instanceof TrpBaselineType) {
//			return new Object[0];
//		}		
//		else if (parentElement instanceof TrpWordType) {
//			return new Object[0];
//		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TrpPageType) {
			return null;
		}
		else if (element instanceof ITrpShapeType) {
			return ((ITrpShapeType)element).getParent();
		}
//		else if (element instanceof TrpTextRegionType) {
//			return ((TrpTextRegionType)element).getPage();
//		}
//		else if (element instanceof TrpTextLineType) {
//			return ((TrpTextLineType) element).getRegion();
//		}
//		else if (element instanceof TrpBaselineType) {
//			return ((TrpBaselineType)element).getLine();
//		}		
//		else if (element instanceof TrpWordType) {
//			return ((TrpWordType)element).getLine();
//		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TrpPageType) {
			return !((TrpPageType)element).getTextRegions(false).isEmpty();
		}
		else if (element instanceof ITrpShapeType) {
			return ((ITrpShapeType)element).hasChildren();
		}
//		else if (element instanceof TrpTextRegionType) {
//			return !((TrpTextRegionType)element).getTextLine().isEmpty();
//		}
//		else if (element instanceof TrpTextLineType) {
//			TrpTextLineType line = (TrpTextLineType) element;
//			return line.getBaseline()==null && line.getWord().isEmpty();
//		}
//		else if (element instanceof TrpBaselineType) {
//			return false;
//		}		
//		else if (element instanceof TrpWordType) {
//			return false;
//		}

		return false;
	}
	
	

}
