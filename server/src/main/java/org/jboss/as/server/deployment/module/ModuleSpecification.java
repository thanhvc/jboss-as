/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.SimpleAttachable;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ResourceLoaderSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Information used to build a module.
 *
 * @author Stuart Douglas
 */
public class ModuleSpecification extends SimpleAttachable {

    /**
     * System dependencies are dependencies that are added automatically by the container.
     */
    private final List<ModuleDependency> systemDependencies = new ArrayList<ModuleDependency>();

    private final Set<ModuleIdentifier> systemDependenciesSet = new HashSet<ModuleIdentifier>();
    /**
     * Local dependencies are dependencies on other parts of the deployment, such as class-path entry
     */
    private final List<ModuleDependency> localDependencies = new ArrayList<ModuleDependency>();
    /**
     * User dependencies are dependencies that the user has specifically added, either via jboss-deployment-structure.xml
     * or via the manifest.
     * <p/>
     * User dependencies are not affected by exclusions.
     */
    private final List<ModuleDependency> userDependencies = new ArrayList<ModuleDependency>();

    private final List<ResourceLoaderSpec> resourceLoaders = new ArrayList<ResourceLoaderSpec>();

    /**
     * Modules that cannot be added as dependencies to the deployment, as the user has excluded them
     */
    private final Set<ModuleIdentifier> exclusions = new HashSet<ModuleIdentifier>();

    /**
     * Flag that is set to true if modules of non private sub deployments should be able to see each other
     */
    private boolean subDeploymentModulesIsolated;

    /**
     * Flag that indicates that this module should never be visible to other sub deployments
     */
    private boolean privateModule;

    /**
     * If set to true this indicates that a dependency on this module requires a dependency on all it's transitive
     * dependencies.
     */
    private boolean requiresTransitiveDependencies;

    public void addSystemDependency(ModuleDependency dependency) {
        if (!exclusions.contains(dependency.getIdentifier()) && !systemDependenciesSet.contains(dependency.getIdentifier())) {
            this.systemDependencies.add(dependency);
            this.systemDependenciesSet.add(dependency.getIdentifier());
        }
    }

    public void addSystemDependencies(Collection<ModuleDependency> dependencies) {
        for (ModuleDependency dependency : dependencies) {
            addSystemDependency(dependency);
        }
    }

    public void addUserDependency(ModuleDependency dependency) {
        this.userDependencies.add(dependency);
    }

    public void addUserDependencies(Collection<ModuleDependency> dependencies) {
        userDependencies.addAll(dependencies);
    }

    public void addLocalDependency(ModuleDependency dependency) {
        if (!exclusions.contains(dependency.getIdentifier())) {
            this.localDependencies.add(dependency);
        }
    }

    public void addLocalDependencies(Collection<ModuleDependency> dependencies) {
        for (ModuleDependency dependency : dependencies) {
            addLocalDependency(dependency);
        }
    }

    public List<ModuleDependency> getSystemDependencies() {
        return Collections.unmodifiableList(systemDependencies);
    }

    public void addExclusion(ModuleIdentifier exclusion) {
        exclusions.add(exclusion);
        Iterator<ModuleDependency> it = systemDependencies.iterator();
        while (it.hasNext()) {
            ModuleDependency dep = it.next();
            if (dep.getIdentifier().equals(exclusion)) {
                it.remove();
            }
        }
        it = localDependencies.iterator();
        while (it.hasNext()) {
            ModuleDependency dep = it.next();
            if (dep.getIdentifier().equals(exclusion)) {
                it.remove();
            }
        }
    }

    public void addExclusions(Iterable<ModuleIdentifier> exclusions) {
        for (ModuleIdentifier exclusion : exclusions) {
            addExclusion(exclusion);
        }
    }


    public List<ModuleDependency> getLocalDependencies() {
        return Collections.unmodifiableList(localDependencies);
    }

    public List<ModuleDependency> getUserDependencies() {
        return Collections.unmodifiableList(userDependencies);
    }

    public void addResourceLoader(ResourceLoaderSpec resourceLoader) {
        this.resourceLoaders.add(resourceLoader);
    }

    public List<ResourceLoaderSpec> getResourceLoaders() {
        return Collections.unmodifiableList(resourceLoaders);
    }

    public boolean isSubDeploymentModulesIsolated() {
        return subDeploymentModulesIsolated;
    }

    public void setSubDeploymentModulesIsolated(boolean subDeploymentModulesIsolated) {
        this.subDeploymentModulesIsolated = subDeploymentModulesIsolated;
    }

    public boolean isPrivateModule() {
        return privateModule;
    }

    public void setPrivateModule(boolean privateModule) {
        this.privateModule = privateModule;
    }

    public boolean isRequiresTransitiveDependencies() {
        return requiresTransitiveDependencies;
    }

    public void setRequiresTransitiveDependencies(final boolean requiresTransitiveDependencies) {
        this.requiresTransitiveDependencies = requiresTransitiveDependencies;
    }

}
