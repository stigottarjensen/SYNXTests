import javax.swing.JOptionPane;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
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
            payload.append("token=" + props.getProperty("token")).append("&");
            // .append("txt=" + props.getProperty("txt")).append("&")
            // .append("hei=" + props.getProperty("hei"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void PostUrl(String SynxCat, String objectID, String melding) {
        try {
            System.out.println("POST1");
            // if (melding==null) melding="EEE";
            String u = props.getProperty("httpUrl");
            URI uri = new URI(u);
            URL url = uri.toURL();
            StringBuilder payLoad = new StringBuilder();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (SynxCat.equals("4"))
                payLoad.append(this.payload).append("&objectID=" + objectID);
            else
                payLoad.append(this.payload).append("&objectid=" + objectID);
            if (melding != null)
                payLoad.append("&txt=" + melding + "&hei=" + melding);
            System.out.println(SynxCat + " " + payLoad);
            conn.setRequestProperty("Synx-Cat", SynxCat);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            osw.write(payload.toString());
            osw.flush();
            osw.close();
            System.out.println("HTTP respons kode " + conn.getResponseCode());
            conn.setReadTimeout(0);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            int t = SynxCat.equals("4") ? 20 : 0;
            for (int i = 0; i <= t; i++) {
                while ((line = br.readLine()) != null)
                    System.out.print("$4$" + line);
                Thread.sleep(15 * t);
            }
            System.out.println("Ferdig SynxCat " + SynxCat);
        } catch (Exception e) {
            e.printStackTrace();
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

        executor.submit(new Runnable() {
            public void run() { try {
                // synx4.PostUrl("4", "2", null);
                String urlen = props.getProperty("httpUrl"); urlen = "http://localhost:3000";
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlen))
                        .GET()
                        //.timeout(Duration.ofSeconds(20))
                        // .header("Synx-Cat", "4")
                        // .POST(BodyPublishers.ofString(
                        //         "token=aToken_124b34e931dd12fa57b28be8d56e6dff371cafe3570ab847e49f87012ff2eca0&&objectid=1&txt=wsswsw&hei=wsswsw"))
                        .build();
                httpClient.sendAsync(request, BodyHandlers.ofString(), pushPromiseHandler())
                        .thenApply(HttpResponse::body)
                        .thenAccept((b) -> System.out.println("\nMain resource:\n" + b))
                        .join();
                asyncPushRequests.forEach(CompletableFuture::join);
                // try {
                //     Thread.sleep(Duration.ofSeconds(10));
                    System.out.println("\nFetched a total of " +
                            asyncPushRequests.size() + " push requests");
                // } catch (InterruptedException ie) {
                //     ie.printStackTrace(System.out);
                // }
            } catch (Exception e) {e.printStackTrace(System.out);}
            }
        });

        int pc = 0;
        while (ok != null && (!ok.trim().equalsIgnoreCase("n") && synx.notFinished.get())) {
            pc++;
            //synx.PostUrl("1", "1", ok);
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
