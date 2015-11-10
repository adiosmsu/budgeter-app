package ru.adios.budgeter.jdbcrepo;

import java.sql.Connection;

import javax.annotation.concurrent.Immutable;

/**
 * Created by Michail Kulikov
 * 11/3/15
 */
@Immutable
public final class ConnectionHolderUtil {

    public static Connection exposeConnection(JdbcConnectionHolder holder) {
        return holder.connection;
    }

    private ConnectionHolderUtil() {}

}
