package com.graphhopper.ui.headlesstiles;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class AnnotationHelper {

	public static void drawCopyright(BufferedImage image,String additional) {
		int y=image.getHeight()-5;
		
		
		
		String copyright="© OpenStreetMap contributors "+additional;
		Graphics g=image.getGraphics();
		 Font f = new Font("sans", Font.PLAIN, 11);
	     g.setFont(f);
	        
	  
	     
	     
		int textWidth=g.getFontMetrics(g.getFont()).stringWidth(copyright);
		int x=image.getWidth()-textWidth-5;
		
		   g.setColor(new Color(0.7f, 0.7f, 0.7f, 0.6f)); 
					g.fillRect(x-5, y-15, textWidth+10,20);
		
		g.setColor(Color.BLACK);
		g.drawString(copyright,x, y-1);
		
		g.dispose();
	}
}
