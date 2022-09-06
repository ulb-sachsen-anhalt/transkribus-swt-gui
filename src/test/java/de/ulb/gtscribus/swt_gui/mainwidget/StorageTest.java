package de.ulb.gtscribus.swt_gui.mainwidget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import javax.security.auth.login.LoginException;

import org.junit.Test;

import eu.transkribus.client.connection.TrpServerConn;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;



/**
 * 
 * Ensure only local Exports work out
 * 
 * @author M3ssman
 */
public class StorageTest {
    
    @Test
    public void testStorageULBExporter() throws Exception {

        // arrange
        Storage storage = ULBStorage.getInstance();

        // act
        Exception exc = assertThrows(RuntimeException.class, () -> {
            storage.exportDocument(null, null, null);
        });

        // assert
        assertEquals("No remote connection supported!", exc.getMessage());
    }
}

class ULBStorage extends Storage {

    private static ULBStorage store = new ULBStorage("local");

    public ULBStorage(String someString) {
        super(someString);
    }

    public static ULBStorage getInstance() {
        return ULBStorage.store;
    }

    public TrpServerConn getConnection() {
        try {
            return new TrpServerConn("no way");
        } catch (LoginException e) {
        }
        return null;
    }
}
