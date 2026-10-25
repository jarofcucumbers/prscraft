package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class BoardDrawingTest{
    @Test void lineHasEndpointsAndNoGaps(){int[] p=BoardDrawing.shape(BoardDrawing.Shape.LINE,1,1,10,10,1);assertEquals(10,p.length);for(int i=1;i<=10;i++){int expected=i*129;assertTrue(Arrays.stream(p).anyMatch(v->v==expected));}}
    @Test void squareAndRectangleAreOutlinesWithUniqueCorners(){assertEquals(40,BoardDrawing.shape(BoardDrawing.Shape.SQUARE,10,10,20,15,1).length);int[] p=BoardDrawing.shape(BoardDrawing.Shape.RECTANGLE,10,10,20,15,1);assertEquals(30,p.length);assertFalse(Arrays.stream(p).anyMatch(v->v==12*128+12));}
    @Test void circleHasCardinalPointsAndClipsOutsideCanvas(){int[] p=BoardDrawing.shape(BoardDrawing.Shape.CIRCLE,64,64,74,64,1);for(int v:new int[]{64*128+54,64*128+74,54*128+64,74*128+64})assertTrue(Arrays.stream(p).anyMatch(i->i==v));assertFalse(Arrays.stream(p).anyMatch(i->i==64*129));int[] clipped=BoardDrawing.shape(BoardDrawing.Shape.CIRCLE,0,0,127,127,16);assertTrue(clipped.length<=16384);for(int v:clipped)assertTrue(v>=0&&v<16384);}
    @Test void thickShapesAndTextChargeDistinctPaintedPixels(){int[] p=BoardDrawing.shape(BoardDrawing.Shape.LINE,20,20,30,20,8);assertEquals(p.length,Arrays.stream(p).distinct().count());assertTrue(p.length>11);int small=BoardDrawing.text("A",0,0,1).length;assertEquals(small*4,BoardDrawing.text("A",0,0,2).length);assertEquals(0,BoardDrawing.text("   ",0,0,1).length);}
    @Test void ukrainianLettersAreRealGlyphsAndUnsupportedCharactersFallback(){for(String c:new String[]{"І","Ї","Є","Ґ","Ж","Я"})assertFalse(Arrays.equals(BoardDrawing.text(c,0,0,1),BoardDrawing.text("?",0,0,1)),c);assertArrayEquals(BoardDrawing.text("ПРИВІТ",0,0,1),BoardDrawing.text("привіт",0,0,1));assertTrue(BoardDrawing.text("ЇЇЇ",126,126,4).length<=4);}
}
