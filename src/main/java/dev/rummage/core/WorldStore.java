package dev.rummage.core;

import java.nio.file.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;

/** Only marked Rummage UUID directories are eligible; favorites and locked worlds are retained. */
public final class WorldStore {
    public record World(String folder,boolean favorite,long modified){}
    private final Path root;
    public WorldStore(Path root){this.root=root.toAbsolutePath().normalize();}
    private Path path(String folder)throws IOException {
        if(!folder.matches("Rummage [0-9a-f-]{36}"))throw new IOException("Not a Rummage match world");
        Path p=root.resolve(folder).normalize();if(!p.getParent().equals(root)||Files.isSymbolicLink(p)||!Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid world directory");return p;
    }
    public void mark(String folder)throws IOException{Path p=path(folder);Files.writeString(p.resolve(".rummage-world"),folder);}
    private boolean marked(Path p)throws IOException{Path marker=p.resolve(".rummage-world");return !Files.isSymbolicLink(marker)&&Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)&&Files.readString(marker).equals(p.getFileName().toString());}
    public List<World> list()throws IOException{
        if(!Files.isDirectory(root))return List.of();List<World> result=new ArrayList<>();
        try(var paths=Files.list(root)){for(Path p:paths.toList()){try{p=path(p.getFileName().toString());if(marked(p))result.add(new World(p.getFileName().toString(),Files.exists(p.resolve(".rummage-favorite")),Files.getLastModifiedTime(p.resolve("level.dat")).toMillis()));}catch(IOException ignored){}}}
        result.sort(Comparator.comparingLong(World::modified).reversed());return result;
    }
    public void favorite(String folder,boolean value)throws IOException{Path p=path(folder);if(!marked(p))throw new IOException("Unmarked world");if(value)Files.writeString(p.resolve(".rummage-favorite"),"keep");else Files.deleteIfExists(p.resolve(".rummage-favorite"));}
    public int prune(int keep,String active)throws IOException {
        if(keep<=0)return 0;int count=0,removed=0;
        for(World world:list()){
            if(world.favorite||world.folder.equals(active))continue;
            if(count++<keep)continue;
            Path p=path(world.folder);if(!marked(p))continue;
            Path lock=p.resolve("session.lock");if(Files.isSymbolicLink(lock))continue;
            // Hold the Minecraft world lock until all other files have been removed.
            try(FileChannel channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock held=channel.tryLock()){
                if(held==null)continue;
                try(var paths=Files.walk(p)){for(Path item:paths.sorted(Comparator.reverseOrder()).toList()){if(!item.equals(p)&&!item.equals(lock))Files.deleteIfExists(item);}}
            }catch(OverlappingFileLockException|AccessDeniedException e){continue;}
            Files.deleteIfExists(lock);Files.deleteIfExists(p);removed++;
        }return removed;
    }
}
