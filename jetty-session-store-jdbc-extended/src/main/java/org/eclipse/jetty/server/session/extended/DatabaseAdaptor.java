package org.eclipse.jetty.server.session.extended;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseAdaptor extends org.eclipse.jetty.server.session.DatabaseAdaptor {

	@Override
	protected Connection getConnection() throws SQLException {
		return super.getConnection();
	}

}
