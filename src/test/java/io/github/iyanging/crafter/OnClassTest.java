package io.github.iyanging.crafter;

import static org.assertj.core.api.Assertions.assertThat;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JavacExtension.class)
@Processors({ Crafter.class })
public class OnClassTest {
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
    public void cannot_generate_from_default_ctor(Results results) {
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
    public void cannot_generate_from_multiple_parameterized_ctor(Results results) {
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
    public void generate_from_single_parameterized_ctor(Results results) {
        // TODO
        return;
    }
}
