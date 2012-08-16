/*******************************************************************************
 * Copyright (C) 2007, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.google.eclipse.mechanic;

import static java.lang.String.format;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.eclipse.mechanic.plugin.core.MechanicLog;

import java.io.File;
import java.util.List;

/**
 * Provides a base class for Tasks that reconcile values in the underlying
 * preferences system.
 * 
 * <p>
 * 
 * Example:
 * 
 * <pre>
 * public class FancyTask extends PreferenceReconcilerTask {
 * 
 *   public FancyTask() {
 *     addReconciler(createReconciler(&quot;/instance/com.google.eclipse.mechanic/ham=fancy&quot;));
 *   }
 * 
 *   public String getDescription() {
 *     return &quot;Reconciles HAM&quot;;
 *   }
 * 
 *   public String getTitle() {
 *     return getDescription();
 *   }
 * }
 * </pre>
 * 
 * 
 * TODO(smckay): cooperate with core mechanic to wake up on changes to
 * preferences we're configured to analyze.
 * 
 * @author smckay@google.com (Steve McKay)
 */
public abstract class PreferenceReconcilerTask extends CompositeTask {

  private final IEclipsePreferences prefsRoot;
  private final List<Reconciler> reconcilers = Lists.newArrayList();

  /**
   * Constructs a new instance with the supplied instance of
   * {@link IEclipsePreferences}.
   */
  public PreferenceReconcilerTask(final IEclipsePreferences root) {
    this.prefsRoot = root;
  }

  /**
   * Constructs a new instance, providing a default implementation of
   * {@link IEclipsePreferences} as provided by
   * {@code Platform.getPreferencesService().getRootNode()}.
   */
  public PreferenceReconcilerTask() {
    this(Platform.getPreferencesService().getRootNode());
  }

  /**
   * Adds a new Reconciler to the list of reconcilers to be applied by this
   * task.
   */
  public void addReconciler(final Reconciler r) {
    reconcilers.add(r);
  }

