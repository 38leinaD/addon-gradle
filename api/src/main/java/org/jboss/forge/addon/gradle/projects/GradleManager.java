/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.gradle.projects;

import org.jboss.forge.furnace.services.Exported;

/**
 * Manages Gradle build system.
 * 
 * @author Adam Wyłuda
 */
@Exported
public interface GradleManager
{
   /**
    * @return True if build was successful, false otherwise.
    */
   boolean runGradleBuild(String directory, String task, String profile, String... arguments);
}
