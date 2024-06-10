package com.hmall.api.config;

import com.hmall.api.fallback.ItemClientFallbackFactory;
import com.hmall.api.interceptors.UserInfoInterceptor;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoInterceptor(){
        return new UserInfoInterceptor();
    }

    @Bean
    public ItemClientFallbackFactory itemClientFallbackFactory(){
        return new ItemClientFallbackFactory();
    }
}
