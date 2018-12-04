import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class MainPanel extends JPanel {
    private String filepath;
    private boolean isMsgShown = false;

    public MainPanel() {}

    public MainPanel(String filepath) {
        this.filepath = filepath;
    }

    public Dimension getPrefferedSize() {
        return new Dimension(820,660);
    }


    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        DraftPaint draftPaint = new DraftPaint();
        DraftParse draftParse= new DraftParse();

        draftParse.setFilename(filepath);
        ArrayList<ArrayList> layers_with_polylines = draftParse.checkLayerExistance();

        if (draftParse.isErrors() && !isMsgShown) {
            JOptionPane.showMessageDialog(getRootPane(), "Ошибки во время чтения записаны в файл errors.txt в корневой директории приложения");
            isMsgShown = true;
        }

        for (ArrayList<ArrayList> polylines : layers_with_polylines) {
            draftPaint.drawLwPolylines(polylines, g);
        }
    }
}
