package ca.sfu.cmpt431.facility;

public class Board{
	
	public int height;
	public int width;
	public boolean [][] bitmap;
	
	public Board(int h, int w) {
		height = h;
		width = w;
		bitmap = new boolean[h][w];
	}
	
}