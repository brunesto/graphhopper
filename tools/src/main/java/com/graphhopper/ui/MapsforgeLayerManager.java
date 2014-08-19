package com.graphhopper.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.view.MapView;

public class MapsforgeLayerManager extends LayerManager{

	Logger logger=Logger.getLogger(MapsforgeLayerManager.class);
	
	public MapsforgeLayerManager(MapView mapView, MapViewPosition mapViewPosition, GraphicFactory graphicFactory) {
	    super(mapView, mapViewPosition, graphicFactory);
    }
	
	@Override
	public void redrawLayers(){
		super.redrawLayers();
	}
	
	@Override
	protected void doWork() throws InterruptedException {
		logger.info("doWork()");
		super.doWork();
		if (!super.hasWork()){
			if (semaphore.availablePermits()==0)
				semaphore.release();
			logger.info("work done, releasing semaphore "+semaphore.availablePermits());
		} else {
			logger.info("more work to do...");			
		}
	}
	
	
	@Override
	public boolean hasWork(){
		return super.hasWork();
	}

	Semaphore semaphore=new Semaphore(0);
	public void andWait() throws InterruptedException {
		semaphore.acquire();
		logger.info("semaphore released");
    }

}
