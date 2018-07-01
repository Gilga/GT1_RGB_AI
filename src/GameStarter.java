import java.io.IOException;

public class GameStarter {

	public static void main(String[] args) {
    	boolean test = true;
    	String host = "141.45.213.102";
    	String name = "DirtyJoker";
    	
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
				t1 = new Client("SpiralSearch",null);
		    	Thread t2 = new Client("CircleSearch",null);
		    	Thread t3 = new Client("RadiusSearch",null);
		    	
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
