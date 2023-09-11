
import java.io.*;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.json.*;

import java.util.concurrent.*;

public class SenderTestJson {

    private static SSLSocketFactory sslsf;

    private String JSONText = "";
    private JSONObject rtw;
    private JSONObject payload;

    private void Write2File(String path) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(path));
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

    private String PostUrl(int threadNo, Properties prop, String synxcat) {

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
                parseJSON(JSONText);

                Iterator<String> it = rtw.keys();
                while (it.hasNext()) {
                    String name = it.next();
                    String content = rtw.get(name).toString();
                    sb.append("&" + name + "=" + URLEncoder.encode(content, "UTF-8"));
                }
                // System.out.println(sb);
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
                    ret.append("......");
                    ret.append(line);
                    parseJSON(JSONText);
                    String tema = rtw.get("TEMA").toString();
                    long t = System.currentTimeMillis();
                    t = (t / 1000) % 1000;
                    Write2File("./outputjava/Tno" + threadNo + tema + t + ".txt");
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
        return ret.toString();
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
            int threads = args != null && args.length > 1 && SynxCat.equals("1")
                    ? strToInt(args[1], 1)
                    : 1;
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

            if (threads == 1)
                senderTest.PostUrl(0, prop, SynxCat);
            else {
                int cpus = Runtime.getRuntime().availableProcessors();
                ExecutorService es = Executors.newFixedThreadPool(threads < cpus ? threads : cpus);
                CountDownLatch cdl = new CountDownLatch(threads);
                for (int i = 1; i <= threads; i++) {
                    es.submit(senderTest.runPost(i, prop, SynxCat, cdl));
                }
                cdl.await(threads * 2, TimeUnit.SECONDS);
                es.shutdown();
                es.awaitTermination(3, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PostRunner runPost(int threadNo, Properties p, String synxcat, CountDownLatch cdl) {
        return new PostRunner(threadNo, p, synxcat, cdl);
    }

    class PostRunner implements Runnable {
        private int threadNo;
        private Properties prop;
        private String synxcat;
        private CountDownLatch cdl;

        public PostRunner(int threadNo, Properties p, String synxcat, CountDownLatch cdl) {
            this.threadNo = threadNo;
            this.prop = p;
            this.synxcat = synxcat;
            this.cdl = cdl;
        }

        public void run() {
            String s = PostUrl(this.threadNo, this.prop, this.synxcat);
            System.out.println(s);
            cdl.countDown();
        }
    }
}
