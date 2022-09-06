package eu.transkribus.swt_gui.metadata;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.customtags.CustomTagFactory;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory.TagRegistryChangeEvent;
import eu.transkribus.core.model.beans.customtags.StructureTag;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.util.ColorChooseButton;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.databinding.DataBinder;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettings;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class StructTagSpecWidget extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(StructTagSpecWidget.class);
	
	TableViewer tableViewer;
	Button /*showAllTagsBtn, drawShapesInStructColorsBtn,*/ customizeBtn, drawDefaultColorsBtn, drawStructTypeTextBtn;
	Label headerLbl;
	
	Map<CustomTagSpec, ControlEditor> insertTagEditors = new ConcurrentHashMap<>();
	Map<CustomTagSpec, ControlEditor> removeTagDefEditors = new ConcurrentHashMap<>();
	Map<CustomTagSpec, ControlEditor> colorEditors = new ConcurrentHashMap<>();
	
	Map<CustomTagSpec, ControlEditor> moveUpEditors = new ConcurrentHashMap<>();
	Map<CustomTagSpec, ControlEditor> moveDownEditors = new ConcurrentHashMap<>();
	
	boolean isEditable=true;
	
	public StructTagSpecWidget(Composite parent, int style, boolean isEditable) {
		super(parent, style);
//		setLayout(new FillLayout());
		setLayout(new GridLayout(1, false));
		
		this.isEditable = isEditable;
		int nCols = isEditable ? 1 : 4;

		Composite topContainer = new Composite(this, SWT.NONE);
//		container.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		topContainer.setLayout(new GridLayout(nCols, false));
//		topContainer.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		if (isEditable) {
			headerLbl = new Label(topContainer, 0);
			headerLbl.setText("Structure Tags");
			Fonts.setBoldFont(headerLbl);
		}
		
//		headerLbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		DataBinder dBinder = DataBinder.get();
		
		if (!isEditable) {
//			if (false) {
//			showAllTagsBtn = new Button(topContainer, SWT.CHECK | SWT.FLAT);
//			showAllTagsBtn.setText("Show all");
//			showAllTagsBtn.setToolTipText("Show all available structure tags");
//			dBinder.bindBeanToWidgetSelection(
//					TrpSettings.SHOW_ALL_STRUCT_TAGS_IN_TAG_EDITOR_PROPERTY, TrpConfig.getTrpSettings(), showAllTagsBtn);
//			}
			
//			drawShapesInStructColorsBtn = new Button(topContainer, SWT.CHECK | SWT.FLAT);
//			drawShapesInStructColorsBtn.setText("Paint struct colors");
//			drawShapesInStructColorsBtn.setToolTipText("Paint all shapes in its structure type color - white means no structure type is set!");
//			dBinder.bindBeanToWidgetSelection(
//					TrpSettings.DRAW_SHAPES_IN_STRUCT_COLORS_PROPERTY, TrpConfig.getTrpSettings(), drawShapesInStructColorsBtn);
			
			drawStructTypeTextBtn = new Button(topContainer, SWT.CHECK | SWT.FLAT);
			drawStructTypeTextBtn.setText("Show structure type names");
			drawStructTypeTextBtn.setToolTipText("Displays the structure type names in the canvas");
			dBinder.bindBeanToWidgetSelection(TrpSettings.SHOW_STRUCT_TYPE_TEXT_PROPERTY, TrpConfig.getTrpSettings(), drawStructTypeTextBtn);
			
			drawDefaultColorsBtn = new Button(topContainer, SWT.CHECK | SWT.FLAT);
			drawDefaultColorsBtn.setText("Display structure types in color");
			drawDefaultColorsBtn.setToolTipText("Draws the colors of the structure types - or if deselected the default shape colors according to their region type");
			dBinder.bindBeanToWidgetSelection(TrpSettings.SHOW_STRUCT_TYPE_COLOR_PROPERTY, TrpConfig.getTrpSettings(), drawDefaultColorsBtn);
			
			customizeBtn = new Button(topContainer, 0);
			customizeBtn.setText("Customize..");
			customizeBtn.setImage(Images.PENCIL);
			SWTUtil.onSelectionEvent(customizeBtn, e -> {
				StructTagConfDialog diag = new StructTagConfDialog(getShell());
				diag.open();
			});
		}
				
		Composite tableContainer = new Composite(this, SWT.NONE);
		tableContainer.setLayout(SWTUtil.createGridLayout(isEditable ? 2 : 1, false, 0, 0));
		tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, nCols, 1));
		
		int tableViewerStyle = SWT.NO_FOCUS | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI;
		tableViewer = new TableViewer(tableContainer, tableViewerStyle);
