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
public class OnceGrabber implements ITileGrabber{

	 static Logger logger = Logger.getLogger(OnceGrabber.class);

	 
	ITileGrabber next;
	
	static class TileRequest {
		
		int waitCnt;
		BufferedImage bufferedImage;
	}
	
	HashMap<TileId,TileRequest> activeRequests=new HashMap<TileId,TileRequest>();
	
	public boolean createTileRequestOrIncWaitCnt(TileId tileId){
		boolean request=false;
    	synchronized (activeRequests) {
    		TileRequest tileRequest=activeRequests.get(tileId);
			if (tileRequest==null){
				request=true;
				activeRequests.put(tileId,new TileRequest());
				if (logger.isDebugEnabled())
					logger.debug("tileId:"+tileId+" added in active request");
			} else {
				tileRequest.waitCnt++;
				if (logger.isDebugEnabled())
					logger.debug("tileId:"+tileId+" waitCnt:"+tileRequest.waitCnt);
			}
    	}
    	return request;
	}
	
	public void completeTileRequest(TileId tileId,BufferedImage bufferedImage){
		TileRequest tileRequest=activeRequests.get(tileId);
		synchronized (tileRequest) {
			tileRequest.bufferedImage=bufferedImage;
			
			if (logger.isDebugEnabled())
				logger.debug("tileId:"+tileId+" got the image, notifying...");
			
			
			
			 
		}
		while (tileRequest.waitCnt>0)
			synchronized (tileRequest) {tileRequest.notify();}   
			
		removeTileRequest(tileId);
		
	}
	
	public BufferedImage waitForTileRequest(TileId tileId) throws InterruptedException{
		TileRequest tileRequest=activeRequests.get(tileId);
		synchronized (tileRequest) {
			tileRequest.wait();
			tileRequest.waitCnt--;
			return tileRequest.bufferedImage;
		}
		
		
	}
	
	
    synchronized void removeTileRequest(TileId tileId) {
    	TileRequest tileRequest=activeRequests.get(tileId);
		if (tileRequest.waitCnt!=0)
			throw new IllegalStateException("tileId:"+tileId+" waitCnt is not 0: "+tileRequest.waitCnt);
    	activeRequests.remove(tileId);
    	if (logger.isDebugEnabled())
			logger.debug("tileId:"+tileId+"  removed from active requests");
	    
    }

	@Override
    public BufferedImage getImage(TileId tileId) throws Exception {
		if (createTileRequestOrIncWaitCnt(tileId)){
    		try {
	    		BufferedImage image=next.getImage(tileId);
	    		completeTileRequest(tileId, image);
	    		return image;	
    		} catch (Exception e) {
    			completeTileRequest(tileId, null);
    			throw e;
    		}
    	} else {
    		return waitForTileRequest(tileId);
    	}
    	
		
				
	}

	public OnceGrabber(ITileGrabber next) {
	    super();
	    this.next = next;
    }

	
	@Override
    public String report() {
		
		StringBuilder retVal=new StringBuilder();
		retVal.append("\n");
		retVal.append(getClass().getSimpleName());
		
		List<TileId> tilesIds;
		synchronized (activeRequests) {
			tilesIds=new LinkedList<TileId>(activeRequests.keySet());
		}
		retVal.append(" activeRequests:"+tilesIds.size()+" elements.");
		Collections.sort(tilesIds);
		
		for(TileId tileId:tilesIds) {
			TileRequest tileRequest=activeRequests.get(tileId);
			
				retVal.append("\ntileId:"+tileId);
			if (tileRequest==null)
				retVal.append("tile removed");
			else {
				
				retVal.append(" image loaded:"+tileRequest.bufferedImage!=null);
				retVal.append(" waitCnt:"+tileRequest.waitCnt);
				
			}
			retVal.append("\n");
		}
		retVal.append(next.report());
	    return retVal.toString();
    }

}
