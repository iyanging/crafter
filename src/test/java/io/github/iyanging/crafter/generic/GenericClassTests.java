package io.github.iyanging.crafter.generic;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.iyanging.crafter.Crafter;

import static io.github.iyanging.crafter.TestUtil.assertGenOneSrc;


@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class GenericClassTests {
    @Test
    @Inline(
        name = "Entity",
        source = """

        import io.github.iyanging.crafter.Builder;

        public class Entity<X> {
            @Builder
            public Entity() {}
        }

        """
    )
    public void ok_withNoArgsCtor(Results results) {
        assertGenOneSrc(results)
            .matchesStructureOf(
                """
                
                import java.lang.Override;
                import javax.annotation.processing.Generated;
                
                @Generated("%s")
                public class EntityBuilder {
                  private EntityBuilder() {
                  }
                
                  public static <X> FinalStage<X> builder() {
                    return new Builder();
                  }

                  public interface FinalStage<X> {
                    Entity<X> build();
                  }

                  public static class Builder<X> implements FinalStage<X> {
                    @Override
                    public Entity<X> build() {
                      return new Entity<X>();
                    }
                  }
                }
                
                """.formatted(Crafter.class.getCanonicalName())
            );
    }

    @Test
    @Inline(
        name = "Entity",
        source = """

        import io.github.iyanging.crafter.Builder;

        public class Entity<X> {
            @Builder
            public Entity(String a) {}
        }

        """
    )
    public void ok_withOneIrrelevantArgsCtor(Results results) {
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
                
                  public static <X> A<X> builder() {
                    return new Builder();
                  }
                
                  public interface A<X> {
                    FinalStage<X> a(String a);
                  }
                
                  public interface FinalStage<X> {
                    Entity<X> build();
                  }
                
                  public static class Builder<X> implements A<X>, FinalStage<X> {
                    protected String a;
                
                    @Override
                    public FinalStage<X> a(String a) {
                      this.a = a;
                      return this;
                    }
                
                    @Override
                    public Entity<X> build() {
                      return new Entity<X>(a);
                    }
                  }
                }
                
                """.formatted(Crafter.class.getCanonicalName())
            );
    }
}
