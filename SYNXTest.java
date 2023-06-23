package synx;
import static javax.swing.JOptionPane.showMessageDialog;
import javax.swing.JOptionPane;

import java.util.concurrent.*;

x
//java -cp .:javax.websocket.jar:tyrus-standalone-client-1.9.jar:json.jar SYNXTest.java

public class SYNXTest {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
     public static void main(String[] args) throws Exception {

        String ok = "";
        RunSYNXTest synx = new RunSYNXTest();
        // if (args != null && args.length > 0) {
        //     synx.msg = synx.msg.replace("---melding---", args[0]);
        // }
        // System.out.println(synx.msg);

        while (ok!=null /*&& synx.notFinished.get()*/) {

            executor.submit(synx);
            System.out.println("Enter something (q to quit): ");
            ok =  JOptionPane.showInputDialog(null,
                                "Ok to continue, cancel to quit","OkCancel",
                                JOptionPane.OK_CANCEL_OPTION);
            System.out.println("input : "+ok);
                
        }
        if (synx.session!=null && synx.session.isOpen())
            synx.session.close();
        System.out.println("bye bye!");

        executor.shutdownNow();
        executor.awaitTermination(9, TimeUnit.SECONDS);
    }
}
