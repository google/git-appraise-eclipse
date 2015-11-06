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

import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.mylyn.commons.ui.FillWidthLayout;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import java.util.List;

/**
 * Implements a full-review diff viewer inside a task editor part.
 */
public class AppraiseDiffViewerPart extends AbstractTaskEditorPart {
  private static final String KEY_DIFF_ATTRIBUTE_EDITOR = "diffviewer";

  /**
   * Gets the TYPE_DIFF task attributes off the task data.
   */
  private List<TaskAttribute> getDiffTaskAttributes() {
    TaskData taskData = getModel().getTaskData();
    return taskData.getAttributeMapper().getAttributesByType(taskData,
        AppraiseReviewTaskSchema.TYPE_DIFF);
  }

  @Override
  public void createControl(Composite parent, final FormToolkit toolkit) {
    final List<TaskAttribute> diffTaskAttributes = getDiffTaskAttributes();

    if (diffTaskAttributes == null || diffTaskAttributes.isEmpty()) {
      return;
    }
    int style = ExpandableComposite.TWISTIE | ExpandableComposite.SHORT_TITLE_BAR;
    final Section groupSection = toolkit.createSection(parent, style);
    groupSection.setText("Changes (" + diffTaskAttributes.size() + ')');
    groupSection.clientVerticalSpacing = 0;
    groupSection.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

    if (groupSection.isExpanded()) {
      addDiffViewersToSection(toolkit, diffTaskAttributes, groupSection);
    } else {
      groupSection.addExpansionListener(new ExpansionAdapter() {
        @Override
        public void expansionStateChanged(ExpansionEvent e) {
          if (groupSection.getClient() == null) {
            try {
              getTaskEditorPage().setReflow(false);
              addDiffViewersToSection(toolkit, diffTaskAttributes, groupSection);
            } finally {
              getTaskEditorPage().setReflow(true);
            }
            getTaskEditorPage().reflow();
          }
        }
      });
    }
  }

  /**
   * Helper method to put the individual diff viewers into the given group section.
   */
  private void addDiffViewersToSection(final FormToolkit toolkit,
      final List<TaskAttribute> diffTaskAttributes, final Section groupSection) {
    Composite composite = createDiffViewers(groupSection, toolkit, diffTaskAttributes);
    groupSection.setClient(composite);
  }

  /**
   * Creates the controls to display diffs and returns the resulting composite.
   */
  private Composite createDiffViewers(Composite parent, final FormToolkit toolkit,
      List<TaskAttribute> diffTaskAttributes) {    
    Composite composite = toolkit.createComposite(parent);
    GridLayout contentLayout = new GridLayout();
    contentLayout.marginHeight = 0;
    contentLayout.marginWidth = 0;
    composite.setLayout(contentLayout);

    for (final TaskAttribute diffTaskAttribute : diffTaskAttributes) {
      createDiffViewer(toolkit, composite, diffTaskAttribute);
    }

    return composite;
  }

  /**
   * Creates an individual diff viewer in the given composite.
   */
  private void createDiffViewer(final FormToolkit toolkit, Composite composite,
      final TaskAttribute diffTaskAttribute) {

    int style = ExpandableComposite.TREE_NODE | ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT
        | ExpandableComposite.COMPACT;
    ExpandableComposite diffComposite = toolkit.createExpandableComposite(composite, style);
    diffComposite.clientVerticalSpacing = 0;
    diffComposite.setLayout(new GridLayout());
    diffComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    diffComposite.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TITLE));
    diffComposite.setText(calculateDiffChangeHeader(diffTaskAttribute));

    final Composite diffViewerComposite = toolkit.createComposite(diffComposite);
    diffComposite.setClient(diffViewerComposite);
    diffViewerComposite.setLayout(
        new FillWidthLayout(EditorUtil.getLayoutAdvisor(getTaskEditorPage()), 15, 0, 0, 3));

    diffComposite.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent event) {
        expandCollapseDiff(toolkit, diffViewerComposite, diffTaskAttribute, event.getState());
      }
    });
    GridDataFactory.fillDefaults().grab(true, false).applyTo(diffComposite);
  }

  /**
   * Finds the appropriate title for an individual change given its various attributes.
   */
  private String calculateDiffChangeHeader(TaskAttribute diffTaskAttribute) {
    String newPath = diffTaskAttribute.getAttribute(AppraiseReviewTaskSchema.DIFF_NEWPATH).getValue();
    String oldPath = diffTaskAttribute.getAttribute(AppraiseReviewTaskSchema.DIFF_OLDPATH).getValue();
    String type = diffTaskAttribute.getAttribute(AppraiseReviewTaskSchema.DIFF_TYPE).getValue();
    ChangeType changeType = ChangeType.MODIFY;
    try {
      changeType = ChangeType.valueOf(type);
    } catch (Exception e) {
    }

    switch (changeType) {
      case ADD:
        return newPath + " (Added)";

      case COPY:
        return newPath + " (Copied from " + oldPath + ")";

      case DELETE:
        return newPath + " (Deleted)";

      case RENAME:
        return newPath + " (was " + oldPath + ")";

      case MODIFY:
      default:
        return newPath + " (Modified)";
    }
  }

  private void expandCollapseDiff(FormToolkit toolkit, Composite composite,
      TaskAttribute diffTaskAttribute, boolean expanded) {
    if (expanded && composite.getData(KEY_DIFF_ATTRIBUTE_EDITOR) == null) {
      AbstractAttributeEditor editor = createAttributeEditor(diffTaskAttribute);
      if (editor != null) {
        editor.setDecorationEnabled(false);
        editor.createControl(composite, toolkit);
        composite.setData(KEY_DIFF_ATTRIBUTE_EDITOR, editor);
        getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);
        getTaskEditorPage().reflow();
      }
    } else if (!expanded && composite.getData(KEY_DIFF_ATTRIBUTE_EDITOR) != null) {
      AbstractAttributeEditor editor =
          (AbstractAttributeEditor) composite.getData(KEY_DIFF_ATTRIBUTE_EDITOR);
      editor.getControl().dispose();
      composite.setData(KEY_DIFF_ATTRIBUTE_EDITOR, null);
      getTaskEditorPage().reflow();
    }
  }
}
