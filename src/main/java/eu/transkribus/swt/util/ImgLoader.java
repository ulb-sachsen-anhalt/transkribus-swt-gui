package eu.transkribus.swt.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.SebisStopWatch;
import eu.transkribus.core.util.SysUtils;
import eu.transkribus.interfaces.types.util.TrpImgMdParser;
import eu.transkribus.interfaces.types.util.TrpImgMdParser.ImageTransformation;
import eu.transkribus.swt_gui.canvas.CanvasImage;

public class ImgLoader {
	private final static Logger logger = LoggerFactory.getLogger(ImgLoader.class);
	
	static Image ERROR_IMG = Images.getOrLoad("/icons/broken_image.png");
	
	public final static boolean TRY_LOAD_IMAGES_WITH_JFACE_FIRST = true;
	public final static boolean LOAD_LOCAL_IMAGES_WITH_JAI = false;
	
	/**
	 * Fixes the orientation of the input image according to the given EXIF orientation tag.<br/>
	 * <em>The original image is disposed if a transformation is performed!</em><br/>
	 * @param img
	 * @param url
	 * @return
	 */
	public static Image fixOrientation(Image img, int exifOrientation) {
		if (exifOrientation < 2 || exifOrientation > 8) {
			return img;
		}
		
		logger.debug("fixing exif image orientation, value of orientation tag = "+exifOrientation);
		
		int origWidth = img.getImageData().width;
		int origHeight = img.getImageData().height;
		Transform t = new Transform(img.getDevice());
		
		int returnWidth = origWidth;
		int returnHeight = origHeight;
		
	    switch (exifOrientation) {
		    case 2: // Flip X
		    	t.scale(-1.0f, 1.0f);
		        t.translate(-origWidth, 0);
		        break;
		    case 3: // PI rotation 
		        t.translate(origWidth, origHeight);
		        t.rotate(180);
		        break;
		    case 4: // Flip Y
		        t.scale(1.0f, -1.0f);
		        t.translate(0, -origHeight);
		        break;
		    case 5: // - PI/2 and Flip X
		    	returnWidth = origHeight;
				returnHeight = origWidth;    	
		        t.rotate(-90);
		        t.scale(-1.0f, 1.0f);
		        break;
		    case 6: // -PI/2 and -width
		    	logger.debug("rotating img clockwise 90°");
		    	returnWidth = origHeight;
				returnHeight = origWidth;
		    	t.translate(origHeight, 0);
		    	t.rotate(90.0f);
		        break;
		    case 7: // PI/2 and Flip
		    	returnWidth = origHeight;
				returnHeight = origWidth;
		        t.scale(-1.0f, 1.0f);
		        t.translate(-origHeight, 0);
		        t.translate(0, origWidth);
		        t.rotate(270.0f);
		        break;
		    case 8: // PI / 2
		    	returnWidth = origHeight;
				returnHeight = origWidth;	    	
		        t.translate(0, origWidth);
		        t.rotate(270.0f);
		        break;
	    }
	    
    	Image returnImg = new Image(img.getDevice(), returnWidth, returnHeight);
    	GC gc = new GC(returnImg);
    	gc.setTransform(t);
    	gc.drawImage(img, 0, 0);
    	img.dispose();
    	t.dispose();
    	gc.dispose();

	    return returnImg;
	}
	
	/**
	 * Loads an image from a URL, checks the EXIF data for an orientation value and orients the image correctly.
	 * If the caller needs to be aware of the transformation done, it should use {@link TrpImgMdParser#readImageDimension(URL)} 
	 * to get the {@link ImageTransformation} and then call {@link #load(URL, ImageTransformation)}.
	 *  
	 * @param url
	 * @param transformation
	 * @return the {@link Image}
	 * @throws IOException
	 */
	public static Image load(URL url) throws IOException {
		ImageTransformation transformation;
		try {
			transformation = TrpImgMdParser.readImageDimension(url);
		} catch (Exception e) {
			logger.warn("Could not read metadata from " + url + ": " + e.getMessage());
			transformation = null;
		}
		return load(url, transformation);
	}
	