  /**
   * Fails if any of Reconcilers need reconciling.
   */
  public boolean evaluate() {
    for (final Reconciler r : reconcilers) {
      if (!r.isReconciled()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Reconciles all un-reconciled tasks.
   */
  public void run() {
    for (final Reconciler r : reconcilers) {

      // We only bother to reconcile un-reconciled prefs.
      if (!r.isReconciled()) {
        r.reconcile();
      }
    }
  }

  /**
   * Creates a Reconciler given an opaque preference string as exported by
   * Eclipse in an EPF file.
   */
  public Reconciler createReconciler(final String key, final String value) {
    return createReconciler(parsePreference(key, value));
  }

  private Preference parsePreference(final String id, final String value) {
    Preconditions.checkNotNull(id, "'id' cannot be null.");
    Preconditions
        .checkArgument(id.length() > 0, "'id' cannot be empty string.");
    Preconditions.checkNotNull(value, "'value' cannot be null.");
    Preconditions.checkArgument(value.length() > 0,
        "'value' cannot be empty string.");

    final int sli = id.lastIndexOf("/");

    Preconditions.checkArgument(sli != -1, format(
        "'pref' must contain a slash in the identifier portion "
            + "of the preference. Bad val: '%s'", id));
    final String path = id.substring(0, sli);

    Preconditions.checkArgument(id.length() > sli + 1, format(
        "'pref' must contain a name after slash in the "
            + "identifier portion of the preference. Bad val: '%s'", id));
    final String key = id.substring(sli + 1);

    return new ImmutablePreference(path, key, value);
  }

  /**
   * Returns a new Reconciler instance that will reconcile prefs that do not
   * match the value exactly.
   */
  public Reconciler createReconciler(final String path, final String key,
      final String value) {
    return createReconciler(new ImmutablePreference(path, key, value));
  }

  /**
   * Returns a new Reconciler instance that will reconcile prefs that do not
   * match the value exactly.
   */
  public Reconciler createReconciler(final Preference pref) {
    return createReconciler(pref, new EqualsMatcher(pref), new SimpleResolver(
        pref));
  }

  /**
   * Returns a new Reconciler instance that will reconcile prefs that do not
   * match the value exactly.
   */
  public Reconciler createReconciler(final Preference pref,
      final Matcher matcher, final Resolver resolver) {
    return new CompositeReconciler(prefsRoot, pref, matcher, resolver);
  }

  /**
   * Groups together the Path, Key, and Value parts of a preference.
   */
  public static interface Preference {

    /**
     * Returns the path value.
     */
    public String getPath();

    /**
     * Returns the key value indicating the preference field identified by this
     * Preference instance.
     */
    public String getKey();

    /**
     * Returns the actual preference value.
     */
    public String getValue();

  }

  /**
   * Reconciles declarations of preferences with the actual values stored in the
   * prefs system.
   */
  public static interface Reconciler {

    /**
     * Returns true if the preference contains the "correct" value.
     */
    public boolean isReconciled();

    /**
     * Updates the preference so that isReconciled will return true.
     */
    public void reconcile();
  }

  /**
   * Provides an interface for matching preference values.
   */
  public static interface Matcher {
    public boolean matches(String subject);
  }

  /**
   * Provides an interface for resolving (i.e. correcting) mis-matched values.
   */
  public interface Resolver {

    /**
     * Resolves a value.
     * 
     * @param subject
     *          the existing value. Could be null.
     */
    public String resolve(String subject);
  }

  /**
   * Implements Preference interface with out any fancy mojo.
   */
  public static class ImmutablePreference implements Preference {

    private final String path;
    private final String key;
    private final String value;

    public ImmutablePreference(final String path, final String key,
        final String value) {
      this.path = path;
      this.key = key;
      this.value = value;
    }

    public String getPath() {
      return path;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Reconciles preferences using a Preference object and a Matcher object.
   */
  public static class CompositeReconciler implements Reconciler {

    private final IEclipsePreferences prefsRoot;
    private final Preference pref;
    private final Matcher matcher;
    private final Resolver resolver;
    private final IEclipsePreferences node;

    public CompositeReconciler(final IEclipsePreferences root,
        final Preference pref, final Matcher matcher, final Resolver resolver) {

      this.prefsRoot = root;
      this.pref = pref;
      this.matcher = matcher;
      this.resolver = resolver;
      this.node = getPrefNode(pref.getPath());
    }

    public boolean isReconciled() {
      final String value = node.get(pref.getKey(), null);
      final boolean result = matcher.matches(value);
      if (!result) {
        MechanicLog.getDefault().logInfo(
            "Value for key '%s' is not good. Expected %s but was %s",
            pref.getKey(), pref.getValue(), value);
      }
      return result;
    }

    public void reconcile() {
      node.put(pref.getKey(), resolver.resolve(node.get(pref.getKey(), null)));
      try {
        node.flush();
      } catch (final BackingStoreException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Returns a reference to the preference node specified by the supplied
     * path.
     */
    private IEclipsePreferences getPrefNode(final String path) {
      return (IEclipsePreferences) prefsRoot.node(path);
    }
  }

  /**
   * Matches values when they subject values return true from "equals".
   */
  public static class EqualsMatcher implements Matcher {

    private final Preference pref;

    public EqualsMatcher(final Preference pref) {
      this.pref = pref;
    }

    public boolean matches(final String subject) {
      return pref.getValue().equals(subject);
    }
  }

  /**
   * Matches when the subject contains the supplied value.
   */
  public static class ContainsMatcher implements Matcher {

    private final String value;

    public ContainsMatcher(final String value) {
      this.value = value;
    }

    public boolean matches(final String subject) {
      return subject != null && subject.contains(this.value);
    }
  }

  /**
   * Resolves to the value of the preference supplied in the constructor.
   */
  public static class SimpleResolver implements Resolver {

    private final Preference pref;

    public SimpleResolver(final Preference pref) {
      this.pref = pref;
    }

    public String resolve(final String subject) {
      return pref.getValue();
    }
  }

  /**
   * Appends the value from the preference supplied in the constructor with the
   * existing preference value using an arbitrary delimiter.
   */
  public static class ListAppendResolver implements Resolver {

    private final String delim;
    private final Preference pref;

    public ListAppendResolver(final String delim, final Preference pref) {
      this.delim = delim;
      this.pref = pref;
    }

    /**
     * Appends the value from the preference to the supplied value.
     */
    public String resolve(final String subject) {
      final String prepend = subject == null ? "" : subject + delim;
      return prepend + pref.getValue();
    }
  }

  /**
   * Appends the value from the preference supplied in the constructor with the
   * existing preference value using {@code File#separator}.
   */
  public static class PathAppendResolver extends ListAppendResolver {

    public PathAppendResolver(final Preference pref) {
      super(File.pathSeparator, pref);
    }
  }

  /**
   * Replaces matching strings in the preference with the supplied value.
   */
  public static class StringReplaceResolver implements Resolver {

    private final String regex;
    private final String replace;

    public StringReplaceResolver(final String regex, final String replace) {
      this.regex = regex;
      this.replace = replace;
    }

    public String resolve(final String subject) {
      if (subject != null) {
        return subject.replaceAll(regex, replace);
      }
      return null;
    }
  }

}
