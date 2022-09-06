package de.ulb.gtscribus.core.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.transkribus.core.io.LocalDocConst;
import de.ulb.gtscribus.core.io.LocalDocReader.DocLoadConfig;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;

/**
 * 
 * Specification for {@link LocalDocReader}
 * 
 * @author m3ssman
 *
 */
public class LocalDocReaderTest {
	
	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();
	
	@Test
	public void loadSingleImageFileAsDoc() throws IOException {
		DocLoadConfig config = new DocLoadConfig();
		config.forceCreatePageXml = false;
		config.writeDocXml = false;
		config.ignoreDocXml = true;
		
		Assert.assertThrows(IOException.class, () -> {
			LocalDocReader.load("src/test/resources/TrpTestDoc_20131209/StAZ-Sign.2-1_003.xml", null);	
		});

		// arrange
		String tmpFolder = "TrpTestDoc_20131209";
		Path srcPath = Path.of("./src/test/resources/TrpTestDoc_20131209");
		File srcDir = srcPath.toAbsolutePath().toFile();
		File dstDir = tmpDir.newFolder(tmpFolder);
		FileUtils.copyDirectory(srcDir, dstDir);

		// act
		TrpDoc d = LocalDocReader.load(dstDir.toString(), config);

		// assert
		// why the heck - the main folder *does* contain 4 Images, not 1
		Assert.assertEquals(4, d.getNPages());
	}
	
	/**
	 * 
	 * Load fails if XML exists but is invalid
	 * 
	 * @throws IOException
	 */
	@Test(expected = IOException.class)
	public void testLoadFailsOnInvalidFile() throws IOException {
		String projectName = "1667522809_J_0013_0001";
		File projectFolder = tmpDir.newFolder(projectName);
		File altoSubFolder = new File(projectFolder, "alto");
		File altoFile = new File(altoSubFolder, "1667522809_J_0013_0001.xml");
		FileUtils.writeStringToFile(altoFile, "");
		String imageName = "1667522809_J_0013_0001";
		File tifFile = new File(projectFolder, imageName + ".tif");
		BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY); 
		ImageIO.write(bufferedImage, "TIFF", tifFile);
		
