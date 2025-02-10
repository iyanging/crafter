package io.github.iyanging.crafter.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.tools.JavaFileObject;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.EqualsVisitor;


/**
 * Reference: {@link EqualsVisitor}
 */
public class CodeStructureAssertion {

    public static void assertStructureEquals(String expected, JavaFileObject actual) {
        final String actualCode;
        try (var actualInputStream = actual.openInputStream()) {
            actualCode = new String(
                actualInputStream.readAllBytes(),
                StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertStructureEquals(expected, actualCode);
    }

    public static void assertStructureEquals(String expected, String actual) {
        final var expectedCu = parse("expected-code", expected);
        final var actualCu = parse("actual-code", actual);

        assertCompilationUnitEquals(expectedCu, actualCu);
    }

    private static void assertCompilationUnitEquals(
        CompilationUnit expectedCu,
        CompilationUnit actualCu
    ) {
        final var expectedTypes = expectedCu.getTypes();
        final var actualTypes = actualCu.getTypes();

        assertArrayEquals(
            expectedTypes.stream().map(TypeDeclaration::getNameAsString).toArray(),
            actualTypes.stream().map(TypeDeclaration::getNameAsString).toArray(),
            "Types existence mismatch"
        );

        for (var i = 0; i < expectedTypes.size(); i++) {
            final var expectedType = (ClassOrInterfaceDeclaration) expectedTypes.get(i);
            final var actualType = (ClassOrInterfaceDeclaration) actualTypes.get(i);

            assertTypeEquals(expectedType, actualType);
        }

    }

    private static void assertTypeEquals(
        ClassOrInterfaceDeclaration expectedType,
        ClassOrInterfaceDeclaration actualType
    ) {
        final var expectedTypeName = expectedType.getNameAsString();

        assertEquals(
            expectedType.isInterface(),
            actualType.isInterface(),
            "Type kind of %s mismatch".formatted(expectedTypeName)
        );

        assertEquals(
            expectedType.getName(),
            actualType.getName(),
            "Type name of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getAnnotations().toArray(),
            actualType.getAnnotations().toArray(),
            "Type annotations of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getModifiers().toArray(),
            actualType.getModifiers().toArray(),
            "Type modifiers of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getTypeParameters().toArray(),
            actualType.getTypeParameters().toArray(),
            "Type parameters of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getExtendedTypes().toArray(),
            actualType.getExtendedTypes().toArray(),
            "Type extends of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getImplementedTypes().toArray(),
            actualType.getImplementedTypes().toArray(),
            "Type implements of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getPermittedTypes().toArray(),
            actualType.getPermittedTypes().toArray(),
            "Type permits of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getConstructors().toArray(),
            actualType.getConstructors().toArray(),
            "Type constructor of %s mismatch".formatted(expectedTypeName)
        );

        assertArrayEquals(
            expectedType.getFields().toArray(),
            actualType.getFields().toArray(),
            "Type permits of %s mismatch".formatted(expectedTypeName)
        );

        final var expectedInnerTypes = expectedType
            .getMembers()
            .stream()
            .filter(m -> m instanceof TypeDeclaration<?>)
            .map(TypeDeclaration.class::cast)
            .toList();
        final var actualInnerTypes = actualType
            .getMembers()
            .stream()
            .filter(m -> m instanceof TypeDeclaration<?>)
            .map(TypeDeclaration.class::cast)
            .toList();

        assertArrayEquals(
            expectedInnerTypes.stream().map(TypeDeclaration::getNameAsString).toArray(),
            actualInnerTypes.stream().map(TypeDeclaration::getNameAsString).toArray(),
            "InnerType existence of %s mismatch".formatted(expectedTypeName)
        );

        for (var i = 0; i < expectedInnerTypes.size(); i++) {
            final var expectedInnerType = (ClassOrInterfaceDeclaration) expectedInnerTypes.get(i);
            final var actualInnerType = (ClassOrInterfaceDeclaration) actualInnerTypes.get(i);

            assertTypeEquals(expectedInnerType, actualInnerType);
        }

        final var expectedMethods = expectedType.getMethods();
        final var actualMethods = actualType.getMethods();

        assertArrayEquals(
            expectedMethods.stream().map(MethodDeclaration::getNameAsString).toArray(),
            actualMethods.stream().map(MethodDeclaration::getNameAsString).toArray(),
            "Method existence of %s mismatch".formatted(expectedTypeName)
        );

        for (var i = 0; i < expectedMethods.size(); i++) {
            final var expectedMethod = expectedMethods.get(i);
            final var actualMethod = actualMethods.get(i);

            assertMethodEquals(expectedMethod, actualMethod);
        }

    }

    private static void assertMethodEquals(
        MethodDeclaration expectedMethod,
        MethodDeclaration actualMethod
    ) {
        final var expectedMethodName = expectedMethod.getNameAsString();

        assertEquals(
            expectedMethod.getName(),
            actualMethod.getName(),
            "Method name of %s mismatch".formatted(expectedMethodName)
        );

        assertArrayEquals(
            expectedMethod.getAnnotations().toArray(),
            actualMethod.getAnnotations().toArray(),
            "Method annotations of %s mismatch".formatted(expectedMethodName)
        );

        assertArrayEquals(
            expectedMethod.getModifiers().toArray(),
            actualMethod.getModifiers().toArray(),
            "Method modifiers of %s mismatch".formatted(expectedMethodName)
        );

        assertArrayEquals(
            expectedMethod.getTypeParameters().toArray(),
            actualMethod.getTypeParameters().toArray(),
            "Method type parameters of %s mismatch".formatted(expectedMethodName)
        );

        assertArrayEquals(
            expectedMethod.getParameters().toArray(),
            actualMethod.getParameters().toArray(),
            "Method parameters of %s mismatch".formatted(expectedMethodName)
        );

        assertArrayEquals(
            expectedMethod.getThrownExceptions().toArray(),
            actualMethod.getThrownExceptions().toArray(),
            "Method throws of %s mismatch".formatted(expectedMethodName)
        );
    }

    private static CompilationUnit parse(String role, String code) {
        final var result = new JavaParser().parse(code);

        if (result.isSuccessful()) {
            return result.getResult().orElseThrow();
        } else {
            throw new IllegalArgumentException(
                "Cannot parse %s because ".formatted(role)
                    + result
            );
        }

    }
}
