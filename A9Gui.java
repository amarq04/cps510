import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class A9Gui extends JFrame {

    private Connection conn;
    private JTextArea outputArea;

    private JTextField custIdField;
    private JTextField custNameField;
    private JTextField custEmailField;
    private JTextField custPhoneField;

    private final String username;
    private final String password;

    //takes usernamme and password
    public A9Gui(String username, String password) {
        super("CPS510 - Assignment 9 GUI");
        this.username = username;
        this.password = password;

        initComponents();
        connectToDatabase();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connectToDatabase() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");

  //oracle connection
            String url = "jdbc:oracle:thin:@localhost:1522/xe";

            conn = DriverManager.getConnection(url, username, password);
            conn.setAutoCommit(false);

            log("Connected to Oracle as " + username + ".\n");

        } catch (Exception e) {
            showError("Login / connection failed: " + e.getMessage());
        }
    }

    private void initComponents() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton dropButton     = new JButton("Drop Tables");
        JButton createButton   = new JButton("Create Tables");
        JButton populateButton = new JButton("Populate Tables");
        JButton queryButton    = new JButton("Query Tables");
        JButton exitButton     = new JButton("Exit");

        buttonPanel.add(dropButton);
        buttonPanel.add(createButton);
        buttonPanel.add(populateButton);
        buttonPanel.add(queryButton);
        buttonPanel.add(exitButton);

        JPanel customerPanel = new JPanel(new GridLayout(3, 4, 5, 5));
        customerPanel.setBorder(
                BorderFactory.createTitledBorder("Customer Search / Update / Delete"));

        customerPanel.add(new JLabel("Customer ID:"));
        customerPanel.add(new JLabel("Name:"));
        customerPanel.add(new JLabel("Email:"));
        customerPanel.add(new JLabel("Phone:"));

        custIdField    = new JTextField();
        custNameField  = new JTextField();
        custEmailField = new JTextField();
        custPhoneField = new JTextField();

        customerPanel.add(custIdField);
        customerPanel.add(custNameField);
        customerPanel.add(custEmailField);
        customerPanel.add(custPhoneField);

        JButton searchCustomerButton = new JButton("Search Customer");
        JButton updateCustomerButton = new JButton("Update Customer");
        JButton deleteCustomerButton = new JButton("Delete Customer");

        customerPanel.add(searchCustomerButton);
        customerPanel.add(updateCustomerButton);
        customerPanel.add(deleteCustomerButton);
        customerPanel.add(new JLabel("")); 

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buttonPanel, BorderLayout.NORTH);
        topPanel.add(customerPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        dropButton.addActionListener(this::handleDrop);
        createButton.addActionListener(this::handleCreate);
        populateButton.addActionListener(this::handlePopulate);
        queryButton.addActionListener(this::handleQuery);
        exitButton.addActionListener(this::handleExit);

        searchCustomerButton.addActionListener(this::handleSearchCustomer);
        updateCustomerButton.addActionListener(this::handleUpdateCustomer);
        deleteCustomerButton.addActionListener(this::handleDeleteCustomer);
    }

   //drop tables
    private void handleDrop(ActionEvent e) {
        if (!ensureConnection()) return;

        log("\n=== Dropping views and tables (restaurant schema) ===\n");

        try (Statement stmt = conn.createStatement()) {
            String[] dropViews = {
                    "DROP VIEW online_order_view",
                    "DROP VIEW reservation_detail_view",
                    "DROP VIEW menu_summary_view"
            };
            for (String v : dropViews) {
                try {
                    stmt.executeUpdate(v);
                    log("Executed: " + v + "\n");
                } catch (SQLException ex) {
                    log("VIEW DROP FAILED (" + v + "): " + ex.getMessage() + "\n");
                }
            }

            String[] dropTables = {
                    "DROP TABLE online_order CASCADE CONSTRAINTS",
                    "DROP TABLE reservations CASCADE CONSTRAINTS",
                    "DROP TABLE payment CASCADE CONSTRAINTS",
                    "DROP TABLE bill CASCADE CONSTRAINTS",
                    "DROP TABLE seating CASCADE CONSTRAINTS",
                    "DROP TABLE menu CASCADE CONSTRAINTS",
                    "DROP TABLE customer CASCADE CONSTRAINTS"
            };
            for (String t : dropTables) {
                try {
                    stmt.executeUpdate(t);
                    log("Executed: " + t + "\n");
                } catch (SQLException ex) {
                    log("TABLE DROP FAILED (" + t + "): " + ex.getMessage() + "\n");
                }
            }

            conn.commit();
            log("Drop completed.\n");

        } catch (SQLException ex) {
            showError("Error dropping tables/views:\n" + ex.getMessage());
            rollbackQuietly();
        }
    }

