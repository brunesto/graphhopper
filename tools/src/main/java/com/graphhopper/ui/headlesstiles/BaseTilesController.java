package com.graphhopper.ui.headlesstiles;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;


public abstract class BaseTilesController {
	
    static Logger logger = Logger.getLogger(BaseTilesController.class);
    
    public abstract void grabTilesAndDraw(List<TileId> tiles,Context context,long timeout);
    
		
	public BaseTilesController(ITileGrabber tileGrabber) {
	    super();
	    this.tileGrabber = tileGrabber;
    }

	public TileId getTileIdFromCoords(RiPoint coord,int zoom){
		int x=(int)CoordinatesHelper.lat2PixY(coord.getLon(), zoom)/256;
		int y=(int)CoordinatesHelper.lat2PixY(coord.getLat(), zoom)/256;
		return new TileId(zoom, x, y);
		
	}
	ITileGrabber tileGrabber;

	public static class Context{
		RiPoint centerCoord;
		int zoom;
		RiPoint viewPortSize;
		
		int centerX;
		int centerY;
		int halfWidth;
		int halfHeight;
		
		int minTileX;
		int minTileY;
		int maxTileX;
		int maxTileY;
		
		BufferedImage image;
		boolean stopDrawing;
		public int drawn;
		public int failures;
		
		public Context(RiPoint centerCoord,int zoom,RiPoint viewPortSize){
			this.centerCoord=centerCoord;
			this.zoom=zoom;
			this.viewPortSize=viewPortSize;
			
			centerX=(int)CoordinatesHelper.lon2PixX(centerCoord.getLon(), zoom);
			centerY=(int)CoordinatesHelper.lat2PixY(centerCoord.getLat(), zoom);
			
			halfWidth=(int)viewPortSize.getWidth()/2;
			halfHeight=(int)viewPortSize.getHeight()/2;
			
			minTileX=(centerX-halfWidth)/256;
			maxTileX=(centerX+halfWidth)/256;
			minTileY=(centerY-halfHeight)/256;
			maxTileY=(centerY+halfHeight)/256;
			
			image=new BufferedImage((int)viewPortSize.getWidth(), (int)viewPortSize.getHeight(), BufferedImage.TYPE_INT_RGB);
			
			
		}
		
		public boolean isStopDrawing() {
			return stopDrawing;
		}

		public void setStopDrawing(boolean stopDrawing) {
			this.stopDrawing = stopDrawing;
		}

		
		public synchronized void draw(TileId tileId,BufferedImage tileImage){
			
			if (!stopDrawing){
				Graphics g=image.getGraphics();
				long x=tileId.x*256-centerX+halfWidth;
				long y=tileId.y*256-centerY+halfHeight;
				if (logger.isDebugEnabled())
					logger.debug("tileId:"+tileId+"drawing tile at "+x+","+y);
				g.drawImage(tileImage,(int)x,(int)y,null);
				g.dispose();
				drawn++;
			}
			
			
		}

		public BufferedImage getImage() {
			
	        return image;
        }

		public int getFailures() {
	        return failures;
        }
		
		
		
	}
	
	
		
	
	List<TileId> getRequestedTiles(Context context){
	
		List<TileId> retVal=new ArrayList<TileId>();
		
		for(int i=context.minTileX;i<=context.maxTileX;i++)
			for(int j=context.minTileY;j<=context.maxTileY;j++) 
				retVal.add(new TileId(context.zoom, i, j));
		return retVal;
	}

	
	public Context  getMap(long timeout,Context context){
		List<TileId> tiles=getRequestedTiles(context);
		grabTilesAndDraw(tiles, context,timeout);
		if (logger.isDebugEnabled())
			logger.debug(tileGrabber.report());
		return context;
	}
	
	public Context  getMap(long timeout,RiPoint centerCoord,int zoom,RiPoint viewPortSize){
		
		Context context=new Context(centerCoord, zoom, viewPortSize);
		return getMap(timeout, context);
		
		
		
	}
	
	
	
}
