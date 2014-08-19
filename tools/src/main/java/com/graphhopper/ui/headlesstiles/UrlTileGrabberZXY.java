package com.graphhopper.ui.headlesstiles;

public class UrlTileGrabberZXY extends UrlTileGrabber {

	String root;
	public UrlTileGrabberZXY(String root){
		this.root=root;
	}
	@Override
    public String getTileUrl(TileId tile) {
		   return root+"/"+tile.zoom+"/"+tile.x+"/"+tile.y+".png";
    }
	

}
