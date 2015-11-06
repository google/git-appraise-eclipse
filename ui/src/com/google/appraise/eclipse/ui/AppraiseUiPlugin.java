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
import com.google.appraise.eclipse.core.AppraisePluginReviewClient;
import com.google.appraise.eclipse.core.AppraisePluginUtils;
import com.google.appraise.eclipse.core.AppraiseTaskMapper;
import com.google.appraise.eclipse.core.client.data.ReviewComment;
import com.google.appraise.eclipse.core.client.git.GitClientException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;

/**
 * The activator lifecycle plugin for the Appraise UI extensions, plus a couple
 * of utility routines.
 */
public class AppraiseUiPlugin extends AbstractUIPlugin {
  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.appraise.eclipse.ui"; // $NON-NLS-1$

  // The id of the review task marker in the editor.
  public static final String REVIEW_TASK_MARKER_ID = "com.google.appraise.eclipse.ui.reviewtask";

  // The shared instance
  private static AppraiseUiPlugin plugin;

  /**
   * The constructor
   */
  public AppraiseUiPlugin() {}

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static AppraiseUiPlugin getDefault() {
    return plugin;
  }

  /**
   * Helper method to log errors as {@link IStatus}.
   */
  public static void logError(String message, Exception e) {
    IStatus status =
        new Status(IStatus.ERROR, AppraiseUiPlugin.PLUGIN_ID, "Error reading commit " + message, e);
    getDefault().getLog().log(status);
  }

  public static void logError(String message) {
    IStatus status =
        new Status(IStatus.ERROR, AppraiseUiPlugin.PLUGIN_ID, "Error reading commit " + message);
    getDefault().getLog().log(status);
  }

  /**
   * Helper method to write a comment into the active task. Does nothing if
   * there is no active task, or the active task is not a Appraise review.
   */
  public void writeCommentForActiveTask(ReviewComment comment) {
    ITask activeTask = TasksUi.getTaskActivityManager().getActiveTask();
    if (activeTask == null) {
      return;
    }
    if (!AppraiseTaskMapper.APPRAISE_REVIEW_TASK_KIND.equals(activeTask.getTaskKind())) {
      return;
    }

    TaskRepository taskRepository = TasksUi.getRepositoryManager().getRepository(
        AppraiseConnectorPlugin.CONNECTOR_KIND, activeTask.getRepositoryUrl());
    try {
      AppraisePluginReviewClient client = new AppraisePluginReviewClient(taskRepository);
      client.writeComment(activeTask.getTaskId(), comment);
    } catch (GitClientException e) {
      AppraiseUiPlugin.logError("Error writing comment for " + activeTask.getTaskId(), e);
    }
  }

  /**
   * Returns the current Git branch, which in the detached head state (should
   * be true in the review workflow) will be the commit id.
   */
  public String getCurrentCommit() {
    ITask activeTask = TasksUi.getTaskActivityManager().getActiveTask();
    TaskRepository taskRepository = TasksUi.getRepositoryManager().getRepository(
        AppraiseConnectorPlugin.CONNECTOR_KIND, activeTask.getRepositoryUrl());
    Repository repo = AppraisePluginUtils.getGitRepoForRepository(taskRepository);
    try {
      return repo.getBranch();
    } catch (IOException e) {
      AppraiseUiPlugin.logError("Failed to retrive git branch", e);
      return null;
    }
  }

  /**
   * Helper method to open the given file in the workspace editor.
   */
  public static void openFileInEditor(String filePath, TaskRepository taskRepository) {
    Repository repository = AppraisePluginUtils.getGitRepoForRepository(taskRepository);
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    String fullPath =
        new Path(repository.getWorkTree().getAbsolutePath()).append(filePath).toOSString();
    File file = new File(fullPath);
    if (!file.exists()) {
      AppraiseUiPlugin.logError("File to open not found: " + fullPath);
      return;
    }
    IWorkbenchPage page = window.getActivePage();
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile fileResource = root.getFileForLocation(new Path(file.getAbsolutePath()));
    if (fileResource != null) {
      try {
        IDE.openEditor(page, fileResource, OpenStrategy.activateOnOpen());
      } catch (PartInitException e) {
        AppraiseUiPlugin.logError("Failed to open editor for " + filePath, e);
      }
    } else {
      IFileStore store = EFS.getLocalFileSystem().getStore(new Path(file.getAbsolutePath()));
      try {
        IDE.openEditor(page, new FileStoreEditorInput(store), EditorsUI.DEFAULT_TEXT_EDITOR_ID);
      } catch (PartInitException e) {
        AppraiseUiPlugin.logError("Failed to open editor for " + filePath, e);
      }
    }
  }
}
