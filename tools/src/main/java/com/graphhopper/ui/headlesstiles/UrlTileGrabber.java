package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public abstract class UrlTileGrabber implements ITileGrabber {

    static Logger logger = Logger.getLogger(UrlTileGrabber.class);
    int requestCnt;
    int failedCnt;
    
    int maxZoomLevel;
    
    
	public abstract String getTileUrl(TileId tile);
	
	@Override
	public BufferedImage getImage(TileId tileId){
		
//		TileAndMetaInfo tileAndMetaInfo=new TileAndMetaInfo();
//		tileAndMetaInfo.tileId=tile;
		
		long startTime=System.currentTimeMillis();
		requestCnt++;
		String url=getTileUrl(tileId);
		try {
			
			if (logger.isDebugEnabled())
				logger.debug(url);

			URLConnection connection=new URL(url).openConnection();
			
			// seems that timeout is ignored anyway?
			// http://stackoverflow.com/questions/3163693/java-urlconnection-timeout
			connection.setConnectTimeout(2000);
			connection.setReadTimeout(2000);
			
			InputStream in=connection.getInputStream();
			BufferedImage image=ImageIO.read(in);
			in.close();
			return image;
			
			
			
		} catch (Exception e){
		    long failedTime=System.currentTimeMillis();
			failedCnt++;
			throw new RuntimeException("failed to download tile:"+tileId+" after "+(failedTime-startTime)+"ms url:"+url+" msg:"+e.getMessage()+"cause:"+(e.getCause()!=null?e.getCause().getMessage():""));
			
		}
	}
	
	@Override
    public String report() {
	   return "\n"+getClass().getSimpleName()+" requestCnt "+requestCnt+" failedCnt:"+failedCnt;
    }
}
