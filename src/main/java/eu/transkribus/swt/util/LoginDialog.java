package eu.transkribus.swt.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt_gui.TrpGuiPrefs;
import eu.transkribus.swt_gui.transcription.autocomplete.TrpAutoCompleteField;

public class LoginDialog extends Dialog {
	private final static Logger logger = LoggerFactory.getLogger(LoginDialog.class);

	private final static String RESET_PW_URL_BASE = "https://account.readcoop.eu/auth/realms/readcoop/login-actions/reset-credentials?client_id=account";
	
	protected Composite container;
	protected Group grpCreds;
	protected Text txtUser;
	protected Text txtPassword;
	protected CCombo serverCombo;
	protected Button rememberCredentials;
	protected Button clearStoredCredentials;
	protected Button autoLogin;

	protected String message;
	protected String[] userProposals;

	protected Label infoLabel;
	protected Label imageLabel;

	protected String[] serverProposals;
	protected int defaultUriIndex;
	
	private String selectedServer;

	TrpAutoCompleteField autocomplete;

	public LoginDialog(Shell parentShell, String message, String[] userProposals, String[] serverProposals,
			int defaultUriIndex) {
		super(parentShell);
		this.message = message;
		this.userProposals = userProposals;
		if(serverProposals == null) {
			serverProposals = new String[]{};
		}
		this.serverProposals = serverProposals;
		this.defaultUriIndex = CoreUtils.bound(defaultUriIndex, 0, serverProposals.length - 1);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginRight = 5;
		layout.marginLeft = 10;
		container.setLayout(layout);
		
		grpCreds = new Group(container, SWT.NONE);
		grpCreds.setLayout(new GridLayout(4, false));
		grpCreds.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		
		initTranskribusAccountFields();
		grpCreds.layout();
		
		if(doShowServerCombo()) { 
			initServerCombo(container);
		} else {
			selectedServer = serverProposals[0];
		}

		autoLogin = new Button(container, SWT.CHECK);
		autoLogin.setText("Auto login");
		autoLogin.setToolTipText("Auto login with last remembered credentials on next startup");

		new Label(container, 0);

		infoLabel = new Label(container, SWT.FLAT);
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
		infoLabel.setForeground(Colors.getSystemColor(SWT.COLOR_DARK_RED));
		infoLabel.setText(message);
		
//		AnimatedGif gif = new AnimatedGif(container, SWT.NONE);
//		gif.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
//		gif.load2("/icons/loading.gif");
//		gif.animate();
//		gif.stop();
		
//		Button testBtn = new Button(container, SWT.PUSH);
//		testBtn.setText("Test");
//		SWTUtil.onSelectionEvent(testBtn, e -> {
//			if (gif.isAnimating()) {
//				gif.stop();
//			}
//			else {
//				gif.animate();
//			}
//		});
		
		postInit();

		return container;
	}

	private boolean doShowServerCombo() {
		return serverProposals.length < 1 || serverProposals.length > 1;
	}

	protected void postInit() {
		// Override this method to perform post init stuff outside the class
		// definition!
	}

