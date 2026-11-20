import java.nio.file.*;import java.util.zip.*;import org.objectweb.asm.*;
public class WidenCompile{public static void main(String[] args)throws Exception{
 try(var in=new ZipFile(args[0]);var out=new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))){
 for(var e:java.util.Collections.list(in.entries())){byte[] b=in.getInputStream(e).readAllBytes();
 if(e.getName().equals("net/minecraft/world/item/CreativeModeTab$Output.class")||e.getName().equals("net/minecraft/world/item/CreativeModeTab.class")){
 var cr=new ClassReader(b);var cw=new ClassWriter(0);cr.accept(new ClassVisitor(Opcodes.ASM9,cw){
 int access(int a){return(a&~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED))|Opcodes.ACC_PUBLIC;}
 public void visit(int v,int a,String n,String sig,String s,String[] i){super.visit(v,access(a),n,sig,s,i);}
 public void visitInnerClass(String n,String o,String i,int a){super.visitInnerClass(n,o,i,access(a));}
 },0);b=cw.toByteArray();}
 out.putNextEntry(new ZipEntry(e.getName()));out.write(b);out.closeEntry();}}
 }}
