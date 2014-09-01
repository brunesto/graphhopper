package com.graphhopper.ui.headlesstiles;

public class TileId implements Comparable<TileId>{

	public TileId(int zoom, long x, long y) {
	    super();
	    this.zoom = zoom;
	    this.x = x;
	    this.y = y;
    }
	int zoom;
	@Override
    public String toString() {
	    return  zoom + "/" + x + "/" + y ;
    }
	long x;
	long y;
	@Override
    public int hashCode() {
	    final long prime = 31;
	    long result = 1;
	    result = prime * result + x;
	    result = prime * result + y;
	    result = prime * result + zoom;
	    return (int)result;
    }
	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    TileId other = (TileId) obj;
	    if (x != other.x)
		    return false;
	    if (y != other.y)
		    return false;
	    if (zoom != other.zoom)
		    return false;
	    return true;
    }
	@Override
    public int compareTo(TileId that) {
	    int zoomCmp=Integer.compare(this.zoom, that.zoom );
	    if (zoomCmp!=0)
	    	return zoomCmp;
	    int xCmp=Long.compare(this.x, that.x );
	    if (xCmp!=0)
	    	return xCmp;
	    int yCmp=Long.compare(this.y, that.y );
	    return yCmp;
	    
    }
	
	
	
	
}
