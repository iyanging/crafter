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
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.palantir.javapoet.*;
import org.jspecify.annotations.Nullable;


public class Crafter extends AbstractProcessor {
    public static final String TOOL_NAME = "Crafter";

    private static final String ANNO_BUILDER_CANONICAL_NAME = Builder.class.getCanonicalName();

    @Override
    public Set<String> getSupportedAnnotationTypes() { return Set.of(ANNO_BUILDER_CANONICAL_NAME); }

    @Override
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

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
                    makeBuilderContainerName(element)
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
            makeBuilderContainerName(clazz)
        );
    }

    private void generateBuilderForCreator(
        ExecutableElement creator,
        String builderContainerName
    ) {
        final var creatorTypeParameterList = calcTypeParameters(creator);

        // initialize builder container class
        final var builderContainer = TypeSpec.classBuilder(builderContainerName)
            .addAnnotation(makeGenerated())
            .addModifiers(calcModifiers(creator))
            .addMethod(
                // make constructor private to prevent instantiation of container
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build()
            );

        // reversely make stages interfaces
        final var stageInterfaceList = new ArrayList<TypeSpec>();

        final var creatorParameterList = creator.getParameters();
        final var creatorTargetClass = extractTargetClass(creator);
        final var creatorTargetClassName = extractClassName(creatorTargetClass);

        if (creatorTargetClassName == null) {
            printError(
                creator,
                "@%s cannot build %s".formatted(
                    ANNO_BUILDER_CANONICAL_NAME,
                    Stream.of(creatorTargetClass.getClass().getSimpleName())
                        // remove postfix "Name"
                        .map(n -> n.substring(0, n.length() - 4))
                        .findFirst()
                        .orElseThrow()
                )
            );
            return;
        }

        final var finalStage = makeStageInterface(
            "FinalStage",
            creatorTypeParameterList,
            "build",
            null,
            creatorTargetClassName
        );
        stageInterfaceList.add(finalStage);

        var nextStage = finalStage;
        for (var i = creatorParameterList.size() - 1; i >= 0; i--) {
            final var creatorParameter = creatorParameterList.get(i);

            final var methodName = creatorParameter.getSimpleName().toString();
            final var stageName = i != 0
                // add some chars to stage name
                // to avoid conflict with "FirstStage" / "FinalStage"
                ? makeUpperCamelCase(methodName) + "_"
                : "FirstStage";

            final var stage = makeStageInterface(
                stageName,
                creatorTypeParameterList,
                creatorParameter.getSimpleName().toString(),
                creatorParameter,
                nextStage
            );

            stageInterfaceList.add(stage);
            nextStage = stage;
        }

        Collections.reverse(stageInterfaceList); // reverse the reversed list

        // add stages interfaces
        builderContainer.addTypes(stageInterfaceList);

        // make builder class
        final var builderClass = makeBuilderClass(
            stageInterfaceList,
            finalStage,
            creator
        );

        builderContainer.addType(builderClass);

        // make `builder()` method
        final var builderMethod = makeBuilderMethod(builderClass);

        builderContainer.addMethod(builderMethod);

        final var builderFile = JavaFile.builder(
            processingEnv.getElementUtils()
                .getPackageOf(creator)
                .getQualifiedName()
                .toString(),
            builderContainer.build()
        ).build();

        try {
            builderFile.writeTo(processingEnv.getFiler());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private TypeSpec makeStageInterface(
        String interfaceName,
        List<TypeParameterElement> typeParameter,
        String methodName,
        @Nullable VariableElement parameter,
        TypeSpec nextStage
    ) {
        return makeStageInterface(
            interfaceName,
            typeParameter,
            methodName,
            parameter,
            extractClassName(nextStage)
        );
    }

    private TypeSpec makeStageInterface(
        String interfaceName,
        List<TypeParameterElement> typeParameterList,
        String methodName,
        @Nullable VariableElement parameter,
        ClassName nextStage
    ) {
        final var typeParameterNameList = typeParameterList.stream()
            .map(TypeVariableName::get)
            .toList();

        final var stageInterface = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariables(typeParameterNameList);

        final var nextStageTypeName = typeParameterList.isEmpty()
            ? nextStage
            : ParameterizedTypeName
                .get(
                    nextStage,
                    // all stages share the same ordered type parameters
                    typeParameterNameList.toArray(new TypeVariableName[0])
                );

        final var stageMethod = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameters(
                Optional.ofNullable(parameter)
                    .map(ParameterSpec::get)
                    .stream()
                    .toList()
            )
            .returns(nextStageTypeName);

        return stageInterface
            .addMethod(stageMethod.build())
            .build();
    }

    private TypeSpec makeBuilderClass(
        List<TypeSpec> stageInterfaceList,
        TypeSpec finalStage,
        ExecutableElement creator
    ) {
        final var creatorTypeParameterList = calcTypeParameters(creator).stream()
            .map(TypeVariableName::get)
            .toList();

        final var builderClass = TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(creatorTypeParameterList)
            .addSuperinterfaces(
                stageInterfaceList.stream()
                    .map(stageInterface -> {
                        final var stageClassName = extractClassName(stageInterface);

                        if (creatorTypeParameterList.isEmpty()) {
                            return stageClassName;

                        } else {
                            return ParameterizedTypeName.get(
                                stageClassName,
                                creatorTypeParameterList.toArray(new TypeVariableName[0])
                            );
                        }

                    })
                    .toList()
            );

        builderClass.addFields(
            stageInterfaceList.stream()
                .filter(s -> {
                    @SuppressWarnings("ReferenceEquality")
                    final var isFinalStage = s != finalStage;
                    return isFinalStage;
                })
                .map(s -> onlyOne(s.methodSpecs()))
                .map(m -> onlyOne(m.parameters()))
                .map(
                    p -> FieldSpec.builder(
                        p.type(),
                        p.name(),
                        Modifier.PROTECTED
                    )
                        .addAnnotations(p.annotations())
                        .build()
                )
                .toList()
        );

        builderClass.addMethods(
            stageInterfaceList.stream()
                .filter(s -> {
                    @SuppressWarnings("ReferenceEquality")
                    final var isFinalStage = s != finalStage;
                    return isFinalStage;
                })
                .map(s -> onlyOne(s.methodSpecs()))
                .map(
                    m -> MethodSpec.methodBuilder(m.name())
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(m.returnType())
                        .addParameters(m.parameters())
                        .addStatement("this.$1L = $1L", onlyOne(m.parameters()).name())
                        .addStatement("return this")
                        .build()
                )
                .toList()
        );

        final var creatorInvocationLiteral = String.join(
            ", ",
            creator.getParameters()
                .stream()
                .map(p -> p.getSimpleName().toString())
                .toList()
        );

        builderClass.addMethods(
            Stream.of(onlyOne(finalStage.methodSpecs()))
                .map(
                    buildMethod -> MethodSpec.methodBuilder(buildMethod.name())
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(buildMethod.returnType())
                        .addStatement(
                            switch (creator.getKind()) {
                                case CONSTRUCTOR -> CodeBlock.builder()
                                    .add(
                                        "return new $T($L)",
                                        extractTargetClass(creator),
                                        creatorInvocationLiteral
                                    )
                                    .build();

                                case METHOD -> CodeBlock.builder()
                                    .add(
                                        "return $T.$L($L)",
                                        // using ClassName gets rid of any type parameters
                                        // the class might have
                                        ClassName.get((TypeElement) creator.getEnclosingElement()),
                                        creator.getSimpleName(),
                                        creatorInvocationLiteral
                                    )
                                    .build();

                                default -> throw new IllegalStateException();
                            }
                        )
                        .build()
                )
                .toList()
        );

        return builderClass.build();
    }

    private MethodSpec makeBuilderMethod(TypeSpec builderClass) {
        final var builderClassName = extractClassName(builderClass);
        final var builderTypeParameterList = builderClass.typeVariables();

        return MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(builderTypeParameterList)
            .returns(
                ParameterizedTypeName.get(
                    builderClassName,
                    builderTypeParameterList.toArray(new TypeVariableName[0])
                )
            )
            .addStatement("return new $T()", builderClassName)
            .build();
    }

    private TypeMirror extractTargetClass(ExecutableElement creator) {
        return switch (creator.getKind()) {
            case CONSTRUCTOR -> Objects
                .requireNonNull(creator.getEnclosingElement())
                .asType();

            case METHOD -> creator.getReturnType();

            default -> throw new IllegalStateException(
                "creator should be CONSTRUCTOR or static METHOD"
            );
        };
    }

    private ClassName extractClassName(TypeSpec type) {
        return ClassName.get(
            "", // directly access
            type.name()
        );
    }

    private @Nullable ClassName extractClassName(TypeMirror typeMirror) {
        final var typeName = TypeName.get(typeMirror);

        if (typeName instanceof ClassName cn) {
            return cn;

        } else if (typeName instanceof ParameterizedTypeName ptn) {
            // remove type parameters
            return ptn.rawType();

        } else {
            return null;
        }

    }

    private Modifier[] calcModifiers(ExecutableElement creator) {
        return new Modifier[] {
            processingEnv.getTypeUtils()
                .asElement(extractTargetClass(creator))
                .getModifiers()
                .stream()
                .filter(
                    modifier -> modifier == Modifier.PUBLIC
                        || modifier == Modifier.PROTECTED
                        || modifier == Modifier.PRIVATE
                )
                .findFirst()
                .orElse(Modifier.DEFAULT) };
    }

    private List<TypeParameterElement> calcTypeParameters(ExecutableElement creator) {
        final var targetClassElement = (TypeElement) processingEnv.getTypeUtils()
            .asElement(extractTargetClass(creator));

        final var targetClassTypeParameterList = targetClassElement.getTypeParameters();

        return Stream.concat(
            targetClassTypeParameterList.stream(),
            creator.getTypeParameters().stream()
        ).toList();
    }

    private AnnotationSpec makeGenerated() {
        return AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", TOOL_NAME)
            .build();
    }

    private String makeBuilderContainerName(Element element) {
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

        return baseName + "Builder";
    }

    private void printError(Element element, String message) {
        processingEnv.getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                message,
                element,
                null
            );
    }

    private static <T> T onlyOne(List<T> list) {
        if (list.size() != 1) {
            throw new IllegalArgumentException(
                "This list must contain exactly one element"
            );
        }

        return list.get(0);
    }

    private static String makeUpperCamelCase(String lowerCamelCase) {
        return lowerCamelCase.substring(0, 1).toUpperCase(Locale.ENGLISH)
            + lowerCamelCase.substring(1);
    }
}
