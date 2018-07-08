
public class Position {
	public float x = 0;
	public float y = 0;
	
	public static class Int {
		public int x = 0;
		public int y = 0;
		
		public Int() {}
		
		public Int(int x, int y) { this.x=x; this.y=y; }
		public Int(float x, float y) { this.x=(int)x; this.y=(int)y; }
		
	    public Int round(Position p) {
	    	this.x=Math.round(p.x); Math.round(p.y);
	    	return this;
	    }
	}
	
	public Position() {}
	public Position(float v) {this.x=v;this.y=v;}
	public Position(float x, float y) {this.x=x;this.y=y;}
	
	public String toString() {
		return "["+x+","+y+"]";
	}
	
    public boolean equals (Object o){

        if (o instanceof Position){

        	Position p= (Position)o;

          if ((x == p.x) &&(y == p.y))
          {
              return true;
          }
        }
     return false;

    }
	
    public Int toInt() {
    	return new Int(x,y);
    }
    
    public Int toRoundInt() {
    	return new Int().round(this);
    }

    public Position getNext(Position direction, float r) {
    	return new Position(x+(r*direction.x),y+(r*direction.y));
    }

    Position getNormalizedDirection(Position t) {
    	return normalize(getDirection(t,1));
    }
    
    Position getDirection(Position t, float dist) {
    	Position n = subtract(this,t);
    	return dist!=1 ? new Position(n.x/dist,n.y/dist) : n;
    }
    
    public Position getAdd(Position t) {
    	return Position.add(this,t);
    }
    
    public Position getSubtract(Position t) {
    	return Position.subtract(this,t);
    }
    
    public Position getMul(Position t) {
    	return Position.mul(this,t);
    }
    
    public Position getMul(float t) {
    	return Position.mul(this,t);
    }
    
    public Position getDiv(Position t) {
    	return Position.div(this,t);
    }
    
    public Position getDiv(float t) {
    	return Position.div(this,t);
    }
    
    public Position getNormalize() {
    	return Position.normalize(this);
    }
    
    public void add(Position t) {
    	Position n = Position.add(this,t);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public void subtract(Position t) {
    	Position n = Position.subtract(this,t);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public void mul(Position t) {
    	Position n = Position.mul(this,t);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public void mul(float t) {
    	Position n = Position.mul(this,t);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public void div(Position t) {
    	Position n = Position.div(this,t);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public void div(float t) {
    	Position n = Position.div(this,t);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public void normalize() {
    	Position n = Position.normalize(this);
    	this.x=n.x;
    	this.y=n.y;
    }
    
    public float magnitude() {
    	return Position.magnitude(this);
    }
    
    public static Position angToPosition(float ang) {
    	ang=new Float(Math.toRadians(ang));
    	return new Position(new Float(Math.sin(ang)),new Float(Math.cos(ang)));
    }

    public static float magnitude(Position p) {
    	return new Float(Math.sqrt(p.x*p.x + p.y*p.y));
    }
    
    public static Position normalize(Position p) {
    	float length = magnitude(p);
    	return new Position(p.x/length,p.y/length);
    }
    
    public static Position mul(Position p, float m) {
    	return new Position(p.x*m,p.y*m);
    }
    
    public static Position mul(Position p, Position t) {
    	return new Position(p.x*t.x,p.y*t.y);
    }
    
    public static Position div(Position p, Position t) {
    	return new Position(p.x/t.x,p.y/t.y);
    }
    
    public static Position div(Position p, float m) {
    	return new Position(p.x/m,p.y/m);
    }
    
    public static Position add(Position p, Position t) {
    	return new Position(p.x+t.x,p.y+t.y);
    }
    
    public static Position subtract(Position p,Position t) {
    	return new Position(t.x-p.x,t.y-p.y);
    }
    
    
    public static boolean equals(Position p, Position t) {
    	return p.x == t.x && p.y == t.y;
    }
    
    public static float distance(Position p1, Position p2) {
        return new Float(Math.sqrt(
                Math.pow(p1.x - p2.x,2.0) + 
                Math.pow(p1.y - p2.y,2.0)
            ));
    }
    
    /**
     * source: https://stackoverflow.com/questions/9970281/java-calculating-the-angle-between-two-points-in-degrees
     * 
     * Calculates the angle from centerPt to targetPt in degrees.
     * The return should range from [0,360), rotating CLOCKWISE, 
     * 0 and 360 degrees represents NORTH,
     * 90 degrees represents EAST, etc...
     *
     * Assumes all points are in the same coordinate space.  If they are not, 
     * you will need to call SwingUtilities.convertPointToScreen or equivalent 
     * on all arguments before passing them  to this function.
     *
     * @param centerPt   Point we are rotating around.
     * @param targetPt   Point we want to calcuate the angle to.  
     * @return angle in degrees.  This is the angle from centerPt to targetPt.
     */
    public static double calcRotationAngleInDegrees(Position centerPt, Position targetPt)
    {
        // calculate the angle theta from the deltaY and deltaX values
        // (atan2 returns radians values from [-PI,PI])
        // 0 currently points EAST.  
        // NOTE: By preserving Y and X param order to atan2,  we are expecting 
        // a CLOCKWISE angle direction.  
        double theta = Math.atan2(targetPt.y - centerPt.y, targetPt.x - centerPt.x);

        // rotate the theta angle clockwise by 90 degrees 
        // (this makes 0 point NORTH)
        // NOTE: adding to an angle rotates it clockwise.  
        // subtracting would rotate it counter-clockwise
        theta += Math.PI/2.0;

        // convert from radians to degrees
        // this will give you an angle from [0->270],[-180,0]
        double angle = Math.toDegrees(theta);

        // convert to positive range [0-360)
        // since we want to prevent negative angles, adjust them now.
        // we can assume that atan2 will not return a negative value
        // greater than one partial rotation
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }
    
    public static boolean isInLine(Position a, Position b, Position c) {
       // if AC is horizontal
       if (a.x == c.x) return b.x == c.x;
       // if AC is vertical.
       if (a.y == c.y) return b.y == c.y;
       // match the gradients
       return (a.x - c.x)*(a.y - c.y) == (c.x - b.x)*(c.y - b.y);
    }
    
}
