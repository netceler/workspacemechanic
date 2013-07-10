/*******************************************************************************
 * Copyright (C) 2007, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.google.eclipse.mechanic.plugin.core;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.eclipse.mechanic.MechanicService;
import com.google.eclipse.mechanic.core.recorder.IPreferenceRecordingService;
import com.google.eclipse.mechanic.core.recorder.PreferenceRecordingService;
import com.google.eclipse.mechanic.internal.MechanicPreferences;
import com.google.eclipse.mechanic.internal.TasksExtensionPoint;
import com.google.eclipse.mechanic.internal.UriCaches;
import com.google.eclipse.mechanic.plugin.ui.PopupNotifier;

/**
 * Controls the plug-in life cycle, and provides access to convenient stuff
 * like a logger and plugin preferences.
 */
public class MechanicPlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.eclipse.mechanic";

  // The shared instance
  private static MechanicPlugin plugin;

  private IMechanicPreferences mechanicPreferences;

  private volatile IPreferenceRecordingService preferenceRecordingService =
      new PreferenceRecordingService();

  private PopupNotifier popupNotifier;

  public MechanicPlugin() {
    plugin = this;
  }

  @Override 
  public void start(BundleContext context) throws Exception {
    super.start(context);

    mechanicPreferences = new MechanicPreferences();

    // popup notifier must start before the mechanic service in order to
    // catch the first statuses.
    popupNotifier = new PopupNotifier(MechanicService.getInstance(), getMechanicPreferences());
    popupNotifier.initialize();

    UriCaches.initialize();

    // immediately start the mechanic service
    MechanicService.getInstance().start();
  }

  @Override 
  public void stop(BundleContext context) throws Exception {
    MechanicService.getInstance().stop();
    UriCaches.destroy();
    popupNotifier.dispose();
    TasksExtensionPoint.dispose();
    plugin = null;
    super.stop(context);
  }

  /**
   * Keep this around as some of the generated code expects it.
   * @return shared instance of this plug-in class
   */
  public static MechanicPlugin getDefault() {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path
   *
   * @param path the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  public IPreferenceRecordingService getPreferenceRecordingService() {
    if (preferenceRecordingService == null) {
      preferenceRecordingService = new PreferenceRecordingService();
    }

    return preferenceRecordingService;
  }

  public IMechanicPreferences getMechanicPreferences() {
    return mechanicPreferences;
  }
}
