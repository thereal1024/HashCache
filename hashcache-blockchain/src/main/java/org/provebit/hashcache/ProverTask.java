package org.provebit.hashcache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.sql.*;

import org.apache.commons.codec.binary.Hex;

public class ProverTask {

	final Runnable taskRunner;
	private ScheduledFuture<?> periodicCheck;
	/** Seconds between site requests */
	final static private int SECONDS_INT = 10;

	public ProverTask() {
		ScheduledExecutorService scheduler = Executors
				.newScheduledThreadPool(1);
		taskRunner = new Runnable() {
			@Override
			public void run() {
				windowopen();
			}
		};

		periodicCheck = scheduler.scheduleWithFixedDelay(taskRunner, 0,
				SECONDS_INT, TimeUnit.SECONDS);
	}

	public void end() {
		periodicCheck.cancel(true);
	}

	private void windowopen() {
		Connection conn = null;
		try {
			conn = DBConnector.getConnection();
			String sql = "select * from NodeHash";
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int id = rs.getInt(1);
				byte[] hash = rs.getBytes(2);
				int winid = rs.getInt(3);
				int level = rs.getInt(4);
				System.out.printf("node %d in win %d, lv %d : %s\n",
						id, winid, level, Hex.encodeHexString(hash));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// finally block used to close resources
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}
	}

}
