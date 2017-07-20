import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

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
    
    int[][] board = new int[1024][1024];
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
    
    void move(int i) {
    	Position p = pos[i];
    	Position t = targetPos[i];
    	Position d = new Position(t.x-p.x, t.y-p.y);
    	
		if(!Position.equals(oldDir[i], d)){
			oldDir[i]=d;
			networkClient.setMoveDirection(i, d.x, d.y);
    		//System.out.printf("(%d|%d){%d} [%f,%f] -> [%f,%f]\n", myID, i, radius[i], pos[i].x, pos[i].y, d.x, d.y);
		}
    }
     
    
    boolean isValid(Position p) {
    	if(p.x<0||p.y<0||p.x>1023||p.y>1023) return false;
		//return board[(int)p.x][(int)p.y] != 0x0; // working incorrectly
		return networkClient.isWalkable((int)p.x, (int)p.y);
    }

    int getBoard(Position p){
    	if(!isValid(p)) return 0x0;
    	//return board[(int)p.y][(int)p.x]; // working incorrectly
    	return 0xFF000000 | networkClient.getBoard((int)p.x, (int)p.y);
    }
    
    boolean canMoveTo(int i, Position t, float step) {
    	Position p=pos[i];

    	float dist=Position.distance(p,t);
    	if(dist<=0) return false;
  
    	Position d = Position.angToPosition(ang[i]);
   	
    	step=(radius[i]/2+1);
    	
    	if(!isValid(Position.add(p, Position.mul(d,new Position(step))))) return false;

    	for(float j=0;j<step;j++) //(dist/step)
    	{
    		p=Position.add(p, d);
    		if(!isValid(p)) return false;
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
    	for(p.y=0;p.y<board.length;p.y++){
        	for(p.x=0;p.x<board[0].length;p.x++){
        		if(getBoard(p) == 0xFFFFFFFF) //isWalkable(p) &&
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
     	int rgb = getBoard(t);
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
	    			rgb = getBoard(t);
	    			
	    			if(!isValid(t)||ColorPixel.isColorDominant(rgb,color)) continue;

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
				if(!isValid(p.getNext(i, Position.angToPosition(a), s))) {
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
    
    void setNewTarget(int i) {
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
    }
    
    void update(int i) {}

    boolean updateBoardFirstTime = true;
    
    public void updateBoard() {
    	int width = board[0].length;
    	int height = board.length;
    	
    	int rgb = 0;
    	int x = 0;
    	int y = 0;
    	for(int i = 0; i<width*height; i++) {
    		if(updateBoardFirstTime || board[y][x] != 0){
	    		rgb = networkClient.getBoard(x,y);
	    		if(!updateBoardFirstTime || rgb!=0) {
	    			board[y][x] = 0xFF000000 | rgb;
	    		}
    		}
    		x++;
    		if(x>=width) { x=0; y++; }
    	}
    	
    	if(updateBoardFirstTime) updateBoardFirstTime=false;
    }
    
    public void generatePicture()
    {
    	int width = board[0].length;
    	int height = board.length;
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    	int x = 0;
    	int y = 0;
    	int[] buffer = new int[width*height];
    	for(int i = 0; i<buffer.length; i++) {
    		buffer[i] =  board[y][x];
    		x++;
    		if(x>=width) { x=0; y++; }
    	}
    	
    	image.setRGB(0,0,width,height,buffer, 0, width);
    	
		try {
			File file = new File(myID + "_board.png");
			ImageIO.write(image, "png", file);
			
		    Desktop dt = Desktop.getDesktop();
		    dt.open(file);
		    
		} catch (Exception e) {
			System.err.println("Fehler: Konnte Boardbild nicht speichern!");
		}
    }
 
    public void run() {
    	tlr = ThreadLocalRandom.current();
    	
		networkClient = new NetworkClient(host, name);
		myID = networkClient.getMyPlayerNumber();
		System.out.printf("Player: %d started\n", myID);

		updateBoard();
		//if(myID == 0) generatePicture();
		
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
		int g = 0;
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
			    				color = ColorPixel.getDominantColor(getBoard(p));
				    			System.out.printf("%d: %s color\n", myID, ColorPixel.getColorStr(color));
			    			}
			    			foundStartPos = true;
			    		}
			    	}
			    }
			}
			
			if(foundStartPos) {
				updateBoard();
				
				if(myID == 0 && g==500) {
					generatePicture();
				}
				g++;
				
				setNewTarget(0);
				setNewTarget(1);
				setNewTarget(2);
				
				move(0);
				move(1);
				move(2);
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
