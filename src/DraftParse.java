import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLWPolyline;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class DraftParse {
    private String filename;

    private ArrayList<String> errors = new ArrayList<>();

    private double figWidth;
    private double figHeight;
    private double figMinX;
    private double figMinY;

    private boolean errors_flag = false;

    public DraftParse() {}

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isErrors() {
        return errors_flag;
    }

    public ArrayList<ArrayList> checkLayerExistance() {
        return modifyPoints(createRawPolylines());
    }

    private ArrayList<ArrayList> createRawPolylines() {
        ArrayList<ArrayList> layers_with_polylines = new ArrayList<>();
        if (filename != null && !filename.isEmpty()) {
            try {
                Parser parser = ParserBuilder.createDefaultParser();
                parser.parse(filename, DXFParser.DEFAULT_ENCODING);
                DXFDocument doc = parser.getDocument();

                figHeight = doc.getBounds().getHeight();
                figWidth = doc.getBounds().getWidth();
                figMinX = doc.getBounds().getMinimumX();
                figMinY = doc.getBounds().getMinimumY();

                if (doc.containsDXFLayer("0") && doc.containsDXFLayer("1")) {
                    DXFLayer layer1 = doc.getDXFLayer("1");
                    DXFLayer layer0 = doc.getDXFLayer(DXFConstants.DEFAULT_LAYER);

                    if (isLayerContainsPolylines(layer1)) {
                        layers_with_polylines.add(parseLayerForLwPolylines(layer1));
                        if (isLayerContainsPolylines(layer0)) {
                            layers_with_polylines.add(parseLayerForLwPolylines(layer0));
                        } else {
                            String layer1_error = filename + " - " + "Отстутствуют полилинии на слое 0. Отверстия показаны не будут";
                            if (!containsCurrentError(layer1_error)) {
                                errors.add(layer1_error);
                                setErrorFlag();
                            }
                        }
                    } else {
                        String layer0_error = filename + " - " + "Отстутствуют полилинии на слое 1. Контур отсутсвует";
                        if (!containsCurrentError(layer0_error)) {
                            errors.add(layer0_error);
                            setErrorFlag();
                        }
                    }
                } else {
                    String layers_error = filename + " - " + "Отсутствуют необходимые слои для работы. Контур должен находиться в слое '1', а отверстия в слое по умолчанию ('0') ";
                    if (!containsCurrentError(layers_error)) {
                        errors.add(layers_error);
                        setErrorFlag();
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
                if (!containsCurrentError(e.getLocalizedMessage())) {
                    errors.add(filename + " - " + e.getLocalizedMessage());
                    setErrorFlag();
                }
            }
            if (errors_flag) {
                createErrorsFile(errors);
            } else {
                deleteErrorsFile();
            }
        }
        return layers_with_polylines;
    }

    private boolean isLayerContainsPolylines(DXFLayer layer) {
        if (layer.hasDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE)) {
            System.out.println("LWPolyline: " + layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE).size());
            return true;
        } else return false;
    }

    private ArrayList<ArrayList> parseLayerForLwPolylines(DXFLayer layer) {
        ArrayList<ArrayList> layer_polylines = new ArrayList<>();
        for (int i = 0; i < layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE).size(); i++) {
            DXFLWPolyline lwpolyline = (DXFLWPolyline) layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE).get(i);
            ArrayList<Point2D> vertices = new ArrayList<>();
            for (int j = 0; j < lwpolyline.getVertexCount(); j++) {
                Point2D vertex = new Point2D.Double(lwpolyline.getVertex(j).getX(), lwpolyline.getVertex(j).getY());
                vertices.add(vertex);
            }
            layer_polylines.add(vertices);
        }
        return makeOffsetToZero(layer_polylines);
    }

    private ArrayList<ArrayList> makeOffsetToZero(ArrayList<ArrayList> listOfLwPolylines) {

        ArrayList<ArrayList> list_of_lwpolylines = new ArrayList<>();
        for (ArrayList<Point2D> lwPolyline : listOfLwPolylines) {
            ArrayList<Point2D> vertices = new ArrayList<>();
            for (int i = 0; i < lwPolyline.size(); i++) {
                //double modifX = lwPolyline.get(i).getX() + figMinX;
                //double modifY = lwPolyline.get(i).getY() + figMinY;
                double modifX = calcOffsetX(lwPolyline.get(i).getX());
                double modifY = calcOffsetY(lwPolyline.get(i).getY());
                Point2D vertex = new Point2D.Double(modifX, modifY);
                vertices.add(vertex);
            }
            list_of_lwpolylines.add(vertices);
        }
        return list_of_lwpolylines;
    }

    private double calcOffsetX(double value) {
        if (figMinX < 0) {
            return value + (-1*figMinX);
        } else {
            return value - figMinX;
        }
    }

    private double calcOffsetY(double value) {
        if (figMinY < 0) {
            return value + (-1*figMinY);
        } else {
            return value - figMinY;
        }
    }

    private ArrayList<ArrayList> modifyPoints(ArrayList<ArrayList> layers) {
        ArrayList<ArrayList> modified_layers = new ArrayList<>();
        for (ArrayList<ArrayList> layer : layers) {
            ArrayList<ArrayList> modified_lwpolylines = new ArrayList<>();
            for (ArrayList<Point2D> lwpolyline : layer) {
                ArrayList<Point2D> modified_points = new ArrayList<>();
                for (Point2D point : lwpolyline) {
                    Point2D mod_point = point;
                    mod_point = reflectionPointY(mod_point);
                    mod_point = scalePoint(mod_point);
                    modified_points.add(mod_point);
                }
                modified_lwpolylines.add(modified_points);
            }
            modified_layers.add(modified_lwpolylines);
        }
        return modified_layers;
    }

    private Point2D reflectionPointY(Point2D point) {
        return new Point2D.Double(point.getX(), (-point.getY() + figHeight));
    }

    private Point2D scalePoint(Point2D point) {
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        int screenWidth = screenSize.width - 50;
        int screenHeight = screenSize.height - 100;
        double k = 1;
        if (figWidth > figHeight) {
            k = screenWidth / figWidth;
            return new Point2D.Double(point.getX()*k, point.getY()*k);
        } else {
            k = screenHeight / figHeight;
            return new Point2D.Double(point.getX()*k, point.getY()*k);
        }
    }

    //#region Errors Handling

    private boolean containsCurrentError(String err) {
        for (String e : errors) {
            if (err.equals(e)) {
                return true;
            }
        }
        return false;
    }

    private void setErrorFlag() {
        if (!errors_flag) {
            errors_flag = true;
        }
    }

    private void createErrorsFile(ArrayList<String> error_list) {
        try {
            Path file = Paths.get("errors.txt");
            Files.write(file, error_list, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteErrorsFile() {
        try {
            Path file = Paths.get("errors.txt");
            if (file.toFile().exists()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //#endregion

    //#region Save to SVG

    public void saveToFile(String file_name, String save_name) {
        ArrayList<ArrayList> layers_with_polylines = new ArrayList<>();
        if (file_name != null && !file_name.isEmpty()) {
            try {
                Parser parser = ParserBuilder.createDefaultParser();
                parser.parse(file_name, DXFParser.DEFAULT_ENCODING);
                DXFDocument doc = parser.getDocument();

                figHeight = doc.getBounds().getHeight();
                figWidth = doc.getBounds().getWidth();
                figMinX = doc.getBounds().getMinimumX();
                figMinY = doc.getBounds().getMinimumY();

                if (doc.containsDXFLayer("0") && doc.containsDXFLayer("1")) {
                    DXFLayer layer1 = doc.getDXFLayer("1");
                    DXFLayer layer0 = doc.getDXFLayer(DXFConstants.DEFAULT_LAYER);

                    if (isLayerContainsPolylines(layer1)) {
                        layers_with_polylines.add(parseLayerForLwPolylines(layer1));
                        if (isLayerContainsPolylines(layer0)) {
                            layers_with_polylines.add(parseLayerForLwPolylines(layer0));
                        }
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        convertPointsToSvg(layers_with_polylines, save_name);
    }

    private void convertPointsToSvg (ArrayList<ArrayList> layers, String save_name) {
        ArrayList<ArrayList> modified_layers = new ArrayList<>();
        for (ArrayList<ArrayList> layer : layers) {
            ArrayList<ArrayList> modified_lwpolylines = new ArrayList<>();
            for (ArrayList<Point2D> lwpolyline : layer) {
                ArrayList<Point2D> modified_points = new ArrayList<>();
                for (Point2D point : lwpolyline) {
                    Point2D mod_point = point;
                    mod_point = reflectionPointY(mod_point);
                    modified_points.add(mod_point);
                }
                modified_lwpolylines.add(modified_points);
            }
            modified_layers.add(modified_lwpolylines);
        }
        buildSvg(figWidth, figHeight, modified_layers.get(0), modified_layers.get(1), save_name);
    }

    private void buildSvg(double width, double height, ArrayList<ArrayList> contour, ArrayList<ArrayList> holes, String name) {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);

        String contour_str = "";
        for (ArrayList<Point2D> points : contour) {
            for (Point2D point : points) {
                contour_str += df.format(point.getX()).replace(",", ".") + "," + df.format(point.getY()).replace(",", ".") + " ";
            }
        }

        ArrayList<String> hole_polygons = new ArrayList<>();
        for (ArrayList<Point2D> points : holes) {
            String polygon = "";
            for (Point2D point : points) {
                polygon += df.format(point.getX()).replace(",", ".") + "," + df.format(point.getY()).replace(",", ".") + " ";
            }
            hole_polygons.add(polygon);
        }

        ArrayList<String> lines = new ArrayList<>();
        lines.add("<svg");
        lines.add("xmlns=\"http://www.w3.org/2000/svg\"");
        lines.add("xml:space=\"preserve\"");
        lines.add("version=\"1.1\"");
        lines.add("xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        lines.add("width=\"100%\"");
        lines.add("height=\"100%\"");
        lines.add("viewBox=\"0 0 " + Math.round(width) + " " + Math.round(height) + "\">");
        lines.add("<g id=\"body\">");
        lines.add("<polygon points=\"" + contour_str + "\"/>");
        lines.add("</g>");
        lines.add("<g id=\"holes\">");
        for (int i = 0; i < hole_polygons.size(); i++) {
            lines.add("<polygon points=\"" + hole_polygons.get(i) + "\" fill=\"white\"/>");
        }
        lines.add("</g>");
        lines.add("</svg>");
        try {
            Path file = Paths.get(name +".svg");
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //#endregion
}
































