package tn.pedialink.treatment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Explicitly configures Spring Data MongoDB to scan only the main repository
 * package, preventing it from picking up JPA H2 repositories.
 * MongoDB auditing (@CreatedDate / @LastModifiedDate) is enabled here.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "tn.pedialink.treatment.repository")
public class MongoConfig {
}
