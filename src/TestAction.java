import javax.swing.*;
import java.awt.event.ActionEvent;

public class TestAction extends AbstractAction {
    public TestAction(String name) {
        super(name);
    }

    public void actionPerformed(ActionEvent event) {
        System.out.println(getValue(Action.NAME) + " selected");
    }
}
