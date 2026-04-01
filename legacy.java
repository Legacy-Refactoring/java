class Legacy {
    static void register_customer(String username, String email, String password, String full_name, String phone, String country, String city, String address) {}
    static void login_customer(String username, String password) {}
    static void get_customer(String customer_id) {}
    static void update_customer_profile(String customer_id, String new_email, String new_phone, String new_address) {}
    static void reset_password(String email, String new_password) {}
    static void verify_email(String token) {}
    static void add_payment_method(String customer_id, String type, String card_number, String expiry_month, String expiry_year, String cvv, String holder_name, String iban) {}
    static void list_payment_methods(String customer_id) {}
    static void delete_payment_method(String pm_id) {}
    static void process_payment(String customer_id, String payment_method_id, String amount, String currency, String external_order_id, String ip) {}
    static void list_payments(String customer_id) {}
    static void get_payment_details(String payment_id) {}
    static void create_refund(String payment_id, String amount, String reason) {}
    static void process_refund(String refund_id) {}
    static void simulate_chargeback(String payment_id, String amount, String reason) {}
    static void resolve_chargeback(String chargeback_id, String won) {}
    static void create_fraud_review(String payment_id, String customer_id, String score) {}
    static void decide_fraud_review(String review_id, String decision, String reviewer_email, String reviewer_password) {}
    static void admin_list_all_customers() {}
    static void admin_export_all_data() {}
    static void search_payments(String search_term) {}
    static void process_recurring_billing() {}
    static void handle_webhook(String payload) {}
    static void ban_customer(String customer_id) {}
    static void generate_api_key(String customer_id) {}
}
