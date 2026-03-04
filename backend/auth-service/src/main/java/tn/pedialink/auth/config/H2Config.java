package tn.pedialink.auth.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Dual-database configuration: configures H2 as a secondary/backup datasource
 * alongside the primary MongoDB. H2 repositories live in the
 * {@code tn.pedialink.auth.h2.repository} package and JPA entities in
 * {@code tn.pedialink.auth.h2.entity}.
 *
 * <p>H2 web console is enabled at {@code http://localhost:8081/h2-console}
 * (JDBC URL: {@code jdbc:h2:file:./data/auth-backup}, user: {@code sa}, password: empty).
 */
@Configuration
@EnableAsync
@EnableJpaRepositories(
        basePackages = "tn.pedialink.auth.h2.repository",
        entityManagerFactoryRef = "h2EntityManagerFactory",
        transactionManagerRef = "h2TransactionManager"
)
public class H2Config {

    // ── DataSource ─────────────────────────────────────────────────────────────

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties h2DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "h2DataSource")
    public DataSource h2DataSource() {
        return h2DataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    // ── EntityManagerFactory ───────────────────────────────────────────────────

    @Bean(name = "h2EntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean h2EntityManagerFactory(
            @Qualifier("h2DataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("tn.pedialink.auth.h2.entity");
        em.setPersistenceUnitName("h2PU");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.put("hibernate.show_sql", false);
        em.setJpaPropertyMap(props);

        return em;
    }

    // ── TransactionManager ─────────────────────────────────────────────────────

    @Bean(name = "h2TransactionManager")
    public PlatformTransactionManager h2TransactionManager(
            @Qualifier("h2EntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
