package com.graphhopper.ui.headlesstiles;

public class CoordinatesHelper {

    
    // wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Lon..2Flat._to_tile_numbers
    public static double lon2PixX(double lon,int zoomLevel){
    	return 256*( (lon + 180) / 360 * (Math.pow(2, zoomLevel)) ) ;
    }
    public static double lat2PixY(double lat,int zoomLevel){
    	double r= 256*( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (Math.pow(2, zoomLevel)) ) ;
    	return r;
    }
    
    public static double tilePixX2lon(double x,int zoomLevel)  {
    	return (x/256.0) / (Math.pow(2, zoomLevel)) * 360.0 - 180;
    }
    
   
	public static double tilePixY2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / (256 * Math.pow(2.0, z));
		double r= Math.toDegrees(Math.atan(Math.sinh(n)));
		return r;
	}
    
}
