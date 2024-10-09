import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface Symbol {

    String getName();


    LLVMValueRef getRef();
}