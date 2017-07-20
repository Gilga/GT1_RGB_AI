
public class ColorPixel {
    static public int[] splitColor(int rgb){
		int b = rgb & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int r = (rgb >> 16) & 0xFF;
		int a = (rgb >> 24) & 0xFF;
    	return new int[]{r,g,b,a};
    }
    
    static public int getColorStrength(int rgb, int color){
		int[] c = splitColor(rgb);
		return color == 0xFF0000FF ? c[2] : (color == 0xFF00FF00 ? c[1] : c[0]);
    }
    
    static public boolean isColorNotDominantOrWhite(int rgba, int color){
    	return rgba == 0xFFFFFFFF || !isColorDominant(rgba, color);
    }
    
    static public boolean isColorDominant(int rgba, int color){
    	return color == getDominantColor(rgba);
    }
    
    static public float getBrightness(int color)
    {
		int[] rgba = splitColor(color);
		//int c = ((r>b && r>g)?r:((g>=r && g>=b)?g:((b>=r && b>=g)?b:b)));
		float c = (rgba[0] * 0.2126f + rgba[1] * 0.7152f + rgba[2] * 0.0722f) / 0xFF;
		return c;
    }

    static public int getColorStrength(int color) {
    	int c = getDominantColor(color);
    	int[] rgba = splitColor(color);
		int b = rgba[2];
		int g = rgba[1];
		int r = rgba[0];
    	return color == c ? c == 0xFFFF0000 ? r : (c == 0xFF00FF00 ? g : b) : 0;
    }
    
    static public int getDominantColor(int color) {
		int b = color & 255;
		int g = (color >> 8) & 255;
		int r = (color >> 16) & 255;
    	return (r>b && r>g)?0xFFFF0000:((g>r && g>b)?0xFF00FF00:((b>r && b>g)?0xFF0000FF:0));
    }
    
    static public String getColorStr(int color) {
    	return (color==0xFF0000FF)?"blue":((color==0xFF00FF00)?"green":((color==0xFFFF0000)?"red":"?"));
    }
}
