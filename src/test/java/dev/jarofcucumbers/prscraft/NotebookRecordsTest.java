package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.*;
class NotebookRecordsTest{
 @Test void experimentCapturesAreFrozenAndAnnotationsDontReplaceResults(){var entries=new ListTag();var entry=new CompoundTag();NotebookRecords.annotate(entry,"Experiment","Hypothesis");var capture=new CompoundTag();int[] points=new int[512];points[0]=42;capture.putIntArray("received",points);entry.put("capture",capture);NotebookRecords.append(entries,entry);capture.putIntArray("received",new int[512]);entry.putString("title","live title changed");var saved=entries.getCompound(0);NotebookRecords.annotate(saved,"Final title","Observed result");assertEquals(42,saved.getCompound("capture").getIntArray("received")[0]);assertEquals("Final title",saved.getString("title"));}
 @Test void resourceAndTextLimitsAreAtomic(){var entries=new ListTag();var entry=new CompoundTag();NotebookRecords.annotate(entry,"Title","");for(int i=0;i<8;i++)NotebookRecords.append(entries,entry);assertThrows(IllegalArgumentException.class,()->NotebookRecords.append(entries,entry));assertEquals(8,entries.size());assertThrows(IllegalArgumentException.class,()->NotebookRecords.annotate(entry,"x".repeat(65),""));assertEquals("Title",entry.getString("title"));}
 @Test void invalidCaptureIsRejectedAndBlankNotesAreAllowed(){var entry=new CompoundTag();entry.put("capture",new CompoundTag());assertThrows(IllegalArgumentException.class,()->NotebookRecords.append(new ListTag(),entry));var blank=new ListTag();NotebookRecords.append(blank,new CompoundTag());assertEquals(1,blank.size());}
}
