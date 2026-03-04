import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {
    private int carX = 350;
    private int carY = 250;
    private int speed = 4;
    private double angle = 0;
    private double rotationSpeed = 0.05;
    private int backroundY = 0;
    private double offSetX = 0;
    private double maxOffSet = 100;
    private double rotationEffect = 2.0;

    //private java.util.List<Tree> trees = new ArrayList<>();
    private java.util.List<Obstacle> obstacles = new java.util.ArrayList<>();

    private boolean up, down, left, right;

    private Thread gameThread;

    public GamePanel() {
//        for (int i = 0; i < 20; i++) {
//            int y = i * 50;
//            trees.add(new Tree(150,y));
//            trees.add(new Tree(650,y));
//        }

        obstacles.add(new Obstacle(380,400));
        obstacles.add(new Obstacle(420,800));
        obstacles.add(new Obstacle(400,1200));

        setFocusable(true);

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                int key = e.getKeyCode();

                if(key == java.awt.event.KeyEvent.VK_UP)
                    up = true;
                if(key == java.awt.event.KeyEvent.VK_DOWN)
                    down = true;
                if(key == java.awt.event.KeyEvent.VK_LEFT)
                    left = true;
                if(key == java.awt.event.KeyEvent.VK_RIGHT)
                    right = true;
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int key = e.getKeyCode();

                if(key == java.awt.event.KeyEvent.VK_UP)
                    up = false;
                if(key == java.awt.event.KeyEvent.VK_DOWN)
                    down = false;
                if(key == java.awt.event.KeyEvent.VK_LEFT)
                    left = false;
                if(key == java.awt.event.KeyEvent.VK_RIGHT)
                    right = false;
            }
        });
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while(true){
            update();
            repaint();

            try{
                Thread.sleep(16);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private void update(){
        if(left) {
            angle -= rotationSpeed;
            offSetX += rotationEffect;
        }
        if(right) {
            angle += rotationSpeed;
            offSetX -= rotationEffect;
        }

        if(up) {
            carX += Math.sin(angle) * speed;
            carY -= Math.cos(angle) * speed;
        }
        if(down) {
            carX -= Math.sin(angle) * speed;
            carY += Math.cos(angle) * speed;
        }

        if(offSetX > maxOffSet) offSetX = maxOffSet;
        if (offSetX < -maxOffSet) offSetX = -maxOffSet;

        //limitam masinuta pentru a nu iesi din fereastra
        int carWidth = 50;
        int carHeight = 100;

        if(getWidth() > 0 && getHeight() > 0) {
            if (carX < 0) carX = 0;
            if (carY < 0) carY = 0;
            if (carX > getWidth() - carWidth) carX = getWidth() - carWidth;
            if (carY > getHeight() - carHeight) carY = getHeight() - carHeight;
        }
        for(Obstacle o :  obstacles){
            int obsX = o.x + (int)offSetX;
            int obsY = (int)(o.y + backroundY % 2000);

            if(carX + 25 > obsX &&
                    carX - 25 < obsX + 20 &&
                    carY + 50 > obsY &&
                    carY - 50 < obsY + 20) {
                speed = 0; // oprire la impact
            }
        }
        //offSetX += rotationSpeed* (left ? 1 : 0) +rotationSpeed * (right ? -1 : 0);
        backroundY += (up ? speed : 0) - (down ? speed : 0);

    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        //iarba
        g2d.setColor(new Color(34,139,34));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        //copaci
//        for(Tree t : trees){
//            int drawY = (int)(t.y + backroundY % 3000);
//
//            //trunchi
//            g2d.setColor(new Color(101,67,33));
//            g2d.fillRect(t.x, drawY,10, 20);
//
//            //coroana
//            g2d.setColor(Color.GREEN);
//            g2d.fillOval(t.x -10, drawY -20,30, 30);
//        }

        //acum facem asfaltul
        g2d.setColor(new Color(60,60,60));
        g2d.fillRect(getWidth()/2 - 200 + (int)offSetX, 0, 390, getHeight());

        g2d.setColor(new Color(50,50,50));
        for (int i = 0; i < 200; i++){
            int randX = (int)(Math.random() * 200);
            int randY = (int)(Math.random() * getHeight());
            g2d.fillRect(getWidth()/2 - 100 + randX + (int)offSetX, randY, 2, 2);
        }

        //bordura
        for(int i = -1000; i < getHeight() + 1000; i+=40){
            if((i / 40)% 2  == 0)
                g2d.setColor(Color.RED);
            else
                g2d.setColor(Color.WHITE);
            g2d.fillRect(getWidth()/2 - 200 + (int)offSetX, i + backroundY, 10, 40);
            g2d.fillRect(getWidth()/2 + 180 + (int)offSetX, i + backroundY, 10, 40);
        }

        for(Obstacle o : obstacles){
            int drawY = (int)(o.y + backroundY % 2000);

            g2d.setColor(Color.ORANGE);
            g2d.fillOval(o.x + (int)offSetX, drawY,20, 20);
        }

        //benzi
        g2d.setColor(Color.WHITE);
        for(int i = -1000; i < getHeight() + 1000; i+=40)
            g2d.fillRect(getWidth()/2 - 3 + (int)offSetX, i + backroundY, 6, 30);


        //salvam transformarea
        java.awt.geom.AffineTransform old = g2d.getTransform();
        g2d.translate(carX+50, carY+25);//mutam aprox pe centru
        g2d.rotate(angle);

        //corp masinuta
        g2d.setColor(Color.PINK);
        g2d.fillRoundRect(-25,-50,50,100,20,20);

        //parbrizul masinutei
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(-15,-30,30,60,10,10);

        //roti
        g2d.setColor(Color.BLACK);
        g2d.fillRect(-33, -40,7, 20);
        g2d.fillRect(-33, 20,7, 20);
        g2d.fillRect(26, -40,7, 20);
        g2d.fillRect(26, 20,7, 20);

        //faruri
        g2d.setColor(Color.YELLOW);
        g2d.fillRoundRect(10, -50,10, 5, 20, 10);
        g2d.fillRoundRect(-20, -50,10, 5, 20, 10);

        g2d.setTransform(old);
    }
}