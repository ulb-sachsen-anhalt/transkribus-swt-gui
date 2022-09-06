package eu.transkribus.swt_gui.credits;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.io.RemoteDocConst;
import eu.transkribus.core.model.beans.CostEstimationRepresentation;
import eu.transkribus.core.model.beans.DocSelection;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.enums.CreditSelectionStrategy;
import eu.transkribus.core.util.DocSelectionUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.util.TextRecognitionConfig;

public class CostEstimateMessageBuilder {
	private static final Logger logger = LoggerFactory.getLogger(CostEstimateMessageBuilder.class);
	
	private Storage store;

	public CostEstimateMessageBuilder() {
		this.store = Storage.getInstance();
	}
	
	public String buildHtrCostEstimateMessage(int colId, TrpDocMetadata docMd, String pages, TextRecognitionConfig config) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		Map<TrpDocMetadata, DocSelection> details = new HashMap<>();
		details.put(docMd, new DocSelection(docMd.getDocId(), pages, null, null));
		return buildHtrCostEstimateMessage(colId, details, config);
	}
	
	public String buildHtrCostEstimateMessage(int colId, Map<TrpDocMetadata, DocSelection> docSelectionDetails, TextRecognitionConfig config) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		//total pages
		int totalNrOfPages = DocSelectionUtil.sumDocSelectionNrOfPages(docSelectionDetails, null);
		//included sample doc pages
		int nrOfSampleDocPagesIncluded = DocSelectionUtil.sumDocSelectionNrOfPages(docSelectionDetails, 
				docMd -> docMd.getStatus() != null && docMd.getStatus().equals(RemoteDocConst.STATUS_SAMPLE_DOC));
		int nrOfPagesToCharge = totalNrOfPages - nrOfSampleDocPagesIncluded;
		logger.debug("Building HTR cost estimate message for: totalNrOfPages = {}, nrOfSampleDOcPagesIncluded = {}, nrOfPagesToCharge = {}", 
				totalNrOfPages, nrOfSampleDocPagesIncluded, nrOfPagesToCharge);
		CostEstimationRepresentation costs = null;
		if(nrOfPagesToCharge > 0) {
			costs = store.getConnection().getCreditCalls().getHtrCosts(colId, config.getHtrId(), nrOfPagesToCharge, null, config.isWriteKwsIndexFiles());
		}
		return buildCostEstimateMessage(costs, nrOfSampleDocPagesIncluded, "HTR");
	}
	
	public String buildOcrCostEstimateMessage(int colId, TrpDocMetadata docMd, String pages, String ocrType, boolean doBlockSegOnly) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		Map<TrpDocMetadata, DocSelection> details = new HashMap<>();
		details.put(docMd, new DocSelection(docMd.getDocId(), pages, null, null));
		return buildOcrCostEstimateMessage(colId, details, ocrType, doBlockSegOnly);
	}
	
	public String buildOcrCostEstimateMessage(int colId, Map<TrpDocMetadata, DocSelection> docSelectionDetails, String ocrType, boolean doBlockSegOnly) throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		//total pages
		int totalNrOfPages = DocSelectionUtil.sumDocSelectionNrOfPages(docSelectionDetails, null);
		//included sample doc pages
		int nrOfSampleDocPagesIncluded = DocSelectionUtil.sumDocSelectionNrOfPages(docSelectionDetails, 
				docMd -> docMd.getStatus() != null && docMd.getStatus().equals(RemoteDocConst.STATUS_SAMPLE_DOC));
		int nrOfPagesToCharge = totalNrOfPages - nrOfSampleDocPagesIncluded;
		logger.debug("Building OCR cost estimate message for: totalNrOfPages = {}, nrOfSampleDOcPagesIncluded = {}, nrOfPagesToCharge = {}", 
				totalNrOfPages, nrOfSampleDocPagesIncluded, nrOfPagesToCharge);
		CostEstimationRepresentation costs = null;
		if(nrOfPagesToCharge > 0) {
			 costs = store.getConnection().getCreditCalls().getOcrCosts(colId, nrOfPagesToCharge, doBlockSegOnly, ocrType, null);
		}
		return buildCostEstimateMessage(costs, nrOfSampleDocPagesIncluded, "OCR");
	}
	
	protected String buildCostEstimateMessage(CostEstimationRepresentation costs, int nrOfSampleDocPagesIncluded, String serviceName) {
		if(costs == null) {
			if(nrOfSampleDocPagesIncluded > 0) {
				return "\nNo credits will be consumed as all selected pages belong to sample documents.";
			}
			return "";
		}
		String msg = String.format("\nThe job will consume %,.2f credits from %s package(s) for %s on %s page(s).", 
				(costs.getCreditValue() * -1), costs.getChargedPackages().size(), serviceName, costs.getNrOfPages());
		if(nrOfSampleDocPagesIncluded > 0) {
			msg += String.format("\n%s sample document pages included in the selection are free of charge.", nrOfSampleDocPagesIncluded);
		}
		String note = "";
		if(!StringUtils.isEmpty(costs.getMessage())) {
			//server-side built messages
			note = costs.getMessage();
		} else if(CreditSelectionStrategy.USER_ONLY.toString().equals(costs.getSelectionStrategy())) {
			note = "Your personal credits will be charged.";
		} else if(CreditSelectionStrategy.COLLECTION_ONLY.toString().equals(costs.getSelectionStrategy())) {
			note = "The credits in the collection will be charged.";
		}
		if(!note.isEmpty()) {
			msg += "\n\n" + note;
		}
		return msg;
	}
}
