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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.palantir.javapoet.*;
import org.jspecify.annotations.NonNull;
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
            final var builderConfig = Objects.requireNonNull(element.getAnnotation(Builder.class));

            try {
                switch (elementKind) {

                    case CLASS, RECORD -> generateBuilderForCreator(
                        builderConfig,
                        findUsableCreator((TypeElement) element)
                    );

                    case CONSTRUCTOR -> generateBuilderForCreator(
                        builderConfig,
                        findUsableCreator((ExecutableElement) element)
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

    private void generateBuilderForCreator(
        Builder builderConfig,
        ExecutableElement creator
    )
        throws UsageViolation {

        final var containerClass = makeContainerClass(builderConfig, creator);

        final var stageList = makeAllStages(creator);

        containerClass.addTypes(stageList);

        final var builderClass = makeBuilderClass(stageList, creator);

        containerClass.addType(builderClass);

        final var builderMethod = makeBuilderMethod(
            builderClass,
            stageList.getFirst()
        );

        containerClass.addMethod(builderMethod);

        final var targetPackage = calcTargetPackage(builderConfig, creator);

        dumpContainerClass(containerClass, targetPackage);
    }

    private TypeSpec.Builder makeContainerClass(
        Builder builderConfig,
        ExecutableElement creator
    ) {
        return TypeSpec.classBuilder(makeBuilderContainerName(creator))
            .addAnnotation(makeGenerated())
            .addModifiers(calcContainerModifier(builderConfig, creator))
            .addMethod(
                // make constructor private to prevent instantiation of container
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build()
            );
    }

    private List<TypeSpec> makeAllStages(ExecutableElement creator)
        throws UsageViolation {
        // Reversely make stages interfaces:
        // Stages are referenced in parameter declaration order,
        // but must be declared in reverse to maintain proper scoping.

        final var stageList = new ArrayList<TypeSpec>();

        final var creatorParamList = creator.getParameters();
        final var creatorTargetClass = getReturnType(creator);
        final var creatorTargetClassName = extractClassName(creatorTargetClass);
        final var creatorTypeParamList = calcTypeParameters(creator);

        if (creatorTargetClassName == null) {
            throw new UsageViolation(
                "creator cannot return %s".formatted(
                    creatorTargetClass.getClass().getSimpleName()
                ),
                creator
            );
        }

        final var buildStage = makeStageInterface(
            "Build$",
            creatorTypeParamList,
            "build",
            null,
            creatorTargetClassName
        );

        stageList.add(buildStage);
        var nextStage = buildStage;

        for (final var creatorParam : creatorParamList.reversed()) {
            final var methodName = creatorParam.getSimpleName().toString();
            final var stageName = makeUpperCamelCase(methodName);
            final var stage = makeStageInterface(
                stageName,
                creatorTypeParamList,
                methodName,
                creatorParam,
                nextStage
            );

            stageList.add(stage);
            nextStage = stage;
        }

        // Java supports forward references,
        // so we will reverse the "reversed" stage declarations
        // to match the parameter declaration order
        return stageList.reversed();
    }

    private TypeSpec makeStageInterface(
        String stageName,
        List<TypeParameterElement> typeParamDeclList,
        String methodName,
        @Nullable VariableElement param,
        TypeSpec nextStage
    ) {
        return makeStageInterface(
            stageName,
            typeParamDeclList,
            methodName,
            param,
            extractClassName(nextStage)
        );
    }

    private static TypeSpec makeStageInterface(
        String stageName,
        List<TypeParameterElement> typeParamDeclList,
        String methodName,
        @Nullable VariableElement param,
        ClassName nextStage
    ) {
        final var stageInterface = TypeSpec.interfaceBuilder(stageName)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariables(
                typeParamDeclList.stream()
                    .map(Gen::asTypeVariableName)
                    .toList()
            );

        Gen.toT

        final var nextStageTypeName = typeParamDeclList.isEmpty()
            ? nextStage
            : ParameterizedTypeName
                .get(
                    nextStage,
                    typeParamDeclList.stream()
                        .map(Gen::toTypeName)
                        .toArray(TypeName[]::new)
                );

        final var stageMethod = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameters(
                param != null
                    ? List.of(Gen.toParamSpec(param))
                    : List.of()
            )
            .returns(nextStageTypeName);

        return stageInterface
            .addMethod(stageMethod.build())
            .build();
    }

    private TypeSpec makeBuilderClass(
        List<TypeSpec> stageList,
        ExecutableElement creator
    ) {
        final var typeParamDeclList = creator.getTypeParameters()
            .stream()
            .map(Gen::asTypeVariableName)
            .toList();

        final var builderClass = TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(typeParamDeclList)
            .addSuperinterfaces(
                stageList.stream()
                    .map(stage -> {
                        final var stageClassName = extractClassName(stage);

                        return typeParamDeclList.isEmpty()
                            ? stageClassName
                            : ParameterizedTypeName.get(
                                stageClassName,
                                creator.getTypeParameters()
                                    .stream()
                                    .map(Gen::toTypeName)
                                    .toArray(TypeName[]::new)
                            );

                    })
                    .toList()
            );

        builderClass.addFields(
            stageList.stream()
                .filter(s -> {
                    @SuppressWarnings("ReferenceEquality")
                    final var isBuildStage = s != stageList.getLast();
                    return isBuildStage;
                })
                .map(s -> onlyOne(s.methodSpecs()))
                .map(m -> onlyOne(m.parameters()))
                .map(
                    p -> FieldSpec
                        .builder(
                            p.type(), // TYPE_USE annotations are already included in the type
                            p.name(),
                            Modifier.PROTECTED
                        )
                        // Note: PARAMETER annotations from the method parameter are not
                        // copied to the field, which is correct behavior
                        .build()
                )
                .toList()
        );

        builderClass.addMethods(
            stageList.stream()
                .filter(s -> {
                    @SuppressWarnings("ReferenceEquality")
                    final var isBuildStage = s != stageList.getLast();
                    return isBuildStage;
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
            Stream.of(onlyOne(stageList.getLast().methodSpecs()))
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
                                        getReturnType(creator),
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

    private MethodSpec makeBuilderMethod(
        TypeSpec builderClass,
        TypeSpec firstStageType
    ) {
        final var builderClassName = extractClassName(builderClass);
        final var firstStageTypeName = extractClassName(firstStageType);

        return MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(firstStageType.typeVariables())
            .returns(
                firstStageType.typeVariables().isEmpty()
                    ? firstStageTypeName
                    : ParameterizedTypeName.get(
                        firstStageTypeName,
                        firstStageType.typeVariables().toArray(new TypeVariableName[0])
                    )
            )
            .addStatement(
                firstStageType.typeVariables().isEmpty()
                    ? "return new $T()"
                    : "return new $T<>()",
                builderClassName
            )
            .build();
    }

    private ExecutableElement findUsableCreator(TypeElement clazz) throws UsageViolation {
        final var parameterizedCtorList = clazz.getEnclosedElements()
            .stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(ctor -> (ExecutableElement) ctor)
            .filter(ctor -> ! ctor.getParameters().isEmpty())
            .limit(2)
            .toList();

        return switch (parameterizedCtorList.size()) {
            case 0 -> throw new UsageViolation(
                "Class/Record has no parameterized constructor to be used to generate the Builder",
                clazz
            );

            case 1 -> findUsableCreator(parameterizedCtorList.getFirst());

            default -> throw new UsageViolation(
                "%s does not know which constructor to be used to generate the Builder"
                    .formatted(getClass().getName()),
                clazz
            );
        };
    }

    private ExecutableElement findUsableCreator(ExecutableElement creator) throws UsageViolation {
        if (
            creator.getModifiers().isEmpty() // package-private access
                || creator.getModifiers().contains(Modifier.PUBLIC)
        ) {
            return creator;

        } else {
            throw new UsageViolation(
                "Only public and package-private constructors are supported for Builder generation",
                creator
            );
        }

    }

    private static ClassName extractClassName(TypeSpec type) {
        return ClassName.get(
            "", // directly access
            type.name()
        );
    }

    private static @Nullable ClassName extractClassName(TypeMirror typeMirror) {
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

    private List<TypeParameterElement> calcTypeParameters(ExecutableElement creator) {
        final var targetClassElement = (TypeElement) processingEnv.getTypeUtils()
            .asElement(getReturnType(creator));

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

    private void dumpContainerClass(
        TypeSpec.Builder containerClass,
        String targetPackage
    ) {
        final var builderFile = JavaFile.builder(
            targetPackage,
            containerClass.build()
        ).build();

        try {
            builderFile.writeTo(processingEnv.getFiler());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private String calcTargetPackage(
        Builder builderConfig,
        ExecutableElement creator
    ) {
        final TypeMirror packageClass;
        try {
            final var ignored = builderConfig.packageClass();

            throw new ImpossibleError("Annotation Class<?> field should be inaccessible");

        } catch (MirroredTypeException e) {
            packageClass = e.getTypeMirror();
        }

        if (! packageClass.toString().equals(Void.class.getCanonicalName())) {
            return processingEnv.getElementUtils()
                .getPackageOf(processingEnv.getTypeUtils().asElement(packageClass))
                .getQualifiedName()
                .toString();

        } else if (! builderConfig.packageName().isEmpty()) {
            return builderConfig.packageName();

        } else {
            return processingEnv.getElementUtils()
                .getPackageOf(creator)
                .getQualifiedName()
                .toString();
        }

    }

    private Modifier calcContainerModifier(Builder builderConfig, ExecutableElement creator) {
        return switch (builderConfig.access()) {
            case SAME_AS_CREATOR -> processingEnv.getTypeUtils()
                .asElement(getReturnType(creator))
                .getModifiers()
                .stream()
                .filter(
                    m -> m == Modifier.PUBLIC
                        || m == Modifier.PROTECTED
                        || m == Modifier.PRIVATE
                )
                .findFirst()
                .orElse(Modifier.DEFAULT);

            case PUBLIC -> Modifier.PUBLIC;
            default -> Modifier.DEFAULT;
        };
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

    private static TypeMirror getReturnType(ExecutableElement creator) {
        return switch (creator.getKind()) {
            case CONSTRUCTOR -> Objects
                .requireNonNull(creator.getEnclosingElement())
                .asType();

            case METHOD -> creator.getReturnType();

            default -> throw new IllegalArgumentException(
                "creator should be CONSTRUCTOR or static METHOD"
            );
        };
    }

    private record Gen() {
        public static ParameterSpec toParamSpec(VariableElement variable) {
            return ParameterSpec
                .builder(
                    asTypeName(variable.asType()),
                    variable.getSimpleName().toString()
                )
                .addAnnotations(
                    variable.getAnnotationMirrors()
                        .stream()
                        .map(AnnotationSpec::get)
                        .toList()
                )
                .addModifiers(variable.getModifiers())
                .build();
        }

        public static TypeName asTypeName(TypeMirror type) {

        }

        public static TypeName toTypeName(TypeParameterElement typeParam) {

        }

        public static TypeVariableName asTypeVariableName(TypeParameterElement typeParam) {

        }
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

    public static class ImpossibleError extends RuntimeException {
        public ImpossibleError(String message) {
            super(message);
        }
    }
}
