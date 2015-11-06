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

import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;

/**
 * Custom marker view field for the review comment resolved field.
 */
public class Resolved extends MarkerField {
  @Override
  public String getValue(MarkerItem item) {
    String result =
        item.getAttributeValue(ReviewMarkerAttributes.REVIEW_RESOLVED_MARKER_ATTRIBUTE, "");
    if ("true".equals(result)) {
      return "yes";
    }
    return "";
  }
}
