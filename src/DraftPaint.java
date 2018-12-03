import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class DraftPaint extends JComponent {

    public DraftPaint() {}

    public void drawLwPolylines(ArrayList<ArrayList> lwpolylines, Graphics g) {
        createPolygon(lwpolylines, g);
    }

    private void createPolygon(ArrayList<ArrayList> lwPolylines, Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (ArrayList<Point2D> vertices_list : lwPolylines) {
            GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, vertices_list.size());
            polygon.moveTo(vertices_list.get(0).getX(), vertices_list.get(0).getY());

            for (int i = 1; i < vertices_list.size(); i++) {
                polygon.lineTo(vertices_list.get(i).getX(), vertices_list.get(i).getY());
            }
            polygon.lineTo(vertices_list.get(0).getX(), vertices_list.get(0).getY());
            g2.draw(polygon);
        }
    }
}
