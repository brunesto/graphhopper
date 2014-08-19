package com.graphhopper.ui.headlesstiles;

import com.graphhopper.util.shapes.GHPoint;

public class RiPoint extends GHPoint{

	/**
	 * instanciate a Point(X,Y)
	 */
	public static RiPoint makePoint(double x, double y){
		return new RiPoint(y,x);
	}

	/**
	 * instanciate a Point(Lat,Lon)
	 */
	public static RiPoint makeCoordinates(double lat, double lon){
		return new RiPoint(lat,lon);
	}
	
	 protected RiPoint( double lat, double lon )
	    {
	        this.lat = lat;
	        this.lon = lon;
	    }
	
	double getWidth(){
		return getLon();
	}
	double getHeight(){
		return getLat();
	}
}
