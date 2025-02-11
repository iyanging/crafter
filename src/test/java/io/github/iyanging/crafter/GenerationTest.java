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
public class GenerationTest {
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
        assertStructureEquals(
            """
                
                @Generated("%s")
                public class EntityBuilder {
                    private EntityBuilder() {}
                
                    public interface FirstStage { B_ a(String a); }
                    public interface B_ { Builder b(Integer b); }
                    public interface FinalStage { Entity build(); }
    
                    public static class Builder implements FirstStage, B_, FinalStage {
                        protected String a;
                        protected Integer b;
                
                        @Override public B_ a(String a);
                        @Override public FinalStage b(Integer b);
                        @Override public Entity build();
                    }
                
                    public static Builder builder();
                }
                
                """.formatted(Crafter.TOOL_NAME),
            results.generatedSources.get(0)
        );
    }
}
