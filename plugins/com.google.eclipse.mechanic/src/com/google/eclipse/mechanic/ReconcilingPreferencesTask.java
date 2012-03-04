/*******************************************************************************
 * Copyright (C) 2007, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.google.eclipse.mechanic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Models an Eclipse preferences export file as a series of individual
 * preference reconcilers. If any of the reconcilers need fixing, they
 * get fixed.
 * 
 * @author smckay@google.com (Steve McKay)
 */
public abstract class ReconcilingPreferencesTask
    extends PreferenceReconcilerTask {

  private final IResourceTaskReference taskRef;

  public ReconcilingPreferencesTask(IResourceTaskReference taskRef) {
    this.taskRef = taskRef;
    initReconcilers();
  }

  /**
   * Returns an id for the specified class and file.
   */
  @Override
  public String getId() {
    return String.format("%s@%s", getClass().getName(), taskRef.getPath());
  }

  /**
   * Adds a new reconciler for each preference line found in the file
   * @throws RuntimeException if any files are not found.
   */
  private void initReconcilers() {

    InputStream is = null;
    Properties props = new Properties();
    
    try {
      is = taskRef.newInputStream();
      props.load(is);

      for (Entry<Object,Object> e : props.entrySet()) {

        String key = (String)e.getKey();
        String value = (String)e.getValue();

        // if the line starts with a slash, we treat it as a preference
        if (key.length() > 0 && key.charAt(0) == '/') {
          addReconciler(createReconciler(key+"="+value));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Couldn't read " + taskRef.getPath(), e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          throw new RuntimeException("Error occured while trying to " +
              "cleanup from another error. Life sucks.", e);
        }
      }
    }
  }
}
