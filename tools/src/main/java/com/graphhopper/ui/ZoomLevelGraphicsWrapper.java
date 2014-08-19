/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.ui.headlesstiles.CoordinatesHelper;
import com.graphhopper.ui.headlesstiles.RiPoint;
import com.graphhopper.util.shapes.BBox;

/**
 */
public class ZoomLevelGraphicsWrapper
{
	private Logger logger = LoggerFactory.getLogger(getClass());

    private Graph g;
    private NodeAccess na;
    private int zoomLevel;
    private int centerXPix;
    private int centerYPix;
    private BBox bounds ;
    int viewPortWidth;
    int viewPortHeight;

    public ZoomLevelGraphicsWrapper( Graph graph)
    {
        this.g = graph;
        if (graph!=null)
        	this.na = graph.getNodeAccess();
        
        zoomLevel =14;
        
        
        BBox graphBounds=graph.getBounds();
       
        setViewPort(100, 100);
        setCenterCoords((graphBounds.maxLat+graphBounds.minLat)/2, (graphBounds.maxLon+graphBounds.minLon)/2);
      
       
        
        
       
    }
    
    public String info(){
    	return "zoomLevel:"+zoomLevel+" center:"+CoordinatesHelper.tilePixY2lat(centerYPix,zoomLevel)+","+CoordinatesHelper.tilePixX2lon(centerXPix,zoomLevel)+","+") x:"+centerXPix+" y:"+centerYPix+")";
    }


    
    public void plotText( Graphics2D g2, double lat, double lon, String text )
    {
    	int width=g2.getFontMetrics().charsWidth(text.toCharArray(), 0, text.length());
    	int x=(int)getX(lon);
    	int y=(int)getY(lat);
    	
    	g2.setColor(Color.LIGHT_GRAY);
    	g2.fillRect(x,y-10, width+10, 20);
    	g2.setColor(Color.BLACK);
    	g2.drawRect(x,y-10, width+10, 20);
        g2.drawString(text, x+ 5,y + 5);
    }

    public void plotEdge( Graphics2D g2, double lat, double lon, double lat2, double lon2, int width )
    {
        g2.setStroke(new BasicStroke(width));
        g2.drawLine((int) getX(lon), (int) getY(lat), (int) getX(lon2), (int) getY(lat2));
    }

    public void plotEdge( Graphics2D g2, double lat, double lon, double lat2, double lon2 )
    {
        plotEdge(g2, lat, lon, lat2, lon2, 1);
    }

    public int getOffsetXPix(){
    	return centerXPix-viewPortWidth/2;
    }
    public int getOffsetYPix(){
    	return centerYPix-viewPortHeight/2;
    	
    }
    
    public double getX( double lon )
    {
        return CoordinatesHelper.lon2PixX(lon,zoomLevel)-getOffsetXPix();
    }

    public double getY( double lat )
    {
    	return CoordinatesHelper.lat2PixY(lat,zoomLevel)-getOffsetYPix();
    }

    public double getLon( int x )
    {
        return CoordinatesHelper.tilePixX2lon(x+getOffsetXPix(),zoomLevel);
    }

    public double getLat( int y )
    {
    	return CoordinatesHelper.tilePixY2lat(y+getOffsetYPix(),zoomLevel);
    }

    public void plotNode( Graphics2D g2, int loc, Color c )
    {
        double lat = na.getLatitude(loc);
        double lon = na.getLongitude(loc);
        if (lat < bounds.minLat || lat > bounds.maxLat || lon < bounds.minLon || lon > bounds.maxLon)
        {
            return;
        }

        Color old = g2.getColor();
        g2.setColor(c);
        plot(g2, lat, lon, 4);
        g2.setColor(old);
    }

    public void plot( Graphics2D g2, double lat, double lon, int width )
    {
        double x = getX(lon);
        double y = getY(lat);
        g2.fillOval((int) x, (int) y, width, width);
    }
  
    
    void setCenterCoords(double centerLat,double centerLon){
    	setCenter((int)CoordinatesHelper.lon2PixX(centerLon,zoomLevel),
    			(int)CoordinatesHelper.lat2PixY(centerLat,zoomLevel));
    	
    	
    	
    }
    void scale( int x, int y, boolean zoomIn )
    {
    	
    	
    	double centerLat=CoordinatesHelper.tilePixY2lat(getOffsetYPix()+viewPortHeight/2,zoomLevel);
    	double centerLon=CoordinatesHelper.tilePixX2lon(getOffsetXPix()+viewPortWidth/2,zoomLevel);
    	
    	
    	if (zoomIn)
    		zoomLevel++;
    	else
    		zoomLevel--;
    		
    	setCenterCoords(centerLat, centerLon);
    			
    	
//        double tmpFactor = 0.5f;
//        if (!zoomIn)
//        {
//            tmpFactor = 2;
//        }
//
//        double oldScaleX = scaleX;
//        double oldScaleY = scaleY;
//        double resX = scaleX * tmpFactor;
//        if (resX > 0)
//        {
//            scaleX = resX;
//        }
//
//        double resY = scaleY * tmpFactor;
//        if (resY > 0)
//        {
//            scaleY = resY;
//        }
//
//        // respect mouse x,y when scaling
//        // TODO minor bug: compute difference of lat,lon position for mouse before and after scaling
//        if (zoomIn)
//        {
//            offsetX -= (offsetX + x) * scaleX;
//            offsetY -= (offsetY + y) * scaleY;
//        } else
//        {
//            offsetX += x * oldScaleX;
//            offsetY += y * oldScaleY;
//        }
//
//        logger.info("mouse wheel moved => repaint. zoomIn:" + zoomIn + " " + offsetX + "," + offsetY
//                + " " + scaleX + "," + scaleY);
    }

    public void setNewOffset( int offX, int offY )
    {
    	setCenter(centerXPix - offX,centerYPix - offY);
    }

    public void setCenter( int offX, int offY )
    {
    	logger.info("setCenter("+offX+","+offY+")");
    	centerXPix = offX ;
    	centerYPix = offY;
    	logger.info(CoordinatesHelper.tilePixY2lat(centerYPix, zoomLevel)+","+CoordinatesHelper.tilePixX2lon(centerXPix, zoomLevel));
    	recomputeBounds();
    }

    public void setViewPort( int offX, int offY )
    {
    	double centerLat=CoordinatesHelper.tilePixY2lat(centerXPix,zoomLevel);
    	double centerLon=CoordinatesHelper.tilePixX2lon(centerYPix,zoomLevel);
    	
    	viewPortWidth=offX;
    	viewPortHeight=offY;
    	setCenterCoords(centerLat,centerLon);
    }

    public void recomputeBounds( )
    {
    	
    	
        double minLon = getLon(0);
        double maxLon = getLon(viewPortWidth);

        double maxLat = getLat(0);
        double minLat = getLat(viewPortHeight);
        bounds = new BBox(minLon, maxLon, minLat, maxLat);
        
    }



	public BBox getBounds() {
		return bounds;
	}

	public int getZoomLevel() {
	    return zoomLevel;
    }

	public int getCenterXPix() {
		return centerXPix;
	}

	public void setCenterXPix(int centerXPix) {
		this.centerXPix = centerXPix;
	}

	public int getCenterYPix() {
		return centerYPix;
	}

	public void setCenterYPix(int centerYPix) {
		this.centerYPix = centerYPix;
	}

	public int getViewPortWidth() {
		return viewPortWidth;
	}

	public int getViewPortHeight() {
		return viewPortHeight;
	}

	
}
