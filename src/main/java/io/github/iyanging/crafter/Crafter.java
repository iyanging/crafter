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

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;


public class Crafter extends AbstractProcessor {

    private static final String ANNO_BUILDER_CANONICAL_NAME = Builder.class.getCanonicalName();

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv
    ) {
        for (final var annotatedElement : roundEnv.getElementsAnnotatedWith(Builder.class)) {

            final var elementKind = annotatedElement.getKind();
            switch (elementKind) {

                case CONSTRUCTOR -> generateBuilderForConstructor();
                case RECORD -> generateBuilderForRecord();

                default -> processingEnv.getMessager()
                    .printMessage(
                        Diagnostic.Kind.ERROR,
                        "@%s cannot be placed on this position %s"
                            .formatted(ANNO_BUILDER_CANONICAL_NAME, elementKind.name()),
                        annotatedElement,
                        Util.getAnnotationMirrorsByClass(annotatedElement, Builder.class)
                            .findFirst()
                            .orElseThrow()
                    );
            }

        }

        return false;
    }

    private void generateBuilderForConstructor() {

    }

    private void generateBuilderForRecord() {

    }

    @Override
    public Set<String> getSupportedAnnotationTypes() { return Set.of(ANNO_BUILDER_CANONICAL_NAME); }

    @Override
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }
}
