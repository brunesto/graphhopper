package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;


public class TileMemCache extends BaseTileCache{

    static Logger logger = Logger.getLogger(TileMemCache.class);
    long maxDelay; 
    int cacheSize;
	
    TilesControllerAsync tilesControllerAsync;
    
   
	public void setTilesControllerAsync(TilesControllerAsync tilesControllerAsync) {
		this.tilesControllerAsync = tilesControllerAsync;
	}



	public TileMemCache(long maxDelay,int cacheSize,ITileGrabber next) {
	    super(next);
	    this.maxDelay=maxDelay;
	    this.cacheSize=cacheSize;
	    queue=new PriorityQueue<TileMemCache.ImageAndTimestamp>(cacheSize,new Comparator<ImageAndTimestamp>() {
			@Override
            public int compare(ImageAndTimestamp o1, ImageAndTimestamp o2) {
	            return Long.compare(o1.timestamp,o2.timestamp);
            }
		});
	    logger.info("maxDelay:"+maxDelay);
	    logger.info("cacheSize:"+cacheSize);
    }
	static class ImageAndTimestamp{
		public ImageAndTimestamp(TileId tileId,BufferedImage image, long timestamp) {
	        super();
	        this.image = image;
	        this.timestamp = timestamp;
	        this.tileId=tileId;
        }
		TileId tileId;
		BufferedImage image;
		long timestamp;
	
	}

	Map<TileId,ImageAndTimestamp> cache=new HashMap<TileId, ImageAndTimestamp>();
	PriorityQueue<ImageAndTimestamp> queue=new PriorityQueue<TileMemCache.ImageAndTimestamp>();

	@Override
    public synchronized BufferedImage checkIsTileCachedMaybeReturnImage(TileId tileId) throws Exception {
		ImageAndTimestamp imageAndTimestamp=cache.get(tileId);
		if (imageAndTimestamp==null)
			return null;
		long delay=System.currentTimeMillis()-imageAndTimestamp.timestamp;
		if (maxDelay>0 && delay>maxDelay){
			
			queue.remove(imageAndTimestamp);
			cache.remove(tileId);
			if (tilesControllerAsync!=null)
				tilesControllerAsync.grabTileAndDraw(tileId, null);
			
			return imageAndTimestamp.image;
		}
		return imageAndTimestamp.image;
			
	}
	
	
	
    public synchronized void removeTile(TileId tileId)  {
		if (logger.isDebugEnabled())
			logger.debug("tileId:"+tileId+" removing from cache");
		
		ImageAndTimestamp imageAndTimestamp=cache.remove(tileId);
		queue.remove(imageAndTimestamp);
    }
	
	

	@Override
    public synchronized void cacheTile(TileId tileId, BufferedImage image) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug(""+cache.size()+" cached tiles");
		ImageAndTimestamp imageAndTimestamp=new ImageAndTimestamp(tileId,image,System.currentTimeMillis());
		cache.put(tileId,imageAndTimestamp );
		queue.add(imageAndTimestamp);
		
		removeOlderTiles();
		
		
    }
	SimpleDateFormat sqlgmtformat=new SimpleDateFormat("yyyy-MM-dd HH'h'mm'm'ss");
	synchronized String timestampToString(long timestamp){
		return sqlgmtformat.format(new Date(timestamp));
	}
	public synchronized void removeOlderTiles() throws Exception {
		while (cache.size()>cacheSize){
			ImageAndTimestamp imageAndTimestamp=queue.poll();
			cache.remove(imageAndTimestamp.tileId);
			if (logger.isDebugEnabled())
				logger.debug("tileId:"+imageAndTimestamp.tileId+" timestamp:"+timestampToString(imageAndTimestamp.timestamp)+" removed from cache .  cache size now "+cache.size());
		}
		
		
	}
			

	@Override
    public String report() {
	   StringBuilder retVal=new StringBuilder();
	   retVal.append("\n"+getClass().getSimpleName());
	   retVal.append("\n cache size: "+cache.size()+"/"+cacheSize);
	   retVal.append("\n maxDelay:"+maxDelay);
	   retVal.append("\n ");
	   retVal.append(queue.size()>0?"oldest timestamp in queue:"+timestampToString(queue.peek().timestamp):"");
//	   for(ImageAndTimestamp imageAndTimestamp:queue){
//		   retVal.append("\n    tileId:"+imageAndTimestamp.tileId+" timestamp:"+timestampToString(imageAndTimestamp.timestamp));
//	   }
	   
	   retVal.append(next.report());
	   return retVal.toString();
    }

}
