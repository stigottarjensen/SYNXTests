
import java.io.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.*;

public class AbisairOBTTables implements Runnable {

    private String createTemplate = "CREATE TABLE [dbo].[<<table_name>>]( [tid] [datetime2] NULL, <<column_list>>) ON [PRIMARY]";
    private String dropTemplate = "IF  EXISTS (SELECT * FROM sys.objects WHERE [name] LIKE '<<table_name>>' AND [type_desc] LIKE 'USER_TABLE') "
            + " DROP TABLE [dbo].[<<table_name>>]";
    private String insertTemplate = "INSERT INTO [<<table_name>>] ([tid], <<column_list>>) VALUES (CURRENT_TIMESTAMP, <<value_list>>)";
    private static final Semaphore semaphore = new Semaphore(1);
    private static final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    private record CreateNInsertFields(List<String> columnNames, StringBuilder create, StringBuilder insert,
            StringBuilder qmarks) {
    }

    private void GetFromDB() throws Exception {
        String sqlFile = "make_obt_type_tables";
        System.out.println();
        System.out.println("INST tables reload.... kl. " + Calendar.getInstance().getTime().toString());
        System.out.println();
        BufferedReader fr = new BufferedReader(new FileReader("./dbsql/"+sqlFile + ".sql"));
        StringBuilder mainSql = new StringBuilder();
        String l;
        boolean isPivotsql = false;
        boolean isMainsql = true;
        StringBuilder pivotSql = new StringBuilder();

        while ((l = fr.readLine()) != null) {
            if (l.contains("--pivotfields sql")) {
                isPivotsql = true;
                isMainsql = false;
                continue;
            }
            if (l.contains("--main sql")) {
                isPivotsql = false;
                isMainsql = true;
                continue;
            }
            if (isPivotsql)
                pivotSql.append(l);
            if (isMainsql)
                mainSql.append(l);
        }

        Properties pr = new Properties();
        pr.loadFromXML(new FileInputStream("ABISAIRParams.xml"));
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String dbUrl = "";
        Connection con = null;

        if (StigMac) {
            dbUrl = "jdbc:sqlserver://" + pr.getProperty("dbServer") + ":" +
                    pr.getProperty("dbPort") + ";databaseName=" +
                    pr.getProperty("dbName") +
                    ";encrypt=true;trustServerCertificate=true;";
            con = DriverManager.getConnection(dbUrl, pr.getProperty("dbUser"),
                    pr.getProperty("dbPassword"));
        } else {
            dbUrl = "jdbc:sqlserver://" + pr.getProperty("dbServer") + ":" +
                    pr.getProperty("dbPort") + ";databaseName=" +
                    pr.getProperty("dbName") + ";encrypt=true;trustServerCertificate=true;"
                    +";integratedSecurity=true;";
            con = DriverManager.getConnection(dbUrl);
       }
        Map<String, CreateNInsertFields> newTablesFields = new HashMap<>();
        Statement st = con.createStatement();
        ResultSet prs = st.executeQuery(pivotSql.toString());
        StringBuilder allPivotFields = new StringBuilder();
        Set<String> labels = new HashSet<>();
        int allPivotFieldsCounter = 0;
        while (prs.next()) {
            String pivField = prs.getString("pivotfields");
            String obt_type = prs.getString("OBT_Type");
            obt_type = obt_type.trim();
            CreateNInsertFields pivFields = newTablesFields.get(obt_type);
            if (pivFields == null) {
                pivFields = new CreateNInsertFields(new ArrayList<String>(), new StringBuilder(),
                        new StringBuilder(), new StringBuilder());
                newTablesFields.put(obt_type, pivFields);
            }
            String komma = ",";
            if (pivFields.create.length() == 0)
                komma = "";
            boolean extraCol = pivField.endsWith("_extracolumn");
            String pivField1 = pivField;
            String pivField2 = "";
            if (extraCol) {
                String[] expivfield = pivField.split("_");
                pivField1 = expivfield[0];
                pivField2 = pivField1 + "_tekst";
            }
            pivFields.columnNames.add(pivField);
            pivFields.insert.append(komma + "[" + pivField1 + "]");
            pivFields.create.append(komma + " [" + pivField1 + "] [varchar] (500) NULL \n");
            pivFields.qmarks.append(komma + "?");
            if (extraCol) {
                pivFields.insert.append(",[" + pivField2 + "]");
                pivFields.create.append(" ,[" + pivField2 + "] [varchar] (500) NULL \n");
                pivFields.qmarks.append(",?");
            }
            if (!labels.contains(pivField)) {
                labels.add(pivField);
                allPivotFields.append("[" + pivField + "],");
                allPivotFieldsCounter++;
            }
        }
        allPivotFields.deleteCharAt(allPivotFields.length() - 1);
        st.close();

        String runSql = "";
        runSql = mainSql.toString();
        runSql = runSql.replace("<<where>>", " ");
        runSql = runSql.replace("<<pivotfields>>", allPivotFields);
        PreparedStatement pst = con.prepareStatement(runSql);

        ResultSet rs = pst.executeQuery();
        StringBuilder createColumnList = new StringBuilder();
        StringBuilder insertColumnList = new StringBuilder();
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] columns = new String[rsmd.getColumnCount() - allPivotFieldsCounter];
        StringBuilder line = new StringBuilder();
        for (int j = 0; j < columns.length; j++) {
            columns[j] = rsmd.getColumnName(j + 1);
            createColumnList.append(" [" + columns[j] + "] [varchar] (500) NULL, \n");
            insertColumnList.append(" [" + columns[j] + "],");
        }
        String objektType = "";
        Statement dropSt = con.createStatement();
        Statement createSt = con.createStatement();
        PreparedStatement insertPSt = null;
        while (rs.next()) {
            line.setLength(0);
            String obt = rs.getString("objekt_type");
            obt = obt == null ? "" : obt.trim();
            if (!obt.equals(objektType)) {
                String tableName = "INST_" + obt;
                System.out.print("  TAB = " + tableName);
                String createSql = createTemplate.replace("<<table_name>>", tableName);
                String dropSql = dropTemplate.replace("<<table_name>>", tableName);
                String insertSql = insertTemplate.replace("<<table_name>>", tableName);
                createSql = createSql.replace("<<column_list>>",
                        createColumnList.toString() + newTablesFields.get(obt).create);
                insertSql = insertSql.replace("<<column_list>>",
                        insertColumnList.toString() + newTablesFields.get(obt).insert);
                insertSql = insertSql.replace("<<value_list>>",
                        "?,".repeat(columns.length) + newTablesFields.get(obt).qmarks);
                System.out.println(createSql);
                System.out.println(insertSql);
                con.setAutoCommit(false);
                dropSt.executeUpdate(dropSql);
                con.commit();
                createSt.executeUpdate(createSql);
                con.commit();
                con.setAutoCommit(true);
                insertPSt = con.prepareStatement(insertSql);
                objektType = obt;
            }
            for (int i = 0; i < columns.length; i++) {
                String content = rs.getString(columns[i]);
                content = content == null ? "" : content.trim();
                insertPSt.setString(i + 1, content);
            }
            int colCount = 0;
            for (int i = 0; i < newTablesFields.get(obt).columnNames.size(); i++) {
                String colName = newTablesFields.get(obt).columnNames.get(i);
                String content = rs.getString(colName);
                content = content == null ? "" : content.trim();
                if (colName.endsWith("_extracolumn")) {
                    String f = "", g = "";
                    String[] cont = content.split("@@@@");
                    if (!content.startsWith("@@@@") && cont.length > 0)
                        f = cont[0];
                    if (!content.endsWith("@@@@") && cont.length > 1)
                        g = cont[1];
                    insertPSt.setString(colCount + columns.length + 1, f);
                    insertPSt.setString(++colCount + columns.length + 1, g);
                } else
                    insertPSt.setString(colCount + columns.length + 1, content);
                colCount++;
            }
            insertPSt.executeUpdate();
        }
        System.out.println();
        System.out.print("Enter 1 for reload tables now, 2 for quit: ");
        con.close();
    }

    @Override
    public void run() {
        runGetDB("1");
    }

    private void runGetDB(String s) {
        if (!s.equals("1"))
            return;
        if (semaphore.tryAcquire()) {
            try {
                GetFromDB();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }

    static boolean StigMac = false;

    public static void main(String[] args) throws Exception {
        try {
            Map<String, String> env = System.getenv();
            if (env.containsKey("USER")) {
                StigMac = env.get("USER").equals("stigottarjensen");
            }
            AbisairOBTTables OBTAbis = new AbisairOBTTables();
            Calendar cal = Calendar.getInstance();
            Calendar time2am = Calendar.getInstance();
            time2am.set(Calendar.HOUR_OF_DAY, 3);
            time2am.set(Calendar.MINUTE, 0);
            time2am.set(Calendar.SECOND, 0);
            time2am.set(Calendar.MILLISECOND, 0);
            if (time2am.compareTo(cal) < 0)
                time2am.add(Calendar.DAY_OF_MONTH, 1);
            long delay = time2am.getTimeInMillis() - cal.getTimeInMillis();
            ses.scheduleAtFixedRate(OBTAbis, delay, 24 * 60 * 60 * 1000L, TimeUnit.MILLISECONDS);
            // ses.scheduleAtFixedRate(OBTAbis, 3000L, 25*1000L, TimeUnit.MILLISECONDS);
            System.out.println(cal.getTime().toString());
            Console cons = System.console();
            String s = "";
            do {
                s = cons.readLine("Enter 1 for reload tables now, 2 for quit: ");
                s = s.trim();
                OBTAbis.runGetDB(s);
            } while (!s.equals("2"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ses.close();
        }
    }

}