/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.gradle.projects.facets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.jarjar.com.google.common.collect.Lists;
import org.gradle.jarjar.com.google.common.collect.Sets;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyQuery;
import org.jboss.forge.addon.dependencies.DependencyRepository;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.dependencies.util.NonSnapshotDependencyFilter;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.facets.constraints.FacetConstraints;
import org.jboss.forge.addon.gradle.parser.GradleSourceUtil;
import org.jboss.forge.addon.gradle.projects.GradleFacet;
import org.jboss.forge.addon.gradle.projects.exceptions.UnremovableElementException;
import org.jboss.forge.addon.gradle.projects.model.GradleDependency;
import org.jboss.forge.addon.gradle.projects.model.GradleDependencyBuilder;
import org.jboss.forge.addon.gradle.projects.model.GradleDependencyConfiguration;
import org.jboss.forge.addon.gradle.projects.model.GradleEffectiveModel;
import org.jboss.forge.addon.gradle.projects.model.GradleRepository;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

/**
 * @author Adam Wyłuda
 */
@FacetConstraints({
   @FacetConstraint(GradleFacet.class)
})
public class GradleDependencyFacet extends AbstractFacet<Project> implements DependencyFacet
{
   @Inject
   private DependencyResolver dependencyResolver;

   @Override
   public boolean install()
   {
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      return getFaceted().hasFacet(GradleFacet.class);
   }

   @Override
   public void addDirectDependency(Dependency dep)
   {
      GradleEffectiveModel model = getGradleFacet().getModel();
      model.addDependency(forgeDepToGradleDepBuilder(dep));
      getGradleFacet().setModel(model);
   }

   @Override
   public void addManagedDependency(Dependency dep)
   {
      if (!hasEffectiveDependency(dep))
      {
         addDirectManagedDependency(dep);
      }
   }

   @Override
   public void addDirectManagedDependency(Dependency dep)
   {
      GradleEffectiveModel model = getGradleFacet().getModel();
      model.addManagedDependency(forgeDepToGradleDepBuilder(dep));
      getGradleFacet().setModel(model);
   }

   @Override
   public void addRepository(String name, String url)
   {
      GradleEffectiveModel model = getGradleFacet().getModel();
      model.addRepository(url);
      getGradleFacet().setModel(model);
   }

   @Override
   public List<Dependency> getDependencies()
   {
      String script = getGradleFacet().getModel().getScript();
      List<GradleDependencyBuilder> gradleDeps = Lists.newArrayList();

      gradleDeps.addAll(GradleSourceUtil.getDependencies(script));
      gradleDeps.addAll(GradleSourceUtil.getDirectDependencies(script));

      return gradleDepBuildersToForgeDeps(gradleDeps);
   }

   @Override
   public List<Dependency> getDependenciesInScopes(String... scopes)
   {
      return filterDependenciesFromScopes(getDependencies(), scopes);
   }

   @Override
   public Dependency getDirectDependency(Dependency dependency)
   {
      return findDependency(getDependencies(), dependency);
   }

   @Override
   public List<Dependency> getEffectiveDependencies()
   {
      return resolveDependencies(getEvaluatedDependencies());
   }

   @Override
   public List<Dependency> getEffectiveDependenciesInScopes(String... scopes)
   {
      return filterDependenciesFromScopes(getEffectiveDependencies(), scopes);
   }

   @Override
   public Dependency getEffectiveDependency(Dependency dependency)
   {
      return findDependency(getEffectiveDependencies(), dependency);
   }

   @Override
   public Dependency getEffectiveManagedDependency(Dependency dependency)
   {
      return findDependency(getEffectiveManagedDependencies(), dependency);
   }

   @Override
   public List<Dependency> getManagedDependencies()
   {
      String script = getGradleFacet().getModel().getScript();
      return gradleDepBuildersToForgeDeps(GradleSourceUtil.getManagedDependencies(script));
   }

   @Override
   public Dependency getDirectManagedDependency(Dependency dependency)
   {
      return findDependency(getManagedDependencies(), dependency);
   }

   @Override
   public List<DependencyRepository> getRepositories()
   {
      List<DependencyRepository> repos = Lists.newArrayList();

      for (GradleRepository gradleRepo : getGradleFacet().getModel().getRepositories())
      {
         repos.add(new DependencyRepository(gradleRepo.getName(), gradleRepo.getUrl()));
      }

      return repos;
   }

   @Override
   public boolean hasDirectDependency(Dependency dependency)
   {
      return listContainsDep(getDependencies(), dependency);
   }

