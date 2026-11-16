package io.cx.gitstats.persistence.config;

import liquibase.integration.spring.SpringLiquibase;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

@AutoConfigureDataJpa
@ComponentScan(basePackages = "io.cx.gitstats.persistence")
@EnableJpaRepositories(basePackages = "io.cx.gitstats.persistence")
public class PersistenceLayerConfig {
    public static final String PROXY_DATABASE_NAME = "DataSource with QueryCountListener";

    @Autowired
    private PropertyResolver propertyResolver;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setPackagesToScan("io.cx.gitstats.persistence");
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactory.setJpaProperties(getJpaTestProperties());

        return entityManagerFactory;
    }

    @Bean
    @Primary
    DataSource dataSource(@Qualifier("originalDataSource") DataSource originalDataSource) {
        return ProxyDataSourceBuilder.create(originalDataSource)
                .name(PROXY_DATABASE_NAME)
                .listener(new DataSourceQueryCountListener())
                .build();
    }

    @Bean
    public DataSource originalDataSource() {
        var container = TestContainersConfig.postgreSqlContainer;

        return DataSourceBuilder.create()
                .url(container.getJdbcUrl())
                .username(container.getUsername())
                .password(container.getPassword())
                .driverClassName(container.getDriverClassName())
                .build();
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);

        var changeLog = propertyResolver.getProperty("spring.liquibase.change-log", "classpath:db/changelog/db.changelog-master.xml");
        liquibase.setChangeLog(changeLog);

        return liquibase;
    }

    private Properties getJpaTestProperties() {
        var properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", propertyResolver.getProperty("spring.jpa.hibernate.ddl-auto", "validate"));
        properties.put("hibernate.dialect", propertyResolver.getProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"));
        properties.put("hibernate.show_sql", propertyResolver.getProperty("spring.jpa.show-sql", "true"));
        return properties;
    }
}
