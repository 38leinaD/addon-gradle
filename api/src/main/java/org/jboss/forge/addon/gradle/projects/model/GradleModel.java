/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.gradle.projects.model;

import java.util.List;

import org.jboss.forge.addon.gradle.projects.exceptions.UnremovableElementException;

/**
 * Contains information about Gradle build script.
 * 
 * @author Adam Wyłuda
 */
public interface GradleModel
{
   String getProjectName();

   String getVersion();

   List<GradleTask> getTasks();

   List<GradleDependency> getDependencies();

   List<GradleDependency> getManagedDependencies();

   List<GradleProfile> getProfiles();

   List<GradlePlugin> getPlugins();

   List<GradleRepository> getRepositories();
   
   List<GradleSourceSet> getSourceSets();

   /**
    * Returns true if exists task with given name.
    */
   boolean hasTask(String name);

   /**
    * Returns true if there exists dependency for which {@link GradleDependencyBuilder#equalsToDep(GradleDependency)}
    * return true.
    */
   boolean hasDependency(GradleDependencyBuilder builder);

   /**
    * Returns true if there exists managed dependency for which
    * {@link GradleDependencyBuilder#equalsToDep(GradleDependency)} return true.
    */
   boolean hasManagedDependency(GradleDependencyBuilder builder);

   /**
    * Returns true if there is profile with given name. 
    */
   boolean hasProfile(String name);

   /**
    * Returns true if project has applied plugin with given name. 
    */
   boolean hasPlugin(String clazz);

   /**
    * Returns true if project defined repository with given name. 
    */
   boolean hasRepository(String url);

   void setProjectName(String name) throws UnremovableElementException;

   void setVersion(String version) throws UnremovableElementException;

   void createTask(GradleTaskBuilder builder);

   void createDependency(GradleDependencyBuilder builder);

   void createManagedDependency(GradleDependencyBuilder builder);

   /**
    * @see GradleProfile
    */
   void createProfile(String name);

   /**
    * Applies plugin with given name or class.
    */
   void applyPlugin(String name);

   void createGradleRepository(GradleRepositoryBuilder builder);

   void removeDependency(GradleDependencyBuilder builder) throws UnremovableElementException;

   void removeManagedDependency(GradleDependencyBuilder builder) throws UnremovableElementException;

   void removeProfile(String name);

   void removeAppliedPlugin(String name) throws UnremovableElementException;

   void removeGradleRepository(GradleRepositoryBuilder builder) throws UnremovableElementException;
}
