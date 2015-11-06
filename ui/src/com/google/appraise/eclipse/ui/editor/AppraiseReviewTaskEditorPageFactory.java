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

import com.google.appraise.eclipse.core.AppraiseConnectorPlugin;

import org.eclipse.mylyn.commons.ui.CommonImages;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPageFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Creates the main review viewing/editing page.
 */
public class AppraiseReviewTaskEditorPageFactory extends AbstractTaskEditorPageFactory {
  @Override
  public boolean canCreatePageFor(TaskEditorInput input) {
    return (input.getTask().getConnectorKind().equals(AppraiseConnectorPlugin.CONNECTOR_KIND)
        || TasksUiUtil.isOutgoingNewTask(input.getTask(), AppraiseConnectorPlugin.CONNECTOR_KIND));
  }

  @Override
  public IFormPage createPage(TaskEditor parentEditor) {
    return new AppraiseReviewTaskEditorPage(parentEditor);
  }

  @Override
  public Image getPageImage() {
    return CommonImages.getImage(TasksUiImages.REPOSITORY_SMALL);
  }

  @Override
  public String getPageText() {
    return "Appraise Review";
  }
  
  @Override
  public int getPriority() {
      return PRIORITY_TASK;
  }
}
