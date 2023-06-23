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


@ClientEndpoint
public class RunSYNXTest implements Runnable {
    private final String uriTest = "ws://localhost:8080";
    private final String uri = "wss://websocket.cioty.com/faciliate/swg/1/channel";
    public String msg = "{\"txt\":\"---melding---\"}";
    public Session session;
    private Properties props = new Properties();
    private String propFile = "SYNXParams.xml";
    private StringBuilder payload = new StringBuilder();
    AtomicBoolean notFinished = new AtomicBoolean(true);
    // private static final ScheduledExecutorService ses =
    // Executors.newSingleThreadScheduledExecutor();
    public WebSocketContainer container;

    public RunSYNXTest() {
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
    //     JSONObject rtw = new JSONObject(message);
    //     JSONArray json = new JSONArray(rtw.get("RTW").toString());
    //     System.out.println(json.toString());
    }

    public void sendMessage(String message) {
        try { 
            System.out.println("wss: "+message);
            this.session.getBasicRemote().sendText(message);
        } catch (IOException ex) {

        }
    }

    @OnClose
    public void onClose(Session session) throws Exception {
        System.out.println("ferdigggg!!!!!!!!!!");
        notFinished.set(false);
    }

    public void run() {
        PostUrl();
    }

    private void PostUrl() {
        try { System.out.println("POST1");
            String u = props.getProperty("httpUrl");
            URI uri = new URI(u);
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty(
                    props.getProperty("ServerName"),
                    props.getProperty("ServerNo"));
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(payload.toString());
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

}
