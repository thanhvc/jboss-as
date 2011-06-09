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
package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that stores method level and class level information about a single annotation type.
 *
 * Class level annotations apply to all methods declared on that class;
 *
 * @author Stuart Douglas
 */
public final class ComponentAnnotationMetadata<T, A extends Annotation> {

    public abstract static class Transformer<T> {
        public abstract T fromJandex(AnnotationInstance instance);
    }

    private final Class<T> dataType;
    private final Class<A> annotationType;
    private final Transformer<T> transformer;
    private final Map<String, T> classAnnotations = new HashMap<String, T>();
    private final Map<MethodIdentifier, T> methodAnnotations = new HashMap<MethodIdentifier, T>();

    public ComponentAnnotationMetadata(final Class<T> dataType, final Class<A> annotationType, final Transformer transformer) {
        this.dataType = dataType;
        this.annotationType = annotationType;
        this.transformer = transformer;
    }

    public void classLevelAnnotation(String className, AnnotationInstance annotationInstance) {
        classAnnotations.put(className, transformer.fromJandex(annotationInstance));
    }

    public void classLevelAnnotation(String className, T data) {
        classAnnotations.put(className, data);
    }

    public void methodLevelAnnotation(final MethodIdentifier methodIdentifier, final AnnotationInstance annotationInstance) {
        methodAnnotations.put(methodIdentifier, transformer.fromJandex(annotationInstance));
    }

    public void methodLevelAnnotation(final MethodIdentifier methodIdentifier, final T data) {
        methodAnnotations.put(methodIdentifier, data);
    }

     /**
     * Removes annotation information from a class if the method has been overridden with a method that has no annotation,
     * as the jandex index cannot be used to get information about methods with no annotations
     */
    public void checkMethodOverrides(final Class<?> componentClass, final DeploymentReflectionIndex index) {

        ClassReflectionIndex<?> classIndex = index.getClassIndex(componentClass);
        Iterator<Map.Entry<MethodIdentifier,T>> iterator = methodAnnotations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MethodIdentifier, T> entry = iterator.next();
            if (annotationOverridden(classIndex, index, entry.getKey(), annotationType)) {
                iterator.remove();
            }
        }
    }

    private boolean annotationOverridden(final ClassReflectionIndex<?> classIndex, final DeploymentReflectionIndex index, final MethodIdentifier method, final Class<? extends Annotation> annotation) {
        ClassReflectionIndex<?> cindex = classIndex;
        while (cindex != null && cindex.getIndexedClass() != Object.class) {
            Method m = cindex.getMethod(method);
            if (m != null) {
                return !m.isAnnotationPresent(annotation);
            }
            cindex = index.getClassIndex(cindex.getIndexedClass().getSuperclass());
        }
        return false;
    }

    public T get(final Method method) {
        final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);
        return get(methodIdentifier, method.getDeclaringClass().getName());
    }

    private T get(final MethodIdentifier methodIdentifier, final String declaringClassName) {
        T value = methodAnnotations.get(methodIdentifier);
        if(value == null) {
            value = classAnnotations.get(declaringClassName);
        }
        return value;
    }

    public static <T,A extends Annotation> void merge(ComponentAnnotationMetadata<T, A> mergeInto,ComponentAnnotationMetadata<T, A> original, ComponentAnnotationMetadata<T, A> override) {
        mergeInto.classAnnotations.putAll(original.classAnnotations);
        mergeInto.classAnnotations.putAll(override.classAnnotations);
        mergeInto.methodAnnotations.putAll(original.methodAnnotations);
        mergeInto.methodAnnotations.putAll(override.methodAnnotations);
    }
}
