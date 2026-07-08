package com.aiflow.enterprise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.environment:development}")
    private String environment;

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local development server");

        Server prodServer = new Server()
                .url("https://api.aiflow.example.com")
                .description("Production server");

        return new OpenAPI()
                .info(new Info()
                        .title("AIFlow Enterprise API")
                        .version("1.0.0")
                        .description("""
                                AIFlow Enterprise – Intelligent Workflow Automation Platform.
                                
                                RESTful API for managing workflows, executions, tasks, users, triggers, and audit logs.
                                All endpoints are fully connected to MongoDB Atlas.
                                """)
                        .contact(new Contact()
                                .name("AIFlow Team")
                                .email("support@aiflow.example.com")
                                .url("https://aiflow.example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://aiflow.example.com/license")))
                .servers(environment.equals("production") ? List.of(prodServer) : List.of(localServer, prodServer));
    }
}
