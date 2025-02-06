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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;


public class Crafter extends AbstractProcessor {

    private static final String ANNO_BUILDER_CANONICAL_NAME = Builder.class.getCanonicalName();

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv
    ) {
        for (final var element : roundEnv.getElementsAnnotatedWith(Builder.class)) {

            final var elementKind = element.getKind();
            switch (elementKind) {

                case CLASS, RECORD -> generateBuilderForClass((TypeElement) element);
                case CONSTRUCTOR -> generateBuilderForConstructor((ExecutableElement) element);

                default -> printError(
                    element,
                    "@%s cannot be placed on this position %s"
                        .formatted(ANNO_BUILDER_CANONICAL_NAME, elementKind.name())
                );
            }

        }

        return false;
    }

    private void generateBuilderForClass(TypeElement clazz) {
        final var usableCtorList = clazz.getEnclosedElements()
            .stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(ctor -> (ExecutableElement) ctor)
            .filter(ctor -> ! ctor.getParameters().isEmpty())
            .toList();

        if (usableCtorList.isEmpty()) {
            printError(
                clazz,
                "Class/Record has no constructor that takes arguments to use to generate the Builder"
            );
            return;

        } else if (usableCtorList.size() > 1) {
            printError(
                clazz,
                "%s does not know which constructor to use to generate the Builder"
                    .formatted(getClass().getName())
            );
            return;
        }

        final var ctor = usableCtorList.get(0);
        generateBuilderForConstructor(ctor);
    }

    private void generateBuilderForConstructor(ExecutableElement constructor) {

    }

    private void printError(Element element, String message) {
        processingEnv.getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                message,
                element,
                Util.getAnnotationMirrorsByClass(element, Builder.class)
                    .findFirst()
                    .orElseThrow()
            );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() { return Set.of(ANNO_BUILDER_CANONICAL_NAME); }

    @Override
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }
}