//		tableViewer.getTable().setToolTipText("List of structure tag specifications);
		
//		tagsTableViewer = new TableViewer(taggingGroup, SWT.FULL_SELECTION|SWT.HIDE_SELECTION|SWT.NO_FOCUS | SWT.H_SCROLL
//		        | SWT.V_SCROLL | SWT.FULL_SELECTION /*| SWT.BORDER*/);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, isEditable ? 1 : 2, 1);
//		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gd.heightHint = 150;
		tableViewer.getTable().setLayoutData(gd);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		ColumnViewerToolTipSupport.enableFor(tableViewer);
//		tableViewer.getTable().addControlListener(new ControlAdapter() {
//	        public void controlResized(ControlEvent e) {
//	            SWTUtil.packAndFillLastColumn(tableViewer);
//	        }
//	    });
		
		TableViewerColumn structTypeCol = new TableViewerColumn(tableViewer, SWT.NONE);
		structTypeCol.getColumn().setText("Structure type");
		structTypeCol.getColumn().setResizable(true);
		structTypeCol.getColumn().setWidth(150);
		structTypeCol.setLabelProvider(new ColumnLabelProvider() {
			@Override public String getText(Object element) {
				if (!(element instanceof StructCustomTagSpec)) {
					return "i am error";
				}
				
				StructCustomTagSpec tagSpec = (StructCustomTagSpec) element;
				String type = (String) tagSpec.getCustomTag().getType();
				
				if (type != null && type.equals("article")){
					logger.debug("id: " + tagSpec.getCustomTag().getAttributeValue("id"));
					type = type.concat("_" + tagSpec.getCustomTag().getAttributeValue("id"));
				}
				//logger.debug("type: " + type);
				return type==null ? "error parsing structure type" : type;
			}
			
//			@Override public Color getForeground(Object element) {
//				if (!(element instanceof CustomTagSpec)) {
//					return null;
//				}
//				CustomTagSpec tagDef = (CustomTagSpec) element;
//				
//				String tagColor = CustomTagFactory.getTagColor(tagDef.getCustomTag().getTagName());
//				return Colors.decode2(tagColor);
//			}
		});
		
		if (true) {
			TableViewerColumn colorCol = new TableViewerColumn(tableViewer, SWT.NONE);
			colorCol.getColumn().setText("Color");
			colorCol.getColumn().setResizable(true);
			colorCol.getColumn().setWidth(50);
			colorCol.setLabelProvider(new CellLabelProvider() {
				@Override public void update(ViewerCell cell) {
					if (!(cell.getElement() instanceof CustomTagSpec)) {
						return;
					}
					
					TableItem item = (TableItem) cell.getItem();
					StructCustomTagSpec tagSpec = (StructCustomTagSpec) cell.getElement();
					
					TableEditor editor = new TableEditor(item.getParent());				
	                editor.grabHorizontal  = true;
	                editor.grabVertical = true;
	                editor.horizontalAlignment = SWT.LEFT;
	                editor.verticalAlignment = SWT.TOP;
	                
//	                String tagColor = CustomTagFactory.getTagColor(tagSpec.getCustomTag().getTagName());
//	                logger.trace("tag color for tag: "+tagSpec.getCustomTag().getTagName()+" color: "+tagColor);
//	                if (tagColor == null) {
//	                	tagColor = CustomTagFactory.getNewTagColor();
//	                }
	                
	                RGB rgb = tagSpec.getRGB();
	                if (rgb == null) {
	                	rgb = StructCustomTagSpec.DEFAULT_COLOR;
	                }
	                
	                ColorChooseButton colorCtrl = new ColorChooseButton((Composite) cell.getViewerRow().getControl(), rgb) {
	                	@Override protected void onColorChanged(RGB rgb) {
	                		tagSpec.setRGB(rgb);
	                		Storage.getInstance().signalStructCustomTagSpecsChanged();
	                	}
	                };
	                colorCtrl.setEditorEnabled(isEditable);

	                editor.setEditor(colorCtrl , item, cell.getColumnIndex());
	                editor.layout();
	                
	                TaggingWidgetUtils.replaceEditor(colorEditors, tagSpec, editor);
				}
			});
		}
		
		TableViewerColumn shortcutCol = new TableViewerColumn(tableViewer, SWT.NONE);
		shortcutCol.getColumn().setText("Shortcut");
		shortcutCol.getColumn().setResizable(false);
		shortcutCol.getColumn().setWidth(100);
		
		shortcutCol.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				String text = "";
				logger.trace("element = "+element);
				if (!(element instanceof CustomTagSpec)) {
					cell.setText("i am error");
					return;
				}
				
				StructCustomTagSpec tagDef = (StructCustomTagSpec) element;
				if (tagDef.getShortCut()!=null) {
					text = "Ctrl+Alt+"+tagDef.getShortCut();
				}
				else {
					text = "";
				}
				
				cell.setText(text);
			}
			
			@Override
	        public String getToolTipText(Object element) {
	           return "Alt + a number between 0 and 9";
	        }
		});

		if (this.isEditable) {
			shortcutCol.setEditingSupport(new EditingSupport(tableViewer) {
				@Override
				protected void setValue(Object element, Object value) {
						logger.debug("setting value of: "+element+" to: "+value);
						StructCustomTagSpec cDef = (StructCustomTagSpec) element;
						if (cDef == null) {
							return;
						}
						
						logger.debug("setting value to: "+value);
						
						String shortCut = (String) value;
						if (shortCut==null || shortCut.isEmpty()) {
							cDef.setShortCut(null);
						} else {
							cDef.setShortCut(shortCut);
						}
						
						tableViewer.refresh(true);
						logger.debug("shorcut value changed - sending signal to storage!");
						Storage.getInstance().signalStructCustomTagSpecsChanged();
				}
				
				@Override
				protected Object getValue(Object element) {
					CustomTagSpec cDef = (CustomTagSpec) element;
					if (cDef == null) { // shouldn't happen I guess...
						return "";
					} else {
						return cDef.getShortCut()==null ? "" : cDef.getShortCut();
					}
				}
				
				@Override
				protected CellEditor getCellEditor(Object element) {
					TextCellEditor ce = new TextCellEditor(tableViewer.getTable());
					
					// add a "default" description text when no shortcut is set
					ce.getControl().addFocusListener(new FocusAdapter() {				
						@Override
						public void focusGained(FocusEvent e) {
							StructCustomTagSpec cDef = (StructCustomTagSpec) element;
							if (StringUtils.isEmpty(cDef.getShortCut())) {
								ce.setValue("0 - 9");
								ce.performSelectAll();		
							}
						}
					});
					
					ce.setValidator(new ICellEditorValidator() {
						@Override
						public String isValid(Object value) {
							String str = (String) value;
							int len = StringUtils.length(str);
							logger.debug("sc = "+str+" len = "+len);
							if (len <= 0) { // empty string are allowed for deleting shortcut
								return null;
							}
							if (len>=2) {
								return "Not a string of size 1!";
							}
							if (!CustomTagSpec.isValidShortCut(str)) {
								return "Not a valid shortcut character (0-9)!";
							}
							
							return null;
						}
					});
					
					return ce;
				}
				
				@Override
				protected boolean canEdit(Object element) {
					return true;
				}
			});
		}

		if (!this.isEditable) { // add an "add tag button" to add the tag to the current position in the transcription widget 
			TableViewerColumn addButtonCol = new TableViewerColumn(tableViewer, SWT.NONE);
			addButtonCol.getColumn().setText("");
			addButtonCol.getColumn().setResizable(false);
			addButtonCol.getColumn().setWidth(100);
			
			CellLabelProvider addButtonColLabelProvider = new CellLabelProvider() {
				@Override public void update(final ViewerCell cell) {
					StructCustomTagSpec tagSpec = (StructCustomTagSpec) cell.getElement();
					final TableItem item = (TableItem) cell.getItem();
					TableEditor editor = new TableEditor(item.getParent());
					
					Button addBtn = new Button((Composite) cell.getViewerRow().getControl(), 0);
					addBtn.setImage(Images.ADD_12);
					addBtn.setToolTipText("Tag selected elements with this structure type");
					SWTUtil.onSelectionEvent(addBtn, e -> {
						if (TrpMainWidget.getInstance() != null && tagSpec != null && tagSpec.getCustomTag()!=null) {
							StructureTag st = tagSpec.getCustomTag();
							if (st.getType().startsWith("article")){
								StructureTag newSt = new StructureTag("article");
								try {
									newSt.setAttribute("id", st.getAttributeValue("id"), true);
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								logger.debug("st.getType: " + st.getType());
								logger.debug("st.getID: " + st.getAttributeValue("id"));
								logger.debug("tag spec color: " + tagSpec.getRGB());
								logger.debug("shortcut: " + tagSpec.getShortCut());
								TrpMainWidget.getInstance().setStructureTypeOfSelected(newSt, false);
							}
							else{
								TrpMainWidget.getInstance().setStructureTypeOfSelected(st.getType(), false);
							}

						}
					});
					                
	                Point size = addBtn.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	                
					editor.minimumWidth = size.x;
					editor.horizontalAlignment = SWT.LEFT;
					
	                editor.setEditor(addBtn , item, cell.getColumnIndex());
	                editor.layout();
	                
	                TaggingWidgetUtils.replaceEditor(insertTagEditors, tagSpec, editor);
				}
			};
			addButtonCol.setLabelProvider(addButtonColLabelProvider);
		} // end add button column
		
		if (!this.isEditable) { // add an "add award tag button" to add the tag to all empty regions in the doc
			TableViewerColumn addButtonCol2 = new TableViewerColumn(tableViewer, SWT.NONE);
			addButtonCol2.getColumn().setText("");
			addButtonCol2.getColumn().setResizable(false);
			addButtonCol2.getColumn().setWidth(100);
			
			CellLabelProvider addButtonColLabelProvider2 = new CellLabelProvider() {
				@Override public void update(final ViewerCell cell) {
					StructCustomTagSpec tagSpec = (StructCustomTagSpec) cell.getElement();
					final TableItem item = (TableItem) cell.getItem();
					TableEditor editor = new TableEditor(item.getParent());
					
					Button addBtn = new Button((Composite) cell.getViewerRow().getControl(), 0);
					addBtn.setImage(Images.AWARD_ADD);
					addBtn.setToolTipText("Advanced options for document...");
					SWTUtil.onSelectionEvent(addBtn, e -> {
						if (TrpMainWidget.getInstance() != null && tagSpec != null && tagSpec.getCustomTag()!=null) {
							StructureTag st = tagSpec.getCustomTag();

							//do batch annotations
							logger.debug("AWARD ADD " + st.getType());
							AdvancedStructTagDialog diag = new AdvancedStructTagDialog(getShell(), st.getType());
							if (diag.open() == Dialog.OK) {
								if (!diag.isAnnotate() && !diag.isDelete() && !diag.isRename()) {
									DialogUtil.showErrorMessageBox(getShell(), "Nothing selected!", "Select one of the options 'annotate', 'delete' or 'rename'!");
									return;
								}
								if (diag.isRename() && diag.getSelectedType()==null) {
									DialogUtil.showErrorMessageBox(getShell(), "Renaming failed!", "Renaming failed because no new structure type name was selected!");
									return;
								}
								String pageString = CoreUtils.getRangeListStrFromSet(diag.getPageIndices());
								String message = "Do you really want to ";
								if (diag.isAnnotate()) {
									message += "annotate all regions without structure type of this document for page(s): " + pageString + " with '" + st.getType() + "'?";
								}
								else if (diag.isDelete()) {
									message += "delete all structure types '" + st.getType() + "' in this document for page(s): " + pageString + "?" ;
								}
								else if (diag.isRename()) {
									message += "rename all structure types '" + st.getType() + "' in this document for page(s): " + pageString + " with '" + diag.getSelectedType() + "'?";
								}
								int r = DialogUtil.showYesNoCancelDialog(getShell(), "Batch process", message);
								if (r == SWT.YES) {
									logger.debug("Pages indizes: " + pageString);
									TrpMainWidget.getInstance().handleStructureTagsInMonitor(diag.getPageIndices(), st.getType(), diag.getSelectedType(), diag.isAnnotate(), diag.isDelete(), diag.isRename());
								}
							}
						}
					});
					                
	                Point size = addBtn.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	                
					editor.minimumWidth = size.x;
					editor.horizontalAlignment = SWT.LEFT;
					
	                editor.setEditor(addBtn , item, cell.getColumnIndex());
	                editor.layout();
	                
	                //TaggingWidgetUtils.replaceEditor(insertTagEditors, tagSpec, editor);
				}
			};
			addButtonCol2.setLabelProvider(addButtonColLabelProvider2);
		} // end add button column
			
		// TODO: add button to add struct tags 'recursively'
		
		tableViewer.refresh(true);
		tableViewer.getTable().pack();
		
		if (this.isEditable) {
			Composite btnsComp = new Composite(tableContainer, 0);
			btnsComp.setLayout(new RowLayout(SWT.VERTICAL));
			btnsComp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true, 1, 1));
			
			Button removeBtn = new Button(btnsComp, 0);
			removeBtn.setImage(Images.DELETE);
			removeBtn.setToolTipText("Remove selected tag from list");
			SWTUtil.onSelectionEvent(removeBtn, e -> {
				removeSelected();
			});
			
			Button moveUpBtn = new Button(btnsComp, 0);
			moveUpBtn.setImage(Images.getOrLoad("/icons/arrow_up.png"));
			moveUpBtn.setToolTipText("Move up selected");
			SWTUtil.onSelectionEvent(moveUpBtn, e -> {
				moveSelected(true);
			});			
			
			Button moveDownBtn = new Button(btnsComp, 0);
			moveDownBtn.setImage(Images.getOrLoad("/icons/arrow_down.png"));
			moveDownBtn.setToolTipText("Move down selected");
			SWTUtil.onSelectionEvent(moveDownBtn, e -> {
				moveSelected(false);
			});			
		}		

		topContainer.layout(true);
		
