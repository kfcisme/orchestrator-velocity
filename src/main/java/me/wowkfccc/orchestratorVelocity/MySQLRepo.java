package me.wowkfccc.orchestratorVelocity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.wowkfccc.Ports;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLRepo implements Ports.MetricsRepo {
    private final VelocityConfig C;
    private final HikariDataSource ds;

    public MySQLRepo(VelocityConfig C) {
        this.C = C;

        // 初始化 HikariCP
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(C.jdbcUrl);
        cfg.setUsername(C.jdbcUser);
        cfg.setPassword(C.jdbcPass);
        cfg.setMaximumPoolSize(C.poolSize);
        cfg.setMinimumIdle(1);
        cfg.setPoolName("MLP-Orchestrator-Velocity");
        ds = new HikariDataSource(cfg);

        // 啟動時檢查 / 創建表格
        ensureTables();
    }

    private void ensureTables() {
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // comp 表
            if (!tableExists(meta, C.tblComp)) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("CREATE TABLE `" + C.tblComp + "` (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "server_id VARCHAR(64)," +
                            "ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "total INT," +
                            "AFK DOUBLE, Build DOUBLE, Explorer DOUBLE, Explosive DOUBLE," +
                            "PvP DOUBLE, Redstone DOUBLE, Social DOUBLE, Survival DOUBLE" +
                            ")");
                }
            }

            // load 表
            if (!tableExists(meta, C.tblLoad)) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("CREATE TABLE `" + C.tblLoad + "` (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "server_id VARCHAR(64)," +
                            "ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "cpu DOUBLE," +
                            "ram_mb DOUBLE" +
                            ")");
                }
            }

            // pred 表
            if (!tableExists(meta, C.tblPred)) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("CREATE TABLE `" + C.tblPred + "` (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "player_id VARCHAR(64)," +
                            "ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "pred_type VARCHAR(32)," +
                            "confidence DOUBLE" +
                            ")");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean tableExists(DatabaseMetaData meta, String table) throws SQLException {
        try (ResultSet rs = meta.getTables(null, null, table, null)) {
            return rs.next();
        }
    }

    // === MetricsRepo 實作 ===
    @Override
    public List<CompPoint> recentComp(int horizon) {
        List<CompPoint> list = new ArrayList<>();
        String sql = "SELECT * FROM " + C.tblComp + " ORDER BY ts DESC LIMIT ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, horizon);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double[] p = new double[]{
                            rs.getDouble("AFK"), rs.getDouble("Build"),
                            rs.getDouble("Explorer"), rs.getDouble("Explosive"),
                            rs.getDouble("PvP"), rs.getDouble("Redstone"),
                            rs.getDouble("Social"), rs.getDouble("Survival")
                    };
                    list.add(new CompPoint(
                            rs.getString("server_id"),
                            rs.getInt("total"), p
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public double latestCpu(String serverId) {
        String sql = "SELECT cpu FROM " + C.tblLoad + " WHERE server_id=? ORDER BY ts DESC LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    @Override
    public double latestRamMb(String serverId) {
        String sql = "SELECT ram_mb FROM " + C.tblLoad + " WHERE server_id=? ORDER BY ts DESC LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }
}

