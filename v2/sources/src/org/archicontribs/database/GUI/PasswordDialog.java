/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class opens up an input dialog and asks for a password
 * 
 * @author Herve Jouin
 */
public class passwordDialog extends Dialog {
    private Text txtPassword;
    Button btnShowPassword;
    private String password = "";
    

	/**
	 * Creates the dialog
	 * @param parentShell
	 */
	public passwordDialog(Shell parentShell) {
		super(parentShell);
	}

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        FormLayout layout = new FormLayout();
        composite.setLayout(layout);

        Label lblPassword = new Label(composite, SWT.NONE);
        lblPassword.setText("Password:");
        FormData fd = new FormData();
		fd.top = new FormAttachment(0, 20);
		fd.left = new FormAttachment(0, 10);
		lblPassword.setLayoutData(fd);
		
		this.btnShowPassword = new Button(composite, SWT.TOGGLE);
		this.btnShowPassword.setImage(DBGui.LOCK_ICON);
		this.btnShowPassword.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.right = new FormAttachment(100, -20);
		this.btnShowPassword.setLayoutData(fd);
		this.btnShowPassword.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { showOrHidePasswordCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});

        this.txtPassword = new Text(composite, SWT.BORDER| SWT.PASSWORD);
        this.txtPassword.setText(this.password);
        fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblPassword, 10);
		fd.right = new FormAttachment(this.btnShowPassword, -10);
		this.txtPassword.setLayoutData(fd);
		
		
        
        return composite;
    }
    
	/**
	 * Called when the "showPassword" button is pressed
	 */
	public void showOrHidePasswordCallback() {
		this.txtPassword.setEchoChar(this.btnShowPassword.getSelection() ? 0x25cf : '\0' );
		this.btnShowPassword.setImage(this.btnShowPassword.getSelection() ? DBGui.LOCK_ICON : DBGui.UNLOCK_ICON);
	}

    @Override
    protected Point getInitialSize() {
        return new Point(450, 300);
    }

    @Override
    protected void okPressed() {
        this.password = this.txtPassword.getText();
        super.okPressed();
    }

    /**
     * Get password
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Set password
     * @param passwrd
     */
    public void setPassword(String passwrd) {
        this.password = passwrd;
    }
}
