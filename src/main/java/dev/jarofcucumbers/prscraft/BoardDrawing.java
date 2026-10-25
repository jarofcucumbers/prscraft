package dev.jarofcucumbers.prscraft;
import java.util.*;
/** Pixel geometry shared by preview and commit; each unique pixel costs one use. */
public final class BoardDrawing{
    private BoardDrawing(){}
    public enum Shape{FREEHAND,LINE,SQUARE,RECTANGLE,CIRCLE,TEXT}
    private static void dot(Set<Integer> points,int x,int y,int size){for(int p:BoardPixels.brush(x,y,size))points.add(p);}
    private static void line(Set<Integer> points,int x0,int y0,int x1,int y1,int size){int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;while(true){dot(points,x0,y0,size);if(x0==x1&&y0==y1)break;int twice=2*err;if(twice>=dy){err+=dy;x0+=sx;}if(twice<=dx){err+=dx;y0+=sy;}}}
    public static int[] shape(Shape shape,int x0,int y0,int x1,int y1,int size){
        for(int v:new int[]{x0,y0,x1,y1})if(v<0||v>127)throw new IllegalArgumentException("pixels");if(size<1||size>16)throw new IllegalArgumentException("brush");var points=new LinkedHashSet<Integer>();
        switch(shape){
            case LINE,FREEHAND->line(points,x0,y0,x1,y1,size);
            case SQUARE,RECTANGLE->{if(shape==Shape.SQUARE){int d=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));x1=x0+(x1<x0?-d:d);y1=y0+(y1<y0?-d:d);}line(points,x0,y0,x1,y0,size);line(points,x1,y0,x1,y1,size);line(points,x1,y1,x0,y1,size);line(points,x0,y1,x0,y0,size);}
            case CIRCLE->{int r=Math.max(Math.abs(x1-x0),Math.abs(y1-y0)),x=r,y=0,error=1-r;while(x>=y){for(int[] p:new int[][]{{x,y},{y,x},{-y,x},{-x,y},{-x,-y},{-y,-x},{y,-x},{x,-y}})dot(points,x0+p[0],y0+p[1],size);y++;if(error<0)error+=2*y+1;else{x--;error+=2*(y-x)+1;}}}
            case TEXT->throw new IllegalArgumentException("shape");
        }return points.stream().mapToInt(Integer::intValue).toArray();
    }
    public static int[] text(String text,int x,int y,int scale){
        if(text==null||text.length()>128||scale<1||scale>4||x<0||x>127||y<0||y>127)throw new IllegalArgumentException("text");var points=new LinkedHashSet<Integer>();int origin=x;
        for(int cp:text.toUpperCase(Locale.ROOT).codePoints().toArray()){
            if(cp=='\n'){x=origin;y+=8*scale;continue;}if(cp!=' '){int[] glyph=ChalkFont.glyph(cp);for(int row=0;row<7;row++)for(int col=0;col<5;col++)if((glyph[row]&(1<<(4-col)))!=0)for(int yy=0;yy<scale;yy++)for(int xx=0;xx<scale;xx++){int px=x+col*scale+xx,py=y+row*scale+yy;if(px>=0&&px<128&&py>=0&&py<128)points.add(py*128+px);}}
            x+=6*scale;
        }return points.stream().mapToInt(Integer::intValue).toArray();
    }
}
