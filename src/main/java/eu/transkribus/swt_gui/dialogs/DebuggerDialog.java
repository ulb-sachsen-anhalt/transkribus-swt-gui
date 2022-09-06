package eu.transkribus.swt_gui.dialogs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.catti.CattiRequest;
import eu.transkribus.core.model.beans.JAXBPageTranscript;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.core.util.SebisStopWatch.SSW;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.LabeledText;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.canvas.SWTCanvas;
import eu.transkribus.swt_gui.canvas.shapes.CanvasPolyline;
import eu.transkribus.swt_gui.canvas.shapes.ICanvasShape;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.transcription.ATranscriptionWidget;
import eu.transkribus.swt_gui.transcription.ITranscriptionWidgetListener;
import eu.transkribus.swt_gui.util.ProgramUpdater;
import eu.transkribus.util.IndexTextUtils;
import eu.transkribus.util.MathUtil;

public class DebuggerDialog extends Dialog {
	private final static Logger logger = LoggerFactory.getLogger(DebuggerDialog.class);
	
	Button invalidateSessionBtn;
	
	TrpMainWidget mw = TrpMainWidget.getInstance();
	SWTCanvas canvas = mw.getCanvas();
	
	Storage storage = Storage.getInstance();
	Button listLibsBtn, clearDebugText;
	
	StyledText debugText;
	
	Button sortBaselinePts;
	LabeledText sortXText, sortYText;
	Button sortBaselineAllRegionsBtn;
	Button syncWithLocalDocBtn;
	Button applyAffineTransformBtn;
	Button batchReplaceImgsBtn;
	Button openSleakBtn;
	Button fixExif6Btn;
	Button imageEnhanceDialogBtn;
	Button cropLinesOnRegionBorderBtn;
	
	LabeledText reverseLinesTagsText;
	Button reverseLinesExcludeNumbersCheck, reverseLinesBtn; 
	
	Button lineToWordSegBtn;
	
	ITranscriptionWidgetListener twl;
	
	Button resetIndexCurrentColl, resetIndexCurrentDoc, resetIndexCurrentPage; 
	
	public DebuggerDialog(Shell parent) {
		super(parent);
	}
	
	@Override protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setSize(800, 800);
		SWTUtil.centerShell(shell, false);
		shell.setText("Debugging Dialog");
	}
	
	@Override protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
		setBlockOnOpen(false);
	}
	
	@Override protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, true));
		
		Composite top = new Composite(container, 0);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		top.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		
		
		Composite btns = new Composite(container, 0);
		btns.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		btns.setLayout(new RowLayout(SWT.HORIZONTAL));

		invalidateSessionBtn = new Button(btns, SWT.PUSH);
		invalidateSessionBtn.setText("Invalidate session");
		
		lineToWordSegBtn = new Button(btns, SWT.PUSH);
		lineToWordSegBtn.setText("Line2Word Seg");
		lineToWordSegBtn.setToolTipText("Perform line to word segmentation on current line - WARNING: EXPERIMENTAL!");
		
		syncWithLocalDocBtn = new Button(btns, SWT.PUSH);
		syncWithLocalDocBtn.setText("Sync with local doc");
		
		applyAffineTransformBtn = new Button(btns, SWT.PUSH);
		applyAffineTransformBtn.setText("Apply affine transformation");
		
		fixExif6Btn = new Button(btns, SWT.PUSH);
		fixExif6Btn.setText("Fix-exif6-rotation");
		
		imageEnhanceDialogBtn = new Button(btns, SWT.PUSH);
		imageEnhanceDialogBtn.setText("Image-Enhancement (only visually)");
		
		batchReplaceImgsBtn = new Button(btns, SWT.PUSH);
		batchReplaceImgsBtn.setText("Batch replace images");	
		
		openSleakBtn = new Button(btns, SWT.PUSH);
		openSleakBtn.setText("Open Sleak");
		
		listLibsBtn = new Button(container, 0);
		listLibsBtn.setText("List libs");		
		
		cropLinesOnRegionBorderBtn = new Button(container, 0);
		cropLinesOnRegionBorderBtn.setText("Crop Lines on Region Borders");		
		
