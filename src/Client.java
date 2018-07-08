import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javafx.util.Pair;
import lenz.htw.zpifub.*;
import lenz.htw.zpifub.net.*;

public class Client extends Thread {
	
	NetworkClient networkClient =  null;
	String host = null;
    String name = null;
    int myID = 0;
    int latency = 0;
    int generatePictureSec = 6;
    boolean generatePictureEnabled = false;
    boolean hasWhite = false;
    
    int SPRAY = 0;
    int BRUSH = 1;
    int BIGBRUSH = 2;
    int COUNT_BRUSH = 3;
    
    Board board = new Board();
    
    List<PowerUp> powerUps = new ArrayList<>();
    
    Client(String name, String host) throws IOException {
    	super(name);
        this.name = name;
        this.host = host;
        this.board.setScreenshotHandler(new Screenshot(generatePictureSec));
    }
    
    ThreadLocalRandom tlr = null;

    int color = 0;
    int[] radius = new int[]{0,0,0}; // radius
    float[] ang = new float[]{0,0,0};
    Position[] pos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldPos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldDir = new Position[]{new Position(),new Position(),new Position()}; // direction
   
    long[] wait = new long[]{0,0,0};
    int[] hitWall = new int[]{0,0,0};
    
    Position[] targetPos = new Position[]{new Position(),new Position(),new Position()};

    boolean[] infinity = new boolean[]{false,false,false};
    
    void move() {
    	for(int brush=0; brush<COUNT_BRUSH; brush++) {
	    	Position p = pos[brush];
	    	Position t = getWayPoint(brush); //getPassablePosition(getWayPoint(bot),radius[bot]/2);
	    	Position d = new Position(t.x-p.x, t.y-p.y);
	
			if(!Position.equals(oldDir[brush], d)){
				oldDir[brush]=d;
				networkClient.setMoveDirection(brush, d.x, d.y);
	    		//System.out.printf("(%d|%d){%d} [%f,%f] -> [%f,%f]\n", myID, bot, radius[bot], pos[bot].x, pos[bot].y, d.x, d.y);
			}
    	}
    }

    boolean canMoveTo(int bot, Position t, float step) {
    	Position p=pos[bot];

    	float dist=Position.distance(p,t);
    	if(dist<=0) return false;
  
    	Position d = Position.angToPosition(ang[bot]);
   	
    	step=(radius[bot]/2+1);
    	
    	if(!board.isValid(Position.add(p, Position.mul(d,new Position(step))))) return false;

    	for(float j=0;j<step;j++) //(dist/step)
    	{
    		p=Position.add(p, d);
    		if(!board.isValid(p)) return false;
    	}

    	return true;
    }
    
    boolean touchedTarget(int bot, Position t) {
    	return Position.isInLine(oldPos[bot], pos[bot], t);
    }
    
    void setPos(int bot, Position p) { oldPos[bot]=pos[bot]; pos[bot] = p;  }
    
    float oppositeAng(float ang) {
    	return (ang+180) % 360;
    }
    
    float randomAngle() {
    	return tlr.nextInt(0, 360);
    }
    
    void setRandomDirection(int bot) {
    	if(!infinity[bot]) setInfiniteDirection(bot, randomAngle());
    }
    
    void setInfiniteDirection(int bot, float a) {
    	setDirection(bot, a, Integer.MAX_VALUE);
    	infinity[bot]=true;
    }
    
    void setDirection(int bot, float a, float v) {
    	Position t = pos[bot].getNext(Position.angToPosition(a), v);
    	if(!Position.equals(targetPos[bot], t)) {
    		ang[bot] = a;
    		targetPos[bot]=t;
    		infinity[bot]=false;
    	}
    }
    
    void setTarget(int bot, Position t) {
    	if(!Position.equals(targetPos[bot], t)) {
    		ang[bot] = new Float(Position.calcRotationAngleInDegrees(pos[bot],t));
    		targetPos[bot]=t;
    		infinity[bot]=false;
    	}
    }
    
    boolean colorIsValid(int bot, int rgb) {
		   return (hasWhite && (
					   (bot == SPRAY && ColorPixel.getBrightness(rgb) >= 1.0f)
					|| (bot == BRUSH && rgb != color)
					|| (bot == BIGBRUSH && ColorPixel.isColorNotDominantOrWhite(rgb,color))
					))
		|| (!hasWhite && !ColorPixel.isColorDominant(rgb,color));
    	//return (hasWhite && ColorPixel.getBrightness(rgb) >= 1f) ||
    	//		(!hasWhite && ColorPixel.isColorDominant(rgb,color));
    }
    
