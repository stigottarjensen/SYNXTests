
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
public class SYNXTest implements Runnable {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    String Url, ServerName, ServerNo, Payload;

    private final String uriTest = "ws://localhost:8080";
    private final String uri = "wss://websocket.cioty.com/faciliate/swg/1/channel";
    public String msg = "{\"txt\":\"---melding---\"}";
    Session session;
    public Properties props = new Properties();
    private String propFile = "SYNXParams.xml";
    private StringBuilder payload = new StringBuilder();
    AtomicBoolean notFinished = new AtomicBoolean(true);
    // private static final ScheduledExecutorService ses =
    // Executors.newSingleThreadScheduledExecutor();
    WebSocketContainer container;

    public SYNXTest() {
        try {
            props.loadFromXML(new FileInputStream(propFile));
            payload.append("token=" + props.getProperty("token")).append("&")
                    .append("objectid=" + props.getProperty("objectid")).append("&")
                    .append("sender=" + props.getProperty("sender")).append("&")
                    .append("receiver=" + props.getProperty("receiver")).append("&")
                    .append("topic=" + props.getProperty("topic")).append("&")
                    .append("payLoad=" + props.getProperty("payLoad"));

            container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(props.getProperty("WebsocketUrl")));
            System.out.println("constructor");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        String ok = "";
        SYNXTest synx = new SYNXTest();

        synx.Url = synx.props.getProperty("httpUrl");
        synx.ServerName = synx.props.getProperty("ServerName");
        synx.ServerNo = synx.props.getProperty("ServerNo");
        synx.Payload = synx.getPayload();
        // if (args != null && args.length > 0) {
        // synx.msg = synx.msg.replace("---melding---", args[0]);
        // }
        // System.out.println(synx.msg);

        while (ok != null /* && synx.notFinished.get() */) {

            executor.submit(synx);
            System.out.println("Enter something (q to quit): ");
            ok = JOptionPane.showInputDialog(null,
                    "Ok to continue, cancel to quit", "OkCancel",
                    JOptionPane.OK_CANCEL_OPTION);
            System.out.println("input : " + ok);

        }
        if (synx.session != null && synx.session.isOpen())
            synx.session.close();
        System.out.println("bye bye!");

        executor.shutdownNow();
        executor.awaitTermination(9, TimeUnit.SECONDS);
    }

    public void run() {
        PostUrl();
    }

    private void PostUrl() {
        try {
            System.out.println("POST1");
            String u = Url;
            URI uri = new URI(u);
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty(ServerName, ServerNo);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(Payload);
            osw.flush();
            osw.close();
            System.out.println("POST2");
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
        System.out.println(message);
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
