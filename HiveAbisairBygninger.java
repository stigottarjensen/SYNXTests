
import java.io.*;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.json.*;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HiveAbisairBygninger {

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

    private String SQLWhere(JSONObject payload) {
        JSONArray list

    }

    private String GetBygninger(String synxcat, Properties prop, String jsonPackage) throws Exception {
        System.out.println(jsonPackage);
        JSONObject jsObj = new JSONObject(jsonPackage);
        String sqlFile = jsObj.get("topic");
        BufferedReader fr = new BufferedReader(new FileReader(sqlFile+".sql"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = fr.readLine()) != null)
            sb.append(l);

        pr.loadFromXML(new FileInputStream("ABISAIRParams.xml"));
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection con = DriverManager.getConnection("jdbc:sqlserver://" + pr.getProperty("dbServer") + ":" +
                pr.getProperty("dbPort") + ";databaseName=" +
                pr.getProperty("dbName") + ";encrypt=true;trustServerCertificate=true;",
                pr.getProperty("dbUser"),
                pr.getProperty("dbPassword"));
        PreparedStatement pst = con.prepareStatement(sql);

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
                js.put(columns[i], rs.getString(i + 1));
            }
            System.out.println(js);
            System.out.println(++c + " ##################################");
            jsList.add(js);
        }
        rs.close();
        jsList.forEach((js) -> {
            try {
                if (synxcat.equals("1")) {
                    PostUrl(prop, "1", js, jsonPackage);
                }
                if (synxcat.equals("4")) {
                    Write2File("./fragetbygninger.txt", js, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return "";
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
                System.out.println();
                System.out.println(urlEncoded);
                System.out.println();
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
                            System.out.println(jj + " " + (++ii) + " ##################################");

                            JSONObject rtw = getRTW(line);
                            JSONObject sy4payload = getPayload(rtw.toString());
                            if (rtw.get("TEMA").equals("queryresult"))
                                Write2File("./testtest.txt", sy4payload, true);
                            if (rtw.get("TEMA").equals("queryrequest"))
                                GetBygninger(synxcat, prop, sy4payload.toString());
                            System.out.println(sy4payload);
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

            HiveAbisairBygninger hiveAbis = new HiveAbisairBygninger();
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
                JSONObject rtw = hiveAbis.getJsonElement("RTW",sb.toString());
                String tema = rtw.get("TEMA").toString();
                if (tema.equals("queryrequest")) {
                    JSONObject payload = new JSONObject(rtw.get("PAYLOAD").toString());
                     hiveAbis.PostUrl(prop, "1", payload, sb.toString());
                }
                else
                 hiveAbis.GetBygninger("1", prop, sb.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
