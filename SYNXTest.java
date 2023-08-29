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
//commons-compress-1.23.0.jar:commons-io-2.13.0.jar:xmlbeans-5.1.1.jar:commons-collections4-4.4.jar SYNXTest.java
//full command -> java -cp .:json.jar:poi-5.2.3.jar:poi-ooxml-5.2.3.jar:poi-ooxml-full-5.2.3.jar:log4j-api-2.20.0.jar:log4j-core-2.20.0.jar:commons-compress-1.23.0.jar:commons-io-2.13.0.jar:xmlbeans-5.1.1.jar:commons-collections4-4.4.jar SYNXTest.java

public class SYNXTest {
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private record TestParams(int SynxCat, String Url, String RequestBody, int Count) {
    }

    private record ReportRow(String SenderKey, String SenderValue, String ListenerKey, String ListenerValue) {
    }

    private static final List<ReportRow> ReportTable = new ArrayList<>();

    private static SSLSocketFactory sslsf;

    public record LoggParams(int threadNo, int SynxCat, String keyText, String LoggText, String textColor,
            int rowIndex) {
    }

    private static final BlockingQueue<LoggParams> Logg = new LinkedBlockingQueue<>();

    private void PostUrl(int threadNo, TestParams tParams, int rowIndex) {
        String textColor = WHITE;
        int rowNum = threadNo + rowIndex;
        if (tParams.Count == 0)
            textColor = YELLOW + "    ";
        try {
            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "", "Start post...... ", textColor, rowNum));
            String uri = tParams.Url;
            if (!uri.toLowerCase().startsWith("https://"))
                uri = "https://" + uri;

