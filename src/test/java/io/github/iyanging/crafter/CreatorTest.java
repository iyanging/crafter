package io.github.iyanging.crafter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JavacExtension.class)
@Processors({ Crafter.class })
public class CreatorTest {
    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            @Builder
            public class Entity {
                String a;
                Integer b;
            }
            
            """
    )
    public void on_class_cannot_generate_from_class_default_ctor(Results results) {
        assertThat(results.errors)
            .hasSize(1)
            .map(diagnostic -> diagnostic.getMessage(null))
            .first()
            .asString()
            .contains("has no parameterized constructor");
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            @Builder
            public class Entity {
                String a;
                Integer b;
    
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
    public void on_class_cannot_generate_from_multiple_parameterized_ctor(Results results) {
        assertThat(results.errors)
            .hasSize(1)
            .map(diagnostic -> diagnostic.getMessage(null))
            .first()
            .asString()
            .contains("does not know which constructor");
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            @Builder
            public class Entity {
                String a;
                Integer b;
    
                public Entity() {
                    this.a = "";
                    this.b = 0;
                }
    
                public Entity(String a, Integer b) {
                    this.a = a;
                    this.b = b;
                }
            }
    
            """
    )
    public void on_class_generate_from_single_parameterized_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "EntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            @Builder
            public record Entity(String a) {}
            
            """
    )
    public void on_record_generate_from_default_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "EntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            public record Entity(String a) {
                @Builder
                Entity {}
            }
            
            """
    )
    public void on_record_generate_from_compact_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "EntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            public record Entity(String a) {
            
                public Entity() {
                    this("");
                }
            
                @Builder
                public Entity(String a, Integer b) {
                    this(a);
                }
            }
            
            """
    )
    public void on_record_generate_from_custom_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "EntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }

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
    public void on_ctor_generate_from_class_annotated_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "EntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            
            public class Entity {
                @Builder
                public record InnerEntity(String a) {}
            }
            
            """
    )
    public void generate_from_inner_record(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "InnerEntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """
            
            import io.github.iyanging.crafter.Builder;
            import java.util.List;
            
            @Builder
            public record Entity<T>(List<T> list) {}
            
            """
    )
    public void generate_from_generic_ctor(Results results) {
        assertEquals(1, results.generatedSources.size());
        assertEquals(
            "EntityBuilder.java",
            Path.of(results.generatedSources.get(0).getName()).getFileName().toString()
        );
    }
}
