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
package com.google.appraise.eclipse.ui.contextmenus;

import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a menu contribution for the marker popup which will display comments.
 */
public class ReviewMarkerContributionFactory extends ExtensionContributionFactory {
  @Override
  public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
    ITextEditor editor = (ITextEditor)
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
    IVerticalRulerInfo rulerInfo = editor.getAdapter(IVerticalRulerInfo.class);

    try {
      List<IMarker> markers = getMarkers(editor, rulerInfo);
      additions.addContributionItem(new ReviewMarkerMenuContribution(editor, markers), null);
      if (!markers.isEmpty()) {
        additions.addContributionItem(new Separator(), null);
      }
    } catch (CoreException e) {
      AppraiseUiPlugin.logError("Error creating marker context menus", e);
    }
  }

  private List<IMarker> getMarkers(ITextEditor editor, IVerticalRulerInfo rulerInfo)
      throws CoreException {
    List<IMarker> clickedOnMarkers = new ArrayList<IMarker>();
    for (IMarker marker : getAllMarkers(editor)) {
      if (markerHasBeenClicked(marker, rulerInfo)) {
        clickedOnMarkers.add(marker);
      }
    }

    return clickedOnMarkers;
  }

  private IMarker[] getAllMarkers(ITextEditor editor) throws CoreException {
    return ((FileEditorInput) editor.getEditorInput())
        .getFile()
        .findMarkers(AppraiseUiPlugin.REVIEW_TASK_MARKER_ID, true, IResource.DEPTH_ZERO);
  }

  private boolean markerHasBeenClicked(IMarker marker, IVerticalRulerInfo rulerInfo) {
    return (marker.getAttribute(IMarker.LINE_NUMBER, 0))
        == (rulerInfo.getLineOfLastMouseButtonActivity() + 1);
  }
}
