package org.iceterm;

import javax.swing.*;
import java.awt.*;

public class IceTermPanel extends JPanel {
    Label hwLabel;
    public IceTermPanel() {
        super();

        hwLabel = new Label("hello world");
        hwLabel.setBackground(Color.RED);
        hwLabel.setPreferredSize(new Dimension(300, 150));
        this.setBackground(Color.GREEN);
        this.add(hwLabel);this.setLayout(new FlowLayout());
    }
}
