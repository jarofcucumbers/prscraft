package dev.jarofcucumbers.prscraft;
/** Coordinates are relative to the map viewport, before the outer GUI scaling. */
public final class MapViewport{
    private double zoom=1,x,y;private final double maxZoom;public MapViewport(){this(3);}public MapViewport(double maxZoom){this.maxZoom=maxZoom;}
    public double zoom(){return zoom;}public double x(){return x;}public double y(){return y;}
    public double worldX(double screen){return (screen-x)/zoom;}public double worldY(double screen){return (screen-y)/zoom;}
    public void fit(double width,double height,double viewWidth,double viewHeight){zoom=Math.max(.04,Math.min(1,Math.min((viewWidth-24)/Math.max(1,width),(viewHeight-24)/Math.max(1,height))));x=(viewWidth-width*zoom)/2;y=(viewHeight-height*zoom)/2;}
    public void pan(double dx,double dy){x+=dx;y+=dy;}
    public void zoomAt(double factor,double px,double py){double wx=worldX(px),wy=worldY(py);zoom=Math.max(.04,Math.min(maxZoom,zoom*factor));x=px-wx*zoom;y=py-wy*zoom;}
}
