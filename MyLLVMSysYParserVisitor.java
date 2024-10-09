import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;

import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;


public class MyLLVMSysYParserVisitor extends SysYParserBaseVisitor<LLVMValueRef>{

    private GlobalScope globalScope = null;
    private Scope currentScope = null;

    private Scope currentFuncScope = null;
    private boolean _return = false;

    private LLVMValueRef function = null;

    private int count = 0;

    private Stack<LLVMBasicBlockRef> loopStack = new Stack<>();
    private Stack<LLVMBasicBlockRef> loopEndStack = new Stack<>();

    private LLVMPassManagerRef passManager;

    public MyLLVMSysYParserVisitor() {
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();



        passManager = LLVMCreatePassManager();
        LLVMAddPromoteMemoryToRegisterPass(passManager);
        LLVMAddInstructionCombiningPass(passManager);
        LLVMAddReassociatePass(passManager);
        LLVMAddGVNPass(passManager);
        LLVMAddCFGSimplificationPass(passManager);
        LLVMInitializeFunctionPassManager(passManager);
    }
    //创建module
    public LLVMModuleRef module = LLVMModuleCreateWithName("module");

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    private final LLVMTypeRef i32Type = LLVMInt32Type();
    private final LLVMTypeRef voidType = LLVMVoidType();
    //创建一个常量,这里是常数0
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);



    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        LLVMValueRef ret = visitChildren(ctx);
        currentScope = currentScope.getEnclosingScope();
        optimizeModule();
        //System.out.println(LLVMPrintModuleToString(module).getString());
        return ret;
    }


    @Override
    public LLVMValueRef visitConstDef(SysYParser.ConstDefContext ctx) {
        //TODO
        if (currentScope == globalScope) {

            LLVMValueRef constDef = LLVMAddGlobal(module, LLVMInt32Type(), "global_" + ctx.IDENT().getText());

            LLVMValueRef init = visit(ctx.constInitVal());

            LLVMSetInitializer(constDef, init);

            //LLVMSetGlobalConstant(constDef, 1);

            VariableSymbol variableSymbol = new VariableSymbol(ctx.IDENT().getText(), constDef);
            currentScope.define(variableSymbol);

        } else {
            LLVMValueRef constDef = LLVMBuildAlloca(builder, LLVMInt32Type(), ctx.IDENT().getText());

            LLVMValueRef init = visit(ctx.constInitVal());

            LLVMBuildStore(builder, init, constDef);

            VariableSymbol variableSymbol = new VariableSymbol(ctx.IDENT().getText(), constDef);
            currentScope.define(variableSymbol);

        }
        return null;
    
    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx) {

        //TODO
        if (currentScope == globalScope){
            LLVMValueRef varDef = LLVMAddGlobal(module, LLVMInt32Type(), "global_" + ctx.IDENT().getText());



            LLVMValueRef init;

            if (ctx.initVal() != null)  init  = visit(ctx.initVal());
            else init = zero;

            LLVMSetInitializer(varDef, init);

            //LLVMSetGlobalConstant(varDef, 0);

            VariableSymbol variableSymbol = new VariableSymbol(ctx.IDENT().getText(), varDef);
            currentScope.define(variableSymbol);
        } else {
            LLVMValueRef varDef = LLVMBuildAlloca(builder, LLVMInt32Type(), ctx.IDENT().getText());

            LLVMValueRef init;

            if (ctx.initVal() != null) { 
                init  = visit(ctx.initVal());
            // else init = zero;

                LLVMBuildStore(builder, init, varDef);

            }
            VariableSymbol variableSymbol = new VariableSymbol(ctx.IDENT().getText(), varDef);
            currentScope.define(variableSymbol);
        }
        return null;
    }



    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //TODO
        String name = ctx.IDENT().getText();
        int size = ctx.funcFParams() == null ? 0 : ctx.funcFParams().funcFParam().size();
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(size);
        for (int i = 0; i < size; i++) {
            argumentTypes.put(i, LLVMInt32Type());
        }
        LLVMTypeRef returnType = (ctx.funcType().VOID() != null) ? LLVMVoidType() : LLVMInt32Type();
        LLVMTypeRef funcType = LLVMFunctionType(returnType, argumentTypes, size, 0);
        LLVMValueRef func = LLVMAddFunction(module, name, funcType);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlock(func, name + "Entry");
        LLVMPositionBuilderAtEnd(builder, entry);

        FunctionSymbol functionSymbol = new FunctionSymbol(name, currentScope, func, ctx.funcType().getText());
        currentScope.define(functionSymbol);
        currentFuncScope = functionSymbol;
        currentScope = currentFuncScope;
        for (int i = 0; i < size; ++i) {
            SysYParser.FuncFParamContext param = ctx.funcFParams().funcFParam(i);
            String varName = param.IDENT().getText();
            LLVMValueRef varPointer = LLVMBuildAlloca(builder,LLVMInt32Type() , varName);
            currentScope.define(new VariableSymbol(varName, varPointer));
            LLVMValueRef argValue = LLVMGetParam(func, i);
            LLVMBuildStore(builder, argValue, varPointer);
        }


        
        super.visitFuncDef(ctx);
        currentScope = currentScope.getEnclosingScope();

        if(returnType.equals(LLVMVoidType()))
            LLVMBuildRet(builder, null);
        else
            LLVMBuildRet(builder, zero);

        return func;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope = new LocalScope(currentScope);
        localScope.setName("block" + (count++)); // 在Lab4中由于只定义main函数,导致此处错误未被发现...
        currentScope = localScope;
        LLVMValueRef ret = super.visitBlock(ctx);
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null) {
            LLVMValueRef result = null;
            if (ctx.exp() != null) {
                result = visit(ctx.exp());
            }

            return LLVMBuildRet(builder, result);
        } else if (ctx.ASSIGN() != null) {
            //System.out.println("assign");
            String name = ctx.lVal().IDENT().getText();
            Symbol symbol = currentScope.resolve(name);
            LLVMValueRef lVal = ((VariableSymbol) symbol).getRef();
            LLVMValueRef exp = visit(ctx.exp());
            return LLVMBuildStore(builder, exp, lVal);
        } else if (ctx.IF() != null) {
            LLVMValueRef cond = visit(ctx.cond());
            LLVMValueRef condRef = LLVMBuildICmp(builder, LLVMIntNE,  LLVMConstInt(i32Type, 0, 0), cond, "ifCond");
            LLVMBasicBlockRef then = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "if_true");
            LLVMBasicBlockRef elseBlock = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "if_false");
            LLVMBasicBlockRef after = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "next");
            LLVMBuildCondBr(builder, condRef, then, elseBlock);

            LLVMPositionBuilderAtEnd(builder, then);
            visit(ctx.stmt(0));
            LLVMBuildBr(builder, after);

            LLVMPositionBuilderAtEnd(builder, elseBlock);
            if (ctx.ELSE() != null) {
                visit(ctx.stmt(1));
            }
            LLVMBuildBr(builder, after);

            LLVMPositionBuilderAtEnd(builder, after);
            return null;
        } else if (ctx.WHILE() != null) {
            LLVMBasicBlockRef condition = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "whileCondition");
            LLVMBasicBlockRef body = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "whileBody");
            LLVMBasicBlockRef after = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "whileAfter");
            LLVMBuildBr(builder, condition);
            LLVMPositionBuilderAtEnd(builder, condition);
            LLVMValueRef cond = visit(ctx.cond());
            LLVMValueRef condRef = LLVMBuildICmp(builder, LLVMIntNE,  LLVMConstInt(i32Type, 0, 0), cond, "whileCond");
            LLVMBuildCondBr(builder, condRef, body, after);
            LLVMPositionBuilderAtEnd(builder, body);
            loopStack.push(condition);
            loopEndStack.push(after);
            visit(ctx.stmt(0));
            LLVMBuildBr(builder, condition);
            loopStack.pop();
            loopEndStack.pop();
            LLVMPositionBuilderAtEnd(builder, after);
            return null;
        } else if (ctx.BREAK() != null) {
            return LLVMBuildBr(builder, loopEndStack.peek());
        } else if (ctx.CONTINUE() != null) {
            return LLVMBuildBr(builder, loopStack.peek());
        }
        return  visitChildren(ctx);
    }

    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {
        //TODO
        if (ctx.getChildCount() == 1) {
            //System.out.println(ctx.getText());
            return visit(ctx.getChild(0));
        } else if (ctx.L_PAREN() != null) {
            if (ctx.IDENT() == null) {
                return visit(ctx.getChild(1));
            } else {
                // IDENT L_PAREN funcRParams? R_PAREN
                String name = ctx.IDENT().getText();
                Symbol symbol = currentScope.resolve(name);
                PointerPointer<Pointer> args = null;
                int size = ctx.funcRParams() == null ? 0 : ctx.funcRParams().param().size();
                if (ctx.funcRParams() != null) {
                    args = new PointerPointer<>(size);
                    for (int i = 0; i < size; ++i) {
                        SysYParser.ParamContext paramContext = ctx.funcRParams().param(i);
                        SysYParser.ExpContext expContext = paramContext.exp();
                        args.put(i, this.visit(expContext));
                    }
                }
                return LLVMBuildCall(builder, symbol.getRef(), args, size, ((FunctionSymbol)symbol).getReturnType().equals("void") ? "" : "funcCall");
            }
        } else if (ctx.unaryOp() != null) {
            LLVMValueRef exp = visit(ctx.getChild(1));
            if (ctx.unaryOp().PLUS() != null) {
                return exp;
            } else if (ctx.unaryOp().MINUS() != null) {
                return LLVMBuildNeg(builder, exp, "negtmp");
            } else if (ctx.unaryOp().NOT() != null) {
                // 最开始没注意到这里的要求,直接调用LLVMBuildNot,但是这个函数是对整数取反的,与要求不符
//                String xxix = null;
//                xxix.equals("okk");
                return LLVMConstIntGetZExtValue(exp) == 0 ? LLVMConstInt(i32Type, 1, 0) : LLVMConstInt(i32Type, 0, 0);
            }
        } else {
            LLVMValueRef left = visit(ctx.getChild(0));
            LLVMValueRef right = visit(ctx.getChild(2));
            if (ctx.PLUS() != null) {
                return LLVMBuildAdd(builder, left, right, "addtmp");
            } else if (ctx.MINUS() != null) {
                return LLVMBuildSub(builder, left, right, "subtmp");
            } else if (ctx.MUL() != null) {
                return LLVMBuildMul(builder, left, right, "multmp");
            } else if (ctx.DIV() != null) {
                return LLVMBuildSDiv(builder, left, right, "divtmp");
            } else if (ctx.MOD() != null) {
                return LLVMBuildSRem(builder, left, right, "modtmp");
            }
        }
        return null;
    }

    private Long myParseLong(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")){
            return Long.parseLong(s.substring(2), 16);
        }  else if (s.startsWith("0") && s.length() > 1) {
            return Long.parseLong(s.substring(1),8);
        } else {
            return Long.parseLong(s);
        }
    }

    @Override
    public LLVMValueRef visitNumber(SysYParser.NumberContext ctx) {
        //TODO Not sure if this is correct
        return LLVMConstInt(i32Type, myParseLong(ctx.INTEGER_CONST().getText()), 1);
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        //TODO
        //System.out.println("lVal");

        String name = ctx.IDENT().getText();
        Symbol symbol = currentScope.resolve(name);
        return LLVMBuildLoad(builder, ((VariableSymbol) symbol).getRef(), name);

    }

    @Override
    public LLVMValueRef visitCond(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            return LLVMBuildZExt(builder, visit(ctx.exp()), i32Type, "ext");
        }

        LLVMValueRef lVal = visit(ctx.cond(0));
        LLVMValueRef rVal = null;

        if (ctx.AND() == null && ctx.OR() == null ){
            rVal = visit(ctx.cond(1));
        }

        LLVMValueRef retCmpResult = null;
        if (ctx.LT() != null) {
            retCmpResult = LLVMBuildICmp(builder, LLVMIntSLT, lVal, rVal, "lttmp");
        } else if (ctx.GT() != null) {
            retCmpResult = LLVMBuildICmp(builder, LLVMIntSGT, lVal, rVal, "gttmp");
        } else if (ctx.LE() != null) {
            retCmpResult = LLVMBuildICmp(builder, LLVMIntSLE, lVal, rVal, "letmp");
        } else if (ctx.GE() != null) {
            retCmpResult = LLVMBuildICmp(builder, LLVMIntSGE, lVal, rVal, "getmp");
        } else if (ctx.EQ() != null) {
            retCmpResult = LLVMBuildICmp(builder, LLVMIntEQ, lVal, rVal, "eqtmp");
        } else if (ctx.NEQ() != null) {
            retCmpResult = LLVMBuildICmp(builder, LLVMIntNE, lVal, rVal, "neqtmp");
        } else if (ctx.AND() != null) {
            LLVMValueRef condRef = LLVMBuildICmp(builder, LLVMIntEQ,  LLVMConstInt(i32Type, 0, 0), lVal, "condRef");
            LLVMBasicBlockRef _true = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "and_true");
            LLVMBasicBlockRef _false = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "and_false");
            LLVMBasicBlockRef after = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "and_after");
            retCmpResult = LLVMBuildAlloca(builder, LLVMInt32Type(), "andtmp");

            LLVMBuildStore(builder, lVal, retCmpResult);
            LLVMBuildCondBr(builder, condRef, _true, _false);


            LLVMPositionBuilderAtEnd(builder, _true);
            LLVMBuildBr(builder, after);



            LLVMPositionBuilderAtEnd(builder, _false);
            rVal = this.visitCond(ctx.cond(1));
            LLVMBuildStore(builder, rVal, retCmpResult);
            LLVMBuildBr(builder, after);

            LLVMPositionBuilderAtEnd(builder, after);
            retCmpResult = LLVMBuildLoad(builder, retCmpResult, "andResult");

        } else if (ctx.OR() != null) {
            LLVMValueRef condRef = LLVMBuildICmp(builder, LLVMIntNE,  LLVMConstInt(i32Type, 0, 0), lVal, "condRef");
            LLVMBasicBlockRef _true = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "or_true");
            LLVMBasicBlockRef _false = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "or_false");
            LLVMBasicBlockRef after = LLVMAppendBasicBlock(((FunctionSymbol)currentFuncScope).getRef(), "or_after");
            retCmpResult = LLVMBuildAlloca(builder, LLVMInt32Type(), "ortmp");
            LLVMBuildStore(builder, lVal, retCmpResult);

            LLVMBuildCondBr(builder, condRef, _true, _false);

            LLVMPositionBuilderAtEnd(builder, _true);
            LLVMBuildBr(builder, after);


            LLVMPositionBuilderAtEnd(builder, _false);
            rVal = this.visitCond(ctx.cond(1));
            LLVMBuildStore(builder, rVal, retCmpResult);
            LLVMBuildBr(builder, after);

            LLVMPositionBuilderAtEnd(builder, after);
            retCmpResult = LLVMBuildLoad(builder, retCmpResult, "orResult");
        }

        return LLVMBuildZExt(builder, retCmpResult, i32Type, "ext");
    }

    private void optimizeModule() {
        LLVMRunPassManager(passManager, module);
    }
}
