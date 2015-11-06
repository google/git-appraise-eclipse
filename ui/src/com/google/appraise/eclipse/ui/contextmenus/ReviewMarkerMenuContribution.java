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

import com.google.appraise.eclipse.core.client.data.ReviewComment;
import com.google.appraise.eclipse.core.client.data.ReviewCommentLocation;
import com.google.appraise.eclipse.core.client.data.ReviewCommentLocationRange;
import com.google.appraise.eclipse.ui.EditCommentDialog;
import com.google.appraise.eclipse.ui.ReviewMarkerAttributes;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.List;

/**
 * Creates a marker context menu contribution that organizes all the given
 * review comments under a submenu.
 */
public class ReviewMarkerMenuContribution extends ContributionItem {
  private final List<IMarker> markers;
  private final ITextEditor editor;

  public ReviewMarkerMenuContribution(ITextEditor editor, List<IMarker> markers) {
    this.markers = markers;
    this.editor = editor;
  }

  @Override
  public void fill(Menu menu, int index) {
    MenuItem submenuItem = new MenuItem(menu, SWT.CASCADE, index);
    submenuItem.setText("&Appraise Review Comments");
    Menu submenu = new Menu(menu);
    submenuItem.setMenu(submenu);
    for (IMarker marker : markers) {
      MenuItem menuItem = new MenuItem(submenu, SWT.CHECK);
      menuItem.setText(marker.getAttribute(IMarker.MESSAGE, ""));
      menuItem.addSelectionListener(createDynamicSelectionListener(marker));
    }
  }

  private SelectionAdapter createDynamicSelectionListener(final IMarker marker) {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        replyToComment(marker);
      }
    };
  }

  /**
   * Returns the path to the active file, minus the Eclipse project name.
   */
  private String getFilePath() {
    IEditorInput input = editor.getEditorInput();
    IFile file = ((FileEditorInput) input).getFile();
    return file.getFullPath().toPortableString().replaceFirst(
        "/" + file.getProject().getName(), "");
  }

  /**
   * Creates a reply to the comment represented by the given marker.
   */
  private void replyToComment(IMarker marker) {
    Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    EditCommentDialog dialog = new EditCommentDialog(activeShell, EditCommentDialog.Mode.REPLY);
    dialog.setReplyToText(marker.getAttribute(IMarker.MESSAGE, ""));
    dialog.setReplyToAuthor(
        marker.getAttribute(ReviewMarkerAttributes.REVIEW_AUTHOR_MARKER_ATTRIBUTE, ""));

    dialog.create();
    if (dialog.open() == Window.OK) {
      ReviewComment comment = new ReviewComment();
      comment.setDescription(dialog.getComment());
      comment.setResolved(dialog.getResolved());
      String parentCommentId =
          marker.getAttribute(ReviewMarkerAttributes.REVIEW_ID_MARKER_ATTRIBUTE, "");
      comment.setParent(parentCommentId);

      ReviewCommentLocation location = new ReviewCommentLocation();
      location.setCommit(AppraiseUiPlugin.getDefault().getCurrentCommit());
      location.setPath(getFilePath());
      ReviewCommentLocationRange range = new ReviewCommentLocationRange();
      range.setStartLine(marker.getAttribute(IMarker.LINE_NUMBER, 0));
      location.setRange(range);
      comment.setLocation(location);
      AppraiseUiPlugin.getDefault().writeCommentForActiveTask(comment);
    }
  }
}
