import javax.swing.JOptionPane;
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
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SYNXHTTPTest {
    public static Properties props = new Properties();
    private static final String[] propFiles = { "StigZorro.xml", "SYNXParams2.xml" };
    private StringBuilder payload = new StringBuilder();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    AtomicBoolean notFinished = new AtomicBoolean(true);

    public SYNXHTTPTest(String propFile) {
        init(propFile);
    }

    private void init(String propFile) {
        try {
            System.out.println("Propertis file = " + propFile);
            props.loadFromXML(new FileInputStream(propFile));
            payload.append("token=" + URLEncoder.encode(props.getProperty("token"), "UTF-8"));
            // .append("txt=" + props.getProperty("txt")).append("&")
            // .append("hei=" + props.getProperty("hei"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static SSLSocketFactory sslsf;

    private void PostUrl(String SynxCat, String objectID, String melding) {
        try {

            System.out.println("POST1");
            // if (melding==null) melding="EEE";
            String u = props.getProperty("httpUrl");
            URI uri = new URI(u);
            URL url = uri.toURL();
            StringBuilder payLoad = new StringBuilder();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslsf);
            // if (SynxCat.equals("4"))
            // payLoad.append(this.payload).append("&objectID=" + objectID);
            // else
            payLoad.append(this.payload).append("&objectid=" + objectID);
            if (melding != null)
                payLoad.append("&txt=" + melding + "&hei=" + melding);
            String urlEncoded = URLEncoder.encode(payload.toString(), "UTF-8");
            System.out.println(SynxCat + " " + urlEncoded);
            conn.setRequestProperty("Synx-Cat", SynxCat);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            osw.write(urlEncoded);
            osw.flush();
            osw.close();
            // System.out.println(SynxCat+" HTTP respons kode " + conn.getResponseCode());
            conn.setReadTimeout(0);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = br.readLine()) != null || SynxCat.equals("4")) {
                if (line != null)
                    System.out.print("> " + line);
                Thread.sleep(1);
            }

            conn.disconnect();
            System.out.println("Ferdig SynxCat " + SynxCat);
        } catch (InterruptedException e) {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void HTTPPostSocket(String SynxCat, String objectID, String melding) {
        Socket so;
        try {
            System.out.println("Socket1");
            // if (melding==null) melding="EEE";
            String u = props.getProperty("httpUrl");

            StringBuilder payLoad = new StringBuilder();

            payLoad.append(this.payload).append("&objectid=" + objectID);
            if (melding != null)
                payLoad.append("&txt=" + melding + "&hei=" + melding);
            System.out.println(SynxCat + " " + payLoad);
            // String urlEncoded = URLEncoder.encode(payload.toString(), "UTF-8");
            SocketFactory sf = SSLSocketFactory.getDefault();
            so = sf.createSocket(" https://stig.cioty.com", 443);
            so.setKeepAlive(true);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(so.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("POST /zorro HTTP/1.1\r\n");
            sb.append("Host: stig.cioty.com\r\n");
            sb.append("Synx-Cat: 4\r\n");
            sb.append("Content-Length: " + payLoad.length() + "\r\n");
            sb.append("Content-Type: application/x-www-form-urlencoded\r\n\r\n");
            sb.append(payLoad.toString());
            bw.write(sb.toString());
            System.out.println(sb);
            bw.flush();
            // token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&objectid=2&txt=jadamasa&hei=jadamasa
            // ByteBuffer buffer = ByteBuffer.allocate(4096);
            // SocketChannel sChannel = so.getChannel();
            // // sChannel.configureBlocking(false);
            // /* from w w w . jav a 2s.c o m */
            // Charset charset = Charset.forName("UTF-8");
            // CharsetDecoder decoder = charset.newDecoder();
            // CharBuffer charBuffer;

            InputStream is = so.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int big = Integer.MAX_VALUE;
            String input = null;
            while (big > 0) {
                // sChannel.read(buffer);
                // buffer.flip();
                // charBuffer = decoder.decode(buffer);
                if (is.available() > 0)
                    input = br.readLine();
                big--;
                if (big % 500000 == 0)
                    System.out.print(input);
               // Thread.sleep(1);
            }
            so.close();
            // conn.setRequestProperty("Synx-Cat", SynxCat);
            // conn.setDoOutput(true);
            // conn.setDoInput(true);

            // OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            // osw.write(payload.toString());
            // osw.flush();
            // osw.close();
            // System.out.println("HTTP respons kode " + conn.getResponseCode());
            // conn.setReadTimeout(0);
            // System.out.println(conn.getContent().toString());
            // BufferedReader br = new BufferedReader(new
            // InputStreamReader(conn.getInputStream()));
            // String line;
            // int t = SynxCat.equals("4") ? 20 : 0;
            // for (int i = 0; i <= t; i++) {
            // while ((line = br.readLine()) != null)
            // System.out.print("$4$" + line);
            // Thread.sleep(15 * t);
            // }
            
        // } catch (InterruptedException ie) {
        //    System.out.println("Ferdig socket SynxCat " + SynxCat);
        //     System.out.println("InterruptedException Ferdig socket SynxCat " + SynxCat);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception Ferdig socket SynxCat " + SynxCat);
        }
    }

    private static final List<CompletableFuture<Void>> asyncPushRequests = new CopyOnWriteArrayList<>();

    private static HttpResponse.PushPromiseHandler<String> pushPromiseHandler() {

        return (HttpRequest initiatingRequest,
                HttpRequest pushPromiseRequest,
                Function<HttpResponse.BodyHandler<String>, CompletableFuture<HttpResponse<String>>> acceptor) -> {
            CompletableFuture<Void> pushcf = acceptor.apply(HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept((b) -> System.out.println(
                            "\nPushed resource body:\n " + b));

            asyncPushRequests.add(pushcf);

            System.out.println("\nJust got promise push number: " +
                    asyncPushRequests.size());
            System.out.println("\nInitial push request: " +
                    initiatingRequest.uri());
            System.out.println("Initial push headers: " +
                    initiatingRequest.headers());
            System.out.println("Promise push request: " +
                    pushPromiseRequest.uri());
            System.out.println("Promise push headers: " +
                    pushPromiseRequest.headers());
        };
    }

    public static void main(String[] args) throws Exception {

        String ok = "";
        final SYNXHTTPTest synx = new SYNXHTTPTest(propFiles[0]);
        final SYNXHTTPTest synx4 = new SYNXHTTPTest(propFiles[0]);
        sslsf = SSLContext.getDefault().getSocketFactory();

        executor.submit(new Runnable() {
            public void run() {
                try {
                    synx4.HTTPPostSocket("4", "2", "jadamasa");
                    // synx4.HTTPPostSocket("4", "2", "jadamasa");
                    // String urlen = props.getProperty("httpUrl");
                    // urlen = "http://localhost:3000";
                    // HttpClient httpClient = HttpClient.newHttpClient();
                    // HttpRequest request = HttpRequest.newBuilder()
                    // .uri(URI.create(urlen))
                    // .GET()
                    // // .timeout(Duration.ofSeconds(20))
                    // // .header("Synx-Cat", "4")
                    // // .POST(BodyPublishers.ofString(
                    // //
                    // "token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&&objectid=1&txt=wsswsw&hei=wsswsw"))
                    // .build();
                    // httpClient.sendAsync(request, BodyHandlers.ofString(), pushPromiseHandler())
                    // .thenApply(HttpResponse::body)
                    // .thenAccept((b) -> System.out.println("\nMain resource:\n" + b))
                    // .join();
                    // asyncPushRequests.forEach(CompletableFuture::join);
                    // // try {
                    // // Thread.sleep(Duration.ofSeconds(10));
                    // System.out.println("\nFetched a total of " +
                    // asyncPushRequests.size() + " push requests");
                    // // } catch (InterruptedException ie) {
                    // // ie.printStackTrace(System.out);
                    // // }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        });

        int pc = 0;
        while (ok != null && (!ok.trim().equalsIgnoreCase("n") && synx.notFinished.get())) {
            pc++;
            synx.PostUrl("1", "1", ok);
            // synx4.PostUrl("4", "2", "jadamasa");
            ok = JOptionPane.showInputDialog(null,
                    "Ok to continue, cancel to quit", "OkCancel",
                    JOptionPane.OK_CANCEL_OPTION);
            System.out.println("input : " + ok);

        }

        System.out.println("bye bye!");

        executor.shutdownNow();
        executor.awaitTermination(3, TimeUnit.SECONDS);
        System.exit(0);
    }
}
