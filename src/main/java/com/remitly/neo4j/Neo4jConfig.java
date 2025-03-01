package com.remitly.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    private static final String NEO4J_URI = System.getenv().getOrDefault("NEO4J_URI", "bolt://localhost:7687");
    private static final String NEO4J_USER = System.getenv().getOrDefault("NEO4J_USER", "neo4j");
    private static final String NEO4J_PASSWORD = System.getenv().getOrDefault("NEO4J_PASSWORD", "password");

    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
    }
}