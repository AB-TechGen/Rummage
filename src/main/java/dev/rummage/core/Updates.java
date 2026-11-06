package dev.rummage.core;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.io.*;
import java.time.Duration;

/** Ed25519 signatures cover the version, URL and SHA-256, not just a mutable release page. */
public final class Updates {
    public record Release(String version,String url,String sha256) {}
    public static Release verify(Properties p,String trustedKey)throws Exception {
        String version=p.getProperty("version",""),url=p.getProperty("url",""),hash=p.getProperty("sha256","");
        if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!hash.matches("[a-f0-9]{64}")||!URI.create(url).getScheme().equals("https"))throw new IOException("Invalid update manifest");
        Signature verifier=Signature.getInstance("Ed25519");verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(trustedKey))));
        verifier.update(payload(version,url,hash));
        if(!verifier.verify(Base64.getDecoder().decode(p.getProperty("signature",""))))throw new SignatureException("Update signature rejected");
        return new Release(version,url,hash);
    }
    public static byte[] payload(String version,String url,String hash){return (version+"\n"+url+"\n"+hash+"\n").getBytes(StandardCharsets.UTF_8);}
    public static boolean newer(String candidate,String current){String[] a=candidate.split("\\."),b=current.split("\\.");for(int i=0;i<3;i++){int d=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(d!=0)return d>0;}return false;}
    private static byte[] fetch(String url,int limit)throws Exception {
        URI uri=URI.create(url);if(!"https".equals(uri.getScheme()))throw new IOException("Updates require HTTPS");
        try(HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()){
            for(int redirects=0;redirects<5;redirects++){
                var response=client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(45)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
                try(var in=response.body()){
                    if(response.statusCode()>=300&&response.statusCode()<400){uri=uri.resolve(response.headers().firstValue("location").orElseThrow());if(!"https".equals(uri.getScheme()))throw new IOException("Unsafe redirect");continue;}
                    if(response.statusCode()!=200)throw new IOException("Update HTTP "+response.statusCode());
                    byte[] bytes=in.readNBytes(limit+1);if(bytes.length>limit)throw new IOException("Update exceeds size limit");return bytes;
                }
            }throw new IOException("Too many redirects");
        }
    }
    public static Release check(String url,String key)throws Exception{Properties p=new Properties();p.load(new ByteArrayInputStream(fetch(url,16384)));Release r=verify(p,key);return newer(r.version,Match.VERSION)?r:null;}
    public static Path download(Release release,Path directory)throws Exception{
        byte[] data=fetch(release.url,32*1024*1024);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        if(!hash.equals(release.sha256))throw new SignatureException("Downloaded jar hash rejected");
        Files.createDirectories(directory);Path jar=directory.resolve("rummage-"+release.version+".jar");Files.write(jar,data);return jar;
    }
    private Updates(){}
}
