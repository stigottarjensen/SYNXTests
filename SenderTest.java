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

import javax.xml.parsers.*;

//java -cp .:json.jar:any_other.jar  someprog.java

//java -cp .:json.jar:poi-5.2.3.jar:poi-ooxml-5.2.3.jar:poi-ooxml-full-5.2.3.jar:log4j-api-2.20.0.jar:log4j-core-2.20.0.jar:
//commons-compress-1.23.0.jar:commons-io-2.13.0.jar:xmlbeans-5.1.1.jar:commons-collections4-4.4.jar SYNXTest.java
//full command -> java -cp .:json.jar:poi-5.2.3.jar:poi-ooxml-5.2.3.jar:poi-ooxml-full-5.2.3.jar:log4j-api-2.20.0.jar:log4j-core-2.20.0.jar:commons-compress-1.23.0.jar:commons-io-2.13.0.jar:xmlbeans-5.1.1.jar:commons-collections4-4.4.jar SYNXTest.java

public class SenderTest {
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private static SSLSocketFactory sslsf;

    private void Write2File(String val) throws IOException {
        JSONObject jso = new JSONObject(val);
        String js2 = jso.get("RTW").toString();
        JSONObject jsobj = new JSONObject(js2);
        String js3 = jsobj.get("PAYLOAD").toString();
        JSONObject jsobj2 = new JSONObject(js3);
        Properties p = Property.toProperties(jsobj2);
        PrintWriter pw = new PrintWriter(new FileWriter("./output/db.txt"));
        p.forEach((k, v) -> {
            String s = (String) v;
            s = s.replace("\u00C3\u00A6", "æ");
            s = s.replace("\u00C3\u00B8", "ø");
            s = s.replace("\u00C3\u00A5", "å");
            s = s.replace("\u00C3&#134;", "Æ");
            s = s.replace("\u00C3&#152;", "Ø");
            s = s.replace("\u00C3&#133;", "Å");
            pw.println(k + "  " + s);
        });
        pw.close();
    }

    private void PostUrl(Properties prop, String synxcat) {
      
        try {
            String key = synxcat.equals("4")?"receiver_":"sender_";

            String uri = prop.getProperty("httpUrl");
            if (!uri.toLowerCase().startsWith("https://"))
                uri = "https://" + uri;

            URI Uri = new URI(uri);
            URL url = Uri.toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslsf);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(prop.getProperty("inputFilename"));
            NodeList nl = doc.getChildNodes();
            for (int i=0; i< nl.getLength(); i++) {
               Element el = (Element) nl.item(i);
               String name= el.getNodeName();
               String content = el.getTextContent();
               System.out.println(name+"  "+content);
            }
            String urlEncoded = "token="+ prop.getProperty(key+"token")+"&objectid="+prop.getProperty(key+"objectid");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Synx-Cat",synxcat);
            conn.setRequestProperty("Content-Length", urlEncoded.length() + "");
            if (synxcat.equals("4")) {
                conn.setRequestProperty("Connection", "keep-alive");
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
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.trim().length()>2)
                sb.append(line + "\n");
               // else 
            }
            line = sb.toString();
            if (line == null || line.trim().length() < 1)
                line = "***Empty response***";
            System.out.println(line);
            conn.disconnect();

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
        }
    }

    private int strToInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            String fileName = "./SenderReceiverTestParams.xml";
            String SynxCat = args != null && args.length > 0 ? args[0] : "1";
            if (!SynxCat.equals("1"))
                SynxCat = "4";
            FileInputStream file = new FileInputStream(fileName);
            Properties prop = new Properties();
            prop.loadFromXML(file);

            sslsf = SSLContext.getDefault().getSocketFactory();

            SenderTest senderTest = new SenderTest();

            senderTest.PostUrl(prop, SynxCat);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
