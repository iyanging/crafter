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

package io.github.iyanging.crafter.annotation_position;

import static io.github.iyanging.crafter.TestUtil.assertGenOneSrc;
import static io.github.iyanging.crafter.TestUtil.assertOneError;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Inline;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.iyanging.crafter.Crafter;


@ExtendWith(JavacExtension.class)
@Processors(Crafter.class)
public class OnClassTests {
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
    void error_withImplicitNoArgsCtor_hasNoParameterizedCtor(Results results) {
        assertOneError(results)
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

            public Entity() {
                this.a = "";
                this.b = 0;
            }
        }
        
        """
    )
    void error_withExplicitNoArgsCtor_hasNoParameterizedCtor(Results results) {
        assertOneError(results)
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
        }
        
        """
    )
    void ok_withOneArgsCtor(Results results) {
        assertGenOneSrc(results);
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

            public Entity(String a) {
                this.a = a;
                this.b = 0;
            }
        }
        
        """
    )
    void ok_withExplicitNoArgsCtor_and_withOneArgsCtor(Results results) {
        assertGenOneSrc(results);
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
    void error_withOneArgsCtor_and_withTwoArgsCtor(Results results) {
        assertOneError(results)
            .contains("does not know which constructor");
    }
}
