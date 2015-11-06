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
package com.google.appraise.eclipse.ui.editor;

import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Custom attribute editor for review comments.
 */
public class CommentAttributeEditor extends AbstractAttributeEditor {
  private Link fileLink;
  private Label commitLabel;
  private Label label;

  public CommentAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
    super(manager, taskAttribute);
  }

  @Override
  public void createControl(final Composite parent, FormToolkit toolkit) {
    Composite composite = new Composite(parent, SWT.NONE);
    RowLayout layout = new RowLayout(SWT.VERTICAL);
    layout.wrap = true;
    layout.fill = true;
    layout.justify = false;
    composite.setLayout(layout);

    label = new Label(composite, SWT.NONE);
    label.setText(getValue());

    final String commit = getCommit();
    final String filePath = getFilePath();
    final int lineNo = getLineNumber();
    if (filePath != null && !filePath.isEmpty()) {
      fileLink = new Link(composite, SWT.BORDER);
      fileLink.setText("<a>" + filePath + '(' + lineNo + ")</a>");
      fileLink.addListener(SWT.Selection, new Listener() {
        @Override
        public void handleEvent(Event event) {
          AppraiseUiPlugin.openFileInEditor(filePath, getModel().getTaskRepository());
        }
      });
    }

    if (commit != null) {
      commitLabel = new Label(composite, SWT.BORDER);
      commitLabel.setText(commit);
    }

    composite.pack();
    setControl(composite);
  }

  private String getFilePath() {
    final TaskAttribute locationFileAttr = getTaskAttribute().getParentAttribute().getAttribute(
        AppraiseReviewTaskSchema.COMMENT_LOCATION_FILE);
    if (locationFileAttr != null) {
      return locationFileAttr.getValue();
    }
    return null;
  }

  private int getLineNumber() {
    final TaskAttribute locationFileAttr = getTaskAttribute().getParentAttribute().getAttribute(
        AppraiseReviewTaskSchema.COMMENT_LOCATION_LINE);
    if (locationFileAttr != null && locationFileAttr.getValue() != null &&
        locationFileAttr.getValue().length() > 0) {
      return Integer.parseInt(locationFileAttr.getValue());
    }
    return 0;
  }

  private String getCommit() {
    final TaskAttribute locationCommitAttr = getTaskAttribute().getParentAttribute().getAttribute(
        AppraiseReviewTaskSchema.COMMENT_LOCATION_COMMIT);
    if (locationCommitAttr != null) {
      return locationCommitAttr.getValue();
    }
    return null;
  }

  public String getValue() {
    return getAttributeMapper().getValue(getTaskAttribute());
  }
}
