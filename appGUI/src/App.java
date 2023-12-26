import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.*;
import oracle.jdbc.driver.*;

public class App {
    private static Connection connection;
    private static Statement statement;
    private static boolean connectedToDatabase = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
    
    /**
	 * @wbp.parser.entryPoint
	 */

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Vivek's Perfect Database  GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel(new GridLayout(5, 1));

        JButton connectButton = new JButton("Connect to Database");
        JButton viewButton = new JButton("View Table Contents");
        JButton searchButton = new JButton("Search by PUBLICATIONID");
        JButton updateButton = new JButton("Update URL by PUBLICATIONID");
        JButton exitButton = new JButton("Exit");

        // Initially, disable buttons that require a database connection
        viewButton.setEnabled(false);
        searchButton.setEnabled(false);
        updateButton.setEnabled(false);

        connectButton.addActionListener(e -> {
            try {
                connectToDatabase();
                connectedToDatabase = true;
                connectButton.setEnabled(false);
                // Enable buttons after successful database connection
                viewButton.setEnabled(true);
                searchButton.setEnabled(true);
                updateButton.setEnabled(true);
            } catch (IOException | SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error connecting to the database.");
            }
        });

        viewButton.addActionListener(e -> viewTableContents());
        searchButton.addActionListener(e -> searchByPublicationID());
        updateButton.addActionListener(e -> updateURLByPublicationID());
        exitButton.addActionListener(e -> closeDatabaseConnection());

        panel.add(connectButton);
        panel.add(viewButton);
        panel.add(searchButton);
        panel.add(updateButton);
        panel.add(exitButton);

