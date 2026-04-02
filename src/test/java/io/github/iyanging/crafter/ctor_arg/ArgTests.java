package io.github.iyanging.crafter.ctor_arg;

import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Processors;
import io.github.iyanging.crafter.Crafter;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class ArgTests {
    // ok: ctor中存在忽略大小写相同的参数名
    public static class Build$ {

    }
}
