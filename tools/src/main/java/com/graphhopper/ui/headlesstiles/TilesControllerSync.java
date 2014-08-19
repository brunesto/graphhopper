package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;
import java.util.List;

import org.apache.log4j.Logger;

public class TilesControllerSync extends BaseTilesController{
	
    static Logger logger = Logger.getLogger(TilesControllerSync.class);

    
		
	public TilesControllerSync(ITileGrabber tileGrabber) {
	    super(tileGrabber);
    }


	@Override
    public void grabTilesAndDraw(List<TileId> tiles,Context context,long timeout){
		for (TileId tileId:tiles)
			try {
					BufferedImage tileImage=tileGrabber.getImage(tileId);
					context.draw(tileId, tileImage);
				} catch (Exception e){
					logger.error(e.getMessage(),e);
				}
	}
	
	
	
}
