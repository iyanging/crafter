/*
 * Copyright (c) 2024 iyanging
 *
 * crafter is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 */

package io.github.iyanging.crafter.integration;

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
public class Example1Tests {
    @Test
    @Inline(
        name = "Entity",
        source = """

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Target;
        import java.util.Map;

        import io.github.iyanging.crafter.Builder;

        @Builder
        public class Entity <@Entity.TypeParameterAnno @Entity.TypeUseAnno T> {

            @Target(ElementType.TYPE_PARAMETER)
            public @interface TypeParameterAnno {}
        
            @Target(ElementType.TYPE_USE)
            public @interface TypeUseAnno {}

            @Target(ElementType.PARAMETER)
            public @interface ParamAnno {}

            public Entity(
                @ParamAnno @TypeUseAnno String a,
                @ParamAnno Integer b,
                Map.@TypeUseAnno Entry<@TypeUseAnno String, ?> c
            ) {
            }
        }

        """
    )
    void ok_onNoArgsCtor(Results results) {
        assertGenOneSrc(results);
    }
}
