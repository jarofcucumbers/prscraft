package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.*;
/** Note edits never replace a captured result. Entries are deep-copied at the experiment boundary. */
public final class NotebookRecords{
    private NotebookRecords(){}
    public static void annotate(CompoundTag entry,String title,String description){if(title==null||description==null||title.length()>64||description.length()>2048)throw new IllegalArgumentException("notebook_entry");entry.putString("title",title);entry.putString("description",description);}
    public static void append(ListTag entries,CompoundTag entry){if(entries.size()>=8)throw new IllegalArgumentException("notebook_full");annotate(entry,entry.getString("title"),entry.getString("description"));if(entry.contains("capture")&&entry.getCompound("capture").getIntArray("received").length!=512)throw new IllegalArgumentException("notebook_capture");if(entry.getString("codebook").length()>8192)throw new IllegalArgumentException("codebook_size");entries.add(entry.copy());}
}
