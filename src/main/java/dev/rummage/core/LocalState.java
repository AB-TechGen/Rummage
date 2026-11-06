package dev.rummage.core;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Atomic local preferences and recovery metadata; never logs invite credentials. */
public final class LocalState {
    private final Path file;
    private final Properties values=new Properties();
    public LocalState(Path file) { this.file=file; if(Files.exists(file))try(var in=Files.newInputStream(file)){values.load(in);}catch(IOException e){throw new UncheckedIOException(e);} }
    public synchronized String get(String key,String fallback){return values.getProperty(key,fallback);}
    public synchronized int number(String key,int fallback){try{return Integer.parseInt(get(key,""));}catch(NumberFormatException e){return fallback;}}
    public synchronized boolean flag(String key,boolean fallback){return Boolean.parseBoolean(get(key,""+fallback));}
    public synchronized void put(String key,Object value){values.setProperty(key,String.valueOf(value));}
    public synchronized void save(){
        try{Files.createDirectories(file.toAbsolutePath().getParent());Path tmp=file.resolveSibling(file.getFileName()+".tmp");try(var out=Files.newOutputStream(tmp)){values.store(out,"Rummage local settings - includes private reconnect credentials");}try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException e){throw new UncheckedIOException(e);}
    }
}
