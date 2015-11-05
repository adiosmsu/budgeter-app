package ru.adios.budgeter;

import android.os.Environment;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;

import java8.util.function.Supplier;
import ru.adios.budgeter.jdbcrepo.JdbcConnectionHolder;
import ru.adios.budgeter.jdbcrepo.JdbcTransactionalSupport;
import ru.adios.budgeter.jdbcrepo.SourcingBundle;

/**
 * Created by Michail Kulikov
 * 11/3/15
 */
@ThreadSafe
public final class BundleProvider {

    public static SourcingBundle getBundle() {
        if (dataSource == null) {
            initDataSource(getDefaultUrl());
        }
        return BundleContainer.BUNDLE;
    }

    public static volatile String DEFAULT_URL = "jdbc:sqlite:" + Environment.getDataDirectory() + "/data/ru.adios.budgeter/databases/budget.db";

    public static void setNewDatabase(String url) {
        synchronized (BundleProvider.class) {
            final SingleConnectionDataSource ds = createDataSource(url);
            BundleContainer.BUNDLE.setNewDataSource(ds, new SpringTransactionalSupport(ds));
            try {
                dataSource.destroy();
            } finally {
                dataSource = ds;
            }
        }
    }

    private static String getDefaultUrl() {
        final String path = Environment.getDataDirectory() + "/data/ru.adios.budgeter/databases";
        final File dbDir = new File(path);
        if (!dbDir.exists()) {
            final boolean mk = dbDir.mkdir();
            if (!mk) {
                throw new IllegalStateException("Unable to create " + path);
            }
        }
        return DEFAULT_URL;
    }

    private static SingleConnectionDataSource createDataSource(String url) {
        final SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, true) {
            @Override
            protected Connection getCloseSuppressingConnectionProxy(Connection target) {
                return new DelegatingConnectionProxy(target);
            }
        };
        dataSource.setAutoCommit(true);
        dataSource.setDriverClassName("org.sqldroid.SQLDroidDriver");
        try {
            dataSource.initConnection();
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Unable to initialize SingleConnectionDataSource", ex);
        }
        return dataSource;
    }

    private static void initDataSource(String url) {
        synchronized (BundleProvider.class) {
            if (dataSource == null) {
                dataSource = createDataSource(url);
            }
        }
    }

    private static volatile SingleConnectionDataSource dataSource;

    @Immutable
    private static final class BundleContainer {

        private static final SourcingBundle BUNDLE;

        static {
            final DataSource ds = dataSource;
            final SourcingBundle sourcingBundle = new SourcingBundle(ds, new SpringTransactionalSupport(ds));
            sourcingBundle.createSchemaIfNeeded();
            BUNDLE = sourcingBundle;
        }

    }

    @ThreadSafe
    private static final class SpringTransactionalSupport implements JdbcTransactionalSupport {

        private final TransactionTemplate txTemplate;

        private SpringTransactionalSupport(DataSource ds) {
            txTemplate = new TransactionTemplate(new DataSourceTransactionManager(ds));
        }

        @Override
        public JdbcConnectionHolder getConnection(DataSource ds) {
            return JdbcTransactionalSupport.Static.getConnection(ds);
        }

        @Override
        public void releaseConnection(JdbcConnectionHolder con, DataSource dataSource) {
            JdbcTransactionalSupport.Static.releaseConnection(con, dataSource);
        }

        @Override
        public void runWithTransaction(final Runnable runnable) {
            synchronized (BundleProvider.class) {
                txTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        runnable.run();
                    }
                });
            }
        }

        @Override
        public <T> T getWithTransaction(final Supplier<T> supplier) {
            synchronized (BundleProvider.class) {
                return txTemplate.execute(new TransactionCallback<T>() {
                    @Override
                    public T doInTransaction(TransactionStatus status) {
                        return supplier.get();
                    }
                });
            }
        }

    }

    @Immutable
    private static final class DelegatingConnectionProxy implements ConnectionProxy {

        private final Connection delegate;

        private DelegatingConnectionProxy(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getTargetConnection() {
            return delegate;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException("isWrapperFor");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException("unwrap");
        }

        @Override
        public void close() throws SQLException {} // Handle close method: don't pass the call on.

        @Override
        public boolean isClosed() throws SQLException {
            return false;
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return false; // SQLDroidConnection spams to error stream otherwise.
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return delegate.prepareStatement(sql); // Work around idiotic SQLDroid behavior with returning null for prepareStatement(String, int).
        }

        @Override
        public void clearWarnings() throws SQLException {
            delegate.clearWarnings();
        }

        @Override
        public void commit() throws SQLException {
            delegate.commit();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return delegate.createArrayOf(typeName, elements);
        }

        @Override
        public Blob createBlob() throws SQLException {
            return delegate.createBlob();
        }

        @Override
        public Clob createClob() throws SQLException {
            return delegate.createClob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return delegate.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return delegate.createSQLXML();
        }

        @Override
        public Statement createStatement() throws SQLException {
            return delegate.createStatement();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return delegate.createStruct(typeName, attributes);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return delegate.getAutoCommit();
        }

        @Override
        public String getCatalog() throws SQLException {
            return delegate.getCatalog();
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return delegate.getClientInfo();
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return delegate.getClientInfo(name);
        }

        @Override
        public int getHoldability() throws SQLException {
            return delegate.getHoldability();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return delegate.getMetaData();
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return delegate.getTransactionIsolation();
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return delegate.getTypeMap();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return delegate.getWarnings();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return delegate.isValid(timeout);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return delegate.nativeSQL(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return delegate.prepareCall(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return delegate.prepareStatement(sql);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return delegate.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return delegate.prepareStatement(sql, columnNames);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            delegate.releaseSavepoint(savepoint);
        }

        @Override
        public void rollback() throws SQLException {
            delegate.rollback();
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            delegate.rollback(savepoint);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            delegate.setAutoCommit(autoCommit);
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            delegate.setCatalog(catalog);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            delegate.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            delegate.setClientInfo(properties);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            delegate.setHoldability(holdability);
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            delegate.setReadOnly(readOnly);
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return delegate.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return delegate.setSavepoint(name);
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            delegate.setTransactionIsolation(level);
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            delegate.setTypeMap(map);
        }

    }

    private BundleProvider() {}

}
