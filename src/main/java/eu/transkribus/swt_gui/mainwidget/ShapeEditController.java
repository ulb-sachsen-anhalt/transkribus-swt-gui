package eu.transkribus.swt_gui.mainwidget;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.JAXBPageTranscript;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory;
import eu.transkribus.core.model.beans.enums.EditStatus;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.RegionType;
import eu.transkribus.core.model.beans.pagecontent.SeparatorRegionType;
import eu.transkribus.core.model.beans.pagecontent.TableCellType;
import eu.transkribus.core.model.beans.pagecontent.TableRegionType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent.TranskribusMetadataType;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPageType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpShapeTypeUtils;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTableRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.MonitorUtil;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.core.util.PointStrUtils;
import eu.transkribus.core.util.SebisStopWatch.SSW;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.canvas.CanvasException;
import eu.transkribus.swt_gui.canvas.CanvasMode;
import eu.transkribus.swt_gui.canvas.editing.CanvasShapeEditor;
import eu.transkribus.swt_gui.canvas.editing.ShapeEditOperation;
import eu.transkribus.swt_gui.canvas.shapes.CanvasPolygon;
import eu.transkribus.swt_gui.canvas.shapes.CanvasPolyline;
import eu.transkribus.swt_gui.canvas.shapes.CanvasShapeUtil;
import eu.transkribus.swt_gui.canvas.shapes.ICanvasShape;
import eu.transkribus.swt_gui.canvas.shapes.ShapePoint;
import eu.transkribus.swt_gui.dialogs.CopyShapesConfDialog;
import eu.transkribus.swt_gui.dialogs.CreateTableRowsDialog;
import eu.transkribus.swt_gui.dialogs.HandleTableConfDialog;
import eu.transkribus.swt_gui.dialogs.MergeTextLinesConfDialog;
import eu.transkribus.swt_gui.dialogs.RemoveTextLinesConfDialog;
import eu.transkribus.swt_gui.dialogs.RemoveTextRegionsConfDialog;
import eu.transkribus.swt_gui.exceptions.BaselineExistsException;
import eu.transkribus.swt_gui.exceptions.NoParentLineException;
import eu.transkribus.swt_gui.exceptions.NoParentRegionException;
import eu.transkribus.swt_gui.factory.TrpShapeElementFactory;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import math.geom2d.Point2D;

public class ShapeEditController extends AMainWidgetController {
	private static final Logger logger = LoggerFactory.getLogger(ShapeEditController.class);
	
	public ShapeEditController(TrpMainWidget mw) {
		super(mw);
	}
	
	public static final double DEFAULT_POLYGON_SIMPLICATION_PERCENTAGE = 1.0d;
	
	public void simplifySelectedLineShapes(boolean onlySelected) {
		simplifySelectedLineShapes(DEFAULT_POLYGON_SIMPLICATION_PERCENTAGE, onlySelected);
	}
	
	public void simplifySelectedLineShapes(double percentage, boolean onlySelected) {
		try {
			logger.debug("simplifying selected line shape");
			canvas.getShapeEditor().simplifyTextLines(percentage, onlySelected);
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}
	}
	
	public void createImageSizeTextRegion() {
		try {
			if (!storage.hasTranscript()) {
				return;
			}
			
			Rectangle imgBounds = canvas.getScene().getMainImage().getBounds();
			
			if (CanvasShapeUtil.getFirstTextRegionWithSize(storage.getTranscript().getPage(), 0, 0, imgBounds.width, imgBounds.height, false) != null) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "Top level region with size of image already exists!");
				return;
			}
			
			CanvasPolygon imgBoundsPoly = new CanvasPolygon(imgBounds);
//			CanvasMode modeBackup = canvas.getMode();
			canvas.setMode(CanvasMode.ADD_TEXTREGION);
			ShapeEditOperation op = canvas.getShapeEditor().addShapeToCanvas(imgBoundsPoly, true);
			canvas.setMode(CanvasMode.SELECTION);
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}	
	}

	public void createDefaultLineForSelectedShape() {
		if (canvas.getFirstSelected() == null)
			return;
		
		try {
			logger.debug("creating default line for seected line/baseline!");
			
//			CanvasPolyline baselineShape = (CanvasPolyline) shape;
//			shapeOfParent = baselineShape.getDefaultPolyRectangle();
			
			ICanvasShape shape = canvas.getFirstSelected();
			CanvasPolyline blShape = (CanvasPolyline) CanvasShapeUtil.getBaselineShape(shape);
			if (blShape == null)
				return;
			
			CanvasPolygon pl = blShape.getDefaultPolyRectangle();
			if (pl == null)
				return;
			
			ITrpShapeType st = (ITrpShapeType) shape.getData();
			TrpTextLineType line = TrpShapeTypeUtils.getLine(st);
			if (line != null) {
				ICanvasShape lineShape = (ICanvasShape) line.getData();
				if (lineShape != null) {
					lineShape.setPoints(pl.getPoints());
					
					canvas.redraw();
				}
			}
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}	
	}

	public void removeSmallTextRegions(Double fractionOfImageSize) {
		try {
			if (!storage.hasTranscript()) {
				return;
			}
			
			Rectangle imgBounds = canvas.getScene().getMainImage().getBounds();
			double area = imgBounds.width * imgBounds.height;
			if (fractionOfImageSize == null) {
				fractionOfImageSize = DialogUtil.showDoubleInputDialog(getShell(), "Input fraction", "Fraction of area ("+area+")", 0.001);
			}
			logger.debug("fractionOfImageSize = "+fractionOfImageSize);
			if (fractionOfImageSize == null) {
				return;
			}
			
			PageXmlUtils.filterOutSmallRegions(storage.getTranscript().getPageData(), fractionOfImageSize*area);
			
			canvas.redraw();
			mw.reloadCurrentTranscript(true, true, () -> {
				storage.setCurrentTranscriptEdited(true);	
			}, null);
			
//			if (CanvasShapeUtil.getFirstTextRegionWithSize(storage.getTranscript().getPage(), 0, 0, imgBounds.width, imgBounds.height, false) != null) {
//				DialogUtil.showErrorMessageBox(getShell(), "Error", "Top level region with size of image already exists!");
//				return;
//			}
//			
//			CanvasPolygon imgBoundsPoly = new CanvasPolygon(imgBounds);
////			CanvasMode modeBackup = canvas.getMode();
//			canvas.setMode(CanvasMode.ADD_TEXTREGION);
//			ShapeEditOperation op = canvas.getShapeEditor().addShapeToCanvas(imgBoundsPoly, true);
//			canvas.setMode(CanvasMode.SELECTION);
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}		
	}
	
	public void removeSmallTextLinesFromLoadedDoc() {
		try {
			logger.debug("removeSmallTextLinesFromLoadedDoc!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}
			
			RemoveTextLinesConfDialog d = new RemoveTextLinesConfDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			double fractionOfRegionSize = d.getThreshPerc();
			Set<Integer> pageIndices = d.getPageIndices();
			boolean dryRun = d.isDryRun();
			final double threshold = fractionOfRegionSize;
			
			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nShapesRemoved=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("removeSmallTextLinesFromLoadedDoc");
						TrpDoc doc = storage.getDoc();
						int worked=0;				
						
						int N;
						if (d.isApplySelected() && d.isDoCurrentPage()) {						
							pageIndices.clear();
							pageIndices.add(storage.getPageIndex());
							N=1;
						}				
						else {
							N = pageIndices==null ? doc.getNPages() : pageIndices.size();
						}

						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Removing small text lines, threshold = "+threshold, N);

						for (int i=0; i<doc.getNPages(); ++i) {
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
						
							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();
							
							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();
							
							List<TrpRegionType> trpShapes = null;
							if (d.isApplySelected() && d.isDoCurrentPage()) {
								//neu: process only selected shapes 
								List<ICanvasShape> shapes = mw.getCanvas().getScene().getSelected();
								if (shapes.isEmpty()) {
									shapes = mw.getCanvas().getScene().getShapes();
								}
								
								trpShapes = new ArrayList<TrpRegionType>();
								for (ICanvasShape shape : shapes) {
									
									ITrpShapeType type = shape.getTrpShapeType();
									//if (!(type instanceof TrpTextLineType) && !(type instanceof TrpWordType)) {
									if (type instanceof TrpTextRegionType || type instanceof TableCellType || type instanceof TableRegionType) {
										//logger.debug("instance of trpregiontype");
										trpShapes.add((TrpRegionType)type);
									}
								}
							}
							
							int nRemoved = PageXmlUtils.filterOutSmallLines(tr.getPageData(), threshold, trpShapes);
							
							res.nShapesRemoved += nRemoved;
							logger.debug("nRemoved = "+nRemoved);
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (nRemoved > 0) {
								++res.nPagesChanged;
								String msg = "Removed "+nRemoved+" lines < "+threshold+" * parent region width";
								
								res.affectedPageIndices.add(i);
								
								if (!dryRun) {
									mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, msg);									
								}
							}
							
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "Removed "+res.nShapesRemoved+" lines from "+res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Removing small lines", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Removed lines", res.msg+"\nPage numbers of affected pages: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));
			
			if (!dryRun) {
				mw.reloadCurrentPage(true, null, null);	
			}
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}		
	}
	
	/*
	 * tables are drawn - text regions needed for P2PaLA
	 */
	public void handleTable(boolean createTableFromRegions) {
		try {
			logger.debug("create text regions from table cells!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}
			
			HandleTableConfDialog d = new HandleTableConfDialog(getShell(),createTableFromRegions);
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			Set<Integer> pageIndices = d.getPageIndices();
			boolean keepBorders = d.isKeepBorders();
			
			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nRegionsCreated=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("create text regions");
						
						storage.reloadDocWithAllTranscripts();
						TrpDoc doc = storage.getDoc();
						int worked=0;				
						
						int N;
						N = pageIndices==null ? doc.getNPages() : pageIndices.size();

						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Create text regions from table cells! = ", N);

						for (int i=0; i<doc.getNPages(); ++i) {
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);

							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();
							//TrpTranscriptMetadata md = p.getTranscriptWichContainsStringInToolnameOrNull("Transkribus");
							
							
							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();
							
							List<TrpRegionType> regions = tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
							List<TrpRegionType> regionsCopy = new ArrayList<TrpRegionType>(regions);
							logger.debug("amount of regions in this page: " + regions.size());
							
							int nCreated = 0;
							int j = 0;
							if (!regions.isEmpty()) {
								
								if (createTableFromRegions) {
									nCreated += fromRectangleRegionsToTable(tr, keepBorders, true);
								}
								else {
									for (int k = 0; k < regionsCopy.size(); k++) {
										TrpRegionType r = regionsCopy.get(k);
										if (r instanceof TrpTableRegionType) {
											logger.debug("table nummero: " + (k+1));
											nCreated += fromTableToRegions(r, j++);											
										}
									}
//									for (TrpRegionType r2remove : toRemove) {
//										TrpShapeTypeUtils.removeShape(r2remove);
//									}
									
								}
						
							}							
							
							List<TrpRegionType> trpShapes = null;
							
							res.nRegionsCreated += nCreated;
							logger.debug("nRegionsCreated = "+nCreated);
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (nCreated > 0) {
								++res.nPagesChanged;
								String msg = "Created ";
								if (createTableFromRegions) {
									msg += nCreated + " table(s) from regions";
								}
								else {
									msg += nCreated + " regions from table columns";
								}

								res.affectedPageIndices.add(i);

								mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, msg);		

							}
							
							MonitorUtil.worked(monitor, ++worked);
							
							
						}
						
						res.msg = createTableFromRegions? "Created "+res.nRegionsCreated+" regions in "+res.nPagesChanged+"/"+res.nPagesTotal+" pages" : "Created "+res.nRegionsCreated+" tables in "+res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
	
			}, "Creating regions/tables", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Created regions/tables ", res.msg+"\nPage numbers of affected pages: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));

			mw.reloadCurrentPage(true, null, null);	
			
			
			
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}		
	}
	
	public void handleRows() {
		
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	handleTableRows();
		    }
		});
		