    int[] searchState = new int[]{0,0,0};
    boolean findRGB(int bot, Position t){
    	if(!board.isValid(t)) return false;
        
    	//board.allWayPoints.get(bot).add(t);
    	
        int rgb = board.get(t);
        if(ColorPixel.isColorDominant(rgb,color)) return false;
        
        if(colorIsValid(bot,rgb))
		{
        	setTarget(bot, t);
        	
        	return true;
		}
        
        return false;
    }
    
	Position[] ways = new Position[]{
			new Position(+1,+0),
    		new Position(+1,+1),
    		new Position(+0,+1),
    		new Position(-1,+1),
    		new Position(-1,+0),
    		new Position(-1,-1),
    		new Position(+0,-1),
    		new Position(+1,-1)
	};
	
	String[] waysStr = new String[]{
			"r",
    		"ru",
    		"u",
    		"lu",
    		"l",
    		"ld",
    		"d",
    		"rd"
	};
    
	int wayPointIndex[] = new int[]{0,0,0};
	List<Pair<Integer, ArrayList<Position>>> wayPoints = new ArrayList<>();

	boolean hasWayPoints(int bot)
	{
		return wayPoints.get(bot).getValue().size()>0;
	}
	
	boolean isEndWayPointIndex(int bot) {
		return wayPointIndex[bot]>=wayPoints.get(bot).getValue().size();
	}
	
	Position getCurrentWayPoint(int bot) {
		Pair<Integer, ArrayList<Position>> p = wayPoints.get(bot);
		return p.getValue().get(p.getKey());
	}
	
	void increaseWayPointIndex(int bot) {
		if(!isEndWayPointIndex(bot)) wayPointIndex[bot]++;
	}
	
	Position getWayPoint(int bot)
	{
		Position wp = null;
		/*
		if(hasWayPoints(bot)) {
			wp = getCurrentWayPoint(bot);
			if(touchedTarget(bot, wp)) {
				System.err.println("YES "+wp);
				increaseWayPointIndex(bot);
			}
		}
		else */
			wp = targetPos[bot];

		return wp;
	}
	
	void addWayPoint(int bot, Position pos)
	{
		wayPoints.get(bot).getValue().add(pos);
	}
	
	void clearWayPoints(int bot)
	{
		wayPointIndex[bot] = 0;
		wayPoints.get(bot).getValue().clear();
	}
	
	boolean hasWayPoint(int bot, Position pos) {
		return wayPoints.get(bot).getValue().contains(pos);
	}
	
	void updateWayPoints(int bot)
	{
		if(!hasWayPoints(bot)) {
			searchState[bot]=0;
			return;
		}
			
		Position w = getWayPoint(bot);
		board.set(w, 0xFF000000);
		
		if(!board.isValid(w) || Position.distance(pos[bot],w) <= radius[bot] || !colorIsValid(bot,board.get(getWayPoint(bot)))){
			wayPointIndex[bot]++;
		}

		if(wayPointIndex[bot]>=wayPoints.get(bot).getValue().size()){
			clearWayPoints(bot);
			searchState[bot]=0;
			System.out.println("done.");
		}
	}
	
	Position getPassablePosition(Position t, float radius){
		Position x = t;
		Position v = t;

		boolean validRadius = true;
		float rm=8;
		float rr=360/rm;
    	for(int dr = 0; dr<=radius;dr++) {
	    	for(float r=0;r<rm;r++) {
	    		x = t.getNext(Position.angToPosition(rr*r), dr);
	    		if(!board.isValid(x)){
					validRadius=false;
	    		} else {
	    			float d1 = Position.distance(x, t);
	    			float d2 = Position.distance(v, t);
	    			if(d1<d2)
	    				v=x;
	    		}
	    	}
	    	if(!validRadius) break;
    	}
    	
    	if(!validRadius) return v;
    	return t;
	}