            URI Uri = new URI(uri);
            URL url = Uri.toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslsf);

            String urlEncoded = tParams.RequestBody;
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Synx-Cat", tParams.SynxCat + "");
            conn.setRequestProperty("Content-Length", urlEncoded.length() + "");
            if (threadNo == 0) {
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
                    "HTTP respons kode ", "" + conn.getResponseCode(), textColor, rowNum));

            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "", "Start response...... ", textColor, rowNum));
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {

                if (threadNo == 0)
                    Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "", line, textColor, rowNum));

                sb.append(line + "\n");
            }
            line = sb.toString();
            if (line == null || line.trim().length() < 1)
                line = "***Empty response***";
            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "", line, textColor, rowNum));
            conn.disconnect();
            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "", "Ferdig SynxCat ", textColor, rowNum));

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logg.offer(new LoggParams(threadNo, tParams.SynxCat, "Error", sw.toString(), textColor, rowNum));
        }
    }

    private int strToInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        (new SYNXTest()).mainRunner();
    }

    private String getCellValue(Row row, int index) {
        Cell c = row.getCell(index);
        if (c == null)
            return "";
        else {
            String val = c.getStringCellValue();
            return val == null ? "" : val;
        }
    }

    private void mainRunner() {
        try {
            System.out.println("Før ssl");
            sslsf = SSLContext.getDefault().getSocketFactory();
            System.out.println("Etter ssl");

            SYNXTest synxTest = new SYNXTest();
            OPCPackage pkg = OPCPackage.open(new File("SynxCat_test_sheet v2.xlsx"));
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            Sheet sheet1 = wb.getSheetAt(0);
            int writeRow = sheet1.getLastRowNum() + 2;
            DataFormatter formatter = new DataFormatter();

            LoggRunner loggRunner = new LoggRunner(Logg);
            TestParams testParamsSender = null, testParamsListener = null;

            final List<TestParams> TestList = new ArrayList<>();
            String UrlSender = null, UrlListener = null, Test = "";
            StringBuilder RequestBodySender = new StringBuilder();
            StringBuilder RequestBodyListener = new StringBuilder();
            int CountSender = 0, CountListener = 0, SynxCatSender = 0, SynxCatListener = 0;

            for (Row row : sheet1) {
                int rowIndex = row.getRowNum();

                Cell cell = row.getCell(0);
                ReportRow rr = new ReportRow(getCellValue(row, 0),
                        getCellValue(row, 1),
                        getCellValue(row, 2),
                        getCellValue(row, 3));
                ReportTable.add(rr);

                String cellValue = cell.getStringCellValue().toLowerCase();
                if (cellValue == null)
                    cellValue = "";
                switch (cellValue) {
                    case "count": {
                        CountSender = strToInt(row.getCell(1).getStringCellValue());
                        Cell c3 = row.getCell(2);
                        if (c3 != null && c3.getStringCellValue().toLowerCase().equals("count"))
                            CountListener = strToInt(row.getCell(3).getStringCellValue());
                        break;
                    }
                    case "synx-cat": {
                        SynxCatSender = strToInt(row.getCell(1).getStringCellValue());
                        Cell c3 = row.getCell(2);
                        if (c3 != null && c3.getStringCellValue().toLowerCase().equals("synx-cat"))
                            SynxCatListener = strToInt(row.getCell(3).getStringCellValue());
                        break;
                    }
                    case "objectid": {
                        int objectid = strToInt(row.getCell(1).getStringCellValue());
                        RequestBodySender.append("&objectid=" + objectid);
                        Cell c3 = row.getCell(2);
                        if (c3 != null && c3.getStringCellValue().toLowerCase().equals("objectid"))
                            objectid = strToInt(row.getCell(3).getStringCellValue());
                        RequestBodyListener.append("&objectid=" + objectid);
                        break;
                    }
                    case "token": {
                        String token = row.getCell(1).getStringCellValue();
                        RequestBodySender.append("token=" + token);
                        Cell c3 = row.getCell(2);
                        if (c3 != null && c3.getStringCellValue().toLowerCase().equals("token"))
                            token = row.getCell(3).getStringCellValue();
                        RequestBodyListener.append("token=" + token);
                        break;
                    }
                    case "url": {
                        UrlSender = row.getCell(1).getStringCellValue();
                        Cell c3 = row.getCell(2);
                        if (c3 != null && c3.getStringCellValue().toLowerCase().equals("url"))
                            UrlListener = row.getCell(3).getStringCellValue();
                        break;
                    }
                    case "":
                    case "sender":
                    case "parameter":
                        break;
                    default: {
                        Cell c = row.getCell(1);
                        if (c != null) {
                            String val = c.getStringCellValue();
                            val = URLEncoder.encode(val, "UTF-8");
                            RequestBodySender.append("&" + cellValue + "=" + val);
                        }
                    }
                }
            }
            ReportTable.add(new ReportRow("", "", "", ""));

            testParamsSender = new TestParams(SynxCatSender, UrlSender, RequestBodySender.toString(),
                    CountSender);
            testParamsListener = SynxCatListener > 0
                    ? new TestParams(SynxCatListener, UrlListener, RequestBodyListener.toString(),
                            CountListener)
                    : null;
            System.out.println(testParamsListener.RequestBody);

            pkg.close();

            List<Callable<String>> senderList = new ArrayList<>();
            ExecutorService es = Executors.newCachedThreadPool();

            rowCounter[0] = rowCounter[1] = ReportTable.size() + 1;

            es.execute(loggRunner);

            if (testParamsListener != null)
                es.submit(new PostUrlRunner(0, testParamsListener, ReportTable.size()));

            for (int i = 1; i <= testParamsSender.Count; i++) {
                senderList.add(new PostUrlRunner(i, testParamsSender, ReportTable.size()));
            }

            Thread.sleep(500);
            es.invokeAll(senderList);
            Thread.sleep(500);
            es.shutdown();
            es.awaitTermination(3, TimeUnit.SECONDS);
            PrintWriter pw = new PrintWriter(new File("wsx.html"));
            pw.println(
                    "<DOCTYPE html><html><head><style>table, td {border-style:solid; border-width:1px; padding: 2px; font-size:20px;} </style></head><body><table><tbody>");
            pw.println("<style>table, td {border-style:solid; border-width:1px; padding: 2px;}</style>");
            pw.println("</head><body><table><tbody>");
            ReportTable.forEach((row) -> {
                pw.print("<tr><td>" + row.SenderKey + "</td>");
                pw.print("<td>" + row.SenderValue + "</td>");
                pw.print("<td>" + row.ListenerKey + "</td>");
                pw.println("<td>" + row.ListenerValue + "</td></tr>");
            });
            pw.println("</tbody></table></body></html>");
            pw.flush();
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("bye bye!");
        System.exit(0);
    }

    private class PostUrlRunner implements Callable<String> {
        private int threadNo;
        private TestParams tp;
        private int writeRow;

        public PostUrlRunner(int threadNo, TestParams tp, int writeRow) {
            this.threadNo = threadNo;
            this.tp = tp;
            this.writeRow = writeRow;
        }

        public String call() {
            (new SYNXTest()).PostUrl(threadNo, tp, writeRow);
            return "";
        }
    }

    private static final int[] rowCounter = new int[2];

    private class LoggRunner implements Runnable {
        BlockingQueue<LoggParams> bq;

        public LoggRunner(BlockingQueue<LoggParams> bq) {
            this.bq = bq;

        }

        public void run() {
            try {
                long timer = System.nanoTime();
                while (true) {
                    LoggParams param = bq.take();
                    boolean threadZero = param.threadNo == 0;
                    int ind = ++rowCounter[threadZero ? 0 : 1];
                    long t = (System.nanoTime() - timer) / 1000000;

                    String oldSenderKey = "", oldSenderValue = "", oldListenerKey = "", oldListenerValue = "";
                    ReportRow oldRR = ind < ReportTable.size() ? ReportTable.get(ind) : null;
                    if (oldRR != null) {
                        oldSenderKey = oldRR.SenderKey != null ? oldRR.SenderKey : "";
                        oldSenderValue = oldRR.SenderValue != null ? oldRR.SenderValue : "";
                        oldListenerKey = oldRR.ListenerKey != null ? oldRR.ListenerKey : "";
                        oldListenerValue = oldRR.ListenerValue != null ? oldRR.ListenerValue : "";
                    }
                    ReportRow rr = new ReportRow(!threadZero ? "Thread:" + param.threadNo : oldSenderKey,
                            !threadZero ? param.LoggText : oldSenderValue, oldListenerKey,
                            threadZero ? param.LoggText : oldListenerValue);
                    ;
                    if (ind >= ReportTable.size()) {
                        ReportTable.add(rr);
                    } else {
                        ReportTable.set(ind, rr);
                    }
                    System.out.println(RED + param.threadNo + RESET + "  " + param.textColor + param.SynxCat + " -- "
                            + param.LoggText + CYAN + ", " + t + "ms" + RESET);

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

// Row row = sheet1.getRow(param.rowIndex);
// if (row == null)
// row = sheet1.createRow(param.rowIndex);
// int cellNo = param.threadNo == 0 ? 2 : 0;
// Cell cell0 = row.createCell(cellNo);
// cell0.setCellValue(param.threadNo);
// Cell cell1 = row.createCell(cellNo + 1);
// cell1.setCellValue(param.LoggText);