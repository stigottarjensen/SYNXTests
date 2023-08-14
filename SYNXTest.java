
import javax.swing.JOptionPane;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.json.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Properties;

//java -cp .:javax.websocket.jar:tyrus-standalone-client-1.9.jar:json.jar SYNXTest.java
@ClientEndpoint
public class SYNXTest {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    String Url, ServerName, ServerNo, Payload;

    private final String uriTest = "ws://localhost:8080";
    private final String uri = "wss://websocket.cioty.com/faciliate/swg/1/channel";
    public String msg = "{\"txt\":\"---melding---\"}";
    Session session;
    public Properties props = new Properties();
    private static final String[] propFiles = { "StigZorro.xml"};//, "SYNXParams2.xml" };
    private StringBuilder payload = new StringBuilder();
    AtomicBoolean notFinished = new AtomicBoolean(true);
    // private static final ScheduledExecutorService ses =
    // Executors.newSingleThreadScheduledExecutor();
    WebSocketContainer container;

    public SYNXTest(String propFile) {
        init(propFile);
    }

    private void init(String propFile) {
        try {
            System.out.println("Propertis file = " + propFile);
            if (session != null && session.isOpen())
                session.close();
            props.loadFromXML(new FileInputStream(propFile));
            payload.append("token=" + props.getProperty("token")).append("&")
                    .append("objectid=" + props.getProperty("objectid")).append("&")
                    .append("sender=" + props.getProperty("sender")).append("&")
                    .append("receiver=" + props.getProperty("receiver")).append("&")
                    .append("topic=" + props.getProperty("topic")).append("&")
                    .append("payLoad=" + props.getProperty("payLoad"));

            // props.setProperty("payload", payload.toString());
            // props.store(new FileWriter("SynxProps.txt"),"");
            container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(props.getProperty("WebsocketUrl")));
            System.out.println("constructor");
            System.out.println("-----------------");
            System.out.println(payload);
            System.out.println("-----------------");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        String ok = "";
        final SYNXTest synx = new SYNXTest(propFiles[0]);
        int pc = 0;
        while (ok != null && (!ok.trim().equalsIgnoreCase("n") && synx.notFinished.get())) {
            pc++;
            executor.submit(new Runnable() {
                public void run() {
                    synx.PostUrl();
                }
            });
            System.out.println("Enter something (q to quit): ");
            ok = JOptionPane.showInputDialog(null,
                    "Ok to continue, cancel to quit", "OkCancel",
                    JOptionPane.OK_CANCEL_OPTION);
            System.out.println("input : " + ok);
            if (ok != null && ok.trim().equalsIgnoreCase("n"))
                synx.init(propFiles[pc % 2]);
        }
        if (synx.session != null && synx.session.isOpen())
            synx.session.close();
        System.out.println("bye bye!");

        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void PostUrl() {
        try {
            System.out.println("POST1");
            String u = props.getProperty("httpUrl");
            URI uri = new URI(u);
            URL url = uri.toURL(); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
           
            conn.addRequestProperty("Synx-Cat","1");
            conn.setDoOutput(true);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            osw.write(getPayload());
            osw.flush();
            osw.close();
            System.out.println("HTTP respons kode "+ conn.getResponseCode());
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = br.readLine()) != null)
                System.out.println(line);
            System.out.println("POST: " + u);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getPayload() {
        return payload.toString();
    }

    @OnOpen
    public void onOpen(Session session) throws Exception {
        this.session = session;
        System.out.println("startet");

        sendMessage(props.getProperty("tokenFaciliate"));
        sendMessage(props.getProperty("address"));
        sendMessage(msg);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("WSMessage = "+message);
        // JSONObject rtw = new JSONObject(message);
        // JSONArray json = new JSONArray(rtw.get("RTW").toString());
        // System.out.println(json.toString());
    }

    public void sendMessage(String message) {
        try {
            System.out.println("wss: " + message);
            this.session.getBasicRemote().sendText(message);
        } catch (IOException ex) {

        }
    }

    @OnClose
    public void onClose(Session session) throws Exception {
        System.out.println("ferdigggg!!!!!!!!!!");
        notFinished.set(false);
    }
}
