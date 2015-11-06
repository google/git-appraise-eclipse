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
package com.google.appraise.eclipse.ui.wizard;

import com.google.appraise.eclipse.core.AppraiseConnectorPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTemplateManager;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom repository settings page for Appraise.
 */
public class AppraiseRepositorySettingsPage extends AbstractRepositorySettingsPage {
  public AppraiseRepositorySettingsPage(TaskRepository repository) {
    super("Appraise Repository", "Appraise Repository Connector", repository);
    setNeedsAnonymousLogin(false);
    setNeedsEncoding(false);
    setNeedsTimeZone(false);
    setNeedsAdvanced(false);
    setNeedsProxy(false);
    setNeedsValidation(false);
    setNeedsValidateOnFinish(false);
    setNeedsAdvanced(false);
    setNeedsHttpAuth(false);
  }

  @Override
  public String getConnectorKind() {
    return AppraiseConnectorPlugin.CONNECTOR_KIND;
  }

  private List<IProject> getProjects() {
    List<IProject> projects = new ArrayList<IProject>();
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      projects.add(project);
    }
    return projects;
  }

  @Override
  protected void repositoryTemplateSelected(RepositoryTemplate template) {
    repositoryLabelEditor.setStringValue(template.label);
    setUrl(template.repositoryUrl);
    setUserId(System.getProperty("user.name"));
    setPassword("");

    getContainer().updateButtons();
  }

  @Override
  protected void addRepositoryTemplatesToServerUrlCombo() {
    RepositoryTemplateManager templateManager = TasksUiPlugin.getRepositoryTemplateManager();

    for (IProject project : getProjects()) {
      String label = "Appraise: " + project.getName();
      String repositoryUrl = "http://appraise.google.com/" + project.getName();

      boolean found = false;
      for (RepositoryTemplate existing :
          templateManager.getTemplates(AppraiseConnectorPlugin.CONNECTOR_KIND)) {
        if (repositoryUrl.equals(existing.repositoryUrl)) {
          found = true;
          break;
        }
      }

      if (!found) {
        RepositoryTemplate template = new RepositoryTemplate(
            label, repositoryUrl, null, null, null, null, null, null, true, false);
        TasksUiPlugin.getRepositoryTemplateManager().addTemplate(
            AppraiseConnectorPlugin.CONNECTOR_KIND, template);
      }
    }
    super.addRepositoryTemplatesToServerUrlCombo();
  }

  @Override
  protected void createAdditionalControls(Composite parent) {}

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    addRepositoryTemplatesToServerUrlCombo();
  }

  @Override
  protected boolean isValidUrl(String url) {
    return url != null && url.trim().length() > 0;
  }

  @Override
  protected Validator getValidator(TaskRepository repository) {
    return null;
  }
}
