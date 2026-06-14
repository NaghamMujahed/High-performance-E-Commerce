package com.example.demo.config;

import com.example.demo.dto.ProductDetailsResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig {

        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

                Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                                "productDetails",
                                defaultConfig.entryTtl(Duration.ofMinutes(10)),

                                "productCatalog",
                                defaultConfig.entryTtl(Duration.ofMinutes(2)),

                                "topProducts",
                                defaultConfig.entryTtl(Duration.ofMinutes(5)),

                                "dailySalesReports",
                                defaultConfig.entryTtl(Duration.ofMinutes(30)),

                                "categories",
                                defaultConfig.entryTtl(Duration.ofHours(1)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigs)
                                .transactionAware()
                                .build();
        }

        @Bean
        public RedisTemplate<String, List<ProductDetailsResponse>> topProductsRedisTemplate(
                        RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, List<ProductDetailsResponse>> template = new RedisTemplate<>();
                StringRedisSerializer stringSerializer = new StringRedisSerializer();
                JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();

                template.setConnectionFactory(connectionFactory);
                template.setKeySerializer(stringSerializer);
                template.setHashKeySerializer(stringSerializer);
                template.setValueSerializer(valueSerializer);
                template.setHashValueSerializer(valueSerializer);
                template.afterPropertiesSet();

                return template;
        }
}
