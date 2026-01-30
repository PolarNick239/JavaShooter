import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private GameManager gameManager;
    private Timer gameTimer;
    private long lastUpdateTime;
    private boolean paused = false;
    private boolean pPressed = false;
    private boolean oPressed = false;
    private boolean iPressed = false;
    private boolean gPressed = false;

    public GamePanel() {
        Dimension size = new Dimension(800, 600);
        gameManager = new GameManager(size.width, size.height);

        setBackground(new Color(20, 20, 40));
        setPreferredSize(size);
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        gameTimer = new Timer(16, this);
        lastUpdateTime = System.nanoTime();
    }

    public void startGame() {
        gameTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gameManager.draw(g2d, getWidth(), getHeight());

        if (paused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String text = "PAUSED";
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (getWidth() - textWidth) / 2, getHeight() / 2);
        }

        if (gameManager.isGameOver()) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String gameOver = "ИГРА ОКОНЧЕНА";
            g2d.drawString(gameOver,
                    getWidth() / 2 - g2d.getFontMetrics().stringWidth(gameOver) / 2,
                    getHeight() / 2 - 50);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String score = "Счёт: " + gameManager.getScore();
            g2d.drawString(score,
                    getWidth() / 2 - g2d.getFontMetrics().stringWidth(score) / 2,
                    getHeight() / 2 + 20);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = currentTime;

        if (!paused) {
            gameManager.update(deltaTime, getWidth(), getHeight());
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_P) {
            if (!pPressed) {
                paused = !paused;
                if (!paused) {
                    lastUpdateTime = System.nanoTime();
                }
                pPressed = true;
            }
            return;
        }
        if (keyCode == KeyEvent.VK_O) {
            if (!oPressed) {
                gameManager.toggleGodMode();
                oPressed = true;
            }
            return;
        }
        if (keyCode == KeyEvent.VK_I) {
            if (!iPressed) {
                gameManager.toggleShowPaths();
                iPressed = true;
            }
            return;
        }
        if (keyCode == KeyEvent.VK_G) {
            if (!gPressed) {
                gameManager.tryThrowGrenade();
                gPressed = true;
            }
            return;
        }
        gameManager.setMovementKey(keyCode, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_P) {
            pPressed = false;
            return;
        }
        if (keyCode == KeyEvent.VK_O) {
            oPressed = false;
            return;
        }
        if (keyCode == KeyEvent.VK_I) {
            iPressed = false;
            return;
        }
        if (keyCode == KeyEvent.VK_G) {
            gPressed = false;
            return;
        }
        gameManager.setMovementKey(keyCode, false);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            gameManager.setShooting(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            gameManager.setShooting(false);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        gameManager.setMousePosition(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        gameManager.setMousePosition(e.getX(), e.getY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}
