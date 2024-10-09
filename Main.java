import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef;
import org.bytedeco.llvm.LLVM.LLVMPassManagerRef;
import org.bytedeco.llvm.global.LLVM;

import java.io.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class Main {
    public static void main(String[] args) throws IOException {
//        if (args.length < 1) {
//            System.err.println("input path is required");
//            return;
//        }

        String src = args[0];
        //String filePath = "main.SysY";
        CharStream input = CharStreams.fromFileName(src);

        SysYLexer lexer = new SysYLexer(input);

        //lexer.removeErrorListeners();

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SysYParser parser = new SysYParser(tokens);

        //parser.removeErrorListeners();

        MyLLVMSysYParserVisitor myLLVMSysYParserVisitor = new MyLLVMSysYParserVisitor();

        myLLVMSysYParserVisitor.visit(parser.program());
        // 添加优化通道


        AsmCodeGenerator asmCodeGenerator = new AsmCodeGenerator();

        asmCodeGenerator.generateAsmCode(myLLVMSysYParserVisitor.module, args[1]);
    }
}