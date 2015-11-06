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
import com.google.appraise.eclipse.core.AppraisePluginUtils;
import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskActivationAdapter;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * Handles task activiation and deactiviation for Appraise tasks.
 */
public class AppraiseReviewTaskActivationListener extends TaskActivationAdapter {
  /**
   * Hold on to the name of the branch we will go to when a task is deactivated.
   */
  private String previousBranch;

  @Override
  public void taskActivated(ITask task) {
    if (task == null) {
      return;
    }

    TaskData taskData = loadTaskData(task);
    if (taskData == null) {
      AppraiseUiPlugin.logError("Failed to load task data for " + task.getTaskId());
      return;
    }

    TaskRepository taskRepository = TasksUi.getRepositoryManager().getRepository(
        AppraiseConnectorPlugin.CONNECTOR_KIND, taskData.getRepositoryUrl());

    previousBranch = null;
    String reviewBranch =
        taskData.getRoot()
            .getAttribute(AppraiseReviewTaskSchema.getDefault().REVIEW_REF.getKey())
            .getValue();
    if (reviewBranch != null && !reviewBranch.isEmpty()) {
      promptSwitchToReviewBranch(taskRepository, reviewBranch);
    }

    new ReviewMarkerManager(taskRepository, taskData).createMarkers();
  }

  /**
   * Asks the user if they want to switch to the review branch, and performs
   * the switch if so.
   */
  private void promptSwitchToReviewBranch(TaskRepository taskRepository, String reviewBranch) {
    MessageDialog dialog = new MessageDialog(null, "Appraise Review", null,
        "Do you want to switch to the review branch (" + reviewBranch + ")", MessageDialog.QUESTION,
        new String[] {"Yes", "No"}, 0);
    int result = dialog.open();
    if (result == 0) {
      Repository repo = AppraisePluginUtils.getGitRepoForRepository(taskRepository);
      try (Git git = new Git(repo)) {
        previousBranch = repo.getFullBranch();
        git.checkout().setName(reviewBranch).call();
      } catch (RefNotFoundException rnfe) {
        MessageDialog alert = new MessageDialog(null, "Oops", null,
            "Branch " + reviewBranch + " not found", MessageDialog.INFORMATION, new String[] {"OK"}, 0);
        alert.open();
      } catch (Exception e) {
        AppraiseUiPlugin.logError("Unable to switch to review branch: " + reviewBranch, e);
      }
    }
  }

  @Override
  public void taskDeactivated(ITask task) {
    if (task == null) {
      return;
    }

    TaskData taskData = loadTaskData(task);
    TaskRepository taskRepository = TasksUi.getRepositoryManager().getRepository(
        AppraiseConnectorPlugin.CONNECTOR_KIND, taskData.getRepositoryUrl());

    if (previousBranch != null) {
      Repository repo = AppraisePluginUtils.getGitRepoForRepository(taskRepository);
      try (Git git = new Git(repo)) {
        git.checkout().setName(previousBranch).call();
      } catch (Exception e) {
        AppraiseUiPlugin.logError("Unable to switch to previous branch: " + previousBranch, e);
      }
    }

    int depth = IResource.DEPTH_INFINITE;
    IProject project = AppraisePluginUtils.getProjectForRepository(taskRepository);
    try {
      project.deleteMarkers(AppraiseUiPlugin.REVIEW_TASK_MARKER_ID, true, depth);
    } catch (CoreException e) {
      AppraiseUiPlugin.logError("Error deleting review markers for task " + task.getTaskId(), e);
    }
  }

  /**
   * Gets the task data for the given task.
   */
  private TaskData loadTaskData(ITask task) {
    TaskData taskData = null;
    try {
      taskData = TasksUi.getTaskDataManager().getTaskData(task);
    } catch (CoreException e) {
      AppraiseUiPlugin.logError("Failed to load task data " + task.getTaskId(), e);
    }
    return taskData;
  }
}
