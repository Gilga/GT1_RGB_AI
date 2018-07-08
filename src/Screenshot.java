import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Screenshot {
	public int generatePictureSec = 10;
	private long generatePictureLastTime = System.currentTimeMillis();
	
	public Screenshot(int pictureSec) { generatePictureSec=pictureSec; };
    
    public void generatePicture(int id, int[][] board)
    {
    	long time = System.currentTimeMillis();
    	if((time - generatePictureLastTime) < generatePictureSec*1000) return;
    	generatePictureLastTime = time;
    	
    	int width = board[0].length;
    	int height = board.length;
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    	//int[] t1 = targetPos[0].toInt();
    	//int[] t2 = targetPos[1].toInt();
    	//int[] t3 = targetPos[2].toInt();
    	
    	int x = 0;
    	int y = 0;
    	int[] buffer = new int[width*height];
    	for(int i = 0; i<buffer.length; i++) {
    		//if(x == t1[0] && y == t1[1]) buffer[i] = 0xFF000000;
    		//else if(x == t2[0] && y == t2[1]) buffer[i] = 0xFF000000;
    		//else if(x == t3[0] && y == t3[1]) buffer[i] = 0xFF000000;
    		//else
    		buffer[i] = board[y][x];
    		x++;
    		if(x>=width) { x=0; y++; }
    	}
    	
    	image.setRGB(0,0,width,height,buffer, 0, width);
    	
		try {
			File file = new File(id + "_board.png");
			ImageIO.write(image, "png", file);
			
		    Desktop dt = Desktop.getDesktop();
		    dt.open(file);
		    
		} catch (Exception e) {
			System.err.println("Fehler: Konnte Boardbild nicht speichern!");
		}
    }
}
