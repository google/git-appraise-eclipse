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

import com.google.appraise.eclipse.core.AppraisePluginUtils;
import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;
import com.google.appraise.eclipse.ui.ReviewMarkerAttributes;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import java.util.List;

/**
 * Creates markers for a given task.
 */
public class ReviewMarkerManager {

  private final TaskData taskData;
  private final TaskRepository taskRepository;

  public ReviewMarkerManager(TaskRepository taskRepository, TaskData taskData) {
    this.taskRepository = taskRepository;
    this.taskData = taskData;
  }

  /**
   * Creates all the markers for this task data instance.
   */
  public void createMarkers() {
    List<TaskAttribute> comments =
        taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_COMMENT);
    for (TaskAttribute commentAttr : comments) {
      markComment(taskRepository, commentAttr, taskData.getTaskId());
    }
  }

  /**
   * Adds a marker for the given comment, assuming it has a location attached.
   */
  private void markComment(
      TaskRepository taskRepository, TaskAttribute commentAttr, String taskId) {
    IProject project = AppraisePluginUtils.getProjectForRepository(taskRepository);

    String filePath = getFilePath(commentAttr);
    IResource resource = project;
    if (filePath != null) {
      resource = project.getFile(filePath);
      if (resource == null || !resource.exists()) {
        return;
      }
    }

    try {
      IMarker marker = resource.createMarker(AppraiseUiPlugin.REVIEW_TASK_MARKER_ID);
      marker.setAttribute(IMarker.MESSAGE, getMessage(commentAttr));
      marker.setAttribute(IMarker.TRANSIENT, true);
      if (filePath != null) {
        marker.setAttribute(IMarker.LINE_NUMBER, getLineNumber(commentAttr));
      }
      marker.setAttribute(IMarker.USER_EDITABLE, false);
      TaskAttribute authorAttribute = commentAttr.getMappedAttribute(TaskAttribute.COMMENT_AUTHOR);
      if (authorAttribute != null) {
        marker.setAttribute(
            ReviewMarkerAttributes.REVIEW_AUTHOR_MARKER_ATTRIBUTE, authorAttribute.getValue());
      }
      marker.setAttribute(
          ReviewMarkerAttributes.REVIEW_DATETIME_MARKER_ATTRIBUTE,
          commentAttr.getMappedAttribute(TaskAttribute.COMMENT_DATE).getValue());
      marker.setAttribute(
          ReviewMarkerAttributes.REVIEW_ID_MARKER_ATTRIBUTE, getCommentId(commentAttr));
      marker.setAttribute(
          ReviewMarkerAttributes.REVIEW_RESOLVED_MARKER_ATTRIBUTE,
          getResolvedDisplayText(commentAttr));
      marker.setAttribute("TaskId", taskId);
    } catch (CoreException e) {
      AppraiseUiPlugin.logError("Failed to create marker at " + filePath, e);
    }
  }
  
  private String getFilePath(TaskAttribute commentAttr) {
    final TaskAttribute locationFileAttr =
        commentAttr.getAttribute(AppraiseReviewTaskSchema.COMMENT_LOCATION_FILE);
    if (locationFileAttr != null) {
      return locationFileAttr.getValue();
    }
    return null;
  }

  private String getMessage(TaskAttribute commentAttr) {
    final TaskAttribute msgAttr = commentAttr.getAttribute(TaskAttribute.COMMENT_TEXT);
    if (msgAttr != null) {
      return msgAttr.getValue();
    }
    return "";
  }

  private int getLineNumber(TaskAttribute commentAttr) {
    final TaskAttribute locationFileAttr =
        commentAttr.getAttribute(AppraiseReviewTaskSchema.COMMENT_LOCATION_LINE);
    if (locationFileAttr != null) {
      return Integer.parseInt(locationFileAttr.getValue());
    }
    return 0;
  }

  private String getResolvedDisplayText(TaskAttribute commentAttr) {
    final TaskAttribute resolvedAttr =
        commentAttr.getAttribute(AppraiseReviewTaskSchema.COMMENT_RESOLVED_ATTRIBUTE);
    if (resolvedAttr != null) {
      if ("true".equals(resolvedAttr.getValue())) {
        return "Yes";
      } else {
        return "No";
      }
    }
    return "";
  }

  private String getCommentId(TaskAttribute commentAttr) {
    final TaskAttribute idAttr =
        commentAttr.getAttribute(AppraiseReviewTaskSchema.COMMENT_ID_ATTRIBUTE);
    if (idAttr != null) {
      return idAttr.getValue();
    }
    return null;
  }
}