//		   new Thread(new Runnable() {
//			      public void run() {
//			         while (true) {
//			            try { Thread.sleep(1000); } catch (Exception e) { }
//			            Display.getDefault().asyncExec(new Runnable() {
//			               public void run() {
//			            	   
//			            	   
//			            	   
//			               }
//			            });
//			         }
//			      }
//			   }).start();
		
		
//		TrpPage p = storage.getPage();
//		TrpTranscriptMetadata md = p.getCurrentTranscript();
//		
//		JAXBPageTranscript tr = new JAXBPageTranscript(md);
//		try {
//			tr.build();
//		
//
//			//pcGtsType.getPage().getTextRegionOrImageRegionOrLineDrawingRegion().addAll(filtered);
//			List<TrpRegionType> regions = tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
//			logger.debug("amount of regions in this page: " + regions.size());
//	
//			if (!regions.isEmpty()) {
//				PcGtsType pc = tr.getPageData();
//				pc = splitTableIntoRows_(pc);	
//				tr.setPageData(pc);
//			}	
//			
//			//tr.build();
//			//storage.setCurrentTranscript(md);
//			storage.saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, "table rows created");	
//				//storage.saveTranscript(Storage.getInstance().getCollId(), "table rows created");
//	
//			mw.reloadCurrentPage(true, null, null);
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SessionExpiredException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ServerErrorException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public void createTableColumnsFromLines(){
		try {
			
			logger.debug("create table columns!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}
			
			storage.reloadDocWithAllTranscripts();
			
			HandleTableConfDialog d = new HandleTableConfDialog(getShell(), false);
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			Set<Integer> pageIndices = d.getPageIndices();
			
			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nRowsCreated=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {

						int worked=0;				
						TrpDoc doc = storage.getDoc();
						int N;
						if (pageIndices != null && !pageIndices.isEmpty()) {						
							N = pageIndices.size();
						}				
						else {
							N = doc.getNPages();
						}

						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Create table columns in nr of pages: ", res.nPagesTotal);

						for (int i=0; i<doc.getNPages(); ++i) {
							final boolean isLoaded = storage.getPageIndex() == i;
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
							
							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();
							
							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();
							
							List<TrpRegionType> regions = tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
							logger.debug("amount of regions in this page: " + regions.size());
							
							int nRowsCreated = 0;
							int j = 0;
							if (!regions.isEmpty()) {
								
								Display.getDefault().syncExec(new Runnable() {
								    public void run() {
								    	createRegionsFromLinesInColumns(tr);
								    }
								});

							}
							
							//res.nRegionsCreated += nRegionsCreated;
							logger.debug("nRowsCreated = "+nRowsCreated);
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (nRowsCreated > 0) {
								++res.nPagesChanged;
								String msg = "rows created "+nRowsCreated;
								
								res.nRowsCreated += nRowsCreated;
								res.affectedPageIndices.add(i);

//								mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, msg);	
//								Display.getDefault().syncExec(new Runnable() {
//								    public void run() {
//								    	if (isLoaded) {
//								    		mw.reloadCurrentPage(true, null, null);
//								    	}
//								    }
//								});
							}
							
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "Created rows "+res.nRowsCreated+" in "+res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}

			}, "Creating table columns", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Created table rows ", res.msg+"\nPage numbers of affected pages: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));

			mw.reloadCurrentPage(true, null, null);	
			
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}	
		
	}
	
	public void handleTableRows(){
		try {
			
			logger.debug("create table rows!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}
			
			storage.reloadDocWithAllTranscripts();
			
			CreateTableRowsDialog d = new CreateTableRowsDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			Set<Integer> pageIndices = d.getPageIndices();
			Integer columnNr = d.getColumnNr();
			double lineWidth = d.getLineWidth();
			boolean useHorizontalSeps = d.isUseHorizontalSeps();
			
			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nRowsCreated=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("create table rows");
						
						int worked=0;				
						TrpDoc doc = storage.getDoc();
						int N;
						if (pageIndices != null && !pageIndices.isEmpty()) {						
							N = pageIndices.size();
						}				
						else {
							N = doc.getNPages();
						}

						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Create table rows in nr of pages: ", res.nPagesTotal);

						for (int i=0; i<doc.getNPages(); ++i) {
							final boolean isLoaded = storage.getPageIndex() == i;
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
							
							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();
							
							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();
							
							List<TrpRegionType> regions = tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
							logger.debug("amount of regions in this page: " + regions.size());
							
							int nRowsCreated = 0;
							int j = 0;
							if (!regions.isEmpty()) {
								
								Display.getDefault().syncExec(new Runnable() {
								    public void run() {
								    	PcGtsType pc = tr.getPageData();
								    	splitTableIntoRows_(pc,columnNr,lineWidth, useHorizontalSeps);
								    }
								});

							}
							
							List<TrpRegionType> adapted = tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
							for (int k = 0; k < adapted.size(); k++) {
								TrpRegionType r = adapted.get(k);
								
								if (r instanceof TrpTableRegionType) {
									TrpTableRegionType currTableRegion = (TrpTableRegionType) r;
									nRowsCreated += currTableRegion.getNRows();
								}
							}
							
							//res.nRegionsCreated += nRegionsCreated;
							logger.debug("nRowsCreated = "+nRowsCreated);
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (nRowsCreated > 0) {
								++res.nPagesChanged;
								String msg = "rows created "+nRowsCreated;
								
								res.nRowsCreated += nRowsCreated;
								res.affectedPageIndices.add(i);

								mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, msg);	
//								Display.getDefault().syncExec(new Runnable() {
//								    public void run() {
//								    	if (isLoaded) {
//								    		mw.reloadCurrentPage(true, null, null);
//								    	}
//								    }
//								});
							}
							
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "Created rows "+res.nRowsCreated+" in "+res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}

			}, "Creating table rows", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Created table rows ", res.msg+"\nPage numbers of affected pages: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));

			mw.reloadCurrentPage(true, null, null);	
			
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}	
		
	}
	
	public int splitTableIntoRows_(PcGtsType pcGtsType, Integer columnIdxForSplit, double lineWidth, boolean useSeparators) {
		
		canvas.setVisible(false);
		
		List<TrpRegionType> regions = pcGtsType.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();

		//canvas.getScene().updateAllShapesParentInfo();
		TrpShapeElementFactory shapeFactory = new TrpShapeElementFactory(mw);

//		List<ICanvasShape> canvasShapes = canvas.getScene().getShapes();
		ICanvasShape currTable = null;
		CanvasShapeEditor shapeEdit = new CanvasShapeEditor(mw.getCanvas());
		for (int k = 0; k < regions.size(); k++) {
			TrpRegionType r = regions.get(k);
			
			int amountOfColumns = 0;
			if (r instanceof TrpTableRegionType) {
				TrpTableRegionType currTableRegion = (TrpTableRegionType) r;
				logger.debug("table found");
				shapeFactory.addAllCanvasShapes(currTableRegion);
				mw.reloadCurrentPage(true, null, null);
				if (r.getChildren(false).isEmpty()) {
					logger.debug("no children");
					continue;
				}
				else {
					logger.debug("get Data");
					currTable = (ICanvasShape) currTableRegion.getData();
					currTable.setData(currTableRegion);
					amountOfColumns = currTableRegion.getNCols();
				}
			}
			else {
				continue;
			}
		
//		if (currTable == null) {
//			logger.debug("no table available to find rows");
//			return;
//		}
			
			
			/*
			 * if separators are available we can use them
			 */
			
			if (useSeparators) {
				//we can use horizontal separators to split table into table rows
				List<SeparatorRegionType> allSeparators = PageXmlUtils.getSeparators(pcGtsType);
				List<SeparatorRegionType> separatorsH = PageXmlUtils.getHorizontalSeparators_specialCase(pcGtsType, currTable.getBounds().getY(), currTable.getBounds().getMaxY());
				logger.debug("amount of horizontal seps: " + separatorsH.size());
				
				for (SeparatorRegionType sep : separatorsH) {

					logger.debug("center Y: " + sep.getBoundingBox().getCenterY());
//					List<ICanvasShape> canvasShapes = canvas.getScene().getShapes();

					shapeEdit.splitShape(currTable, -1, (int) sep.getBoundingBox().getCenterY(), 1, (int) sep.getBoundingBox().getCenterY(), false, false);
//					try {
//						System.in.read();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}
				
				//remove all separators
				for (ITrpShapeType s : allSeparators) {
					TrpShapeTypeUtils.removeShape(s);
				}
				
				if (separatorsH.size()>0) {
					continue;
				}
			}

			ICanvasShape currTableCopy = currTable.copy();
			//check the column idx;
			
			logger.debug("columnIdxForSplit "+columnIdxForSplit);
			if (amountOfColumns<columnIdxForSplit || columnIdxForSplit<0){
				columnIdxForSplit=0;
			}
			else {
				if (columnIdxForSplit>=1) {
					columnIdxForSplit = columnIdxForSplit-1;
				}
			}
			
			if (currTableCopy.getChild(columnIdxForSplit) == null) {
				logger.debug("no child");
				continue;
			}
			
			ICanvasShape column4Split = currTableCopy.getChild(columnIdxForSplit);
			List<ICanvasShape> lines = column4Split.getChildren(false);
			int nrColumns = currTableCopy.getNChildren();
			
			if (lines.isEmpty()) {
				logger.debug("no lines available fo find splits");
				return 0;
			}
			
			/*
			 * the first column contains the lines which are necessary to compute the rows
			 * so iterate over them and compute line polygon to split table
			 */
			for (int i = 1; i <lines.size(); i++) {
							
				ICanvasShape lineShape = lines.get(i);
				int x = lineShape.getX();
				int height = (int) lineShape.getBounds().getHeight();
				int correctionValue = height/6;
				int minY = lineShape.getY()+correctionValue;
				int x2 = (int) (x + lineShape.getBounds().getWidth());
				int maxY = (int) (lineShape.getBounds().getMaxY())-correctionValue;
				
				CanvasPolyline base = (CanvasPolyline) CanvasShapeUtil.getBaselineShape(lineShape);
				if (base == null) {
					logger.debug("line without baseline");
					continue;
				}
				
				/*
				 * no line should be in the same column, with smaller x and same height
				 * we need the first line in x direction in a column
				 */
				boolean alreadySplitted = false;
				for (int l = 1; l<lines.size(); l++) {
					if (l==i) {
						continue;
					}
					ICanvasShape sibling = lines.get(l);
					int xSib = sibling.getX();
					int minYSib = sibling.getY();
					int maxYSib = (int) sibling.getBounds().getMaxY();
	
	
					if (base.getY() > minYSib && base.getY() < maxYSib && xSib < x) {
						logger.debug(i + "-th row is already done - two lines in the same column with same height!");
						logger.debug("base y" + base.getY());
						logger.debug("sibling y" + minYSib);
						logger.debug("max sibling y" + maxYSib);
						logger.debug("base x" + x);
						logger.debug("sibling x" + xSib);
						alreadySplitted = true;
					}
	
				}
				
				if (alreadySplitted || lineShape.getBounds().getWidth() < (column4Split.getBounds().getWidth() * lineWidth)) {
	
					continue;
				}
	
				int startY = minY-5;
	
				/*
				 * get all lines which have the baseline in the 'scope' of the line from the first column
				 * start in the first column, omit the column which occurs on position=columnIdxForSplit 
				 */
				
				List<Point> pts = new ArrayList<Point>();
							
				for (int j = 0; j<nrColumns; j++) {
					
					//add the point for this column at the correct position
					if (j==columnIdxForSplit) {
						pts.add(new Point(x, startY));
						pts.add(new Point(x2, startY));
						continue;
					}
	
					List<ICanvasShape> furtherLines = currTableCopy.getChild(j).getChildren(false);
					for (int m = 0; m<furtherLines.size(); m++) { 
						ICanvasShape nextLine = furtherLines.get(m);
						CanvasPolyline bl = (CanvasPolyline) CanvasShapeUtil.getBaselineShape(nextLine);
						if (bl == null) {
							continue;
						}
						int blY = bl.getY();
						
						if (blY > minY && blY < maxY) {
							
							java.awt.Rectangle rect = nextLine.getBounds();
							
							//logger.debug(k + " (k-th line) point found" + (nextLine.getY()+10));
							pts.add(new Point((nextLine.getX()), (nextLine.getY()-5)));
							pts.add(new Point((nextLine.getX()+rect.width), (rect.y-5)));
							break;
						}
					}
				}
				CanvasPolyline pl4split = new CanvasPolyline(pts);
				List<ShapePoint> pl4splitShapes = currTableCopy.intersectionPoints(pl4split, true);
				
				if (pl4splitShapes.size()==2) {
					logger.debug("2 points added" + (pl4splitShapes.get(0).getP().getY()));
					pts.add(0, new Point(pl4splitShapes.get(0).getP()));
					pts.add(pts.size(), new Point(pl4splitShapes.get(1).getP()));
				}
				
				CanvasPolyline pl4splitExtended = new CanvasPolyline(pts);
				
	//			for (Point p : pl4splitExtended.getPoints()) {
	//				logger.debug("point.X in polyline" + p.getX());
	//				logger.debug("point.Y in polyline" + p.getY());
	//			}
				
				logger.debug("split with this polyline: " + pl4splitExtended.getPoints());
				
				//shapeEdit.splitShape(currTable, -1, startY, 1, startY, false);
				List<ShapeEditOperation> shapeEditOp = shapeEdit.splitShape(currTable, pl4splitExtended, false, false);
				if (shapeEditOp == null) {
					//try horizontal split
					logger.debug("split horizontal-§§§>§§§§§§§§§§§§>");
					shapeEdit.splitShape(currTable, -1, startY, 1, startY, false, false);
				}			
			
				
	//			TrpShapeElementFactory.syncCanvasShapeAndTrpShape(currTable, r);
	//			
	//			for (int a = 0; a < shapeEditOp.size(); a++) {
	//				ShapeEditOperation seo = shapeEditOp.get(a);
	//				List<ICanvasShape> b = seo.getBackupShapes();
	//				for (int c = 0; c<b.size();c++) {
	//					ICanvasShape currNewShape = b.get(c);
	//					logger.debug("amount of childs: " + currNewShape.getNChildren());
	//				}
	//				
	//			}
				
				//theTable.setData(currTable);
	
				
			}
			
		}
				
		try {
			//Storage.getInstance().saveTranscript(Storage.getInstance().getCollId(), "table rows created");
//			mw.reloadCurrentPage(true, null, null);
			
//		} catch (SessionExpiredException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (ServerErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			//mw.reloadCurrentPage(true, null, null);
			canvas.setVisible(true);
		}
		
		if (currTable != null && currTable.getData() != null) {
			TrpTableRegionType trt = (TrpTableRegionType) currTable.getData();
			return trt.getNRows();
		}
		
		return 0;	
		
	}
	
	public void removeRegions(){
		
		logger.debug("remove regions into columns");
			TrpTranscriptMetadata md = storage.getTranscriptMetadata();
			JAXBPageTranscript tr = new JAXBPageTranscript(md);
			try {
				tr.build();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			

			
			List<TrpRegionType> regions = tr.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
			TrpShapeElementFactory shapeFactory = new TrpShapeElementFactory(mw);
			
			logger.debug("amount of regions for split regions into columns: " + regions.size());

			ICanvasShape currRegion = null;
			CanvasShapeEditor shapeEdit = new CanvasShapeEditor(mw.getCanvas());
			
			List<ICanvasShape> regions2remove = new ArrayList<ICanvasShape>();
			canvas.getShapeEditor().removeShapesFromCanvas(regions2remove, false);
			
			for (int k = 0; k < regions.size(); k++) {
				TrpRegionType r = regions.get(k);
				if (r instanceof TrpTextRegionType) {
					
					logger.debug("region for split found: " + r.getId());
					TrpTextRegionType currTextRegion = (TrpTextRegionType) r;
					shapeFactory.addAllCanvasShapes(currTextRegion);
					
					currRegion = (ICanvasShape) currTextRegion.getData();
					currRegion.setData(currTextRegion);
					
					logger.debug("page " + storage.getTranscript().getPage());
					
					regions2remove.add(currRegion);
					
					//tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion().remove(currTextRegion);

//					canvas.getScene().updateAllShapesParentInfo();
//					ICanvasShape cs = (ICanvasShape) currTextRegion.getData();
//					logger.debug("bounds of shape: " + cs.getBounds());
//					canvas.getShapeEditor().removeShapeFromCanvas(cs, false);
//					canvas.getScene().updateAllShapesParentInfo();
//					canvas.redraw();
				}
				else {
					logger.debug("no text-region for split found: " + r.getId());
				}
			}
			try {
				
				//logger.debug("removed shape??: " + TrpShapeTypeUtils.removeShapes(regions2remove));
				mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, "test");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mw.reloadCurrentPage(true, null, null);
		}
	
public void createRegionsFromLinesInColumns(JAXBPageTranscript tr){
		
		canvas.setVisible(false);
		
		List<TrpRegionType> regions = tr.getPageData().getPage() .getTextRegionOrImageRegionOrLineDrawingRegion();
		TrpShapeElementFactory shapeFactory = new TrpShapeElementFactory(mw);
		
		logger.debug("amount of regions for split regions into columns: " + regions.size());

		List<SeparatorRegionType> separatorsH = PageXmlUtils.getHorizontalSeparators_specialCase(tr.getPageData(), 0, tr.getPageData().getPage().getImageHeight());
		List<ICanvasShape> regions2remove = new ArrayList<ICanvasShape>();
		List<ICanvasShape> regions2add = new ArrayList<ICanvasShape>();
		
		Map<ICanvasShape, List<ITrpShapeType>> regionMap = new HashMap<>();
		int upperBorder = 0;
		
		for (int k = 0; k < regions.size(); k++) {
			TrpRegionType r = regions.get(k);
			
			List<ITrpShapeType> collectedLines = new ArrayList<ITrpShapeType>();
			if (r instanceof TrpTextRegionType) {
				
				logger.debug("region for split found: " + r.getId());
				TrpTextRegionType currTextRegion = (TrpTextRegionType) r;
				shapeFactory.addAllCanvasShapes(currTextRegion);
				List<ITrpShapeType> lines = currTextRegion.getChildren(false);

				if (lines.isEmpty()) {
					logger.debug("no lines available fo find splits");
					regions2remove.add((ICanvasShape) currTextRegion.getData());
					continue;
				}
				
				if (lines.size()<3) {
					regions2remove.add((ICanvasShape) currTextRegion.getData());
					continue;
				}
			
				int prevX = 0;
				int prevMaxX = 0;
				int prevY = 0;
				
				if (upperBorder==0) {
					for (SeparatorRegionType sep : separatorsH) {
						if (currTextRegion.getBoundingBox().getMinY()<sep.getBoundingBox().getCenterY() && sep.getBoundingBox().getCenterY()<(tr.getPageData().getPage().getImageHeight()/3)) {
							upperBorder = (int) sep.getBoundingBox().getCenterY();
							break;
						}
					}
				}
				
				//region above the upper border of the table can be deleted
				if (upperBorder>0 && currTextRegion.getBoundingBox().getMaxY()<upperBorder) {
					regions2remove.add((ICanvasShape) currTextRegion.getData());
				}
				
				if (upperBorder>0) {
					ICanvasShape cs = (ICanvasShape) currTextRegion.getData();
					//cs.getBounds().setLocation((int) currTextRegion.getBoundingBox().getMinX(), upperBorder);
					int translateY = upperBorder-cs.getY();
					cs.translate(0,translateY);
					
					List<Point> currPointList = cs.getPoints();
					//if region has more then 4 corners use the bounding rectangle
					if (currPointList.size() != 4) {
						currPointList = cs.getBoundsPoints();
					}
					
					sortPoints(currPointList);
	
					Point leftTop = currPointList.get(0);
					Point leftBottom = currPointList.get(1);
					Point rightTop = currPointList.get(2);
					Point rightBottom = currPointList.get(3);
					
					leftBottom.setLocation(leftBottom.x, leftBottom.y-translateY);
					rightBottom.setLocation(rightBottom.x, rightBottom.y-translateY);
					
					currPointList.remove(3);
					currPointList.remove(2);
					currPointList.remove(1);
					currPointList.remove(0);
					
					currPointList.add(0, leftTop);
					currPointList.add(1, leftBottom);
					currPointList.add(2, rightBottom);
					currPointList.add(3, rightTop);
					
					cs.setPoints(currPointList);
				}
				
				

				if (isSingleColumn(lines)) {
					continue;
				}
				
				/*
				 * the first column contains the lines which are necessary to compute the rows
				 * so iterate over them and compute line polygon to split table
				 */
				for (int i = 0; i <lines.size(); i++) {
								
					TrpBaselineType bl = ((TrpTextLineType) lines.get(i)).getTrpBaseline();
					ICanvasShape blShape = (ICanvasShape) bl.getData();
					
					TrpBaselineType blNext = null;
					ICanvasShape blShapeNext = null;
					if (i+1<lines.size()){
						blNext = ((TrpTextLineType) lines.get(i+1)).getTrpBaseline();
						blShapeNext = (ICanvasShape) blNext.getData();
					}
					
					if (blShape == null) {
						logger.debug("line without baseline");
						continue;
					}
					
					int x = blShape.getX();
					int y = blShape.getY();
					
					if (prevX == 0) {
						prevX=x;
						prevMaxX = (int) blShape.getBounds().getMaxX();
					}
					if (prevY == 0) {
						prevY=y;
					}
					
					if (Math.abs(x-prevX)<=50 && prevY<=y || (x>prevX && blShape.getBounds().getMaxX()<prevMaxX)) {
						logger.debug("line in one rows...." + bl.getId());
						if (!(y<upperBorder)) {
							collectedLines.add(lines.get(i));
						}
						boolean keepOldX = (x>prevX && blShape.getBounds().getMaxX()<prevMaxX);
						if (!keepOldX) {
							prevX = x;
							prevMaxX = (int) blShape.getBounds().getMaxX();
						}
						prevY = y;
					}
					//current line starts above the previous one and the next line as well;
					else if (y<prevY && (blShapeNext != null && blShapeNext.getY()<prevY)) {
						//new column -> create new region with the collected lines
						logger.debug("create new text regions....");
						if (collectedLines.size()>3) {
							ICanvasShape shape2add = createNewTextRegion(currTextRegion, collectedLines);
							if (shape2add != null) {
								for (ITrpShapeType l : collectedLines) {
									ICanvasShape lShape = (ICanvasShape) l.getData();
									lShape.setParent(shape2add);
									shape2add.getChildren(false).add(lShape);
								}
								regions2add.add(shape2add);
								regionMap.put(shape2add, collectedLines);
							}
						}
						
//						logger.debug("removed shape??: " + TrpShapeTypeUtils.removeShape(currTextRegion));
//						canvas.getScene().updateAllShapesParentInfo();
						ICanvasShape cs = (ICanvasShape) currTextRegion.getData();
						regions2remove.add(cs);
//						logger.debug("bounds of shape: " + cs.getBounds());
//						canvas.getShapeEditor().removeShapeFromCanvas(cs, false);
						
						
						//tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion().remove(currTextRegion);
						
						collectedLines.clear();
						if (!(y<upperBorder)) {
							collectedLines.add(lines.get(i));
						}
						prevX = x;
						prevMaxX = (int) blShape.getBounds().getMaxX();
						prevY = y;
						
					}
					else if (Math.abs(x-prevX)>50 && prevY<y) {
						logger.debug("line not in one row....");
						logger.debug("x..."+x);
						logger.debug("prevX..." + prevX);
						logger.debug("y..."+y);
						logger.debug("prevY..." + prevY);
						
						if (!(y<upperBorder)) {
							collectedLines.add(lines.get(i));
						}
						prevX = x;
						prevMaxX = (int) blShape.getBounds().getMaxX();
						prevY = y;
						//next line out of line - omit this and try the next line
						//restart with the current line
//						if (collectedLines.size()<3) {
//							collectedLines.clear();
//							collectedLines.add(lines.get(i));
//							prevX = x;
//							prevMaxX = (int) blShape.getBounds().getMaxX();
//							prevY = y;
//						}
					}
					else {
						prevX = x;
						prevMaxX = (int) blShape.getBounds().getMaxX();
						prevY = y;
					}
					
					if (i+1==lines.size()){
						logger.debug("last line....");
						logger.debug("at the end: create new text regions....");
						ICanvasShape shape2add = createNewTextRegion(currTextRegion, collectedLines);

						if (shape2add != null) {
							for (ITrpShapeType l : collectedLines) {
								ICanvasShape lShape = (ICanvasShape) l.getData();
								lShape.setParent(shape2add);
								shape2add.getChildren(false).add(lShape);
							}
							regions2add.add(shape2add);
							regionMap.put(shape2add, collectedLines);
						}
						collectedLines.clear();
						ICanvasShape cs = (ICanvasShape) currTextRegion.getData();
						regions2remove.add(cs);

						
					}

				}

			}
								
			try {
				//Storage.getInstance().saveTranscript(Storage.getInstance().getCollId(), "table rows created");
	//			mw.reloadCurrentPage(true, null, null);
				
	//		} catch (SessionExpiredException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
			} catch (ServerErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				//mw.reloadCurrentPage(true, null, null);
				canvas.setVisible(true);
			}

		}

		logger.debug("page " + storage.getTranscript().getPage());
		canvas.getShapeEditor().removeShapesFromCanvas(regions2remove, false);
		tr.getPage().sortRegions();
		try {
			storage.reloadTranscript();
		} catch (JAXBException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        // Iterating HashMap through for loopMap<ICanvasShape, List<ITrpShapeType>> regionMap
        for (Map.Entry<ICanvasShape, List<ITrpShapeType>> shape : regionMap.entrySet()) {
			logger.debug("neues rectangle.... " + shape);
			TrpPageType parent = tr.getPage();
			
			TrpRegionType neueTr;
			try {
				neueTr = (TrpRegionType) shapeFactory.createTextRegionFromShape(shape.getKey(), parent);
				
				for (ITrpShapeType line : shape.getValue()) {
					line.setParent(neueTr);
					canvas.getScene().addShape((ICanvasShape)line.getData(), (ICanvasShape)neueTr.getData(), false);
					shapeFactory.addCanvasShape(line);
				}
				
				TrpMainWidget.getInstance().getScene().updateAllShapesParentInfo();

				//TrpTextRegionType neueTr = new TrpTextRegionType(tr.getPage());
				//neueTr.setCoordinates(shape., this);
//				shapeFactory.addAllCanvasShapes(neueTr);
//				
//				ICanvasShape newCanvasShape = (ICanvasShape) neueTr.getData();
//				newCanvasShape.setData(neueTr);
//	
//				//shapeFactory.addCanvasShape(neueTr);
//				tr.getPage().getTextRegionOrImageRegionOrLineDrawingRegion().add(neueTr);
				
				mw.reloadCurrentPage(true, null, null);
			} catch (CanvasException | NoParentRegionException | NoParentLineException | BaselineExistsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		canvas.redraw();
		
		//canvas.getScene().updateAllShapesParentInfo();
		try {
			mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, "tried to find columns");
			mw.reloadCurrentPage(true, null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	

	private boolean isSingleColumn(List<ITrpShapeType> lines) {
		//if region has only lines in one row -> keep that region, nothing to do
		int prevX = 0;
		int prevMaxX = 0;
		int prevY = 0;
		
		for (int i = 0; i <lines.size(); i++) {
			
			TrpBaselineType bl = ((TrpTextLineType) lines.get(i)).getTrpBaseline();
			ICanvasShape blShape = (ICanvasShape) bl.getData();
			if (blShape == null) {
				logger.debug("line without baseline");
				continue;
			}
			
			int x = blShape.getX();
			int y = blShape.getY();
			if (prevX == 0) {
				prevX=x;
				prevMaxX = (int) blShape.getBounds().getMaxX();
			}
			if (prevY == 0) {
				prevY=y;
			}
			if (prevY<=y) {
				prevY = y;
				continue;
			}
			else {
				return false;
			}

		}
	return true;
}

	private ICanvasShape createNewTextRegion(TrpRegionType r, List<ITrpShapeType> collectedLines) {
		if (collectedLines.isEmpty() || collectedLines.size()<3) {
			logger.debug("collected lines size: " + collectedLines.size());
			return null;
		}
		ICanvasShape createdShape = null;
		for (ITrpShapeType shape : collectedLines) {
			//get minX, minY, maxX, maxY and create text region with this
			ICanvasShape canvShape = (ICanvasShape) shape.getData();
			if (createdShape == null) {
				createdShape = canvShape;
			}
			else {
				createdShape = createdShape.merge(canvShape);
			}
		}
		
		List<Point2D> newRectStr = PointStrUtils.buildPoints2DList(PointStrUtils.pointsToString(createdShape.getBounds()));
		logger.debug("the new rectangle string: " + newRectStr);
		createdShape.setPoints2D(newRectStr);
		return createdShape;

		
		
	}

	public void splitTableIntoRows() {
		
		TrpTranscriptMetadata md = Storage.getInstance().getTranscriptMetadata();
		
		JAXBPageTranscript tr = new JAXBPageTranscript(md);
		try {
			tr.build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		canvas.getScene().updateAllShapesParentInfo();

		List<ICanvasShape> canvasShapes = canvas.getScene().getShapes();
		ICanvasShape currTable = null;
		
		/*
		 * find the table in the canvas
		 * ToDo: several tables can be on one page
		 * running through all shapes and finding the tables brought 'Concurrent modification exception
		 */
		CanvasShapeEditor shapeEdit = new CanvasShapeEditor(mw.getCanvas());
		for (ICanvasShape table : canvasShapes) {
			if (table.getData() instanceof TrpTableRegionType) {
				if (table.getChildren(false).isEmpty()) {
					continue;
				}
				else {
					currTable = table;
					break;
				}
			}
		}
		
		if (currTable == null) {
			logger.debug("no table available to find rows");
			return;
		}
		
		ICanvasShape currTableCopy = currTable.copy();
		
		int columnIdxForSplit = 0;
		
		List<ICanvasShape> lines = currTableCopy.getChild(columnIdxForSplit).getChildren(false);
		int nrColumns = currTableCopy.getNChildren();
		
		if (lines.isEmpty()) {
			logger.debug("no lines available fo find splits");
			return;
		}
		
		/*
		 * the first column contains the lines which are necessary to compute the rows
		 * so iterate over them and compute line polygon to split table
		 */
		for (int i = 1; i <lines.size(); i++) {
						
			ICanvasShape lineShape = lines.get(i);
			int x = lineShape.getX();
			int height = (int) lineShape.getBounds().getHeight();
			int correctionValue = height/6;
			int minY = lineShape.getY()+correctionValue;
			int x2 = (int) (x + lineShape.getBounds().getWidth());
			int maxY = (int) (lineShape.getBounds().getMaxY())-correctionValue;
			
			CanvasPolyline base = (CanvasPolyline) CanvasShapeUtil.getBaselineShape(lineShape);
			if (base == null) {
				logger.debug("line without baseline");
				continue;
			}
			
			/*
			 * no line should be in the same column, with smaller x and same height
			 * we need the first line in x direction in a column
			 */
			boolean alreadySplitted = false;
			for (int k = 1; k<lines.size(); k++) {
				if (k==i) {
					continue;
				}
				ICanvasShape sibling = lines.get(k);
				int xSib = sibling.getX();
				int minYSib = sibling.getY();
				int maxYSib = (int) sibling.getBounds().getMaxY();


				if (base.getY() > minYSib && base.getY() < maxYSib && xSib < x) {
					logger.debug(i + "-th row is already done - two lines in the same column with same height!");
					logger.debug("base y" + base.getY());
					logger.debug("sibling y" + minYSib);
					logger.debug("max sibling y" + maxYSib);
					logger.debug("base x" + x);
					logger.debug("sibling x" + xSib);
					alreadySplitted = true;
				}

			}
			
			if (alreadySplitted) {

				continue;
			}

			int startY = minY-5;
			
			/*
			 * get all lines which have the baseline in the 'scope' of the line from the first column
			 * start in the first column, omit the column which occurs on position=columnIdxForSplit 
			 */
			
			List<Point> pts = new ArrayList<Point>();
						
			for (int j = 0; j<nrColumns; j++) {
				
				//add the point for this column at the correct position
				if (j==columnIdxForSplit) {
					pts.add(new Point(x, startY));
					pts.add(new Point(x2, startY));
					continue;
				}

				List<ICanvasShape> furtherLines = currTableCopy.getChild(j).getChildren(false);
				for (int k = 0; k<furtherLines.size(); k++) { 
					ICanvasShape nextLine = furtherLines.get(k);
					CanvasPolyline bl = (CanvasPolyline) CanvasShapeUtil.getBaselineShape(nextLine);
					if (bl == null) {
						continue;
					}
					int blY = bl.getY();
					
					if (blY > minY && blY < maxY) {
						
						java.awt.Rectangle rect = nextLine.getBounds();
						
						//logger.debug(k + " (k-th line) point found" + (nextLine.getY()+10));
						pts.add(new Point((nextLine.getX()), (nextLine.getY()-5)));
						pts.add(new Point((nextLine.getX()+rect.width), (rect.y-5)));
						break;
					}
				}
				
			}
			CanvasPolyline pl4split = new CanvasPolyline(pts);
			
			
			List<ShapePoint> pl4splitShapes = currTableCopy.intersectionPoints(pl4split, true);
			
			if (pl4splitShapes.size()==2) {
				logger.debug("2 points added" + (pl4splitShapes.get(0).getP().getY()));
				pts.add(0, new Point(pl4splitShapes.get(0).getP()));
				pts.add(pts.size(), new Point(pl4splitShapes.get(1).getP()));
			}
			
			CanvasPolyline pl4splitExtended = new CanvasPolyline(pts);
			
//			for (Point p : pl4splitExtended.getPoints()) {
//				logger.debug("point.X in polyline" + p.getX());
//				logger.debug("point.Y in polyline" + p.getY());
//			}
			
			logger.debug("split with this polyline: " + pl4splitExtended.getPoints());
			
			//shapeEdit.splitShape(currTable, -1, startY, 1, startY, false);
			if (shapeEdit.splitShape(currTable, pl4splitExtended, false, false) == null) {
				//try horizontal split
				logger.debug("split horizontal-§§§>§§§§§§§§§§§§>");
				shapeEdit.splitShape(currTable, -1, startY, 1, startY, false);
			}
			
			
			canvas.getScene().updateAllShapesParentInfo();
			
		}
				
		try {
			
			Storage.getInstance().saveTranscript(Storage.getInstance().getCollId(), "table rows created");
			mw.reloadCurrentPage(true, null, null);
			
		} catch (SessionExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServerErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		canvas.getScene().updateAllShapesParentInfo();		
		
	}


	private int fromTableToRegions(TrpRegionType region, int nrOfTable) {
		
		TrpTextRegionType tr = null;
		int createdRegions = 0;
			
		if (region instanceof TableRegionType) {

			logger.debug("converting from table to text-regions...");
			TableRegionType table = (TableRegionType) region;
			
			TrpShapeTypeUtils.removeShape(table);
			
			for (TableCellType cell : table.getTableCell()) {
				//logger.debug("cell structure type: " + cell.getCustomTagList().getNonIndexedTag("structure").getAttributeValue("type"));
				logger.debug("new type value would be_: " +  "T"+nrOfTable+"C"+cell.getCol());
				
				try {
					CustomTag ct = cell.getCustomTagList().getNonIndexedTag("structure");
					if (ct==null) {
						ct = CustomTagFactory.create("structure");
					}
					ct.setAttribute("type", "T"+nrOfTable+"C"+cell.getCol(), true);
					cell.getCustomTagList().addOrMergeTag(ct, null);
					
					logger.debug("cell structure type after new set: " + cell.getCustomTagList().getNonIndexedTag("structure").getAttributeValue("type"));
					
					tr = new TrpTextRegionType(cell);
					
					tr.getCustomTagList().addOrMergeTag(ct, "structure");
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				cell.getPage().getTextRegionOrImageRegionOrLineDrawingRegion().add(tr);
				
				ICanvasShape cellShape = (ICanvasShape) tr.getData();
				if (cellShape != null) {
					cellShape.setData(tr);
				}
				
				canvas.getScene().updateAllShapesParentInfo();

			}
			
			table.getPage().sortRegions();
			createdRegions += table.getTableCell().size();
		}
			
		return createdRegions;

	}
	
	private int fromRectangleRegionsToTable(JAXBPageTranscript tr, boolean keepBorders) {
		return fromRectangleRegionsToTable(tr, keepBorders, false);
	}
	
	
	/*
	 * this method works with several tables too
	 * distinction between different tables is made with the custum structure type 'T0C0', 'T0C1', T0C2,... T1C0, T1C1
	 * with T0 as first table and T1 as second table
	 * so we read the structure type in each region to see if a new table needs to be created
	 * param2: if true the regions become table cells as they are, if false the right border of each column gets adapted (e.g. to the left border of the next column)
	 */
	private int fromRectangleRegionsToTable(JAXBPageTranscript tr, boolean keepBorders, boolean deleteSeparatorsAfterOp) {
		
		boolean firstRegion = true;
		int regionNr = 0;
		int tables = 0;
		
		TableCellType tct = null;
		TableRegionType table = null;	
		TrpPageType page = null;
		
		TrpShapeElementFactory shapeFactory = new TrpShapeElementFactory(mw);
		
		//List<TrpRegionType> tmpList = new ArrayList<TrpRegionType>(tr.getPageData().getPage().getTextRegionOrImageRegionOrLineDrawingRegion());
		List<SeparatorRegionType> separatorsV = PageXmlUtils.getAllVerticalSeparators(tr.getPageData());
		
		List<SeparatorRegionType> allSeparators = PageXmlUtils.getSeparators(tr.getPageData());
		List<TextRegionType> textRegionsStart = PageXmlUtils.getTextRegionsWithoutTableCells(tr.getPageData());

		if (textRegionsStart.isEmpty()) {
			logger.debug("no text regions available to create a table");
			return tables;
		}
		
		List<TextRegionType> textRegions = mergeRegionsWithSameStructure(textRegionsStart);
		for (int j = 0; j<textRegionsStart.size(); j++) {
			TrpShapeTypeUtils.removeShape(textRegionsStart.get(j));
		}		
		
		Point foundBorderTop = null;
		Point foundBorderBottom = null;
		
		//go over all regions resp. text regions
		for (int j = 0; j<textRegions.size(); j++) {
			boolean lastRegionOfThisTable = false;
			
			TrpRegionType region = textRegions.get(j);
			TrpRegionType furtherRegion = null;
			if (j+1<textRegions.size()) {
				furtherRegion = textRegions.get(j+1);
				if (isTableEnd(region.getCustom(), furtherRegion.getCustom())) {
					lastRegionOfThisTable = true;
				}
			}
			
			if  ((j+1)==textRegions.size()) {
				lastRegionOfThisTable = true;
			}

			page = region.getPage();
			int imgHeight = page.getImageHeight();
			/*
			 * ignore regions which are very small
			 */
			if (region.getBoundingBox().height < imgHeight/5) {
				continue;
			}
			
			String structString = region.getStructure();
			String structID = structString.substring(structString.length()-1);
			logger.debug("struct ID" + structID);
			//structID should be 
//			if (region.getStructure().substring(.length()-1)) {
//				continue;
//			}

			logger.debug("converting from text-regions to table...");
			if (region instanceof TrpTextRegionType){
				
				List<ITrpShapeType> k = region.getChildren(false);
				logger.debug("number of childrens: " + k.size());

				shapeFactory.addCanvasShape(region);
				
				TrpTextRegionType currRegion = (TrpTextRegionType) region;
				ICanvasShape regionShape = (ICanvasShape) currRegion.getData();
				
				List<Point> currPointList = regionShape.getPoints();
				//if region has more then 4 corners use the bounding rectangle
				if (currPointList.size() != 4) {
					currPointList = regionShape.getBoundsPoints();
				}
				
				sortPoints(currPointList);

				Point leftTop = currPointList.get(0);
				Point leftBottom = currPointList.get(1);
				Point rightTop = currPointList.get(2);
				Point rightBottom = currPointList.get(3);
				
				//new border was found in previous step -> separator in between two rectangles was found
				if (foundBorderTop != null && foundBorderBottom != null && !keepBorders) {
					leftTop = foundBorderTop;
					leftBottom = foundBorderBottom;
				}
				
				if (furtherRegion != null && furtherRegion instanceof TrpTextRegionType && !keepBorders) {
					shapeFactory.addCanvasShape(furtherRegion);
					
					TrpTextRegionType nextRegion = (TrpTextRegionType) furtherRegion;
					ICanvasShape nextRegionShape = (ICanvasShape) nextRegion.getData();
					
					List<Point> nextPointList = nextRegionShape.getPoints();
					if (nextPointList.size() != 4) {
						nextPointList = nextRegionShape.getBoundsPoints();
					}
					sortPoints(nextPointList);
					
					/*
					 * here we take the leftmost points from the neighbor rectangle as border between the two rectangle
					 * if a separator border was found this gets overwritten
					 */
					
					Point rightTopAlt = nextPointList.get(0);
					Point rightBottomAlt = nextPointList.get(1);
					
					/*
					 * use separators to set the borders precisely
					 */
					int xSep = getBorderFromSeparator(separatorsV, regionShape, nextRegionShape);
					if (xSep != 0) {
						logger.debug("region id " + region.getId());
						logger.debug("x separator != 0" + xSep);
						rightTopAlt.setLocation(xSep, rightTopAlt.y);
						rightBottomAlt.setLocation(xSep, rightBottomAlt.y);
						
						foundBorderTop = rightTopAlt;
						foundBorderBottom = rightBottomAlt;
					}
					//no suitable separator found or no separator at all -> take the left coordinates of the further region
					else {	
						logger.debug("x separator == 0 " + xSep);
						foundBorderTop = null;
						foundBorderBottom = null;
					}
					
					//if height of left and right border differs extremely we can change this
					if (Math.abs(leftTop.y-rightTopAlt.y)>100) {
						int correctionY = Math.min(rightTopAlt.y, leftTop.y);
						leftTop.setLocation(leftTop.x, correctionY);
						rightTopAlt.setLocation(rightTopAlt.x, correctionY);
					}
					
					//if height of left and right border differs extremely we can change this
					if (Math.abs(leftBottom.y-rightBottomAlt.y)>100) {
						int correctionY = Math.max(rightBottomAlt.y, leftBottom.y);
						leftBottom.setLocation(leftBottom.x, correctionY);
						rightBottomAlt.setLocation(rightBottomAlt.x, correctionY);
					}

					/*
					 * reordering of these point is necessary so that the table cell works correct
					 * ordering of points from P2PaLA is different
					 */
					currPointList.remove(3);
					currPointList.remove(2);
					currPointList.remove(1);
					currPointList.remove(0);
					
					currPointList.add(0, leftTop);
					currPointList.add(1, leftBottom);
					currPointList.add(2, rightBottomAlt);
					currPointList.add(3, rightTopAlt);

				}
				/*
				 * re-arrange the points
				 */
				else {
					currPointList.remove(3);
					currPointList.remove(2);
					currPointList.remove(1);
					currPointList.remove(0);
					
					currPointList.add(0, leftTop);
					currPointList.add(1, leftBottom);
					currPointList.add(2, rightBottom);
					currPointList.add(3, rightTop);
				}
				
				String coords = PointStrUtils.pointsToString(currPointList);
				
				ICanvasShape copiedRegionShape = regionShape.copy();
				copiedRegionShape.setPoints(currPointList);
				
				int minY = (int) currRegion.getBoundingBox().getMinY();

				logger.debug("create table cell...");
				tct = new TableCellType();
				tct.setId("cell"+ ++regionNr);
				tct.setCornerPts("0 1 2 3");
				logger.debug("table coords " + coords);
				tct.setCoordinates(coords, null);
				tct.setCol(currRegion.getReadingOrderAsInt());
				tct.setRow(0);
				tct.setRowSpan(1);
				tct.setColSpan(1);
				tct.setReadingOrder(currRegion.getReadingOrder(), null);
				tct.setCustom(currRegion.getCustom());
				List<TextLineType> l = ((TextRegionType) tct).getTextLine();

				for (int m = 0; m<k.size(); m++) {
					TrpTextLineType currLine = (TrpTextLineType) k.get(m);
					
					//yCoord = currLine.getBoundingBox).getMinY();
					
					l.add(currLine);
				}			
				
				logger.debug("number of children in table cell: " + ((TextRegionType) tct).getTextLine().size());

				//table.setCoordinates(currRegion.getCoordinates(), null);
				
//				String coords = table.getCoordinates();
				if (firstRegion) {
					table = new TableRegionType();
					String type = getTableId(currRegion.getCustom());
					String tableId = "1";
					if (type.startsWith("T")) {
						//table ID created from structure type of cell T0C0 -> first colum from first table
						tableId = type;
					}
					table.setId("table_"+tableId);
					firstRegion = false;
					logger.debug("firstRegion - create table " +table.getId());
					table.setData(regionShape);
					table.setCoordinates(coords, null);
				}
				else {
					ICanvasShape mergedShape = ((ICanvasShape) table.getData()).merge(copiedRegionShape);
					String coords2 = PointStrUtils.pointsToString(mergedShape.getPoints());
					logger.debug("coords of mergde shape == " + coords2);
					table.setCoordinates(coords2, null);
					table.setData(mergedShape);
					if (lastRegionOfThisTable) {
						firstRegion = true;
					}
				}
				
				if (tct != null) {
					tct.setData(regionShape);
				}
				
				table.getTableCell().add(tct);
				
				TrpShapeTypeUtils.removeShape(region);

				/*
				 * if the last region is reached we store the table
				 */
				if (lastRegionOfThisTable) {
					tables++;					
					page.getTextRegionOrImageRegionOrLineDrawingRegion().add(table);

				}
				
			}
			//mw.reloadCurrentPage(true, null, null);
//			splitTableWithSeparators(page);
//			try {
//				mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, "regions converted to table");
//				//Storage.getInstance().saveTranscript(Storage.getInstance().getCollId(), "regions converted to table");
//
//			} catch (SessionExpiredException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ServerErrorException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		
		//remove all separators
		if (deleteSeparatorsAfterOp) {
			for (ITrpShapeType s : allSeparators) {
				TrpShapeTypeUtils.removeShape(s);
			}
		}
		
		return tables;
	}
	
	private List<TextRegionType> mergeRegionsWithSameStructure(List<TextRegionType> textRegionsStart) {
		//go over all regions resp. text regions
		List<TextRegionType> merged = new ArrayList<TextRegionType>();
		TextRegionType prev = null;
		for (int j = 0; j<textRegionsStart.size(); j++) {
			TextRegionType curr = textRegionsStart.get(j);
			if (prev == null) {
				prev = curr;
				continue;
			}
			else if (curr.getStructure().equals(prev.getStructure())) {
				prev.setCoordinates(prev.getCoordinates().concat(" ")+curr.getCoordinates(), "merged");
				java.awt.Rectangle rect = PointStrUtils.getBoundingBox(prev.getCoordinates());
				String simpleCoords = PageXmlUtils.getCoords(rect);
				prev.setCoordinates(simpleCoords, "");

			}
			else {
				merged.add(prev);
				prev = curr;
			}
			
			if (j==(textRegionsStart.size()-1)) {
				merged.add(curr);
			}
			

		}
		return merged;
	}

	public void splitTableWithSeparators() {
		
		logger.debug("split table found");
		try {
			storage.reloadDocWithAllTranscripts();
		} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TrpTranscriptMetadata md = storage.getDoc().getPages().get(0).getCurrentTranscript();
		
		
		JAXBPageTranscript tr = new JAXBPageTranscript(md);
		try {
			tr.build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		PcGtsType pcgts = tr.getPageData();
		List<TrpRegionType> regions = pcgts.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
		
		logger.debug("amount of regions: " + regions.size());

		//canvas.getScene().updateAllShapesParentInfo();
		TrpShapeElementFactory shapeFactory = new TrpShapeElementFactory(mw);

//		List<ICanvasShape> canvasShapes = canvas.getScene().getShapes();
		ICanvasShape currTable = null;
		CanvasShapeEditor shapeEdit = new CanvasShapeEditor(mw.getCanvas());
		for (int k = 0; k < regions.size(); k++) {
			TrpRegionType r = regions.get(k);

			if (r instanceof TableRegionType) {
				if (r instanceof TableRegionType) {
					TableRegionType currTableRegion = (TableRegionType) r;
					logger.debug("table found");
					shapeFactory.addAllCanvasShapes(currTableRegion);
					//mw.reloadCurrentPage(true, null, null);
					if (((TableRegionType) r).getTableCell().isEmpty()) {
						logger.debug("no children");
						continue;
					}
					else {
						logger.debug("get Data");
						currTable = (ICanvasShape) currTableRegion.getData();
						currTable.setData(currTableRegion);
					}
				}
				else {
					continue;
				}

				//mw.reloadCurrentPage(true, null, null);
				
				//we can use horizontal separators to split table into table rows
				List<SeparatorRegionType> separatorsH = PageXmlUtils.getHorizontalSeparators_specialCase(pcgts, currTable.getBounds().getY(), currTable.getBounds().getMaxY());
				logger.debug("amount of horizontal seps: " + separatorsH.size());
				
				for (SeparatorRegionType sep : separatorsH) {

					logger.debug("center Y: " + sep.getBoundingBox().getCenterY());
//					List<ICanvasShape> canvasShapes = canvas.getScene().getShapes();

					shapeEdit.splitShape(currTable, -1, (int) sep.getBoundingBox().getCenterY(), 1, (int) sep.getBoundingBox().getCenterY(), false, false);
					try {
						System.in.read();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	private String getTableId(String custom) {
		
		int typeIdx = custom.indexOf("type:");
		logger.debug("typeIdx:: " + typeIdx);
		String type = custom.substring(typeIdx+5, typeIdx+7);
		logger.debug("custom:: " + custom);
		logger.debug("custom type:: " + type);
		return type;
		
	}
	
	private boolean isTableEnd(String custom1, String custom2) {
		
		String type1 = getTableId(custom1);
		String type2 = getTableId(custom2);

		return (type1.startsWith("T") && !type1.equals(type2));
		
	}

	private void sortPoints(List<Point> currPointList) {

		currPointList.sort(new Comparator<Point>() {
			@Override
			public int compare(Point o1, Point o2) {
				// TODO Auto-generated method stub
				int compareX = Double.compare(o1.getX(), o2.getX());
				if (compareX == 0) {
					return Double.compare(o1.getY(), o2.getY());
				}
				else {
					return compareX;
				}
			}
		});
		
	}

	/*
	 * find separator between the two shapes (should have a proper length) and return the x coordinate
	 */
	private int getBorderFromSeparator(List<SeparatorRegionType> separators, ICanvasShape regionShape,
			ICanvasShape nextRegionShape) {
		
		int meanX = 0;
		ITrpShapeType s = null;
		
		int height = (int) regionShape.getBounds().getHeight();
		for (int i = 0; i<separators.size(); i++){
			SeparatorRegionType currSep = separators.get(i);
			if (currSep.getBoundingBox().getHeight()>(height/5)) {
				if (currSep.getBoundingBox().getMinX() > regionShape.getX() && currSep.getBoundingBox().getMinX() < (nextRegionShape.getX()+(nextRegionShape.getBounds().getWidth()/2))){
					int currMeanX = (int) (currSep.getBoundingBox().getMinX() + currSep.getBoundingBox().getMaxX())/2;
					if (meanX == 0 || Math.abs(nextRegionShape.getX()-currMeanX) < Math.abs(nextRegionShape.getX()-meanX)) {
						meanX = currMeanX;
						s = currSep;
					}
				}
			}
		}
		
		if (s != null) {
			TrpShapeTypeUtils.removeShape(s);
			separators.remove(s);
		}
		
		return meanX;
	}

	
	public void mergeSmallTextLinesFromLoadedDoc() {
		try {
			logger.debug("mergeSmallTextLinesFromLoadedDoc!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}
			
			MergeTextLinesConfDialog d = new MergeTextLinesConfDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			double horizontalThreshold = d.getThreshPixel();
			Set<Integer> pageIndices = d.getPageIndices();
			boolean dryRun = d.isDryRun();
			final double threshold = horizontalThreshold;
			
			class Result {
				public int nPagesTotal=0;
				public int nLinesTotal=0;
				public int nPagesChanged=0;
				public int nShapesMerged=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("mergeSmallTextLinesFromLoadedDoc");
						TrpDoc doc = storage.getDoc();
						int worked=0;				
						
						int N;
						if (d.isApplySelected() && d.isDoCurrentPage()) {						
							pageIndices.clear();
							pageIndices.add(storage.getPageIndex());
							N=1;
						}				
						else {
							N = pageIndices==null ? doc.getNPages() : pageIndices.size();
						}

						res.nPagesTotal = N;
						res.nLinesTotal = 0;
						
						MonitorUtil.beginTask(monitor, "Removing small text lines, threshold = "+threshold, N);

						for (int i=0; i<doc.getNPages(); ++i) {
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
						
							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();
							
							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();
							
							List<TrpRegionType> trpShapes = null;
							if (d.isApplySelected() && d.isDoCurrentPage()) {
								//neu: process only selected shapes 
								List<ICanvasShape> shapes = mw.getCanvas().getScene().getSelected();
								if (shapes.isEmpty()) {
									shapes = mw.getCanvas().getScene().getShapes();
								}
								
								trpShapes = new ArrayList<TrpRegionType>();
								for (ICanvasShape shape : shapes) {
									
									ITrpShapeType type = shape.getTrpShapeType();
									//if (!(type instanceof TrpTextLineType) && !(type instanceof TrpWordType)) {
									if (type instanceof TrpTextRegionType || type instanceof TableCellType || type instanceof TableRegionType) {
										//logger.debug("instance of trpregiontype");
										trpShapes.add((TrpRegionType)type);
									}
								}
							}
							
							ImmutablePair<Integer, Integer> stats = PageXmlUtils.mergeSmallLines(tr.getPageData(), threshold, trpShapes);
							
							res.nShapesMerged += stats.getRight();
							res.nLinesTotal += stats.getLeft();
							logger.debug("nMerged = "+stats.getRight());
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (stats.getRight() > 0) {
								++res.nPagesChanged;
								String msg = "After merge "+stats.getRight()+" lines are left";
								
								res.affectedPageIndices.add(i);
								
								if (!dryRun) {
									mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, msg);									
								}
							}
							
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "After merging "+res.nShapesMerged+" lines remain/from total lines "+ res.nLinesTotal + " -> pages changed/pages total" +res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Merging small lines", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Merging lines", res.msg+"\nChosen page numbers: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));
			
			if (!dryRun) {
				mw.reloadCurrentPage(true, null, null);	
			}
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}		
	}
	
	public void removeSmallTextRegionsFromLoadedDoc() {
		try {
			logger.debug("removeSmallTextRegionsFromLoadedDoc!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			
			if (!mw.saveTranscriptDialogOrAutosave()) {
				return;
			}
			
			RemoveTextRegionsConfDialog d = new RemoveTextRegionsConfDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			double fractionOfImageSize = d.getThreshPerc();
			Set<Integer> pageIndices = d.getPageIndices();
			boolean dryRun = d.isDryRun();

			Rectangle imgBounds = canvas.getScene().getMainImage().getBounds();
			double area = imgBounds.width * imgBounds.height;
			
//			if (fractionOfImageSize == null) {
//				fractionOfImageSize = DialogUtil.showDoubleInputDialog(getShell(), "Input fraction", "Fraction of area ("+area+")", 0.0005);
//			}
//			logger.debug("fractionOfImageSize = "+fractionOfImageSize);
//			if (fractionOfImageSize == null) {
//				return;
//			}
			final double threshold = fractionOfImageSize*area;
			
			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nRegionsRemoved=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("removeSmallTextRegionsFromLoadedDoc");
						TrpDoc doc = storage.getDoc();
						int worked=0;
						int N = pageIndices==null ? doc.getNPages() : pageIndices.size();
						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Removing text regions, threshold = "+threshold, N);
						for (int i=0; i<doc.getNPages(); ++i) {
							if (pageIndices!=null && !pageIndices.contains(i)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
							
							TrpPage p = doc.getPages().get(i);
							TrpTranscriptMetadata md = p.getCurrentTranscript();
							
							JAXBPageTranscript tr = new JAXBPageTranscript(md);
							tr.build();

							int nRemovedEmpty = 0;
							int nRemovedSmall = 0;
							if (d.isRemoveEmpty()) {
								nRemovedEmpty += PageXmlUtils.filterOutEmptyRegions(tr.getPageData());
							}
							
							if (d.isRemoveSmall()) {
								nRemovedSmall += PageXmlUtils.filterOutSmallRegions(tr.getPageData(), threshold);
							}
							 
							res.nRegionsRemoved += nRemovedEmpty;
							res.nRegionsRemoved += nRemovedSmall;
							logger.debug("nRemoved = "+res.nRegionsRemoved);
//							PageXmlUtils.filterOutSmallRegions(md.getUrl().toString(), threshold);
							if (nRemovedEmpty > 0 || nRemovedSmall > 0) {
								++res.nPagesChanged;
								String msg = "Removed "+ nRemovedEmpty + " empty & " + nRemovedSmall +" small text-regions";
								
								res.affectedPageIndices.add(i);
								
								if (!dryRun) {
									TranskribusMetadataType metadataInPageXml = tr.getPageData().getMetadata().getTranskribusMetadata();
									PageXmlUtils.checkAndSyncPageXmlMetadataAndTrpTranscriptMetadata(metadataInPageXml, md);
									tr.getPageData().getMetadata().setTranskribusMetadata(metadataInPageXml);
									mw.getStorage().getConnection().updateTranscript(mw.getStorage().getCurrentDocumentCollectionId(), doc.getId(), i+1, md.getStatus(), tr.getPage().getPcGtsType(), md.getTsId(), "Remove regions tool", msg);
									//mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), tr, msg);									
								}
							}
							
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "Removed "+res.nRegionsRemoved+" text-regions from "+res.nPagesChanged+"/"+res.nPagesTotal+" pages";
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Removing text-regions", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Removed text-regions", res.msg+"\nAffected pages: "+CoreUtils.getRangeListStrFromList(res.affectedPageIndices));
			
			if (!dryRun) {
				mw.reloadCurrentPage(true, null, null);	
			}
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}		
	}
	
	public void copyShapesToOtherPagesInLoadedDoc() {
		try {
			logger.debug("copyShapesToOtherPagesInLoadedDoc!");

			if (!storage.isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return;
			}
			CopyShapesConfDialog d = new CopyShapesConfDialog(getShell());
			if (d.open() != IDialogConstants.OK_ID) {
				return;
			}
			
			Set<Integer> pageIndices = d.getPageIndices();
			boolean dryRun = d.isDryRun();
			
			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public int nShapesCopied=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("copyShapesToOtherPagesInLoadedDoc");
						TrpDoc doc = storage.getDoc();
						int worked=0;
						int N = pageIndices==null ? doc.getNPages() : pageIndices.size();
						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, "Copy selected shapes to nr. of pages = ", N);
						List<ICanvasShape> shapes = mw.getCanvas().getScene().getSelected();
						if (shapes.isEmpty()) {
							shapes = mw.getCanvas().getScene().getShapes();
						}
						
						List<TrpRegionType> trpShapes = new ArrayList<TrpRegionType>();
						for (ICanvasShape shape : shapes) {
							
							ITrpShapeType type = shape.getTrpShapeType();
							//if (!(type instanceof TrpTextLineType) && !(type instanceof TrpWordType)) {
							if (type instanceof TrpRegionType && !(type instanceof TableCellType)) {
								logger.debug("instance of trpregiontype");
								trpShapes.add((TrpRegionType)type);
							}
							else if (type instanceof TrpTableRegionType) {
								logger.debug("instance of trpTableRegiontype");
							}

						}
						
						res.nShapesCopied = trpShapes.size();
						
						//the current page from where we want to copy
						int idx = storage.getPageIndex();
						
						TrpPage p = doc.getPages().get(idx);
						TrpTranscriptMetadata md = p.getCurrentTranscript();
						
						JAXBPageTranscript tr = new JAXBPageTranscript(md);
						
						for (int i=0; i<doc.getNPages(); ++i) {

							if (pageIndices!=null && !pageIndices.contains(i) || i==idx) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
							
							TrpPage currP = doc.getPages().get(i);
							TrpTranscriptMetadata currMd = currP.getCurrentTranscript();
							
							JAXBPageTranscript currTr = new JAXBPageTranscript(currMd);
							currTr.build();
							
							logger.debug("currTr: " + currTr.getPage());

							if (!trpShapes.isEmpty()) {
								PcGtsType pc = currTr.getPageData();
								if (pc == null) {
									logger.debug("pc is null");
									continue;
								}
								pc = PageXmlUtils.copyShapes(pc, trpShapes);
								currTr.setPageData(pc);
									
								if (!dryRun) {
									mw.getStorage().saveTranscript(mw.getStorage().getCurrentDocumentCollectionId(), currTr, "copied regions");	
									
								}
								res.nPagesChanged++;
								
							}
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = !dryRun ? "Copied "+res.nShapesCopied+" shape(s) to "+ res.nPagesChanged+" page(s)" : res.nShapesCopied+" shape(s) would be copied to "+ res.nPagesChanged+" page(s)." ;
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Copying shapes", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Copying shapes", res.msg);
			
			if (!dryRun) {
				mw.reloadCurrentPage(true, null, null);	
			}
		} catch (Throwable e) {
			mw.onError("Error", e.getMessage(), e);
		}		
	}
	
	public void splitLinesOnRegionBorders() {
		try {
			logger.debug("split lines on region borders");
			if (!storage.hasTranscript()) {
				return;
			}
			
			JAXBPageTranscript t = mw.getStorage().getTranscript();
			SSW sw = new SSW();
			PageXmlUtils.cutBaselinesToTextRegions(t.getPageData());
			mw.getScene().updateAllShapesParentInfo();
			sw.stop(true, "cutting time: ", logger);
			mw.reloadCurrentTranscript(true, true, null, null);
			
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}
	}

	public void rectifyAllRegions() {
		try {
			if (!storage.hasTranscript()) {
				return;
			}
			
			logger.debug("rectifyAllRegions");
			
			for (ICanvasShape s : mw.getCanvas().getScene().getShapes()) {
				ITrpShapeType st = CanvasShapeUtil.getTrpShapeType(s);
				if (st instanceof RegionType) {
					s.setPoints(s.getBoundsPoints());
				}
			}
			mw.getCanvas().redraw();
			storage.setCurrentTranscriptEdited(true);
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Error", e.getMessage(), e);
		}
	}

	public void extendBaselines(int left, int right, boolean onlySelectedShape) {
		mw.getCanvas().getShapeEditor().extendBaselines(left, right, onlySelectedShape);
	}

}
