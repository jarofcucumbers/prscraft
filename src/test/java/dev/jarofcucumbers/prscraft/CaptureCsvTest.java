package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CaptureCsvTest {
    @Test void exportsAllSamplesOnTheSameTimeGrid(){
        double[] samples=new double[512];samples[1]=.5;int[] values=SignalChain.pack(samples);
        String csv=CaptureCsv.encode(1,new int[][]{values,values,values,values,values});
        String[] lines=csv.split("\n");assertEquals(513,lines.length);
        assertEquals("0.001953125,0.5,0.5,0.5,0.5,0.5",lines[2]);
        assertThrows(IllegalArgumentException.class,()->CaptureCsv.encode(1,new int[][]{values}));
    }
}