	void DijkstraSearch(int bot, int max)
	{
		ArrayList<Position> list = new ArrayList<>();
    	ArrayList<Position> rlist = new ArrayList<>();
    	ArrayList<Position> filter = new ArrayList<>();
    	ArrayList<Position> wlist = wayPoints.get(bot).getValue();
    	
    	Position start = pos[bot];
    	Position end = targetPos[bot];
		Position nextPos = start;
		Position bestPos = start;
		Position current = start;
		int bestIndex = 0;
		
		float dist = Position.distance(start, end);
		float dist_last = -1;
		float dist_best = dist;
		float dist_current = dist;
		int found = 0;
		Position rp;
		
		System.out.print("0 ");
    	for(int s = 0; s<1000;s++){
    		
    		found = 0;
    		dist_last = -1;
    		for(int i=0;i<ways.length;i++) {
    			current=Position.add(nextPos,ways[i]);
    			
	    		board.set(current,0xFFDDDDDD);
	    		
    			boolean validRadius = true;
    			
    			// beachte den radius
    			/*
    			float rm=8;
    			float rr=360/rm;
    	    	for(int dr = 0; dr<=radius[bot]/2;dr++) {
    		    	for(float r=0;r<rm;r++) {
    		    		rp = current.getNext(Position.angToPosition(rr*r), dr);
    		    		if(!isValid(rp)){
    						validRadius=false;
    						break;
    		    		}
    		    	}
    		    	if(!validRadius) break;
    	    	}
    	    	*/
    	           			
    			if (validRadius && board.isValid(current) && !list.contains(current) && !filter.contains(current)){
    				if(found<1) {
    					found = 1;
    					bestIndex=i;
    					bestPos=current;
    				}
    				dist_current = Position.distance(current, end);
    				if(dist_last<=0 || dist_current<dist_last) {
    					bestPos=current;
    					bestIndex=i;
    					dist_last=dist_current;
    					if(found<2) found = 2;
    					if(dist_last<dist_best) dist_best=dist_last;
     				}
    			}
    		}
    		
    		if(found>0) {
    			nextPos=bestPos;
    			if(Position.distance(nextPos, end)<=0) break;
    			System.out.print("+"+waysStr[bestIndex]+" ");
    			rlist.add(ways[bestIndex]);
    			list.add(nextPos);
    			board.set(nextPos,0xFF00FF00);
    		}
    		else {
    			System.out.print("-");
    			board.set(nextPos,0xFFAAAAAA);
    			if(list.size()<=0) break;
     			int step=list.size()-1;
    			list.remove(step);
    			rlist.remove(step);
    			filter.add(nextPos);
    			step--;
       			nextPos = (step<0) ? start : list.get(step);
       			bestIndex=-1;
       			if(step>=0)  {
       				Position h = rlist.get(step);
       				for(int i=0;i<ways.length;i++) { if(Position.equals(h, ways[i])){ bestIndex=i; break;}; }
       			}
       			System.out.print("("+((bestIndex<0) ? "0" : waysStr[bestIndex])+") ");
    		}
    	}
    	System.out.println();

    	nextPos = start;
    	bestPos = new Position();
    	
    	for(Position w : rlist)
    	{
    		nextPos=Position.add(nextPos,w);
    		if(Position.equals(bestPos, w)) continue;
    		bestPos=w;
    		wlist.add(nextPos);
    	}
    	
		for(Position w : filter) board.set(w, 0xFFAA88AA);
    	for(Position w : list) board.set(w, 0xFFCCCC00);
    	
    	board.set(start, 0xFF00FFFF);
    	board.set(end, 0xFF0000FF);
	}
	
	Position.Int[] defaultDirs = new Position.Int[]{
		new Position.Int(0,1),
		new Position.Int(0,-1),
		new Position.Int(1,0),
		new Position.Int(-1,0),
		
		new Position.Int(1,1),
		new Position.Int(-1,-1),
		new Position.Int(-1,1),
		new Position.Int(1,-1),
	};
	
	//List<Position.Int> dirs = new ArrayList<>();
	//void resetDirs() { dirs = Arrays.asList(defaultDirs);	}
	
