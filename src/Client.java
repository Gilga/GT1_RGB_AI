import java.awt.Point;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import lenz.htw.kipifub.ColorChange;
import lenz.htw.kipifub.net.NetworkClient;

public class Client extends Thread {
	NetworkClient networkClient =  null;
	String host = null;
    String name = null;
    int myID = 0;
    
    Client(String name, String host) throws IOException {
    	super(name);
        this.name = name;
        this.host = host;
    }
    
    ThreadLocalRandom tlr = null;
    
    int[] pr = new int[]{0,0,0}; // radius
    int[] px = new int[]{0,0,0}; // x
    int[] py = new int[]{0,0,0}; // y
    Point[] pd = new Point[]{new Point(),new Point(),new Point()}; // direction
    int[] pdc = new int[]{0,0,0}; // choose Direction (top,left, right, ...)
    
    void move(int i, int dx, int dy){
		Point d=pd[i];
		if(d.x != dx || d.y != dy){
			pd[i]=new Point(dx,dy);
			networkClient.setMoveDirection(i, dx, dy);
			System.out.printf("[%d.%d] (%d,%d)\n", myID, i, dx, dy);
		}
    }
    
    boolean canMove(int i, int dx, int dy) {
    	int x=px[i];
    	int y=py[i];
    	int nx=x+(pr[i]/2)*dx;
    	int ny=y+(pr[i]/2)*dy;
    	
    	boolean valid = networkClient.isWalkable(nx, ny);
		System.out.printf("[%d.%d] (%d,%d) -> (%d,%d) %s\n", myID, i, x, y, nx, ny, valid ? "ok" : "failed");

		return valid;
    }
    
    void setPos(int i, int x, int y) {
	    px[i] = x;
	    py[i] = y;
    }
    
    boolean changedPos(int i, int x, int y) {
	    return px[i]!=x || py[i]!=y;
    }

    void chooseAndMove(int i) {
    	int x = 0;
    	int y = 0;
    	int dc = pdc[i]; 
    	
    	if(dc == 0) { x=1; y=0;}
    	else if(dc == 1) { x=1; y=1;}
    	else if(dc == 2) { x=0; y=1;}
    	else if(dc == 3) { x=-1; y=1;}
    	else if(dc == 4) { x=-1; y=0;}
    	else if(dc == 5) { x=-1; y=-1;}
    	else if(dc == 6) { x=0; y=-1; System.out.printf("6\n"); }
    	else if(dc == 7) { x=1; y=-1; System.out.printf("7\n"); }
    	else if(dc == 8) { System.out.printf("8\n"); }
    	
    	if(canMove(i, x, y)) move(i, x, y);
    	else {

    		int d=0;
    		while(true) {
    			d=tlr.nextInt(0, 8);
    			if(d!=dc) break;
    		}
    		
    		pdc[i]=d;
    	} 
    }
    
    public void run() {
    	tlr = ThreadLocalRandom.current();
    	pdc[0]=tlr.nextInt(0, 8);
    	pdc[1]=tlr.nextInt(0, 8);
    	pdc[2]=tlr.nextInt(0, 8);
    	
		networkClient = new NetworkClient(host, name);
		myID = networkClient.getMyPlayerNumber();
		System.out.printf("Player: %d started\n", myID);
		 
		// radius
		pr[0] = networkClient.getInfluenceRadiusForBot(0);
		pr[1] = networkClient.getInfluenceRadiusForBot(1);
		pr[2] = networkClient.getInfluenceRadiusForBot(2);
		
		//int rgb = networkClient.getBoard(0, 0); // 0-1023 ->
		//int b = rgb & 255;
		//int g = (rgb >> 8) & 255;
		//int r = (rgb >> 16) & 255;
	    //long score = networkClient.getScore(0); // Punkte von rot
		
		boolean foundStartPos = false;
		ColorChange colorChange = new ColorChange();
		
		while(true){

			while ((colorChange = networkClient.pullNextColorChange()) != null) {
			    //verarbeiten von colorChange
			    int player = colorChange.player; 
			    int bot = colorChange.bot; 
			    int x = colorChange.x; 
			    int y = colorChange.y;
			    
			    if(player == myID){
			    	if(changedPos(bot, x, y)){
			    		setPos(bot, x, y);
			    		if(bot == 0) foundStartPos = true;
			    	}
			    }
			}
			
			if(foundStartPos) {
				chooseAndMove(0);
				chooseAndMove(1);
				chooseAndMove(2);
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
}
