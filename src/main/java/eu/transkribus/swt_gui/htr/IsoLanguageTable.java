package eu.transkribus.swt_gui.htr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.IsoLangUtils;
import eu.transkribus.swt.mytableviewer.ColumnConfig;
import eu.transkribus.swt.mytableviewer.MyTableViewer;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.TableLabelProvider;

public class IsoLanguageTable extends Composite {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IsoLanguageTable.class);
	
	MyTableViewer tv;
	String languageString;
	Text addLangText;
	Button addLangBtn;
	Button removeLangBtn;
	
	List<String> availableLangsPlusIsoCode;
	SimpleContentProposalProvider scp;
	
	public IsoLanguageTable(Composite parent, int style, String languageString) {
		super(parent, 0);
		this.setLayout(SWTUtil.createGridLayout(3, false, 0, 0));
		
		Label l = new Label(this, 0);
		l.setText("Current languages:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		Fonts.setBoldFont(l);
		
		tv = new MyTableViewer(this, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tv.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		tv.getTable().setLinesVisible(true);
		tv.getTable().setHeaderVisible(true);
		tv.setContentProvider(new ArrayContentProvider());

		createColumns();
		
		Label l1 = new Label(this, 0);
		l1.setText("Add Language:");
		l1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		Fonts.setBoldFont(l1);
		
		addLangText = new Text(this, SWT.SINGLE | SWT.BORDER);
		addLangText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		initAutoCompletion(addLangText, null);
		addLangText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setProposals(addLangText.getText());
			}
		});
		addLangBtn = new Button(this, 0);
		addLangBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		addLangBtn.setImage(Images.ADD);
		addLangBtn.setToolTipText("Add language to list");
		SWTUtil.onSelectionEvent(addLangBtn, e -> {
			addLanguage();
		});
		
		removeLangBtn = new Button(this, 0);
		removeLangBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		removeLangBtn.setText("Remove selected language");
		removeLangBtn.setImage(Images.DELETE);
		SWTUtil.onSelectionEvent(removeLangBtn, e -> {
			removeSelectedLanguage();
		});
		
		setLanguageString(languageString);
	}
	
	private void removeSelectedLanguage() {
		List<String> selLangs = ((IStructuredSelection) tv.getSelection()).toList();
		List<String> langs = getCurrentLanguages();
		for (String selLang : selLangs) {
			logger.debug("removing langauge: "+selLang);
			langs.remove(selLang);
		}
		setLanguageString(CoreUtils.join(langs));
	}

	private void addLanguage() {
		String langStr = addLangText.getText();
		if (StringUtils.isEmpty(langStr)) {
			return;
		}
		
		String iso = langStr.split("-")[0].trim();
		String lang = IsoLangUtils.DEFAULT_RESOLVER.resolveLabelFromCode(iso);
		if (lang == null) {
			if (iso.contains(",")) {
				DialogUtil.showErrorMessageBox(getShell(), "No commas allowed", "No commas allowed are allowed for custom languages!");
				return;
			}	
			iso = langStr.trim();
			int answer = DialogUtil.showYesNoDialog(getShell(), "ISO code not found", "The ISO-639-2 code for language '"+iso+"' was not found - do you want to add it as a custom language?");
			if (answer != DialogUtil.YES) {
				return;
			}
		}
		
		if (getCurrentLanguages().contains(iso)) {
			DialogUtil.showInfoMessageBox(getShell(), "Already added", "The language is already in the list");
			return;
		}
		
		String newLangString = this.languageString==null ? "" : this.languageString.trim();
		if (!StringUtils.isEmpty(newLangString)) {
			newLangString += ",";
		}
		newLangString += iso;
		setLanguageString(newLangString);
		addLangText.setText("");
	}

	private List<String> getAvailableLanguageListWithLeadingIsoCodeSorted() {
		List<String> l = new ArrayList<>();
		Map<String, String> map = IsoLangUtils.DEFAULT_RESOLVER.getCodeToLabelMap();
		for (String iso : map.keySet()) {
			l.add(iso+" - "+map.get(iso));
		}
		Collections.sort(l);
		return l;
	}
	
	private String[] getProposals(String value) {
		List<String> props = availableLangsPlusIsoCode;
		if (!StringUtils.isEmpty(value)) {
			props = availableLangsPlusIsoCode.stream().filter(l -> l.toLowerCase().contains(value.toLowerCase())).collect(Collectors.toList());
		}
		return props.toArray(new String[0]);
	}
	
	private void setProposals(String langText) {
//		logger.info("setProposals, langText = "+langText);
		scp.setProposals(getProposals(langText));
	}
	
	private void initAutoCompletion(Text text, String value) {
		availableLangsPlusIsoCode = getAvailableLanguageListWithLeadingIsoCodeSorted();
		ContentProposalAdapter adapter = null;
		String[] defaultProposals = getProposals(value);
		
		scp = new SimpleContentProposalProvider(defaultProposals);
		scp.setProposals(defaultProposals);
//			KeyStroke ks = KeyStroke.getInstance("Ctrl+Space");
		adapter = new ContentProposalAdapter(text, new TextContentAdapter(), scp, null, null);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}
	
	public void setLanguageString(String languageString) {
		this.languageString = languageString==null ? "" : languageString;
		List<String> langs = getCurrentLanguages();
		logger.debug("parsed "+langs.size()+" languages");
		tv.setInput(langs);
	}
	
	public String getLanguageString() {
		return this.languageString;
	}
	
	public List<String> getCurrentLanguages() {
		return CoreUtils.parseStringList(languageString, ",", true, true);
	}

	private void createColumns() {
		final ColumnConfig[] cols = new ColumnConfig[] {
			new ColumnConfig("ISO-639-2", 100, true),
			new ColumnConfig("Language", 300, true),
		};
		tv.addColumns(cols);
		
		tv.setLabelProvider(new TableLabelProvider() {
			@Override public String getColumnText(Object element, int columnIndex) {
				if (!(element instanceof String)) {
					return "i am error";
				}
				
				String iso = (String) element;
				String lang = IsoLangUtils.DEFAULT_RESOLVER.resolveLabelFromCode(iso);
				
				if (columnIndex == 0) {
					return lang != null ? iso : lang;
				}
				else {
					return lang != null ? lang : iso;
				}
			}
		});
	}

}
