package com.kopo.pocket.config;

import org.springframework.jdbc.datasource.AbstractDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class LazyConnectionDataSource extends AbstractDataSource {
    private final AtomicReference<DataSource> targetDataSource = new AtomicReference<>();
    private final Supplier<DataSource> dataSourceSupplier;

    public LazyConnectionDataSource(Supplier<DataSource> dataSourceSupplier) {
        this.dataSourceSupplier = dataSourceSupplier;
    }

    private DataSource getTargetDataSource() {
        DataSource dataSource = targetDataSource.get();
        if (dataSource == null) {
            synchronized (this) {
                dataSource = targetDataSource.get();
                if (dataSource == null) {
                    dataSource = dataSourceSupplier.get();
                    targetDataSource.set(dataSource);
                }
            }
        }
        return dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getTargetDataSource().getConnection(username, password);
    }
}