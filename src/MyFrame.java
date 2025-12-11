import javax.swing.*;
import java.awt.*;

public class MyFrame extends JFrame {
    private GamePanel gamePanel;

    public MyFrame() {
        setTitle("Шутер");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);

        // Запуск игрового цикла
        gamePanel.startGame();
    }
}