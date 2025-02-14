package io.github.iyanging.crafter;

import static io.github.iyanging.crafter.util.CodeStructureAssertion.assertStructureEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class ParamTest {
    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            @Builder
            public record Entity<T>(T a) {}
            
            """
    )
    public void generate_from_type_variable_param(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertStructureEquals(
            """
            
            @Generated("%s")
            public class EntityBuilder {
                private EntityBuilder() {}
            
                public static <T> Builder<T> builder();
            
                public interface FirstStage<T> {
                    FinalStage<T> a(T a);
                }
            
                public interface FinalStage<T> {
                    Entity<T> build();
                }
            
                public static class Builder<T> implements FirstStage<T>, FinalStage<T> {
                    protected T a;
            
                    @Override
                    public FinalStage<T> a(T a);
            
                    @Override
                    public Entity<T> build();
                }
            }
            
            """.formatted(Crafter.TOOL_NAME),
            results.generatedSources.get(0)
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            import java.util.List;
            
            @Builder
            public record Entity<T>(List<T> a) {}
            
            """
    )
    public void generate_from_parameterized_type_param(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertStructureEquals(
            """
            
            @Generated("Crafter")
            public class EntityBuilder {
                private EntityBuilder() {}
            
                public static <T> Builder<T> builder();
            
                public interface FirstStage<T> {
                    FinalStage<T> a(List<T> a);
                }
            
                public interface FinalStage<T> {
                    Entity<T> build();
                }
            
                public static class Builder<T> implements FirstStage<T>, FinalStage<T> {
                    protected List<T> a;
            
                    @Override
                    public FinalStage<T> a(List<T> a);
            
                    @Override
                    public Entity<T> build();
                }
            }
            
            """,
            results.generatedSources.get(0)
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            import java.util.Map;
            import org.jspecify.annotations.Nullable;
            import jakarta.validation.constraints.NotEmpty;
            
            @Builder
            public record Entity(
                Map.@Nullable Entry<
                    @NotEmpty(message = "name cannot be null or empty")
                    String,
                    String
                > a
            ) {}
            
            """
    )
    public void generate_from_annotated_param(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertStructureEquals(
            """
            
            @Generated("Crafter")
            public class EntityBuilder {
                private EntityBuilder() {}
            
                public static Builder builder() {
                    return new Builder();
                }
            
                public interface FirstStage {
                    FinalStage a(Map. @Nullable Entry<String, String> a);
                }
            
                public interface FinalStage {
                    Entity build();
                }
            
                public static class Builder implements FirstStage, FinalStage {
                    protected Map. @Nullable Entry<
                        @NotEmpty(message = "name cannot be null or empty") String,
                        String
                    > a;
            
                    @Override
                    public FinalStage a(Map. @Nullable Entry<
                        @NotEmpty(message = "name cannot be null or empty") String,
                        String
                    > a);
            
                    @Override
                    public Entity build();
                }
            }
            
            """,
            results.generatedSources.get(0)
        );
    }
}
