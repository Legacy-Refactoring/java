// Legacy.java
// Extremely insecure legacy payment system in Java
// Educational bad code example - full of SQL injection, plain text secrets, massive code duplication

import java.sql.*;
import java.io.*;
import java.time.*;
import java.util.*;

public class Legacy {

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "5432";
    private static final String DB_NAME = "payment_legacy_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "SuperSecret123!";
    private static final String SITE_SECRET = "myglobalsecret123";

    private static Connection GLOBAL_CONN = null;

    private static Connection getConnection() throws SQLException {
        if (GLOBAL_CONN == null || GLOBAL_CONN.isClosed()) {
            String url = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
            GLOBAL_CONN = DriverManager.getConnection(url, DB_USER, DB_PASS);
            try (Statement stmt = GLOBAL_CONN.createStatement()) {
                stmt.execute("SET client_encoding = 'UTF8';");
            }
        }
        return GLOBAL_CONN;
    }

    private static void appendToLog(String msg) {
        try (FileWriter fw = new FileWriter("legacy_errors.log", true)) {
            fw.write(LocalDateTime.now() + " | " + msg + "\n");
        } catch (IOException e) {
            // silent fail - classic legacy behavior
        }
    }

    static void register_customer(String username, String email, String password, String full_name,
                                  String phone, String country, String city, String address) {
        try {
            Connection conn = getConnection();
            String id = "cust_" + System.currentTimeMillis();
            String sql = "INSERT INTO customers (" +
                    "id, username, email, password, full_name, phone, country, city, address_line_1, " +
                    "created_at, updated_at, register_ip, user_agent, is_admin, role_name" +
                    ") VALUES (" +
                    "'" + id + "', '" + username + "', '" + email + "', '" + password + "', '" + full_name + "', '" +
                    phone + "', '" + country + "', '" + city + "', '" + address + "', " +
                    "NOW()::text, NOW()::text, '127.0.0.1', 'JAVA-LEGACY', 'false', 'customer'" +
                    ") RETURNING id;";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    System.out.println("Customer registered ID: " + rs.getString("id"));
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void login_customer(String username, String password) {
        try {
            Connection conn = getConnection();
            String sql = "SELECT * FROM customers WHERE username = '" + username +
                         "' AND password = '" + password + "' LIMIT 1;";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String session_token = Long.toString(System.currentTimeMillis());
                    String update = "UPDATE customers SET session_token = '" + session_token +
                                    "', last_login_ip = '127.0.0.1', failed_login_count = '0', " +
                                    "updated_at = NOW()::text WHERE id = '" + id + "';";
                    stmt.executeUpdate(update);
                    System.out.println("LOGIN SUCCESS Session: " + session_token);
                } else {
                    String failSql = "UPDATE customers SET failed_login_count = (failed_login_count::int + 1)::text " +
                                     "WHERE username = '" + username + "';";
                    stmt.executeUpdate(failSql);
                    System.out.println("LOGIN FAILED");
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void get_customer(String customer_id) {
        try {
            Connection conn = getConnection();
            String sql = "SELECT * FROM customers WHERE id = '" + customer_id + "' LIMIT 1;";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    System.out.println("Customer found: " + rs.getString("username"));
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void update_customer_profile(String customer_id, String new_email, String new_phone, String new_address) {
        try {
            Connection conn = getConnection();
            String sql = "UPDATE customers SET email = '" + new_email + "', phone = '" + new_phone +
                         "', address_line_1 = '" + new_address + "', updated_at = NOW()::text WHERE id = '" + customer_id + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Customer profile updated");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void reset_password(String email, String new_password) {
        try {
            Connection conn = getConnection();
            String sql = "UPDATE customers SET password = '" + new_password +
                         "', reset_token = 'reset_' || md5(NOW()::text), " +
                         "reset_token_expires_at = (NOW() + INTERVAL '1 day')::text WHERE email = '" + email + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Password reset token generated for " + email);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void verify_email(String token) {
        try {
            Connection conn = getConnection();
            String sql = "UPDATE customers SET email_verification_token = NULL WHERE email_verification_token = '" + token + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Email verified with token " + token);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void add_payment_method(String customer_id, String type, String card_number,
                                   String expiry_month, String expiry_year, String cvv,
                                   String holder_name, String iban) {
        try {
            Connection conn = getConnection();
            String id = "pm_" + System.currentTimeMillis();
            String sql = "INSERT INTO payment_methods (" +
                         "id, customer_id, type, provider, card_number, card_expiry_month, card_expiry_year, " +
                         "card_cvv, card_holder_name, iban, active_flag, created_at, updated_at" +
                         ") VALUES (" +
                         "'" + id + "', '" + customer_id + "', '" + type + "', 'legacy_bank_gateway', '" +
                         card_number + "', '" + expiry_month + "', '" + expiry_year + "', '" + cvv + "', '" +
                         holder_name + "', '" + iban + "', 'true', NOW()::text, NOW()::text" +
                         ") RETURNING id;";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    System.out.println("Payment method added ID: " + rs.getString("id"));
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void process_payment(String customer_id, String payment_method_id, String amount,
                                String currency, String external_order_id, String ip) {
        try {
            Connection conn = getConnection();
            String id = "pay_" + System.currentTimeMillis();
            String realIp = (ip == null || ip.isEmpty()) ? "127.0.0.1" : ip;
            String extOrder = (external_order_id == null || external_order_id.isEmpty()) ?
                              "ord_" + System.currentTimeMillis() : external_order_id;

            String rawPayload = "{\"card_number\":\"****4242\",\"provider_secret\":\"sk_live_9876543210abcdef\",\"cvv_used\":\"123\",\"3ds_password\":\"customer123\"}";

            String sql = "INSERT INTO payments (" +
                         "id, customer_id, payment_method_id, external_order_id, amount, currency, status, " +
                         "provider_ref, ip_address, raw_provider_payload, created_at, paid_at, captured_flag" +
                         ") VALUES (" +
                         "'" + id + "', '" + customer_id + "', '" + payment_method_id + "', '" + extOrder + "', '" +
                         amount + "', '" + currency + "', 'captured', " +
                         "'prov_" + System.currentTimeMillis() + "', '" + realIp + "', '" + rawPayload + "', " +
                         "NOW()::text, NOW()::text, 'true'" +
                         ") RETURNING id;";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String payId = rs.getString("id");
                    String update = "UPDATE customers SET total_paid = (COALESCE(total_paid::numeric, 0) + " +
                                    amount + ")::text WHERE id = '" + customer_id + "';";
                    stmt.executeUpdate(update);
                    System.out.println("PAYMENT PROCESSED ID: " + payId + " Amount: " + amount + " " + currency);
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void list_payments(String customer_id) {
        try {
            Connection conn = getConnection();
            String sql = "SELECT * FROM payments WHERE customer_id = '" + customer_id + "' ORDER BY created_at DESC;";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) count++;
                System.out.println("Listed " + count + " payments for customer");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void create_refund(String payment_id, String amount, String reason) {
        try {
            Connection conn = getConnection();
            String id = "ref_" + System.currentTimeMillis();
            String sql = "INSERT INTO refunds (id, payment_id, amount, currency, status, reason, created_at) " +
                         "VALUES ('" + id + "', '" + payment_id + "', '" + amount + "', 'EUR', 'pending', '" +
                         reason + "', NOW()::text);";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Refund created for payment " + payment_id);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void process_refund(String refund_id) {
        try {
            Connection conn = getConnection();
            String sql = "UPDATE refunds SET status = 'processed', processed_at = NOW()::text WHERE id = '" + refund_id + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Refund processed ID: " + refund_id);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void simulate_chargeback(String payment_id, String amount, String reason) {
        try {
            Connection conn = getConnection();
            String id = "cb_" + System.currentTimeMillis();
            String sql = "INSERT INTO chargebacks (id, payment_id, amount, currency, reason, status, created_at, deadline_at) " +
                         "VALUES ('" + id + "', '" + payment_id + "', '" + amount + "', 'EUR', '" + reason +
                         "', 'open', NOW()::text, (NOW() + INTERVAL '7 days')::text);";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Chargeback created for payment " + payment_id);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void resolve_chargeback(String chargeback_id, String won) {
        try {
            Connection conn = getConnection();
            String sql = "UPDATE chargebacks SET status = 'closed', won_flag = '" + won +
                         "', closed_at = NOW()::text WHERE id = '" + chargeback_id + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Chargeback resolved ID: " + chargeback_id);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void create_fraud_review(String payment_id, String customer_id, String score) {
        try {
            Connection conn = getConnection();
            String id = "fraud_" + System.currentTimeMillis();
            String sql = "INSERT INTO fraud_reviews (id, payment_id, customer_id, score, decision, created_at) " +
                         "VALUES ('" + id + "', '" + payment_id + "', '" + customer_id + "', '" + score +
                         "', 'pending', NOW()::text);";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Fraud review created for payment " + payment_id);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void decide_fraud_review(String review_id, String decision, String reviewer_email, String reviewer_password) {
        try {
            Connection conn = getConnection();
            String check = "SELECT * FROM customers WHERE email = '" + reviewer_email +
                           "' AND password = '" + reviewer_password + "' AND is_admin = 'true';";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(check)) {
                if (rs.next()) {
                    String sql = "UPDATE fraud_reviews SET decision = '" + decision +
                                 "', reviewer = '" + reviewer_email +
                                 "', updated_at = NOW()::text WHERE id = '" + review_id + "';";
                    stmt.executeUpdate(sql);
                    System.out.println("Fraud review decided as " + decision);
                } else {
                    System.out.println("Fraud review access denied");
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void admin_export_all_data() {
        try {
            Connection conn = getConnection();
            String sql = "COPY (" +
                         "SELECT * FROM customers UNION ALL SELECT * FROM payments UNION ALL SELECT * FROM payment_methods " +
                         "UNION ALL SELECT * FROM refunds UNION ALL SELECT * FROM chargebacks UNION ALL SELECT * FROM fraud_reviews" +
                         ") TO '/tmp/legacy_full_export_" + System.currentTimeMillis() + ".csv' WITH CSV HEADER;";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Full data export completed to /tmp/legacy_full_export_*.csv");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void ban_customer(String customer_id) {
        try {
            Connection conn = getConnection();
            String sql = "UPDATE customers SET blocked_flag = 'true' WHERE id = '" + customer_id + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Customer banned");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    static void generate_api_key(String customer_id) {
        try {
            Connection conn = getConnection();
            String key = "key_" + System.currentTimeMillis();
            String secret = "secret_" + (System.currentTimeMillis() * 2);
            String sql = "UPDATE customers SET api_key = '" + key + "', api_secret = '" + secret +
                         "' WHERE id = '" + customer_id + "';";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("API key generated: " + key);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            appendToLog(e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("LEGACY PAYMENT SYSTEM STARTED (Java version)");

        register_customer("testuser1", "test1@example.com", "PlainPass123", "Test User One",
                          "381601234567", "RS", "Belgrade", "Novi Beograd 1");
        register_customer("testuser2", "test2@example.com", "AnotherPass456", "Test User Two",
                          "381609876543", "RS", "Novi Sad", "Address 2");

        login_customer("testuser1", "PlainPass123");
        login_customer("testuser2", "AnotherPass456");

        add_payment_method("cust_...", "card", "4242424242424242", "12", "2028", "123", "Test User One", "");
        add_payment_method("cust_...", "iban", "", "", "", "", "Test User Two", "RS12345678901234567890");

        process_payment("cust_...", "pm_...", "149.99", "EUR", "ORDER-1001", "");
        process_payment("cust_...", "pm_...", "299.50", "USD", "ORDER-1002", "");

        create_refund("pay_...", "49.99", "partial return");
        process_refund("ref_...");

        simulate_chargeback("pay_...", "299.50", "dispute");
        resolve_chargeback("cb_...", "false");

        create_fraud_review("pay_...", "cust_...", "78");
        decide_fraud_review("fraud_...", "approve", "admin@legacy.com", "AdminPass123");

        reset_password("test1@example.com", "NewPlainPass789");
        verify_email("email_verify_token_demo");

        admin_export_all_data();

        ban_customer("cust_...");
        generate_api_key("cust_...");

        System.out.println("LEGACY PAYMENT SYSTEM WORKFLOW COMPLETE");
    }
}