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
import org.apache.poi.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.openxml4j.opc.*;
import org.apache.logging.log4j.*;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.io.output.*;
import org.apache.xmlbeans.*;
import org.apache.commons.collections4.*;
import org.openxmlformats.schemas.drawingml.x2006.main.*;

//java -cp .:json.jar:any_other.jar  someprog.java

//java -cp .:json.jar:poi-5.2.3.jar:poi-ooxml-5.2.3.jar:poi-ooxml-full-5.2.3.jar:log4j-api-2.20.0.jar:log4j-core-2.20.0.jar:
//commons-compress-1.23.0.jar:commons-io-2.13.0.jar:xmlbeans-5.1.1.jar:commons-collections4-4.4.jar SYNXHTTPTest.java
//full command -> java -cp .:json.jar:poi-5.2.3.jar:poi-ooxml-5.2.3.jar:poi-ooxml-full-5.2.3.jar:log4j-api-2.20.0.jar:log4j-core-2.20.0.jar:commons-compress-1.23.0.jar:commons-io-2.13.0.jar:xmlbeans-5.1.1.jar:commons-collections4-4.4.jar SYNXHTTPTest.java

public class SYNXHTTPTest {
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public record TestParams(String SynxCat, String Url, String RequestBody, String Test, int Count, int Pause) { // Count
                                                                                                                  // :
                                                                                                                  // how
                                                                                                                  // many
                                                                                                                  // times
                                                                                                                  // testing,
                                                                                                                  // Pause
                                                                                                                  // in
                                                                                                                  // ms
                                                                                                                  // between
                                                                                                                  // them
    }

    private static final AtomicBoolean finished = new AtomicBoolean(false);

    private static SSLSocketFactory sslsf;

    public record LoggParams(String SynxCat, String LoggText) {
    }

    private static final BlockingQueue<LoggParams> Logg = new LinkedBlockingQueue<>();

