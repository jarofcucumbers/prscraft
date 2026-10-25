package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GrayCodecTest{
    @Test void allSixteenBitWordsRoundTripAndAdjacentCodesDifferByOneBit(){for(int i=0;i<65536;i++){assertEquals(i,GrayCodec.decode(GrayCodec.encode(i)));if(i>0)assertEquals(1,Integer.bitCount(GrayCodec.encode(i)^GrayCodec.encode(i-1)));}}
    @Test void examplesRespectWordBoundariesAndPartialWords(){assertEquals("1110",GrayCodec.convert("1011",4,false));assertEquals("1011",GrayCodec.convert("1110",4,true));assertEquals("00111110",GrayCodec.convert("00101011",4,false));assertEquals("101101",GrayCodec.convert(GrayCodec.convert("101101",4,false),4,true));}
    @Test void levelIndicesSerializeToGrayThenBackToBinary(){double[] x=new double[512];for(int i=0;i<512;i++)x[i]=-1+2*(i/128)/3.;var s=new GraySettings("levels_to_gray",2,10,0,-1,1,4,-1,1,10);var result=GrayCodec.process(x,.4,s);assertEquals("0,1,2,3",result.input());assertEquals("00011110",result.output());var reverse=GrayCodec.process(result.samples(),.4,new GraySettings("gray_to_binary",2,20,0,-1,1,4,-1,1,10));assertEquals("00011011",reverse.output());}
    @Test void levelsNeedEnoughBitsAndCapturesStayBounded(){assertThrows(IllegalArgumentException.class,()->new GraySettings("levels_to_gray",2,10,0,-1,1,16,-1,1,100));assertThrows(IllegalArgumentException.class,()->GrayCodec.process(new double[512],16,GraySettings.DEFAULT));assertThrows(IllegalArgumentException.class,()->GrayCodec.convert("102",4,false));}
    @Test void settingsRoundTrip(){assertEquals(GraySettings.DEFAULT,GraySettings.read(GraySettings.DEFAULT.save()));}
}
