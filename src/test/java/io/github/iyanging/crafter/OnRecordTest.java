package io.github.iyanging.crafter;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JavacExtension.class)
public class OnRecordTest {
    @Test
    @Processors({ Crafter.class })
    @Inline(
        name = "Student",
        source = """
        
        import io.github.iyanging.crafter.Builder;
        
        @Builder
        public record Student(
            String a,
            Integer b
        ) {}
        
        """
    )
    public void generate_without_nullable(Results results) {
        return;
    }
}
