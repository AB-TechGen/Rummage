import javax.tools.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/** Offline fallback for an already provisioned, unobfuscated Minecraft 26.3 cache.
 * Usage: java tools/CachedBuild.java <directory-of-dependency-jars>
 * Dependencies are only a compiler classpath; none are bundled in the output.
 */
class CachedBuild {
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Pass the directory containing cached dependency jars.");
        Path deps=Path.of(args[0]),classes=Path.of("build/cached-classes");
        Files.createDirectories(classes);
        List<String> jars;
        try(var paths=Files.list(deps)){jars=paths.filter(p->p.toString().endsWith(".jar")).map(p->p.toAbsolutePath().toString()).toList();}
        if(jars.isEmpty())throw new IllegalArgumentException("No dependency jars found");
        List<Path> sources;
        try(var paths=Files.walk(Path.of("src/main/java"))){sources=paths.filter(p->p.toString().endsWith(".java")).toList();}
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager manager=compiler.getStandardFileManager(null,null,StandardCharsets.UTF_8);
        boolean success=compiler.getTask(null,manager,null,List.of("--release","25","-proc:none","-encoding","UTF-8","-classpath",String.join(File.pathSeparator,jars),"-d",classes.toString()),null,manager.getJavaFileObjectsFromPaths(sources)).call();
        // This short-lived compiler process leaves ZIP handles to the OS at exit.
        // Explicit ZipFS close hits a Windows sandbox toRealPath restriction here.
        if(!success)System.exit(1);
        Manifest manifest=new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version","1.0");
        manifest.getMainAttributes().putValue("Main-Class","dev.rummage.net.Synchronizer");
        Path output=Path.of("build/libs/rummage-0.1.0.jar");Files.createDirectories(output.getParent());
        try(JarOutputStream jar=new JarOutputStream(Files.newOutputStream(output),manifest)){
            addTree(jar,classes);addTree(jar,Path.of("src/main/resources"));
            jar.putNextEntry(new JarEntry("LICENSE"));Files.copy(Path.of("LICENSE"),jar);jar.closeEntry();
        }
        System.out.println("Compiled "+sources.size()+" source files. Created "+output+" (no dependencies bundled).");
    }
    private static void addTree(JarOutputStream jar,Path root)throws IOException{
        try(var files=Files.walk(root)){
            for(Path file:files.filter(Files::isRegularFile).sorted().toList()){
                jar.putNextEntry(new JarEntry(root.relativize(file).toString().replace('\\','/')));
                Files.copy(file,jar);jar.closeEntry();
            }
        }
    }
}
