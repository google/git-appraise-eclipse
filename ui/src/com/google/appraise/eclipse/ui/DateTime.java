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

import java.text.DateFormat;

import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;

/**
 * Custom marker view field for review comment timestamps.
 */
public class DateTime extends MarkerField {
  @Override
  public String getValue(MarkerItem item) {
    String value =
        item.getAttributeValue(ReviewMarkerAttributes.REVIEW_DATETIME_MARKER_ATTRIBUTE, "");
    if (!value.isEmpty()) {
      DateFormat df = DateFormat.getDateTimeInstance();
      return df.format(Long.parseLong(value));
    }
    return "Unknown";
  }
}
