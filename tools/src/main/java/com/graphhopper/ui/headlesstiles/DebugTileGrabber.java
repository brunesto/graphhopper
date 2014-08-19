package com.graphhopper.ui.headlesstiles;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.apache.log4j.Logger;

public class DebugTileGrabber implements ITileGrabber {

    static Logger logger = Logger.getLogger(DebugTileGrabber.class);
    ITileGrabber next;
	long delay;
	public DebugTileGrabber(long delay,ITileGrabber next) {
	    super();
	    this.next = next;
	    this.delay=delay;
    }

	@Override
	public BufferedImage getImage(TileId tileId) throws Exception{
		
		Thread.sleep((long)(new Random().nextDouble()*delay));
		
		BufferedImage bufferedImage;
		if (next==null) 
			bufferedImage=new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		else{	 
			bufferedImage=next.getImage(tileId);
			
		}
		Graphics g=bufferedImage.getGraphics();
		if (next==null) {
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, 255, 255);
		}
		g.setColor(Color.RED);
		g.drawRect(0, 0, 255, 255);
		g.drawString(tileId.zoom+"/"+tileId.x+"/"+tileId.y, 5, 20);
		g.drawString(tileId.zoom+"/"+tileId.x+"/"+tileId.y, 5, 250);
		g.dispose();
		return bufferedImage;
	}

	@Override
    public String report() {
	   return "\nDebugTileGrabber delay "+delay+next.report();
    }
}
