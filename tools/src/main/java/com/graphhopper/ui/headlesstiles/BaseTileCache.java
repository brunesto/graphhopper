package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

public abstract class BaseTileCache implements ITileGrabber{

    static Logger logger = Logger.getLogger(BaseTileCache.class);

	
	ITileGrabber next;
	
	
	public BaseTileCache(ITileGrabber next) {
	    super();
	    this.next = next;
    }

	
	public abstract BufferedImage  checkIsTileCachedMaybeReturnImage(TileId tileId) throws Exception;
	public abstract void cacheTile(TileId tileId, BufferedImage image)  throws Exception;
	
	@Override
    public BufferedImage getImage(TileId tileId) throws Exception {
		BufferedImage retVal=checkIsTileCachedMaybeReturnImage(tileId);
		if (retVal!=null){
			if (logger.isDebugEnabled())
				logger.debug("tileId:"+tileId+"return cached tile ");
		} else {
				retVal = grabAndCache(tileId);
		} 
		return retVal;
    }

	BufferedImage grabAndCache(TileId tileId) throws Exception {
	    BufferedImage retVal;
	    retVal=next.getImage(tileId);
	    if (logger.isDebugEnabled())
	    	logger.debug("tileId:"+tileId+"caching tile ");
	    cacheTile(tileId,retVal);
	    return retVal;
    }


	

}