//create tables
    private void handleCreate(ActionEvent e) {
        if (!ensureConnection()) return;

        log("\n=== Creating tables and views (restaurant schema) ===\n");

        try (Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "CREATE TABLE customer (" +
                            "  customer_name  VARCHAR2(20) NOT NULL, " +
                            "  customer_email VARCHAR2(20) NOT NULL, " +
                            "  customer_phone VARCHAR2(12) NOT NULL, " +
                            "  customer_id    INT NOT NULL, " +
                            "  CONSTRAINT customer_pk PRIMARY KEY (customer_id)" +
                            ")"
            );
            log("Created CUSTOMER.\n");

            stmt.executeUpdate(
                    "CREATE TABLE menu (" +
                            "  dish_id     INT NOT NULL, " +
                            "  dish_name   VARCHAR2(20) NOT NULL, " +
                            "  price       NUMBER(7,2) NOT NULL CHECK (price >= 0), " +
                            "  quantity    NUMBER NOT NULL CHECK (quantity > 0), " +
                            "  category_id VARCHAR2(20) NOT NULL " +
                            "     CHECK (category_id IN ('APPETIZER','MAIN','DESSERT','DRINK')), " +
                            "  CONSTRAINT dishes_pk PRIMARY KEY (dish_id)" +
                            ")"
            );
            log("Created MENU.\n");

            stmt.executeUpdate(
                    "CREATE TABLE seating (" +
                            "  table_id           INT NOT NULL, " +
                            "  table_capacity     NUMBER(2) NOT NULL CHECK (table_capacity BETWEEN 1 AND 12), " +
                            "  table_availability NUMBER(1) NOT NULL CHECK (table_availability IN (0,1)), " +
                            "  CONSTRAINT seating_pk PRIMARY KEY (table_id)" +
                            ")"
            );
            log("Created SEATING.\n");

            stmt.executeUpdate(
                    "CREATE TABLE bill (" +
                            "  bill_id     INT NOT NULL, " +
                            "  subtotal    NUMBER(10,2) NOT NULL, " +
                            "  tax         NUMBER(10,2) NOT NULL, " +
                            "  tip         NUMBER(10,2) DEFAULT 0 NOT NULL, " +
                            "  split_count NUMBER(2) DEFAULT 1 NOT NULL, " +
                            "  grand_total NUMBER(10,2), " +
                            "  CONSTRAINT bill_pk PRIMARY KEY (bill_id)" +
                            ")"
            );
            log("Created BILL.\n");

            stmt.executeUpdate(
                    "CREATE TABLE payment (" +
                            "  payment_id     INT NOT NULL, " +
                            "  bill_id        INT NOT NULL, " +
                            "  payment_method VARCHAR2(5) NOT NULL " +
                            "     CHECK (payment_method IN ('CARD','CASH')), " +
                            "  amount         NUMBER(10,2) NOT NULL, " +
                            "  CONSTRAINT payment_pk PRIMARY KEY (payment_id), " +
                            "  CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id) REFERENCES bill(bill_id)" +
                            ")"
            );
            log("Created PAYMENT.\n");

            stmt.executeUpdate(
                    "CREATE TABLE reservations (" +
                            "  reservation_id INT NOT NULL, " +
                            "  customer_id    INT NOT NULL, " +
                            "  bill_id        INT, " +
                            "  date_id        VARCHAR2(20), " +
                            "  time_id        VARCHAR2(20), " +
                            "  party_size     INT NOT NULL, " +
                            "  table_id       INT NOT NULL, " +
                            "  CONSTRAINT reservations_pk PRIMARY KEY (reservation_id), " +
                            "  CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id), " +
                            "  CONSTRAINT fk_reservations_bill FOREIGN KEY (bill_id) REFERENCES bill(bill_id), " +
                            "  CONSTRAINT fk_seating FOREIGN KEY (table_id) REFERENCES seating(table_id)" +
                            ")"
            );
            log("Created RESERVATIONS.\n");

            stmt.executeUpdate(
                    "CREATE TABLE online_order (" +
                            "  online_order_id INT NOT NULL, " +
                            "  customer_id     INT NOT NULL, " +
                            "  bill_id         INT, " +
                            "  pickup_time     VARCHAR2(20), " +
                            "  order_status    NUMBER(1) NOT NULL CHECK (order_status IN (0,1)), " +
                            "  dish_id         INT NOT NULL REFERENCES menu(dish_id), " +
                            "  CONSTRAINT online_order_pk PRIMARY KEY (online_order_id), " +
                            "  CONSTRAINT fk_online_order_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id), " +
                            "  CONSTRAINT fk_online_order_bill FOREIGN KEY (bill_id) REFERENCES bill(bill_id)" +
                            ")"
            );
            log("Created ONLINE_ORDER.\n");


            //views
            stmt.executeUpdate(
                    "CREATE OR REPLACE VIEW menu_summary_view AS " +
                            "SELECT category_id, COUNT(*) AS num_dishes " +
                            "FROM menu " +
                            "GROUP BY category_id"
            );
            log("Created MENU_SUMMARY_VIEW.\n");

            stmt.executeUpdate(
                    "CREATE OR REPLACE VIEW reservation_detail_view AS " +
                            "SELECT " +
                            "    r.reservation_id, " +
                            "    c.customer_name, " +
                            "    r.date_id, " +
                            "    r.time_id, " +
                            "    r.party_size, " +
                            "    s.table_capacity, " +
                            "    s.table_availability " +
                            "FROM reservations r " +
                            "JOIN customer c ON r.customer_id = c.customer_id " +
                            "JOIN seating s ON r.table_id = s.table_id"
            );
            log("Created RESERVATION_DETAIL_VIEW.\n");

            stmt.executeUpdate(
                    "CREATE OR REPLACE VIEW online_order_view AS " +
                            "SELECT " +
                            "    o.online_order_id, " +
                            "    c.customer_name, " +
                            "    m.dish_name, " +
                            "    o.pickup_time, " +
                            "    CASE " +
                            "        WHEN o.order_status = 0 THEN 'PENDING' " +
                            "        WHEN o.order_status = 1 THEN 'READY' " +
                            "    END AS order_status " +
                            "FROM online_order o " +
                            "JOIN customer c ON o.customer_id = c.customer_id " +
                            "JOIN menu m ON o.dish_id = m.dish_id"
            );
            log("Created ONLINE_ORDER_VIEW.\n");

            conn.commit();
            log("All tables and views created successfully.\n");

        } catch (SQLException ex) {
            showError("Error creating tables/views:\n" + ex.getMessage());
            rollbackQuietly();
        }
    }

    //populate tables
    private void handlePopulate(ActionEvent e) {
        if (!ensureConnection()) return;

        log("\n=== Populating tables with sample data ===\n");

        try (Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("INSERT INTO customer VALUES ('Varen', 'varen@email.com', '4192946250', 1)");
            stmt.executeUpdate("INSERT INTO customer VALUES ('NJ', 'nithieshan@email.com', '4167282882', 2)");
            stmt.executeUpdate("INSERT INTO customer VALUES ('Alwin', 'alwin@email.com', '1234567890', 3)");

            stmt.executeUpdate("INSERT INTO menu VALUES (1, 'Burger', 11.99, 10, 'MAIN')");
            stmt.executeUpdate("INSERT INTO menu VALUES (2, 'Fries', 4.99, 20, 'APPETIZER')");
            stmt.executeUpdate("INSERT INTO menu VALUES (3, 'Cake', 5.99, 5, 'DESSERT')");
            stmt.executeUpdate("INSERT INTO menu VALUES (4, 'Soda', 2.50, 15, 'DRINK')");

            stmt.executeUpdate("INSERT INTO seating VALUES (1, 2, 1)");
            stmt.executeUpdate("INSERT INTO seating VALUES (2, 4, 1)");
            stmt.executeUpdate("INSERT INTO seating VALUES (3, 6, 0)");

            stmt.executeUpdate("INSERT INTO bill VALUES (1, 45.50, 5.92, 7.00, 2, 67.60)");
            stmt.executeUpdate("INSERT INTO bill VALUES (2, 22.00, 2.86, 3.00, 1, 76.76)");
            stmt.executeUpdate("INSERT INTO bill VALUES (3, 78.25, 10.17, 8.00, 3, 10.04)");

            stmt.executeUpdate("INSERT INTO payment VALUES (1, 1, 'CARD', 67.60)");
            stmt.executeUpdate("INSERT INTO payment VALUES (2, 2, 'CARD', 76.76)");
            stmt.executeUpdate("INSERT INTO payment VALUES (3, 3, 'CARD', 10.04)");

            stmt.executeUpdate("INSERT INTO reservations VALUES (1, 1, 1, '2025-09-15', '18:30', 2, 1)");
            stmt.executeUpdate("INSERT INTO reservations VALUES (2, 2, 2, '2025-09-15', '20:00', 4, 2)");
            stmt.executeUpdate("INSERT INTO reservations VALUES (3, 3, 3, '2025-09-16', '19:00', 3, 3)");

            stmt.executeUpdate("INSERT INTO online_order VALUES (1, 1, 1, '8:45', 1, 1)");
            stmt.executeUpdate("INSERT INTO online_order VALUES (2, 2, 2, '9:15', 0, 2)");
            stmt.executeUpdate("INSERT INTO online_order VALUES (3, 3, 3, '10:00', 1, 3)");

            stmt.executeUpdate("INSERT INTO customer VALUES ('Dave', 'Dave@email.com', '9050000000', 4)");
            stmt.executeUpdate("INSERT INTO reservations VALUES (4, 4, 1, '2025-09-17', '18:00', 2, 1)");

            conn.commit();
            log("Sample data inserted successfully.\n");

        } catch (SQLException ex) {
            showError("Error populating tables:\n" + ex.getMessage());
            rollbackQuietly();
        }
    }

