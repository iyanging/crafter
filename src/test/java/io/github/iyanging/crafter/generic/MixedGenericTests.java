package io.github.iyanging.crafter.generic;

import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Processors;
import io.github.iyanging.crafter.Crafter;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class MixedGenericTests {

    public static class A<X> {}

    public static <X, Y> A<Y> build(X x) {
        return new A<>();
    }
}