//		new Label(container, 0);
		
		Group sortBaselinePtsGroup = new Group(container, 0);
		sortBaselinePtsGroup.setText("Sort baseline pts");
		
		sortBaselinePtsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		sortBaselinePtsGroup.setLayout(new GridLayout(3, false));
		sortXText = new LabeledText(sortBaselinePtsGroup, "X = ");
		sortXText.text.setText("1");
		sortYText = new LabeledText(sortBaselinePtsGroup, "Y = ");
		sortYText.text.setText("0");
		sortBaselineAllRegionsBtn = new Button(sortBaselinePtsGroup, SWT.CHECK);
		sortBaselineAllRegionsBtn.setSelection(true);
		sortBaselineAllRegionsBtn.setText("All regions");
		
		sortBaselinePts = new Button(sortBaselinePtsGroup, SWT.PUSH);
		sortBaselinePts.setText("Sort!");
		
		Group reverseLinesGrp = new Group(container, 0);
		reverseLinesGrp.setText("Reverse text of lines");
		reverseLinesGrp.setLayout(new GridLayout(1, false));
		reverseLinesGrp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		reverseLinesTagsText = new LabeledText(reverseLinesGrp, "Exception tags: ");
		reverseLinesTagsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		reverseLinesExcludeNumbersCheck = new Button(reverseLinesGrp, SWT.CHECK);
		reverseLinesExcludeNumbersCheck.setText("Exclude numbers");
		reverseLinesExcludeNumbersCheck.setSelection(true);
		reverseLinesBtn = new Button(reverseLinesGrp, 0);
		reverseLinesBtn.setText("Reverse!");
		
		if (storage.isAdminLoggedIn()) {
			Group resetIndexGrp = new Group(container, 0);
			resetIndexGrp.setText("Reset index for...");
			resetIndexGrp.setLayout(new GridLayout(3, false));
			resetIndexGrp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			resetIndexCurrentColl = new Button(resetIndexGrp, SWT.PUSH);
			resetIndexCurrentColl.setText("Current collection");
			resetIndexCurrentDoc = new Button(resetIndexGrp, SWT.PUSH);
			resetIndexCurrentDoc.setText("Current document");
			resetIndexCurrentPage = new Button(resetIndexGrp, SWT.PUSH);
			resetIndexCurrentPage.setText("Current page");	
		}
		
//		new Label(shell, 0); // spacer label
		
		clearDebugText = new Button(container, SWT.PUSH);
		clearDebugText.setText("Clear log");
		clearDebugText.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				debugText.setText("");
			}
		});
		
		debugText = new StyledText(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		debugText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		debugText.addLineStyleListener(new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent e) {
				// Set the line number
				e.bulletIndex = debugText.getLineAtOffset(e.lineOffset);

				// Set the style, 12 pixles wide for each digit
				StyleRange style = new StyleRange();
				style.metrics = new GlyphMetrics(0, 0, Integer.toString(debugText.getLineCount() + 1).length() * 12);

				// Create and set the bullet
				e.bullet = new Bullet(ST.BULLET_NUMBER, style);
			}
		});	
		
		addListener();
				
		return container;
	}
	
	private void addListener() {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				try {
					if (e.widget == invalidateSessionBtn) {
						logger.debug("invalidating session...");
						storage.invalidateSession();
					}
					if (e.widget == sortBaselinePts) {
						sortBaselinePts();
					}
				} catch (Throwable ex) {
					mw.onError("An error occured", ex.getMessage(), ex);
				}
			}
		};
		invalidateSessionBtn.addSelectionListener(selectionAdapter);
		sortBaselinePts.addSelectionListener(selectionAdapter);
		
		SWTUtil.onSelectionEvent(syncWithLocalDocBtn, (e) -> {mw.getDocSyncController().syncPAGEFilesWithLoadedDoc();} );