//our query and views
    private void handleQuery(ActionEvent e) {
        if (!ensureConnection()) return;

        log("\n=== Querying views ===\n");

        runAndPrintQuery("MENU_SUMMARY_VIEW",
                "SELECT * FROM menu_summary_view");

        runAndPrintQuery("RESERVATION_DETAIL_VIEW",
                "SELECT * FROM reservation_detail_view");

        runAndPrintQuery("ONLINE_ORDER_VIEW",
                "SELECT * FROM online_order_view");
    }

    //print all the queries
    private void runAndPrintQuery(String title, String sql) {
        log("\n--- " + title + " ---\n");
        log(sql + "\n");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            StringBuilder formatBuilder = new StringBuilder();
            for (int i = 0; i < columnCount; i++) {
                formatBuilder.append("%-20s");
            }
            formatBuilder.append("\n");
            String format = formatBuilder.toString();

            Object[] headers = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                headers[i - 1] = meta.getColumnName(i);
            }
            outputArea.append(String.format(format, headers));

            outputArea.append("-".repeat(20 * columnCount) + "\n");

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    String val = rs.getString(i);
                    row[i - 1] = (val == null ? "NULL" : val);
                }
                outputArea.append(String.format(format, row));
            }

        } catch (SQLException ex) {
            showError("Error running query (" + title + "):\n" + ex.getMessage());
        }
    }

    //searching customers
    private void handleSearchCustomer(ActionEvent e) {
        if (!ensureConnection()) return;

        String idText = custIdField.getText().trim();
        if (idText.isEmpty()) {
            showError("Please enter a Customer ID to search.");
            return;
        }

        try {
            int id = Integer.parseInt(idText);

            String sql = "SELECT customer_name, customer_email, customer_phone " +
                         "FROM customer WHERE customer_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String name  = rs.getString("customer_name");
                        String email = rs.getString("customer_email");
                        String phone = rs.getString("customer_phone");

                        custNameField.setText(name);
                        custEmailField.setText(email);
                        custPhoneField.setText(phone);

                        log("\nFound customer:\n");
                        log("ID: " + id + ", Name: " + name +
                            ", Email: " + email + ", Phone: " + phone + "\n");
                    } else {
                        log("\nNo customer found with ID " + id + ".\n");
                    }
                }
            }
        } catch (NumberFormatException ex) {
            showError("Customer ID must be a number.");
        } catch (SQLException ex) {
            showError("Error searching customer:\n" + ex.getMessage());
        }
    }
