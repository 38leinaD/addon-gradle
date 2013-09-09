/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.gradle.projects.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.jarjar.com.google.common.collect.Lists;
import org.gradle.jarjar.com.google.common.collect.Maps;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;

/**
 * @author Adam Wyłuda
 */
public class GradleModelLoadUtil
{
   private GradleModelLoadUtil()
   {
   }
   
   /**
    * Parses XML source into Gradle model, setting given file resource as Gradle resource.
    */
   public static GradleEffectiveModel load(String source)
   {
      Node root = XMLParser.parse(source);
      List<GradleProfile> profiles = profilesFromNode(root);
      GradleEffectiveModel projectModel = modelFromNode(root.getSingle("project"), profiles);
      return projectModel;
   }

   private static List<GradleProfile> profilesFromNode(Node rootNode)
   {
      List<GradleProfile> profiles = new ArrayList<GradleProfile>();
      for (Node profileNode : rootNode.get("profile"))
      {
         String name = profileNode.getSingle("name").getText().trim();
         GradleEffectiveModel model = modelFromNode(profileNode.getSingle("project"), new ArrayList<GradleProfile>());
         profiles.add(new GradleProfileImpl(name, model));
      }
      return profiles;
   }

   private static GradleEffectiveModel modelFromNode(Node projectNode, List<GradleProfile> profiles)
   {
      String group = groupFromNode(projectNode);
      String name = nameFromNode(projectNode);
      String version = versionFromNode(projectNode);
      String packaging = packagingFromNode(projectNode);
      String archivePath = archivePathFromNode(projectNode);
      String projectPath = projectPathFromNode(projectNode);
      String rootProjectDirectory = rootProjectDirectoryFromNode(projectNode);
      List<GradleTask> tasks = tasksFromNode(projectNode);
      List<GradleDependency> deps = depsFromNode(projectNode);
      List<GradleDependency> managedDeps = managedDepsFromNode(projectNode);
      List<GradlePlugin> plugins = pluginsFromNode(projectNode);
      List<GradleRepository> repositories = reposFromNode(projectNode);
      List<GradleSourceSet> sourceSets = sourceSetsFromNode(projectNode);
      Map<String, String> properties = propertiesFromNode(projectNode);

      return new GradleModelImpl(group, name, version,
               projectPath, rootProjectDirectory,
               packaging, archivePath, tasks, deps,
               managedDeps, profiles, plugins, repositories, sourceSets, properties);
   }

   private static String groupFromNode(Node projectNode)
   {
      return projectNode.getSingle("group").getText().trim();
   }

   private static String nameFromNode(Node projectNode)
   {
      return projectNode.getSingle("name").getText().trim();
   }

   private static String versionFromNode(Node projectNode)
   {
      return projectNode.getSingle("version").getText().trim();
   }

   private static String projectPathFromNode(Node projectNode)
   {
      return projectNode.getSingle("projectPath").getText().trim();
   }

   private static String rootProjectDirectoryFromNode(Node projectNode)
   {
      return projectNode.getSingle("rootProjectDirectory").getText().trim();
   }

   private static String packagingFromNode(Node projectNode)
   {
      return projectNode.getSingle("packaging").getText().trim();
   }

   private static String archivePathFromNode(Node projectNode)
   {
      return projectNode.getSingle("archivePath").getText().trim();
   }

   private static List<GradleTask> tasksFromNode(Node projectNode)
   {
      List<GradleTask> tasks = new ArrayList<GradleTask>();
      Map<GradleTask, List<String>> taskDepsMap = new HashMap<GradleTask, List<String>>();
      Map<String, GradleTask> taskByNameMap = new HashMap<String, GradleTask>();

      for (Node taskNode : projectNode.getSingle("tasks").get("task"))
      {
         String name = taskNode.getSingle("name").getText().trim();
         List<String> taskDeps = new ArrayList<String>();
         for (Node dependsOnNode : taskNode.getSingle("dependsOn").get("task"))
         {
            String text = dependsOnNode.getText().trim();
            taskDeps.add(text);
         }
         GradleTask task = new GradleTaskImpl(name, Lists.<GradleTask> newArrayList());
         tasks.add(task);
         taskDepsMap.put(task, taskDeps);
         taskByNameMap.put(task.getName(), task);
      }

      // We couldn't get full set of dependencies so we will complete it now
      for (GradleTask task : tasks)
      {
         List<String> deps = taskDepsMap.get(task);
         for (String depName : deps)
         {
            task.getDependsOn().add(taskByNameMap.get(depName));
         }
      }

      return tasks;
   }

