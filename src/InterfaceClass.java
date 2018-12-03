import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InterfaceClass {
    private static String current_filename = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        System.out.println("Created GUI on EDT? " + SwingUtilities.isEventDispatchThread());
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        JFrame f = new JFrame("ZTTA - DXF to SVG converter");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        createMenuBar(f);
        JPanel p = new JPanel();
        f.add(p);
        f.pack();
        f.setSize(800,600);
        f.setVisible(true);
    }

    private static void paintDraftFromFile(JFrame f, String filepath) {
        f.setVisible(false);
        f.getContentPane().removeAll();
        MainPanel panel = new MainPanel(filepath);
        f.getContentPane().add(panel);
        f.pack();
        f.setSize(panel.getPrefferedSize());
        f.setVisible(true);
    }


    public static void createMenuBar(JFrame frame) {
        JMenu fileMenu = new JMenu("Файл");

        //Create a file chooser
        final JFileChooser fc = new JFileChooser();
        System.out.println(fc.getCurrentDirectory());
        if (extractLastPath() != null && !extractLastPath().equals("")) {
            File filepath = new File(extractLastPath());
            fc.setCurrentDirectory(filepath);
        }
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                return (f.getName().toLowerCase().endsWith("dxf"));
            }

            @Override
            public String getDescription() {
                return "DXF (Drawing eXchange Format)";
            }
        };
        fc.setFileFilter(fileFilter);

        Action openAction = new TestAction("Открыть") {
            @Override
            public void actionPerformed(ActionEvent event) {
                int returnVal = fc.showOpenDialog(frame);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    current_filename = file.getAbsolutePath();
                    System.out.println(file.getAbsolutePath());
                    paintDraftFromFile(frame, file.getAbsolutePath());
                    frame.setTitle(file.getPath());
                    saveLastPath(file.getParent());
                    /*
                    if (Paths.get("errors.txt").toFile().exists()) {
                        showInfoMessage(frame, "Внимание", "Ошибки во время чтения записаны в файл errors.txt в корневой директории приложения");
                    }
                    */
                } else {
                    System.out.println("Open command cancelled by user");
                }
            }
        };
        JMenuItem openItem = fileMenu.add(openAction);
        openItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));

        Action saveAction = new TestAction("Сохранить") {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser();

                if (extractLastPath() != null && !extractLastPath().equals("")) {
                    File filepath = new File(extractLastPath());
                    fileChooser.setCurrentDirectory(filepath);
                    fileChooser.setSelectedFile(new File(current_filename));
                }

                FileFilter fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return (f.getName().toLowerCase().endsWith("svg"));
                    }

                    @Override
                    public String getDescription() {
                        return "SVG (Scalable Vector Graphics)";
                    }
                };
                fileChooser.setFileFilter(fileFilter);

                int rVal = fileChooser.showSaveDialog(frame);

                if (rVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (!current_filename.equals("")) {
                        DraftParse draftParse = new DraftParse();
                        draftParse.saveToFile(current_filename, file.getAbsolutePath());
                    } else {
                        showInfoMessage(frame, "Внимание", "Отсутствует файл для сохранения");
                    }
                } else {
                    System.out.println("Save command cancelled by user");
                }
            }
        };
        JMenuItem saveItem = fileMenu.add(saveAction);
        saveItem.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));


        fileMenu.add(new AbstractAction("Выход") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        menuBar.add(fileMenu);
    }

    private static void showInfoMessage(JFrame frame, String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static void saveLastPath(String filepath) {
        try {
            Path file = Paths.get("lastpath.txt");
            Files.write(file, filepath.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String extractLastPath() {
        String result = "";
        try {
            result = Files.readAllLines(Paths.get("lastpath.txt")).get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}


