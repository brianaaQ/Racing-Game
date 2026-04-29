import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Server {
    private static final int PORT = 9999;
    public static final int MAX_PLAYERS = 3;

    //date pentru fiecare jucator
    static double[] px = new double [MAX_PLAYERS];
    static double[] py = new double [MAX_PLAYERS];
    static int[] plaps =  new int[MAX_PLAYERS];
    static double[] pangle = new double[MAX_PLAYERS];
    static boolean[] connected = new boolean[MAX_PLAYERS];
    static boolean[] finished = new boolean[MAX_PLAYERS];
    static int finishCounter = 0;

    static PrintWriter[] writers = new PrintWriter[MAX_PLAYERS];
    static int connectedCount = 0;
    static boolean raceStarted = false;

    public static void main (String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server pornit pe portul " + PORT+ ". Astept cei 3 jucatori...");

        for(int i = 0; i < MAX_PLAYERS; i++){
            Socket socket = serverSocket.accept();
            int id = i;
            connected[id] = true;
            writers[id] = new PrintWriter(socket.getOutputStream(), true);
            connectedCount++;
            System.out.println("Jucator " + id + "conectat. (" + connectedCount + "/3)");

            writers[id].println("ID|" + id + "|" + connectedCount);

            for(int j = 0; j < id; j++){
                if(connected[j] && writers[j] != null){
                    writers[j].println("CONNECTED|" + connectedCount);
                }
            }

            final Socket s = socket;
            new Thread(() -> handleClient(s, id)).start();
        }
        System.out.println("Toti jucatorii sunt conectati! Cursa incepe!");
        broadcast("START!");
    }

    private static void handleClient(Socket socket, int id) {
        try{
            BufferedReader reader = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null){
                String[] parts = line.split("\\|");

                if(parts[0].equals("POS") && parts.length == 5){
                    px[id] =  Double.parseDouble(parts[1]);
                    py[id] = Double.parseDouble(parts[2]);
                    plaps[id] = Integer.parseInt(parts[3]);
                    pangle[id] = Double.parseDouble(parts[4]);

                    broadcastPositions();
                }

                if(parts[0].equals("FINISHED") && parts.length == 3){
                    if(!finished[id]){
                        finished[id] = true;
                        finishCounter++;
                        broadcast("FINISHED|" + id + "|" + finishCounter + "|" + parts[1]);
                        System.out.println("Jucatorul " + id + " a terminat pe locul " + finishCounter);
                    }
                }
            }
        } catch (IOException e){
            System.out.println("Jucatorul " + id + " a fost deconectat.");
            connected[id] = false;
        }
    }

    private static synchronized void broadcastPositions(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < MAX_PLAYERS; i++){
            if(connected[i]){
                sb.append(i).append(",")
                    .append(px[i]).append(",")
                    .append(py[i]).append(",")
                    .append(plaps[i]).append(",")
                    .append(pangle[i]).append("|");
            }
        }
        String msg = "PLAYERS|" + sb.toString();
        for(int i = 0; i < MAX_PLAYERS; i++){
            if(connected[i] && writers[i] != null) writers[i].println(msg);
        }
    }

    private static synchronized void broadcast(String msg){
        for(int i = 0; i < MAX_PLAYERS; i++){
            if(connected[i] && writers[i] != null) writers[i].println(msg);
        }
    }

}