	void avoidBlackBorder(int bot)
	{
		Position p = pos[bot];
		Position t = targetPos[bot];
		Position next = null;

		//Position d = p.getNormalizedDirection(t);
		//Position r90d_cw = new Position(-1 * d.y, 1 * d.x); // CW
		//Position r90d_cc = new Position(1 * d.y, -1 * d.x); // CC
		//Position.Int n = d.toRoundInt();
		
		if(!isEndWayPointIndex(bot)) return;
		clearWayPoints(bot);
		board.allWayPoints.get(bot).clear();
		
		for(int i=0; i<1000; i++) {
			
			//dirs.remove(n);
			List<Pair<Float, Position>> list = new ArrayList<>();
			
			for(Position.Int dir : defaultDirs) {
				next = new Position(p.x+dir.x,p.y+dir.y);
				if(!board.isValid(next) || hasWayPoint(bot,next)) continue;
				//if(!board.isValid(next) || hasWayPoint(bot,next)) continue;
				float dist = Position.distance(new Position(p.x+dir.x,p.y+dir.y),t);
				list.add(new Pair<Float, Position>(dist,next));
			}
			
			if(list.isEmpty()) {
				System.err.println("Cannot move aynmore: "+p);
				break;
			}
			
			Collections.sort(list, new Comparator<Pair<Float, Position>>() {
			    @Override
			    public int compare(final Pair<Float, Position> o1, final Pair<Float, Position> o2) {
					return o1.getKey() == o2.getKey() ? 0 : (o1.getKey() < o2.getKey() ? -1 : 1);
			    }
			});
			
			p = list.get(0).getValue();
			addWayPoint(bot, p);
			board.allWayPoints.get(bot).add(p);
			//System.out.println("Add: "+p);
			if(p == t) { System.out.println("YEs: "+p); break;}
			
		}
		
		//if(wayPoints.get(bot).getValue().size()>0) {
			//board.allWayPoints.set(bot, wayPoints.get(bot).getValue());
			//System.out.println(wayPoints.get(bot).getValue().size());
		//}
	}
    
