package io.github.iyanging.crafter.ctor_arg;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import io.github.iyanging.crafter.Crafter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.github.iyanging.crafter.TestUtil.assertGenOneSrc;


@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class ParamAnnoTests {
    @Test
    @Inline(
        name = "Entity",
        source = """
        
        import java.lang.annotation.*;

        import io.github.iyanging.crafter.Builder;

        public class Entity {
            @Target(ElementType.PARAMETER)
            public @interface ParamAnno {}

            @Builder
            public Entity(@ParamAnno String a) {}
        }

        """
    )
    public void ok_copyParamAnno(Results results) {
        assertGenOneSrc(results)
            .matchesStructureOf(
                """

                import java.lang.Override;
                import java.lang.String;
                import javax.annotation.processing.Generated;

                @Generated("%s")
                public class EntityBuilder {
                  private EntityBuilder() {
                  }
                
                  public static A builder() {
                    return new Builder();
                  }
                
                  public interface A {
                    Build$ a(@Entity.ParamAnno String a);
                  }
                
                  public interface Build$ {
                    Entity build();
                  }
                
                  public static class Builder implements A, Build$ {
                    protected String a;

                    @Override
                    public Build$ a(@Entity.ParamAnno String a) {
                      this.a = a;
                      return this;
                    }
                
                    @Override
                    public Entity build() {
                      return new Entity(a);
                    }
                  }
                }
                
                """.formatted(Crafter.class.getCanonicalName())
            );
    }
}
