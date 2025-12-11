public class Main {
    public static void main(String[] args) throws InterruptedException {
        MyFrame frame = new MyFrame();
        while (true) {
            frame.repaint();

            // давайте отрисовывать окно не чаще чем раз в 10 миллисекунд - т.е. не чаще чем 100 раз в секунду
            Thread.sleep(10); // для этого ждем 10 миллисекунд прежде чем вновь вызвать frame.repaint();
            // это полезно для того чтобы не грузить процессор компьютера слишком сильно
        }
    }
}
