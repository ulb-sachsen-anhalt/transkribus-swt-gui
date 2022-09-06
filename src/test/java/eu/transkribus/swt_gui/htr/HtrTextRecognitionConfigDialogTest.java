package eu.transkribus.swt_gui.htr;

import java.util.concurrent.Future;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import eu.transkribus.client.connection.ATrpServerConn;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class HtrTextRecognitionConfigDialogTest {

	public static void main(String[] args) throws Exception {
		Storage store=null;
		try {
			store = Storage.getInstance();
			store.updateProxySettings();
			store.login(ATrpServerConn.PROD_SERVER_URI, args[0], args[1]);
			Future<?> fut = store.reloadDocList(2); // reload doclist of a collection just that the collection id gets set!
			fut.get();
			
			ApplicationWindow aw = new ApplicationWindow(null) {
				@Override
				protected Control createContents(Composite parent) {
					getShell().setSize(300, 200);
					SWTUtil.centerShell(getShell());
					
					System.out.println(Storage.getInstance().loadTextRecognitionConfig());
					
					HtrTextRecognitionConfigDialog diag = new HtrTextRecognitionConfigDialog(getShell(), null);
					
//					HtrModelsDialog diag = new HtrModelsDialog(getShell());
					if (diag.open() == Dialog.OK) {
//						System.out.println("selected model: "+diag.getSelectedHtr());
					}
					
//					Text2ImageConfDialog diag = new Text2ImageConfDialog(getShell());
//					if (diag.open() == Dialog.OK) {
//						System.out.println("conf: "+diag.getConfig());
//					}
	
					return parent;
				}
			};
			aw.setBlockOnOpen(true);
			aw.open();
	
			Display.getCurrent().dispose();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (store!=null) {
				store.logout();
			}
		}
	}

}
