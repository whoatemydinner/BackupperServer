import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class Server extends JFrame implements Runnable{
    private ServerSocket serverSocket;
    private Vector<Client> users = new Vector<Client>();
    private Map<String,String> mapping;
    static private int port_number;
    

    public Server(String title, int p) {
        super(title);
        int port = p;
        mapping = new TreeMap<String,String>();
        loadLogs("login.txt");
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error on start-up");
            System.exit(1);
        }
        //Tworzony jest przycisk w celu zakonczenia dzialania serwera
        Button bs = new Button("stop and exit");
        Button b = new Button("Uruchom serwer");
        bs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                send(BackupProt.STOP);
                while(users.size() != 0) {
                    try {
                        Thread.sleep(500);
                    }catch(InterruptedException e) {}
                }
                System.exit(0);
            }
        });

        add(bs);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        //start watku serwera
        new Thread(this).start();
        pack();
        setVisible(true);
        
        
    }
    
    @Override
    //Serwer oczekuje na nowych klientow i w razie polaczenia tworzy nowego uzytkownika
    public void run() {
        
        while(true)
            try {
                
                Socket userSocket = serverSocket.accept();
                System.out.println("nastapilo polaczenie");
                Client user = new Client(userSocket, this);
                addUser(user);
                
            } catch (IOException e) {
                System.err.println("Error accepting connection. "
                        + "Client will not be served.");
            }
        
        
    }
    
    /**
     * Wysyla komunikaty do wszystkich uzytkownikow, ktorzy sa obecnie polaczeni
     */
    synchronized void send(String command) {
        Enumeration<Client> e = users.elements();
        while(e.hasMoreElements()) {
            ((Client)e.nextElement()).send(command);
        }
    }
    
    /**
     * Sprawdza, czy login istnieje w bazie
     */
    boolean loginExists(String log) {
        return mapping.containsKey(log);
    }
    
    /**
     * Sprawdza poprawnosc wprowadzonego hasla
     */
    boolean passwordOK(String log, String pass) {
        return (mapping.get(log)).equals(pass);
    }
    
    /**
     * Metoda �aduj�ca loginy i has�a do mapy
     */
    private void loadLogs(String fileName) {
        try {
            
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            
            String next;
            
            while((next = reader.readLine()) != null) {
                String[] parts = next.split(" ", 2);
                
                mapping.put(parts[0],parts[1]);
                
            }
            
            reader.close();
            
        } catch(IOException e) {
            
            System.out.println(e.getMessage());
            
        }
    }
    
    void addAccount(String login, String password, String fileName) {
        try {
            
            PrintWriter writer = new PrintWriter(new FileWriter(fileName, true));
            
            writer.println();
            writer.append(login + " " + password);
            mapping.put(login, password);
            
            writer.close();
            
            new File(login).mkdir();
            
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
   
    synchronized void addUser(Client user) throws IOException {
        user.init();
        users.addElement(user);
        new Thread(user).start();
        System.out.println("nowy Uzytkownik zostal dodany");
        
    }
    
    synchronized void removeUser(Client user) {
        users.removeElement(user);
        user.close();
        System.out.println("Uzytkownik zostal usuniety");
    }
    
    public static void main(String args[]) {
        Properties prop = new Properties();
        InputStream inS = null;
        try {
            inS = new FileInputStream("Server.properties");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            prop.load(inS);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        port_number = Integer.parseInt(prop.getProperty("PORT_NUMBER"));
        Server cloudServer = new Server("Backup Service", port_number);
        cloudServer.setLayout(new FlowLayout());
        cloudServer.setSize(200,100);
        
    }
}
