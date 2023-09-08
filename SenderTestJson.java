import javax.swing.JOptionPane;
//import javax.swing.text.html.HTMLDocument.Iterator;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.io.*;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.*;
import java.time.Duration;
import java.util.*;
import org.json.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;

public class SenderTestJson {

    private static SSLSocketFactory sslsf;

    private String JSONText = "";
    private JSONObject rtw;
    private JSONObject payload;

    private void Write2File(Writer writer) throws IOException {
        Properties p = Property.toProperties(payload);
        PrintWriter pw = new PrintWriter(writer);
        p.forEach((k, v) -> {
            String s = (String) v;
            String t = (String) k;
            if (t.trim().length() > 0)
                pw.println(k + " :  " + s);
        });
        pw.close();
    }

    private void parseJSON(String jsonText) throws Exception {
        JSONObject jso = new JSONObject(jsonText);
        rtw = new JSONObject(jso.get("RTW").toString());
        payload = new JSONObject(rtw.get("PAYLOAD").toString());
    }

    private void PostUrl(Properties prop, String synxcat) {

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
                parseJSON(JSONText);

                Iterator<String> it = rtw.keys();
                while (it.hasNext()) {
                    String name = it.next();
                    String content = rtw.get(name).toString();
                    sb.append("&" + name + "=" + URLEncoder.encode(content, "UTF-8"));
                }
                System.out.println(sb);
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
                conn.setRequestProperty("Keep-Alive", "timeout=1200,max=250");// 1200 sek, max 250 connections
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
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 2) {
                    sb.append(line + "\n");
                    line = line.trim();
                    System.out.println("......");
                    System.out.println(line);
                    parseJSON(JSONText);
                    String tema = rtw.get("TEMA").toString();
                    long t = System.currentTimeMillis();
                    t = (t/1000)%1000;
                    Write2File(
                            new PrintWriter(new FileWriter("./outputjava/db" + tema +t+ ".txt")));
                }
            }
            line = sb.toString();
            if (line == null || line.trim().length() < 1)
                line = "***Empty response***";
            System.out.println(line);
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
        }
    }

    public static void main(String[] args) throws Exception {
        try {

            String fileName = "./SenderReceiverTestParams.properties";
            String SynxCat = args != null && args.length > 0 ? args[0] : "1";
            if (!SynxCat.equals("1"))
                SynxCat = "4";
            FileInputStream fInput = new FileInputStream(fileName);
            Properties prop = new Properties();
            prop.load(fInput);

            sslsf = SSLContext.getDefault().getSocketFactory();

            SenderTestJson senderTest = new SenderTestJson();
            FileReader file = new FileReader(prop.getProperty("inputFilenameJson"));
            BufferedReader br = new BufferedReader(file);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            senderTest.JSONText = sb.toString();
            senderTest.PostUrl(prop, SynxCat);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
