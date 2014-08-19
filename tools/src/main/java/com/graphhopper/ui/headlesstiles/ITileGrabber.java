package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;


public interface ITileGrabber {
	
	BufferedImage getImage(TileId tileId)  throws Exception;

	String report();
	
}
