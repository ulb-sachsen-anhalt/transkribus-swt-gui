package de.ulb.gtscribus.core.util;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;

import org.junit.Test;

import de.ulb.gtscribus.core.io.format.XmlFormat;


/**
 * 
 * Specification for {@link ULBXmlUtils}
 * 
 * @author m3ssman
 *
 */
public class ULBXmlUtilsTest {
  
    @Test
    public void testGetXMLNamespacePAGE2019() throws Exception {

        // arrange
        Path thePath = Path.of("./src/test/resources/sample-page2019/page/16258167.xml");
        File theFile = thePath.toAbsolutePath().toFile();

        // act
        XmlFormat theFormat = ULBXmlUtils.getXmlFormat(theFile);

        // assert
        assertNotEquals(XmlFormat.UNKNOWN, theFormat);
        assertEquals(XmlFormat.PAGE_2019, theFormat);
    }

    @Test
    public void testGetXMLNamespacePAGE2013() throws Exception {

        // arrange
        Path thePath = Path.of("./src/test/resources/page/StAZ-Sign.2-1_001_with_alternatives.xml");
        File theFile = thePath.toAbsolutePath().toFile();

        // act
        XmlFormat theFormat = ULBXmlUtils.getXmlFormat(theFile);

        // assert
        assertNotEquals(XmlFormat.UNKNOWN, theFormat);
        assertEquals(XmlFormat.PAGE_2013, theFormat);
    }

    @Test
    public void testGetXMLNamespacePAGE2010() throws Exception {

        // arrange
        Path thePath = Path.of("./src/test/resources/page/002_080_001.xml");
        File theFile = thePath.toAbsolutePath().toFile();

        // act
        XmlFormat theFormat = ULBXmlUtils.getXmlFormat(theFile);

        // assert
        assertEquals(XmlFormat.PAGE_2010, theFormat);
    }

    @Test
    public void testGetXMLNamespaceALTOV3() throws Exception {

        // arrange
        Path thePath = Path.of("./src/test/resources/alto/1667522809_J_0025_0001.xml");
        File theFile = thePath.toAbsolutePath().toFile();

        // act
        XmlFormat theFormat = ULBXmlUtils.getXmlFormat(theFile);

        // assert
        assertNotEquals(XmlFormat.UNKNOWN, theFormat);
        assertEquals(XmlFormat.ALTO_3, theFormat);
    }

    @Test
    public void testGetXMLNamespaceALTOV4() throws Exception {

        // arrange
        Path thePath = Path.of("./src/test/resources/alto/urn+nbn+de+gbv+3+3-21437-p0001-0-737434.xml");
        File theFile = thePath.toAbsolutePath().toFile();

        // act
        XmlFormat theFormat = ULBXmlUtils.getXmlFormat(theFile);

        // assert
        assertNotEquals(XmlFormat.UNKNOWN, theFormat);
        assertEquals(XmlFormat.ALTO_4, theFormat);
    }

}
