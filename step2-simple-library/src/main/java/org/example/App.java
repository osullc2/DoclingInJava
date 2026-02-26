package org.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class App {
    public static void main(String[] args) throws IOException {
        String path = System.getProperty("graalpy.resources");
        if (path == null || path.isBlank() || path.equals("null")) {
            System.err.println("Please provide 'graalpy.resources' system property.");
            System.err.println("Example: mvn -f step2-simple-library/pom.xml "
                    + "exec:java -Dexec.mainClass=org.example.App "
                    + "-Dgraalpy.resources=./step2-simple-library/python-resources");
            System.exit(1);
        }

        try (var context = GraalPy.createPythonContext(path)) {
            // Import qrcode and io as typed Java interfaces
            QRCode qrCode = context.eval("python", "import qrcode; qrcode").as(QRCode.class);
            IO io = context.eval("python", "import io; io").as(IO.class);

            IO.BytesIO bytesIO = io.BytesIO();
            qrCode.make("Hello from GraalPy on JDK " + System.getProperty("java.version"))
                    .save(bytesIO);

            var qrImage = ImageIO.read(new ByteArrayInputStream(bytesIO.getvalue().toByteArray()));

            JFrame frame = new JFrame("QR Code (Step 2)");
            frame.getContentPane().add(new JLabel(new ImageIcon(qrImage)));
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(400, 400);
            frame.setVisible(true);
        }
    }
}

