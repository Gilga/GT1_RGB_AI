import java.io.IOException;

import lenz.htw.kipifub.ColorChange;
import lenz.htw.kipifub.net.NetworkClient;

public class GameStarter {

	public static void main(String[] args) {
    	boolean test = true;
    	String host = null;
    	String name = "CLIENT";
    	
    	for(String arg : args){
    		int find = arg.indexOf("=");
    		if(find!=-1){
    			String cmd = arg.substring(0,find);
    			String value = arg.substring(find+1);
    			
    			if(cmd.equals("test")) {
    				if(value.equals("true")) test = true;
    				else if(value.equals("false")) test = false;
    			}
    			else if(cmd.equals("host")) host = value;
    			else if(cmd.equals("name")) name = value;
    		}
    	}
    	
		try {
	    	if(test)
	    	{
	    		String[] serverArgs = {};
		    	Thread server = new Server(serverArgs);
		    	
		    	Thread t1;
				t1 = new Client(name + " 0",null);
		    	Thread t2 = new Client(name + " 1",null);
		    	Thread t3 = new Client(name + " 2",null);
		    	
		    	server.start();
		    	t1.start();
		    	t2.start();
		    	t3.start();
	    	}
	    	else
	    	{
	    		Thread c = new Client(name, host);
	    		c.start();
	    	}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