	void SpiralSearch(int bot, float size)
	{
		Position p = pos[bot];
		Position d = Position.subtract(p, new Position(board.HWidth, board.HHeight));
		
		float WH = size/2;
		float HH = size/2;
		int t = 0;
	    int x,y,dx,dy;
	    x = y = dx =0;
	    dy = -1;
	    
	    for(int i = 0; i < size; i++){
	    	//double rr = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
	    	
	        if ((-WH <= x) && (x <= WH) && (-HH <= y) && (y <= HH))
	        {
	        	if(findRGB(bot, new Position(p.x+x,p.y+y))) return;
	        }
	        
	        if( (x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1-y))){
	            t = dx;
	            dx = -dy;
	            dy = t;
	        }

	        x += dx;
	        y += dy;
	    }
	}

	void CircleSearch(int bot, float size)
	{
		Position p = pos[bot];
    	for(float r=1;r<size/2;r++) {
    		float r2 = r*r;
	        // iterate through all x-coordinates
	        for (float i = p.y-r; i <= p.y+r; i++) {
	        	float di2 = (i-p.y)*(i-p.y);
	            // iterate through all y-coordinates
	            for (float j = p.x-r; j <= p.x+r; j++) {
	            	float di1 = (j-p.x)*(j-p.x);
	                // test if in-circle
	                if (di1 + di2 <= r2) {
	                	if(findRGB(bot, new Position(j, i))) return;
	                }
	            }
	        }
    	}
	}

	void RadiusSearch(int bot, float size)
	{
		Position p = pos[bot];
     	Position d = null;
    	float old_ang = ang[bot];
    	float a = 0;
     	float z = radius[bot];
     	float sz = 0;
     	float sr = 0;
     	float dd = Position.distance(p,new Position(board.HWidth, board.HHeight));
     	float rr = dd + board.HWidth + board.HHeight;
     	
    	for(float s=1;s<size;s++) {
         	sr = 0; //randomAngle(); //non smooth option
    		sz = s+z/2;
	    	for(float r=0;r<360;r++) {
	    		d = Position.angToPosition(old_ang + sr + r);
	    		if(findRGB(bot, p.getNext(d, sz))) return;
	    	}
    	}
	}
    
	int PowerUpRadius = 15;
	
	boolean avoidPowerUps(int bot) {
		float dist = 0;
		float speed = board.isValid(oldPos[bot]) ? (Position.distance(pos[bot], oldPos[bot])+1) : 1;
		
    	for(PowerUp powerUp : powerUps) {
    		if(powerUp.type != PowerUp.SLOW) continue;
    		dist = Position.distance(pos[bot],powerUp.pos);
    		if(dist <=(1+latency)*(radius[bot]+PowerUpRadius+speed)) {
        		Position d = pos[bot].getNormalizedDirection(powerUp.pos);
        		Position dirs[] = new Position[] {
        				new Position(-1 * d.y, 1 * d.x), // r90d_cw
        				new Position(1 * d.y, -1 * d.x), // r90d_cc
        				new Position(-1 * d.y, -1 * d.x) // r180d_cw
        		};
    			setTarget(bot,Position.add(pos[bot], dirs[tlr.nextInt(0, dirs.length)]));
    			return true;
    		}
    	}
    	return false;
	}
    
    boolean preferPowerUps(int bot) {
    	Position t = null;
    	Position target = null;
    	float dist = 0;
    	float min_dist = -1;

    	for(PowerUp powerUp : powerUps) {
    		if(powerUp.type == PowerUp.SLOW) continue;
    		t = powerUp.pos;
    		dist=Position.distance(pos[bot],t);
    		if(min_dist<0||dist<min_dist) { min_dist = dist; target = t; }
    	}
    	if(target!=null) { setTarget(bot,target); return true; }
    	
    	setRandomDirection(bot);
    	
    	return false;
    }
    
    void search(int bot) {
    	preferPowerUps(bot);
    	
     	hasWhite = board.hasWhite();
    	
    	Position t = targetPos[bot];
    	float dist=Position.distance(pos[bot],t);

    	int rgb = board.isValid(t) ? board.get(t) : 0x0;
    	
    	//if(!(myID == 0 && bot == SPRAY)) return;
    	
    	if(!(infinity[bot] || !colorIsValid(bot,rgb) || dist<=radius[bot])) return;

    	int searchRadius = board.Size*3;
    	
    	if(bot==SPRAY) SpiralSearch(bot, searchRadius);
    	else if(bot==BRUSH) SpiralSearch(bot, searchRadius);
    	else if(bot==BIGBRUSH) SpiralSearch(bot, searchRadius);
    	
    	/*
    	if(searchState[bot]==1){
    		clearBoard();
    		//SpiralSearch(bot, size*3);
    		targetPos[bot] = new Position(tlr.nextInt(250, 750),tlr.nextInt(250, 750));
    		searchState[bot]++;
    	//if(name == "SpiralSearch") SpiralSearch(bot, size*3);
    	//else if(name == "CircleSearch") CircleSearch(bot, 200);
    	//else if(name == "RadiusSearch") RadiusSearch(bot, size*3);
    	}
    	
    	*/
    }
    
    void findObstacle(int bot) {
		int ts = Integer.MAX_VALUE;
		int s = 0;
		float a = 0;
		float an = ang[bot];
		float tr = 0;
		
		boolean found = false;
		Position p = pos[bot];
		
		int max = radius[bot];
		for(int r=0;r<360;r++){
			a=an+r;
			for(s=1;s<max;s++) {
				if(!board.isValid(p.getNext(Position.angToPosition(a), s))) {
					if(s<ts) { found=true; ts = s; tr = a;}
					break;
				}
			}
		}
		
		if(found){
			setDirection(bot, oppositeAng(tr)+tlr.nextInt(-90, 90), radius[bot]*10);
			//setInfiniteDirection(bot,oppositeAng(tr)+tlr.nextInt(-45, 45));
			hitWall[bot]++;
		}
		else hitWall[bot]=0;
    }
    
    Position[] waitPos = new Position[]{new Position(),new Position(),new Position()};
    boolean picture=false;
    
    void findTarget() {
    	for(int bot=0; bot<COUNT_BRUSH; bot++) {
	    	if(Position.equals(targetPos[bot], new Position())) targetPos[bot] = pos[bot];
    		if(hasWayPoints(bot) && !isEndWayPointIndex(bot)) continue;
    		
	    	search(bot);
	    	//avoidBlackBorder(bot);
	    	
	    	/*
	    	if(searchState[bot]==1) {
	        	if((myID == 0 && bot == SPRAY)) System.out.println("DijkstraSearch");
	        	DijkstraSearch(bot, board.Size);
	        	searchState[bot]++;
	    	}
	    	
	    	if(searchState[bot]==2) {
	    		if((myID == 0 && bot == SPRAY)) System.out.println("updateWayPoints: " + wayPoints.get(bot).getValue().size());
	    		updateWayPoints(bot);
	    		
	    		//if(!colorIsValid(bot,rgb) || dist<=radius[bot]) {
	    		//	clearWayPoints(bot);
	    		//}
	    	}
	    	*/
	    	
	    	// QUICK AND DIRTY SOLUTON FOR COLLISION ON WALL
	    	if((System.currentTimeMillis() - wait[bot]) >= 3) {
	    		if(Position.equals(waitPos[bot],pos[bot]))
	    		{
	    			findObstacle(bot);
	    		}
	    		else hitWall[bot]=0;
	    		
	    		waitPos[bot]=pos[bot];
	    		wait[bot]=System.currentTimeMillis();
	    	}
	    	
	    	avoidPowerUps(bot);
    	}
    }
    
    public long getMedian(long[] numArray) {
    	Arrays.sort(numArray);
    	
    	int middle = numArray.length/2;
    	long medianValue = 0; //declare variable 
    	
    	if (numArray.length%2 == 1) 
    	    medianValue = numArray[middle];
    	else
    	   medianValue = (numArray[middle-1] + numArray[middle]) / 2;
    	
    	return medianValue;
    }
    
    void remove(int bot, PowerUp p) {
    	float dist = 0;
    	int i = -1;
    	for(PowerUp q : powerUps) {
    		i++;
    		dist = Position.distance(p.pos, q.pos);
    		if(p.type == q.type && dist <= 5)
    		{
    	    	System.out.println(p+" removed.");
    	    	powerUps.remove(i);
    			//if(bot>-1) setRandomDirection(bot);
    			break;
    		}
    	}
    }
    
    boolean foundMyColor = false;
    
    void findMyColor(int x, int y) {
		if(foundMyColor) return;
		
		//myID == 0 ? 0xFFFF0000 : (myID == 1 ? 0xFF00FF00 : (myID == 2 ? 0xFF0000FF : 0));
		int c = 0xFF000000 | networkClient.getBoard(x,y);
		color = ColorPixel.getDominantColor(c);
		
		if(color != 0) {
			System.out.printf("%d: %s color\n", myID, ColorPixel.getColorStr(color));
			foundMyColor = true;
		}
		else System.err.printf("%d: %s color (INVALID)\n", myID, ColorPixel.getColorStr(c));
    }
    
    public void run() {
    	tlr = ThreadLocalRandom.current();
    	
		networkClient = new NetworkClient(host, name, "Enemy Rage Quit");
		myID = networkClient.getMyPlayerNumber();
		System.out.printf("Player: %d started\n", myID);

		// radius, setInfiniteDirection
		for(int bot=0; bot<COUNT_BRUSH; bot++) {
			radius[bot] = networkClient.getInfluenceRadiusForBot(bot);
			if(bot == BRUSH) radius[bot] *= 2;
			setInfiniteDirection(bot,randomAngle());
			wayPoints.add(new Pair<>(0, new ArrayList<>()));
		}
		
		board.update(networkClient);
		
		Update update = null;
		long time = 0;
		
		while(true){
			time = System.currentTimeMillis();
			while ((update = networkClient.pullNextUpdate()) != null) {
				latency = (int) (System.currentTimeMillis() - time);
				
			    //verarbeiten von colorChange
				long score = networkClient.getScore(0); // Punkte von rot
			    int player = update.player; 
			    int bot = update.bot; 
			    int x = update.x; 
			    int y = update.y;
			    PowerupType type = update.type;
			    Position p = new Position(x,y);
			    
			    if (type != null){
			    	PowerUp powerUp = new PowerUp(p,type);
			    	if(player == -1) {
			    		powerUps.add(powerUp); // add
			    		System.out.println(type+" "+x+" "+y);
			    	}
			    	else remove(bot, powerUp); // remove, it was used
			    }
			    
			    if(player == myID){
			    	//if(!Position.equals(pos[bot], p))
			    	//Position dir = Position.normalize(Position.subtract(p,pos[bot]));
			    	//p = Position.add(p, Position.mul(dir, 1+latency));
		    		setPos(bot, p);
		    		findMyColor(x,y);
			    	
			    	/*
			    	if(myID==0 && bot==1) {
			    		float px = pos[bot].x;
			    		float py = pos[bot].y;
			    		targetPos[bot] = new Position((px > 500f ? -1: 1)*100+500,(px > 500f ? -1: 1)*100+500);
			    	}
			    	else targetPos[bot]= pos[bot];
			    	*/
			    }
			}
			
			if(foundMyColor) {
				board.update(networkClient);
				
				findTarget();
				
				if(generatePictureEnabled) board.show(myID, powerUps);
				
				move();
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			time = System.currentTimeMillis();
		}
    }
}
