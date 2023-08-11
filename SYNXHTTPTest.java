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

public class SYNXHTTPTest {
    public static Properties props = new Properties();
    private static final String[] propFiles = { "StigZorro.xml", "SYNXParams2.xml" };
    private static StringBuilder payload = new StringBuilder();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    AtomicBoolean notFinished = new AtomicBoolean(true);

    public SYNXHTTPTest(String propFile) {
        init(propFile);
    }

    private static void init(String propFile) {
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

    private static void NioHTTPSocket(String SynxCat, String objectID, String melding) {
        try {
            System.out.println("NioSocket1");
            StringBuilder payLoad = new StringBuilder();
            payLoad.append(payload).append("&objectid=" + objectID);
            if (melding != null)
                payLoad.append("&txt=" + melding + "&hei=" + melding);
            StringBuilder sb = new StringBuilder();
            sb.append("POST /zorro HTTP/1.1\r\n");
            sb.append("Host: stig.cioty.com\r\n");
            sb.append("Synx-Cat: 4\r\n");
            sb.append("Content-Length: " + payLoad.length() + "\r\n");
            sb.append("Content-Type: application/x-www-form-urlencoded\r\n\r\n");
            sb.append(payLoad.toString());

            System.out.println(SynxCat + " " + payLoad);
            // SocketFactory sf = SSLSocketFactory.getDefault();
            // Socket so = sf.createSocket(" https://stig.cioty.com", 443);
            InetSocketAddress address = new InetSocketAddress("stig.cioty.com", 443);
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            SocketChannel sChannel = SocketChannel.open();
            sChannel.connect(address);
            sChannel.configureBlocking(false);
            Selector selector = Selector.open();
            sChannel.register(selector, SelectionKey.OP_WRITE);
            sChannel.open();
            final SSLEngine engine = SSLContext.getDefault().createSSLEngine();
            engine.setUseClientMode(true);
            engine.beginHandshake();
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer;
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    if (sk.isWritable()) {
                        SocketChannel sc = (SocketChannel) sk.channel();
                        buffer.clear();
                        buffer.put(sb.toString().getBytes(charset));
                        buffer.flip();
                        while (buffer.hasRemaining())
                            sc.write(buffer);
                        sc.register(selector, SelectionKey.OP_READ);
                    }
                    if (sk.isReadable()) {
                        SocketChannel sc = (SocketChannel) sk.channel();
                        buffer.clear();
                        sc.read(buffer);
                        String res = new String(buffer.array(), charset);
                        System.out.println(res);
                    }
                }
            }
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

            InputStream is = so.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String input = null;
            for (int i = 0; i < 10; i++) {
                while ((input = br.readLine()) != null) {
                    System.out.println(input);
                }
                Thread.sleep(2000);
            }
            so.close();
             System.out.println("Ferdig socket SynxCat " + SynxCat);
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
                    // SYNXHTTPTest.NioHTTPSocket("4", "2", "jadamasa");
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
