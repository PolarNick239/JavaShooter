import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

public class MyFrame extends JFrame {
    public MyFrame() {
        setSize(640, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setVisible(true); // делаем окно видимым только после того как оно полностью готово
    }

    @Override
    public void paint(Graphics g) {
        BufferStrategy bufferStrategy = getBufferStrategy(); // Обращаемся к стратегии буферизации
        if (bufferStrategy == null) { // Если она еще не создана
            createBufferStrategy(2); // то создаем ее
            bufferStrategy = getBufferStrategy(); // и опять обращаемся к уже наверняка созданной стратегии
        }
        g = bufferStrategy.getDrawGraphics(); // Достаем текущую графику (текущий буфер) - это наш холст для рисования (спрятанный от глаз пользователя)
        g.clearRect(0, 0, getWidth(), getHeight()); // Очищаем наш холст (ведь там остался предыдущий кадр)

        // Выполняем рисование:
        g.drawOval(200, 100, 20, 10); // рисуем тестовый овал чтобы убедиться что все работает

        g.dispose();                // Освободить все временные ресурсы графики (после этого в нее уже нельзя рисовать)
        bufferStrategy.show();      // Сказать буферизирующей стратегии отрисовать новый буфер (т.е. поменять показываемый и обновляемый буферы местами)
    }
}