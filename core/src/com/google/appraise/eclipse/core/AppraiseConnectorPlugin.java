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
package com.google.appraise.eclipse.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The Appraise plugin bundle.
 */
public class AppraiseConnectorPlugin extends Plugin {
  /**
   * The global plugin instance.
   */
  public static AppraiseConnectorPlugin plugin;

  /**
   * The plugin id.
   */
  public static final String PLUGIN_ID = "com.google.appraise.eclipse.core";

  /**
   * The type of the connector.
   */
  public static final String CONNECTOR_KIND = "com.google.appraise";

  /**
   * Connector query attribute for the user as review requestor.
   */
  public static final String QUERY_REQUESTER = PLUGIN_ID + ".requestor";

  /**
   * Connector query attribute for the user as reviewer.
   */
  public static final String QUERY_REVIEWER = PLUGIN_ID + ".reviewer";

  /**
   * Connector query attribute for the review commit hash prefix.
   */
  public static final String QUERY_REVIEW_COMMIT_PREFIX = PLUGIN_ID + ".reviewcommitprefix";

  private static BundleContext context;

  static BundleContext getContext() {
    return context;
  }

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    super.start(bundleContext);
    plugin = this;
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    plugin = null;
    super.stop(bundleContext);
  }

  /**
   * Returns the shared instance.
   */
  public static AppraiseConnectorPlugin getDefault() {
    return plugin;
  }

  public static void logError(final String message, final Throwable throwable) {
    getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, throwable));
  }

  public static void logWarning(final String message, final Throwable throwable) {
    getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, throwable));
  }
}