//updates customer
    private void handleUpdateCustomer(ActionEvent e) {
        if (!ensureConnection()) return;

        String idText = custIdField.getText().trim();
        String name   = custNameField.getText().trim();
        String email  = custEmailField.getText().trim();
        String phone  = custPhoneField.getText().trim();

        if (idText.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showError("Please fill ID, Name, Email, and Phone to update.");
            return;
        }

        try {
            int id = Integer.parseInt(idText);

            String sql = "UPDATE customer " +
                         "SET customer_name = ?, customer_email = ?, customer_phone = ? " +
                         "WHERE customer_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setInt(4, id);

                int rows = ps.executeUpdate();
                conn.commit();

                if (rows > 0) {
                    log("\nUpdated customer ID " + id + " successfully.\n");
                } else {
                    log("\nNo customer updated. Check if ID " + id + " exists.\n");
                }
            }
        } catch (NumberFormatException ex) {
            showError("Customer ID must be a number.");
        } catch (SQLException ex) {
            showError("Error updating customer:\n" + ex.getMessage());
            rollbackQuietly();
        }
    }


    //delete customer
    private void handleDeleteCustomer(ActionEvent e) {
        if (!ensureConnection()) return;

        String idText = custIdField.getText().trim();
        if (idText.isEmpty()) {
            showError("Please enter a Customer ID to delete.");
            return;
        }

        try {
            int id = Integer.parseInt(idText);

            String sql = "DELETE FROM customer WHERE customer_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);

                int rows = ps.executeUpdate();
                conn.commit();

                if (rows > 0) {
                    log("\nDeleted customer ID " + id + " successfully.\n");
                    custNameField.setText("");
                    custEmailField.setText("");
                    custPhoneField.setText("");
                } else {
                    log("\nNo customer deleted. Check if ID " + id + " exists.\n");
                }
            }
        } catch (NumberFormatException ex) {
            showError("Customer ID must be a number.");
        } catch (SQLException ex) {
            showError("Error deleting customer:\n" + ex.getMessage());
            rollbackQuietly();
        }
    }

