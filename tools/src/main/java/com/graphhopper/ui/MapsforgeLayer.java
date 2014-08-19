package com.graphhopper.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.io.File;
import java.util.Observer;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.swing.MapViewer;
import org.mapsforge.map.swing.controller.MapViewComponentListener;
import org.mapsforge.map.swing.util.JavaUtilPreferences;
import org.mapsforge.map.swing.view.MapView;

public class MapsforgeLayer {
	
	Logger logger=Logger.getLogger(MapsforgeLayer.class);
	
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final boolean SHOW_DEBUG_LAYERS = false;
	
	MapsforgeLayerView mapView;
	public MapsforgeLayer(String mapFilename) {
		
		
		File mapFile = new File(mapFilename);
		mapView = createMapView();
		final BoundingBox boundingBox = addLayers(mapView, mapFile);

		PreferencesFacade preferencesFacade = new JavaUtilPreferences(Preferences.userNodeForPackage(MapViewer.class));
		 Model model = mapView.getModel();
		model.init(preferencesFacade);
		setViewportSize(100,100);
		
		

//		MainFrame mainFrame = new MainFrame();
//		mainFrame.add(mapView);
//		mainFrame.addWindowListener(new WindowCloseDialog(mainFrame, model, preferencesFacade));
//		mainFrame.setVisible(true);

//		mainFrame.addWindowListener(new WindowAdapter() {
//			@Override
//			public void windowOpened(WindowEvent e) {
//				byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox,
//						model.displayModel.getTileSize());
//				model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
//			}
//		});
	}
	public void setViewportSize(int width,int height){
		mapView.setBounds(0,0,width,height);
		Dimension size = this.mapView.getSize();
		mapView.getModel().mapViewDimension.setDimension(new org.mapsforge.core.model.Dimension(size.width, size.height));
	}
	
	
	public void paint(Graphics g,double latitude,double longitude,int zoomLevel) throws InterruptedException {
		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("("+latitude+","+longitude+") zoom="+zoomLevel);
		
//		MapPosition current=mapView.getModel().mapViewPosition.getMapPosition();
//		if (current.latLong.latitude==latitude && current.latLong.longitude==longitude && current.zoomLevel==zoomLevel){
//			//skip
//		} else 
		{
		
			mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), (byte) zoomLevel));
			mapView.getLayerManager().redrawLayers();
	//		
	//		 
			do{
				Thread.sleep(200);
			} while(tileRendererLayer.getJobQueue().size()>0);
	//		
		}
		logger.info("done");
		mapView.paint(g);
	}
	static MapsforgeTileRendererLayer tileRendererLayer;
	private static BoundingBox addLayers(MapsforgeLayerView mapView, File mapFile) {
		Layers layers = mapView.getLayerManager().getLayers();
		TileCache tileCache = createTileCache();

		// layers.add(createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition));
		tileRendererLayer = createTileRendererLayer(tileCache, mapView.getModel().mapViewPosition,mapFile);
		BoundingBox boundingBox = tileRendererLayer.getMapDatabase().getMapFileInfo().boundingBox;
		layers.add(tileRendererLayer);
		if (SHOW_DEBUG_LAYERS) {
			layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
			layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
		}
		return boundingBox;
	}

	private  MapsforgeLayerView createMapView() {
		MapsforgeLayerView mapView = new MapsforgeLayerView();
		mapView.getMapScaleBar().setVisible(true);
		//mapView.getFpsCounter().setVisible(true);
		//mapView.addComponentListener(new MapViewComponentListener(mapView, mapView.getModel().mapViewDimension));

//		MouseEventListener mouseEventListener = new MouseEventListener(mapView.getModel());
//		mapView.addMouseListener(mouseEventListener);
//		mapView.addMouseMotionListener(mouseEventListener);
//		mapView.addMouseWheelListener(mouseEventListener);

		return mapView;
	}
	
//	public void addObserver(Observer observer){
//		mapView.addObserver(observer);
//	}

	private static TileCache createTileCache() {
		TileCache firstLevelTileCache = new InMemoryTileCache(64);
		File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}

//	@SuppressWarnings("unused")
//	private static Layer createTileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition) {
//		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
//		TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(tileCache, mapViewPosition, tileSource,
//				GRAPHIC_FACTORY);
//		tileDownloadLayer.start();
//		return tileDownloadLayer;
//	}
	
	static class MapsforgeTileRendererLayer extends TileRendererLayer {

	public MapsforgeTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent, GraphicFactory graphicFactory) {
	    super(tileCache, mapViewPosition, isTransparent, graphicFactory);

    }
		JobQueue<RendererJob> getJobQueue(){
			return jobQueue;
		}
		
	}

	@SuppressWarnings("unused")
	private static MapsforgeTileRendererLayer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition,
			File mapFile) {
		boolean isTransparent = false;
		MapsforgeTileRendererLayer tileRendererLayer = new MapsforgeTileRendererLayer(tileCache, mapViewPosition, isTransparent,
				GRAPHIC_FACTORY);
		tileRendererLayer.setMapFile(mapFile);
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
		
		
		return tileRendererLayer;
	}

//	private static File getMapFile(String[] args) {
//		if (args.length == 0) {
//			throw new IllegalArgumentException("missing argument: <mapFile>");
//		} else if (args.length > 1) {
//			throw new IllegalArgumentException("too many arguments: " + Arrays.toString(args));
//		}
//
//		File mapFile = new File(args[0]);
//		if (!mapFile.exists()) {
//			throw new IllegalArgumentException("file does not exist: " + mapFile);
//		} else if (!mapFile.isFile()) {
//			throw new IllegalArgumentException("not a file: " + mapFile);
//		} else if (!mapFile.canRead()) {
//			throw new IllegalArgumentException("cannot read file: " + mapFile);
//		}
//
//		return mapFile;
//	}

	 
    
    
}

