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
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.ColumnSpan;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.RowSpan;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.io.IOException;

/**
 * Custom attribute editor for the review commit. Handles jumping to the commit
 * in the Eclipse commit editor.
 */
public class CommitAttributeEditor extends AbstractAttributeEditor {
  private Link link;

  public CommitAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
    super(manager, taskAttribute);
    setLayoutHint(new LayoutHint(RowSpan.SINGLE, ColumnSpan.SINGLE));
  }

  @Override
  public void createControl(final Composite parent, FormToolkit toolkit) {
    link = new Link(parent, SWT.BORDER);
    link.setText("<a>" + getValue() + "</a>");
    link.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        try {
          RepositoryCommit commit = getCommit();
          if (commit != null) {
            CommitEditor.openQuiet(commit);
          } else {
            MessageDialog alert = new MessageDialog(parent.getShell(), "Oops", null,
                "Commit " + getValue() + " not found", MessageDialog.ERROR, new String[] {"OK"}, 0);
            alert.open();
          }
        } catch (IOException e) {
          AppraiseUiPlugin.logError("Error reading commit " + getValue(), e);
        }
      }
    });
    setControl(link);
  }

  public String getValue() {
    return getAttributeMapper().getValue(getTaskAttribute());
  }

  private RepositoryCommit getCommit() throws IOException {
    Repository repository =
        AppraisePluginUtils.getGitRepoForRepository(this.getModel().getTaskRepository());
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(ObjectId.fromString(getValue()));
      if (commit != null) {
        return new RepositoryCommit(repository, commit);
      }
    } catch (MissingObjectException e) {
      AppraiseUiPlugin.logError("Commit not found " + getValue(), e);
    } catch (IncorrectObjectTypeException e) {
      AppraiseUiPlugin.logError("Not a commit " + getValue(), e);
    }
    return null;
  }
}
