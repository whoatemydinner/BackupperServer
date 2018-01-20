import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class CloudServer extends JFrame implements Runnable{
	
	/**
	 * Gniazdko serwera
	 */
	private ServerSocket serverSocket;
	
	/**
	 * Vector przechowujący użytkowników aktualnie korzystających z systemu backuppera
	 */
	private Vector<User> users = new Vector<User>();
	
	/**
	 * Mapa przechowująca loginy oraz hasła użytkowników
	 */
	private Map<String,String> mapa;
	
	/**
	 * Konstruktor serwera, laduje loginy oraz hasla do drzewa, na podstawie ktorego bedzie sprawdzana poprawnosc danych klienta probujacego sie zalogowac
	 */
	public CloudServer(String title, int p) {
		
		super(title);
		System.out.println("Powstaje serwer");
		int port = p;
		
		mapa = new TreeMap<String,String>();
		
		loadLogs("login.txt");
		System.out.println("Zostaly zaladowane loginy i hasla");
		
		try {
			
			serverSocket = new ServerSocket(port);
			
			
		} catch (IOException e) {
			System.err.println("Error starting CloudServer");
			System.exit(1);
		}
		//Tworzony jest przycisk w celu zakonczenia dzialania serwera
		Button b = new Button("stop and exit");
		b.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {
				// TUTAJ MOZE JAKAS DAC POLECENIE WYSLANAI KOMENDY DO WSZYSTKICH UZYTKOWNIKOW O ZAKONCZENIU DZIALANIA CHMURY
				send(CloudProtocol.STOP);
				while(users.size() != 0) {
					try {
						Thread.sleep(500);
					}catch(InterruptedException e) {
						
					}
				}
				
				System.exit(0);
				
			}
		});
		
		add(b);
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
				User user = new User(userSocket, this);
				addUser(user);
				
			} catch (IOException e) {
				System.err.println("Error accepting connection. "
                        + "Client will not be served...");
			}
		
		
	} 
	
	/**
	 * Wysyla komunikaty do wszystkich uzytkownikow, ktorzy sa obecnie polaczeni
	 */
	synchronized void send(String command) {
		Enumeration<User> e = users.elements();
		while(e.hasMoreElements()) {
			((User)e.nextElement()).send(command);
		}
	}
	
	/**
	 * Sprawdza, czy login istnieje w bazie
	 */
	boolean loginExists(String log) {
		return mapa.containsKey(log);
	}
	
	/**
	 * Sprawdza poprawnosc wprowadzonego hasla
	 */
	boolean passwordOK(String log, String pass) {
		return (mapa.get(log)).equals(pass);
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
				
				mapa.put(parts[0],parts[1]);
				
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
			mapa.put(login, password);
			
			writer.close();
			
			new File(login).mkdir();
			
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	
	
	/**
	 * DO ZAIMPLEMENTOWANIA
	 * Metoda dodaje u�ytkownika do listy i rozpoczyna dzia�anie jego w�tku
	 * @param user
	 * @throws IOException 
	 */
	synchronized void addUser(User user) throws IOException {
		user.init();
		users.addElement(user);
		new Thread(user).start();
		System.out.println("nowy Uzytkownik zostal dodany");
		
	}
	
	/**
	 * DO ZAIMPLEMENTOWANIA
	 * Metoda usuwaj�ca u�ytkownika z listy
	 * @param user
	 */
	synchronized void removeUser(User user) {
		users.removeElement(user);
		user.close();
		System.out.println("Uzytkownik zostal usuniety");
	}
	
	public static void main(String args[]) {
		
		
		new CloudServer("CloudServer", 40000);
		
	}
}
