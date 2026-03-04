import javax.swing.*;


//obstacole meh
public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Testare JFrame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900,700);
        frame.setLocationRelativeTo(null);

        GamePanel panel = new GamePanel();
        frame.add(panel);
        panel.requestFocusInWindow();

        frame.setVisible(true);
    }
}