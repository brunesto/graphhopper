package com.graphhopper.ui;

import java.awt.Graphics;

import org.apache.log4j.Logger;

import com.graphhopper.ui.headlesstiles.BaseTilesController.Context;
import com.graphhopper.ui.headlesstiles.AnnotationHelper;
import com.graphhopper.ui.headlesstiles.CoordinatesHelper;
import com.graphhopper.ui.headlesstiles.DebugTileGrabber;
import com.graphhopper.ui.headlesstiles.TileDiskCache;
import com.graphhopper.ui.headlesstiles.UrlTileGrabberZXY;
import com.graphhopper.ui.headlesstiles.OnceGrabber;
import com.graphhopper.ui.headlesstiles.RiPoint;
import com.graphhopper.ui.headlesstiles.TileMemCache;
import com.graphhopper.ui.headlesstiles.TilesControllerAsync;

public class HeadlessTilesLayer {
	
	Logger logger=Logger.getLogger(HeadlessTilesLayer.class);
	
	
	TilesControllerAsync tilesController;
	public HeadlessTilesLayer(String url,String cacheDir) {
		
		TileDiskCache tileDiskCache=new TileDiskCache(cacheDir,new OnceGrabber(
				new DebugTileGrabber(0, 
						new UrlTileGrabberZXY(url))));
		 tilesController=new TilesControllerAsync(2, tileDiskCache);
		
	}
	
	
	public void paint(Graphics g,ZoomLevelGraphicsWrapper mg,long timeout) throws InterruptedException {
		logger.info("");
		logger.info("");
		logger.info("");
		
		Context context=new Context(
				RiPoint.makeCoordinates(
						CoordinatesHelper.tilePixY2lat(mg.getCenterYPix(),mg.getZoomLevel()),
						CoordinatesHelper.tilePixX2lon(mg.getCenterXPix(),mg.getZoomLevel())),
				mg.getZoomLevel(),
				RiPoint.makePoint(mg.getViewPortWidth(),mg.getViewPortHeight()));
		tilesController.getMap(timeout, context);
		AnnotationHelper.drawCopyright(context.getImage(), "");
		g.drawImage(context.getImage(),0,0,null);
	}
}

