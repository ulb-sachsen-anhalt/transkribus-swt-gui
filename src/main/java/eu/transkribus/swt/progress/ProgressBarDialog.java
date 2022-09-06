package eu.transkribus.swt.progress;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import examples.AnimatedGif;
import junit.framework.Assert;

public class ProgressBarDialog extends Dialog implements IProgressMonitor {
	private final static Logger logger = LoggerFactory.getLogger(ProgressBarDialog.class);

	private Button cancelButton;
	private Button minButton;
	private Composite cancelComposite;
	// private Label lineLabel;
	private Composite progressBarComposite;

	private AnimatedGif waitingImage;
	private CLabel taskNameLabel;
	private CLabel subTaskLabel, detailLabel;
	private ProgressBar progressBar = null;
	protected Image processImage = Images.getOrLoad("/icons/wait.gif");

	private Shell shell;
	private Display display = null;

	// protected String shellTitle = "Processing...";
	protected boolean mayCancel = true;
	final protected int progressBarStyle = SWT.SMOOTH;

//	protected MyIRunnableWithProgress runnable;
	protected String taskName = "Processing...";
	protected Thread workerThread;
	protected volatile boolean cancelled = false;
	protected volatile boolean done = false;
	
	ScheduledExecutorService executorService;
	Callable<?> internalRunnable;
	Shell parentShell;
	
	public ProgressBarDialog(Shell parent) {
		super(parent);
		parentShell=parent;
		display = parent.getDisplay();
	}
	
	public static void open(Shell parent, final IRunnableWithProgress runnable, String title, boolean mayCancel) throws InterruptedException, Throwable {
		ProgressBarDialog pd = new ProgressBarDialog(parent);
		pd.open(runnable, title, mayCancel);
	}

	public void open(final IRunnableWithProgress runnable, String title, boolean mayCancel) throws InterruptedException, Throwable {
		this.done = false;
		executorService = Executors.newSingleThreadScheduledExecutor();
		initInternalRunnable(runnable);
		
		this.setMayCancel(mayCancel);
		Assert.assertNotNull(runnable);

		createContents();

		shell.setText(title);
		shell.open();
		shell.layout();
		
		if (waitingImage != null) {
			waitingImage.animate();
		}
				
		Future<?> fut = executorService.submit(internalRunnable);
		while (!shell.isDisposed()) {
			if (!display.isDisposed() && !display.readAndDispatch()) {
				checkIfFutIsDone(fut);
				
				display.sleep();	
			}
		}
	}
	
	private void checkIfFutIsDone(Future fut) throws Throwable {
//		logger.trace("is fut done: "+fut.isDone());
		if (fut.isDone()) {
			try {
				fut.get();
			} catch (Throwable ee) {
				Throwable cause = ee.getCause();
//				logger.debug("cause = "+cause);
				if (cause != null) {
					if (cause instanceof InvocationTargetException) {
						throw ((InvocationTargetException) cause).getTargetException();
					} else if (cause instanceof InterruptedException) {
						throw (InterruptedException) cause;
					} else
						throw cause;
				} else
					throw ee;
			}
			finally {
				done();
			}
		}
	}
	
	private void initInternalRunnable(final IRunnableWithProgress runnable) {
		internalRunnable = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				runnable.run(ProgressBarDialog.this);
				return null;
				
//				try {
//					runnable.run(ProgressBarDialog.this);
//					if (!parentShell.getDisplay().isDisposed())
//					parentShell.getDisplay().syncExec(new Runnable() {
//						@Override public void run() {
//							runnable.runInGuiOnSuccess();
//						}
//					});					
//				} catch (final InvocationTargetException e) {
//					if (!parentShell.getDisplay().isDisposed())
//					parentShell.getDisplay().syncExec(new Runnable() {
//						@Override public void run() {
//							runnable.runInGuiOnError(e.getTargetException());
//						}
//					});
//				} catch (final InterruptedException e) {
//					if (!parentShell.getDisplay().isDisposed())
//					parentShell.getDisplay().syncExec(new Runnable() {
//						@Override public void run() {
//							runnable.runInGuiOnError(e);
//						}
//					});
//				} finally {
//					done();
//				}
			}
		};
	}
	
	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.APPLICATION_MODAL);
		
		display = shell.getDisplay();
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;

		shell.setLayout(gridLayout);

		shell.setSize(450, 250);

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));
		
		// TEST
