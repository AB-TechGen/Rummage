package dev.rummage.core;
import java.nio.file.*;
import java.security.*;
import java.util.*;
/** Runs from a separate helper jar, waits for Minecraft to exit, and retains a rollback copy. */
public final class UpdateInstaller {
    public static void launch(Path current,Path downloaded,String hash)throws Exception{
        current=current.toAbsolutePath().normalize();downloaded=downloaded.toAbsolutePath().normalize();
        if(!Files.isRegularFile(current)||!current.getFileName().toString().endsWith(".jar")||!current.getParent().getFileName().toString().equals("mods"))throw new IllegalArgumentException("Automatic install requires a jar in the mods directory. Install the verified download manually in development.");
        Path helper=downloaded.getParent().resolve("installer.jar");Files.copy(current,helper,StandardCopyOption.REPLACE_EXISTING);
        boolean windows=System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");Path java=Path.of(System.getProperty("java.home"),"bin",windows?"javaw.exe":"java");
        new ProcessBuilder(java.toString(),"-cp",helper.toString(),UpdateInstaller.class.getName(),""+ProcessHandle.current().pid(),current.toString(),downloaded.toString(),hash).redirectErrorStream(true).redirectOutput(downloaded.getParent().resolve("install.log").toFile()).start();
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=4)throw new IllegalArgumentException("Expected PID, current jar, verified download, hash");
        ProcessHandle parent=ProcessHandle.of(Long.parseLong(args[0])).orElse(null);if(parent!=null)parent.onExit().get();
        Path old=Path.of(args[1]).toAbsolutePath().normalize(),download=Path.of(args[2]).toAbsolutePath().normalize();
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(download)));if(!hash.equals(args[3]))throw new SecurityException("Update changed after verification");
        if(Files.isSymbolicLink(old)||Files.isSymbolicLink(download)||!old.getParent().getFileName().toString().equals("mods"))throw new SecurityException("Invalid install path");
        Path target=old.getParent().resolve(download.getFileName()),backup=download.getParent().resolve("previous.jar.backup");
        if(Files.exists(target)&&!target.equals(old))throw new IllegalStateException("Target already exists: "+target);
        Path ready=Files.createTempFile(old.getParent(),"rummage-install-",".part");
        try{Files.copy(download,ready,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){Files.deleteIfExists(ready);throw e;}
        for(int attempt=0;attempt<30;attempt++){
            try{Files.move(old,backup,StandardCopyOption.REPLACE_EXISTING);try{Files.move(ready,target);}catch(Exception e){Files.move(backup,old,StandardCopyOption.REPLACE_EXISTING);throw e;}System.out.println("Installed "+target.getFileName());return;}
            catch(java.io.IOException e){if(attempt==29){Files.deleteIfExists(ready);throw e;}Thread.sleep(1000);}
        }
    }
}
