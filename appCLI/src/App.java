import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import oracle.jdbc.driver.*;

public class App {
    static Connection con;
    static Statement stmt;

    public static void main(String[] argv) {       
        Scanner scanner = new Scanner(System.in);
        int conny = 0;
        while(conny ==0){if (connectToDatabase(scanner)) {
            conny = 1;
            boolean exit = false;            
            while (!exit) {
                int choice = displayMainMenu(scanner);
                try {switch (choice) {
                    case 1:
                        viewTableContents(scanner);
                        break;
                    case 2:
                        searchByPublicationID();
                        break;
                    case 3:
                        updateURLByPublicationID(scanner);
                        break;
                    case 4:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                        break;
                    }} catch (InputMismatchException e) {
                        System.out.println("Invalid option. Please try again.");
                        scanner.nextLine();
                    }
                }       
            closeDatabaseConnection();
        } else {
            System.out.println("Failed to connect to the database.");
        }}
    }

    public static boolean connectToDatabase(Scanner scanner) {
        String driverPrefixURL = "jdbc:oracle:thin:@";
        String jdbc_url = "artemis.vsnet.gmu.edu:1521/vse18c.vsnet.gmu.edu";

        System.out.print("Enter your Oracle username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your Oracle password: ");
        String password = scanner.nextLine();
        try {
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            System.out.println(driverPrefixURL + jdbc_url);
            con = DriverManager.getConnection(driverPrefixURL + jdbc_url, username, password);
            DatabaseMetaData dbmd = con.getMetaData();
            stmt = con.createStatement();

            System.out.println("Connected.");

            if (dbmd == null) {
                System.out.println("No database metadata");
            } else {
                System.out.println("Database Product Name: " + dbmd.getDatabaseProductName());
                System.out.println("Database Product Version: " + dbmd.getDatabaseProductVersion());
                System.out.println("Database Driver Name: " + dbmd.getDriverName());
                System.out.println("Database Driver Version: " + dbmd.getDriverVersion());
            }
            StringBuilder script = new StringBuilder();
            String paperSQLPath = null;
            while (paperSQLPath == null) {
                System.out.print("Enter the path to paper.sql: ");
                paperSQLPath = scanner.nextLine();
                File file = new File(paperSQLPath);
                if (!file.exists()) {
                    System.out.println("File not found. Please enter a valid file path.");
                    paperSQLPath = null;
        }
    }

            try {
                BufferedReader br = new BufferedReader(new FileReader(paperSQLPath));
                String line; 
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        break;
                    }
                
                    script.append(line).append("\n");
                    if (line.contains(";")) {
                        String sqlStatement = script.toString().replace(";", "").trim();
                        if (!sqlStatement.isEmpty()) {
                            stmt.execute(sqlStatement);
                        }
                        script.setLength(0); 
                    }
                }
                br.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            return false;
        }
    }

    public static void closeDatabaseConnection() {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static int displayMainMenu(Scanner scanner) {
        int choice = 0;
        while (choice == 0) {
            try {
                System.out.println("Main Menu:");
                System.out.println("1. View table contents");
                System.out.println("2. Search by PUBLICATIONID");
                System.out.println("3. Update URL by PUBLICATIONID");
                System.out.println("4. Exit");
                System.out.print("Enter your choice: ");
                choice = scanner.nextInt();
    
                if (choice < 1 || choice > 4) {
                    System.out.println("Invalid option. Please try again.");
                    choice = 0;
                }
            } catch (InputMismatchException e) {
                scanner.next(); 
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            }
        }
        return choice;
    } 

    public static void viewTableContents(Scanner scanner) {
        int tableChoice = 0;
        while (tableChoice < 1 || tableChoice > 2) {
            try {
                System.out.println("Select table to view:");
                System.out.println("1. PUBLICATIONS");
                System.out.println("2. AUTHORS");
                System.out.print("Enter your choice: ");
                tableChoice = scanner.nextInt();
    
                if (tableChoice < 1 || tableChoice > 2) {
                    System.out.println("Invalid option. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();
            }
        }
        String tableName = (tableChoice == 1) ? "PUBLICATIONS" : "AUTHORS";
    
        try {
            String query = "SELECT * FROM " + tableName;
            ResultSet resultSet = stmt.executeQuery(query);
    
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = resultSet.getString(i);
                    if (columnValue == null) {
                        columnValue = "NULL";
                    } else if (i == 4) {
                        columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                    }else if (i == 5) {
                        columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                    } else {
                        columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                    }
                    System.out.printf("%-20s", columnValue);
                }
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void searchByPublicationID() {
        Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the Publication ID: ");
            int publicationID = scanner.nextInt();
            if (publicationID < 1 || publicationID > 91) {
                System.out.println("Invalid Publication ID.");
                return;
            }
        try {
            String query = "SELECT P.PUBLICATIONID, P.YEAR, P.TYPE, " +
            "SUBSTR(P.TITLE, 1, 30) AS TITLE, " +
            "CASE WHEN LENGTH(P.URL) > 20 THEN SUBSTR(P.URL, 1, 20) || '...' ELSE P.URL END AS URL, " +
            "(SELECT COUNT(A.AUTHOR) FROM AUTHORS A WHERE A.PUBLICATIONID = P.PUBLICATIONID) AS \"No. Of Authors\" " +
            "FROM PUBLICATIONS P " +
            "WHERE P.PUBLICATIONID = ?";

            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1, publicationID);
            ResultSet resultSet = preparedStatement.executeQuery();
    
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();
    
        while (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
                String columnValue = resultSet.getString(i);
                if (columnValue == null) {
                    columnValue = "NULL";
                } else if (i == 5) {
                    columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                } else {
                    columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                }
                System.out.printf("%-20s", columnValue);
            }
            System.out.println();
        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
        public static void updateURLByPublicationID(Scanner scanner) {
            try {
                scanner.nextLine();
                System.out.print("Enter the path to url.csv: ");
                String csvPath = scanner.nextLine();
                

                System.out.print("Enter the PUBLICATIONID: ");
                int publicationID = scanner.nextInt();
                scanner.nextLine();
    
                String newURL = readURLFromCSV(csvPath, publicationID);
    
                if (newURL != null) {
                    String updateQuery = "UPDATE PUBLICATIONS SET URL = ? WHERE PUBLICATIONID = ?";
                    PreparedStatement preparedStatement = con.prepareStatement(updateQuery);
                    preparedStatement.setString(1, newURL);
                    preparedStatement.setInt(2, publicationID);
                    int rowsAffected = preparedStatement.executeUpdate();
    
                    if (rowsAffected > 0) {
                        displayUpdatedTuple(publicationID);
                    } else {
                        System.out.println("Update failed for PUBLICATIONID: " + publicationID);
                    }
                } else {
                    System.out.println("No URL found for the specified PUBLICATIONID in the CSV file.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public static String readURLFromCSV(String csvPath, int publicationID){
            try {
            BufferedReader br = new BufferedReader(new FileReader(csvPath));
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                int idFromCSV = Integer.parseInt(data[0].trim());
                if (idFromCSV == publicationID) {
                    br.close();
                    return data[1].trim();
                }
            }
            br.close();
            } catch(IOException e){
                System.out.println("Invalid filepath");;

            }
            return null; 
        }


        public static void displayUpdatedTuple(int publicationID) throws SQLException {
            String query = "SELECT * FROM PUBLICATIONS WHERE PUBLICATIONID = ?";
            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1, publicationID);
            ResultSet resultSet = preparedStatement.executeQuery();
    
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
    
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = resultSet.getString(i);
                    if (columnValue == null) {
                        columnValue = "NULL";
                    } else if (i == 4) {
                        columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                    }else if (i == 5) {
                        columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                    } else {
                        columnValue = (columnValue.length() > 20) ? columnValue.substring(0, 15) + "..." : columnValue;
                    }
                    System.out.printf("%-20s", columnValue);
                }
                System.out.println();
            }
        }
}

