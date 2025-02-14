package io.github.iyanging.crafter;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;


public class IntegrationTest {
    // public static class Manager {
    // private final String name;
    // private final Integer age;
    // private final Map.@Nullable Entry<String, String> additionalInformation;
    //
    // public Manager() {
    // this("");
    // }
    //
    // public Manager(String name) {
    // this(name, -1, null);
    // }
    //
    // @Builder
    // public Manager(
    // String name,
    // Integer age,
    // Map.@Nullable Entry<String, String> additionalInformation
    // ) {
    // this.name = name;
    // this.age = age;
    // this.additionalInformation = additionalInformation;
    // }
    //
    // public String getName() { return name; }
    //
    // public Integer getAge() { return age; }
    //
    // public Map.@Nullable Entry<String, String> getAdditionalInformation() {
    // return additionalInformation;
    // }
    // }

    // @Builder
    // public record Employee(
    // @NotEmpty(message = "name cannot be null or empty") String name,
    // @Nullable @Min(18) Integer age,
    // Map.@Nullable Entry<String, String> additionalInformation
    // ) {}

    @Builder
    public record Group <T>(
        @NotNull List<@Valid T> members
    ) {}

    @Test
    public void integration_test() {

    }
}