   private static List<GradleDependency> depsFromNode(Node projectNode)
   {
      // Gradle string -> Best dependency 
      // (one which has the biggest priority, determined by overrides relationship)
      Map<String, GradleDependency> depByString = new HashMap<String, GradleDependency>();
      
      for (Node depNode : projectNode.getSingle("dependencies").get("dependency"))
      {
         GradleDependency gradleDep = depFromNode(depNode);
         String gradleString = gradleDep.toGradleString();
         if (!depByString.containsKey(gradleString))
         {
            depByString.put(gradleString, gradleDep);
         }
         else
         {
            GradleDependency olderDep = depByString.get(gradleString);
            if (gradleDep.getConfiguration().overrides(olderDep.getConfiguration()))
            {
               depByString.put(gradleString, gradleDep);
            }
         }
      }


      List<GradleDependency> deps = new ArrayList<GradleDependency>();
      deps.addAll(depByString.values());
      return deps;
   }

   private static List<GradleDependency> managedDepsFromNode(Node projectNode)
   {
      List<GradleDependency> deps = new ArrayList<GradleDependency>();
      for (Node depNode : projectNode.getSingle("managedDependencies").get("dependency"))
      {
         deps.add(depFromNode(depNode));
      }
      return deps;
   }

   private static GradleDependency depFromNode(Node depNode)
   {
      String group = depNode.getSingle("group").getText().trim();
      String name = depNode.getSingle("name").getText().trim();
      String version = depNode.getSingle("version").getText().trim();
      String config = depNode.getSingle("configuration").getText().trim();
      return new GradleDependencyImpl(group, name, version, GradleDependencyConfiguration.fromName(config), config);
   }

   private static List<GradlePlugin> pluginsFromNode(Node projectNode)
   {
      List<GradlePlugin> plugins = new ArrayList<GradlePlugin>();
      for (Node pluginNode : projectNode.getSingle("plugins").get("plugin"))
      {
         plugins.add(pluginFromNode(pluginNode));
      }
      return plugins;
   }

   private static GradlePlugin pluginFromNode(Node pluginNode)
   {
      String clazz = pluginNode.getSingle("class").getText().trim();
      return new GradlePluginImpl(clazz, GradlePluginType.typeByClazz(clazz));
   }

   private static List<GradleRepository> reposFromNode(Node projectNode)
   {
      List<GradleRepository> repos = new ArrayList<GradleRepository>();
      for (Node repoNode : projectNode.getSingle("repositories").get("repository"))
      {
         String name = repoNode.getSingle("name").getText().trim();
         String url = repoNode.getSingle("url").getText().trim();
         repos.add(new GradleRepositoryImpl(name, url));
      }
      return repos;
   }

   private static List<GradleSourceSet> sourceSetsFromNode(Node projectNode)
   {
      List<GradleSourceSet> sourceSets = new ArrayList<GradleSourceSet>();
      for (Node sourceSetNode : projectNode.getSingle("sourceSets").get("sourceSet"))
      {
         sourceSets.add(sourceSetFromNode(sourceSetNode));
      }
      return sourceSets;
   }

   private static GradleSourceSet sourceSetFromNode(Node sourceSetNode)
   {
      String name = sourceSetNode.getSingle("name").getText().trim();
      List<GradleSourceDirectory> javaSourceDirs = new ArrayList<GradleSourceDirectory>();
      for (Node directoryNode : sourceSetNode.getSingle("java").get("directory"))
      {
         javaSourceDirs.add(sourceDirectoryFromNode(directoryNode));
      }
      List<GradleSourceDirectory> resourceSourceDirs = new ArrayList<GradleSourceDirectory>();
      for (Node directoryNode : sourceSetNode.getSingle("resources").get("directory"))
      {
         resourceSourceDirs.add(sourceDirectoryFromNode(directoryNode));
      }
      return new GradleSourceSetImpl(name, javaSourceDirs, resourceSourceDirs);
   }

   private static GradleSourceDirectory sourceDirectoryFromNode(Node directoryNode)
   {
      String path = directoryNode.getText().trim();
      return new GradleSourceDirectoryImpl(path);
   }

   private static Map<String, String> propertiesFromNode(Node projectNode)
   {
      Map<String, String> properties = Maps.newHashMap();
      for (Node propertyNode : projectNode.getSingle("properties").get("property"))
      {
         String key = propertyNode.getSingle("key").getText().trim();
         String value = propertyNode.getSingle("value").getText().trim();
         properties.put(key, value);
      }
      return properties;
   }
}