//		waitingImage = new AnimatedGif(composite, 0);
//		waitingImage.load2("/icons/wait.gif");
//		waitingImage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		taskNameLabel = new CLabel(composite, SWT.NONE);
		taskNameLabel.setImage(processImage);
		taskNameLabel.setLayoutData(new GridData(SWT.FILL, GridData.CENTER, true, false, 1, 1));
		taskNameLabel.setText(taskName);

		subTaskLabel = new CLabel(composite, SWT.NONE);
		subTaskLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
		subTaskLabel.setText("");

		progressBarComposite = new Composite(shell, SWT.NONE);
		progressBarComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		progressBarComposite.setLayout(new FillLayout());

		progressBar = new ProgressBar(progressBarComposite, progressBarStyle);
		progressBar.setMaximum(0);
		
		detailLabel = new CLabel(shell, SWT.NONE);
		detailLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));

		cancelComposite = new Composite(shell, SWT.NONE);
		cancelComposite.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		cancelComposite.setLayout(gridLayout_1);

		cancelButton = new Button(cancelComposite, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setCanceled(true);
			}
		});
		cancelButton.setLayoutData(new GridData(78, SWT.DEFAULT));
		cancelButton.setText("Cancel");
		cancelButton.setEnabled(this.mayCancel);

		shell.addListener(SWT.Close, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!done) {
					event.doit = false;
					setCanceled(true);
				}
			}
		});
		
		minButton = new Button(cancelComposite,SWT.NONE);
		minButton.setLayoutData(new GridData(78, SWT.DEFAULT));
		minButton.setText("Minimize");
		minButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.setMinimized(true);
				shell.dispose();
			}
		});
		
		
		
		center();
		shell.setSize(450, shell.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
	}
		
	public void center() {
		Shell parentShell = getParent();
		
	    Rectangle bounds = parentShell.getBounds();
	    Rectangle rect = shell.getBounds();
	    
	    int x = bounds.x + (bounds.width - rect.width) / 2;
	    int y = bounds.y + (bounds.height - rect.height) / 2;
	    
	    shell.setLocation(x, y);
	}

//	public void setProcessImage(Image img) {
//		processImage = img;
//	}

	public void setMayCancel(boolean mayCancel) {
		this.mayCancel = mayCancel;
	}

	// functions for IProgressMonitor:
	
	@Override
	public void beginTask(String name, final int totalWork) {
		setTaskName(name);
		if (!shell.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					logger.debug("totalWork: " + totalWork);
					if (totalWork <= IProgressMonitor.UNKNOWN) {
						progressBar.dispose();
						progressBar = new ProgressBar(progressBarComposite, SWT.INDETERMINATE);
						progressBar.moveAbove(detailLabel);
						progressBarComposite.layout(true);
					} else {
						progressBar.setMaximum(totalWork);	
					}
				}
			});
		}
	}

	@Override
	public void done() {
		if (SWTUtil.isDisposed(shell)) {
			return;
		}
		
		display.asyncExec(() -> {
			done = true;
//			shell.setVisible(false);
			if (!SWTUtil.isDisposed(shell)) {
				shell.close();
				executorService.shutdown();
			}
		});
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public boolean isCanceled() {
		return cancelled;
	}

	@Override
	public void setCanceled(boolean value) {
		if (mayCancel) {
			this.cancelled = value;
			setDetailMessage("Cancelled!");
		}
	}

	@Override
	public void setTaskName(final String name) {
		if (!shell.isDisposed())
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					taskName = name;
					taskNameLabel.setText(taskName);
				}
			});
	}

	public void setDetailMessage(final String message) {
		if (!shell.isDisposed())
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					detailLabel.setText(message);
				}
			});
	}

	@Override
	public void subTask(final String name) {
		if (!shell.isDisposed())
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					subTaskLabel.setText(name);
				}
			});
	}

	@Override
	public void worked(final int work) {
		if (!shell.isDisposed())
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if ((progressBar.getStyle() & SWT.INDETERMINATE) == 0)
						progressBar.setSelection(work);
				}
			});
	}
}
