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

/**
 * Definitions for the attributes we stick on review IMarkers in the source
 * editor. These are mirrored in the reviewmarker extension in plugin.xml.
 */
public class ReviewMarkerAttributes {

  public static final String REVIEW_AUTHOR_MARKER_ATTRIBUTE = "Author";
  
  public static final String REVIEW_ID_MARKER_ATTRIBUTE = "Id";
  
  public static final String REVIEW_DATETIME_MARKER_ATTRIBUTE = "DateTime";
  
  public static final String REVIEW_RESOLVED_MARKER_ATTRIBUTE = "Resolved";
}
