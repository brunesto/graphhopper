package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

/**
 * note: does not clean itself !
 * @author bc
 *
 */
public class TileDiskCache extends BaseTileCache{

  // static Logger logger = Logger.getLogger(TileDiskCache.class);

	
	String cacheDir="target/tile-cache";
	
	
	
	public TileDiskCache(String cacheDir,ITileGrabber next) {
	    super(next);
	    this.cacheDir=cacheDir;
    }

	public String getPath(TileId tileId){
		return cacheDir+"/"+tileId.zoom+"/"+tileId.x+"/"+tileId.y+".png";
	}
	

	@Override
    public BufferedImage checkIsTileCachedMaybeReturnImage(TileId tileId) throws Exception {
		String path=getPath(tileId);
		try {
			File file=new File(getPath(tileId));
			if (file.exists()){
				
				return ImageIO.read(file);
			} else
				return null;
		} catch (Exception e){
			logger.error("tileId:"+tileId+" caught exception "+e.getMessage()+" path:"+path,e);
			return null;
		}
	}

	@Override
    public void cacheTile(TileId tileId, BufferedImage image) throws Exception {
		File file=new File(getPath(tileId));
		
		file.mkdirs();
		ImageIO.write(image, "png", file);
    }

	@Override
    public String report() {
	   return "\n"+getClass().getSimpleName()+" cacheDir: "+cacheDir+next.report();
    }
}
