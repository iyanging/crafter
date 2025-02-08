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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import com.palantir.javapoet.*;


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

                case CONSTRUCTOR -> generateBuilderForCreator(
                    (ExecutableElement) element,
                    makeBuilderClassName(element)
                );

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
                "Class/Record has no parameterized constructor to be used to generate the Builder"
            );
            return;

        } else if (usableCtorList.size() > 1) {
            printError(
                clazz,
                "%s does not know which constructor to be used to generate the Builder"
                    .formatted(getClass().getName())
            );
            return;
        }

        final var ctor = usableCtorList.get(0);

        generateBuilderForCreator(
            ctor,
            makeBuilderClassName(clazz)
        );
    }

    private void generateBuilderForCreator(
        ExecutableElement creator,
        String builderClassName
    ) {
        // initialize builder class
        final var builderClass = TypeSpec.classBuilder(builderClassName)
            .addAnnotation(makeGenerated())
            .addModifiers(calcModifiers(creator))
            .addTypeVariables(extractTypeVariables(creator));

        // reversely make stages interfaces
        final var reversedStageInterfaceList = new ArrayList<TypeSpec>();

        reversedStageInterfaceList.add(makeStageFinalBuild(creator));

        // forwardly add stages interfaces

        final var builderFile = JavaFile.builder(
            processingEnv.getElementUtils()
                .getPackageOf(creator)
                .getQualifiedName()
                .toString(),
            builderClass.build()
        ).build();

        try {
            builderFile.writeTo(processingEnv.getFiler());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private TypeSpec makeStageFinalBuild(ExecutableElement creator) {
        return TypeSpec.interfaceBuilder("FinalBuild")
            .addModifiers(Modifier.PUBLIC)
            .addMethod(
                MethodSpec.methodBuilder("build")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeName.get(switch (creator.getKind()) {

                        case CONSTRUCTOR -> Objects
                            .requireNonNull(creator.getEnclosingElement())
                            .asType();
                        case METHOD -> creator.getReturnType();

                        default -> throw new IllegalStateException(
                            "creator should be CONSTRUCTOR or static METHOD"
                        );
                    }))
                    .build()
            )
            .build();
    }

    private Modifier[] calcModifiers(ExecutableElement creator) {
        return new Modifier[] {
            Optional.ofNullable(
                processingEnv.getElementUtils()
                    .getPackageOf(creator)
                    .getModifiers()
            )
                .stream()
                .flatMap(Set::stream)
                .filter(
                    modifier -> modifier == Modifier.PUBLIC
                        || modifier == Modifier.PROTECTED
                        || modifier == Modifier.PRIVATE
                        || modifier == Modifier.DEFAULT
                )
                .findFirst()
                .orElse(Modifier.DEFAULT) };
    }

    private List<TypeVariableName> extractTypeVariables(ExecutableElement creator) {
        return creator.getTypeParameters()
            .stream()
            .map(TypeVariableName::get)
            .toList();
    }

    private AnnotationSpec makeGenerated() {
        return AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", getClass().getName())
            .build();
    }

    private String makeBuilderClassName(Element element) {
        final var baseName = switch (element.getKind()) {
            case CLASS, RECORD -> element
                .getSimpleName()
                .toString();

            case CONSTRUCTOR -> Objects.requireNonNull(element.getEnclosingElement())
                .getSimpleName()
                .toString();

            default -> throw new IllegalArgumentException(
                "Unsupported element kind: " + element.getKind()
            );
        };

        return baseName + "_";
    }

    private void printError(Element element, String message) {
        processingEnv.getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                message,
                element,
                Util.getAnnotationMirrorsByClass(element, Builder.class)
                    .findFirst()
                    .orElse(null)
            );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() { return Set.of(ANNO_BUILDER_CANONICAL_NAME); }

    @Override
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }
}
