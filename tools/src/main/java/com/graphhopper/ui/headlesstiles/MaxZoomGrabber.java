package com.graphhopper.ui.headlesstiles;

import java.awt.LinearGradientPaint;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Ensures that one tile is only beeing requested once at a given moment
 *
 */
public class MaxZoomGrabber implements ITileGrabber{

	 static Logger logger = Logger.getLogger(MaxZoomGrabber.class);

	 
	ITileGrabber next;
	
	
	int maxZoomLevel;

	public MaxZoomGrabber(int maxZoomLevel,ITileGrabber next) {
	    super();
	    this.next = next;
	    this.maxZoomLevel = maxZoomLevel;
    }

	@Override
    public BufferedImage getImage(TileId tileId) throws Exception {
		if (tileId.zoom>maxZoomLevel)
			return null;
		else
			return next.getImage(tileId);
				
	}

	@Override
    public String report() {
	    return "\nmaxZoomLevel:"+maxZoomLevel+next.report();
	    
    }

}
