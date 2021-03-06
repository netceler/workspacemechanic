/*******************************************************************************
 * Copyright (C) 2007, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.google.eclipse.mechanic.plugin.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.eclipse.mechanic.Task;
import com.google.eclipse.mechanic.internal.BlockedTaskIdsParser;
import com.google.eclipse.mechanic.internal.FileTaskProvider;
import com.google.eclipse.mechanic.internal.ResourceTaskProviderParser;
import com.google.eclipse.mechanic.internal.UriCaches;
import com.google.eclipse.mechanic.internal.UriTaskProvider;
import com.google.eclipse.mechanic.internal.VariableManagerStringParser;

/**
 * Class used to initialize and access various plugin related preference values.
 *
 * <p> API Note: I tried using the newer API (as the Preferences API was
 * deprecated, but I had such a bizarre issue? As evidenced by the test
 * in MechanicPreferencesTest.testWithFunnyKey.
 *
 * @deprecated use MechanicPreferences.
 */
@SuppressWarnings("deprecation") // Uses the old-style API.
public class OldMechanicPreferences {
 
  /**
   * Returns the plugin preferences. Just a convenience method.
   */
  private static Preferences getPreferences() {
    return MechanicPlugin.getDefault().getPluginPreferences();
  }

  private static final MechanicLog log = MechanicLog.getDefault();

  public static void addListener(IPropertyChangeListener listener) {
    Preferences prefs = getPreferences();
    prefs.addPropertyChangeListener(listener);
  }

  public static void removeListener(IPropertyChangeListener listener) {
    Preferences prefs = getPreferences();
    prefs.removePropertyChangeListener(listener);
  }

  // CHM used for thread-safe map.
  private static final ConcurrentMap<String, String> sourcesFailingInitialization = Maps.newConcurrentMap();

  /**
   * Return a list of task sources where tasks may be found.
   *
   * @return list of task sources where tasks may be found.
   */
  public static List<ResourceTaskProvider> getTaskProviders() {
    String paths = getString(IMechanicPreferences.DIRS_PREF);

    ResourceTaskProviderParser parser = new ResourceTaskProviderParser(VariableManagerStringParser.INSTANCE);
    List<ResourceTaskProvider> providers = Lists.newArrayList();
    for (String source : parser.parse(paths)) {
      ResourceTaskProvider provider = toProvider(source);
      if (provider != null) {
        IStatus initializationStatus = provider.initialize();
        if (initializationStatus.isOK()) {
          providers.add(provider);
          sourcesFailingInitialization.remove(source);
        } else {
          if (!sourcesFailingInitialization.containsKey(source)) {
            sourcesFailingInitialization.put(source, source);
            log.log(initializationStatus);
          }
        }
      }
    }
    return providers;
  }

  private static ResourceTaskProvider toProvider(String source) {
    URI uri;
    try {
      uri = new URI(source);
      if (uri.getScheme() != null) {
        return new UriTaskProvider(uri, UriCaches.getStateSensitiveCache(),
//             UriCaches.getLifetimeCache());
            UriCaches.getStateSensitiveCache());
      } else {
        return new FileTaskProvider(new File(source));
      }
    } catch (URISyntaxException e) {
      // This is a fall-through for files like C:\path\to\file
      return new FileTaskProvider(new File(source));
    }
  }

  /**
   * Returns the number of seconds the mechanic should sleep between passes.
   */
  public static int getThreadSleepSeconds() {
    int seconds = getInt(IMechanicPreferences.SLEEPAGE_PREF);
    return cleanSleepSeconds(seconds);
  }

  /**
   * Ensures the supplied sleep duration falls in an acceptable range.
   */
  public static int cleanSleepSeconds(int seconds) {
    return Math.max(seconds, IMechanicPreferences.MINIMUM_SLEEP_SECONDS);
  }

  /**
   * Returns a mutable set of blocked Task ids.
   */
  public static Set<String> getBlockedTaskIds() {
    BlockedTaskIdsParser parser = new BlockedTaskIdsParser();

    String val = getString(IMechanicPreferences.BLOCKED_PREF);
    Set<String> set = Sets.newHashSet();
    Collections.addAll(set, parser.parse(val));
    return set;
  }

  /**
   * Saves the supplied Task id set in the preferences system.
   */
  public static void setBlockedTaskIds(Set<String> ids) {
    BlockedTaskIdsParser parser = new BlockedTaskIdsParser();

    String unparse = parser.unparse(ids.toArray(new String[0]));

    Preferences prefs = getPreferences();
    prefs.setValue(IMechanicPreferences.BLOCKED_PREF, unparse);
  }

  /**
   * Adds the supplied Task's id to the set of blocked Tasks.
   */
  public static void blockItem(Task item) {
    Set<String> ids = getBlockedTaskIds();
    ids.add(item.getId());
    setBlockedTaskIds(ids);
  }

  /**
   * Returns the mechanic help url.
   */
  public static String getHelpUrl() {
    return getString(IMechanicPreferences.HELP_URL_PREF);
  }

  public static boolean contains(String key) {
    Preferences prefs = getPreferences();
    return prefs.contains(key);
  }

  /**
   * returns the value of given key as a int.
   */
  public static int getInt(String key) {
    Preferences prefs = getPreferences();
    return prefs.getInt(key);
  }

  /**
   * returns the value of given key as a long.
   */
  public static long getLong(String key) {
    Preferences prefs = getPreferences();
    return prefs.getLong(key);
  }

  /**
   * Set the long value of a preference on the MechanicPreferences scope.
   */
  public static void setLong(String key, long value) {
    Preferences prefs = getPreferences();
    prefs.setValue(key, value);
  }

  /**
   * returns the value of given key as a string.
   */
  public static String getString(String key) {
    Preferences prefs = getPreferences();
    return prefs.getString(key);
  }

  /**
   * Set the string value of a preference on the MechanicPreferences scope.
   */
  public static void setString(String key, String value) {
    Preferences prefs = getPreferences();
    prefs.setValue(key, value);
  }

  /**
   * Return {@code true} if the notification popup should be shown when
   * tasks fail.
   */
  public static boolean isShowPopup() {
    Preferences prefs = getPreferences();
    return prefs.getBoolean(IMechanicPreferences.SHOW_POPUP_PREF);
  }

  /**
   * Disable the preference that shows the notification popup.
   */
  public static void doNotShowPopup() {
    Preferences prefs = getPreferences();
    prefs.setValue(IMechanicPreferences.SHOW_POPUP_PREF, false);
  }

  /**
   * Enable preference that shows the notification popup.
   *
   * <p>For tests only.
   */
  public static void showPopup() {
    Preferences prefs = getPreferences();
    prefs.setValue(IMechanicPreferences.SHOW_POPUP_PREF, true);
  }

//  /**
//   * Return {@code true} if web caching is enabled.
//   */
//  public static boolean isWebCacheEnabled() {
//    return preferencesService.getBoolean(
//        MechanicPlugin.PLUGIN_ID, CACHE_URI_CONTENT_PREF, true, null);
//  }

//  /**
//   * Return the age of web cache entries, in hours. Meaningless when {@link
//   * #isWebCacheEnabled()} is {@code false}.
//   */
//  public static int getWebCacheEntryAgeHours() {
//    return preferencesService.getInt(
//        MechanicPlugin.PLUGIN_ID, CACHE_URI_AGE_HOURS_PREF, 0, null);
//  }

  /**
   * Get the validation status of a preferences file.
   */
  public static IStatus validatePreferencesFile(IPath path) {
    return Preferences.validatePreferenceVersions(path);
  }
}
