import org.antlr.v4.runtime.misc.Pair;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.presets.LLVM;

import java.util.ArrayList;
import java.util.List;

public class FunctionSymbol extends BaseScope implements Symbol {

    private final LLVMValueRef ref;

    private final String returnType;
    public FunctionSymbol(String name, Scope enclosingScope, LLVMValueRef ref, String returnType) {
        super(name, enclosingScope);
        this.ref = ref;
        this.returnType = returnType;
    }

    @Override
    public LLVMValueRef getRef() {
        return ref;
    }

    public String getReturnType() {
        return returnType;
    }
}