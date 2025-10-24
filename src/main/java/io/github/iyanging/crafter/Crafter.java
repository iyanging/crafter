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
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.palantir.javapoet.*;
import org.jspecify.annotations.Nullable;


public class Crafter extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Builder.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv
    ) {
        for (final var element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            final var elementKind = element.getKind();

            try {
                switch (elementKind) {

                    case CLASS, RECORD -> generateBuilderForClass((TypeElement) element);

                    case CONSTRUCTOR -> generateBuilderForCreator(
                        (ExecutableElement) element,
                        makeBuilderContainerName(element)
                    );

                    default -> throw new UsageViolation(
                        "@%s cannot be placed on this position %s"
                            .formatted(Builder.class.getCanonicalName(), elementKind.name()),
                        element
                    );
                }

            } catch (UsageViolation violation) {
                violation.reportTo(processingEnv);
            }

        }

        return false;
    }

    private void generateBuilderForClass(TypeElement clazz) throws UsageViolation {
        final var ctor = findUsableCtor(clazz);

        generateBuilderForCreator(
            ctor,
            makeBuilderContainerName(clazz)
        );
    }

    private void generateBuilderForCreator(
        ExecutableElement creator,
        String builderContainerName
    )
        throws UsageViolation {

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
        final var creatorTargetClass = Util.getReturnType(creator);
        final var creatorTargetClassName = extractClassName(creatorTargetClass);

        if (creatorTargetClassName == null) {
            throw new UsageViolation(
                "@%s cannot build %s".formatted(
                    Builder.class.getCanonicalName(),
                    Stream.of(creatorTargetClass.getClass().getSimpleName())
                        // remove postfix "Name"
                        .map(n -> n.substring(0, n.length() - 4))
                        .findFirst()
                        .orElseThrow()
                ),
                creator
            );
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
            final var stageName = makeUpperCamelCase(methodName);

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
        final var builderMethod = makeBuilderMethod(
            builderClass,
            stageInterfaceList.isEmpty()
                ? builderClass
                : stageInterfaceList.getFirst()
        );

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
                parameter != null
                    ? List.of(
                        // ParameterSpec::get() will not copy parameter annotations
                        // so we need to build ParameterSpec manually
                        ParameterSpec
                            .builder(
                                TypeName.get(parameter.asType()),
                                parameter.getSimpleName().toString()
                            )
                            .addAnnotations(
                                parameter.getAnnotationMirrors()
                                    .stream()
                                    .map(AnnotationSpec::get)
                                    .toList()
                            )
                            .addModifiers(parameter.getModifiers())
                            .build()
                    )
                    : List.of()

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
                    p -> FieldSpec
                        .builder(
                            p.type(),
                            p.name(),
                            Modifier.PROTECTED
                        )
                        // ParameterSpec::annotations() will return ElementType.PARAMETER
                        // which is not proper for class fields
                        // so we cannot do addAnnotations()
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
                                        Util.getReturnType(creator),
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

    private MethodSpec makeBuilderMethod(TypeSpec builderClass, TypeSpec firstStageType) {
        final var builderClassName = extractClassName(builderClass);

        final var firstStageTypeName = extractClassName(firstStageType);
        final var firstStageTypeParameterList = firstStageType.typeVariables();

        return MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(firstStageTypeParameterList)
            .returns(
                firstStageTypeParameterList.isEmpty()
                    ? firstStageTypeName
                    : ParameterizedTypeName.get(
                        firstStageTypeName,
                        firstStageTypeParameterList.toArray(new TypeVariableName[0])
                    )
            )
            .addStatement("return new $T()", builderClassName)
            .build();
    }

    private ExecutableElement findUsableCtor(TypeElement clazz) throws UsageViolation {
        final var usableCtorList = Util.getAllConstructors(clazz)
            .filter(ctor -> ! ctor.getParameters().isEmpty())
            .limit(2)
            .toList();

        return switch (usableCtorList.size()) {
            case 0 -> throw new UsageViolation(
                "Class/Record has no parameterized constructor to be used to generate the Builder",
                clazz
            );

            case 1 -> usableCtorList.getFirst();

            default -> throw new UsageViolation(
                "%s does not know which constructor to be used to generate the Builder"
                    .formatted(getClass().getName()),
                clazz
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
                .asElement(Util.getReturnType(creator))
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
            .asElement(Util.getReturnType(creator));

        final var targetClassTypeParameterList = targetClassElement.getTypeParameters();

        return Stream.concat(
            targetClassTypeParameterList.stream(),
            creator.getTypeParameters().stream()
        ).toList();
    }

    private AnnotationSpec makeGenerated() {
        return AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", Crafter.class.getCanonicalName())
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

    private static <T> T onlyOne(List<T> list) {
        if (list.size() != 1) {
            throw new IllegalArgumentException(
                "This list must contain exactly one element"
            );
        }

        return list.getFirst();
    }

    private static String makeUpperCamelCase(String lowerCamelCase) {
        return lowerCamelCase.substring(0, 1).toUpperCase(Locale.ENGLISH)
            + lowerCamelCase.substring(1);
    }

    public static class UsageViolation extends Exception {
        private final String errorMessage;
        private final Element element;

        public UsageViolation(String errorMessage, Element element) {
            super(errorMessage);

            this.errorMessage = errorMessage;
            this.element = element;
        }

        public void reportTo(ProcessingEnvironment processingEnv) {
            processingEnv.getMessager()
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    errorMessage,
                    element,
                    null
                );
        }
    }
}