//exit the application
    private void handleExit(ActionEvent e) {
        log("\nExiting application...\n");
        closeConnection();
        dispose();
        System.exit(0);
    }

    private boolean ensureConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                showError("Not connected to database.");
                return false;
            }
        } catch (SQLException e) {
            showError("Connection check failed: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void rollbackQuietly() {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (SQLException ignored) {}
    }

    private void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                log("Connection closed.\n");
            }
        } catch (SQLException e) {
            showError("Error closing connection:\n" + e.getMessage());
        }
    }

    private void log(String msg) {
        outputArea.append(msg);
    }

    private void showError(String msg) {
        outputArea.append("ERROR: " + msg + "\n");
        System.err.println(msg);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginWindow());
    }
}

//our login window

class LoginWindow extends JFrame {

    private final JTextField userField;
    private final JPasswordField passField;

    public LoginWindow() {
        super("Oracle Login");

        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("Username:"));
        userField = new JTextField();
        add(userField);

        add(new JLabel("Password:"));
        passField = new JPasswordField();
        add(passField);

        JButton loginButton = new JButton("Login");
        add(new JLabel(""));
        add(loginButton);

        loginButton.addActionListener(e -> handleLogin());

        setSize(300, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void handleLogin() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            return;
        }

        new A9Gui(user, pass);
        dispose();
    }
}