//		if (isEditable) {
//			SWTUtil.packAndFillLastColumn(tableViewer);
//		}
		
		updateAvailableTagSpecs();

		
		// Listener:
		
		IStorageListener storageListener = new IStorageListener() {
			@Override public void handlStructTagSpecsChangedEvent(StructTagSpecsChangedEvent e) {
				updateAvailableTagSpecs();
			}
		};
		Storage.getInstance().addListener(storageListener);
		
		Observer customTagFactoryObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (arg instanceof TagRegistryChangeEvent) {
					logger.debug("TagRegistryChangeEvent: "+arg);
					updateAvailableTagSpecs();
				}				
			}
		};
		CustomTagFactory.addObserver(customTagFactoryObserver);
		
		PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
//				if (evt.getPropertyName().equals(TrpSettings.SHOW_ALL_STRUCT_TAGS_IN_TAG_EDITOR_PROPERTY)) {
//					updateAvailableTagSpecs();
//				}
			}
		};
		TrpConfig.getTrpSettings().addPropertyChangeListener(propertyChangeListener);
		
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				CustomTagFactory.deleteObserver(customTagFactoryObserver);
				TrpConfig.getTrpSettings().removePropertyChangeListener(propertyChangeListener);
				Storage.getInstance().removeListener(storageListener);
			}
		});
	}
	
	private void removeSelected() {
		for (StructCustomTagSpec cDef : getSelected()) {
			if (cDef != null) {
				Storage.getInstance().removeStructCustomTagSpec(cDef);
			}			
		}
	}
	
	private void moveSelected(boolean moveUp) {
		StructCustomTagSpec tagDef = getFirstSelected();
		if (tagDef==null) {
			return;
		}
		
		logger.debug("moving selected: "+tagDef);
		
		List<StructCustomTagSpec> cDefs = Storage.getInstance().getStructCustomTagSpecs();
		int i = cDefs.indexOf(tagDef);
		if (moveUp && i>=1) {
			if (cDefs.remove(tagDef)) {
				cDefs.add(i-1, tagDef);
				Storage.getInstance().signalStructCustomTagSpecsChanged();
			}
		}
		else if (!moveUp && i<cDefs.size()-1) {
			if (cDefs.remove(tagDef)) {
				cDefs.add(i+1, tagDef);
				Storage.getInstance().signalStructCustomTagSpecsChanged();
			}
		}
	}
	
	private void updateAvailableTagSpecs() {
		logger.info("updating available struct tag specs: "+Storage.getInstance().getStructCustomTagSpecs());
		Display.getDefault().asyncExec(() -> {
			if (SWTUtil.isDisposed(tableViewer.getTable()) || SWTUtil.isDisposed(this)) {
				return;
			}
			
//			boolean showAllTags = TrpConfig.getTrpSettings().isShowAllStructTagsInTagEditor();
//			boolean showAllTags = false;
//			if (!isEditable) {
//				SWTUtil.setSelection(showAllTagsBtn, showAllTags);
//			}
			tableViewer.setInput(Storage.getInstance().getStructCustomTagSpecs());
			
//			if (isEditable || !showAllTags) {
//				headerLbl.setText("Structure Tag Specifications");
//				tableViewer.setInput(Storage.getInstance().getStructCustomTagSpecs());
//			}
//			else {
//				headerLbl.setText("All Structure Tags");
//				List<StructCustomTagSpec> allTagsSpecs = new ArrayList<>();
//				
//				int i=0;
//				for (TextTypeSimpleType t : TextTypeSimpleType.values()) {
//					StructureTag st = new StructureTag(t.value());
//					String colorStr = TaggingWidgetUtils.INDEX_COLORS[i++];
//					StructCustomTagSpec ts = new StructCustomTagSpec(st, colorStr); // TODO: use fixed colors!
//					allTagsSpecs.add(ts);
//				}
//				tableViewer.setInput(allTagsSpecs);
//			}
			
			Collection<CustomTagSpec> tagSpecs = (Collection<CustomTagSpec>) tableViewer.getInput();
			TaggingWidgetUtils.updateEditors(colorEditors, tagSpecs);
			TaggingWidgetUtils.updateEditors(removeTagDefEditors, tagSpecs);
			TaggingWidgetUtils.updateEditors(insertTagEditors, tagSpecs);
			TaggingWidgetUtils.updateEditors(moveUpEditors, tagSpecs);
			TaggingWidgetUtils.updateEditors(moveDownEditors, tagSpecs);
			
			tableViewer.refresh(true);
		});
	}
	
	public void updateVisibility(boolean setEnabled){
		
		customizeBtn.setEnabled(setEnabled);
		drawDefaultColorsBtn.setEnabled(setEnabled);
		drawStructTypeTextBtn.setEnabled(setEnabled);
		tableViewer.getTable().setEnabled(setEnabled);
	
	}
	
	public TableViewer getTableViewer() {
		return tableViewer;
	}

	public List<StructCustomTagSpec> getSelected() {
		return tableViewer.getStructuredSelection().toList();
	}
	
	public StructCustomTagSpec getFirstSelected() {
		return getSelected().isEmpty() ? null : getSelected().get(0);
	}
	
//	public Label getHeaderLbl() {
//		return headerLbl;
//	}
	
	public Button getDrawDefaultColorsBtn() {
		return drawDefaultColorsBtn;
	}

}
