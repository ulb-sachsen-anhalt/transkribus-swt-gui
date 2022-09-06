package eu.transkribus.swt_gui.htr;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.models.ModelsComposite;
import eu.transkribus.util.TextRecognitionConfig;
import eu.transkribus.util.TextRecognitionConfig.Mode;

public class HtrTextRecognitionConfigDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(HtrTextRecognitionConfigDialog.class);

	private HtrDictionaryComposite htrDictComp;
//	private HtrModelsComposite htrModelsComp;
	private ModelsComposite htrModelsComp;

	private TextRecognitionConfig config;
	
	Group dictGrp;
	SashForm sash;

	public HtrTextRecognitionConfigDialog(Shell parent, TextRecognitionConfig config) {
		super(parent);
		this.config = config;
	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		
		sash = new SashForm(cont, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sash.setLayout(new GridLayout(2, false));
		
		htrModelsComp = new ModelsComposite(sash, ModelUtil.TYPE_TEXT, null, 0);
		GridLayout gl = (GridLayout) htrModelsComp.getLayout();
		gl.marginHeight = gl.marginWidth = 0;
		htrModelsComp.setLayout(gl);
		
		htrModelsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		htrModelsComp.getModelTableWidget().getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				updateUi();
			}
		});
	
		Group dictGrp = new Group(sash, SWT.NONE);
		dictGrp.setLayout(new GridLayout(1, false));
		dictGrp.setText("Language Model");
		
		htrDictComp = new HtrDictionaryComposite(dictGrp, 0);
		htrDictComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		applyConfig();

		sash.setWeights(new int[] { 88, 12 });
		
		htrModelsComp.getModelTableWidget().getTableViewer().getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				okPressed();
			}
		});
		
		updateUi();
		
		/*
		 * for selecting the previously chosen HTR model in the paged tree when opening the htr selection dialog
		 */
		if (config!= null && config.getHtrId()!=0) {
			logger.debug("set the htr id to " + config.getHtrId());
			htrModelsComp.setSelection(config.getHtrId());
			htrModelsComp.getModelTableWidget().loadPage_useBinarySearch("modelId", config.getHtrId(), false);
		}

		return cont;
	}
	
	private void updateUi() {
		if(htrModelsComp.getSelectedModel() == null) {
			return;
		}
		final String provider = htrModelsComp.getSelectedModel().getProvider();
		if (provider.equals(ModelUtil.PROVIDER_PYLAIA)) {
//			sash.setWeights(new int[] { 100, 0 });
			htrDictComp.updateUi(false, true, false);
			sash.setWeights(new int[] { 88, 12 });			
		} else if (provider.equals(ModelUtil.PROVIDER_CITLAB_PLUS)
				|| provider.equals(ModelUtil.PROVIDER_CITLAB)) {
			//show option to select integrated dictionary if available for this model
			htrDictComp.updateUi(false, ModelUtil.hasModelLanguageModel(htrModelsComp.getSelectedModel()), true);
			sash.setWeights(new int[] { 88, 12 });
		} else {
			sash.setWeights(new int[] { 88, 12 });
		}
	}

	private void applyConfig() {
		if (config == null) {
			return;
		}
		
		Mode mode = config.getMode();
		switch (mode) {
		case CITlab:
			htrModelsComp.setSelection(config.getHtrId());
			
			TrpModelMetadata selHtr = htrModelsComp.getSelectedModel();
			boolean showLangModOption = selHtr != null && ModelUtil.hasModelLanguageModel(selHtr);
			htrDictComp.updateUi(false, showLangModOption, true);
			htrDictComp.updateSelection(config.getDictionary());
			break;
		case UPVLC:
			htrModelsComp.setSelection(config.getHtrId());
			htrDictComp.updateSelection(config.getLanguageModel());
			break;
		default:
			break;
		}
	}

	public TextRecognitionConfig getConfig() {
		return config;
	}
	
	private Mode getModeForProvider(String provider) {
		logger.debug("provider = "+provider);
				
		if (ModelUtil.PROVIDER_CITLAB.equals(provider) || ModelUtil.PROVIDER_CITLAB_PLUS.equals(provider)) {
			return Mode.CITlab;
		}
		if (ModelUtil.PROVIDER_PYLAIA.equals(provider)) {
			return Mode.UPVLC;
		}
		
		return null;
	}

	@Override
	protected void okPressed() {
		try {
			htrModelsComp.getModelDetailsWidget().checkForUnsavedChanges();
			TrpModelMetadata htr = htrModelsComp.getSelectedModel();
			
			if (htr != null) {
				Mode mode = getModeForProvider(htr.getProvider());
				if (mode == null) {
					DialogUtil.showErrorMessageBox(getShell(), "Error parsing mode from provider", "Unknown model provider: "+htr.getProvider());
					return;
				}
				config = new TextRecognitionConfig(mode);
				
				if (mode == Mode.CITlab) { // FIXME: set language model here once ready for CITlab recognition
					config.setDictionary(htrDictComp.getDictionarySetting());
				}
				else { // for PyLaia, only language model setting is relevant!
					config.setLanguageModel(htrDictComp.getLanguageModelSetting());	
				}
				
//				if (htr == null) {
//					DialogUtil.showErrorMessageBox(this.getParentShell(), "Error", "Please select a HTR.");
//					return;
//				}
				config.setHtrId(htr.getModelId());
				config.setHtrName(htr.getName());
				config.setLanguage(htr.getLanguage());				
			}
			else {
				logger.debug("model was probably deleted - setting config to null!");
				config = null;
			}
		} catch (Exception e) {
			logger.error("Error while setting HTR: "+e.getMessage(), e);
		}
		finally {
			super.okPressed();
		}
	}
	
	@Override
	protected void cancelPressed() {
		htrModelsComp.getModelDetailsWidget().checkForUnsavedChanges();
		super.cancelPressed();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Text Recognition Configuration");
		newShell.setMinimumSize(800, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1280, 768);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.APPLICATION_MODAL | SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
}
