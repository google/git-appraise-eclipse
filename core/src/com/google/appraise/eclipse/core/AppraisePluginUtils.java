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
package com.google.appraise.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Useful methods for working with projects, repositories, etc.
 */
public class AppraisePluginUtils {

  /**
   * Gets the Eclipse project (if any) for the given task repository.
   */
  public static IProject getProjectForRepository(TaskRepository repo) {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (repo.getUrl().endsWith(project.getName())) {
        return project;
      }
    }
    return null;
  }
  
  /**
   * Gets the git repository object (if any) associated with the given task repository.
   */
  public static Repository getGitRepoForRepository(TaskRepository repo) {
    IProject project = getProjectForRepository(repo);
    GitProvider provider = (GitProvider) RepositoryProvider.getProvider(project, GitProvider.ID);
    if (provider != null) {
      return provider.getData().getRepositoryMapping(project).getRepository();
    }
    return null;
  }
}

