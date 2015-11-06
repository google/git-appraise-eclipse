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

import com.google.appraise.eclipse.core.AppraiseConnectorPlugin;
import com.google.appraise.eclipse.ui.wizard.AppraiseRepositorySettingsPage;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

/**
 * The UI extensions for the Appraise plugin.
 */
public class AppraiseConnectorUi extends AbstractRepositoryConnectorUi {

  @Override
  public String getConnectorKind() {
    return AppraiseConnectorPlugin.CONNECTOR_KIND;
  }

  @Override
  public ITaskRepositoryPage getSettingsPage(TaskRepository repository) {
    return new AppraiseRepositorySettingsPage(repository);
  }

  @Override
  public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery query) {
    RepositoryQueryWizard wizard = new RepositoryQueryWizard(repository);
    wizard.addPage(new AppraiseReviewsQueryPage(repository, query));
    return wizard;
  }

  @Override
  public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping selection) {
    return new NewTaskWizard(repository, selection);
  }

  @Override
  public boolean hasSearchPage() {
    return true;
  }
}
