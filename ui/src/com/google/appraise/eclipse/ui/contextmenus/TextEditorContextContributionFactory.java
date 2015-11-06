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

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Adds a menu contribution for the text editor popup which will allow creation
 * of new comments.
 */
public class TextEditorContextContributionFactory extends ExtensionContributionFactory {
  @Override
  public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
    ITextEditor editor = (ITextEditor)
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();

    additions.addContributionItem(new TextEditorContextMenuContribution(editor), null);
  }
}