        frame.getContentPane().add(BorderLayout.NORTH, panel);
        frame.setVisible(true);
    }

    private static void connectToDatabase() throws IOException, SQLException {
        String driverPrefixURL = "jdbc:oracle:thin:@";
        String jdbcUrl = "artemis.vsnet.gmu.edu:1521/vse18c.vsnet.gmu.edu";

        String username = JOptionPane.showInputDialog("Enter your Oracle username:");
        String password = JOptionPane.showInputDialog("Enter your Oracle password:");

        try {
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            connection = DriverManager.getConnection(driverPrefixURL + jdbcUrl, username, password);
            DatabaseMetaData dbmd = connection.getMetaData();
            statement = connection.createStatement();

            JOptionPane.showMessageDialog(null, "Connected to the database.");

            if (dbmd != null) {
                JOptionPane.showMessageDialog(null,
                        "Database Product Name: " + dbmd.getDatabaseProductName() +
                                "\nDatabase Product Version: " + dbmd.getDatabaseProductVersion() +
                                "\nDatabase Driver Name: " + dbmd.getDriverName() +
                                "\nDatabase Driver Version: " + dbmd.getDriverVersion());
            }

            executeScriptFromFile(); // Execute the SQL script from the file
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Database connection error: " + ex.getMessage());
            throw ex;
        }
    }

    private static void executeScriptFromFile() {
        StringBuilder script = new StringBuilder();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose SQL Script File");
        File file;

        do {
            int userSelection = fileChooser.showOpenDialog(null);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                if (!file.getName().equalsIgnoreCase("paper.sql")) {
                    int option = JOptionPane.showConfirmDialog(null,
                            "Invalid SQL file selected. Do you want to choose a different file?",
                            "Invalid File", JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.NO_OPTION) {
                        JOptionPane.showMessageDialog(null, "Exiting program.");
                        System.exit(0);
                    }
                } else {
                    break;
                }
            } else {
                int option = JOptionPane.showConfirmDialog(null,
                        "No file selected. Do you want to choose a file?",
                        "No File Selected", JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(null, "Exiting program.");
                    System.exit(0);
                }
            }
        } while (true);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    break;
                }

                script.append(line).append("\n");
                if (line.contains(";")) {
                    String sqlStatement = script.toString().replace(";", "").trim();
                    if (!sqlStatement.isEmpty()) {
                        statement.execute(sqlStatement);
                    }
                    script.setLength(0);
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error executing SQL script from file.");
        }
    }

    private static void closeDatabaseConnection() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            JOptionPane.showMessageDialog(null, "Database connection closed. Please close the application.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void viewTableContents() {
        String[] options = {"PUBLICATIONS", "AUTHORS"};
        String tableName = (String) JOptionPane.showInputDialog(null,
                "Select table to view:", "Table Selection", JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        try {
            String query = "SELECT * FROM " + tableName;
            ResultSet resultSet = statement.executeQuery(query);

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            DefaultTableModel tableModel = new DefaultTableModel();
            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(metaData.getColumnName(i));
            }

            while (resultSet.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    rowData[i - 1] = resultSet.getString(i);
                }
                tableModel.addRow(rowData);
            }

            JTable table = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(table);
            JFrame resultFrame = new JFrame(tableName + " Table Contents");
            resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            resultFrame.getContentPane().add(scrollPane);
            resultFrame.setSize(800, 400);
            resultFrame.setVisible(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void searchByPublicationID() {
        try {
            int publicationID = getValidPublicationID();
            if (publicationID == -1) {
                return; // Invalid input, exit method
            }

            String query = "SELECT P.PUBLICATIONID, P.YEAR, P.TYPE, " +
                    "SUBSTR(P.TITLE, 1, 30) AS TITLE, " +
                    "CASE WHEN LENGTH(P.URL) > 20 THEN SUBSTR(P.URL, 1, 20) || '...' ELSE P.URL END AS URL, " +
                    "(SELECT COUNT(A.AUTHOR) FROM AUTHORS A WHERE A.PUBLICATIONID = P.PUBLICATIONID) AS \"No. Of Authors\" " +
                    "FROM PUBLICATIONS P " +
                    "WHERE P.PUBLICATIONID = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, publicationID);
            ResultSet resultSet = preparedStatement.executeQuery();

            displayResultSet(resultSet, "Search Results for PUBLICATIONID: " + publicationID);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static String readURLFromCSV(String csvPath, int publicationID) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(csvPath));
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                int idFromCSV;
                try {
                    idFromCSV = Integer.parseInt(data[0].trim());
                } catch (NumberFormatException e) {
                    continue; // Skip invalid lines
                }

                if (idFromCSV == publicationID) {
                    br.close();
                    return data[1].trim();
                }
            }
            br.close();
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error reading URL from CSV.");
        }
        return null;
    }

    private static void updateURLByPublicationID() {
        try {
            if (!connectedToDatabase) {
                JOptionPane.showMessageDialog(null, "Not connected to the database. Connect first.");
                return;
            }

            int publicationID = getValidPublicationID();
            if (publicationID == -1) {
                return; // Invalid input, exit method
            }

            String csvPath = showFileNavigator("Select the url.csv file");
            if (csvPath == null) {
                JOptionPane.showMessageDialog(null, "No file selected. Operation canceled.");
                return;
            }

            String newURL = readURLFromCSV(csvPath, publicationID);

            if (newURL != null) {
                String updateQuery = "UPDATE PUBLICATIONS SET URL = ? WHERE PUBLICATIONID = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
                preparedStatement.setString(1, newURL);
                preparedStatement.setInt(2, publicationID);
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    displayUpdatedTuple(publicationID);
                } else {
                    JOptionPane.showMessageDialog(null, "Update failed for PUBLICATIONID: " + publicationID);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No URL found for the specified PUBLICATIONID in the CSV file.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void displayUpdatedTuple(int publicationID) {
        try {
            String query = "SELECT * FROM PUBLICATIONS WHERE PUBLICATIONID = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, publicationID);
            ResultSet resultSet = preparedStatement.executeQuery();

            displayResultSet(resultSet, "Updated Tuple for PUBLICATIONID: " + publicationID);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Helper method to display ResultSet in a scrolling window
    private static void displayResultSet(ResultSet resultSet, String title) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        DefaultTableModel tableModel = new DefaultTableModel();
        for (int i = 1; i <= columnCount; i++) {
            tableModel.addColumn(metaData.getColumnName(i));
        }

        while (resultSet.next()) {
            Object[] rowData = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                // Check if the column name is "URL" or "TITLE" and truncate the text if necessary
                if ("URL".equals(metaData.getColumnName(i)) || "TITLE".equals(metaData.getColumnName(i))) {
                    rowData[i - 1] = truncateText(resultSet.getString(i), 30); // You can adjust the truncation length
                } else {
                    rowData[i - 1] = resultSet.getString(i);
                }
            }
            tableModel.addRow(rowData);
        }

        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        JFrame resultFrame = new JFrame(title);
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultFrame.getContentPane().add(scrollPane);
        resultFrame.setSize(800, 400);
        resultFrame.setVisible(true);
    }

    // Helper method to truncate text to a specified length
    private static String truncateText(String text, int length) {
        if (text != null) {
            if (text.length() > length) {
                return text.substring(0, length) + "...";
            } else {
                return text;
            }
        } else {
            return "N/A"; // or any default value you prefer for null text
        }
    }

    // Helper method to get a valid Publication ID from user input
    private static int getValidPublicationID() {
        try {
            int publicationID = Integer.parseInt(JOptionPane.showInputDialog("Enter the Publication ID:"));

            if (publicationID < 1 || publicationID > 91) {
                JOptionPane.showMessageDialog(null, "Invalid Publication ID.");
                return -1;
            }
            return publicationID;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid input. Please enter a valid number.");
            return -1;
        }
    }

    // Helper method to show file navigator dialog
    private static String showFileNavigator(String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);

        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        }

        return null;
    }
}
