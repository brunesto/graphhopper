package com.graphhopper.ui;



import java.awt.Container;
import java.awt.Graphics;
import java.util.Observable;
import java.util.Observer;

import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;

public class MapsforgeLayerView extends Container implements org.mapsforge.map.view.MapView {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final long serialVersionUID = 1L;


	private final FrameBuffer frameBuffer;
	private final FrameBufferController frameBufferController;
	private final MapsforgeLayerManager layerManager;
	private final MapScaleBar mapScaleBar;
	private final Model model;

//	Observable observable=new Observable(){
//		@Override
//		public void notifyObservers(Object arg){
//			setChanged();
//			super.notifyObservers(arg);
//		}
//	};
	
	public MapsforgeLayerView() {
		super();

		this.model = new Model();

		
		this.frameBuffer = new FrameBuffer(this.model.frameBufferModel, new DisplayModel(), GRAPHIC_FACTORY);
		this.frameBufferController = FrameBufferController.create(this.frameBuffer, this.model);

		this.layerManager = new MapsforgeLayerManager(this, this.model.mapViewPosition, GRAPHIC_FACTORY);
		this.layerManager.start();
		LayerManagerController.create(this.layerManager, this.model);

		MapViewController.create(this, this.model);

		this.mapScaleBar = new MapScaleBar(this.model.mapViewPosition, this.model.mapViewDimension, GRAPHIC_FACTORY,
				new DisplayModel());
	}

	@Override
	public void destroy() {
		this.layerManager.interrupt();
		this.frameBufferController.destroy();
	}

	@Override
	public Dimension getDimension() {
		return new Dimension(getWidth(), getHeight());
	}

	
	@Override
	public FrameBuffer getFrameBuffer() {
		return this.frameBuffer;
	}

	@Override
	public MapsforgeLayerManager getLayerManager() {
		return this.layerManager;
	}

	public MapScaleBar getMapScaleBar() {
		return this.mapScaleBar;
	}

	@Override
	public Model getModel() {
		return this.model;
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		GraphicContext graphicContext = AwtGraphicFactory.createGraphicContext(graphics);
		this.frameBuffer.draw(graphicContext);
		this.mapScaleBar.draw(graphicContext);
	}

	@Override
    public FpsCounter getFpsCounter() {
	    return null;
    }
//	public void addObserver(Observer observer){
//		observable.addObserver(observer);
//	}
//	@Override
//	public void repaint() {
//		super.repaint();
//		
//		observable.notifyObservers(null);
//	}
}
