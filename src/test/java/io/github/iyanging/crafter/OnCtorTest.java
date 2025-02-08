package io.github.iyanging.crafter;

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
        // TODO
        return;
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
        assertEquals()
        assertThat(results.generatedSources).hasSize(1);
    }
}
