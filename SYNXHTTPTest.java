import javax.swing.JOptionPane;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class SYNXHTTPTest {
    public Properties props = new Properties();
    private static final String[] propFiles = { "SYNXParams.xml", "SYNXParams2.xml" };
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
            //if (melding==null) melding="EEE";
            String u = props.getProperty("httpUrl");
            URI uri = new URI(u);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            StringBuilder payLoad = new StringBuilder();
            payLoad.append(this.payload).append("&objectID=" + objectID);
            if (melding != null)
                payLoad.append("&txt="+melding+"&hei="+melding);
                System.out.println(SynxCat+" "+payLoad);
            conn.addRequestProperty("Synx-Cat", SynxCat);
            conn.setDoOutput(true);
            // conn.setReadTimeout(300000);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

            osw.write(payload.toString());
            osw.flush();
            osw.close();
            System.out.println("HTTP respons kode " + conn.getResponseCode());
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            int i = 0;
            while ((line = br.readLine()) != null)
                System.out.print(line);
            // System.out.println((++i)+" Synx-Cat: "+SynxCat+">"+line);
            System.out.println("Ferdig SynxCat " + SynxCat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        String ok = "";
        final SYNXHTTPTest synx = new SYNXHTTPTest(propFiles[0]);
        executor.submit(new Runnable() {
            public void run() {
                synx.PostUrl("4", "2", null);
            }
        });
        int pc = 0;
        while (ok != null && (!ok.trim().equalsIgnoreCase("n") && synx.notFinished.get())) {
            pc++;
            synx.PostUrl("1", "1", ok);
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
