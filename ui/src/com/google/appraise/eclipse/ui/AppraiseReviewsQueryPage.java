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

import com.google.appraise.eclipse.core.AppraiseConnectorPlugin;

import org.eclipse.mylyn.commons.workbench.forms.SectionComposite;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Custom Appraise review query page.
 */
public class AppraiseReviewsQueryPage extends AbstractRepositoryQueryPage2 {
  private Button requesterCheckbox;
  private Button reviewerCheckbox;
  private Text reviewCommitPrefixText;

  public AppraiseReviewsQueryPage(TaskRepository repository, IRepositoryQuery query) {
    super("reviews", repository, query);
    setTitle("Appraise Review Search");
    setDescription("Specify search parameters.");
  }

  @Override
  protected void createPageContent(SectionComposite parent) {
    Composite composite = parent.getContent();
    composite.setLayout(new GridLayout(2, false));
    requesterCheckbox = new Button(composite, SWT.CHECK);
    requesterCheckbox.setText("Requester");
    reviewerCheckbox = new Button(composite, SWT.CHECK);
    reviewerCheckbox.setText("Reviewer");

    Label reviewCommitPrefixLabel = new Label(composite, SWT.NONE);
    reviewCommitPrefixLabel.setText("Review Commit Hash (prefix):");
    reviewCommitPrefixText = new Text(composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    composite.pack();
  }

  @Override
  protected boolean hasRepositoryConfiguration() {
    return true;
  }

  @Override
  protected boolean restoreState(IRepositoryQuery query) {
    String requester = query.getAttribute(AppraiseConnectorPlugin.QUERY_REQUESTER);
    requesterCheckbox.setSelection(Boolean.parseBoolean(requester));

    String reviewer = query.getAttribute(AppraiseConnectorPlugin.QUERY_REVIEWER);
    reviewerCheckbox.setSelection(Boolean.parseBoolean(reviewer));

    String reviewCommitPrefix = query.getAttribute(AppraiseConnectorPlugin.QUERY_REVIEW_COMMIT_PREFIX);
    reviewCommitPrefixText.setText(reviewCommitPrefix);

    return true;
  }

  @Override
  public void applyTo(IRepositoryQuery query) {
    if (getQueryTitle() != null) {
      query.setSummary(getQueryTitle());
    }
    query.setAttribute(
        AppraiseConnectorPlugin.QUERY_REQUESTER, Boolean.toString(requesterCheckbox.getSelection()));
    query.setAttribute(
        AppraiseConnectorPlugin.QUERY_REVIEWER, Boolean.toString(reviewerCheckbox.getSelection()));
    query.setAttribute(AppraiseConnectorPlugin.QUERY_REVIEW_COMMIT_PREFIX, reviewCommitPrefixText.getText());
  }

  @Override
  protected void doRefreshControls() {
  }
}
