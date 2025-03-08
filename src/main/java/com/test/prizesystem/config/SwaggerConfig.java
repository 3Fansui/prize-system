package com.test.prizesystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger配置类
 * <p>
 * 配置Swagger API文档生成器，为接口提供详细的文档说明。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String appName;

    /**
     * 创建Docket对象
     * 
     * @return Docket实例
     */
    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.test.prizesystem.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 构建API文档详细信息
     * 
     * @return ApiInfo对象
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("抽奖系统 API文档")
                .contact(new Contact("MCP生成", null, "support@example.com"))
                .version("1.0")
                .description("抽奖系统的接口文档，包含抽奖、活动、统计等功能")
                .build();
    }
}

