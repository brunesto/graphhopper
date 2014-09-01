package com.graphhopper.ui.headlesstiles;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

public class ZoomTileGrabber implements ITileGrabber {

    static Logger logger = Logger.getLogger(ZoomTileGrabber.class);
    ITileGrabber next;
	int maxZoomLevel;
	public ZoomTileGrabber(int maxZoomLevel,ITileGrabber next) {
	    super();
	    this.next = next;
	    this.maxZoomLevel=maxZoomLevel;
    }

	
	@Override
	public BufferedImage getImage(TileId tileId) throws Exception{
		
		
		if (tileId.zoom<=maxZoomLevel)
			return next.getImage(tileId);
		
		else{
//			int oldX=tileId.x*256;
//			int oldY=tileId.y*256;
//			double lon=CoordinatesHelper.tilePixX2lon(tileId.x*256, tileId.zoom);
//			double lat=CoordinatesHelper.tilePixY2lat(tileId.y*256, tileId.zoom);

//			int newYInOldZoom=(int)CoordinatesHelper.lat2PixY(lat, tileId.zoom);
//			int newXInOldZoom=(int)CoordinatesHelper.lon2PixX(lon, tileId.zoom);
			
			long deltaY=(tileId.y & 1)*256;
			long deltaX=(tileId.x & 1)*256;
			
			int newZoom=tileId.zoom-1;
			
//			int newY=(int)CoordinatesHelper.lat2PixY(lat, newZoom);
//			int newX=(int)CoordinatesHelper.lon2PixX(lon, newZoom);
//			
			long newY=tileId.y>>1;
		long newX=tileId.x>>1;
			TileId zoomedTileId=new TileId(newZoom,newX,newY);
			
			BufferedImage zoomedImage=this.getImage(zoomedTileId);
			
			if (zoomedImage==null)
					return null;
			
			
			BufferedImage bufferedImage=new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
			Graphics g=bufferedImage.getGraphics();
			
			g.drawImage(zoomedImage, (int)-deltaX,(int)-deltaY,512,512,null);
			g.dispose();
			return bufferedImage;
		}
	}

	@Override
    public String report() {
	   return "\nZoomTileGrabber maxZoomLevel: "+maxZoomLevel+next.report();
    }
}
