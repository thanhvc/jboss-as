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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.filter.PathFilters;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Processor that filters out API and other common classes that users accidentally include in their applications
 * <p/>
 * Dependencies on AS modules are added in their place
 *
 * @author Stuart Douglas
 */
public class DependencyFilterProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment.module");

    private final Map<String, List<ModuleIdentifier>> filters;

    private static final String FILE = "META-INF/filtering.properties";

    private static final String DO_NOT_FILTER = "Do-Not-Filter";

    public DependencyFilterProcessor() {
        //load paths that should be filtered from META-INF/filtering.properties
        final Map<String, List<ModuleIdentifier>> filters = new HashMap<String, List<ModuleIdentifier>>();
        InputStream stream = null;
        try {
            stream = getClass().getClassLoader().getResourceAsStream(FILE);
            Properties properties = new Properties();
            properties.load(stream);
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                final String path = prop.getKey().toString().replace('.', '/').trim();
                final String modules = (String) prop.getValue();
                final List<ModuleIdentifier> identifiers = new ArrayList<ModuleIdentifier>(1);
                if (modules != null) {
                    for (String id : modules.split(",")) {
                        final ModuleIdentifier identifier = ModuleIdentifier.fromString(id);
                        identifiers.add(identifier);
                    }
                }
                filters.put(path, identifiers);
            }
        } catch (IOException e) {
            log.error("Could not load " + FILE + " deployment filtering of redundant artifacts will not be available");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }

        this.filters = filters;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);

        final Set<ModuleIdentifier> unfilteredIdentifiers = new HashSet<ModuleIdentifier>();

        for (final ResourceRoot resourceRoot : resourceRoots) {
            final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
            if (manifest != null) {
                Object doNotFilter = manifest.getMainAttributes().getValue(DO_NOT_FILTER);
                if (doNotFilter != null) {
                    String[] parts = ((String) doNotFilter).split("\\s");
                    for (String part : parts) {
                        unfilteredIdentifiers.add(ModuleIdentifier.fromString(part));
                    }
                }
            }
        }

        final Map<ModuleIdentifier, Set<ResourceRoot>> packages = new HashMap<ModuleIdentifier, Set<ResourceRoot>>();

        for (final ResourceRoot resourceRoot : resourceRoots) {
            final VirtualFile root = resourceRoot.getRoot();
            for (Map.Entry<String, List<ModuleIdentifier>> entry : this.filters.entrySet()) {
                final String path = entry.getKey();
                final VirtualFile file = root.getChild(path);
                if (file.exists() && file.isDirectory()) {
                    boolean doNotFilter = false;
                    for (ModuleIdentifier identifier : entry.getValue()) {
                        if(unfilteredIdentifiers.contains(identifier)) {
                            doNotFilter = true;
                            break;
                        }
                    }
                    if(doNotFilter) {
                        continue;
                    }

                    for (ModuleIdentifier identifier : entry.getValue()) {
                        Set<ResourceRoot> pkgs = packages.get(identifier);
                        if(pkgs == null) {
                            packages.put(identifier, pkgs = new HashSet<ResourceRoot>());
                        }
                        pkgs.add(resourceRoot);
                    }
                    resourceRoot.getExportFilters().add(new FilterSpecification(PathFilters.is(path), false));
                }
            }
        }
        for(Map.Entry<ModuleIdentifier, Set<ResourceRoot>> entry : packages.entrySet()) {


            log.warnf("Classes from %s were found in %s, these will not be used, and the application server's " +
                            "version will be used instead. If you wish to use the bundled version of these classes, add the following entry to your " +
                            "MANIFEST.MF file 'Do-Not-Filter: %s", entry.getKey(), entry.getValue(), entry.getKey());
            moduleSpecification.addDependency(new ModuleDependency(Module.getBootModuleLoader(), entry.getKey(), false, false, true));
        }
    }

    public void undeploy(final DeploymentUnit context) {

    }
}
