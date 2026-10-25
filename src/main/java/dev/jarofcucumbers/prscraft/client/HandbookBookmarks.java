package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.HandbookBookmark;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
final class HandbookBookmarks{
    private HandbookBookmarks(){}
    private static Path file(){var mc=Minecraft.getInstance();String id=mc.player==null?"local":mc.player.getUUID().toString();return mc.gameDirectory.toPath().resolve("config/prscraft/handbook-"+id+".json");}
    static HandbookBookmark load(){try{var path=file();return Files.isRegularFile(path)?HandbookBookmark.read(Files.readString(path,StandardCharsets.UTF_8)):HandbookBookmark.defaults();}catch(java.io.IOException e){return HandbookBookmark.defaults();}}
    static void save(HandbookBookmark state){try{Path path=file();Files.createDirectories(path.getParent());Path temp=Files.createTempFile(path.getParent(),"bookmark-",".tmp");try{Files.writeString(temp,state.json(),StandardCharsets.UTF_8);try{Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}catch(java.io.IOException e){com.mojang.logging.LogUtils.getLogger().warn("Could not save PRSCraft handbook bookmark",e);}}
}
