package eu.transkribus.swt_gui.models;

import java.awt.BasicStroke;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.ClientErrorException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.io.util.TrpProperties;
import eu.transkribus.core.model.beans.HtrTrainConfig;
import eu.transkribus.core.model.beans.PyLaiaCreateModelPars;
import eu.transkribus.core.model.beans.PyLaiaTrainCtcPars;
import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TextFeatsCfg;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.TrpPreprocPars;
import eu.transkribus.core.model.beans.enums.DataSetType;
import eu.transkribus.core.model.beans.enums.DocType;
import eu.transkribus.core.rest.JobConst;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.HtrCITlabUtils;
import eu.transkribus.core.util.HtrPyLaiaUtils;
import eu.transkribus.core.util.IsoLangUtils;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.core.util.StrUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.LabeledCombo;
import eu.transkribus.swt.util.MetadataTextFieldValidator;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.dialogs.CharSetViewerDialog;
import eu.transkribus.swt_gui.dialogs.DocImgViewerDialog;
import eu.transkribus.swt_gui.htr.IsoLanguageEditComposite;
import eu.transkribus.swt_gui.htr.PyLaiaAdvancedConfDialog;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class ModelDetailsWidget extends SashForm {
	private static final Logger logger = LoggerFactory.getLogger(ModelDetailsWidget.class);
	
	private static final String NOT_AVAILABLE = "N/A";

	private static final String[] CITLAB_TRAIN_PARAMS = { HtrTrainConfig.NUM_EPOCHS_KEY, 
			HtrTrainConfig.LEARNING_RATE_KEY, HtrTrainConfig.NOISE_KEY, HtrTrainConfig.TRAIN_SIZE_KEY,
			HtrTrainConfig.BASE_MODEL_ID_KEY, HtrTrainConfig.BASE_MODEL_NAME_KEY };

	Label finalTrainCerLbl, finalValCerLbl;
	Label nameLbl, langLbl;
	Text nameTxt, descTxt, nrOfLinesTxt, nrOfWordsTxt, finalTrainCerTxt, finalValCerTxt;
	IsoLanguageEditComposite langEditor;
	Combo publishStateCombo;
	Text appsTxt;
	Combo docTypeCombo;
	Table paramTable;
	Button updateMetadataBtn, showTrainSetBtn, showValSetBtn, showCharSetBtn, showAdvancedParsBtn;
	ChartComposite jFreeChartComp;
	JFreeChart chart = null;
	DocImgViewerDialog trainDocViewer, valDocViewer = null;
	CharSetViewerDialog charSetViewer = null;
	LabeledCombo ratingCombo;
	
	private final Storage store;
	private TrpModelMetadata model;
	private final MetadataTextFieldValidator<TrpModelMetadata> validator;
	
	public ModelDetailsWidget(Composite parent, int style) {
		super(parent, style);
		store = Storage.getInstance();
		validator = new MetadataTextFieldValidator<>();
		
		// a composite for the model's metadata
		Composite mdComp = new Composite(this, SWT.BORDER);
		mdComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mdComp.setLayout(new GridLayout(2, true));
		
		nameLbl = new Label(mdComp, SWT.NONE);
		nameLbl.setText("Name:");
		langLbl = new Label(mdComp, SWT.NONE);
		langLbl.setText("Language:");

		nameTxt = new Text(mdComp, SWT.BORDER);
		nameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		validator.attach("Name", nameTxt, 1, 100, h -> h.getName());
		
		langEditor = new IsoLanguageEditComposite(mdComp, 0);
		langEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));	
		validator.attach("Language", langEditor.getText(), 1, 300, h -> IsoLangUtils.DEFAULT_RESOLVER.getLanguageWithResolvedIsoCodes(h.getLanguage()));

		Label descLbl = new Label(mdComp, SWT.NONE);
		descLbl.setText("Description:");
		Label paramLbl = new Label(mdComp, SWT.NONE);
		paramLbl.setText("Parameters:");

		Composite descMdContainer = new Composite(mdComp, 0);
		descMdContainer.setLayout(SWTUtil.createGridLayout(2, false, 0, 0));
		descMdContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		descTxt = new Text(descMdContainer, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		descTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		validator.attach("Description", descTxt, 1, 2048, h -> h.getDescriptions() == null ? null : h.getDescriptions().get(Locale.ENGLISH.getLanguage()));
		
		Label docTypeLabel = new Label(descMdContainer, SWT.NONE);
		docTypeLabel.setText("Document Type:");
		createDocTypeCombo(descMdContainer);
		
		Composite paramsContainer = new Composite(mdComp, 0);
		paramsContainer.setLayout(SWTUtil.createGridLayout(1, false, 0, 0));
		paramsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		paramTable = new Table(paramsContainer, SWT.BORDER | SWT.V_SCROLL);
		paramTable.setHeaderVisible(false);
		TableColumn paramCol = new TableColumn(paramTable, SWT.NONE);
		paramCol.setText("Parameter");
		TableColumn valueCol = new TableColumn(paramTable, SWT.NONE);
		valueCol.setText("Value");
		paramTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		showAdvancedParsBtn = new Button(paramsContainer, 0);
		showAdvancedParsBtn.setText("Show advanced parameters...");
//		showAdvancedParsBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		showAdvancedParsBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label nrOfWordsLbl = new Label(mdComp, SWT.NONE);
		nrOfWordsLbl.setText("Nr. of Words:");
		Label nrOfLinesLbl = new Label(mdComp, SWT.NONE);
		nrOfLinesLbl.setText("Nr. of Lines:");

		nrOfWordsTxt = new Text(mdComp, SWT.BORDER | SWT.READ_ONLY);
		nrOfWordsTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		nrOfLinesTxt = new Text(mdComp, SWT.BORDER | SWT.READ_ONLY);
		nrOfLinesTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		//publishing models is restricted to admins for now
		if(store.isAdminLoggedIn()) {
			createPublishStateComposite(mdComp);
			ratingCombo = new LabeledCombo(mdComp, "Rating: ");
			ratingCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			ratingCombo.combo.setItems(new String[] {"-", "1", "2", "3", "4", "5"});
			for (String i : ratingCombo.combo.getItems()) { // needed for the validator
				ratingCombo.combo.setData(i, i);
			}
			validator.attach("Rating", ratingCombo.combo, -1, -1, h -> h.getInternalRating()==null ? "-" : ""+h.getInternalRating());
			
			Label appsLbl = new Label(mdComp, SWT.NONE);
			appsLbl.setText("Applications:");
			appsTxt = new Text(mdComp, SWT.BORDER);
			appsTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			validator.attach("Applications", appsTxt, -1, -1, 
					h -> {
						if(h.getApps() == null) {
							return "";
						} else {
							return h.getApps().stream().collect(Collectors.joining(","));
						}
					});
		}

		Composite btnComp = new Composite(mdComp, SWT.NONE);
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		//save, trainSet, valSet, charSet buttons
		final int numButtons = 4;
		btnComp.setLayout(new GridLayout(numButtons, true));
		GridData btnGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		
		updateMetadataBtn = new Button(btnComp, SWT.PUSH);
		updateMetadataBtn.setText("Save");
		updateMetadataBtn.setImage(Images.DISK);
		updateMetadataBtn.setLayoutData(btnGridData);
		
		showTrainSetBtn = new Button(btnComp, SWT.PUSH);
		showTrainSetBtn.setText("Show Train Set");
		showTrainSetBtn.setLayoutData(btnGridData);
		
		showValSetBtn = new Button(btnComp, SWT.PUSH);
		showValSetBtn.setText("Show Validation Set");
		showValSetBtn.setLayoutData(btnGridData);
		
		showCharSetBtn = new Button(btnComp, SWT.PUSH);
		showCharSetBtn.setText("Show Characters");
		showCharSetBtn.setLayoutData(btnGridData);

		mdComp.pack();
		
		// a composite for the CER stuff
		Composite cerComp = new Composite(this, SWT.BORDER);
		cerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cerComp.setLayout(new GridLayout(4, false));

		// Label cerLbl = new Label(cerComp, SWT.NONE);
		// cerLbl.setText("Train Curve:");

		jFreeChartComp = new ChartComposite(cerComp, SWT.BORDER);
		jFreeChartComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		finalTrainCerLbl = new Label(cerComp, SWT.NONE);
		finalTrainCerLbl.setText("CER on Train Set:");
		finalTrainCerTxt = new Text(cerComp, SWT.BORDER | SWT.READ_ONLY);
		finalTrainCerTxt.setLayoutData(gd);

		finalValCerLbl = new Label(cerComp, SWT.NONE);
		finalValCerLbl.setText("CER on Validation Set:");
		finalValCerTxt = new Text(cerComp, SWT.BORDER | SWT.READ_ONLY);
		finalValCerTxt.setLayoutData(gd);
		
		if(publishStateCombo != null) {
			this.setWeights(new int[] { 58, 42 });
		}
		
		this.model = null;
		
		//init with no model selected, i.e. disable controls
		updateDetails(null);
		
		addListeners();
	}
	
	public TrpModelMetadata getModel() {
		return model;
	}

	private void createPublishStateComposite(Composite mdComp) {
		Composite publishComp = new Composite(mdComp, SWT.NONE);
		publishComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		GridLayout publishCompLayout = new GridLayout(2, false);
		publishCompLayout.marginHeight = publishCompLayout.marginWidth = 0;
		publishComp.setLayout(publishCompLayout);
		
		Label publishStateLabel = new Label(publishComp, SWT.NONE);
		publishStateLabel.setText("Visibility:");
		publishStateCombo = new Combo(publishComp, SWT.READ_ONLY);
		publishStateCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		List<Pair<String, ReleaseLevel>> publishStates = new ArrayList<>(3);
		publishStates.add(Pair.of("Private Model", ReleaseLevel.None));
		publishStates.add(Pair.of("Public model with private data sets", ReleaseLevel.UndisclosedDataSet));
		publishStates.add(Pair.of("Public model with public data sets", ReleaseLevel.DisclosedDataSet));
		
		for(Pair<String, ReleaseLevel> e : publishStates) {
			publishStateCombo.add(e.getKey());
			publishStateCombo.setData(e.getKey(), e.getValue());
		}
		validator.attach("Visibility", publishStateCombo, -1, -1, h -> h.getReleaseLevel());
	}
	
	private void createDocTypeCombo(Composite parent) {
		docTypeCombo = new Combo(parent, SWT.READ_ONLY);
		docTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		List<Pair<String, DocType>> docTypes = new ArrayList<>(2);
		docTypes.add(Pair.of("Handwritten", DocType.HANDWRITTEN));
		docTypes.add(Pair.of("Print", DocType.PRINT));
		
		for(Pair<String, DocType> e : docTypes) {
			docTypeCombo.add(e.getKey());
			docTypeCombo.setData(e.getKey(), e.getValue());
		}
		if(store.isAdminLoggedIn()) {
			docTypeCombo.setEnabled(model != null && store.isAdminLoggedIn());
			//do not attach validator to disabled fields
			validator.attach("Document Type", docTypeCombo, -1, -1, h -> "" + DocType.fromString(h.getDocType()));
		} else {
			docTypeCombo.setEnabled(false);
		}
	}

	void updateDetails(TrpModelMetadata briefRepresentation) {
		if(briefRepresentation == null) {
			this.model = null;
		} else {
			try {
				//the list contains the brief representations of models. Retrieve details from the server here
				this.model = store.getConnection().getModelCalls().getModel(briefRepresentation);
			} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
				DialogUtil.showErrorBalloonToolTip(this, "Could not retrieve model details.", "");
			}
		}
		validator.setOriginalObject(model);
		
		if(publishStateCombo != null) {
			publishStateCombo.setEnabled(model != null);
		}
		if (ratingCombo != null) {
			ratingCombo.setEnabled(model != null);
		}
		docTypeCombo.setEnabled(model != null && store.isAdminLoggedIn());
		
		nameTxt.setEnabled(model != null);
		descTxt.setEnabled(model != null);
		
		if (model!=null) {
			langLbl.setVisible(ModelUtil.hasTypeLanguage(model.getType()));
			langEditor.setVisible(ModelUtil.hasTypeLanguage(model.getType()));
			langEditor.setEnabled(ModelUtil.hasTypeLanguage(model.getType()));
			if (ModelUtil.hasTypeLanguage(model.getType())) {
				validator.attach("Language", langEditor.getText(), 1, 300, h -> IsoLangUtils.DEFAULT_RESOLVER.getLanguageWithResolvedIsoCodes(h.getLanguage()));
			}
			else {
				validator.detach("Language");
			}
		}
		else {
			langLbl.setVisible(true);
			langEditor.setVisible(true);			
			langEditor.setEnabled(false);
		}
