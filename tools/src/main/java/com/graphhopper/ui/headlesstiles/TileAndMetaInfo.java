package com.graphhopper.ui.headlesstiles;

import java.awt.image.BufferedImage;

public class TileAndMetaInfo {

	TileId tileId;
	BufferedImage image;
	long lastModified;
	enum Status{NEW,LOADING,SUCCESS,ERROR};
	Status status;
	
	public synchronized Status getStatus() {
		return status;
	}
	public synchronized void setStatus(Status status) {
		this.status = status;
	}
	
	
	
}
