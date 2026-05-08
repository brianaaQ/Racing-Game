import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel implements Runnable {
    //dimensiunile ferestrei
    private static final int W = 800;
    private static final int H = 600;

    //dimensiuni pista
    private static final int Track_Length = 3000;
    private static final int Track_X = 200;
    private static final int Track_W = 400;

    //pozitie masina pe ecran
    private static final int Car_Screen_Y = H / 2;

    //starea jocului
    private double worldY = 0;
    private double carLaneX = W / 2;
    private double speed = 0;
    private double angle = 0;
    private int laps = 0;
    private int finishPlace =1;

    private static final double Max_Speed = 6.0;
    private static final double Acceleration = 0.15;
    private static final double Deceleration = 0.10;
    private static final double Rotation_Speed = 0.03;
    private static final int Total_Laps = 6;

    //timpi cursa
    private long raceStartTime = -1;
    private long raceFinishTime = -1;
    private boolean raceFinished = false;

    //linie finish
    private static final double Finish_World = 500;

    private double prevWorldY = 0;
    private static final double LAP_COOLDOWN = 200;

    //obstacole
    private java.util.List<double[]> obstacles = new java.util.ArrayList<>();

    private int[] dotX = new int[400];
    private int[] dotY = new int[400];

    private boolean up, down, left, right;

    private BufferedImage buffer = new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
    private Thread gameThread;

    //Multiplayer
    private int myId = -1;
    private int connectedPlayers = 1;
    private boolean waitingForPlayers = true;

    //date jucatori
    private final Map<Integer, double[]> otherPlayers = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> finishPlaces = new ConcurrentHashMap<>();
    private final Map<Integer, Long> finishTimes = new ConcurrentHashMap<>();
    private final Map<Integer, String> finishTimeStrings = new ConcurrentHashMap<>();

    private PrintWriter netOut;
    private static final Color[] PLAYER_COLORS = {Color.BLUE, Color.RED, Color.GREEN};

    public void connectToServer(String host){
        new Thread(() -> {
            try{
                Socket socket = new Socket(host, 9999);
                netOut = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while((line = in.readLine()) != null){
                    String[] parts = line.split("\\|");

                    switch(parts[0]){
                        case "ID":
                            myId = Integer.parseInt(parts[1]);
                            if(parts.length >= 3){
                                connectedPlayers = Integer.parseInt(parts[2]);
                            }
                            System.out.println("Sunt jucatorul " + myId);
                            break;
                        case "START!":
                            waitingForPlayers = false;
                            raceStartTime = System.currentTimeMillis();
                            break;
                        case "PLAYERS":
                            for(int i = 1;  i < parts.length; i++){
                                if(parts[i].isEmpty()) continue;
                                String[] d = parts[i].split(",");
                                int id = Integer.parseInt(d[0]);
                                if(id == myId) continue;
                                otherPlayers.put(id, new double[]{
                                    Double.parseDouble(d[1]),
                                    Double.parseDouble(d[2]),
                                    Double.parseDouble(d[3]),
                                    Double.parseDouble(d[4])
                                });
                            }
                            break;
                        case "FINISHED":
                            int pid = Integer.parseInt(parts[1]);
                            int place = Integer.parseInt(parts[2]);
                            long elapsedMs = Long.parseLong(parts[3]);
                            long secondsTotal = elapsedMs / 1000;
                            long minutes = secondsTotal / 60;
                            long seconds = secondsTotal % 60;
                            String formattedTime = String.format("%2d:%2d", minutes, seconds);
                            finishTimeStrings.put(pid, formattedTime);
                            finishPlaces.put(pid, place);
                            finishTimes.put(pid, elapsedMs);
                            if(pid == myId){
                                finishPlace = place;
                                raceFinishTime = raceStartTime + elapsedMs;
                                raceFinished = true;
                                speed = 0;
                            }
                            break;
                        case "CONNECTED":
                            connectedPlayers = Integer.parseInt(parts[1]);
                            break;
                    }
                }
            } catch(IOException e){
                System.out.println("Eroare conexiune la server: " + e.getMessage());
            }
        }).start();
    }

    public GamePanel() {
        setPreferredSize(new Dimension(W,H));

        setFocusable(true);
        int[] obsX = {W/2-80, W/2+60, W/2-40, W/2+90, W/2-70,
                W/2+30, W/2-100, W/2+70, W/2-50, W/2+110, W/2-30};

        for(int i = 0; i < obsX.length; i++){
            obstacles.add(new double[]{700 + i * 220.0, obsX[i]});
        }

        for(int i = 0; i < dotY.length; i++){
            dotY[i] = (int)(Math.random() * Track_Length);
            dotX[i] = Track_X + 10+ (int)(Math.random() * (Track_W - 20 ));
        }


        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                switch(e.getKeyCode()){
                    case java.awt.event.KeyEvent.VK_UP:
                        up = true;
                        break;
                    case java.awt.event.KeyEvent.VK_DOWN:
                        down = true;
                        break;
                    case java.awt.event.KeyEvent.VK_LEFT:
                        left = true;
                        break;
                    case java.awt.event.KeyEvent.VK_RIGHT:
                        right = true;
                        break;
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                switch(e.getKeyCode()){
                    case java.awt.event.KeyEvent.VK_UP:
                        up = false;
                        break;
                    case java.awt.event.KeyEvent.VK_DOWN:
                        down = false;
                        break;
                    case java.awt.event.KeyEvent.VK_LEFT:
                        left = false;
                        break;
                    case java.awt.event.KeyEvent.VK_RIGHT:
                        right = false;
                        break;
                }
            }
        });
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while(true){
            update();
            render();
            repaint();

            try{
                Thread.sleep(16);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private double worldToScreenY(double obj){
        double delta =  obj - worldY;
        if(delta > Track_Length / 2.0) delta -=  Track_Length;
        if(delta < -Track_Length / 2.0) delta += Track_Length;
        return Car_Screen_Y - delta;
    }

    private void update(){
        if(raceFinished) return;

        if (raceStartTime < 0 && !waitingForPlayers & (up || left || right || down)){
            raceStartTime = System.currentTimeMillis();
        }

        if(up) {
            speed += Acceleration;
            if(speed > Max_Speed) speed = Max_Speed;
        } else if(down) {
            speed -= Acceleration;
            if(speed < -Max_Speed / 2) speed = -Max_Speed / 2;
        } else {
            if (speed > 0) {
                speed -= Deceleration;
                if(speed < 0) speed = 0;
            }
            if(speed < 0){
                speed += Deceleration;
                if(speed > 0) speed = 0;
            }
        }

        double steerfactor =  speed / Max_Speed;
        if(Math.abs(speed) > 0.1){
            if(left) angle -= Rotation_Speed * steerfactor;
            if (right) angle += Rotation_Speed * steerfactor;
        }

        carLaneX += Math.sin(angle) * speed;

        //limitam masinuta pentru a nu iesi din fereastra
        if(carLaneX < Track_X + 20){
            carLaneX = Track_X + 20;
            if( angle < 0) angle *= 0.4;
        }
        if(carLaneX > Track_X + Track_W - 20){
            carLaneX = Track_X + Track_W - 20;
            if( angle > 0) angle *= 0.4;
        }

        if(!left && !right) angle *= 0.88;

        worldY += speed;
        if(worldY >= Track_Length) worldY -= Track_Length;
        if(worldY < 0) worldY += Track_Length;

        //obstacole
        for(double[] obs : obstacles){
            double sy = worldToScreenY(obs[0]);
            if(Math.abs(carLaneX - obs[1]) < 30 && Math.abs(Car_Screen_Y - sy) < 35){
                speed = -speed * 0.4;
                worldY -= speed * 2;
                if(worldY < 0) worldY +=Track_Length;
                if(worldY >= Track_Length) worldY -= Track_Length;
            }
        }

        //linie finish
        if(speed > 0){
            boolean normalCross = (prevWorldY <= Finish_World && worldY > Finish_World);

            if(normalCross){
                laps++;
                System.out.println("LAP " + laps);
                if(laps >= Total_Laps){
                    raceFinishTime = System.currentTimeMillis();
                    raceFinished = true;
                    finishPlace = 1;
                    speed = 0;
                    long elapsed = raceFinishTime - raceStartTime;
                    long secondsTotal = elapsed / 1000;
                    long minutes = secondsTotal / 60;
                    long seconds = secondsTotal % 60;
                    String timeStr = String.format("%2d:%2d", minutes, seconds);
                    finishTimeStrings.put(myId, timeStr);
                }
            }
        }
        prevWorldY = worldY;

        //trimitem pozitiile la server
        if(netOut != null && !waitingForPlayers){
            netOut.println("POS|" + carLaneX + "|" + worldY + "|" + laps + "|" + angle);
        }

        //trimitem finished la server
        if(raceFinished && netOut != null && !finishPlaces.containsKey(myId)){
            long elapsed = System.currentTimeMillis() - raceStartTime;
            netOut.println("FINISHED|" + elapsed + "|" + laps);
        }
    }

    //setter pentru a comunica locul final
    private void setFinishPlace(int place){
        this.finishPlace = place;
    }

    //getteri si setteri pentru comunicarea cu serverul
    public long getRaceStartTime() {
        return raceStartTime;
    }
    public long getRaceFinishTime() {
        return raceFinishTime;
    }
    public boolean isRaceFinished(){
        return raceFinished;
    }

    private void render(){
        Graphics2D g = buffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //iarba
        g.setColor(new Color(34,139,34));
        g.fillRect(0,0,W,H);

        //asfaltul
        g.setColor(new Color(60,60,60));
        g.fillRect(Track_X,0,Track_W,H);

        //textura asfalt
        g.setColor(new Color(75,75,75));
        for(int i = 0; i < dotY.length; i++){
            int sy = (int)worldToScreenY(dotY[i]);
            if(sy >= 0 && sy < H)
                g.fillRect(dotX[i],sy,3,3);
        }

        //borduri
        int stripeH = 40;
        int borderW = 12;
        for(int i = -1; i <= H / stripeH; i++){
            int scrollOff = (int)(worldY % stripeH);
            int y = i * stripeH - scrollOff;
            int worldTile = (int)(worldY / stripeH) + i;
            g.setColor((worldTile % 2 == 0) ?  Color.RED : Color.WHITE);
            g.fillRect(Track_X, y,borderW,stripeH);
            g.fillRect(Track_X + Track_W - borderW, y, borderW, stripeH);
        }

        //benzi
        g.setColor(Color.WHITE);
        int dashH = 30, period = 60;
        for(int i = -1; i <= H / period + 1; i++){
            int scrollOff = (int)(worldY % period);
            int y = i * period - scrollOff;
            g.fillRect(W / 2 -3, y, 6, dashH);
        }

        //linie finish
        double finishSy = worldToScreenY(Finish_World);
        if(finishSy > -20 && finishSy < H + 20){
            int fy = (int)finishSy;
            int lineH = 12;
            int innerX = Track_X + 12;
            int innerW = Track_W - 24;
            int sqW = innerW / 16;
            g.setColor(Color.WHITE);
            g.fillRect(innerX,fy - lineH, innerW,lineH * 2);
            for(int col = 0; col < 16; col++){
                for(int row = 0; row < 2; row++){
                    g.setColor(((col + row) % 2 == 0) ?  Color.BLACK : Color.WHITE);
                    g.fillRect(innerX + col * sqW, fy - lineH + row * lineH,sqW,lineH);
                }
            }

        }

        //obstacole
        for(double[] obs : obstacles){
            double sy = worldToScreenY(obs[0]);
            if(sy > -30 && sy < H + 30){
                int ox = (int) obs[1] - 15;
                int oy = (int) sy - 15;
                g.setColor(new Color(255, 140, 0));
                g.fillOval(ox, oy, 30, 30);
                g.setColor(Color.RED.darker());
                g.setStroke(new BasicStroke(2));
                g.drawOval(ox, oy, 30, 30);
                g.setStroke(new BasicStroke(1));
            }
        }

        //masinuta
        AffineTransform old = g.getTransform();
        g.translate((int)carLaneX, Car_Screen_Y);
        g.rotate(angle);

        //caroserie
        g.setColor(myId >= 0 ? PLAYER_COLORS[myId % PLAYER_COLORS.length] : Color.PINK);
        g.fillRoundRect(-20, -45, 40, 90, 15, 15);

        //parbriz
        g.setColor(new Color(200, 230, 255, 200));
        g.fillRoundRect(-12, -30, 24, 55, 8, 8);

        //roti
        g.setColor(Color.BLACK);
        g.fillRoundRect(-28, -38, 8, 18, 4, 4);
        g.fillRoundRect(-28,  18, 8, 18, 4, 4);
        g.fillRoundRect( 20, -38, 8, 18, 4, 4);
        g.fillRoundRect( 20,  18, 8, 18, 4, 4);

        //faruri
        g.setColor(Color.YELLOW);
        g.fillRoundRect(  8, -46, 9, 5, 3, 3);
        g.fillRoundRect(-17, -46, 9, 5, 3, 3);

        //
        g.setColor(Color.RED);
        g.fillRoundRect(  8,  41, 9, 4, 2, 2);
        g.fillRoundRect(-17,  41, 9, 4, 2, 2);

        g.setTransform(old);

        for(Map.Entry<Integer, double[]> entry : otherPlayers.entrySet()){
            int id = entry.getKey();
            double[] d = entry.getValue();
            double ox = d[0];
            double owy = d[1];
            double oangle = d[3];

            //calculam pozitia pe ecran
            double delta = owy - worldY;
            if(delta > Track_Length / 2.0) delta -= Track_Length;
            if(delta < -Track_Length / 2.0) delta += Track_Length;
            double screenY = Car_Screen_Y - delta;

            if(screenY < -60 || screenY > H + 60) continue;

            AffineTransform oldT = g.getTransform();
            g.translate((int) ox,  (int) screenY);
            g.rotate(oangle);

            Color carColor = PLAYER_COLORS[id %  PLAYER_COLORS.length];
            g.setColor(carColor);
            g.fillRoundRect(-20, -45, 40, 90, 15, 15);
            g.setColor(new Color(200, 230, 255, 200));
            g.fillRoundRect(-12, -30, 24, 55, 8, 8);
            g.setColor(Color.BLACK);
            g.fillRoundRect(-28, -38, 8, 18, 4, 4);
            g.fillRoundRect( 20, -38, 8, 18, 4, 4);
            g.fillRoundRect( 20,  18, 8, 18, 4, 4);
            g.fillRoundRect(  -28, 18, 8, 18, 4, 4);

            g.setTransform(oldT);
            g.setColor(carColor);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("P" + (id+1), (int)ox - 8, (int)screenY - 50);
        }

        //mesaj
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(10, 10, 190, 85, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Laps: " + laps + " / " + Total_Laps, 20, 38);
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        int speedPct= (int)(Math.abs(speed) / Max_Speed * 100);
        g.drawString("Speed: " + speedPct + "%", 20, 60);
        if(raceStartTime > 0 && !raceFinished){
            long elapsed = System.currentTimeMillis() - raceStartTime;
            g.drawString("Time: " + formatTime(elapsed), 20, 82);
        }

        if(waitingForPlayers){
            g.setColor(new Color(0,0,0,180));
            g.fillRoundRect(W / 2, H / 2 -30, 360, 60, 15, 15);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            String msg = "Asteptam jucatori... (" + connectedPlayers + "/" + Server.MAX_PLAYERS + ")";
            int mw = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (W - mw) / 2, H / 2 + 7);
        }

        // Clasament live
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(W - 160, 10, 150, 25 + Server.MAX_PLAYERS * 22, 10, 10);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(Color.YELLOW);
        g.drawString("CLASAMENT", W - 148, 28);

        //sortare jucatori dupa laps
        List<int[]> ranking = new ArrayList<>();

        for(int i = 0; i < Server.MAX_PLAYERS; i++){
            if(i == myId) {
                ranking.add(new int[]{i, laps});
            } else if (otherPlayers.containsKey(i)){
                ranking.add(new int[]{i, (int) otherPlayers.get(i)[2]});
            } else {
                ranking.add(new int[]{i, 0});
            }
        }
        ranking.sort((a, b) -> b[1] - a[1]);

        for(int i = 0; i < ranking.size(); i++){
            int pid = ranking.get(i)[0];
            int plapCount = ranking.get(i)[1];
            Color c = (pid == myId) ? Color.WHITE : PLAYER_COLORS[pid % PLAYER_COLORS.length];
            g.setColor(c);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            String label = (pid == myId) ? "Tu" : "P" + (pid + 1);
            g.drawString((i+1) + ". " + label + " - tur " + plapCount, W - 148, 50 + i * 22);
        }

        if(raceFinished) drawFinishOverlay(g);

        g.dispose();
    }

    private void drawFinishOverlay(Graphics2D g){
        long elapsed = raceFinishTime > 0 ? raceFinishTime - raceStartTime : 0;

        //fundal semi transparent
        g.setColor(new Color(0,0,0,180));
        g.fillRect(0,0,W, H);

        int pw = 550, ph = 320;
        int px = (W - pw) / 2,  py = (H - ph) / 2;
        g.setColor(new Color(20, 20, 40, 230));
        g.fillRoundRect(px, py, pw, ph, 30, 30);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(px, py, pw, ph, 30, 30);
        g.setStroke(new BasicStroke(1));

        //titlu
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(new Color(255, 215, 0));
        String title = "FELICITARI CURSA TERMINATA!";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (W - tw) / 2, py + 55);

        //locul castigat
        String[] medals = {"LOCUL UNU" , "LOCUL DOI", "LOCUL TREI"};
        Color[] medalColors = {
                new Color(255, 215, 0),
                new Color(192, 192, 192),
                new Color(205, 127, 50),
        };
        String placeText = (finishPlace >= 1 && finishPlace <= 3) ? medals[finishPlace - 1] : "LOCUL " + finishPlace;
        Color placeColor = (finishPlace >= 1 && finishPlace <= 3) ? medalColors[finishPlace - 1] : Color.WHITE;

        g.setFont(new Font("Arial", Font.BOLD, 42));
        g.setColor(placeColor);
        int placeW = g.getFontMetrics().stringWidth(placeText);
        g.drawString(placeText, (W - placeW) / 2, py + 120);

        //timpul obtinut
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        String timeStr = "Timp: " + finishTimeStrings.get(myId);
        int timeW = g.getFontMetrics().stringWidth(timeStr);
        g.drawString(timeStr, (W - timeW) / 2, py + 170);

        //nr tururi
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(new Color(180, 180, 180));
        String lapsStr = Total_Laps + " tururi completate";
        int lapsW = g.getFontMetrics().stringWidth(lapsStr);
        g.drawString(lapsStr, (W - lapsW) / 2, py + 205);

        //mesaj de asteptare pentru muliplayer
        g.setFont(new Font("Arial", Font.ITALIC, 15));
        g.setColor(new Color(140, 200, 255));
        String waiting = "Asteaptati conectarea celorlalti jucatori...";
        int ww = g.getFontMetrics().stringWidth(waiting);
        g.drawString(waiting, (W - ww) / 2, py + 250);
    }

    private String formatTime(long ms){
        long secondsTotal = ms / 1000;
        long minutes = secondsTotal / 60;
        long seconds = secondsTotal % 60;
        return String.format("%d:%d", minutes, seconds);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(buffer != null)
            g.drawImage(buffer, 0, 0, null);
    }
}
