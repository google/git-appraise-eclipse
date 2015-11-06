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

import static org.eclipse.egit.ui.UIPreferences.THEME_DiffAddBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffAddForegroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHeadlineBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHeadlineFont;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHeadlineForegroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHunkBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHunkForegroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffRemoveBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffRemoveForegroundColor;

import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.ColumnSpan;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.RowSpan;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Attribute editor for individual files in a diff.
 */
public class DiffAttributeEditor extends AbstractAttributeEditor {
  
  /**
   * Line type used for diff highlighting.
   */
  private enum DiffLineType {
    ADD,
    REMOVE,
    OTHER,
    HUNK,
    HEADLINE
  }

  public DiffAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
    super(manager, taskAttribute);
    setLayoutHint(new LayoutHint(RowSpan.MULTIPLE, ColumnSpan.MULTIPLE));
  }

  @Override
  public void createControl(final Composite parent, FormToolkit toolkit) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    composite.setLayout(layout);

    final String filePath =
        getTaskAttribute().getAttribute(AppraiseReviewTaskSchema.DIFF_NEWPATH).getValue();

    Link fileLink = new Link(composite, SWT.BORDER);
    fileLink.setText("<a>View in Workspace</a>");
    fileLink.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        AppraiseUiPlugin.openFileInEditor(filePath, getModel().getTaskRepository());
      }
    });

    final String diffText =
        getTaskAttribute().getAttribute(AppraiseReviewTaskSchema.DIFF_TEXT).getValue();

    final StyledText text = new StyledText(composite, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.READ_ONLY);
    text.setText(diffText);
    text.setStyleRanges(getStyleRangesForDiffText(diffText));

    GridData diffTextGridData = new GridData();
    diffTextGridData.grabExcessHorizontalSpace = true;
    diffTextGridData.horizontalAlignment = SWT.FILL;
    text.setLayoutData(diffTextGridData);

    composite.pack();
    setControl(composite);
  }

  /**
   * Figures out all the combined {@link StyleRange}s for the given unified-diff text.
   */
  private StyleRange[] getStyleRangesForDiffText(String diffText) {
    List<StyleRange> ranges = new ArrayList<>();
    if (diffText != null) {
      String[] lines = diffText.split("\n");

      // Read the first line.
      DiffLineType curLineType = getDiffLineType(lines[0]);
      StyleRange currentRange = initDiffStyleRangeForLineType(curLineType, 0);
      int charsRead = lines[0].length() + 1;
      currentRange.length = charsRead;

      // Read the subsequent lines.
      int lineNum = 1;
      for (; lineNum < lines.length; lineNum++) {
        String curLine = lines[lineNum];
        DiffLineType newLineType = getDiffLineType(curLine);
        if (newLineType != curLineType) {
          currentRange.length = charsRead - currentRange.start;
          curLineType = newLineType;
          ranges.add(currentRange);
          currentRange = initDiffStyleRangeForLineType(curLineType, charsRead);
        }
        charsRead += curLine.length();
        charsRead++; // for the newline.
      }

      // Close out the last range.
      currentRange.length = charsRead - currentRange.start;
      ranges.add(currentRange);
    }
    return ranges.toArray(new StyleRange[ranges.size()]);
  }

  /**
   * Calculates the type of line given the prefix in unified-diff format.
   * TODO: This might be overly simplistic...
   */
  private DiffLineType getDiffLineType(String line) {
    if (line.startsWith("@@")) {
      return DiffLineType.HUNK;
    } else if (line.startsWith("diff --git")) {
      return DiffLineType.HEADLINE;
    } else if (line.startsWith("+")) {
      return DiffLineType.ADD;
    } else if (line.startsWith("-")) {
      return DiffLineType.REMOVE;
    } else {
      return DiffLineType.OTHER;
    }
  }

  /**
   * Starts a new {@link StyleRange} given a specific line type.
   */
  private StyleRange initDiffStyleRangeForLineType(DiffLineType lineType, int startTextOffset) {
    ColorRegistry reg =
        PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
    StyleRange range = new StyleRange();
    range.start = startTextOffset;
    switch (lineType) {
      case ADD:
        range.foreground = reg.get(THEME_DiffAddForegroundColor);
        range.background = reg.get(THEME_DiffAddBackgroundColor);
        break;
      case REMOVE:
        range.foreground = reg.get(THEME_DiffRemoveForegroundColor);
        range.background = reg.get(THEME_DiffRemoveBackgroundColor);
        break;
      case HUNK:
        range.foreground = reg.get(THEME_DiffHunkForegroundColor);
        range.background = reg.get(THEME_DiffHunkBackgroundColor);
        break;
      case HEADLINE:
        range.foreground = reg.get(THEME_DiffHeadlineForegroundColor);
        range.background = reg.get(THEME_DiffHeadlineBackgroundColor);
        FontRegistry fontReg =
            PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
        range.font = fontReg.get(THEME_DiffHeadlineFont);
        break;
      default:
        break;
    }
    return range;
  }
}
