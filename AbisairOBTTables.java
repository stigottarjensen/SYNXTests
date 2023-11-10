
import java.io.*;
import java.util.*;
import java.sql.*;

public class AbisairOBTTables {

    private String createTemplate = "CREATE TABLE [dbo].[<<table_name>>](<<column_list>>) ON [PRIMARY]";
    private String dropTemplate = "IF  EXISTS (SELECT * FROM sys.objects WHERE [name] LIKE '<<table_name>>' AND [type_desc] LIKE 'USER_TABLE') "
            + " DROP TABLE [dbo].[<<table_name>>]";
    private String insertTemplate = "INSERT INTO [<<table_name>>] (<<column_list>>) VALUES (<<value_list>>)";

    private void GetFromDB() throws Exception {
        String sqlFile = "make_obt_type_tables";

        BufferedReader fr = new BufferedReader(new FileReader(sqlFile + ".sql"));
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
        Connection con = DriverManager.getConnection("jdbc:sqlserver://" + pr.getProperty("dbServer") + ":" +
                pr.getProperty("dbPort") + ";databaseName=" +
                pr.getProperty("dbName") + ";encrypt=true;trustServerCertificate=true;",
                pr.getProperty("dbUser"),
                pr.getProperty("dbPassword"));

        StringBuilder pivotFields = new StringBuilder();

        Statement st = con.createStatement();
        ResultSet prs = st.executeQuery(pivotSql.toString());
        while (prs.next()) {
            String s = prs.getString("pivotfields");
            System.out.println(s);

            if (pivotFields.length() == 0)
                pivotFields.append("[" + s + "]");
            else
                pivotFields.append(",[" + s + "]");

        }
        st.close();

        String runSql = "";

        runSql = mainSql.toString();
        runSql = runSql.replace("<<where>>", " ");
        runSql = runSql.replace("<<pivotfields>>", pivotFields);
        System.out.println(runSql);

        PreparedStatement pst = con.prepareStatement(runSql);

        ResultSet rs = pst.executeQuery();
        StringBuilder columnList = new StringBuilder();
        StringBuilder insertColumnList = new StringBuilder();
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] columns = new String[rsmd.getColumnCount()];
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            columns[i] = rsmd.getColumnLabel(i + 1);
            line.append(columns[i] + "\t");
            columnList.append(" [" + columns[i] + "] [varchar] (500) NULL \n");
            insertColumnList.append(" [" + columns[i] + "]");
            if (i < columns.length - 1) {
                columnList.append(",");
                insertColumnList.append(",");
            }
        }
        String qmarks = " ?,".repeat(columns.length - 1);
        qmarks = qmarks + " ?";
        createTemplate = createTemplate.replace("<<column_list>>", columnList.toString());
        insertTemplate = insertTemplate.replace("<<column_list>>", insertColumnList.toString());
        insertTemplate = insertTemplate.replace("<<value_list>>", qmarks);
        String objektType = "";
        Statement dropSt = con.createStatement();
        Statement createSt = con.createStatement();
        PreparedStatement insertPSt = null;
        ;
        while (rs.next()) {
            line.setLength(0);
            String obt = rs.getString("objekt_type");
            obt = obt == null ? "" : obt.trim();
            if (!obt.equals(objektType)) {
                String tableName = "INST_" + obt;
                String createSql = createTemplate.replace("<<table_name>>", tableName);
                String dropSql = dropTemplate.replace("<<table_name>>", tableName);
                String insertSql = insertTemplate.replace("<<table_name>>", tableName);
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
                String content = rs.getString(i + 1);
                content = content == null ? "" : content.trim();
                insertPSt.setString(i + 1, content);
            }
            insertPSt.executeUpdate();
        }
        con.close();
    }

    public static void main(String[] args) throws Exception {
        try {

            AbisairOBTTables OBTAbis = new AbisairOBTTables();

            OBTAbis.GetFromDB();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}