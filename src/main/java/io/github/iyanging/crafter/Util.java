/*
 * Copyright (c) 2024 iyanging
 *
 * crafter is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 */

package io.github.iyanging.crafter;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.stream.Stream;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;


/* package */ final class Util {
    private Util() {
    }

    public static Stream<? extends AnnotationMirror> getAnnotationMirrorsByClass(
        Element element,
        Class<? extends Annotation> annotationClass
    ) {
        return element.getAnnotationMirrors()
            .stream()
            .filter(
                am -> am.getAnnotationType()
                    .toString()
                    .equals(annotationClass.getCanonicalName())
            );
    }

    /**
     * Get all constructors of a given class/record.
     *
     * @param clazz the class/record to get constructors from
     */
    public static Stream<ExecutableElement> getAllConstructors(TypeElement clazz) {
        return clazz.getEnclosedElements()
            .stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(ctor -> (ExecutableElement) ctor);
    }

    /**
     * Get the return type of the given constructor or static method.
     * <p>
     * If the given executable element is a constructor, it returns the type of the enclosing
     * class/record.
     * <p>
     * If the given executable element is a static method, it returns the return type of the method.
     *
     * @param executable the constructor or static method to get the return type of
     * @throws IllegalArgumentException if the given executable element is not a constructor or
     *                                  static method
     */
    static TypeMirror getReturnType(ExecutableElement executable) {
        return switch (executable.getKind()) {
            case CONSTRUCTOR -> Objects
                .requireNonNull(executable.getEnclosingElement())
                .asType();

            case METHOD -> executable.getReturnType();

            default -> throw new IllegalArgumentException(
                "executable should be CONSTRUCTOR or static METHOD"
            );
        };
    }
}
