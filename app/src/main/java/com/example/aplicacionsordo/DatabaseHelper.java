package com.example.aplicacionsordo;

import android.os.Handler;
import android.os.Looper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper {

    private static final String HOST = "pg-1fdc8e2b-proyecto-tesis-purizaca.h.aivencloud.com";
    private static final int PORT = 21367;
    private static final String USER = "avnadmin";
    private static final String PASS = "";

    private static final String[] DB_NAMES = {"bdsordo", "dbtesis", "defaultdb"};

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    static {
        // Prevent PostgreSQL driver from trying to access java.lang.management.ManagementFactory on Android
        try {
            System.setProperty("org.postgresql.maxResultBuffer", "0");
        } catch (Throwable ignored) {}
    }

    public interface LoginCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public static void authenticateUser(final String usernameInput, final String rawPassword, final LoginCallback callback) {
        executor.execute(() -> {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String errorMsg = null;
            User authenticatedUser = null;

            try {
                // Load PostgreSQL JDBC Driver
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (Throwable t) {
                    // Driver fallback
                }

                String inputHashedPass = hashSHA256(rawPassword);

                // Setup connection properties for Android
                Properties props = new Properties();
                props.setProperty("user", USER);
                props.setProperty("password", PASS);
                props.setProperty("ssl", "true");
                props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
                props.setProperty("connectTimeout", "10");
                props.setProperty("socketTimeout", "10");

                // Try database connections
                for (String dbName : DB_NAMES) {
                    String url = String.format("jdbc:postgresql://%s:%d/%s", HOST, PORT, dbName);
                    try {
                        conn = DriverManager.getConnection(url, props);
                        if (conn != null) {
                            String query = "SELECT usuario_id, usuario, password_user, nombres, ape_paterno, ape_materno, estado_usuario " +
                                           "FROM public.usuario WHERE LOWER(usuario) = LOWER(?) LIMIT 1;";
                            stmt = conn.prepareStatement(query);
                            stmt.setString(1, usernameInput.trim());
                            rs = stmt.executeQuery();

                            if (rs.next()) {
                                int id = rs.getInt("usuario_id");
                                String dbUser = rs.getString("usuario");
                                String dbHashedPass = rs.getString("password_user");
                                String nombres = rs.getString("nombres");
                                String apePaterno = rs.getString("ape_paterno");
                                String apeMaterno = rs.getString("ape_materno");
                                boolean estado = rs.getBoolean("estado_usuario");

                                if (!estado) {
                                    errorMsg = "El usuario se encuentra inactivo.";
                                } else if (dbHashedPass != null && dbHashedPass.equalsIgnoreCase(inputHashedPass)) {
                                    authenticatedUser = new User(id, dbUser, nombres, apePaterno, apeMaterno, estado);
                                } else {
                                    errorMsg = "Contraseña incorrecta.";
                                }
                            } else {
                                errorMsg = "El usuario '" + usernameInput + "' no existe en la base de datos.";
                            }
                            rs.close();
                            stmt.close();
                            conn.close();
                            
                            // If we connected to the active database successfully, exit loop
                            if (authenticatedUser != null || errorMsg != null) {
                                break;
                            }
                        }
                    } catch (Throwable ex) {
                        errorMsg = "Error al conectar con PostgreSQL: " + ex.getLocalizedMessage();
                    }
                }

            } catch (Throwable t) {
                errorMsg = "Error inesperado de conexión: " + t.getLocalizedMessage();
            } finally {
                try { if (rs != null && !rs.isClosed()) rs.close(); } catch (Throwable ignored) {}
                try { if (stmt != null && !stmt.isClosed()) stmt.close(); } catch (Throwable ignored) {}
                try { if (conn != null && !conn.isClosed()) conn.close(); } catch (Throwable ignored) {}
            }

            final User finalUser = authenticatedUser;
            final String finalError = errorMsg;

            mainHandler.post(() -> {
                if (finalUser != null) {
                    callback.onSuccess(finalUser);
                } else {
                    callback.onError(finalError != null ? finalError : "Error de autenticación.");
                }
            });
        });
    }

    public static String hashSHA256(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}
