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

import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;

/**
 * Custom task mapper for use with Appraise.
 */
public class AppraiseTaskMapper extends TaskMapper {

  public static final String APPRAISE_REVIEW_TASK_KIND = "appraisereview";
  
  public AppraiseTaskMapper(TaskData taskData) {
    super(taskData);
  }

  @Override
  public String getTaskKind() {
    return APPRAISE_REVIEW_TASK_KIND;
  }
}
