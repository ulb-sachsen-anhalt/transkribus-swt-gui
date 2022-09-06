package eu.transkribus.util;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.exec.OS;
import org.eclipse.swt.graphics.RGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import eu.transkribus.core.i18n.I18nUtils;
import eu.transkribus.core.util.SysUtils;
import eu.transkribus.swt.portal.PortalWidget.Docking;
import eu.transkribus.swt.portal.PortalWidget.Position;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt_gui.canvas.CanvasSettings;

public class Utils {
	final static Logger logger = LoggerFactory.getLogger(Utils.class);
		
	public static int countTrue(Collection<Boolean> l) {
		int c=0;
		for (Boolean b : l) {
			if (b!=null && b)
				++c;
		}
		return c;
	}
	
	public static int countFalse(Collection<Boolean> l) {
		int c=0;
		for (Boolean b : l) {
			if (b==null || !b)
				++c;
		}
		return c;
	}
	
	/**
	 * Returns all lines from the given string, i.e. the text between newline characters and start and end of the string. 
	 * Also parses empty lines as new lines, e.g. the String "\n\n" yields three empty lines.
	 */
	public static List<String> getLines(String str) {
		String line = null;
		List<String> lines = new ArrayList<>();
		if (str.isEmpty()) {
			lines.add("");
			return lines;
		}
		
//		if (str.startsWith("\n") || str.startsWith("\r\n"))
//			lines.add("");
		
		Scanner scanner = new Scanner(str);
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			lines.add(line);
		}
		scanner.close();
		
		if (str.endsWith("\n") || str.endsWith("\r\n"))
			lines.add("");
		
		return lines;
	}
	
	public static List<Integer> getAllOccurences(String str, char guess) {
		List<Integer> occs = new ArrayList<>();
		int index = str.indexOf(guess);
		while (index >= 0) {
			occs.add(index);
		    index = str.indexOf(guess, index + 1);
		}
		return occs;
	}
	
	public static String getDefaultFontName() {
		if (SysUtils.isWin()) {
			return "sans";
		} else if (SysUtils.isLinux()) {
			return "sans";
		} else
			return "sans";
	}
	
	public static boolean equalsEps(float v1, float v2, float eps) {
		return Math.abs(v1-v2) < eps;
	}
	
	public static boolean equalsEps(double v1, double v2, double eps) {
		return Math.abs(v1-v2) < eps;
	}	
	
	public static boolean val(Boolean value) {
		return value == null ? false : value;		
	}
	
	public static int val(Integer value) {
		return value == null ? 0 : value;		
	}
	
	public static float val(Float value) {
		return value == null ? 0.0f : value;		
	}

	public static double val(Double value) {
		return value == null ? 0.0d : value;		
	}
	
	/**
	 * Get the index of the word of the given text and cursor position
	 */
	public static int getWordIndexFromCursor(String text, int cursor) {
		// compile pattern to divide words in a line
		Matcher matcher = Pattern.compile("(\\S+)(\\s*)").matcher(text);
		
		// find word whose bounds match the cursor:
	    int i=0;
	    while (matcher.find()) {
	    	if (cursor >= matcher.start(1) && cursor <= matcher.end(1))
	    		return i;
	    	
	    	++i;
	    }
	   
	    return -1;		
	}
	
	/** Parse the word of the text around the given position */
	public static String parseWord(String text, int pos) {
		String word="";
		char c = ' ';
		for (int i=pos-1; i>=0; i--) {
			c = text.charAt(i);
			if (Character.isWhitespace(c))
				break;
			
			word = c+word;
		}
		for (int i=pos; i<text.length(); i++) {
			c = text.charAt(i);
			if (Character.isWhitespace(c))
				break;			
			
			word += c;
		}
		
		return word;
	}
	
	/** Where does the word in the given text at the given position begin? */
	public static int wordStartIndex(String text, int pos) {
		for (int i=pos-1; i>=0; i--) {
			if (Character.isWhitespace(text.charAt(i)))
				return i+1;
		}
		return 0;
	}
		
	public static Point wordBoundary(String text, int pos) {
		int si = wordStartIndex(text, pos);
		int ei = si+parseWord(text, pos).length();
		return new Point(si, ei);
	}
	
	@Deprecated
	public static Point wordBoundaryNew(String text, int pos) {
		BreakIterator boundary = BreakIterator.getWordInstance();
		boundary.setText(text);
		int end = boundary.following(pos);
		int start = boundary.previous();
		return new Point(start, end);
	}
	
	
	/**
	 * @deprecated does not really work -> replaced classloader is not loading all new classes
	 */
	public static void replaceSystemClassLoaderWithUrlClassLoaders(ClassLoader replacement) throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    	logger.info("replacing system classloader with urlclassloader");
    	java.lang.reflect.Field objSCL = ClassLoader.class.getDeclaredField("scl");
    	objSCL.setAccessible(true);
    	
    	SebisClassloader newClassLoader = new SebisClassloader(replacement==null ? ClassLoader.getSystemClassLoader() : replacement);
    	objSCL.set(null, newClassLoader);