   @Override
   public boolean hasEffectiveDependency(Dependency dependency)
   {
      return listContainsDep(getEffectiveDependencies(), dependency);
   }

   @Override
   public boolean hasEffectiveManagedDependency(Dependency managedDependency)
   {
      return listContainsDep(getEffectiveManagedDependencies(), managedDependency);
   }

   @Override
   public boolean hasDirectManagedDependency(Dependency managedDependency)
   {
      return listContainsDep(getManagedDependencies(), managedDependency);
   }

   @Override
   public boolean hasRepository(String url)
   {
      for (DependencyRepository repo : getRepositories())
      {
         if (repo.getUrl().equals(url))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   public void removeDependency(Dependency dependency)
   {
      try
      {
         GradleEffectiveModel model = getGradleFacet().getModel();
         model.removeDependency(forgeDepToGradleDepBuilder(dependency));
         getGradleFacet().setModel(model);
      }
      catch (UnremovableElementException e)
      {
         // TODO Handle Gradle exceptions
         e.printStackTrace();
      }
   }

   @Override
   public void removeManagedDependency(Dependency managedDependency)
   {
      try
      {
         GradleEffectiveModel model = getGradleFacet().getModel();
         model.removeManagedDependency(forgeDepToGradleDepBuilder(managedDependency));
         getGradleFacet().setModel(model);
      }
      catch (UnremovableElementException e)
      {
         // TODO Handle Gradle exceptions
         e.printStackTrace();
      }
   }

   @Override
   public DependencyRepository removeRepository(String url)
   {
      DependencyRepository repo = null;
      try
      {
         GradleEffectiveModel model = getGradleFacet().getModel();
         repo = findRepositoryByUrl(getRepositories(), url);
         model.removeRepository(url);
         getGradleFacet().setModel(model);
      }
      catch (UnremovableElementException e)
      {
         // TODO Handle Gradle exceptions
         e.printStackTrace();
      }
      return repo;
   }

   @Override
   public List<Coordinate> resolveAvailableVersions(Dependency dep)
   {
      DependencyQueryBuilder query = DependencyQueryBuilder.create(dep.getCoordinate()).setRepositories(
               getRepositories());
      if (dep.getCoordinate().getVersion() != null && !dep.getCoordinate().getVersion().contains("SNAPSHOT"))
      {
         query.setFilter(new NonSnapshotDependencyFilter());
      }
      List<Coordinate> versions = dependencyResolver.resolveVersions(query);
      return versions;
   }

   @Override
   public List<Coordinate> resolveAvailableVersions(String gavs)
   {
      return resolveAvailableVersions(DependencyBuilder.create(gavs));
   }

   @Override
   public List<Coordinate> resolveAvailableVersions(DependencyQuery query)
   {
      List<Coordinate> versions = dependencyResolver.resolveVersions(query);
      return versions;
   }

   @Override
   public Dependency resolveProperties(Dependency dependency)
   {
      GradleEffectiveModel model = getGradleFacet().getModel();
      Map<String, String> props = model.getProperties();
      DependencyBuilder builder = DependencyBuilder.create(dependency);

      builder.setGroupId(resolveProperties(props, dependency.getCoordinate().getGroupId()));
      builder.setArtifactId(resolveProperties(props, dependency.getCoordinate().getArtifactId()));
      builder.setVersion(resolveProperties(props, dependency.getCoordinate().getVersion()));
      builder.setClassifier(resolveProperties(props, dependency.getCoordinate().getClassifier()));
      builder.setPackaging(resolveProperties(props, dependency.getCoordinate().getPackaging()));
      builder.setScopeType(resolveProperties(props, dependency.getScopeType()));

      return builder;
   }
   
   /**
    * Returns a list of dependencies and their transitive dependencies.
    */
   public List<Dependency> resolveDependencies(List<Dependency> deps)
   {
      Map<String, Dependency> depByString = new HashMap<String, Dependency>();
      
      for (Dependency dep : deps)
      {
         depByString.put(dep.toString(), dep);
         
         Set<Dependency> depDeps = dependencyResolver.resolveDependencies(
                  DependencyQueryBuilder.create(dep.getCoordinate()).setRepositories(getRepositories()));
         for (Dependency depDep : depDeps)
         {
            String depDepString = depDep.toString();
            if (!depByString.containsKey(depDepString))
            {
               depByString.put(depDepString, depDep);
            }
         }
      }
      
      return new ArrayList<Dependency>(depByString.values());
   }

   public List<Dependency> getEffectiveManagedDependencies()
   {
      return resolveDependencies(getEvaluatedManagedDependencies());
   }

   public List<Dependency> getEvaluatedDependencies()
   {
      return gradleDepsToForgeDeps(getGradleFacet().getModel().getEffectiveDependencies());
   }

   public List<Dependency> getEvaluatedManagedDependencies()
   {
      return gradleDepsToForgeDeps(getGradleFacet().getModel().getEffectiveManagedDependencies());
   }

   private List<Dependency> gradleDepBuildersToForgeDeps(List<GradleDependencyBuilder> gradleDeps)
   {
      List<Dependency> deps = Lists.newArrayList();
      for (GradleDependencyBuilder builder : gradleDeps)
      {
         deps.add(gradleDepBuilderToForgeDep(builder));
      }
      return deps;
   }

   private List<Dependency> gradleDepsToForgeDeps(List<GradleDependency> gradleDeps)
   {
      List<Dependency> deps = Lists.newArrayList();
      for (GradleDependency gradleDep : gradleDeps)
      {
         deps.add(gradleDepToForgeDep(gradleDep));
      }
      return deps;
   }

   private Dependency gradleDepToForgeDep(GradleDependency gradleDep)
   {
      Dependency forgeDep;
      forgeDep = DependencyBuilder.create()
               .setScopeType(gradleDep.getConfiguration().toMavenScope())
               .setGroupId(gradleDep.getGroup())
               .setArtifactId(gradleDep.getName())
               .setVersion(gradleDep.getVersion());
      return forgeDep;
   }

   private Dependency gradleDepBuilderToForgeDep(GradleDependencyBuilder gradleDep)
   {
      Dependency forgeDep;
      forgeDep = DependencyBuilder.create()
               .setScopeType(GradleDependencyConfiguration
                        .fromName(gradleDep.getConfiguration()).toMavenScope())
               .setGroupId(gradleDep.getGroup())
               .setArtifactId(gradleDep.getName())
               .setVersion(gradleDep.getVersion())
               .setPackaging(gradleDep.getPackaging());
      return forgeDep;
   }

   private GradleDependencyBuilder forgeDepToGradleDepBuilder(Dependency forgeDep)
   {
      return GradleDependencyBuilder.create()
               .setConfiguration(GradleDependencyConfiguration
                        .fromMavenScope(forgeDep.getScopeType()).getName())
               .setGroup(forgeDep.getCoordinate().getGroupId())
               .setName(forgeDep.getCoordinate().getArtifactId())
               .setVersion(forgeDep.getCoordinate().getVersion())
               .setPackaging(forgeDep.getCoordinate().getPackaging());
   }

   private List<Dependency> filterDependenciesFromScopes(List<Dependency> deps, String... scopes)
   {
      List<Dependency> foundDeps = Lists.newArrayList();
      Set<String> scopeSet = Sets.newHashSet(scopes);

      for (Dependency dep : deps)
      {
         if (scopeSet.contains(dep.getScopeType()))
         {
            foundDeps.add(dep);
         }
      }

      return foundDeps;
   }

   private boolean listContainsDep(List<Dependency> deps, Dependency dep)
   {
      for (Dependency listDep : deps)
      {
         if (depEquals(listDep, dep))
         {
            return true;
         }
      }
      return false;
   }

   private Dependency findDependency(List<Dependency> deps, Dependency dep)
   {
      for (Dependency listDep : deps)
      {
         if (depEquals(listDep, dep))
         {
            return listDep;
         }
      }
      return null;
   }

   private boolean depEquals(Dependency dep1, Dependency dep2)
   {
      return dep1.getCoordinate().getGroupId().equals(dep2.getCoordinate().getGroupId()) &&
               dep1.getCoordinate().getArtifactId().equals(dep2.getCoordinate().getArtifactId()) &&
               dep1.getCoordinate().getVersion().equals(dep2.getCoordinate().getVersion());
   }

   private DependencyRepository findRepositoryByUrl(List<DependencyRepository> list, String url)
   {
      for (DependencyRepository repo : list)
      {
         if (repo.getUrl().equals(url))
         {
            return repo;
         }
      }
      return null;
   }
   
   private String resolveProperties(Map<String, String> properties, String value)
   {
      if (value != null)
      {
         for (Map.Entry<String, String> entry : properties.entrySet())
         {
            value = value.replaceAll("\\$ext\\." + entry.getKey(), entry.getValue());
            value = value.replaceAll("\\$\\{ext\\." + entry.getKey() + "\\}", entry.getValue());
         }
      }
      return value;
   }


   private GradleFacet getGradleFacet()
   {
      return getFaceted().getFacet(GradleFacet.class);
   }
}