	private void initTranskribusAccountFields() {
		
		clearGrpCreds(2);
		
		Label lblUser = new Label(grpCreds, SWT.NONE);
		lblUser.setText("User:");
	
		txtUser = new Text(grpCreds, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtUser.setText("");
		
		Label lblPassword = new Label(grpCreds, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.horizontalIndent = 1;
		lblPassword.setLayoutData(gd_lblNewLabel);
		lblPassword.setText("Password:");
	
		txtPassword = new Text(grpCreds, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtPassword.setText(String.valueOf(""));
		
		autocomplete = new TrpAutoCompleteField(txtUser, new TextContentAdapter(), new String[] {},
				KeyStroke.getInstance(SWT.CTRL, SWT.SPACE), null);
		autocomplete.getAdapter().setEnabled(true);
		autocomplete.getAdapter().setPropagateKeys(true);
		

		rememberCredentials = new Button(grpCreds, SWT.CHECK);
		rememberCredentials.setText("Remember credentials");
		rememberCredentials.setToolTipText("If login is successful those credentials will be stored");

		Composite btnGrp = new Composite(grpCreds, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(2).equalWidth(false).applyTo(btnGrp);
		GridDataFactory.create(GridData.FILL_BOTH).applyTo(btnGrp);
		
		Button resetPwBtn = new Button(btnGrp, SWT.PUSH);
		resetPwBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		resetPwBtn.setText("Forgot password?");
		resetPwBtn.setToolTipText("Opens the password reset page in a new browser tab");
//		resetPwBtn.setImage(Images.getOrLoad("/icons/world.png"));
		
		clearStoredCredentials = new Button(btnGrp, SWT.PUSH);
		clearStoredCredentials.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		clearStoredCredentials.setText("Clear stored credentials");
		clearStoredCredentials.setToolTipText("Clears previously stored credentials from harddisk");
		
		txtUser.addModifyListener(new ModifyListener() {
			@Override public void modifyText(ModifyEvent e) {
				updateCredentialsOnTypedUser();
			}
		});

		txtPassword.addFocusListener(new FocusListener() {
			@Override public void focusLost(FocusEvent e) {
			}

			@Override public void focusGained(FocusEvent e) {
				updateCredentialsOnTypedUser();
			}
		});
		
		resetPwBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				final String resetPwUrl;
				if(!StringUtils.isEmpty(txtUser.getText())) {
					resetPwUrl = RESET_PW_URL_BASE + "&prefillUsername=" + txtUser.getText();
				} else {
					resetPwUrl = RESET_PW_URL_BASE;
				}
				DesktopUtil.browse(resetPwUrl, 
						"Sorry, we could not identify your browser.\nTo reset your password, please go to:\n\n" + RESET_PW_URL_BASE, 
						LoginDialog.this.getParentShell());
			}
		});
		
		clearStoredCredentials.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				try {
					TrpGuiPrefs.clearCredentials();
				} catch (Exception e1) {
					logger.error(e1.getMessage());
				}
			}
		});

		// update credentials for default user if any:
		Pair<String, String> storedCreds = TrpGuiPrefs.getStoredCredentials(null);
		if (storedCreds != null) {
			logger.debug("found stored creds for user: " + storedCreds.getLeft());
			setUsername(storedCreds.getLeft());
			setPassword(storedCreds.getRight());
		}
	}
	
	private void clearGrpCreds(int numColsToInit) {
		for(Control c : grpCreds.getChildren()){
			c.dispose();
		}
		grpCreds.setLayout(new GridLayout(numColsToInit, false));
		grpCreds.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
	}

	private void initServerCombo(Composite container) {
		Label serverLabel = new Label(container, SWT.FLAT);
		serverLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
		serverLabel.setText("Server: ");

		serverCombo = new CCombo(container, SWT.DROP_DOWN);
		serverCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		for (String s : serverProposals) {
			serverCombo.add(s);
		}
		if (serverProposals.length > 0) {
			serverCombo.select(0);
		}
		if (defaultUriIndex >= 0 && defaultUriIndex < serverProposals.length) {
			serverCombo.select(defaultUriIndex);
		}
		selectedServer = serverCombo.getText();
		serverCombo.addModifyListener(new ModifyListener() {
			@Override public void modifyText(ModifyEvent e) {
				logger.trace("Server combo text changed.");
				selectedServer = serverCombo.getText();
			}
		});
	}

	public void setInfo(String info) {
		infoLabel.setText(info);
	}

	public String getSelectedServer() {
		return selectedServer;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Login");
	}

	// override method to use "Login" as label for the OK button
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Login", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		int initialHeight = 270;
		if(doShowServerCombo()) {
			initialHeight = 300;
		}
		return new Point(465, initialHeight);
	}

	public void setMessage(String message) {
		this.message = message;
		infoLabel.setText(message);
	}

	public String getUser() {
		return txtUser.getText();
	}

	public void setPassword(String pw) {
		txtPassword.setText(pw);
	}

	public void setUsername(String username) {
		txtUser.setText(username);
	}

	public char[] getPassword() {
		return txtPassword.getText().toCharArray();
	}

	public boolean isRememberCredentials() {
		return rememberCredentials.getSelection();
	}
	
	public boolean isDisposed() {
		return getShell() == null || getShell().isDisposed();
	}

	void updateCredentialsOnTypedUser() {
		Pair<String, String> storedCreds = TrpGuiPrefs.getStoredCredentials(getUser());
		if (storedCreds != null) {
			setPassword(storedCreds.getRight());
		} else {
			setPassword("");
		}
	}
}