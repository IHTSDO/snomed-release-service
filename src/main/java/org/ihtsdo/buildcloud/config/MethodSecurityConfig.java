package org.ihtsdo.buildcloud.config;

import org.ihtsdo.buildcloud.core.service.PermissionService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig  {

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(@Lazy PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler =
                new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }

    @Bean
    public PermissionEvaluator permissionEvaluator(@Lazy PermissionService permissionService) {
        return new PermissionEvaluator() {
            @Override
            public boolean hasPermission(@NonNull Authentication authentication, Object role, @Nullable Object releaseCenterKey) {
                if (releaseCenterKey == null) {
                    throw new SecurityException("Release center is null, can not ascertain roles.");
                }
                return permissionService.userHasRoleOnReleaseCenter((String) role, (String) releaseCenterKey);
            }

            @Override
            public boolean hasPermission(@NonNull Authentication authentication, @NonNull Serializable targetId, @NonNull String targetType, @NonNull Object permission) {
                return false;
            }
        };
    }

    @Bean
    public static BeanFactoryPostProcessor infrastructureRoleSetter() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("permissionEvaluator")) {
                beanFactory.getBeanDefinition("permissionEvaluator")
                        .setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            }
            if (beanFactory.containsBeanDefinition("methodSecurityConfig")) {
                beanFactory.getBeanDefinition("methodSecurityConfig")
                        .setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            }
        };
    }
}

