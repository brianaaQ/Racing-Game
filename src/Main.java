import javax.swing.*;


public class Main {
    public static void main(String[] args) {
        String host = JOptionPane.showInputDialog(
                null,
                "Introduceti IP-ul serverului:",
                "Conectare",
                JOptionPane.QUESTION_MESSAGE
        );

        if(host == null || host.isBlank()) host = "localhost";

        JFrame frame = new JFrame("Racing game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.setLocationRelativeTo(null);

        GamePanel panel = new GamePanel();
        frame.add(panel);
        panel.requestFocusInWindow();
        frame.setVisible(true);

        panel.connectToServer(host.trim());
    }
}