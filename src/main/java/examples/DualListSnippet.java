package examples;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.mihalis.opal.itemSelector.DLItem;
import org.mihalis.opal.itemSelector.DLItem.LAST_ACTION;
import org.mihalis.opal.itemSelector.DualList;
import org.mihalis.opal.itemSelector.SelectionChangeEvent;
import org.mihalis.opal.itemSelector.SelectionChangeListener;

/**
 * A simple snipper for the ItemSelector Widget
 * 
 */
public class DualListSnippet {

        public static void main(final String[] args) {
                final Display display = new Display();
                final Shell shell = new Shell(display);
                shell.setText("Dual List Snippet");
                shell.setSize(600, 600);
                shell.setLayout(new GridLayout(1, false));
                System.out.println("shell = "+shell);

                final DualList dl = new DualList(shell, SWT.NONE);
                dl.setItems(createItems(shell));

                dl.addSelectionChangeListener(new SelectionChangeListener() {

                        @Override
                        public void widgetSelected(final SelectionChangeEvent e) {
                                System.out.println("Selection Change Listener called");
                                for (final DLItem item : e.getItems()) {
                                        final StringBuilder sb = new StringBuilder();
                                        if (item.getLastAction() == LAST_ACTION.SELECTION) {
                                                sb.append("[SELECTION] ");
                                        } else {
                                                sb.append("[DE-SELECTION] ");
                                        }
                                        sb.append(item.getText());
                                        System.out.println(sb.toString());
                                }
                        }
                });

                dl.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

                shell.pack();
                shell.open();

                while (!shell.isDisposed()) {
                        if (!display.readAndDispatch()) {
                                display.sleep();
                        }
                }
                display.dispose();

        }

        private static List<DLItem> createItems(final Shell shell) {
                final List<DLItem> list = new ArrayList<DLItem>();

                String defaultFontName = null;
                int defaultHeight = -1;
                for (final FontData fontData : shell.getFont().getFontData()) {
                        if (defaultFontName == null) {
                                defaultFontName = fontData.getName();
                        }
                        if (defaultHeight == -1) {
                                defaultHeight = fontData.getHeight();
                        }
                }

                final Font font = new Font(shell.getDisplay(), defaultFontName, defaultHeight, SWT.BOLD);

                list.add(new DLItem("Austria", createImage(shell, "austria")));
                list.add(new DLItem("Belgium", createImage(shell, "belgium")));
                list.add(new DLItem("Bulgaria", createImage(shell, "bulgaria")));
                list.add(new DLItem("Cyprus", createImage(shell, "cyprus")));
                list.add(new DLItem("Czech Republic", createImage(shell, "czech")));
                list.add(new DLItem("Denmark", createImage(shell, "denmark")));
                list.add(new DLItem("Estonia", createImage(shell, "estonia")));
                list.add(new DLItem("Finland", createImage(shell, "finland")));
                list.add(new DLItem("France", createImage(shell, "france"), font));
                list.add(new DLItem("Germany", createImage(shell, "germany")));
                list.add(new DLItem("Greece", createImage(shell, "greece")));
                list.add(new DLItem("Hungary", createImage(shell, "hungary")));
                list.add(new DLItem("Ireland", createImage(shell, "ireland")));
                list.add(new DLItem("Italy", createImage(shell, "italy")));
                list.add(new DLItem("Latvia", createImage(shell, "latvia")));
                list.add(new DLItem("Lithuania", createImage(shell, "lithuania")));
                list.add(new DLItem("Luxembourg", createImage(shell, "luxembourg")));
                list.add(new DLItem("Malta", createImage(shell, "malta")));
                list.add(new DLItem("Netherlands", createImage(shell, "netherlands")));
                list.add(new DLItem("Poland", createImage(shell, "poland"), shell.getDisplay().getSystemColor(SWT.COLOR_WHITE), shell.getDisplay().getSystemColor(SWT.COLOR_RED)));
                list.add(new DLItem("Portugal", createImage(shell, "portugal")));
                list.add(new DLItem("Romania", createImage(shell, "romania")));
                list.add(new DLItem("Slovakia", createImage(shell, "slovakia")));
                list.add(new DLItem("Slovenia", createImage(shell, "slovenia")));
                list.add(new DLItem("Spain", createImage(shell, "spain")));
                list.add(new DLItem("Sweden", createImage(shell, "sweden")));
                list.add(new DLItem("United Kingdom", createImage(shell, "unitedkingdom")));

                shell.addDisposeListener(new DisposeListener() {

                        @Override
                        public void widgetDisposed(final DisposeEvent e) {
                                font.dispose();
                        }
                });

                return list;
        }

        private static Image createImage(final Shell shell, final String fileName) {
        	if (true)
        		return null;
        	
                final Image image = new Image(shell.getDisplay(), DualListSnippet.class.getClassLoader().getResourceAsStream("org/mihalis/opal/itemSelector/flags/" + fileName + ".png"));
                shell.addDisposeListener(new DisposeListener() {

                        @Override
                        public void widgetDisposed(final DisposeEvent de) {
                                image.dispose();
                        }
                });
                return image;
        }
}
