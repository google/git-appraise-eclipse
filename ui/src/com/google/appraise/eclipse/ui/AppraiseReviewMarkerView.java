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

import org.eclipse.ui.views.markers.MarkerSupportView;

/**
 * Custom marker view for Appraise-style review tasks.
 */
public class AppraiseReviewMarkerView extends MarkerSupportView {
  public AppraiseReviewMarkerView() {
    super("com.google.appraise.eclipse.ui.reviewMarkerGenerator");
  }
}
