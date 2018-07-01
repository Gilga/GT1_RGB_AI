
public class Server extends Thread {
	String[] args = null;
    public Server(String[] args) {
    	super("Server");
    	this.args=args;
    }
    public void run() {
    	lenz.htw.zpifub.Server.main(args);
    }
}
