package de.ulb.gtscribus.swt_gui.structure_tree;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import eu.transkribus.swt_gui.structure_tree.StructureTreeWidget;
import eu.transkribus.swt_gui.structure_tree.StructureTreeWidget.ColConfig;

/**
 * @author M3ssman
 */
public class StructureTreeWidgetTest {


	/**
	 * 
	 * Ensure modified List widget (left side) 
	 * still intact with three columns
	 * "Type", "Text" and "Coords"
	 * 
	 * @throws Exception
	 */
	@Test
    public void testStorageULBExporter() throws Exception {

        // arrange
        ColConfig[] columns = StructureTreeWidget.COLUMNS;

		// act
		List<String> colLabels = Arrays.asList(columns).stream().map(conf -> conf.name).collect(Collectors.toList());

        // assert
        assertEquals(3, columns.length);
		assertEquals("[Type, Text, Coords]", colLabels.toString());
    }

}
