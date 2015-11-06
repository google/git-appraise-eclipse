/*******************************************************************************
 * Copyright (c) 2015 Google and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Scott McMaster - initial implementation
 *******************************************************************************/
package com.google.appraise.eclipse.ui;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Modal dialog to collect new review comments and edit existing ones.
 * Pops up over the text editor when there is an active review.
 */
public class EditCommentDialog extends TitleAreaDialog {
  public enum Mode {
    NEW,
    REPLY
  }
  
  private Text commentText;
  private Combo resolvedCombo;
  private Button resolvedCheck;

  private String comment;
  private Boolean resolved;
  private String replyToText;
  private String replyToAuthor;
  
  private final Mode mode;

  public EditCommentDialog(Shell parentShell, Mode mode) {
    super(parentShell);
    this.mode = mode;
  }

  @Override
  public void create() {
    super.create();
    setTitle("Review Comment");
    setMessage("Enter the new review comment", IMessageProvider.INFORMATION);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout layout = new GridLayout(2, false);
    container.setLayout(layout);
    
    if (mode == Mode.REPLY) {
      createReplyToAuthor(container);
      createReplyToText(container);
    }
    createCommentText(container);
    createResolvedWidget(container);
    commentText.setFocus();
    return area;
  }

  private void createReplyToText(Composite container) {
    Label lblReplyToText = new Label(container, SWT.NONE);
    lblReplyToText.setText("Said:");
    GridData replyToLblLayoutData = new GridData();
    replyToLblLayoutData.verticalAlignment = GridData.BEGINNING;
    lblReplyToText.setLayoutData(replyToLblLayoutData);

    Text replyToTextData = new Text(container, SWT.MULTI | SWT.READ_ONLY);
    replyToTextData.setText(replyToText);
  }

  private void createReplyToAuthor(Composite container) {
    Label lblReplyToAuthor = new Label(container, SWT.NONE);
    lblReplyToAuthor.setText("Reviewer:");
    Text replyToAuthorData = new Text(container, SWT.MULTI | SWT.READ_ONLY);
    replyToAuthorData.setText(replyToAuthor);
  }

  private void createCommentText(Composite container) {
    Label lblComment = new Label(container, SWT.NONE);
    if (mode == Mode.NEW) {
      lblComment.setText("Comment:");
    } else {
      lblComment.setText("Reply:");
    }
    GridData commentLblLayoutData = new GridData();
    commentLblLayoutData.verticalAlignment = GridData.BEGINNING;
    lblComment.setLayoutData(commentLblLayoutData);
    
    GridData commentTextLayoutData = new GridData();
    commentTextLayoutData.grabExcessHorizontalSpace = true;
    commentTextLayoutData.grabExcessVerticalSpace = true;
    commentTextLayoutData.horizontalAlignment = GridData.FILL;
    commentTextLayoutData.verticalAlignment = GridData.FILL;

    commentText = new Text(container, SWT.BORDER | SWT.MULTI);
    commentText.setLayoutData(commentTextLayoutData);
  }

  private void createResolvedWidget(Composite container) {
    Label lbtLastName = new Label(container, SWT.NONE);
    lbtLastName.setText("Resolved?");

    GridData resolvedCheckLayoutData = new GridData();
    if (mode == Mode.REPLY) {
      resolvedCheckLayoutData.grabExcessHorizontalSpace = true;
      resolvedCheckLayoutData.horizontalAlignment = GridData.FILL;
      resolvedCheck = new Button(container, SWT.CHECK);
      resolvedCheck.setLayoutData(resolvedCheckLayoutData);
    } else {
      resolvedCombo = new Combo(container, SWT.READ_ONLY);
      resolvedCombo.setItems(new String[] {"", "Yes", "No"});
      resolvedCombo.setLayoutData(resolvedCheckLayoutData);
    }
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  private void saveInput() {
    comment = commentText.getText();
    String resolvedValue = resolvedCombo.getText();
    if (mode == Mode.REPLY) {
      resolved = resolvedCheck.getSelection();
      if (!resolved) {
        // Interpret no selection as no resolved value in the comment.
        resolved = null;
      }
    } else {
      if ("Yes".equals(resolvedValue)) {
        resolved = true;
      } else if ("No".equals(resolvedValue)) {
        resolved = false;
      } else {
        resolved = null;
      }
    }
  }

  @Override
  protected void okPressed() {
    saveInput();
    super.okPressed();
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Boolean getResolved() {
    return resolved;
  }

  public void setResolved(Boolean resolved) {
    this.resolved = resolved;
  }

  public String getReplyToText() {
    return replyToText;
  }

  public void setReplyToText(String replyToText) {
    this.replyToText = replyToText;
  }

  public String getReplyToAuthor() {
    return replyToAuthor;
  }

  public void setReplyToAuthor(String replyToAuthor) {
    this.replyToAuthor = replyToAuthor;
  }
}
