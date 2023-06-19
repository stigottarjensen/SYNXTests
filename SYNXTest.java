import java.io.*;
import java.net.*;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import java.util.Properties;
import java.util.concurrent.*;

@ClientEndpoint
public class SYNXTest implements Runnable {
    private final String uriTest = "ws://localhost:8080";
    private final String uri = "wss://websocket.cioty.com/faciliate/swg/1/channel";

    private Session session;
    private Properties props = new Properties();
    private String propFile = "SYNXParams.xml";
    private StringBuilder payload = new StringBuilder();
    private static final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public SYNXTest() {
        try {
            props.loadFromXML(new FileInputStream(propFile));
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            payload.append("token="+props.getProperty("token")).append("&").
                    append("objectid="+props.getProperty("objectid")).append("&").
                    append("sender="+props.getProperty("sender")).append("&").
                    append("receiver="+props.getProperty("receiver")).append("&").
                    append("topic="+props.getProperty("topic")).append("&").
                    append("payLoad="+props.getProperty("payLoad"));
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
         
        sendMessage(props.getProperty("tokenJson"));
        sendMessage(props.getProperty("address"));
        sendMessage(msg);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(message);
    }

    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException ex) {

        }
    }

    public static void main(String[] args) throws Exception {
        SYNXTest sy = new SYNXTest();
        ScheduledFuture sf = ses.scheduleWithFixedDelay(sy,5,10,TimeUnit.SECONDS);
        
        System.in.read();
        sf.cancel(true);
        ses.shutdownNow();
        ses.awaitTermination(5,TimeUnit.SECONDS);
    }

    public void run() {
        PostUrl();
    }

    private void PostUrl() {
        try {
        URI uri = new URI(props.getProperty("httpUrl"));
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
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line=br.readLine()) != null)
            System.out.println(line);
        } catch (Exception e) {e.printStackTrace();}
    }

    // private String token = "{\"token\":\"aToken_249df9c9a7fa8eac91bc21cad0208675b0d1539ff44cfe47bacf58100f5f4da1\"}";
    // // this is private token for faciliate/swg
    // private String address = "{\"url\":\"faciliate/swg/1\"}";
    private String msg = "{\"txt\":\"poiuytrewq\"}";

    // private String httpUrl = "https://faciliate.cioty.com/swg";
    // private String payLoad = "token=aToken_3003b45b115695e0bfeef3bc124fce7d1efa98685a7ee091df62e479888dc3c1&" +
    //         "objectid=1&" +
    //         "sender=qazxsw&" +
    //         "receiver=bikkjaTilHenrik&" +
    //         "topic=svarttjern&" +
    //         "payload=jadadada";
}