package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
class MessageSignalTest{
 private MessageSettings source(String text){return new MessageSettings("utf8",text,"raw",1000,0,-1,1,0);}
 @Test void fullByteStreamDoesNotDependOnPreviewResolution(){var s=source("A".repeat(1000));var signal=MessageSignal.source(s,true,1);assertEquals(8000,signal.values.length);assertEquals(512,signal.preview(0).length);assertArrayEquals(s.sourceBytes(),signal.channel(.01,0,0,42,false).observed(0).aligned());}
 @Test void noisyBytesAreNotRecoveredFromTheOriginalMetadata(){var s=source("Привіт, лабораторіє!");var signal=MessageSignal.source(s,true,1).channel(.5,20,0,42,false);assertTrue(signal.bitErrors(0)>0);var tag=new CompoundTag();signal.describe(tag,s,true);assertFalse(tag.getBoolean("messageMatch"));assertFalse(tag.getBoolean("messageUtf8"));assertNotEquals(java.util.HexFormat.of().formatHex(s.sourceBytes()),java.util.HexFormat.of().formatHex(tag.getByteArray("messageBytes")));}
 @Test void compressionAndReceiverDecodeUseObservedBytes(){var s=source("A".repeat(500));var signal=MessageSignal.source(s,true,1).channel(.5,0,0,42,false).process(s.operation("huffman"),"message_codec");var tag=new CompoundTag();signal.channel(.5,0,0,17,false).describe(tag,s.operation("decode"),true);assertTrue(tag.getBoolean("messageMatch"));assertEquals("A".repeat(500),tag.getString("messageText"));}
 @Test void mismatchedClocksAndBadReceiverCrcAreExplicitErrors(){var s=source("data");var signal=MessageSignal.source(s,true,1);assertThrows(IllegalArgumentException.class,()->signal.process(new MessageSettings("utf8","","raw",100,0,-1,1,0),"message_codec"));var tag=new CompoundTag();signal.describe(tag,s.operation("crc_decode"),true);assertEquals("data_crc",tag.getString("messageError"));assertFalse(tag.contains("messageText"));}
 @Test void savedMessageAndPlotSettingsRetainTheirBoundaries(){var s=source("Їжак 🌍");assertEquals(s,MessageSettings.read(s.save()));assertEquals(PlotSettings.DEFAULT,PlotSettings.read(PlotSettings.DEFAULT.save()));assertThrows(IllegalArgumentException.class,()->new PlotSettings(false,false,1,1,-1,1,72));assertThrows(IllegalArgumentException.class,()->new PlotSettings(false,false,0,1,Double.NaN,1,72));}
}
