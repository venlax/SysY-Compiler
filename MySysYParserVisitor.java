
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class MySysYParserVisitor extends SysYParserBaseVisitor<Void> {

//    private static final List<String> errorInfo = Arrays.asList(null, "Undefine variable",
//            "Undefined function", "Redefined variable",
//            "Redefined function", "type.Type mismatched for assignment.",
//            "type.Type mismatched for operands.", "type.Type mismatched for return.",
//            "Function is not applicable for arguments.", "Not an array",
//            "Not a function", "The left-hand side of an assignment must be a variable.");
//
//
//    private GlobalScope globalScope = null;
//    private Scope currentScope = null;
//
//    private Scope currentFuncScope = null;
//    private int countScope = 0;
//    public boolean error = false;
//
//
//    private String errorMessage(int errorno, int line ){
//        error = true;
//        return "Error type " + errorno + " at Line " + line + ": " + errorInfo.get(errorno);
//    }
//
//
//    private Type getType(SysYParser.LValContext ctx) {
//        Symbol symbol = currentScope.resolve(ctx.IDENT().getText());
//        if (symbol == null) {
//            return new TypeSymbol("vType");
//        }
//        Type varType = symbol.getType();
//        for (SysYParser.ExpContext ignored : ctx.exp()) {
//            if (varType instanceof ArrayType) {
//                varType = ((ArrayType) varType).contained;
//            } else {
//                return new TypeSymbol("vType");
//            }
//        }
//        return varType;
//    }
//    private Type getType(SysYParser.ExpContext ctx) {
//        if (ctx.IDENT() != null) {
//            String funcName = ctx.IDENT().getText();
//            Symbol symbol = currentScope.resolve(funcName);
//            if (symbol != null && symbol.getType() instanceof FunctionType) {
//                FunctionType functionType = (FunctionType) currentScope.resolve(funcName).getType();
//                ArrayList<Type> paramsType = functionType.getParamsType(), argsType = new ArrayList<>();
//                if (ctx.funcRParams() != null) {
//                    ctx.funcRParams().param().stream()
//                            .map(paramContext -> getType(paramContext.exp()))
//                            .forEach(argsType::add);
//
//                }
//                if (paramsType.equals(argsType)) {
//                    return functionType.getRetType();
//                }
//            }
//        } else if (ctx.L_PAREN() != null || ctx.unaryOp() != null) {
//            return getType(ctx.exp(0));
//        } else if (ctx.lVal() != null) {
//            return getType(ctx.lVal());
//        } else if (ctx.number() != null) {
//            return new TypeSymbol("int");
//        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
//            Type op1Type = getType(ctx.exp(0));
//            Type op2Type = getType(ctx.exp(1));
//            if (op1Type.toString().equals("int") && op2Type.toString().equals("int")) {
//                return op1Type;
//            }
//        }
//        return new TypeSymbol("vType");
//    }
//
//    private Type getType(SysYParser.CondContext ctx) {
//        if (ctx.exp() != null) {
//            return getType(ctx.exp());
//        }
////        System.out.println(getType(ctx.cond(0)).toString());
////        System.out.println(getType(ctx.cond(1)).toString());
//
//        if (getType(ctx.cond(0)).toString().equals("int") && getType(ctx.cond(1)).toString().equals("int")) {
//            return getType(ctx.cond(0));
//        }
//        return new TypeSymbol("vType");
//    }
//
//    private boolean checkFuncArgsTypes(ArrayList<Type> paramsType, ArrayList<Type> argsType) {
////        for (Type type: paramsType) System.out.println(type.toString());
////        for (Type type: argsType) System.out.println(type.toString());
//
//        if (paramsType.stream().anyMatch(type -> type.toString().equals("vType")) ||
//                argsType.stream().anyMatch(type -> type.toString().equals("vType"))) {
//            return true;
//        }
//
//        if (paramsType.size() != argsType.size()) {
//            return false;
//        }
//
//        return IntStream.range(0, paramsType.size())
//                .allMatch(i -> paramsType.get(i).toString().equals(argsType.get(i).toString()));
//    }
//
//
//    @Override
//    public Void visitProgram(SysYParser.ProgramContext ctx) {
//        globalScope = new GlobalScope(null);
//        currentScope = globalScope;
//        return visitChildren(ctx);
//    }
//    @Override
//    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
//
//        Type retType = (Type) globalScope.resolve(ctx.funcType().getText());
//
//        String funcName = ctx.IDENT().getText();
//
//
//        if (currentScope.definedSymbol(funcName)) {
//            System.err.println(errorMessage(4, ctx.getStart().getLine()) + ": " + funcName + ".");
//            return null;
//        }
//
//        FunctionSymbol fun = new FunctionSymbol(funcName, currentScope, new FunctionType(retType, new ArrayList<>()));
//
//        currentScope.define(fun);
//
//        currentScope = fun;
//
//       // fun.define(fun);
//
//        currentFuncScope = fun;
//
//        //System.out.println(currentScope);
//        Void ret = visitChildren(ctx);
//
//        currentScope = currentScope.getEnclosingScope();
//
//        currentFuncScope = null;
//
//        return ret;
//    }
//
//    @Override
//    public Void visitBlock(SysYParser.BlockContext ctx) {
//        LocalScope localScope = new LocalScope(currentScope);
//        localScope.setName(localScope.getName() + (countScope++));
//        currentScope = localScope;
//        Void ret = visitChildren(ctx);
//        currentScope = currentScope.getEnclosingScope();
//        return ret;
//    }
//
//
//    @Override
//    public Void visitVarDef(SysYParser.VarDefContext ctx) {
//        String typeName = "int";
//
//        Type varType = (Type) globalScope.resolve(typeName);
//        String varName = ctx.IDENT().getText();
//        if (currentScope.definedSymbol(varName) || (currentFuncScope != null && currentFuncScope.definedSymbol(varName))) {
//            System.err.println(errorMessage(3, ctx.getStart().getLine()) + ": " + varName + ".");
//            return visitChildren(ctx);
//        }
//
//        for (SysYParser.ConstExpContext constExpContext : ctx.constExp()) {
//            varType = new ArrayType(Integer.parseInt(String.valueOf(Integer.decode(constExpContext.getText()))), varType);
//        }
//
//
//        if (ctx.ASSIGN() != null) {
//            SysYParser.ExpContext expContext = ctx.initVal().exp();
//            if (expContext != null) {
//                Type initValType = getType(expContext);
//                if (!initValType.toString().equals("vType") && !varType.toString().equals(initValType.toString())) {
//                    System.err.println(errorMessage(5, ctx.getStart().getLine()));
//                }
//            }
//        }
//        currentScope.define(new VariableSymbol(varName, varType));
//        return visitChildren(ctx);
//    }
//
//    @Override
//    public Void visitConstDef(SysYParser.ConstDefContext ctx) {
//        String typeName = "int";
//
//
//        Type constType = (Type) globalScope.resolve(typeName);
//        String constName = ctx.IDENT().getText();
//        if (currentScope.definedSymbol(constName) || (currentFuncScope != null && currentFuncScope.definedSymbol(constName))) {
//            System.err.println(errorMessage(3, ctx.getStart().getLine()) + ": " + constName + ".");
//            return visitChildren(ctx);
//        }
//
//        for (SysYParser.ConstExpContext constExpContext : ctx.constExp()) {
//            int elementCount = Integer.parseInt(String.valueOf(Integer.decode(constExpContext.getText())));
//            constType = new ArrayType(elementCount, constType);
//        }
//
//        SysYParser.ConstExpContext expContext = ctx.constInitVal().constExp();
//        if (expContext != null) {
//            Type initValType = getType(expContext.exp());
//            if (!initValType.toString().equals("vType") && !constType.toString().equals(initValType.toString())) {
//                System.err.println(errorMessage(5, ctx.getStart().getLine()));
//            }
//        }
//
//        VariableSymbol constSymbol = new VariableSymbol(constName, constType);
//        currentScope.define(constSymbol);
//
//
//        return visitChildren(ctx);
//    }
//
//    @Override
//    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
//        String varTypeName = ctx.INT().getText();
//        Type varType = (Type) globalScope.resolve(varTypeName);
//        for (TerminalNode ignored : ctx.L_BRACKT()) {
//            varType = new ArrayType(0, varType);
//        }
//        String varName = ctx.IDENT().getText();
//        VariableSymbol varSymbol = new VariableSymbol(varName, varType);
//        //System.out.println(currentScope);
//        if (currentScope.definedSymbol(varName)) {
//            System.err.println(errorMessage(3, ctx.getStart().getLine()) +   ": " + varName + ".");
//        } else {
//            currentScope.define(varSymbol);
//            ((FunctionSymbol) currentScope).getType().getParamsType().add(varType);
//        }
//        return visitChildren(ctx);
//    }
//
//    @Override
//    public Void visitLVal(SysYParser.LValContext ctx) {
//        String varName = ctx.IDENT().getText();
//        Symbol symbol = currentScope.resolve(varName);
//        if (symbol == null) {
//            System.err.println(errorMessage(1, ctx.getStart().getLine()) +   ": " + varName + ".");
//            return null;
//        }
//
//        Type varType = symbol.getType();
//        int arrayDimension = ctx.exp().size();
//        for (int i = 0; i < arrayDimension; ++i) {
//            if (varType instanceof ArrayType) {
//                varType = ((ArrayType) varType).contained;
//                SysYParser.ExpContext expContext = ctx.exp(i);
//                varName += "[" + expContext.getText() + "]";
//            } else {
//                System.err.println(errorMessage(9, ctx.getStart().getLine()) +   ": " + varName + ".");
//                break;
//            }
//        }
//
//        return visitChildren(ctx);
//    }
//
//    @Override
//    public Void visitStmt(SysYParser.StmtContext ctx) {
//        if (ctx.ASSIGN() != null) {
//            Type lValType = getType(ctx.lVal());
//            Type rValType = getType(ctx.exp());
//            if (lValType instanceof FunctionType) {
//                System.err.println(errorMessage(11, ctx.getStart().getLine()));
//            } else if (!lValType.toString().equals("vType") && !rValType.toString().equals("vType") && !lValType.toString().equals(rValType.toString())) {
//                System.err.println(errorMessage(5, ctx.getStart().getLine()));
//            }
//        } else if (ctx.RETURN() != null) {
//            Type retType = new TypeSymbol("void");
//            if (ctx.exp() != null) {
//                retType = getType(ctx.exp());
//            }
//
//            Scope tmpScope = currentScope;
//            while (!(tmpScope instanceof FunctionSymbol)) {
//                tmpScope = tmpScope.getEnclosingScope();
//            }
//
//            Type expectedType = ((FunctionSymbol) tmpScope).getType().getRetType();
//            if (!retType.toString().equals("vType") && !expectedType.toString().equals("vType") && !retType.toString().equals(expectedType.toString())) {
//                System.err.println(errorMessage(7, ctx.getStart().getLine()));
//            }
//        }
//        return visitChildren(ctx);
//    }
//
//    @Override
//    public Void visitCond(SysYParser.CondContext ctx) {
//        if (ctx.exp() == null && !getType(ctx).toString().equals("int")) {
//            System.err.println(errorMessage(6, ctx.getStart().getLine()));
//        }
//        return visitChildren(ctx);
//    }
//
//    @Override
//    public Void visitExp(SysYParser.ExpContext ctx) {
//        if (ctx.IDENT() != null) {
//            String funcName = ctx.IDENT().getText();
//            Symbol symbol = currentScope.resolve(funcName);
//            if (symbol == null) {
//                System.err.println(errorMessage(2, ctx.getStart().getLine()) +   ": " + funcName + ".");
//            } else if (!(symbol.getType() instanceof FunctionType)) {
//                System.err.println(errorMessage(10, ctx.getStart().getLine()) +   ": " + funcName + ".");
//            } else {
//                FunctionType functionType = (FunctionType) symbol.getType();
//                ArrayList<Type> paramsType = functionType.getParamsType();
//                ArrayList<Type> argsType = new ArrayList<>();
//                if (ctx.funcRParams() != null) {
//                    for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
//                        argsType.add(getType(paramContext.exp()));
//                    }
//                }
//                if (!checkFuncArgsTypes(paramsType, argsType)) {
//                    System.err.println(errorMessage(8, ctx.getStart().getLine()));
//                }
//            }
//        } else if (ctx.unaryOp() != null) {
//            Type expType = getType(ctx.exp(0));
//            if (!expType.toString().equals("int")) {
//                System.err.println(errorMessage(6, ctx.getStart().getLine()));
//            }
//        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
//            Type op1Type = getType(ctx.exp(0)), op2Type = getType(ctx.exp(1));
//            if (op1Type.toString().equals("vType") || op2Type.toString().equals("vType")) {
//            } else if (op1Type.toString().equals("int") && op2Type.toString().equals("int")) {
//            } else {
//                System.err.println(errorMessage(6, ctx.getStart().getLine()) );
//            }
//        }
//        return visitChildren(ctx);
//    }




}
