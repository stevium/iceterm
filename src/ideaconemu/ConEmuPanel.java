package ideaconemu;

import javax.swing.*;
import java.awt.*;

public class ConEmuPanel extends JPanel {
    Label hwLabel;
    public ConEmuPanel() {
        super();

        hwLabel = new Label("hello world");
        hwLabel.setBackground(Color.RED);
        hwLabel.setPreferredSize(new Dimension(300, 150));
        this.setBackground(Color.GREEN);
        this.add(hwLabel);this.setLayout(new FlowLayout());
    }
}
