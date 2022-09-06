package eu.transkribus.swt_gui.transcription;

import org.eclipse.swt.widgets.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.enums.TranscriptionLevel;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.util.PointStrUtils;
import eu.transkribus.swt_gui.canvas.shapes.ICanvasShape;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;

public class LineTranscriptionWidgetListener extends ATranscriptionWidgetListener {
	private final static Logger logger = LoggerFactory.getLogger(LineTranscriptionWidgetListener.class);
		
	public LineTranscriptionWidgetListener(TrpMainWidget mainWidget, LineTranscriptionWidget transcriptionWidget) {		
		super(mainWidget, transcriptionWidget);
	}
		
	@Override
	protected void handleFocus(Event event) {
		if (event.data != null && event.data instanceof TrpTextLineType) {
			TrpTextLineType tl = (TrpTextLineType) event.data;
			ITrpShapeType shapeToSelect = getShapeToSelect(tl);
			ICanvasShape shape = mainWidget.selectObjectWithData(shapeToSelect, true, false);
			mainWidget.getCanvas().focusShape(shape);
		}
	}
		
	@Override
	protected void handleTextModified(Event event) {
		try {
			if (event.data != null && event.data instanceof TrpTextLineType) {
				TrpTextLineType tl = (TrpTextLineType) event.data;
				logger.debug("line text modified "+tl.getId()+ ", new text: "+event.text+" start/end: "+event.start+"/"+event.end);
				
				// OLD set new text if it has changed:
//				if (!event.text.equals(tl.getUnicodeText()))
//					tl.setUnicodeText(event.text, transcriptionWidget);
				
				// NEW edit new text:
				tl.editUnicodeText(event.start, event.end, event.text, transcriptionWidget);
				
				mainWidget.getUi().getStructureTreeWidget().updateTextLabels(null);
			}
		} catch (Throwable th) {
			String msg = "Error during text line modification";
			mainWidget.onError("Error updating transcription", msg, th);					
		}
	}
	
	@Override
	protected void handleDefaultSelectionChanged(Event event) {
		try {
			if (TrpMainWidget.getInstance().getUi().getSelectedTranscriptionType()!=TranscriptionLevel.LINE_BASED) {
				return;
			}			
			
			logger.debug("line default selection change, event: ["+event.start+"-"+event.end+"]");
			super.handleDefaultSelectionChanged(event);
			
			if (ATranscriptionWidget.SHOW_WORD_GRAPH_STUFF) {
				transcriptionWidget.getWordGraphEditor().refresh();
			}

			mainWidget.updatePageRelatedMetadata();
		} catch (Throwable th) {
			String msg = "Could not update default selection from transcription widget";
			mainWidget.onError("Error updating selection", msg, th);
		}
	}
		
	/**
	 * Depending on the data the given textline contains, returns the actual shape that is going to be selected: either the line
	 * itself or the baseline, if no line exists (nr of points <= 2) or lines are not visible!
	 */
	private static ITrpShapeType getShapeToSelect(TrpTextLineType tl) {
		int nPts=0;
		try {
			nPts = PointStrUtils.parsePoints(tl.getCoordinates()).size();
		} catch (Exception e) {
			logger.warn("cannot parse points from line: "+tl.getId()+", t = "+tl.getUnicodeText());
		}
		ITrpShapeType shapeToSelect = null;
		if (tl.getBaseline() != null && (nPts <= 2 || !TrpMainWidget.getInstance().getTrpSets().isShowLines())) {
			shapeToSelect = (TrpBaselineType) tl.getBaseline();
		} else {
			shapeToSelect = tl;
		}
		
		return shapeToSelect;
	}
	
	@Override
	protected void handleSelectionChanged(Event event) {
		try {
//			logger.debug("line selection change, event widget: "+event.widget);
			
			if (event.data!=null && event.data instanceof TrpTextLineType) {
				TrpTextLineType tl = (TrpTextLineType) event.data;
				int nPts = PointStrUtils.parsePoints(tl.getCoordinates()).size();
				
				ITrpShapeType shapeToSelect = getShapeToSelect(tl);
				logger.debug("selecting shape with data: "+shapeToSelect);
				
				ICanvasShape shape = mainWidget.selectObjectWithData(shapeToSelect, true, false);
				mainWidget.getScene().makeShapeVisible(shape);
				
				// TODO: multiselection in line transcription widget
//				ICanvasShape lastShape = null;
//				mainWidget.getScene().clearSelected();
//				for (TrpTextLineType stl : transcriptionWidget.getSelectedShapes()) {
//					lastShape = mainWidget.selectObjectWithData(stl, false, true);
//					logger.debug("SEL: "+lastShape);
//				}
//				if (lastShape != null)
//					mainWidget.getScene().makeShapeVisible(lastShape);	
				
			}
		} catch (Throwable th) {
			String msg = "Could not update selection from transcription widget";
			mainWidget.onError("Error updating selection", msg, th);
		}	
	}

}
