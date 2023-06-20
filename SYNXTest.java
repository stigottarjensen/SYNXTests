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
         
        sendMessage(props.getProperty("tokenFaciliate"));
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
        if (args!=null && args.length>0) {
            sy.msg = sy.msg.replace("---melding---", args[0]);
        }
        System.out.println(sy.msg);
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

    private String msg = "{\"txt\":\"---melding---\"}";

}