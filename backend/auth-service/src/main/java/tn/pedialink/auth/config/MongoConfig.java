package tn.pedialink.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Explicitly configures Spring Data MongoDB to scan only the main repository
 * package, preventing it from picking up JPA H2 repositories.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "tn.pedialink.auth.repository")
public class MongoConfig {
}
