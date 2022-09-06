package eu.transkribus.swt_gui.models;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpModelMetadata;

public class ShareModelMetadataDialog extends ShareModelDialog {

	public ShareModelMetadataDialog(Shell parentShell, TrpModelMetadata model) {
		super(parentShell, model);
	}

	protected void addModelToCollection(int colId)
			throws TrpClientErrorException, TrpServerErrorException, SessionExpiredException {
		store.getConnection().getModelCalls().addModelToCollection(getModel(), colId);
	}

	protected void removeModelFromCollection(int colId)
			throws TrpClientErrorException, TrpServerErrorException, SessionExpiredException {
		store.getConnection().getModelCalls().removeModelFromCollection(getModel(), colId);
	}

	protected List<TrpCollection> getCollectionList()
			throws TrpServerErrorException, TrpClientErrorException, SessionExpiredException {
		return store.getConnection().getModelCalls().getModelCollections(getModel());
	}
}
