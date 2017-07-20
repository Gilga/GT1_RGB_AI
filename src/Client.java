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
    
    int color = 0;
    int[] radius = new int[]{0,0,0}; // radius
    float[] ang = new float[]{0,0,0};
    Position[] pos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldPos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldDir = new Position[]{new Position(),new Position(),new Position()}; // direction
   
    int[] wait = new int[]{0,0,0};
    int[] hitWall = new int[]{0,0,0};
    
    Position[] targetPos = new Position[]{new Position(),new Position(),new Position()};

    boolean[] infinity = new boolean[]{false,false,false};
    
    void moveDirection(int i, Position d){

		if(!Position.equals(oldDir[i], d)){
			oldDir[i]=d;
			networkClient.setMoveDirection(i, d.x, d.y);
    		System.out.printf("(%d|%d){%d} [%f,%f] -> [%f,%f]\n", myID, i, radius[i], pos[i].x, pos[i].y, d.x, d.y);
			//System.out.printf("[%d.%d] (%f,%f)\n", myID, i, nd.x, nd.y);
		}
    }
    
    void moveTo(int i, Position p) {
    	moveDirection(i, new Position(p.x-pos[i].x, p.y-pos[i].y));
    }
     
    boolean isWalkable(Position p) {
		return networkClient.isWalkable((int)p.x, (int)p.y);
    }
    
    boolean canMoveTo(int i, Position t, float step) {
    	Position p=pos[i];

    	float dist=Position.distance(p,t);
    	if(dist<=0) return false;
  
    	Position d = Position.angToPosition(ang[i]);
   	
    	step=(radius[i]/2+1);
    	
    	if(!isWalkable(Position.add(p, Position.mul(d,new Position(step))))) return false;

    	for(float j=0;j<step;j++) //(dist/step)
    	{
    		p=Position.add(p, d);
    		if(!isWalkable(p)) return false;
    	}

    	return true;
    }
    
    void setPos(int i, Position p) { oldPos[i]=pos[i]; pos[i] = p;  }
    
    float oppositeAng(float ang) {
    	return (ang+180) % 360;
    }
    
    float randomAngle() {
    	return tlr.nextInt(0, 360);
    }
    
    void setInfiniteDirection(int i, float a) {
    	setDirection(i,a, Integer.MAX_VALUE);
    	infinity[i]=true;
    }
    
    void setDirection(int i, float a, float v) {
    	Position t = pos[i].getNext(i, Position.angToPosition(a), v);
    	if(!Position.equals(targetPos[i], t)) {
    		ang[i] = a;
    		targetPos[i]=t;
    		infinity[i]=false;
    	}
    }
    
    boolean boardHasWhite() {
    	Position p = new Position();
    	for(p.y=0;p.y<1000;p.y++){
        	for(p.x=0;p.x<1000;p.x++){
        		if(getRGB(p) == 0xFFFFFF) //isWalkable(p) &&
        			return true;
        	}
    	}
    	return false;
    }
    
    boolean colorIsValid(int i, int rgb) {
		   return (hasWhite && (
					   (i==0 && ColorPixel.getBrightness(rgb) >= 1f)
					|| (i==1 && rgb != color)
					|| (i==2 && ColorPixel.isColorNotDominantOrWhite(rgb,color))
					))
		|| (!hasWhite && !ColorPixel.isColorDominant(rgb,color));
    	//return (hasWhite && ColorPixel.getBrightness(rgb) >= 1f) ||
    	//		(!hasWhite && ColorPixel.isColorDominant(rgb,color));
    }
    
    boolean hasWhite = false;
    void searchFreeSpot(int i) {

    	hasWhite = boardHasWhite();
    	
    	boolean found=false;
    	float old_ang = ang[i];
    	float a = 0;
    	Position d = null;
    	Position t = targetPos[i];
     	float dist=Position.distance(pos[i],t);
     	float z = radius[i];
     	float sz = 0;
     	Position p = pos[i];
     	int rgb = getRGB(t);
     	float sr = 0;
     	
     	if(infinity[i] || !colorIsValid(i,rgb) || dist<=7.5f)
     	{
	    	for(float s=1;s<1000;s++) {
	         	sr = 0; //randomAngle(); //non smooth option
	    		sz = s+radius[i]/2;
		    	for(float r=0;r<360;r++) {
	    			a = old_ang + sr + r;
		    		d = Position.angToPosition(a);
		    		t = p.getNext(i, d, sz);
	    			rgb = getRGB(t);
	    			
	    			if(!isWalkable(t)||ColorPixel.isColorDominant(rgb,color)) continue;

		    		if(colorIsValid(i,rgb))
		    		{
		    			//setInfiniteDirection(i,b);
		    			setDirection(i, a, sz);
		    			found=true;
		    			break;
		    		}
		    	}
		    	if(found) break;
	    	}
	    	
			if(!found) setDirection(i, a, 0);
    	}
    }
    
    void findObstacle(int i) {
		
		int ts = Integer.MAX_VALUE;
		int s = 0;
		float a = 0;
		float an = ang[i];
		float tr = 0;
		
		boolean found = false;
		Position p = pos[i];
		
		int max = radius[i];
		for(int r=0;r<360;r++){
			a=an+r;
			for(s=1;s<max;s++) {
				if(!isWalkable(p.getNext(i, Position.angToPosition(a), s))) {
					if(s<ts) { found=true; ts = s; tr = a;}
					break;
				}
			}
		}
		
		if(found){
			//setDirection(i, oppositeAng(tr)+tlr.nextInt(-45, 45), radius[i]);
			setInfiniteDirection(i,oppositeAng(tr)+tlr.nextInt(-45, 45));
			hitWall[i]++;
		}
		else hitWall[i]=0;
    }
    
    Position[] waitPos = new Position[]{new Position(),new Position(),new Position()};
    
    void chooseAndMove(int i) {
    	searchFreeSpot(i);

    	if(wait[i]>=3) {
    		if(Position.equals(waitPos[i],pos[i]))
    		{
    			findObstacle(i);
    		}
    		else hitWall[i]=0;
    		
    		waitPos[i]=pos[i];
    		wait[i]=0;
    	}
    	else {
    		wait[i]++;
    	}

    	moveTo(i, targetPos[i]);
    }
    
    int getRGB(Position p){
    	return networkClient.getBoard((int)p.x, (int)p.y);
    }
    
    void update(int i) {}
 
    public void run() {
    	tlr = ThreadLocalRandom.current();
    	
		networkClient = new NetworkClient(host, name);
		myID = networkClient.getMyPlayerNumber();
		System.out.printf("Player: %d started\n", myID);
		 
		// radius
		radius[0] = networkClient.getInfluenceRadiusForBot(0);
		radius[1] = networkClient.getInfluenceRadiusForBot(1);
		radius[2] = networkClient.getInfluenceRadiusForBot(2);

		setInfiniteDirection(0,randomAngle());
		setInfiniteDirection(1,randomAngle());
		setInfiniteDirection(2,randomAngle());
		
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
			    Position p = new Position(x,y);
			    
			    if(player == myID){
			    	
			    	if(!Position.equals(pos[bot], p)){
			    		setPos(bot, p);
				    	update(bot);
			    		
			    		if(bot == 0) {

			    			if(!foundStartPos){
			    				color = ColorPixel.getDominantColor(getRGB(p));
				    			System.out.printf("%d: %s color\n", myID, ColorPixel.getColorStr(color));
			    			}
			    			foundStartPos = true;
			    		}
			    	}
			    }
			}
			
			if(foundStartPos) {
				chooseAndMove(0);
				chooseAndMove(1);
				chooseAndMove(2);
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
}