	/**
	 * Loads an image from a URL.
	 * If the {@link ImageTransformation} is not null, the EXIF orientation value included will be taken into account for orienting the image.
	 * The transformation object can be obtained by {@link TrpImgMdParser#readImageDimension(URL)}.
	 * 
	 * @param url
	 * @param transformation
	 * @return the {@link Image}
	 * @throws IOException
	 */
	public static Image load(URL url, ImageTransformation transformation) throws IOException {
		String prot = url.getProtocol() == null ? "" : url.getProtocol();
		boolean isLocal = prot.startsWith("file");

		boolean loadedWithSwt=false;
		Image img=null;
		if (TRY_LOAD_IMAGES_WITH_JFACE_FIRST || !(isLocal && LOAD_LOCAL_IMAGES_WITH_JAI)) {
			try {
				logger.trace("loading image with jface");
				img = loadWithSWTDownloadFirst(url);
				loadedWithSwt=true;
			} catch (Exception e) {
				logger.warn("Error loading image with JFace - now trying to load with JAI (slower due to the awt->swt-image conversion process!, url: "+url);
				img = loadWithJAI(url);
			}
		} else {
			logger.debug("loading image with jai");
			img = loadWithJAI(url);
		}
		if( (loadedWithSwt && SysUtils.IS_OSX) || transformation==null ) { // when image is loaded with SWT, we do not need to correct the transformation 
			logger.trace("no transformation applied, loadedWithSwt: "+loadedWithSwt+", isOSX: "+SysUtils.IS_OSX+", transformation: "+transformation);
			return img;
		} else {
			return fixOrientation(img, transformation.getExifOrientation());
		}
	}
	
	/**
	 * @fixme returns white images when compression is unknown (e.g. group 4 compressed binary images) 
	 * and remote url is given (example: https://dbis-thure.uibk.ac.at/f/Get?id=ZFGXEWUAHRBYUKIAYZPVKACO -> when file is downloaded first, it works...)
	 * and no error is thrown!
	 */
	@Deprecated
	public static Image loadWithJFace(URL url) throws Exception {
		if (url==null)
			throw new NullPointerException("The given url is null!");
//		return Images.getOrLoad(url);		
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
		
//		if (imageDescriptor == null || imageDescriptor.getImageData() == null)
//			throw new Exception("Could not load image from url: "+url.toString());
		if (imageDescriptor == null) // should not happen here...
			throw new IOException("Could not load image from url: "+url.toString());
		
		Image img = imageDescriptor.createImage(false);
		if (img == null)
			throw new IOException("Could not create image from url: "+url.toString());
		
		return img;
	}
	
	public static Image loadWithJAI(URL url) throws IOException {
//		SebisStopWatch.SW.start();
//		ImageIO.setUseCache(false);

		BufferedImage buffImg = ImageIO.read(url);
//		SebisStopWatch.SW.stop(true, "loading with jai: ", logger);

//		SebisStopWatch.SW.start();
		ImageData swtImg = SWTUtil.convertToSWT(buffImg);
//		SebisStopWatch.SW.stop(true, "converting awt->swt: ", logger);
		return new Image(Display.getDefault(), swtImg);
	}
	
	/** is a little bit slower than loadWithJFace but has more or less the same results (same errors on group4 compressed images returning a plain white image!) */
	@Deprecated
	public static Image loadWithSWT(URL url) throws IOException {
		try (InputStream is = url.openStream()) {
			try {				
				return new Image(Display.getDefault(), is);
			} catch (Throwable e) {
				throw new IOException(e.getMessage(), e);
			}
		}
	}
	