		String projectPath = projectFolder.getAbsolutePath();
		LocalDocReader.load(projectPath);
	}
	
	/**
	 * 
	 * Test: 
	 * Exact match of image name and XML name (minus file extensions):
	 * XML and Image can be matched
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoadALTOFileWithSameName() throws IOException {
		String projectName = "1667522809_J_0013_0001";
		File projectFolder = tmpDir.newFolder(projectName);
		File altoSubFolder = new File(projectFolder, "alto");
		File altoFile = new File(altoSubFolder, "1667522809_J_0013_0001.xml");
		FileUtils.writeStringToFile(altoFile, "");
		
		String imageName = "1667522809_J_0013_0001";
		File actual = LocalDocReader.findXml(imageName, altoSubFolder, true, false);
		
		assertNotNull(actual);
		assertEquals("1667522809_J_0013_0001.xml", actual.getName());
	}
	
	/**
	 * 
	 * Test: 
	 * Put multiple matching files in the folder, prefer the one with an exact match.
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * @throws IOException
	 */	
	@Test
	public void testLoadALTOFilePreferSameName() throws IOException {
		String projectName = "1667522809_J_0013_0001";
		File projectFolder = tmpDir.newFolder(projectName);
		File altoSubFolder = new File(projectFolder, "alto");
		File altoFile = new File(altoSubFolder, "1667522809_J_0013_0001.xml");
		FileUtils.writeStringToFile(altoFile, "");
		File altoFile2 = new File(altoSubFolder, "0_i_am_also_matching_but_listed_before_1667522809_J_0013_0001.xml");
		FileUtils.writeStringToFile(altoFile2, "");
		
		String imageName = "1667522809_j_0013_0001"; // note the lowercase 'j' letter -> case insensitity!
		File actual = LocalDocReader.findXml(imageName, altoSubFolder, true, true);
		
		assertNotNull(actual);
		assertEquals("1667522809_J_0013_0001.xml", actual.getName());
	}	
	
	/**
	 * 
	 * Test: 
	 * Put multiple matching files in the folder, prefer the one with an exact match.
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * @throws IOException
	 */	
	@Test
	public void testLoadALTOFileCaseInsensitive() throws IOException {
		String projectName = "1667522809_J_0013_0001";
		File projectFolder = tmpDir.newFolder(projectName);
		File altoSubFolder = new File(projectFolder, "alto");
		File altoFile = new File(altoSubFolder, "i_am_a_file.xml");
		FileUtils.writeStringToFile(altoFile, "");
		
		String imageName = "I_AM_A_FILE";
		File actual = LocalDocReader.findXml(imageName, altoSubFolder, true, true);
		
		assertNotNull(actual);
		assertEquals("i_am_a_file.xml", actual.getName());
	}	

	
	/**
	 * 
	 * Test: 
	 * Match image name and XML name with additional extension?
	 * XML and Image can be matched
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoadALTOFileWithGTExtension() throws IOException {
		String projectName = "1667522809_J_0013_0001";
		File projectFolder = tmpDir.newFolder(projectName);
		File altoSubFolder = new File(projectFolder, "alto");
		File altoFile = new File(altoSubFolder, "1667522809_J_0013_0001.gt.art.xml");
		FileUtils.writeStringToFile(altoFile, "");
		
		String imageName = "1667522809_J_0013_0001";
		File actual = LocalDocReader.findFile(imageName, altoSubFolder, "xml", true);
		
		assertNotNull(actual);
		assertEquals("1667522809_J_0013_0001.gt.art.xml", actual.getName());
	}
	
	
	/**
	 * 
	 * Test: 
	 * Match image name and XML name with large additional name section?
	 * XML and Image can be matched
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoadALTOFileWithLargeSuffix() throws IOException {
		String projectName = "1667522809_J_0025_0384";
		File projectFolder = tmpDir.newFolder(projectName);
		File altoSubFolder = new File(projectFolder, "alto");
		File altoFile = new File(altoSubFolder, "1667522809_J_0025_0384_2350x350_6425x5300.xml");
		FileUtils.writeStringToFile(altoFile, "");
		
		String imageName = "1667522809_J_0025_0384";
		File actual = LocalDocReader.findFile(imageName, altoSubFolder, "xml", true);
		
		assertNotNull(actual);
		assertEquals("1667522809_J_0025_0384_2350x350_6425x5300.xml", actual.getName());
	}
	
	/**
	 * 
	 * Test: 
	 * 
	 * shape polygon points in ALTO are properly converted into PAGE Coord
	 * 
	 * 
	 * @throws IOException, JDOMException
	 */
	@Test
	public void testConvertALTOShapePolygon() throws IOException, JDOMException {
		// arrange
		String projectName = "al_Fatawa_Page_024";
		File projectFolder = this.tmpDir.newFolder(projectName);
		writeImage(projectFolder.getAbsolutePath(), projectName, "tif", 71, 108);
		File altoSubDir = new File(projectFolder, "alto");
		altoSubDir.mkdir();
		File sourceAlto = new File("src/test/resources/alto/al_Fatawa_Page_024.xml");
		File targetAlto = altoSubDir.toPath().resolve(projectName + ".xml").toFile();
		com.google.common.io.Files.copy(sourceAlto, targetAlto);

		// act
		TrpDoc result = LocalDocReader.load(projectFolder.getAbsolutePath());
		
		// assert
		TrpPage pageOne = result.getPages().get(0);
		TrpTranscriptMetadata transcription = pageOne.getCurrentTranscript();
		URL urlPageFile = transcription.getUrl();
		assertNotNull(urlPageFile);
		String urlLocal = urlPageFile.toString().replace("file:", "");
		Document doc = readXMLDocument(urlLocal);
		XPathExpression<Element> xpath = generateXpression(".//page2013:TextLine/page2013:Coords");
		List<Element> els = xpath.evaluate(doc);
		assertEquals(24, els.size());
		// if suceeded, the first text line coords should have
		// actually a lot of points more than 4 pairs if it is 
		// shrunk to a bounding box with just 4 corners
		for (Element el : els) {
			assertTrue(el.getAttributeValue("points").split(" ").length > 4);
		}
		// Alas! First Textline spans over 74(!) points
		assertEquals(74, els.get(0).getAttributeValue("points").split(" ").length);
	}

	/**
	 * 
	 * Test: 
	 * Match image name and XML name with additional extension?
	 * XML and Image can be matched
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * see issue #57
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFindPageXmlFileWithImageNameOverlap() throws IOException {
		String projectName = "some_new_doc";
		String imageName = "imagename";
		String imageDuplicateName = imageName + " copy";
		
		File projectFolder = tmpDir.newFolder(projectName);
		File pageSubFolder = new File(projectFolder, LocalDocConst.PAGE_FILE_SUB_FOLDER);
		
		//assume there is a copied version of the image with a matching page file
		File pageFile = new File(pageSubFolder, imageDuplicateName + ".xml");
		FileUtils.writeStringToFile(pageFile, "");
		
		File imagePage = LocalDocReader.findFile(imageName, pageSubFolder, "xml", false);
		File imageDuplicatePage = LocalDocReader.findFile(imageDuplicateName, pageSubFolder, "xml", false);
		
		assertNotNull(imagePage);
		assertNotNull(imageDuplicatePage);
	}
	
	/**
	 * 
	 * Test: 
	 * Match image name and XML name with additional extension?
	 * XML and Image can be matched
	 * Matching does not consider the full path to the XML, but only its name
	 * 
	 * see issue #57
	 * 
	 * {@link LocalDocReader#findXml(imgName, xmlInputDir, ignorePrefix)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFindPageXmlFileWithNameInPath() throws IOException {
		String projectName = "some_new_doc_1902";
		String image1Name = "1"; //e.g. 1.jpg
		String image2Name = "2"; //2.jpg, "2" is part of the path of the document name!
		
		File projectFolder = tmpDir.newFolder(projectName);
		File pageSubFolder = new File(projectFolder, LocalDocConst.PAGE_FILE_SUB_FOLDER);
		
		//create 1.xml page file
		File pageFile = new File(pageSubFolder, image1Name + ".xml");
		FileUtils.writeStringToFile(pageFile, "");
		
		File image1Page = LocalDocReader.findFile(image1Name, pageSubFolder, "xml", false);
		File image2Page = LocalDocReader.findFile(image2Name, pageSubFolder, "xml", false);
		
		assertNotNull(image1Page);
		assertNull(image2Page);
	}
	
	
	@Test
	public void testListImgFiles() throws IOException {
		String projectName = "1667522809_J_0013_0001";
		File projectFolder = tmpDir.newFolder(projectName);
		String imageName = "1667522809_J_0013_0001";
		File tifFile = new File(projectFolder, imageName + ".tif");
		FileUtils.writeByteArrayToFile(tifFile, new byte[] {});
		
		int nrOfFiles1 = 0, nrOfFiles2 = 0;
		nrOfFiles1 = LocalDocReader.findImgFiles(projectFolder).size();
		nrOfFiles2 = LocalDocReader.findImgFilenames(projectFolder).size();
		assertEquals(nrOfFiles1, nrOfFiles2);
	}


	@Test
	public void testReadLocalTIFxALTOV3() throws Exception {

		// arrange
		String fileLabel = "alto_v3_mwe";
		File projectDir = tmpDir.newFolder("project1");
		writeImage(projectDir.getAbsolutePath(), fileLabel, "tif", 71, 108);
		File altoSubDir = new File(projectDir.getAbsolutePath(), "alto");
		altoSubDir.mkdir();
		File sourceAlto = new File("src/test/resources/alto/1667522809_J_0025_0001.xml");
		File targetAlto = altoSubDir.toPath().resolve(fileLabel + ".xml").toFile();
		com.google.common.io.Files.copy(sourceAlto, targetAlto);

		// act
		TrpDoc result = LocalDocReader.load(projectDir.getAbsolutePath());

		// assert
		TrpPage pageOne = result.getPages().get(0);
		TrpTranscriptMetadata transcription = pageOne.getCurrentTranscript();
		URL urlPageFile = transcription.getUrl();
		assertNotNull(urlPageFile);
		String urlLocal = urlPageFile.toString().replace("file:", "");
		Document doc = readXMLDocument(urlLocal);
		XPathExpression<Element> xpath = generateXpression(".//page2013:TextLine");
		List<Element> els = xpath.evaluate(doc);

		// we expect exactly 51 lines
		assertEquals(51, els.size());
	}

	@Test
	public void testReadLocalTIFxALTOV3WithoutComposedBlocks() throws Exception {

		// arrange
		String fileLabel = "alto_v3_mwe";
		File projectDir = tmpDir.newFolder("project1");
		String imagePath = writeImage(projectDir.getAbsolutePath(), fileLabel, "tif", 71, 108);
		assertNotNull(imagePath);
		File altoSubDir = new File(projectDir.getAbsolutePath(), "alto");
		altoSubDir.mkdir();
		File sourceAlto = new File("src/test/resources/alto/1667522809_J_0001_0002.xml");
		File targetAlto = altoSubDir.toPath().resolve(fileLabel + ".xml").toFile();
		com.google.common.io.Files.copy(sourceAlto, targetAlto);

		// act
		TrpDoc result = LocalDocReader.load(projectDir.getAbsolutePath());

		// assert
		TrpPage pageOne = result.getPages().get(0);
		TrpTranscriptMetadata transcription = pageOne.getCurrentTranscript();
		URL urlPageFile = transcription.getUrl();
		assertNotNull(urlPageFile);
		String urlLocal = urlPageFile.toString().replace("file:", "");
		Document doc = readXMLDocument(urlLocal);
		XPathExpression<Element> xpath = generateXpression(".//page2013:TextLine");
		List<Element> els = xpath.evaluate(doc);
		
		// we expect exact 21 lines
		assertEquals(21, els.size());
	}
	
	/**
	 * Test loading a simple document.
	 * Then add an image and reload to check for proper change detection.
	 * 
	 * Ignore since it seems to copy rather slow, so data is not ready before requested
	 * 
	 * @throws IOException
	 */
	@Test 
	public void testLoadDocument() throws IOException {
		String projectName = "some_new_doc_1902";
		String image1Name = "1.jpg";
		String image2Name = "12.jpg";
		
		File projectFolder = tmpDir.newFolder(projectName);
		
		//prepare doc with two images
		URL testImage = this.getClass().getClassLoader().getResource("TrpTestDoc_20131209/StAZ-Sign.2-1_001.tif");
		FileUtils.copyURLToFile(testImage, new File(projectFolder, image1Name));
		FileUtils.copyURLToFile(testImage, new File(projectFolder, image2Name));
		
		DocLoadConfig config = new DocLoadConfig();
		TrpDoc doc = LocalDocReader.load(projectFolder.getAbsolutePath(), config, null);
		
		//two pages shall be loaded
		Assert.assertEquals(2, doc.getMd().getNrOfPages());
		//two separate XMLs shall be created
		int nrOfPageFiles = new File(projectFolder, LocalDocConst.PAGE_FILE_SUB_FOLDER).list().length;
		Assert.assertEquals(2, nrOfPageFiles);
		
		//add a new file with name partially matching image3Name
		String image3Name = "2.jpg";
		FileUtils.copyURLToFile(testImage, new File(projectFolder, image3Name));
		
		//loading it again should re-init the document and create an additional PAGE XML file
		TrpDoc doc2 = LocalDocReader.load(projectFolder.getAbsolutePath(), config, null);
		//two pages shall be loaded
		Assert.assertEquals(3, doc2.getMd().getNrOfPages());
		//two separate XMLs shall be created
		int nrOfPageFiles2 = new File(projectFolder, LocalDocConst.PAGE_FILE_SUB_FOLDER).list().length;
		Assert.assertEquals(2, nrOfPageFiles2);
	}

	static String writeImage(String imageDir, String label, String format, int width, int height) throws IOException {
		Path imageFile = new File(imageDir).toPath().resolve(label + "." + format);
		BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		ImageIO.write(bi2, format.toUpperCase(), imageFile.toFile());
		return imageFile.toString();
	}

	static Document readXMLDocument(String filePath) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		return builder.build(new File(filePath));
	}
	
	static XPathExpression<Element> generateXpression(String xpathStr) {
		XPathBuilder<Element> builder = new XPathBuilder<Element>(xpathStr, Filters.element());
		Namespace NS_PAGE_2013 = Namespace.getNamespace("page2013", "http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15");
		builder.setNamespace(NS_PAGE_2013);
		return builder.compileWith(XPathFactory.instance());
	}


	@Test
	public void testLoadPAGE2019Document() throws IOException {
		// arrange
		Path srcPath = Path.of("./src/test/resources/sample-page2019");
		File srcDir = srcPath.toAbsolutePath().toFile();
		String tmpFolder = "sample-page2019";
		File dstDir = tmpDir.newFolder(tmpFolder);
		FileUtils.copyDirectory(srcDir, dstDir);
		
		// act
		assertTrue(Files.exists(dstDir.toPath()));
		String theAbsolutePath = dstDir.toPath().toAbsolutePath().toString();
		TrpDoc doc = LocalDocReader.load(theAbsolutePath);

		// assert
		assertNotNull(doc);
		assertEquals(1, doc.getNPages());
	}
}
