import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks compiled injection target/shadow descriptors without starting a game. Not a runtime smoke test. */
class VerifyMixins {
    static Object value(AnnotationNode a,String key){for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals(key))return a.values.get(i+1);return null;}
    static List<AnnotationNode> annotations(List<AnnotationNode> a,List<AnnotationNode>b){List<AnnotationNode> all=new ArrayList<>();if(a!=null)all.addAll(a);if(b!=null)all.addAll(b);return all;}
    static ClassNode read(byte[] bytes){ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,ClassReader.SKIP_CODE);return n;}
    public static void main(String[] args)throws Exception{
        int checks=0;
        try(JarFile minecraft=new JarFile(args[0]);var files=Files.walk(Path.of(args[1]))){
            for(Path p:files.filter(f->f.toString().endsWith("Mixin.class")).toList()){
                ClassNode mixin=read(Files.readAllBytes(p));
                for(var a:annotations(mixin.visibleAnnotations,mixin.invisibleAnnotations))if(a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")){
                    for(Object type:(List<?>)value(a,"value")){
                        String name=((Type)type).getInternalName();var entry=minecraft.getJarEntry(name+".class");if(entry==null)throw new AssertionError("Missing target "+name);
                        ClassNode target;try(var in=minecraft.getInputStream(entry)){target=read(in.readAllBytes());}
                        for(var field:mixin.fields)for(var annotation:annotations(field.visibleAnnotations,field.invisibleAnnotations))if(annotation.desc.endsWith("/Shadow;")){
                            if(target.fields.stream().noneMatch(f->f.name.equals(field.name)&&f.desc.equals(field.desc)))throw new AssertionError("Shadow mismatch "+name+"."+field.name);checks++;
                        }
                        for(var method:mixin.methods)for(var annotation:annotations(method.visibleAnnotations,method.invisibleAnnotations))if(annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;")){
                            for(Object selector:(List<?>)value(annotation,"method")){
                                String selectorText=selector.toString();int bracket=selectorText.indexOf('(');String methodName=bracket<0?selectorText:selectorText.substring(0,bracket);
                                var matches=target.methods.stream().filter(m->m.name.equals(methodName)&&(bracket<0||m.desc.equals(selectorText.substring(bracket)))).toList();
                                Type[] handler=Type.getArgumentTypes(method.desc);Type replacement=Type.getReturnType(method.desc);
                                if(matches.isEmpty()||handler.length!=1||!handler[0].equals(replacement)||matches.stream().anyMatch(m->Arrays.stream(Type.getArgumentTypes(m.desc)).noneMatch(replacement::equals)))throw new AssertionError("Argument modifier mismatch "+name+"."+selectorText);
                                checks++;
                            }
                        }
                        for(var method:mixin.methods)for(var annotation:annotations(method.visibleAnnotations,method.invisibleAnnotations))if(annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")){
                            for(Object selector:(List<?>)value(annotation,"method")){
                                String s=selector.toString();int bracket=s.indexOf('(');String methodName=bracket<0?s:s.substring(0,bracket),descriptor=bracket<0?null:s.substring(bracket);
                                var matches=target.methods.stream().filter(m->m.name.equals(methodName)&&(descriptor==null||descriptor.equals(m.desc))).toList();
                                if(matches.isEmpty())throw new AssertionError("Missing injection target "+name+"."+s);
                                Type[] callback=Type.getArgumentTypes(method.desc);
                                for(var match:matches){Type[] expected=Type.getArgumentTypes(match.desc);if(callback.length!=1){if(callback.length!=expected.length+1)throw new AssertionError("Handler arity "+method.name);for(int i=0;i<expected.length;i++)if(!callback[i].equals(expected[i]))throw new AssertionError("Handler descriptor "+method.name);}}
                                checks++;
                            }
                        }
                    }
                }
            }
        }System.out.println("PASS: "+checks+" mixin injection/shadow descriptor checks. Runtime application still requires Minecraft.");
    }
}