	public static Image loadWithSWTDownloadFirst(URL url) throws IOException {
		
		try (InputStream is = url.openStream()) {
			try {
				String prot = url.getProtocol() == null ? "" : url.getProtocol();
				logger.trace("loading image with protocol = "+prot);
				
				if (!prot.startsWith("file")) {
					final File tempFile = File.createTempFile("transkribus_tmp_img_", "");
					try (FileOutputStream out = new FileOutputStream(tempFile)) {
			            IOUtils.copy(is, out);
			        }
					logger.trace("temp file: "+tempFile.getAbsolutePath());
					Image img = new Image(Display.getDefault(), tempFile.getAbsolutePath());
					if (!tempFile.delete())
						logger.warn("temp image file could not be deleted: "+tempFile.getAbsolutePath());

					return img;
				} else {
					logger.debug("this is a local image: "+url.getFile());					
					// NOTE: have to use constructor with filename to initiate native call, cf. https://bugs.eclipse.org/bugs/show_bug.cgi?id=57382
					// constructor with inputstream will return blank white image for group 4 compressed local images!
					return new Image(Display.getDefault(), url.getFile()); 
				}
			} catch (Throwable e) {
				throw new IOException(e.getMessage(), e);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		SebisStopWatch sw = new SebisStopWatch();
		
//		String path = "file:///home/sebastianc/Bilder/0021_0.tif";
//		String path = "file:///home/sebastianc/Downloads/420000317426_J.Ritter_Briefe_00000020.TIF";
//		String path = "file:///home/sebastianc/Downloads/wp54808274x-00044.tif";
//		String path = "file:///home/sebastianc/Downloads/wp54808274x-00044-uncompressed.tif";
//		String path = "https://dbis-thure.uibk.ac.at/f/Get?id=DPHHOZDONDISGPLGBQFCYZCH";
//		String path = "https://dbis-thure.uibk.ac.at/fimagestoreTrp/Get?id=EGRAXVKOJJNRSIKNPOWENBYV";
//		String path = "https://dbis-thure.uibk.ac.at/f/Get?id=ZFGXEWUAHRBYUKIAYZPVKACO";
		String path = "file:///home/sebastianc/Transkribus_TestDoc/035_323_001.jpg";
//		String path = "file:///home/sebastianc/Downloads/av_1930_61_0031.TIF";
//		String path = "file:///home/sebastianc/Bilder/watch_you_step_jpeg2000.jp2";
//		String path = "https://dbis-thure.uibk.ac.at/fimagestoreTrp/Get?id=UHLQLLVFELOQECBZJCRTRVDO";
//		String path = "https://dbis-thure.uibk.ac.at/fimagestoreTrp/Get?id=UHLQLLVFELOQECBZJCRTRVDO";
		
		final URL url = new URL(path);
		
//		ImageData imDat = img.img.getImageData();
//		for (int i=0; i<img.width; ++i) {
//			for (int j=0; j<img.height; j++) {
//				int v = imDat.getPixel(i, j);
//				logger.info("("+i+","+j+"): "+v);
//			}
//		}
		
//		sw.start();
//		loadWithSWT(url);
//		sw.stop(true);
	
//		sw.start();
//		loadWithJFace(url);
//		sw.stop(true);
		
		sw.start();
		loadWithSWTDownloadFirst(url);
		sw.stop(true);
		
		sw.start();
		for (int i=0; i<10; ++i)
			loadWithJAI(url);
		sw.stop(true);
		
		if (true)
			return;
		
		final CanvasImage img = new CanvasImage(url);
		
		// run the event loop as long as the window is open
		final Display display = Display.getCurrent();
		Realm.runWithDefault(SWTObservables.getRealm(display), new Runnable() {
			@Override
			public void run() {
				Realm testRealm = Realm.getDefault();
				
				final Shell shell = new Shell(Display.getCurrent());
				shell.setLayout(new FillLayout());
				
//				CanvasWidget cw = new CanvasWidget(shell, 0);
//				cw.getCanvas().getScene().setMainImage(img);		
				shell.open();
			
				
				while (!shell.isDisposed()) {
				    // read the next OS event queue and transfer it to a SWT event 
				  if (!display.readAndDispatch())
				   {
				  // if there are currently no other OS event to process
				  // sleep until the next OS event is available 
				    display.sleep();
				   }
				}
				// disposes all associated windows and their components
				display.dispose(); 				
			}
		});		

//		sw.start();
//		load(url, false);
//		sw.stop(true);

//		Image img = ImgLoader.load(new URL(path), false);
	}

}
