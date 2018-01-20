
/**
 * Protokół implementujący komunikację mięzy serwerem a klientem
 * @author malic
 *
 */
public class CloudProtocol {
	
static final String LOGIN = "login"; // server i klient zaimplementowal
	
	static final String LOGGEDIN = "loggedin"; // server i klient zaimplementowal
	
	static final String WRONG_LOGIN = "wrong_login";// server i klient zaimplementowal
	
	static final String WRONG_PASSWORD = "wrong_password";// server i klient zaimplementowal
	
	static final String LOGIN_EXISTS = "login_exists";// server i klient zaimplementowal
	
	static final String CREATE = "create";// server i klient zaimplementowal
	
	static final String CREATED = "created";// server i klient zaimplementowal
	
	static final String NULL_COMMAND = "null_command";// server i klient zaimplementowal
	
	static final String LOGOUT = "logout";// server i klient zaimplementowal
	
	static final String LOGGEDOUT = "loggout";// server i klient zaimplementowal
	
	static final String ADD_FILE = "add_file";// server zaimplementowal
	
	static final String ADD_ACCEPTED = "add_accepted";
	
	static final String ADD_START = "add_start";
	
	static final String FILE_EXISTS = "file_exists";// server zaimplementowal
	
	static final String NOT_FOUND = "not_found";// server zaimplementowal
	
	static final String ADD_SUCCESS = "add_success";// server zaimplementowal
	
	static final String DELETE_SUCCESS = "delete_success";// server zaimplementowal
	
	static final String GET_FILE = "get_file";// server zaimplementowal
	
	static final String SEND_START = "send_start"; // server zaimplementowal
	
	static final String DELETE_FILE = "delete_file";// server zaimplementowal
	
	static final String MAKE_DIR = "make_dir";
	
	static final String STOP = "stop";// server i klientzaimplementowal
	
	static final String STOPPED = "stopped";// server i klient zaimplementowal
	
}
