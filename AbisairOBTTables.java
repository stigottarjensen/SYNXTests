
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.*;
import java.sql.*;

public class AbisairOBTTables {

    private String GetFromDB() throws Exception {
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
        if (pivotSql.length() > 2) {
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
        }

        String runSql = "";

        runSql = mainSql.toString();
        runSql = runSql.replace("<<where>>", " ");
        runSql = runSql.replace("<<pivotfields>>", pivotFields);
        System.out.println(runSql);

        PreparedStatement pst = con.prepareStatement(runSql);

        ResultSet rs = pst.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] columns = new String[rsmd.getColumnCount()];
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            columns[i] = rsmd.getColumnLabel(i + 1);
            line.append(columns[i] + "\t");
        }

        int c = 0;
        ArrayList<String> rsList = new ArrayList<>();
        PrintWriter pw = new PrintWriter(new FileWriter("obt.txt"));
        pw.println(line.toString());
        while (rs.next()) {
            line.setLength(0);
            for (int i = 0; i < columns.length; i++) {
                String content = rs.getString(i + 1);
                line.append(content == null ? "" : content.trim());
                line.append("\t");
            }
            pw.println(line.toString());
        }
        pw.close();
        rs.close();

        return "";
    }

    private static int strToInt(String input, int def) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return def;
        }
    }

    public static void main(String[] args) throws Exception {
        try {

            AbisairOBTTables OBTAbis = new AbisairOBTTables();

            OBTAbis.GetFromDB();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void wmain(String[] args) throws Exception {

        WatchService watchService = FileSystems.getDefault().newWatchService();

        Path path = Paths.get("./");

        path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                System.out.println(
                        "Event kind:" + event.kind()
                                + ". File affected: " + event.context() + ".");
            }
            key.reset();
        }
    }
}