    private void PostUrl(TestParams tParams) {
        try {
            System.out.println("******************SynxCat "+tParams.SynxCat);
            String uri = tParams.Url;
            if (!uri.toLowerCase().startsWith("https://"))
                uri = "https://" + uri;
            URI Uri = new URI(uri);
            URL url = Uri.toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslsf);

            String urlEncoded = tParams.RequestBody;
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Synx-Cat", tParams.SynxCat);
            conn.setRequestProperty("Content-Length", urlEncoded.length() + "");
            if (tParams.SynxCat.equals("4")) {
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Keep-Alive", "timeout=1200,max=250");// 1200 sek, max 250 connections
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            osw.write(urlEncoded);

            osw.flush();
            osw.close();

            Logg.offer(new LoggParams(tParams.SynxCat,
                    "HTTP respons kode " + conn.getResponseCode() + " Test = " + tParams.Test));

            Logg.offer(new LoggParams(tParams.SynxCat, "Start response...... "));
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null) {
                // if (line.trim().isEmpty())
                // Logg.offer(new LoggParams(tParams.SynxCat, "Empty line with " + line.length()
                // + " spaces"));
                // else
                Logg.offer(new LoggParams(tParams.SynxCat, line));
            }
            conn.disconnect();
            Logg.offer(new LoggParams(tParams.SynxCat, "Ferdig SynxCat "));

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logg.offer(new LoggParams(tParams.SynxCat, sw.toString()));
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Før ssl");
            sslsf = SSLContext.getDefault().getSocketFactory();
            System.out.println("Etter ssl");

            SYNXHTTPTest synxhttpTest = new SYNXHTTPTest();
            OPCPackage pkg = OPCPackage.open(new File("SynxCat_test_sheet.xlsx"));
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            Sheet sheet1 = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            ExecutorService es = Executors.newSingleThreadExecutor();

            final List<TestParams> TestList = new ArrayList<>();

            for (Row row : sheet1) {
                int rowIndex = row.getRowNum();
                if (rowIndex < 4)
                    continue;
                String SynxCat = "", Url = "", Token = "", RequestBody = "", Test = "";
                int Count = 1, Pause = 0;
                for (Cell cell : row) {
                    int colIndex = cell.getColumnIndex();
                    if (colIndex > 7)
                        continue;

                    CellReference cellRef = new CellReference(rowIndex, colIndex);

                    String cellContent = formatter.formatCellValue(cell);
                    if (colIndex == 1 && !cellContent.equals("1"))
                        break;
                    if (colIndex > 0 && colIndex < 5 && (cellContent == null || cellContent.length() < 1))
                        continue;

                    switch (colIndex) {
                        case 0:
                            SynxCat = cellContent;
                            break;
                        case 2:
                            Url = cellContent;
                            break;
                        case 3:
                            Token = cellContent;
                            break;
                        case 4:
                            RequestBody = cellContent;
                            break;
                        case 5:
                            Test = cellContent;
                            break;
                        case 6:
                        case 7: {
                            int n;
                            try {
                                n = Integer.parseInt(cellContent);
                            } catch (NumberFormatException nfe) {
                                n = 1;
                            }
                            if (colIndex == 6)
                                Count = n;
                            else
                                Pause = n;
                        }
                    }
                }
                RequestBody = (Token.length() > 0 ? "token=" + Token : "")
                        + (RequestBody.length() > 0 ? "&" + RequestBody : "");
                if (RequestBody.length() > 0 && RequestBody.charAt(0) == '&')
                    RequestBody = RequestBody.substring(1);
                if (Url.length() > 0) {
                    final TestParams tParams = new TestParams(SynxCat, Url, RequestBody, Test, Count, Pause);
                    if (SynxCat.equals("4"))
                        es.execute(new Runnable() {
                            public void run() {
                                (new SYNXHTTPTest()).PostUrl(tParams);
                            }
                        });
                    else
                        TestList.add(tParams);
                }
            }

            pkg.close();

            TestList.forEach((tp) -> {
                for (int i = 0; i < tp.Count; i++) {
                    try {
                        Thread.sleep(tp.Pause);
                    } catch (InterruptedException inter) {
                    }
                    synxhttpTest.PostUrl(tp);
                }
            });

            es.shutdownNow();
            es.awaitTermination(3, TimeUnit.SECONDS);
            Logg.forEach((param) -> {
                String COLOR;
                if (param.SynxCat.equals("4"))
                    COLOR = YELLOW + "     ";
                else
                    COLOR = WHITE;
                System.out.println(COLOR + param.SynxCat + " -- " + param.LoggText + RESET);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("bye bye!");
        System.exit(0);
    }

    // public static void zmain(String[] args) throws Exception {
    // System.out.println("Før ssl");
    // sslsf = SSLContext.getDefault().getSocketFactory();
    // System.out.println("Etter ssl");
    // (new SYNXHTTPTest()).PostUrl(new TestParams("4",
    // "https://stig.cioty.com/zorro",
    // "token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&objectid=2",
    // "Test"));
    // }
}

// executor.submit(new Runnable() {
// public void run() {
// try {
// synx4.HTTPPostSocket("4", "2", "jadamasa");
// SYNXHTTPTest.NioHTTPSocket("4", "2", "jadamasa");
// synx4.HTTPPostSocket("4", "2", "jadamasa");
// String urlen = props.getProperty("httpUrl");
// HttpClient httpClient = HttpClient.newHttpClient();
// HttpRequest request = HttpRequest.newBuilder()
// .uri(URI.create(urlen))
// // .GET()
// .timeout(Duration.ofSeconds(1))
// .header("Synx-Cat", "4")
// .POST(BodyPublishers.ofString("token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&&objectid=1&txt=wsswsw&hei=wsswsw"))
// .build();
// HttpResponse<InputStream> response = httpClient.send(request,
// BodyHandlers.ofInputStream());
// InputStream iStream = response.body();
// byte[] b = new byte[8192];
// System.out.println("Synxcat4 httpclient");
// while (true){
// int size = iStream.read(b);
// if (size > 0)
// System.out.println(new String(b,0, size));
// Thread.sleep(20);
// }

// httpClient.sendAsync(request, BodyHandlers.ofString(), pushPromiseHandler())
// .thenApply(HttpResponse::body)
// .thenAccept((b) -> System.out.println("\nMain resource:\n" + b))
// .join();
// asyncPushRequests.forEach(CompletableFuture::join);
// try {
// Thread.sleep(Duration.ofSeconds(10));
// System.out.println("\nFetched a total of " +
// asyncPushRequests.size() + " push requests");
// } catch (InterruptedException ie) {
// ie.printStackTrace(System.out);
// }
// } catch (Exception e) {
// e.printStackTrace(System.out);
// }
// }
// });

// private static void NioHTTPSocket(String SynxCat, String objectID, String
// melding) {
// try {
// System.out.println("NioSocket1");
// StringBuilder payLoad = new StringBuilder();
// payLoad.append(payload).append("&objectid=" + objectID);
// if (melding != null)
// payLoad.append("&txt=" + melding + "&hei=" + melding);
// StringBuilder sb = new StringBuilder();
// sb.append("POST /zorro HTTP/1.1\r\n");
// sb.append("Host: stig.cioty.com\r\n");
// sb.append("Synx-Cat: 4\r\n");
// sb.append("Content-Length: " + payLoad.length() + "\r\n");
// sb.append("Content-Type: application/x-www-form-urlencoded\r\n\r\n");
// sb.append(payLoad.toString());

// System.out.println(SynxCat + " " + payLoad);
// // SocketFactory sf = SSLSocketFactory.getDefault();
// // Socket so = sf.createSocket(" https://stig.cioty.com", 443);
// InetSocketAddress address = new InetSocketAddress("stig.cioty.com", 443);
// ByteBuffer buffer = ByteBuffer.allocate(4096);
// SocketChannel sChannel = SocketChannel.open();
// sChannel.connect(address);
// sChannel.configureBlocking(false);
// Selector selector = Selector.open();
// sChannel.register(selector, SelectionKey.OP_WRITE);
// sChannel.open();
// final SSLEngine engine = SSLContext.getDefault().createSSLEngine();
// engine.setUseClientMode(true);
// engine.beginHandshake();
// Charset charset = Charset.forName("UTF-8");
// CharsetDecoder decoder = charset.newDecoder();
// CharBuffer charBuffer;
// while (true) {
// selector.select();
// Set<SelectionKey> keys = selector.selectedKeys();
// Iterator<SelectionKey> it = keys.iterator();
// while (it.hasNext()) {
// SelectionKey sk = it.next();
// if (sk.isWritable()) {
// SocketChannel sc = (SocketChannel) sk.channel();
// buffer.clear();
// buffer.put(sb.toString().getBytes(charset));
// buffer.flip();
// while (buffer.hasRemaining())
// sc.write(buffer);
// sc.register(selector, SelectionKey.OP_READ);
// }
// if (sk.isReadable()) {
// SocketChannel sc = (SocketChannel) sk.channel();
// buffer.clear();
// sc.read(buffer);
// String res = new String(buffer.array(), charset);
// System.out.println(res);
// }
// }
// }
// } catch (Exception e) {
// e.printStackTrace();
// }
// }

// private void HTTPPostSocket(String SynxCat, String objectID, String melding)
// {
// Socket so;
// try {
// System.out.println("Socket1");
// // if (melding==null) melding="EEE";
// String u = props.getProperty("httpUrl");

// StringBuilder payLoad = new StringBuilder();

// payLoad.append(this.payload).append("&objectid=" + objectID);
// if (melding != null)
// payLoad.append("&txt=" + melding + "&hei=" + melding);
// System.out.println(SynxCat + " " + payLoad);
// // String urlEncoded = URLEncoder.encode(payload.toString(), "UTF-8");
// SocketFactory sf = SSLSocketFactory.getDefault();
// so = sf.createSocket(" https://stig.cioty.com", 443);
// so.setKeepAlive(true);

// BufferedWriter bw = new BufferedWriter(new
// OutputStreamWriter(so.getOutputStream()));
// StringBuilder sb = new StringBuilder();
// sb.append("POST /zorro HTTP/1.1\r\n");
// sb.append("Host: stig.cioty.com\r\n");
// sb.append("Synx-Cat: 4\r\n");
// sb.append("Content-Length: " + payLoad.length() + "\r\n");
// sb.append("Content-Type: application/x-www-form-urlencoded\r\n\r\n");
// sb.append(payLoad.toString());
// bw.write(sb.toString());
// System.out.println(sb);
// bw.flush();
// //
// token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&objectid=2&txt=jadamasa&hei=jadamasa

// InputStream is = so.getInputStream();
// BufferedReader br = new BufferedReader(new InputStreamReader(is));

// String input = null;
// for (int i = 0; i < 10; i++) {
// while ((input = br.readLine()) != null) {
// System.out.println(input);
// }
// Thread.sleep(2000);
// }
// so.close();
// System.out.println("Ferdig socket SynxCat " + SynxCat);
// } catch (Exception e) {
// e.printStackTrace();
// System.out.println("Exception Ferdig socket SynxCat " + SynxCat);
// }
// }

// private static final List<CompletableFuture<Void>> asyncPushRequests = new
// CopyOnWriteArrayList<>();

// private static HttpResponse.PushPromiseHandler<String> pushPromiseHandler() {

// return (HttpRequest initiatingRequest,
// HttpRequest pushPromiseRequest,
// Function<HttpResponse.BodyHandler<String>,
// CompletableFuture<HttpResponse<String>>> acceptor) -> {
// CompletableFuture<Void> pushcf =
// acceptor.apply(HttpResponse.BodyHandlers.ofString())
// .thenApply(HttpResponse::body)
// .thenAccept((b) -> System.out.println(
// "\nPushed resource body:\n " + b));

// asyncPushRequests.add(pushcf);

// System.out.println("\nJust got promise push number: " +
// asyncPushRequests.size());
// System.out.println("\nInitial push request: " +
// initiatingRequest.uri());
// System.out.println("Initial push headers: " +
// initiatingRequest.headers());
// System.out.println("Promise push request: " +
// pushPromiseRequest.uri());
// System.out.println("Promise push headers: " +
// pushPromiseRequest.headers());
// };
// }
