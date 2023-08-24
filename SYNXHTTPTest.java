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

    public record TestParams(String SynxCat, String Url, String RequestBody, String Test, int Count, int Pause) {
        // Count : how many times testing, Pause in ms between them
    }

    private static SSLSocketFactory sslsf;

    public record LoggParams(int threadNo, String SynxCat, String LoggText, String textColor) {
    }

    private static final BlockingQueue<LoggParams> Logg = new LinkedBlockingQueue<>();

    private void PostUrl(int threadNo, TestParams tParams) {
        String textColor = WHITE;
        if (tParams.Count == 0)
            textColor = YELLOW + "    ";
        try {
            System.out.println("******************SynxCat " + tParams.SynxCat);
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
            if (tParams.Count ==0) {
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Keep-Alive", "timeout=1200,max=250");// 1200 sek, max 250 connections
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            osw.write(urlEncoded);

            osw.flush();
            osw.close();

            Logg.offer(new LoggParams(threadNo, tParams.SynxCat,
                    "HTTP respons kode " + conn.getResponseCode() + " Test = " + tParams.Test, textColor));

            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "Start response...... ", textColor));
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null) {
                Logg.offer(new LoggParams(threadNo, tParams.SynxCat, line, textColor));
            }
            conn.disconnect();
            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "Ferdig SynxCat ", textColor));

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, sw.toString(), textColor));
        }
    }

    public static void main(String[] args) {
        (new SYNXHTTPTest()).mainRunner();
    }

    private void mainRunner() {
        try {
            System.out.println("Før ssl");
            sslsf = SSLContext.getDefault().getSocketFactory();
            System.out.println("Etter ssl");

            SYNXHTTPTest synxhttpTest = new SYNXHTTPTest();
            OPCPackage pkg = OPCPackage.open(new File("SynxCat_test_sheet.xlsx"));
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            Sheet sheet1 = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            ExecutorService es = Executors.newCachedThreadPool();

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
                        case 6: {
                            try {
                                Count = Integer.parseInt(cellContent);
                            } catch (NumberFormatException nfe) {
                                Count = -1;
                            }
                            break;
                        }
                        case 7: {
                            try {
                                Pause = Integer.parseInt(cellContent);
                            } catch (NumberFormatException nfe) {
                                Pause = 1;
                            }
                            break;
                        }
                    }
                }
                RequestBody = (Token.length() > 0 ? "token=" + Token : "")
                        + (RequestBody.length() > 0 ? "&" + RequestBody : "");
                if (RequestBody.length() > 0 && RequestBody.charAt(0) == '&')
                    RequestBody = RequestBody.substring(1);
                if (Url.length() > 0) {
                    final TestParams tParams = new TestParams(SynxCat, Url, RequestBody, Test, Count, Pause);
                    TestList.add(tParams);
                }
            }

            pkg.close();

            List<Callable<String>> senderList = new ArrayList<>();

            TestList.forEach((tp) -> {
                if (tp.Count == 0) {
                    es.submit(new PostUrlRunner(0, tp));
                } else {
                    for (int i = 1; i <= tp.Count; i++) {
                        senderList.add(new PostUrlRunner(i, tp));
                    }
                }
            });
            es.invokeAll(senderList);
            es.shutdownNow();
            es.awaitTermination(3, TimeUnit.SECONDS);
            Logg.forEach((param) -> {
                System.out.println(param.textColor + param.SynxCat + " -- " + param.LoggText + RESET);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("bye bye!");
        System.exit(0);
    }

    private class PostUrlRunner implements Callable<String> {
        private int threadNo;
        private TestParams tp;

        public PostUrlRunner(int threadNo, TestParams tp) {
            this.threadNo = threadNo;
            this.tp = tp;
        }

        public String call() {
            (new SYNXHTTPTest()).PostUrl(threadNo, tp);
            return "";
        }

    }

}