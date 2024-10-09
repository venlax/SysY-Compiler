import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class VariableSymbol extends BaseSymbol {
    public VariableSymbol(String name, LLVMValueRef ref) {
        super(name,  ref);
    }
}