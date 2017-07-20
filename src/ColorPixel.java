
public class ColorPixel {
    static public int[] splitColor(int rgb){
		int b = rgb & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int r = (rgb >> 16) & 0xFF;
    	return new int[]{r,g,b};
    }
    
    static public int getColorStrength(int rgb, int color){
		int[] c = splitColor(rgb);
		return color == 0xFF ? c[2] : (color == 0xFF00 ? c[1] : c[0]);
    }
    
    static public boolean isColorNotDominantOrWhite(int rgb, int color){
    	return rgb == 0xFFFFFF || !isColorDominant(rgb, color);
    }
    
    static public boolean isColorDominant(int rgb, int color){
    	return color == getDominantColor(rgb);
    }
    
    static public float getBrightness(int color)
    {
		int[] rgb = splitColor(color);
		//int c = ((r>b && r>g)?r:((g>=r && g>=b)?g:((b>=r && b>=g)?b:b)));
		float c = (rgb[0] * 0.2126f + rgb[1] * 0.7152f + rgb[2] * 0.0722f) / 0xFF;
		return c;
    }

    static public int getColorStrength(int color) {
    	int c = getDominantColor(color);
    	int[] rgb = splitColor(color);
		int b = rgb[2];
		int g = rgb[1];
		int r = rgb[0];
    	return color == c ? c == 0xFF0000 ? r : (c == 0xFF00 ? g : b) : 0;
    }
    
    static public int getDominantColor(int color) {
		int b = color & 255;
		int g = (color >> 8) & 255;
		int r = (color >> 16) & 255;
    	return (r>b && r>g)?0xFF0000:((g>r && g>b)?0xFF00:((b>r && b>g)?0xFF:0));
    }
    
    static public String getColorStr(int color) {
    	return (color==0xFF)?"blue":((color==0xFF00)?"green":((color==0xFF0000)?"red":"?"));
    }
}