//		langEditor.setEnabled(model != null && ModelUtil.hasTypeLanguage(model.getType()));
		
		jFreeChartComp.setEnabled(model != null);
		
		logger.debug("Model = {}", (model == null ? "null" : model.getModelId() + " - " + model.getName()));
		
		if (model == null) {
			//clear text fields and disable buttons
			nameTxt.setText("");
			descTxt.setText("");
			langEditor.setLanguageString("");
			finalTrainCerTxt.setText("");
			finalValCerTxt.setText("");
			paramTable.clearAll();
			nrOfLinesTxt.setText("");
			nrOfWordsTxt.setText("");
			if(publishStateCombo != null) {
				publishStateCombo.select(0);
			}
			if (ratingCombo != null) {
				ratingCombo.combo.select(0);
			}
			if(appsTxt != null) {
				appsTxt.setText("");
			}
			docTypeCombo.deselectAll();
			showCharSetBtn.setEnabled(false);
			showValSetBtn.setEnabled(false);
			showTrainSetBtn.setEnabled(false);
			showAdvancedParsBtn.setEnabled(false);
			updateParamTable(null);
			updateChart(null);
			finalTrainCerLbl.setText("CER on Train Set:");
			finalValCerLbl.setText("CER on Validation Set:");
			return;
		}
		
		nameTxt.setText(StrUtil.get(model.getName()));
		langEditor.setLanguageString(model.getLanguage());
		descTxt.setText(StrUtil.get(model.getDescriptions() == null ? "" : model.getDescriptions().get(Locale.ENGLISH.getLanguage())));
		nrOfWordsTxt.setText(model.getNrOfWords() > 0 ? "" + model.getNrOfWords() : NOT_AVAILABLE);
		nrOfLinesTxt.setText(model.getNrOfLines() > 0 ? "" + model.getNrOfLines() : NOT_AVAILABLE);
		
		DocType docType = DocType.fromString(model.getDocType());
		logger.debug("Setting docType: {} -> {}", model.getDocType(), docType);
		for(int i = 0; i < docTypeCombo.getItemCount(); i++) {
			if(docTypeCombo.getData(docTypeCombo.getItem(i)).equals(docType)) {
				docTypeCombo.select(i);
				break;
			}
		}
		
		ReleaseLevel releaseLevel = ReleaseLevel.fromString(model.getReleaseLevel());
		if(publishStateCombo != null) {
			for(int i = 0; i < publishStateCombo.getItemCount(); i++) {
				if(publishStateCombo.getData(publishStateCombo.getItem(i)).equals(releaseLevel)) {
					publishStateCombo.select(i);
					break;
				}
			}
		}
		if(appsTxt != null) {
			appsTxt.setText(model.getApps() == null ? "" : model.getApps().stream().collect(Collectors.joining(",")));
		}
		if (ratingCombo != null) {
			ratingCombo.combo.select(model.getInternalRating()==null ? 0 : model.getInternalRating());
		}
		
		showAdvancedParsBtn.setEnabled(model.getProvider().equals(ModelUtil.PROVIDER_PYLAIA));
		
		Properties params = new Properties();
		if(model.getParameters() != null) {
			params.putAll(model.getParameters());
		}
		
		updateParamTable(new TrpProperties(params));

		showCharSetBtn.setEnabled(model.getSymbols() != null && !model.getSymbols().isEmpty());
		
		showTrainSetBtn.setEnabled(model.isGtAccessible());
		showValSetBtn.setEnabled(model.isGtAccessible());
		
		finalTrainCerLbl.setText(ModelUtil.getLossLabelForModel(model)+" on Train Set:");
		finalValCerLbl.setText(ModelUtil.getLossLabelForModel(model)+" on Validation Set:");		
		
		updateChart(model);
		
		enableMetadataEditing(store.isAdminLoggedIn() || store.getUserId() == model.getUserId());
	}
	
	private void enableMetadataEditing(boolean enabled) {
		updateMetadataBtn.setEnabled(enabled);
		Text[] mdTextFields = {
				nameTxt,
				descTxt
			};
		for(Text t : mdTextFields) {
			t.setEditable(enabled);
		}
		langEditor.setEnabled(enabled);
	}
	
	private TableItem addTableItem(Table paramTable, String key, String value) {
		TableItem item = new TableItem(paramTable, SWT.NONE);
		item.setText(0, key);
		item.setText(1, value);
		return item;
	}
	
	private void updateParamTable(TrpProperties paramsProps) {
		paramTable.removeAll();
		if (model == null) {
			return;
		}
		
		if (paramsProps == null || paramsProps.isEmpty()) {
			addTableItem(paramTable, NOT_AVAILABLE, NOT_AVAILABLE);
		} else {
			if (model.getProvider().equals(ModelUtil.PROVIDER_PYLAIA)) {
				TextFeatsCfg textFeatsCfg = TextFeatsCfg.fromConfigString2(paramsProps.getProperty("textFeatsCfg"));
				PyLaiaCreateModelPars createModelPars = PyLaiaCreateModelPars.fromSingleLineString2(paramsProps.getProperty("createModelPars"));
				PyLaiaTrainCtcPars trainCtcPars = PyLaiaTrainCtcPars.fromSingleLineString2(paramsProps.getProperty("trainCtcPars"));

				// add fixed pars:
				if (trainCtcPars!=null) {
					addTableItem(paramTable, "Max epochs", trainCtcPars.getParameterValue("--max_epochs"));
					addTableItem(paramTable, "Early stopping", trainCtcPars.getParameterValue("--max_nondecreasing_epochs"));
					if (model.getCerLog()!=null) {
						addTableItem(paramTable, "Epochs trained", ""+model.getCerLog().size());	
					}
					
					String lrStr = trainCtcPars.getParameterValue("--learning_rate");
					try {
						addTableItem(paramTable, "Learning rate", CoreUtils.formatDoubleNonScientific(Double.valueOf(lrStr)));
					} catch (Exception e) {
						addTableItem(paramTable, "Learning rate", lrStr);
					}
					addTableItem(paramTable, "Batch size", trainCtcPars.getParameterValue("--batch_size"));			
				}
				
				if (textFeatsCfg!=null) {
					addTableItem(paramTable, "Normalized height", ""+textFeatsCfg.getNormheight());
				}
				
				String baseModelStr = "";
				if (paramsProps.containsKey(HtrPyLaiaUtils.BASE_MODEL_ID_KEY)) {
					baseModelStr += paramsProps.getProperty(HtrPyLaiaUtils.BASE_MODEL_ID_KEY);
				}
				if (paramsProps.containsKey(HtrPyLaiaUtils.BASE_MODEL_NAME_KEY)) {
					baseModelStr += (baseModelStr.isEmpty() ? "" : " / ") + paramsProps.getProperty(HtrPyLaiaUtils.BASE_MODEL_NAME_KEY);
				}
				if (!StringUtils.isEmpty(baseModelStr)) {
					addTableItem(paramTable, "Base model", baseModelStr);
				}
			} else {
				for (String s : CITLAB_TRAIN_PARAMS) {
					if (paramsProps.containsKey(s)) {
						addTableItem(paramTable, s + " ", paramsProps.getProperty(s));
					}
				}
			}
			
			// those are common parameters (PyLaia and HTR+):
			
			// omitted tags:
			if (paramsProps.containsKey(HtrTrainConfig.OMITTED_TAGS_KEY)) {
				addTableItem(paramTable, HtrTrainConfig.OMITTED_TAGS_KEY, paramsProps.getProperty(HtrTrainConfig.OMITTED_TAGS_KEY));
			}
			
			// reverse text params:
			if (paramsProps.getBoolProperty(JobConst.PROP_REVERSE_TEXT)) {
				String val = "exclude-digits: "+paramsProps.getBoolProperty(JobConst.PROP_REVERSE_TEXT_EXCLUDE_DIGITS);
				if (paramsProps.containsKey(JobConst.PROP_REVERSE_TEXT_TAG_EXCEPTIONS)) {
					val += ", tag-exceptions: " + paramsProps.getProperty(JobConst.PROP_REVERSE_TEXT_TAG_EXCEPTIONS);	
				}
				addTableItem(paramTable, "Text reversed", val);
			}
			
		}
		paramTable.getColumn(0).pack();
		paramTable.getColumn(1).pack();
	}

	private void updateChart(final TrpModelMetadata model) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		String storedHtrTrainCerStr = NOT_AVAILABLE;
		String storedHtrValCerStr = NOT_AVAILABLE;

		CerLogAdapter htr = new CerLogAdapter(model);
		
		if (htr != null && htr.hasCerLog()) {
			XYSeries series = buildXYSeries(ModelUtil.getLossLabelForModel(model)+" Train", htr.getCerLog());
			dataset.addSeries(series);
		}

		if (htr != null && htr.hasCerTestLog()) {
			XYSeries valSeries = buildXYSeries(ModelUtil.getLossLabelForModel(model)+" Validation", htr.getCerTestLog());
			dataset.addSeries(valSeries);
		}
		
		String yAxisLabel = ModelUtil.getLossLabelForModel(model);
		chart = ChartFactory.createXYLineChart("Learning Curve", "Epochs", yAxisLabel, dataset,
				PlotOrientation.VERTICAL, true, true, false);
		XYPlot plot = (XYPlot) chart.getPlot();
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		DecimalFormat pctFormat = new DecimalFormat("#%");
		rangeAxis.setNumberFormatOverride(pctFormat);
		rangeAxis.setRange(0.0, 1.0);
		
		double[] referenceSeries = htr.getReferenceSeries();
		if(referenceSeries != null && referenceSeries.length > 0) {
			
			int storedNetEpoch;
			if(ModelUtil.PROVIDER_CITLAB_PLUS.equals(htr.getProvider()) || ModelUtil.PROVIDER_PYLAIA.equals(htr.getProvider())) {
				//HTR+ always uses model from last training iteration
				storedNetEpoch = referenceSeries.length;
			}
			
			else {
				
				//legacy routine, working for HTR and PyLaia but not HTR+! Find min value in referenceSeries
				storedNetEpoch = htr.getMinCerEpoch();
			}

			try {
				if(storedNetEpoch > 0) {
					logger.debug("best net stored after epoch {}", storedNetEpoch);
					int seriesIndex = 0;
					if(htr.hasCerLog()) {
						double storedHtrTrainCer = htr.getCerLog()[storedNetEpoch - 1];
						storedHtrTrainCerStr = HtrCITlabUtils.formatCerVal(storedHtrTrainCer);
						plot.getRenderer().setSeriesPaint(seriesIndex++, java.awt.Color.BLUE);
					}
					
					if (htr.hasCerTestLog()) {
						double storedHtrValCer = htr.getCerTestLog()[storedNetEpoch - 1];
						storedHtrValCerStr = HtrCITlabUtils.formatCerVal(storedHtrValCer);
						plot.getRenderer().setSeriesPaint(seriesIndex++, java.awt.Color.RED);
					}
					
					//annotate storedNetEpoch in the chart
					XYLineAnnotation lineAnnot = new XYLineAnnotation(storedNetEpoch, 0.0, storedNetEpoch, 100.0,
							new BasicStroke(), java.awt.Color.GREEN);
					lineAnnot.setToolTipText("Stored HTR");
					plot.addAnnotation(lineAnnot);
				}
			} catch (Exception e) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "Cannot determine best net epoch: "+e.getMessage());
			}
		} else {
			plot.setNoDataMessage("No data available");
		}
		
		jFreeChartComp.setChart(chart);
		triggerChartUpdate();

		finalTrainCerTxt.setText(storedHtrTrainCerStr);
		finalValCerTxt.setText(storedHtrValCerStr);
	}
	
	void triggerChartUpdate() {
		if(chart != null) {
			chart.fireChartChanged();
		}
	}

	private XYSeries buildXYSeries(String name, double[] cerLog) {
		XYSeries series = new XYSeries(name);
		series.setDescription(name);
		// build XYSeries
		for (int i = 0; i < cerLog.length; i++) {
			double val = cerLog[i];
			series.add(i + 1, val);
		}
		return series;
	}
	
	
	private void addListeners() {
		SWTUtil.onSelectionEvent(showAdvancedParsBtn, e -> {
			if(model == null || !model.getProvider().equals(ModelUtil.PROVIDER_PYLAIA)) {
				return;
			}
			
			Properties paramsProps = new Properties();
			if(model.getParameters() != null) {
				paramsProps.putAll(model.getParameters());
			}
			TextFeatsCfg textFeatsCfg = TextFeatsCfg.fromConfigString2(paramsProps.getProperty("textFeatsCfg"));
			TrpPreprocPars trpPreprocPars = TrpPreprocPars.fromJson2(paramsProps.getProperty("trpPreprocPars"));
			PyLaiaCreateModelPars createModelPars = PyLaiaCreateModelPars.fromSingleLineString2(paramsProps.getProperty("createModelPars"));
			PyLaiaTrainCtcPars trainCtcPars = PyLaiaTrainCtcPars.fromSingleLineString2(paramsProps.getProperty("trainCtcPars"));
			
			PyLaiaAdvancedConfDialog d = new PyLaiaAdvancedConfDialog(getShell(), textFeatsCfg, trpPreprocPars, createModelPars, trainCtcPars);
			d.open();
		});
		
		this.showTrainSetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(model == null) {
					return;
				}
				if (trainDocViewer != null) {
					trainDocViewer.setVisible();
				} else {
					try {
						TrpDoc doc = store.getModelDataSetAsDoc(model, DataSetType.TRAIN);
						trainDocViewer = new DocImgViewerDialog(getShell(), "Train Set", doc);
						trainDocViewer.open();
					} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException
							| NoConnectionException e1) {
						logger.error(e1.getMessage(), e);
					}

					trainDocViewer = null;
				}
				super.widgetSelected(e);
			}
		});
		
		this.showValSetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(model == null) {
					return;
				}
				if (valDocViewer != null) {
					valDocViewer.setVisible();
				} else {
					try {
						TrpDoc doc = store.getModelDataSetAsDoc(model, DataSetType.VALIDATION);
						valDocViewer = new DocImgViewerDialog(getShell(), "Validation Set", doc);
						valDocViewer.open();
					} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException
							| NoConnectionException e1) {
						logger.error(e1.getMessage(), e);
					}

					valDocViewer = null;
				}
				super.widgetSelected(e);
			}
		});

		this.showCharSetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(model == null) {
					return;
				}
				if (charSetViewer != null) {
					charSetViewer.setVisible();
					charSetViewer.update(model);
				} else {
					charSetViewer = new CharSetViewerDialog(getShell(), model);
					charSetViewer.open();
					charSetViewer = null;
				}
			}
		});
		
		this.updateMetadataBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!validator.hasInputChanged()) {
					logger.debug("No changes. Ignoring {}", e);
					return;
				}
				updateMetadata();
			}
		});
	}
	
	private void updateMetadata() {
		if(!validator.isInputValid()) {
			final String msg = validator.getValidationErrorMessages().stream().collect(Collectors.joining("\n"));
			DialogUtil.showBalloonToolTip(updateMetadataBtn, SWT.ICON_WARNING, "Invalid input", msg);
			return;
		}
		TrpModelMetadata modelToStore = new TrpModelMetadata(ModelDetailsWidget.this.model);
		modelToStore.setModelId(model.getModelId());
		modelToStore.setName(nameTxt.getText());
		modelToStore.getDescriptions().put(Locale.ENGLISH.getLanguage(), descTxt.getText());
		modelToStore.setLanguage(langEditor.getLanguageString());
		
		if(publishStateCombo != null) {
			Object publishStateData = publishStateCombo.getData(publishStateCombo.getText());
			ReleaseLevel releaseLevel = (ReleaseLevel) publishStateData;
			
			//check if there is a change that would publish a private data set
			if(!ReleaseLevel.isPrivateDataSet(releaseLevel) 
					&& ReleaseLevel.isPrivateDataSet(releaseLevel)) {
				final int answer = DialogUtil.showYesNoDialog(getShell(), "Are you sure you want to publish your data?", 
						"The new visibility setting will allow other users to access the data sets used to train this model!\n\n"
						+ "Are you sure you want to save this change?", SWT.ICON_WARNING);
				if(answer == SWT.NO) {
					logger.debug("User denied publishing the data.");
					return;
				}
			}
			
			modelToStore.setReleaseLevel(((ReleaseLevel) publishStateData).toString());
		}
		if (ratingCombo != null) {
			Integer rating = ratingCombo.combo.getSelectionIndex()==0 ? null : ratingCombo.combo.getSelectionIndex();
			logger.debug("setting rating to "+rating);
			modelToStore.setInternalRating(rating);
		}
		if (appsTxt != null) {
			final String appsStr = appsTxt.getText();
			List<String> apps = new ArrayList<>();
			if(!StringUtils.isEmpty(appsStr)) {
				apps = CoreUtils.parseStringList(appsStr, ",", true, true);
			}
			logger.debug("Setting models.apps = {}", apps);
			modelToStore.setApps(apps);
		}
		
		if(store.isAdminLoggedIn()) {
			DocType docType = (DocType) docTypeCombo.getData(docTypeCombo.getText());
			modelToStore.setDocType(docType.toString());
		}
		
		try {
			TrpModelMetadata storedModel = store.updateModelMetadata(modelToStore);
			logger.debug("HTR updated: {}", storedModel);
			//reset the text fields to new values
			updateDetails(modelToStore);
			DialogUtil.showBalloonToolTip(updateMetadataBtn, SWT.ICON_INFORMATION, "", "Changes saved.");
		} catch(Exception ex) {
			DialogUtil.showDetailedErrorMessageBox(getShell(), "Error while saving metadata", "HTR metadata could not be updated.", ex);
		}
	}
	
	/**
	 * Checks all editable text fields for unsaved changes and bothers the user with a yes/no-dialog that allows to save or discard changes.
	 */
	public void checkForUnsavedChanges() {
		if(!validator.hasInputChanged()) {
			//no changes. Go on
			return;
		}
		int answer = DialogUtil.showYesNoDialog(getShell(), "Unsaved Changes", 
				"You have edited the metadata of this model. Do you want to save the changes?", SWT.ICON_WARNING);
		if(answer == SWT.YES) {
			updateMetadata();
		}
	}
	
	private static class CerLogAdapter {
		final TrpModelMetadata model;
		double[] referenceSeries;
		double[] cerLog;
		double[] cerValidationLog;
		public CerLogAdapter(TrpModelMetadata model) {
			this.model = model;
			if(model != null) {
				this.cerLog = toDoubleArray(model.getCerLog());
				this.cerValidationLog = toDoubleArray(model.getCerValidationLog());
				referenceSeries = null;
				if (hasCerLog()) {
					referenceSeries = cerLog;
				}
				if (hasCerTestLog()) {
					//if available then validation CER is reference for stored net
					referenceSeries = cerValidationLog;
				}
			} else {
				this.cerLog = null;
				this.cerValidationLog = null;
				this.referenceSeries = null;
			}
			logger.debug("cerLog = {}", cerLog);
			logger.debug("cerValidationLog = {}", cerValidationLog);
			logger.debug("referenceSeries = {}", referenceSeries);
		}
		private double[] toDoubleArray(List<Double> cerLog) {
			if(CollectionUtils.isEmpty(cerLog)) {
				return null;
			}
			double[] a = new double[cerLog.size()];
			for(int i = 0; i < cerLog.size(); i++) {
				a[i] = cerLog.get(i);
			}
			return a;
		}
		public double[] getReferenceSeries() {
			return referenceSeries == null ? null : referenceSeries;
		}
		public String getProvider() {
			if(model == null) {
				return null;
			}
			return model.getProvider();
		}
		public double[] getCerTestLog() {
			return cerValidationLog;
		}
		public boolean hasCerTestLog() {
			return cerValidationLog != null && cerValidationLog.length > 0;
		}
		public double[] getCerLog() {
			return cerLog;
		}
		public boolean hasCerLog() {
			return cerLog != null && cerLog.length > 0;
		}
		
		private int getMinCerEpoch() {
			double min = Double.MAX_VALUE;
			int minCerEpoch = -1;
			if(model.isBestNetStored()) {
				//if best net is stored then seach reference CER series for the minimum value
				for (int i = 0; i < referenceSeries.length; i++) {
					final double val = referenceSeries[i];
					//HTR+ always stores best net. If validation CER does not change, the first net with this CER is kept
					if (val < min) {
						min = val;
						minCerEpoch = i + 1;
					}
				}
			} else {
				//set last epoch as minimum
				minCerEpoch = referenceSeries.length;
			}
			return minCerEpoch;
		}
	}
}
