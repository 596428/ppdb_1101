package com.kopo.pocket.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Configuration
@MapperScan(basePackages = "com.kopo.pocket.mapper.cards", sqlSessionFactoryRef = "cardsSqlSessionFactory")
public class DynamicCardsDBConfig {
    
    @Value("${db.cards.host}")
    private String dbHost;
    
    @Value("${db.cards.port}")
    private String dbPort;
    
    @Value("${db.cards.username}")
    private String dbUsername;
    
    @Value("${db.cards.password}")
    private String dbPassword;
    
    @Value("${db.cards.driver-class-name}")
    private String driverClassName;

    private String buildJdbcUrl(String dbName) {
        return String.format("jdbc:mariadb://%s:%s/%s", dbHost, dbPort, dbName);
    }

    private DataSource createDataSource(String dbName) {
        return DataSourceBuilder.create()
            .url(buildJdbcUrl(dbName))
            .username(dbUsername)
            .password(dbPassword)
            .driverClassName(driverClassName)
            .build();
    }

    @Primary
    @Bean(name = "cardsDataSource")
    public DataSource dynamicDataSource() {
        AbstractRoutingDataSource dataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                try {
                    ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attr != null) {
                        HttpSession session = attr.getRequest().getSession(false);
                        if (session != null) {
                            Object lang = session.getAttribute("CURRENT_LANGUAGE");
                            if (LanguageConfig.Language.EN.equals(lang)) {
                                return LanguageConfig.Language.EN;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error determining lookup key: " + e.getMessage());
                }
                return LanguageConfig.Language.KR;
            }
        };

        // 한글 DB (즉시 생성)
        DataSource krDataSource = createDataSource("p_pocket_kr");
        
        // 영어 DB (지연 생성)
        LazyConnectionDataSource enDataSource = new LazyConnectionDataSource(
            () -> createDataSource("p_pocket_en")
        );

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put(LanguageConfig.Language.KR, krDataSource);
        dataSources.put(LanguageConfig.Language.EN, enDataSource);

        dataSource.setTargetDataSources(dataSources);
        dataSource.setDefaultTargetDataSource(krDataSource);
        dataSource.afterPropertiesSet();
        
        return dataSource;
    }

    @Primary
    @Bean(name = "cardsSqlSessionFactory")
    public SqlSessionFactory cardsSqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dynamicDataSource());
        sessionFactory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/cards/*.xml"));
        return sessionFactory.getObject();
    }

    @Primary
    @Bean(name = "cardsSqlSessionTemplate")
    public SqlSessionTemplate cardsSqlSessionTemplate(
            SqlSessionFactory cardsSqlSessionFactory) {
        return new SqlSessionTemplate(cardsSqlSessionFactory);
    }
}