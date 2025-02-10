package io.github.iyanging.crafter;

import static io.github.iyanging.crafter.util.CodeStructureAssertion.assertStructureEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JavacExtension.class)
@Processors({ Crafter.class })
public class OnCtorTest {
    @Test
    @Inline(
        name = "Entity",
        source = """
        
        import io.github.iyanging.crafter.Builder;
        
        @Builder
        public class Entity {
            String a;
            Integer b;

            @Builder
            public Entity(String a) {
                this.a = a;
                this.b = 0;
            }

            public Entity(String a, Integer b) {
                this.a = a;
                this.b = b;
            }
        }
        
        """
    )
    public void generate_from_class_annotated_ctor(Results results) {
        // TODO
        return;
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
        
        import io.github.iyanging.crafter.Builder;
        
        public record Entity(String a, Integer b) {
            @Builder
            public Entity {}
        }
        
        """
    )
    public void generate_from_record_compact_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());

        assertStructureEquals(
            """
            
            @Generated("Crafter")
            public class EntityBuilder {
                public interface FinalStage {
                    Entity build();
                }
            
                public interface b {
                    FinalStage b(Integer b);
                }
            
                public interface FirstStage {
                    b a(String a);
                }
            
                public static class Builder implements FinalStage, b, FirstStage {
                    private String a;
                    private Integer b;

                    private Builder() {}

                    public b a(String a) {
                        this.a = a;
                        return this;
                    }

                    public b a(Integer b) {
                        this.b = b;
                        return this;
                    }

                    public Entity build() {
                        return new Entity(a, b);
                    }
                }

                public static Builder builder() {
                    return new Builder();
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
        
        public record Entity(String a, Integer b) {
            public Entity {}

            @Builder
            public Entity(String a) {
                this(a, 0);
            }
        }
        
        """
    )
    public void generate_from_record_custom_ctor(Results results) {
        // TODO
        assertThat(results.generatedSources).hasSize(1);
    }
}