//    	objSCL.set(null, new URLClassLoader(new URL[0]));
    	Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0], newClassLoader));
    }
	
	/**
	 * @deprecated Throws an error when called via java >= 9 (appclassloader cannot be cast to urlclassloader) 
	 */
	public static boolean addJarToClasspath(File jarFile) {
		if (SysUtils.isJavaVersionGreater8()) {
			logger.warn("Java version > 8 detected -> cannot add jar file to classpath via reflection: "+jarFile.getAbsolutePath());
			return false;
		}
		
		try {
			URL url = jarFile.toURI().toURL();
			URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Class<?> urlClass = URLClassLoader.class;
			Method method = urlClass.getDeclaredMethod("addURL", new Class<?>[] { URL.class });
			method.setAccessible(true);
			method.invoke(urlClassLoader, new Object[] { url });
			return true;
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return false;
		}
	}
			
	public static List<Integer> parse3DIntVectors(String str) throws Exception {
		String regex = "^[\\{\\(\\[]{0,1}" // optional opening brackets
				+ "\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*"
				+ "[\\}\\)\\]]{0,1}$"; // optional end brackets
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		if (!matcher.matches() || matcher.groupCount() != 3) {
			throw new Exception("Could not parse vector string "+str);
		}
		
//		System.out.println("group count "+matcher.groupCount());
		List<Integer> res = new ArrayList<>();
		for (int i=1; i<=matcher.groupCount(); ++i) {
//			System.out.println("s = "+matcher.group(i));
			res.add(Integer.parseInt(matcher.group(i)));			 
		}
		
		return res;
	}
	
	public static List<Integer> parseIntVectors(String str) throws Exception {
		List<Integer> res = new ArrayList<>();
		String tmp = new String(str);
		String [] braces = new String[] { "\\[", "\\]", "\\(", "\\)", "\\{", "\\}" };
		for (String  b : braces) {
			tmp = tmp.replaceAll(b, "");
		}
		
		for (String s : tmp.split(",")) {
			try {
			res.add(Integer.parseInt(s.trim()));
			}
			catch (Exception e) {
				throw new Exception("Could not parse vector string: "+str);
			}
		}
		
		return res;
	}
	
	public static String toVecString(RGB rgb) {
		return "("+rgb.red+","+rgb.green+","+rgb.blue+")";
	}

	/** Sets members of a bean according to the properties defined in the given properties object 
	 * Implicitly converts some types that could otherwise not be initialized in a config file: <br>
	 * <ul>
	 * <li> org.eclipse.swt.graphics.Color objects are parsed from a vector of RGB values, e.g. (255, 120, 30), [200, 300, 100] ... </li>
	 * </ul>
	 * Prints out an error message, if any attribute could not be set e.g. due to String to Type conversion errors
	 * **/
	public static void setBeanProperties(Object bean, Properties properties) /*throw IllegalAccessException, InvocationTargetException, NoSuchMethodException*/ {
		BeanMap beanmap = new BeanMap(bean);
				
		for (Map.Entry<Object, Object> e : properties.entrySet()) {
			if (e.getKey() instanceof String && beanmap.containsKey(e.getKey())) {
				String key = (String) e.getKey();
				String value = e.getValue().toString().trim();
				Class<?> type = beanmap.getType(key);
				if (type == null) // no such property!
					continue;
				
				logger.debug("setting property '"+key+"' of bean '"+bean.getClass().getSimpleName()+"' to '"+value+"'"+ " type = "+beanmap.getType(key));
				
				try {
					Object valueObject = getValue(beanmap.getType(key), value);
					BeanUtils.setProperty(bean, key, valueObject);
				}
				catch (Exception ex) {
					logger.warn("Could not set value of config attribute "+key+" to "+value+", error: "+ex.getMessage());
				}
			}
		}
	}
	
	public static void setBeanProperties(Object bean, Configuration config) /*throw IllegalAccessException, InvocationTargetException, NoSuchMethodException*/ {
		BeanMap beanmap = new BeanMap(bean);
		
		Iterator<String> it = config.getKeys();
		
		while (it.hasNext()) {
			String key = it.next();
			String value = config.getProperty(key).toString().trim();
			Class<?> type = beanmap.getType(key);
			if (type == null) {
				// no such property!
				logger.debug("Could not find property: " + key);
				continue;
			}
				
			
			logger.debug("setting property '"+key+"' of bean '"+bean.getClass().getSimpleName()+"' to '"+value+"'"+ " type = "+type);
			
			try {
				Object valueObject = getValue(beanmap.getType(key), value);
				BeanUtils.setProperty(bean, key, valueObject);
			}
			catch (Exception ex) {
				logger.warn("Could not set value of config attribute "+key+" to "+value+", error: "+ex.getMessage());
			}
			
		}
	}	
	
	public static Object getValue(Class<?> type, String valueStr) throws Exception {
		if (type == org.eclipse.swt.graphics.Color.class) { // if type of member is swt color, parse input as (x,y,z) string vector
//			logger.debug("this is an swt color!");
			List<Integer> v = parseIntVectors(valueStr);
			if (v.size() != 3)
				throw new Exception("Could not parse vector, size: "+v.size());
			
			RGB rgb = new RGB(v.get(0), v.get(1), v.get(2));
			return Colors.createColor(rgb);
//			return rgb;
		}
		else if (type == Locale.class) {
			return I18nUtils.getLocaleFromString(valueStr);
		}
		else if (type == Docking.class) {
			return Docking.valueOf(valueStr);
		}
		else if (type == Position.class) {
			return Position.valueOf(valueStr, true, true);
		}
		
		return valueStr;
	}
	
	public static boolean propertyExists(Object bean, String property) {
		return PropertyUtils.isReadable(bean, property) && PropertyUtils.isWriteable(bean, property);
	}
	
	public static void testDiffUtils() {
		
		String str1="hello world";
		String str2="hello cruel world";
		
		List<String> l1 = new ArrayList<>();
		List<String> l2 = new ArrayList<>();
		l1.add(str1);
		l2.add(str2);
		
		Patch p = DiffUtils.diff(l1, l2);
		
		for (int i=0; i<p.getDeltas().size(); ++i) {
			Delta d = (Delta) p.getDeltas().get(i);
			logger.info("delta: "+d);
			logger.info(d.getType().toString());
			logger.info(d.getOriginal().toString());
			logger.info(d.getRevised().toString());
		}

		
	}
	
	public static String getStartScriptName() throws URISyntaxException {
		String base="Transkribus.";
		if (OS.isFamilyWindows()) {
			String exe = base+"exe";
			String cmd = "cmd /c start "+exe;
//			cmd += "& del "+getCurrentJar().getName(); // this cleans the old version in windows after the new version has started --> should work, as current JVM should exit sooner than new program has started! 
			return cmd;
		} else if (OS.isFamilyMac()) {
			return "./"+base+"command";
		} else {
			return "./"+base+"sh";
		}
	}
	
	public static void restartApplication(int exitCode) throws URISyntaxException, IOException {
//		  final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
//		  final File currentJar = new File(TrpGui.class.getProtectionDomain().getCodeSource().getLocation().toURI());

		  /* is it a jar file? */
//		  if(!currentJar.getName().endsWith(".jar"))
//		    return;

		  /* Build command: java -jar application.jar */
		  final ArrayList<String> command = new ArrayList<String>();
		  
		  logger.debug("restarting: "+getStartScriptName());
		  command.addAll(Arrays.asList(getStartScriptName().split(" ")));
		  
//		  command.add(javaBin);
//		  command.add("-jar");
//		  command.add(currentJar.getPath());

		  final ProcessBuilder builder = new ProcessBuilder(command);
		  builder.start();
		  
		  System.exit(exitCode);
	}
	
	public static void main(String [] args) {		
		CanvasSettings cs = new CanvasSettings();
		
//		Object valueObject = getValue(beanmap.getType(key), value);
		Object valueObject = null;
		try {
			System.out.println(propertyExists(cs, "whatsoever"));
			System.out.println(propertyExists(cs, "scalingFactor"));
			
			BeanUtils.setProperty(cs, "whatsoever", valueObject);
			
//			BeanUtils.getProp
			BeanUtils.setProperty(cs, "scalingFactor", 1.0f);
		} catch (IllegalAccessException | InvocationTargetException e1) {
			e1.printStackTrace();
		}
		
		
		System.out.println("I AM DONE");
		System.out.println(cs);
		
		
//		testDiffUtils();
		
		if (true) return;
		
		
		
		String [] vecs = new String[] {
				"(0, 1,    3]",
				"(0, 1,    3",
				"0, 1,    3",
				"(0, 1,    3, ]",
				"(0, 1,    3, 5, 6]",
				"(, 1,    3, 5, 6]",
				"(,asdf asf, adsff",
				"(0, 1, 3.0)",
				
		};
		
		for (String vec : vecs) {
			try{
			System.out.println(vec+ " => (2) "+parse3DIntVectors(vec));
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
			
			try{
			System.out.println(vec+ " => (1) "+parseIntVectors(vec));
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
			
		}
	}
}