//		SWTUtil.onSelectionEvent(applyAffineTransformBtn, (e) -> {mw.applyAffineTransformToDoc();} );
		SWTUtil.onSelectionEvent(applyAffineTransformBtn, (e) -> { applyAffineTransformToDoc(); } );
		SWTUtil.onSelectionEvent(fixExif6Btn, (e) -> { fixeExif6Rotation(); } );
		SWTUtil.onSelectionEvent(batchReplaceImgsBtn, (e) -> {mw.batchReplaceImagesForDoc();} );
		SWTUtil.onSelectionEvent(openSleakBtn, (e) -> { mw.openSleak(); } );
		SWTUtil.onSelectionEvent(reverseLinesBtn, e -> {
			JAXBPageTranscript tr = storage.getTranscript();
			if (tr==null || tr.getPageData()==null) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No page loaded!");
			}
			
			String[] tagExceptions = new String[0];
			if (reverseLinesTagsText.getText().trim().length()>0) {
				tagExceptions = reverseLinesTagsText.getText().trim().split(" ");
			}
			PageXmlUtils.reverseTextForAllLines(tr.getPageData(), reverseLinesExcludeNumbersCheck.getSelection(), tagExceptions);
		});
		SWTUtil.onSelectionEvent(imageEnhanceDialogBtn, e -> {
			ImageEnhanceDialog imgEnhanceDialog = new ImageEnhanceDialog(canvas.getShell());
			imgEnhanceDialog.open();
		});
		
		listLibsBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				try {
					ProgramUpdater.getLibs(true);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		cropLinesOnRegionBorderBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				JAXBPageTranscript t = mw.getStorage().getTranscript();
				SSW sw = new SSW();
				PageXmlUtils.cutBaselinesToTextRegions(t.getPageData());
				TrpMainWidget.getInstance().getScene().updateAllShapesParentInfo();
				sw.stop(true, "cutting time: ", logger);
//				CropLinesOnRegionBorderFilter f = new CropLinesOnRegionBorderFilter();
//				f.accept(t.getPageData());
				mw.reloadCurrentTranscript(true, true, null, null);
//				mw.relo
			}
		});
		
		twl = new ITranscriptionWidgetListener() {
			@Override public void onCattiMessage(CattiRequest r, String message) {
				logger.debug("catti message: "+message);
				if (!SWTUtil.isDisposed(debugText)) {
					Display.getDefault().asyncExec(() -> debugText.append(message+"\n"));
				}
			}
		};
		
		mw.getUi().getLineTranscriptionWidget().addListener(twl);
		
		// line2word seg
		lineToWordSegBtn.addSelectionListener(new SelectionAdapter() {
			
			@Override public void widgetSelected(SelectionEvent e) {
				ATranscriptionWidget tw = mw.getUi().getSelectedTranscriptionWidget();
				if (tw!=null && tw.getCurrentLineObject()!=null) {
					TrpTextLineType tl = tw.getCurrentLineObject();
			
					List<TrpWordType> segmentedWords = IndexTextUtils.getWordsFromLine(tl, false).getRight();
					logger.debug("performed line 2 word seg");
					
					// remove old words:
					List<TrpWordType> oldWords = new ArrayList<TrpWordType>();
					oldWords.addAll(tl.getTrpWord());
					for (TrpWordType w : oldWords) {
						ICanvasShape cs = canvas.getScene().findShapeWithData(w);
						if (cs != null) {
							mw.getCanvas().getShapeEditor().removeShapeFromCanvas(cs, false);
						}
					}
					
					// add new words:
					int i=0;
					for (TrpWordType w : segmentedWords) {
						w.setLine(tl);
						w.reInsertIntoParent(i++);
						
						try {
							mw.getShapeFactory().addCanvasShape(w);
						} catch (Exception e1) {
							e1.printStackTrace();
						}	
					}
					
					canvas.redraw();
				}
			}
		});
		
		SWTUtil.onSelectionEvent(resetIndexCurrentColl, e -> {
			resetIndex(true, false, false);
		});
		SWTUtil.onSelectionEvent(resetIndexCurrentDoc, e -> {
			resetIndex(false, true, false);
		});
		SWTUtil.onSelectionEvent(resetIndexCurrentPage, e -> {
			resetIndex(false, false, true);
		});
	}
	
	void resetIndex(boolean currentColl, boolean currentDoc, boolean currentPage) {
		if (!storage.isLoggedIn()) {
			DialogUtil.showErrorMessageBox(getShell(), "Not logged in", "You have to log in!");
		}
		else {
			Integer colId=null, docId=null, pageId=null;
			if (currentColl) {
				colId = mw.getSelectedCollectionId();
				if (colId <= 0) {
					DialogUtil.showErrorMessageBox(getShell(), "No collection loaded", "No collection loaded");
					return;			
				}
			}
			else if (currentDoc) {
				if (storage.getDoc()==null) {
					DialogUtil.showErrorMessageBox(getShell(), "No doc loaded", "No doc loaded");
					return;
				}				
				docId = storage.getDocId();
			}
			else if (currentPage) {
				if (storage.getPage()==null) {
					DialogUtil.showErrorMessageBox(getShell(), "No page loaded", "No page loaded");
					return;
				}
				pageId = storage.getPage().getPageId();
			}
			
			try {
				String msg = storage.getConnection().getSearchCalls().resetIndexFlag(colId, docId, pageId);
				DialogUtil.showInfoMessageBox(getShell(), "Reset index", msg);
			} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", e.getMessage());
			}
		}
	}
	
	void sortBaselinePts() {
		int x = 1; int y = 0;
		try {
			x = Integer.parseInt(sortXText.getText());
		} catch (Exception ex) {
		}
		try {
			y = Integer.parseInt(sortYText.getText());
		} catch (Exception ex) {
		}						
		
		List<TrpTextRegionType> regions = new ArrayList<>();
		if (sortBaselineAllRegionsBtn.getSelection()) {
			regions.addAll(storage.getTranscript().getPage().getTextRegions(false));
		} else {
			ICanvasShape s = canvas.getFirstSelected();
			if (s != null && s.getData() instanceof TrpTextRegionType) {
				regions.add((TrpTextRegionType) s.getData());
			}
		}
		
		logger.debug("sorting baseline pts, x = "+x+" y = "+y+" nregions = "+regions.size());
		
		for (TrpTextRegionType r : regions) {
			for (TextLineType l : r.getTextLine()) {
				logger.debug("sorting baseline pts for line: "+l);
				TrpTextLineType tl = (TrpTextLineType) l;
				if (tl.getBaseline() != null) {
					ICanvasShape bls = canvas.getScene().findShapeWithData(tl.getBaseline());
//					logger.debug("bls = "+bls);
					if (bls instanceof CanvasPolyline) {
						logger.debug("sorting baseline pts!");
						CanvasPolyline pl = (CanvasPolyline) bls;
						pl.sortPoints(x, y);
					}
				}
			}
		}
		mw.getCanvas().redraw();
	}
	
	public void fixeExif6Rotation() {
		try {
			logger.debug("fixing exif6 rotation coordinates!");

			if (!storage.isDocLoaded())
				throw new IOException("No document loaded!");

			final FixExif6RotationCoordsDialog d = new FixExif6RotationCoordsDialog(getShell(), storage.getDoc().getPages());
			if (d.open() != Dialog.OK) {
				logger.debug("cancelled");
				return;
			}

			if (d.getSelectedPages()==null) {
				logger.debug("no pages specified");
				return;
			}

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("fixing exif-6 rotated coordinates!");
						storage.applyFunctionToTranscriptsAndSave(d.getSelectedPages(), monitor, tr -> {
							PageXmlUtils.applyExif6Fix(tr.getPage());
						}, "Fixed coords from Exif 6 rotated image");
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Transforming coordinates", true);

			if (d.getSelectedPages().contains(storage.getPageIndex())) {
				mw.reloadCurrentPage(true, null, null);
			}
		} catch (Throwable e) {
			mw.onError("Affine transformation error", "Error during affine transformation of document", e);
		}
	}
	
	public void applyAffineTransformToDoc() {
		try {
			logger.debug("applying affine transformation!");

			if (!storage.isDocLoaded())
				throw new IOException("No document loaded!");

			final AffineTransformDialog d = new AffineTransformDialog(getShell(), storage.getDoc().getPages());
			if (d.open() != Dialog.OK) {
				logger.debug("cancelled");
				return;
			}

			if (!d.hasTransform()) {
				logger.debug("no transform specified");
				return;
			}

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug("applying affine transformation");
						String trTxt = "tx="+d.getTx()+", ty="+d.getTy()+", sx="+d.getSx()+", sy="+d.getSy()+", rot="+d.getRot();
						logger.debug(trTxt);
						String msg = "Applied affine transformation: "+trTxt;
						storage.applyFunctionToTranscriptsAndSave(d.getSelectedPages(), monitor, tr -> {
							double rotRad = MathUtil.degToRad(d.getRot());
							PageXmlUtils.applyAffineTransformation(tr.getPage(), d.getTx(), d.getTy(), d.getSx(), d.getSy(), rotRad);
						}, msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Transforming coordinates", true);

			if (d.getSelectedPages().contains(storage.getPageIndex())) {
				mw.reloadCurrentPage(true, null, null);
			}
		} catch (Throwable e) {
			mw.onError("Affine transformation error", "Error during affine transformation of document", e);
		}
	}	
}
