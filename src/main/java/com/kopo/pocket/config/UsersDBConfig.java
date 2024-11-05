package com.kopo.pocket.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = "com.kopo.pocket.mapper.users", 
            sqlSessionFactoryRef = "usersSqlSessionFactory")
public class UsersDBConfig {
    
    @Value("${db.users.host}")
    private String dbHost;
    
    @Value("${db.users.port}")
    private String dbPort;
    
    @Value("${db.users.username}")
    private String dbUsername;
    
    @Value("${db.users.password}")
    private String dbPassword;
    
    @Value("${db.users.driver-class-name}")
    private String driverClassName;
    
    @Bean(name = "usersDataSource")
    public DataSource usersDataSource() {
        return DataSourceBuilder.create()
            .url(String.format("jdbc:mariadb://%s:%s/p_pocket_user", dbHost, dbPort))
            .username(dbUsername)
            .password(dbPassword)
            .driverClassName(driverClassName)
            .build();
    }
    
    @Bean(name = "usersSqlSessionFactory")
    public SqlSessionFactory usersSqlSessionFactory(
            @Qualifier("usersDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/users/*.xml"));
                
        // MyBatis Configuration
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        sessionFactory.setConfiguration(configuration);
        
        return sessionFactory.getObject();
    }
    
    @Bean(name = "usersSqlSessionTemplate")
    public SqlSessionTemplate usersSqlSessionTemplate(
            @Qualifier("usersSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}