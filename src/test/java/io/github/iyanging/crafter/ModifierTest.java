package io.github.iyanging.crafter;

import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class ModifierTest {}
