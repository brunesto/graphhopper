package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;



import org.apache.log4j.Logger;

public class TilesControllerAsync extends BaseTilesController{
	
	ExecutorService executorService;
	BlockingQueue<TileId> blockingQueue;
	public TilesControllerAsync(int threads,ITileGrabber tileGrabber) {
	    super(tileGrabber);
	    executorService = Executors.newFixedThreadPool(threads);
    }


	static Logger logger = Logger.getLogger(TilesControllerAsync.class);
	   
	


	public Future<?> grabTileAndDraw(final TileId tileId,final Context context){
		return executorService.submit(new Runnable() {
			@Override
			public void run() {
				BufferedImage tileImage;
                try {
                    tileImage = tileGrabber.getImage(tileId);
                    onImageGrabSuccess(tileId,context,tileImage);
                  
                } catch (Throwable t) {
                	onImageGrabFailed(tileId, context, t);
                }
				
			}
		});
	}
	

	protected void onImageGrabSuccess(TileId tileId,Context context,BufferedImage tileImage) {
		  if (context!=null)
          	context.draw(tileId, tileImage);
    }
	protected void onImageGrabFailed(TileId tileId,Context context,Throwable t) {
		logger.error("tileId:"+tileId+" exception caught :"+t.getClass().getName()+":"+t.getMessage());
		if (logger.isDebugEnabled())
		  logger.debug(t.getMessage(),t);
  }


	@Override
	public void grabTilesAndDraw(List<TileId> tiles,final Context context,long timeout){
		List<Pair<TileId,Future<?>>> futures=new LinkedList<Pair<TileId,Future<?>>>();
		for (final TileId tileId:tiles)
			try {
					futures.add(new Pair<TileId,Future<?>>(tileId,grabTileAndDraw(tileId, context)));
				} catch (Exception e){
					logger.error("tileId:"+tileId+": exception while spawning future:"+e.getClass().getName()+":"+e.getMessage(),e);
				}
		
		
		long start=System.currentTimeMillis();
		for (Pair<TileId,Future<?>> future:futures){
			long now=System.currentTimeMillis();
			long timeoutNow=start-now+timeout;
			if (logger.isDebugEnabled())
				logger.debug("tileId:"+future.getA()+" timeoutNow:"+timeoutNow);
			if (timeoutNow<=0)
				break;
			try {
				future.getB().get(timeout, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				logger.error("tileId:"+future.getA()+": exception while calling future.get:"+e.getClass().getName()+":"+e.getMessage());
			}

		}
		context.setStopDrawing(true);
		context.failures=futures.size()-context.drawn;
		
		if (logger.isDebugEnabled())
			logger.debug("failures:"+context.failures);
	}


	public void shutdown() {
		List<Runnable> tasks=executorService.shutdownNow();
		if (logger.isInfoEnabled())
			logger.info("tasks still running for tilesController:"+tasks.size());
	    
    }
}
