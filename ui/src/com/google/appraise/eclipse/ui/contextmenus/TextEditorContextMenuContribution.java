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

import com.google.appraise.eclipse.core.AppraiseTaskMapper;
import com.google.appraise.eclipse.core.client.data.ReviewComment;
import com.google.appraise.eclipse.core.client.data.ReviewCommentLocation;
import com.google.appraise.eclipse.core.client.data.ReviewCommentLocationRange;
import com.google.appraise.eclipse.ui.EditCommentDialog;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Creates a context menu for text editors that allows creation of new
 * review comments.
 */
public class TextEditorContextMenuContribution extends ContributionItem {
  private final ITextEditor editor;

  public TextEditorContextMenuContribution(ITextEditor editor) {
    this.editor = editor;
  }

  @Override
  public void fill(Menu menu, int index) {
    MenuItem submenuItem = new MenuItem(menu, SWT.CASCADE, index);
    submenuItem.setText("&Appraise Review Comments");
    Menu submenu = new Menu(menu);
    submenuItem.setMenu(submenu);

    MenuItem reviewCommentMenuItem = new MenuItem(submenu, SWT.CHECK);
    reviewCommentMenuItem.setText("New &Review Comment...");
    reviewCommentMenuItem.addSelectionListener(createReviewCommentSelectionListener());

    MenuItem fileCommentMenuItem = new MenuItem(submenu, SWT.CHECK);
    fileCommentMenuItem.setText("New &File Comment...");
    fileCommentMenuItem.addSelectionListener(createFileCommentSelectionListener());

    MenuItem fileLineCommentMenuItem = new MenuItem(submenu, SWT.CHECK);
    fileLineCommentMenuItem.setText("New &Line Comment...");
    fileLineCommentMenuItem.addSelectionListener(createFileLineCommentSelectionListener());

    // Can only add Appraise comments if there is an active Appraise review task.
    ITask activeTask = TasksUi.getTaskActivityManager().getActiveTask();
    submenuItem.setEnabled(activeTask != null
        && AppraiseTaskMapper.APPRAISE_REVIEW_TASK_KIND.equals(activeTask.getTaskKind()));
  }

  private SelectionAdapter createReviewCommentSelectionListener() {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        createComment(null, 0);
      }
    };
  }

  private SelectionAdapter createFileCommentSelectionListener() {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        createComment(getFilePath(), 0);
      }
    };
  }

  private SelectionAdapter createFileLineCommentSelectionListener() {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        createComment(getFilePath(), getSelectedLineNumber());
      }
    };
  }

  /**
   * Returns the 1-based line number of the current text editor selection,
   * or 0 if some weird error occurs. If there is a multi-line selection. the
   * first line will get returned. Which is what we want for the purposes of
   * creating a review comment.
   */
  private int getSelectedLineNumber() {
    ITextSelection textSelection =
        (ITextSelection) editor.getSite().getSelectionProvider().getSelection();
    IDocumentProvider provider = editor.getDocumentProvider();
    IDocument document = provider.getDocument(editor.getEditorInput());
    int offset = textSelection.getOffset();
    try {
      return document.getLineOfOffset(offset) + 1;
    } catch (BadLocationException e) {
      return 0;
    }
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
   * Creates a comment in the active review at the given line number and file,
   * a file-level comment if the line number is 0, or a review-level comment
   * if there is no file specified.
   */
  private void createComment(String path, int lineNumber) {
    Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    EditCommentDialog dialog = new EditCommentDialog(activeShell, EditCommentDialog.Mode.NEW);
    dialog.create();
    if (dialog.open() == Window.OK) {
      ReviewComment comment = new ReviewComment();
      comment.setDescription(dialog.getComment());
      comment.setResolved(dialog.getResolved());

      ReviewCommentLocation location = new ReviewCommentLocation();
      location.setCommit(AppraiseUiPlugin.getDefault().getCurrentCommit());
      if (path != null && !path.isEmpty()) {
        location.setPath(path);
      }
      if (lineNumber > 0) {
        ReviewCommentLocationRange range = new ReviewCommentLocationRange();
        range.setStartLine(lineNumber);
        location.setRange(range);
      }
      comment.setLocation(location);
      AppraiseUiPlugin.getDefault().writeCommentForActiveTask(comment);
    }
  }
}
