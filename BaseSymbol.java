
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.ArrayList;

public class BaseSymbol implements Symbol {
    final String name;
    final LLVMValueRef ref;


    public BaseSymbol(String name, LLVMValueRef ref) {
        this.name = name;
        this.ref = ref;

    }

    public String getName() {
        return name;
    }

    @Override
    public LLVMValueRef getRef() {
        return ref;
    }


}