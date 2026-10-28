package com.cs_42_3.surveyplatformbackend.behavior.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers bounded behavior upload configuration without modifying other collection modules.
 *
 * @author Simon Tian
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BehaviorProperties.class)
public class BehaviorConfiguration { }
