
import java.io.*;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.Date;
import org.json.*;

import java.text.*;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HiveAbisair {

    private static SSLSocketFactory sslsf;

    private Properties pr = new Properties();
    private static final AtomicBoolean abContinue = new AtomicBoolean(true);
    PrintWriter pw = null;

    private void Write2File(String path, JSONObject payload, boolean jsonFormat) throws IOException {
        if (pw == null) {
            pw = new PrintWriter(new FileWriter(path, true));
            pw.println("[");
        } else
            pw.println(",");
        if (jsonFormat) {
            pw.println(payload.toString(4));
        } else {
            Properties p = Property.toProperties(payload);
            p.forEach((k, v) -> {
                String s = (String) v;
                String t = (String) k;
                if (t.trim().length() > 0)
                    pw.println(k + " :  " + s);
            });
        }
    }

    private record QueryParams(Map<String, String> sqlWhere, List<String> params, String whereTemplate) {
    }

    private QueryParams SQLWhere(JSONObject payload, String selectSql, String whereTemplate) {
        JSONArray filters = payload.getJSONArray("filters");
        StringBuilder sb = new StringBuilder();
        SortedMap<Integer, String> sqlWhereList = new TreeMap<>(new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return b.intValue() - a.intValue();
            }
        });

        int size = filters.length();
        List<String> params = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            JSONObject js = filters.getJSONObject(i);
            String field = js.getString("field");
            int id = js.getInt("id");
            if (!selectSql.contains(field))
                continue;
            String type = js.getString("type");
            JSONArray values = js.getJSONArray("values");

            switch (type) {
                case "equal":
                case "=":
                    sqlWhereList.put(id, "[" + field + "] = ? ");
                    params.add("%" + values.get(0).toString() + "%");
                    break;
                case "like":
                    sqlWhereList.put(id, "[" + field + "] LIKE ? ");
                    params.add("%" + values.get(0).toString() + "%");
                    break;
                case "in":
                    sb.setLength(0);
                    sb.append("[" + field + "] IN ( ");
                    for (int j = 0; j < values.length(); j++) {
                        params.add(values.get(j).toString());
                        sb.append("?");
                        if (j + 1 < values.length())
                            sb.append(",");
                    }
                    sb.append(") ");
                    sqlWhereList.put(id, sb.toString());
                    break;
                case "between":
                    sqlWhereList.put(id, "[" + field + "] BETWEEN ? AND ? ");
                    params.add(values.get(0).toString());
                    params.add(values.get(1).toString());
                    break;
            }
        }
        Map<String, String> ScrambledWhereList = new HashMap<>();
        final StringBuilder whereT = new StringBuilder(whereTemplate);
        sqlWhereList.forEach((key, value) -> {
            String scrambledId = "[[[" + key + "]]]";
            String wt = whereT.toString().replace(key.toString(), scrambledId);
            whereT.setLength(0);
            whereT.append(wt);
            System.out.println(whereT);
            ScrambledWhereList.put(scrambledId, value);
        });

        return new QueryParams(ScrambledWhereList, params, whereT.toString());
    }

    String legalTemplateCharacters = "0123456789 ()&|";

    private String GetFromDB(Properties prop, String jsonPackage) throws Exception {
        JSONObject jsObj = new JSONObject(jsonPackage);
        JSONObject rtw = getRTW(jsonPackage.toString());
        JSONObject payload = getPayload(rtw.toString());
        String sqlFile = payload.get("request_name").toString();
        String template = payload.get("template").toString();
        List<Object> listPivotFields;
        try {
            listPivotFields = payload.getJSONArray("pivotfields").toList();
        } catch (JSONException jse) {
            listPivotFields = null;
        }
        StringCharacterIterator sci = new StringCharacterIterator(template);
        char ch;
        while ((ch = sci.next()) != CharacterIterator.DONE) {
            if (!legalTemplateCharacters.contains(ch + ""))
                throw new Exception("Illegal character in template: " + ch);
        }

        BufferedReader fr = new BufferedReader(new FileReader("./dbsql/"+sqlFile + ".sql"));
        StringBuilder mainSql = new StringBuilder();
        String l;
        boolean isPivotsql = false;
        boolean isMainsql = true;
        boolean isInstsql = false;
        StringBuilder pivotSql = new StringBuilder();
        StringBuilder instSql = new StringBuilder();

        while ((l = fr.readLine()) != null) {
            if (l.contains("--pivotfields sql")) {
                isPivotsql = true;
                isMainsql = false;
                isInstsql = false;
                continue;
            }
            if (l.contains("--main sql")) {
                isPivotsql = false;
                isMainsql = true;
                isInstsql = false;
                continue;
            }
            if (l.contains("--inst sql")) {
                isPivotsql = false;
                isMainsql = false;
                isInstsql = true;
                continue;
            }
            if (isPivotsql)
                pivotSql.append(l);
            if (isMainsql)
                mainSql.append(l);
            if (isInstsql)
                instSql.append(l);
        }

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
                if (listPivotFields == null || listPivotFields.contains(s)) {
                    if (pivotFields.length() == 0)
                        pivotFields.append("[" + s + "]");
                    else
                        pivotFields.append(",[" + s + "]");
                }
            }
            st.close();
        }

        List<String> instFields = new ArrayList<>();
        if (instSql.length() > 2) { 
            Statement st = con.createStatement();
            ResultSet irs = st.executeQuery(instSql.toString());
            while (irs.next()) {
                String s = irs.getString("installasjon_kode");
                if (!s.startsWith("INST_"))
                    s = "INST_" + s;
                instFields.add(s);
            }
            st.close();
        }

        QueryParams qp = SQLWhere(payload, mainSql.toString(), template);
        template = qp.whereTemplate;

        if (qp.sqlWhere.size() > 0) {

            for (Map.Entry<String, String> me : qp.sqlWhere.entrySet()) {
                template = template.replace(me.getKey(), me.getValue());
            }
            template = template.replace("|", "OR");
            template = template.replace("&", "AND");
        }
        String runSql = "";
        if (pivotFields.length() < 2) {
            mainSql.append(" WHERE ");
            mainSql.append(template);
            runSql = mainSql.toString();
        } else {
            runSql = mainSql.toString();
            runSql = runSql.replace("<<where>>", " WHERE " + template);
            runSql = runSql.replace("<<pivotfields>>", pivotFields);
        }

        System.out.println(runSql);
        if (instFields.size() > 0) {
            for (int a = 0; a < instFields.size(); a++) {
                String tab = instFields.get(a);
                String sql = runSql.replace("<<table>>", tab);
                System.out.println(sql);
                instFields.set(a, sql);
            }
        }

        int loops = Math.max(1, instFields.size());
        for (int a = 0; a < loops; a++) {
            String sql = runSql;
            if (instFields.size() > 0)
                sql = instFields.get(a);
            List<JSONObject> jsList = executeSQL(sql, qp, con);

            String now = timeStamp();
            for (int i = 0; i < jsList.size(); i++) {
                try {
                    Write2File("./dbresult/" + sqlFile + "-" + now + ".json", jsList.get(i), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (pw != null) {
            pw.println("]");
            pw.close();
        }
        return "";
    }

    private List<JSONObject> executeSQL(String sql, QueryParams qp, Connection con) throws Exception {
        PreparedStatement pst = con.prepareStatement(sql);
        for (int c = 0; c < qp.params.size(); c++) {
            pst.setString(c + 1, qp.params.get(c));
        }

        ResultSet rs = pst.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] columns = new String[rsmd.getColumnCount()];
        for (int i = 0; i < columns.length; i++)
            columns[i] = rsmd.getColumnLabel(i + 1);
        int c = 0;
        List<JSONObject> jsList = new ArrayList<>();
        while (rs.next()) {
            JSONObject js = new JSONObject();
            for (int i = 0; i < columns.length; i++) {
                String content = rs.getString(i + 1);
                js.put(columns[i], content == null ? "" : content.trim());
            }
            jsList.add(js);
        }
        rs.close();
        return jsList;
    }

    private String timeStamp() {
        java.util.Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        return dateFormat.format(date);
    }

    private JSONObject getJsonElement(String name, String jsonText) throws Exception {
        JSONObject jso = new JSONObject(jsonText);
        return new JSONObject(jso.get(name).toString());
    }

    private JSONObject getRTW(String jsonText) throws Exception {
        JSONObject jso = new JSONObject(jsonText);
        return new JSONObject(jso.get("RTW").toString());
    }

    private JSONObject getPayload(String jsonText) throws Exception {
        JSONObject jso = new JSONObject(jsonText);
        return new JSONObject(jso.get("PAYLOAD").toString());
    }

    // curl -k https://stig.cioty.com -H "Synx-Cat: 4" -d
    // "token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&objectid=1&payload=hello"
    private String PostUrl(Properties prop, String synxcat, String jsonPackage) {

        int cnt = 0;
        while (abContinue.get() && synxcat.equals("4") || cnt == 0) {
            if (synxcat.equals("1"))
                cnt = 1;
            StringBuilder ret = new StringBuilder();
            try {
                String key = synxcat.equals("4") ? "receiver_" : "sender_";

                String uri = prop.getProperty("httpUrl")
                        + prop.getProperty("path");
                if (!uri.toLowerCase().startsWith("https://"))
                    uri = "https://" + uri;

                URI Uri = new URI(uri);
                URL url = Uri.toURL();
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setSSLSocketFactory(sslsf);
                StringBuilder sb = new StringBuilder();
                if (synxcat.equals("1")) {
                    JSONObject rtw = getRTW(jsonPackage);
                    Iterator<String> it = rtw.keys();
                    while (it.hasNext()) {
                        String name = it.next();
                        String content = rtw.get(name).toString();
                        sb.append("&" + name + "=" + URLEncoder.encode(content, "UTF-8"));
                    }
                }
                if (synxcat.equals("4"))
                    sb.append("&format=json");
                String urlEncoded = "token=" + prop.getProperty(key + "token") + "&objectid="
                        + prop.getProperty(key + "objectid") + sb;
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("Synx-Cat", synxcat);
                conn.setRequestProperty("Content-Length", urlEncoded.length() + "");
                if (synxcat.equals("4")) {
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    // conn.setRequestProperty("Keep-Alive", "timeout=12000,max=25000");// 1200 sek,
                    // max 250 connections
                }
                conn.setDoOutput(true);
                conn.setDoInput(true);
                OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

                osw.write(urlEncoded);
                osw.flush();
                osw.close();

                InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                sb.setLength(0);
                int ii = 0, jj = 0;
                while ((line = br.readLine()) != null) {
                    if (line.trim().length() > 2) {
                        sb.append(line + "\n");
                        line = line.trim();
                        ret.append("......");
                        ret.append(line);
                        jj++;
                        if (synxcat.equals("4")) {
                            System.out.println(jj + " " + (++ii));

                            JSONObject rtw = getRTW(line);
                            JSONObject sy4payload = getPayload(rtw.toString());
                            // if (rtw.get("TEMA").equals("queryresult"))
                            // Write2File("./testtest.txt", sy4payload, true);
                            if (rtw.get("TOPIC").equals("queryrequest"))
                                GetFromDB(prop, sy4payload.toString());
                            System.out.println("---" + sy4payload + "---");
                        }
                    }
                }
                line = sb.toString();
                if (line == null || line.trim().length() < 1)
                    line = "***Empty response***";
                ret.append(line);
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                ret.append(sw.toString());
            }
        }

        return "";
    }

    private static int strToInt(String input, int def) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return def;
        }
    }

    private String getQuery(String jsonFile) {
        String ret = null;
        try {
            FileReader file = new FileReader(jsonFile);
            BufferedReader br = new BufferedReader(file);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            ret = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            ret = null;
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        try {

            String propsFile = "./SenderReceiverTestParams.properties";
            String SynxCat = args != null && args.length > 0 ? args[0] : "0";

            HiveAbisair hiveAbis = new HiveAbisair();
            FileInputStream fInput = new FileInputStream(propsFile);
            Properties prop = new Properties();
            prop.load(fInput);
            if (!SynxCat.equals("0"))
                sslsf = SSLContext.getDefault().getSocketFactory();
            if (SynxCat.equals("4"))
                hiveAbis.PostUrl(prop, "4", null);
            else if (SynxCat.equals("1")) {
                String jsonText = hiveAbis.getQuery(prop.getProperty("inputFilenameJson"));

                if (jsonText != null)
                    hiveAbis.PostUrl(prop, "1", jsonText);
                // else
                // hiveAbis.GetFromDB("1", prop, sb.toString());
            } else {
                WatchService watchService = FileSystems.getDefault().newWatchService();

                Path path = Paths.get("./dbfetch");
                String folder = path.getFileName().toString();
                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        System.out.println(
                                "Event kind:" + event.kind()
                                        + ". File affected: " + event.context() + ".");
                        String fileName = event.context().toString();
                        try {
                            hiveAbis.GetFromDB(prop, hiveAbis.getQuery(folder + "/" + fileName));
                            Files.move(Paths.get(folder + "/" + fileName), Paths.get("./dbdone/" + fileName),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    key.reset();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
