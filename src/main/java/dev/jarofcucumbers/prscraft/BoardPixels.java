package dev.jarofcucumbers.prscraft;
import java.util.*;
/** Palette indices persist independently of renderer and dye item IDs. */
public final class BoardPixels {
    private BoardPixels(){}
    public static final int SIZE=128,COUNT=SIZE*SIZE;
    public static final String[] COLORS={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    public static final int[] RGB={0xfff5f3e8,0xffeb9748,0xffd079d8,0xff82cde9,0xffedda63,0xffa6d666,0xffedacc5,0xff70777d,0xffb9c0c2,0xff58b8ba,0xffa787d5,0xff728cdd,0xffac8161,0xff7baf73,0xffdf7772,0xff22292c};
    public static int color(int palette){return palette>=1&&palette<=16?RGB[palette-1]:0xff20372f;}
    public static int[] brush(int x,int y,int size){
        if(size<1||size>16)throw new IllegalArgumentException("brush");
        var pixels=new java.util.ArrayList<Integer>();int lo=-(size/2);
        for(int yy=y+lo;yy<y+lo+size;yy++)for(int xx=x+lo;xx<x+lo+size;xx++)if(xx>=0&&xx<SIZE&&yy>=0&&yy<SIZE)pixels.add(yy*SIZE+xx);
        return pixels.stream().mapToInt(Integer::intValue).toArray();
    }
    /** Reject the whole packet before charging or changing anything. */
    public static int[] checked(int[] pixels){
        if(pixels==null||pixels.length==0||pixels.length>COUNT)throw new IllegalArgumentException("pixels");
        for(int p:pixels)if(p<0||p>=COUNT)throw new IllegalArgumentException("pixels");
        return Arrays.stream(pixels).distinct().toArray();
    }
    public static byte[] read(byte[] bytes){if(bytes.length!=COUNT)return new byte[COUNT];byte[] result=bytes.clone();for(int i=0;i<result.length;i++)if(result[i]<0||result[i]>16)result[i]=0;return result;}
}
