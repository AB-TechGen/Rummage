import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** generate <private-key-file> OR sign <key> <version> <https-jar-url> <jar> <manifest> */
class SignRelease {
    public static void main(String[] args)throws Exception{
        if(args.length==2&&args[0].equals("generate")){
            var pair=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Files.write(Path.of(args[1]),pair.getPrivate().getEncoded(),StandardOpenOption.CREATE_NEW);
            System.out.println("Trusted public key: "+Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));return;
        }
        if(args.length!=6||!args[0].equals("sign"))throw new IllegalArgumentException("generate <private-key-file> OR sign <key> <version> <https-jar-url> <jar> <manifest>");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[4]))));
        Signature s=Signature.getInstance("Ed25519");s.initSign(KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(Path.of(args[1])))));
        s.update((args[2]+"\n"+args[3]+"\n"+hash+"\n").getBytes(StandardCharsets.UTF_8));
        Properties p=new Properties();p.setProperty("version",args[2]);p.setProperty("url",args[3]);p.setProperty("sha256",hash);p.setProperty("signature",Base64.getEncoder().encodeToString(s.sign()));
        try(var out=Files.newOutputStream(Path.of(args[5]))){p.store(out,"Rummage signed update");}
    }
}
