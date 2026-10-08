import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
public final class ParseJava {
 public static void main(String[]args)throws Exception{
  List<java.io.File>files=new ArrayList<>();try(java.util.stream.Stream<Path>paths=Files.walk(Paths.get(args[0]))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toFile()));}
  JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();DiagnosticCollector<JavaFileObject>d=new DiagnosticCollector<>();
  try(StandardJavaFileManager fm=compiler.getStandardFileManager(d,null,java.nio.charset.StandardCharsets.UTF_8)){
   JavacTask task=(JavacTask)compiler.getTask(null,fm,d,Arrays.asList("--release","11","-proc:none"),null,fm.getJavaFileObjectsFromFiles(files));task.parse();boolean error=false;
   for(Diagnostic<?>msg:d.getDiagnostics())if(msg.getKind()==Diagnostic.Kind.ERROR){System.out.println(msg);error=true;}
   if(error)throw new AssertionError("Java syntax parsing failed");System.out.println("PASS syntax parsed "+files.size()+" Java application files");
  }
 }
}
