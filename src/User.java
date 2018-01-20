import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class User implements Runnable{
	
	/**
	 * Atrybut przechowujący odniesienie do serwera
	 */
	private CloudServer server;
	
	/**
	 * Gniazdko użytkownika
	 */
	private Socket userSocket;
	
	/**
	 * Strumien wejsciowy uzytkownika 
	 */
	private BufferedReader input;
	
	/**
	 * Strumien wyjsciowy uzytkownika
	 */
	private PrintWriter output;
	
	/**
	 * Login uzytkownika 
	 */
	String userName = " ";
	
	/**
	 * Konstruktor użytkownika
	 * @param socket 
	 * @param server
	 */
	public User(Socket socket, CloudServer server) {
		this.server = server;
		userSocket = socket;
		System.out.println("Zostalo stworzone kolejne polaczenie");
	}
	
	/**
	 * Metoda inicjuje strumienie wejscia i wyjscia uzytkownika 
	 * @throws IOException
	 */
	void init() throws IOException {
		Reader reader = new InputStreamReader(userSocket.getInputStream());
		input = new BufferedReader(reader);
		output = new PrintWriter(userSocket.getOutputStream(), true);
	}
	
	/**
	 * Metoda zamyka strumienie uzytkownika
	 */
	void close() {
		try {
			output.close();
			input.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("Error closing client ");
		} finally {
			output = null;
			input = null;
			userSocket = null;
		}
	}

	@Override
	public void run() {
		
		while(true) {
			
			String request = receive();
			System.out.println(request);
			StringTokenizer st = new StringTokenizer(request);
			String command = st.nextToken();
			
			
			if(command.equals(CloudProtocol.LOGIN)) {
				// Tutaj jesli uzytkownik podal zly login lub haslo zostaje automatycznie usuniety
				String login = st.nextToken();
				String password = st.nextToken();
				if(!(server.loginExists(login))) {
					send(CloudProtocol.WRONG_LOGIN);
					break;
				} else if (!(server.passwordOK(login, password))) {
					send(CloudProtocol.WRONG_PASSWORD);
					break;
				} else {
					// Tutaj uzytkownikowi udalo sie zalogowac. Kod w zwiazku co dalej do dopisania
					
					
					userName = login;
					
					send(CloudProtocol.LOGGEDIN + getUserFiles()); // serwer wysyla komende LOGGEDIN 
					//wraz ze wszystkimi plikami, ktore znajduja sie w folderze uzytkownika
				}
				
			} else if(command.equals(CloudProtocol.CREATE)) {
				// Tutaj sprawdzamy, czy login juz jest uzywany. jesli nie to nowe konto jest zakladane.
				// Jesli tak to uzytkownik zostaje automatycznie usuniety
				String login = st.nextToken();
				String password = st.nextToken();
				if(server.loginExists(login)) {
					send(CloudProtocol.LOGIN_EXISTS);
					break;
				} else {
					// Tutaj uzytkownikowi udalo sie zalozyc konto. Kod w zwiazku co dalej do dopisania
					
					server.addAccount(login, password, "login.txt");
					userName = login;
					
					send(CloudProtocol.CREATED);
				}
				
			} else if(command.equals(CloudProtocol.ADD_FILE)) {
				// TODO Tutaj ma nastapic sprawdzenie, czy plik o danej nazwie nie jest juz zapisany w chmurze.
				// A jesli jest, to czy data ostatniej modyfikacji jest inna niz data zapisana w chmurze
				// Do sprawdzania daty potrzebny jest jakis rejestr.
				String fileName = st.nextToken();
				String filePath = st.nextToken();
                                String fileSize = st.nextToken();
                                
				if(!alreadyExists(fileName)) {
					send(CloudProtocol.ADD_ACCEPTED + " " + fileName + " " + filePath + " " + fileSize);
					
				} else {
					send(CloudProtocol.FILE_EXISTS);
				}
				
			} else if(command.equals(CloudProtocol.ADD_START)) {
				
				String fileName = st.nextToken();
                                String fileSize = st.nextToken();
				
				try {
					addFile(fileName, fileSize); //wywolanie metody archiwizujacy nowy plik do folderu uzytkownika
					send(CloudProtocol.ADD_SUCCESS + getUserFiles());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else if(command.equals(CloudProtocol.DELETE_FILE)) {
				
				String fileName = st.nextToken();
				if(alreadyExists(fileName)) {
					deleteFile(fileName);
					send(CloudProtocol.DELETE_SUCCESS+ getUserFiles());
					
				} else {
					send(CloudProtocol.NOT_FOUND);
				}
				
			} else if(command.equals(CloudProtocol.GET_FILE)) {
				String fileName = st.nextToken();
                                String fileSize = st.nextToken();
				if(alreadyExists(fileName)) {
					try {
						send(CloudProtocol.SEND_START + " " + fileName + " " + fileSize);
						sendFile(fileName);
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// dodac jakas komende po wyslaniu pliku
					
				} else {
					send(CloudProtocol.NOT_FOUND);
				}
				
			} else if(command.equals(CloudProtocol.NULL_COMMAND)) {
				send(CloudProtocol.NULL_COMMAND); // tylko do testow. potem usunac
				break;
			} else if(command.equals(CloudProtocol.LOGOUT)) {
				send(CloudProtocol.LOGGEDOUT);
				break;
			} else if(command.equals(CloudProtocol.STOPPED)) {
				break;
			} 
			
		}
		server.removeUser(this);
		
	}
	
	/**
	 * Usuwa wskazany plik z chmury
	 */
	private void deleteFile(String fileName) {
		
		File main = new File(System.getProperty("user.dir"));
    	String path = main.getPath();
    	File file = new File(path + File.separator + userName + File.separator + fileName);
    	file.delete();
    	
	}
	
	/**
	 * Wysyla plik do uzytkownika
	 * @throws IOException 
	 */
	private void sendFile(String fileName) throws IOException {
		
		// Wysyla pliki bezposrednio znajdujace sie w folderze uzytkownika. Nie w nowo utworzonych folderach
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		OutputStream os = null;
		int port = 40001;
		ServerSocket servSock = null;
		Socket sock = null;
		
		try {
			servSock = new ServerSocket(port);
			try {
				sock = servSock.accept();
				File myFile = new File(userName + File.separator +fileName); // tutaj pobiera pliki, ktore znajduja sie bezposrednio w folderze uzytkownika
				byte [] mybytearray = new byte[(int)myFile.length()];
				fis = new FileInputStream(myFile);
				bis = new BufferedInputStream(fis);
				bis.read(mybytearray,0,mybytearray.length);
				os = sock.getOutputStream();
				os.write(mybytearray,0,mybytearray.length);
				os.flush();
				
			} finally {
				bis.close();
				fis.close();
				os.close();
			}
		} finally {
			if(servSock != null) servSock.close();
		}
		
		
		
	}
	
	/**
	 * TODO dodac parametr NAZWA HOSTA i ewentualnie port
	 * Dodaje nowy plik do chmury uzytkownika
	 */
	private void addFile(String fileName, String fS) throws IOException {
        int fileSize =  Integer.valueOf(fS);
        int bytesRead;
        int current = 0;
        int port = 40001;
        InetAddress a = userSocket.getInetAddress();
        String host = a.getHostAddress(); 
        Socket sock = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        
	    
		try {
			sock = new Socket(host,port);
                        dis = new DataInputStream(sock.getInputStream());
			File main = new File(System.getProperty("user.dir"));
                        String path = main.getPath();
                        File folder = new File(path + File.separator + userName +File.separator + fileName);
			fos = new FileOutputStream(folder);
                        
                        byte[] buffer = new byte[4096];
                        
                        int read = 0;
                        int total = 0;
                        int remaining = fileSize;
                        
                        while((read = dis.read(buffer, 0, Math.min(buffer.length, (int)remaining))) > 0){
                            total += read;
                            remaining -= read;
                            fos.write(buffer, 0, read);
                        }  
			   
                        fos.close();
		 
		    
		} finally {
			
			
				fos.close();
				dis.close();		
		}
		
	}
	
	/**
	 * Zwraca tablice nazw wszystkich plikow znajdujacych sie w folderze uzytkownika
	 */
	private String getUserFiles() {
		File main = new File(System.getProperty("user.dir"));
    	String path = main.getPath();
    	File folder = new File(path + File.separator + userName);
    	File[] list = folder.listFiles();
    	String listOfFiles = "";
    	for(File f : list) {
                //DecimalFormat df = new DecimalFormat("0.00");
                //double sizeinMb = (double)f.length()/(1024*1024);
    		listOfFiles = listOfFiles + " " + f.getName() + "," + f.length();
    	}
    	
    	return listOfFiles;
	}
	
	/**
	 * Sprawdza, czy plik istnieje juz w folderze uzytkownika
	 */
	private boolean alreadyExists(String name) {
		
		File main = new File(System.getProperty("user.dir"));
    	String path = main.getPath();
    	File folder = new File(path + File.separator + userName);
    	File[] list = folder.listFiles();
    	for(File f : list) {
    		
    		if((f.getName()).equals(name))
    			return true;
    	}
    	
    	return false;
	}
	
	
	/**
	 * Metoda sprawdza czy serwer jest w stanie odczytac zadanie
	 * @return
	 */
	private String receive() {
		try {
			return input.readLine();
		} catch (IOException e) {
			System.err.println("Error reading");
		} 
		
		return CloudProtocol.NULL_COMMAND;
		
	}
	
	/**
	 * Metoda wysyla odpowiedz do klienta
	 * @param command
	 */
	void send(String command) {
		
		output.println(command);
	}
}
