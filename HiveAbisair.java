
import java.io.*;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.net.http.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
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

    private void Write2File(String path, JSONObject payload, boolean jsonFormat) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(path, true));
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
        pw.close();
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
        String sqlFile = jsObj.get("topic").toString();
        String template = jsObj.get("template").toString();
        List<Object> listPivotFields;
        try {
            listPivotFields = jsObj.getJSONArray("pivotfields").toList();
        } catch (JSONException jse) {
            listPivotFields = null;
        }
        StringCharacterIterator sci = new StringCharacterIterator(template);
        char ch;
        while ((ch = sci.next()) != CharacterIterator.DONE) {
            if (!legalTemplateCharacters.contains(ch + ""))
                throw new Exception("Illegal character in template: " + ch);
        }

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
                if (listPivotFields == null || listPivotFields.contains(s)) {
                    if (pivotFields.length() == 0)
                        pivotFields.append("[" + s + "]");
                    else
                        pivotFields.append(",[" + s + "]");
                }
            }
            st.close();
        }

        QueryParams qp = SQLWhere(jsObj, mainSql.toString(), template);
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

        PreparedStatement pst = con.prepareStatement(runSql);
        for (int c = 0; c < qp.params.size(); c++) {
            pst.setString(c + 1, qp.params.get(c));
        }

        ResultSet rs = pst.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] columns = new String[rsmd.getColumnCount()];
        for (int i = 0; i < columns.length; i++)
            columns[i] = rsmd.getColumnLabel(i + 1);
        int c = 0;
        ArrayList<JSONObject> jsList = new ArrayList<>();
        while (rs.next()) {
            JSONObject js = new JSONObject();
            for (int i = 0; i < columns.length; i++) {
                String content = rs.getString(i + 1);
                js.put(columns[i], content == null ? "" : content.trim());
            }
            jsList.add(js);
        }
        rs.close();

        String now = timeStamp();
        jsList.forEach((js) -> {
            try {
                 Write2File("./" + sqlFile + "-" + now + ".txt", js, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return "";
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
    private String PostUrl(Properties prop, String synxcat, JSONObject payload, String jsonPackage) {

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
                    rtw.put("PAYLOAD", payload);
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
                            //     Write2File("./testtest.txt", sy4payload, true);
                            if (rtw.get("TEMA").equals("queryrequest"))
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

    public static void main(String[] args) throws Exception {
        try {

            String fileName = "./SenderReceiverTestParams.properties";
            String SynxCat = args != null && args.length > 0 ? args[0] : "1";

            if (!SynxCat.equals("1"))
                SynxCat = "4";

            HiveAbisair hiveAbis = new HiveAbisair();
            FileInputStream fInput = new FileInputStream(fileName);
            Properties prop = new Properties();
            prop.load(fInput);
            sslsf = SSLContext.getDefault().getSocketFactory();
            if (SynxCat.equals("4"))
                hiveAbis.PostUrl(prop, "4", null, null);
            else {
                FileReader file = new FileReader(prop.getProperty("inputFilenameJson"));
                BufferedReader br = new BufferedReader(file);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
                JSONObject rtw = hiveAbis.getJsonElement("RTW", sb.toString());
                String tema = rtw.get("TEMA").toString();
                if (tema.equals("queryrequest")) {
                    JSONObject payload = new JSONObject(rtw.get("PAYLOAD").toString());
                    hiveAbis.PostUrl(prop, "1", payload, sb.toString());
                } 
                // else
                //     hiveAbis.GetFromDB("1", prop, sb.toString());
            }

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
