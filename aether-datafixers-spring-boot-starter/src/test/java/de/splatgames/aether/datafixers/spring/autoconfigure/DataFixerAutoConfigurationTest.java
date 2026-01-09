/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.datafixers.spring.autoconfigure;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.SimpleType;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.api.util.Pair;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import de.splatgames.aether.datafixers.spring.AetherDataFixersProperties;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataFixerAutoConfiguration}.
 */
@DisplayName("DataFixerAutoConfiguration")
class DataFixerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataFixerAutoConfiguration.class))
            .withPropertyValues(
                    "aether.datafixers.enabled=true",
                    "aether.datafixers.default-current-version=100"
            );

    @Nested
    @DisplayName("Basic Auto-Configuration")
    class BasicAutoConfiguration {

        @Test
        @DisplayName("creates DataFixerRegistry bean")
        void createsDataFixerRegistryBean() {
            contextRunner
                    .withUserConfiguration(SingleBootstrapConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(DataFixerRegistry.class);
                    });
        }

        @Test
        @DisplayName("creates AetherDataFixer bean from bootstrap")
        void createsAetherDataFixerBeanFromBootstrap() {
            contextRunner
                    .withUserConfiguration(SingleBootstrapConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(AetherDataFixer.class);
                        AetherDataFixer fixer = context.getBean(AetherDataFixer.class);
                        assertThat(fixer.currentVersion().getVersion()).isEqualTo(100);
                    });
        }

        @Test
        @DisplayName("registers fixer in registry")
        void registersFixerInRegistry() {
            contextRunner
                    .withUserConfiguration(SingleBootstrapConfig.class)
                    .run(context -> {
                        DataFixerRegistry registry = context.getBean(DataFixerRegistry.class);
                        assertThat(registry.size()).isEqualTo(1);
                        assertThat(registry.getDefault()).isNotNull();
                    });
        }
    }

    @Nested
    @DisplayName("Multi-Domain Support")
    class MultiDomainSupport {

        private final ApplicationContextRunner multiDomainRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataFixerAutoConfiguration.class))
                .withPropertyValues(
                        "aether.datafixers.enabled=true",
                        "aether.datafixers.domains.game.current-version=200",
                        "aether.datafixers.domains.user.current-version=150"
                );

        @Test
        @DisplayName("creates multiple AetherDataFixer beans with manual configuration")
        void createsMultipleAetherDataFixerBeans() {
            multiDomainRunner
                    .withUserConfiguration(MultiBootstrapWithFixersConfig.class)
                    .run(context -> {
                        assertThat(context.getBeansOfType(AetherDataFixer.class)).hasSize(2);
                    });
        }

        @Test
        @DisplayName("registers all fixers in registry")
        void registersAllFixersInRegistry() {
            multiDomainRunner
                    .withUserConfiguration(MultiBootstrapWithFixersConfig.class)
                    .run(context -> {
                        DataFixerRegistry registry = context.getBean(DataFixerRegistry.class);
                        assertThat(registry.size()).isEqualTo(2);
                        assertThat(registry.contains("game")).isTrue();
                        assertThat(registry.contains("user")).isTrue();
                    });
        }

        @Test
        @DisplayName("qualifiers allow individual injection")
        void qualifiersAllowIndividualInjection() {
            multiDomainRunner
                    .withUserConfiguration(MultiBootstrapWithFixersConfig.class)
                    .run(context -> {
                        DataFixerRegistry registry = context.getBean(DataFixerRegistry.class);

                        AetherDataFixer gameFixer = registry.get("game");
                        AetherDataFixer userFixer = registry.get("user");

                        assertThat(gameFixer.currentVersion().getVersion()).isEqualTo(200);
                        assertThat(userFixer.currentVersion().getVersion()).isEqualTo(150);
                    });
        }
    }

    @Nested
    @DisplayName("Configuration Properties")
    class ConfigurationProperties {

        @Test
        @DisplayName("respects enabled=false")
        void respectsEnabledFalse() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(DataFixerAutoConfiguration.class))
                    .withPropertyValues(
                            "aether.datafixers.enabled=false",
                            "aether.datafixers.default-current-version=100"
                    )
                    .withUserConfiguration(SingleBootstrapConfig.class)
                    .run(context -> {
                        // Registry might still exist but should be empty or config disabled
                        assertThat(context.containsBean("aetherDataFixer")).isFalse();
                    });
        }

        @Test
        @DisplayName("uses default-current-version from properties")
        void usesDefaultCurrentVersionFromProperties() {
            contextRunner
                    .withPropertyValues("aether.datafixers.default-current-version=500")
                    .withUserConfiguration(VersionlessBootstrapConfig.class)
                    .run(context -> {
                        AetherDataFixer fixer = context.getBean(AetherDataFixer.class);
                        assertThat(fixer.currentVersion().getVersion()).isEqualTo(500);
                    });
        }
    }

    @Nested
    @DisplayName("No Bootstrap Configuration")
    class NoBootstrapConfiguration {

        @Test
        @DisplayName("auto-configuration skipped when no bootstrap defined")
        void autoConfigurationSkippedWhenNoBootstrapDefined() {
            // Without any DataFixerBootstrap bean, the @ConditionalOnBean condition
            // prevents the auto-configuration from loading
            contextRunner
                    .run(context -> {
                        // DataFixerRegistry is not created when no bootstrap is defined
                        assertThat(context.containsBean("dataFixerRegistry")).isFalse();
                    });
        }
    }

    @Nested
    @DisplayName("Properties Bean")
    class PropertiesBean {

        @Test
        @DisplayName("creates AetherDataFixersProperties bean")
        void createsPropertiesBean() {
            contextRunner
                    .withUserConfiguration(SingleBootstrapConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(AetherDataFixersProperties.class);
                    });
        }

        @Test
        @DisplayName("binds properties from environment")
        void bindsPropertiesFromEnvironment() {
            contextRunner
                    .withPropertyValues(
                            "aether.datafixers.default-current-version=999",
                            "aether.datafixers.default-format=JACKSON"
                    )
                    .withUserConfiguration(SingleBootstrapConfig.class)
                    .run(context -> {
                        AetherDataFixersProperties props = context.getBean(AetherDataFixersProperties.class);
                        assertThat(props.getDefaultCurrentVersion()).isEqualTo(999);
                    });
        }
    }

    // Test Configuration Classes

    @Configuration
    static class SingleBootstrapConfig {
        @Bean
        public DataFixerBootstrap simpleBootstrap() {
            return new DataFixerBootstrap() {
                public static final int CURRENT_VERSION = 100;

                @Override
                public void registerSchemas(SchemaRegistry schemas) {
                    schemas.register(new TestSchema(100, "test"));
                }

                @Override
                public void registerFixes(FixRegistrar fixes) {
                    // No fixes needed
                }
            };
        }
    }

    @Configuration
    static class MultiBootstrapConfig {
        @Bean
        @Qualifier("game")
        public DataFixerBootstrap gameBootstrap() {
            return new DataFixerBootstrap() {
                public static final int CURRENT_VERSION = 200;

                @Override
                public void registerSchemas(SchemaRegistry schemas) {
                    schemas.register(new TestSchema(200, "game"));
                }

                @Override
                public void registerFixes(FixRegistrar fixes) {
                }
            };
        }

        @Bean
        @Qualifier("user")
        public DataFixerBootstrap userBootstrap() {
            return new DataFixerBootstrap() {
                public static final int CURRENT_VERSION = 150;

                @Override
                public void registerSchemas(SchemaRegistry schemas) {
                    schemas.register(new TestSchema(150, "user"));
                }

                @Override
                public void registerFixes(FixRegistrar fixes) {
                }
            };
        }
    }

    /**
     * Multi-domain configuration with manually created AetherDataFixer beans.
     * This demonstrates the proper way to configure multi-domain support.
     */
    @Configuration
    static class MultiBootstrapWithFixersConfig {
        @Bean
        @Qualifier("game")
        public DataFixerBootstrap gameBootstrap() {
            return new DataFixerBootstrap() {
                @Override
                public void registerSchemas(SchemaRegistry schemas) {
                    schemas.register(new TestSchema(200, "game"));
                }

                @Override
                public void registerFixes(FixRegistrar fixes) {
                }
            };
        }

        @Bean
        @Qualifier("user")
        public DataFixerBootstrap userBootstrap() {
            return new DataFixerBootstrap() {
                @Override
                public void registerSchemas(SchemaRegistry schemas) {
                    schemas.register(new TestSchema(150, "user"));
                }

                @Override
                public void registerFixes(FixRegistrar fixes) {
                }
            };
        }

        @Bean
        @Qualifier("game")
        public AetherDataFixer gameFixer(
                @Qualifier("game") DataFixerBootstrap bootstrap,
                AetherDataFixersProperties properties,
                DataFixerRegistry registry
        ) {
            return DataFixerAutoConfiguration.createQualifiedFixer(bootstrap, "game", properties, registry);
        }

        @Bean
        @Qualifier("user")
        public AetherDataFixer userFixer(
                @Qualifier("user") DataFixerBootstrap bootstrap,
                AetherDataFixersProperties properties,
                DataFixerRegistry registry
        ) {
            return DataFixerAutoConfiguration.createQualifiedFixer(bootstrap, "user", properties, registry);
        }
    }

    @Configuration
    static class VersionlessBootstrapConfig {
        @Bean
        public DataFixerBootstrap versionlessBootstrap() {
            return new DataFixerBootstrap() {
                // No CURRENT_VERSION constant - should use property

                @Override
                public void registerSchemas(SchemaRegistry schemas) {
                    schemas.register(new TestSchema(500, "test"));
                }

                @Override
                public void registerFixes(FixRegistrar fixes) {
                }
            };
        }
    }

    /**
     * Simple test schema for unit tests.
     */
    static class TestSchema extends Schema {
        private final String typeName;

        public TestSchema(int versionId, String typeName) {
            super(versionId, null);
            this.typeName = typeName;
        }

        @Override
        @NotNull
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        @Override
        protected void registerTypes() {
            registerType(new SimpleType<>(
                    new TypeReference(typeName),
                    dynamicPassthroughCodec()
            ));
        }

        @NotNull
        private static Codec<Dynamic<?>> dynamicPassthroughCodec() {
            return new Codec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final Dynamic<?> input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T prefix) {
                    @SuppressWarnings("unchecked")
                    final Dynamic<Object> dynamicObj = (Dynamic<Object>) input;
                    final Dynamic<T> converted = dynamicObj.convert(ops);
                    return DataResult.success(converted.value());
                }

                @NotNull
                @Override
                public <T> DataResult<Pair<Dynamic<?>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                                   @NotNull final T input) {
                    final Dynamic<T> dynamic = new Dynamic<>(ops, input);
                    return DataResult.success(Pair.of(dynamic, ops.empty()));
                }
            };
        }
    }
}
