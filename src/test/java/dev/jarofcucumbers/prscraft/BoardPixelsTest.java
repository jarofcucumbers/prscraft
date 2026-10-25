package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class BoardPixelsTest{
    @Test void brushesChargeOnlyThePartInsideTheCanvas(){assertEquals(1,BoardPixels.brush(0,0,1).length);assertEquals(256,BoardPixels.brush(64,64,16).length);assertEquals(64,BoardPixels.brush(0,0,16).length);assertThrows(IllegalArgumentException.class,()->BoardPixels.brush(0,0,17));}
    @Test void invalidPacketsAreRejectedAsAWhole(){assertThrows(IllegalArgumentException.class,()->BoardPixels.checked(new int[]{0,16384}));assertThrows(IllegalArgumentException.class,()->BoardPixels.checked(new int[257]));assertArrayEquals(new int[]{0,128},BoardPixels.checked(new int[]{0,0,128}));}
    @Test void corruptedPaletteAndSizeNeverReachTheRenderer(){assertEquals(16384,BoardPixels.read(new byte[2]).length);byte[] bytes=new byte[16384];bytes[0]=-1;bytes[1]=17;bytes[2]=16;byte[] safe=BoardPixels.read(bytes);assertEquals(0,safe[0]);assertEquals(0,safe[1]);assertEquals(16,safe[2]);safe[2]=0;assertEquals(16,bytes[2]);}
    @Test void largeCanvasNeedsEightColorsAndHalfTheArea(){byte[] p=new byte[16384];for(int i=0;i<8192;i++)p[i]=(byte)(i%8+1);assertTrue(ChallengeRules.fullPalette(p));p[8191]=0;assertFalse(ChallengeRules.fullPalette(p));java.util.Arrays.fill(p,(byte)1);assertFalse(ChallengeRules.fullPalette(p));}
    @Test void muralRequiresBothAreaAndColorDiversity(){byte[] p=new byte[16384];Arrays.fill(p,0,256,(byte)1);assertFalse(ChallengeRules.mural(p));p[1]=2;p[2]=3;assertTrue(ChallengeRules.mural(p));p[255]=0;assertFalse(ChallengeRules.mural(p));}
}
