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
}
