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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import javax.tools.JavaFileObject;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.EqualsVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.karuslabs.elementary.Results;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;


public class TestUtil {

    public static class JavaFileAssert extends AbstractAssert<JavaFileAssert, JavaFileObject> {
        public JavaFileAssert(JavaFileObject actual) {
            super(actual, JavaFileAssert.class);
        }

        public static JavaFileAssert assertThat(JavaFileObject actual) {
            return new JavaFileAssert(actual);
        }

        public JavaFileAssert matchesStructureOf(String expected) {
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

            return this;
        }

        private static void assertStructureEquals(String expected, String actual) {
            final var expectedCu = parse("expected-code", expected);
            final var actualCu = parse("actual-code", actual);

            assertCompilationUnitEquals(expectedCu, actualCu);
        }

        /**
         * Asserts that the structures of two {@link CompilationUnit} are equal.
         * 
         * @implNote Manually rewrite from {@link EqualsVisitor#visit(CompilationUnit, Visitable)}
         *           for accurate error messages
         */
        @SuppressWarnings("PatternMatchingInstanceof")
        private static void assertCompilationUnitEquals(
            CompilationUnit expectedCu,
            CompilationUnit actualCu
        ) {
            final var expectedTypes = expectedCu.getTypes();
            final var actualTypes = actualCu.getTypes();

            Assertions
                .assertThatList(actualTypes)
                .hasSameSizeAs(expectedTypes);

            for (var i = 0; i < expectedTypes.size(); i++) {
                final var expectedType = expectedTypes.get(i);
                final var actualType = actualTypes.get(i);

                Assertions
                    .assertThat(actualType)
                    .hasSameClassAs(expectedType);

                if (expectedType instanceof ClassOrInterfaceDeclaration) {
                    assertTypeEquals(
                        (ClassOrInterfaceDeclaration) expectedType,
                        (ClassOrInterfaceDeclaration) actualType
                    );

                } else {
                    Assertions.fail("Cannot handle %s".formatted(expectedType.getClass()));
                }

            }

        }

        /**
         * Asserts that the structures of two {@link ClassOrInterfaceDeclaration} are equal.
         *
         * @implNote Manually rewrite from
         *           {@link EqualsVisitor#visit(ClassOrInterfaceDeclaration, Visitable)} for
         *           accurate error messages
         */
        private static void assertTypeEquals(
            ClassOrInterfaceDeclaration expectedType,
            ClassOrInterfaceDeclaration actualType
        ) {
            Assertions
                .assertThatList(actualType.getExtendedTypes())
                .isEqualTo(expectedType.getExtendedTypes());

            Assertions
                .assertThatList(actualType.getImplementedTypes())
                .isEqualTo(expectedType.getImplementedTypes());

            Assertions
                .assertThat(actualType.isInterface())
                .isEqualTo(expectedType.isInterface());

            Assertions
                .assertThatList(actualType.getPermittedTypes())
                .isEqualTo(expectedType.getPermittedTypes());

            Assertions
                .assertThatList(actualType.getTypeParameters())
                .isEqualTo(expectedType.getTypeParameters());

            assertMemberEquals(
                expectedType.getMembers(),
                actualType.getMembers()
            );

            Assertions
                .assertThatList(actualType.getModifiers())
                .isEqualTo(expectedType.getModifiers());

            Assertions
                .assertThat(actualType.getName())
                .isEqualTo(expectedType.getName());

            Assertions
                .assertThatList(actualType.getAnnotations())
                .isEqualTo(expectedType.getAnnotations());
        }

        @SuppressWarnings("PatternMatchingInstanceof")
        private static void assertMemberEquals(
            List<BodyDeclaration<?>> expectedMembers,
            List<BodyDeclaration<?>> actualMembers
        ) {
            Assertions
                .assertThatList(actualMembers)
                .hasSameSizeAs(expectedMembers);

            for (var i = 0; i < expectedMembers.size(); i++) {
                final var expectedMember = expectedMembers.get(i);
                final var actualMember = actualMembers.get(i);

                Assertions.assertThat(actualMember).hasSameClassAs(expectedMember);

                if (expectedMember instanceof ClassOrInterfaceDeclaration) {
                    assertTypeEquals(
                        (ClassOrInterfaceDeclaration) expectedMember,
                        (ClassOrInterfaceDeclaration) actualMember
                    );

                } else if (expectedMember instanceof ConstructorDeclaration) {
                    Assertions
                        .assertThat(actualMember)
                        .isEqualTo(expectedMember);

                } else if (expectedMember instanceof FieldDeclaration) {
                    Assertions
                        .assertThat(actualMember)
                        .isEqualTo(expectedMember);

                } else if (expectedMember instanceof MethodDeclaration) {
                    assertMethodEquals(
                        (MethodDeclaration) expectedMember,
                        (MethodDeclaration) actualMember
                    );

                } else {
                    Assertions.fail("Cannot handle %s".formatted(expectedMember.getClass()));
                }

            }

        }

        /**
         * Asserts that the structures of two {@link MethodDeclaration} are equal.
         *
         * @implNote Manually rewrite from {@link EqualsVisitor#visit(MethodDeclaration, Visitable)}
         *           for accurate error messages
         */
        private static void assertMethodEquals(
            MethodDeclaration expectedMethod,
            MethodDeclaration actualMethod
        ) {

            Assertions
                .assertThat(actualMethod.getBody())
                .isEqualTo(expectedMethod.getBody());

            Assertions
                .assertThat(actualMethod.getType())
                .isEqualTo(expectedMethod.getType());

            Assertions
                .assertThat(actualMethod.getModifiers())
                .isEqualTo(expectedMethod.getModifiers());

            Assertions
                .assertThat(actualMethod.getName())
                .isEqualTo(expectedMethod.getName());

            Assertions
                .assertThat(actualMethod.getParameters())
                .isEqualTo(expectedMethod.getParameters());

            Assertions
                .assertThat(actualMethod.getReceiverParameter())
                .isEqualTo(expectedMethod.getReceiverParameter());

            Assertions
                .assertThat(actualMethod.getThrownExceptions())
                .isEqualTo(expectedMethod.getThrownExceptions());

            Assertions
                .assertThat(actualMethod.getTypeParameters())
                .isEqualTo(expectedMethod.getTypeParameters());

            Assertions
                .assertThat(actualMethod.getAnnotations())
                .isEqualTo(expectedMethod.getAnnotations());
        }

        private static CompilationUnit parse(String role, String code) {
            final var result = new JavaParser().parse(code);

            if (result.isSuccessful()) {
                return result.getResult().orElseThrow();
            } else {
                throw new IllegalArgumentException(
                    "Cannot parse %s because %s".formatted(role, result)
                );
            }

        }
    }

    public static AbstractStringAssert<?> assertOneError(Results results) {
        return Assertions
            .assertThat(results.errors)
            .hasSize(1)
            .map(diagnostic -> diagnostic.getMessage(Locale.ENGLISH))
            .first()
            .asString();
    }

    public static JavaFileAssert assertGenOneSrc(Results results) {
        Assertions.assertThat(results.errors).isEmpty();
        Assertions.assertThat(results.generatedSources).hasSize(1);
        return JavaFileAssert.assertThat(results.generatedSources.get(0));
    }
}
