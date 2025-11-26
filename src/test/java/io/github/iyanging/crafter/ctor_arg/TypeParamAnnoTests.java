package io.github.iyanging.crafter.ctor_arg;

import static io.github.iyanging.crafter.TestUtil.assertGenOneSrc;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.iyanging.crafter.Crafter;


@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class TypeParamAnnoTests {
    @Test
    @Inline(
        name = "Entity",
        source = """
        
        import java.lang.annotation.*;

        import io.github.iyanging.crafter.Builder;

        public class Entity {
            @Target(ElementType.TYPE_PARAMETER)
            public @interface TypeParamAnno {}

            @Builder
            public <@TypeParamAnno T> Entity(T a) {}
        }

        """
    )
    public void ok_copyParamAnno(Results results) {
        assertGenOneSrc(results)
            .matchesStructureOf(
                """
                
                import java.lang.Override;
                import javax.annotation.processing.Generated;
                
                @Generated("%s")
                public class EntityBuilder {
                    private EntityBuilder() {
                    }

                    public static <@Entity.TypeParamAnno T> A<T> builder() {
                        return new Builder();
                    }

                    public interface A <@Entity.TypeParamAnno T> {
                        FinalStage a(T a);
                    }

                    public interface FinalStage {
                        Entity build();
                    }

                    public static class Builder <@Entity.TypeParamAnno T> implements A<T>, FinalStage {
                        protected T a;

                        @Override
                        public FinalStage a(T a) {
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
