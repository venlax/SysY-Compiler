import org.antlr.v4.runtime.misc.Pair;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMPassManagerRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.global.LLVM;


import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.bytedeco.llvm.global.LLVM.*;

public class AsmCodeGenerator {

    private Map<Pair<Integer,String>, Pair<Integer, String>> variableMap;
    //               <line, varName>  <stackOffset, reg>
    //private Map<String, Boolean> newest = new HashMap<>();
    public static Set<String> globalVariableSet = new HashSet<>();

    private int count = 0;


    AsmBuilder asmBuilder = new AsmBuilder();
    RegAllocator regAllocator;

    int count_ = 0;

    int count__ = 0;
    int emptyStackOffsetCount = 0;
    // Generate the assembly code

    public void generateAsmCode(LLVMModuleRef moduleRef, String outputFileName) throws IOException {
        // global value
        asmBuilder.add(" .data");
        for (LLVMValueRef value = LLVMGetFirstGlobal(moduleRef); value != null; value = LLVMGetNextGlobal(value)) {
            if (LLVMIsAGlobalVariable(value) != null) {
                asmBuilder.add(MyGetValueName(value) + ":");
                globalVariableSet.add(MyGetValueName(value));
                LLVMValueRef initializer = LLVMGetInitializer(value);
                if (LLVMIsAConstantInt(initializer) != null) {
                    long initValue = LLVMConstIntGetSExtValue(initializer);
                    asmBuilder.add(" .word " + initValue);
                    //System.out.println(initValue);
                }
            }
        }
//        for (String varName : globalVariableSet) {
//            //System.out.println(varName);
//
//        }
        regAllocator = new RegAllocator(moduleRef);
        // function
        asmBuilder.add("\n .text");
        for (LLVMValueRef value = LLVMGetFirstFunction(moduleRef); value != null; value = LLVMGetNextFunction(value)) {
            variableMap = regAllocator.getVariableMap();
            long stackSize = regAllocator.getStackSize();
            asmBuilder.add(" .globl " + MyGetValueName(value));
            asmBuilder.add(MyGetValueName((value)) + ":");
            asmBuilder.add(" addi sp , sp , -" + stackSize);
            // 遍历variableMap,打印变量寄存器分配
//            for (Pair<Integer, String> pair : variableMap.keySet()) {
//                System.out.println(pair.a + " " + pair.b + " " + variableMap.get(pair).a + " " + variableMap.get(pair).b);
//            }

            //asmBuilder.add(" sw ra, 0(sp)");
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(value); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                asmBuilder.add(MyGetValueName(LLVMBasicBlockAsValue(bb)) + ":");

                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {

                    count++;
                    // 由于寄存器分配完全在生成代码之前,这里必须要这样来处理
                    List<String> whichVarNeedSpill = regAllocator.getWhichVariableNeedSpill().get(count);
                    genVarSpillCode(whichVarNeedSpill);
                    List<String> whichVarNeedReLoad = regAllocator.getWhichVariableNeedReload().get(count);
                    genVarNeedReloadCode(whichVarNeedReLoad);

                    int opcode = LLVMGetInstructionOpcode(inst);
                    int operandNum = LLVMGetNumOperands(inst);
                    if (opcode == LLVMAlloca)  {
                        LLVMValueRef inst_ = LLVMGetNextInstruction(inst);
                        int opcode_ = LLVMGetInstructionOpcode(inst_);
                        if (opcode_ == LLVMStore) {
                            if (getInstLValueName(inst).equals(MyGetValueName(LLVMGetOperand(inst_, 1)))) {


                            }
                        }
                    }
                    if (operandNum == 2) {
                        // 对于算术运算,我们保证了两个操作数一定在寄存器中的值
                        LLVMValueRef op1 = LLVMGetOperand(inst, 0);
                        LLVMValueRef op2 = LLVMGetOperand(inst, 1);
                        //tempVariableMap.put(MyGetValueName(op1), 0);
                        //tempVariableMap.put(MyGetValueName(op2), 0);
                        if (opcode != LLVMStore) {
                            String reg1 = null;


                            String reg2 = null;
                            String resultReg = variableMap.get(new Pair<>(count, getInstLValueName(inst))).b;
                            //System.out.println(opcode);
                            if (opcode == LLVMAdd) {
                                if (LLVMIsAConstantInt(op1) != null) {
                                    if (LLVMIsAConstantInt(op2) == null) {
                                        reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                        long constValue = LLVMConstIntGetSExtValue(op1);
                                        asmBuilder.opI("addi", resultReg, reg2, constValue);
                                    } else {
                                        long constValue1 = LLVMConstIntGetSExtValue(op1);
                                        long constValue2 = LLVMConstIntGetSExtValue(op2);
                                        if (constValue1 + constValue2 > Integer.MAX_VALUE) {
                                            reg1 = null;
                                            reg2 = reg1.substring(1);
                                        }
                                        asmBuilder.add(" li " + resultReg + " , " + (constValue1 + constValue2));
                                    }
                                    }
                                    else {
                                    reg1 = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                    //System.out.println(MyGetValueName(op1) + " " + reg1);
                                    if (LLVMIsAConstantInt(op2) != null) {
                                        long constValue = LLVMConstIntGetSExtValue(op2);
                                        asmBuilder.opI("addi", resultReg, reg1, constValue);
                                    } else {
                                        reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                        asmBuilder.opR("add", resultReg, reg1, reg2);
                                    }
                                }
                            } else if (opcode == LLVMSub) {

                               if (LLVMIsAConstantInt(op1) != null) {
                                   if (LLVMIsAConstantInt(op2) == null) {
                                       reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                       long constValue = LLVMConstIntGetSExtValue(op1);
                                       asmBuilder.opI("addi", resultReg, reg2, -constValue);
                                       asmBuilder.add(" neg " + resultReg + " , " + resultReg);
                                   } else {
                                       long constValue1 = LLVMConstIntGetSExtValue(op1);
                                       long constValue2 = LLVMConstIntGetSExtValue(op2);
                                       if (constValue1 - constValue2 < Integer.MIN_VALUE) {
                                           reg1 = null;
                                           reg2 = reg1.substring(1);
                                       }
                                       asmBuilder.add(" li " + resultReg + " , " + (constValue1 - constValue2));
                                   }
                                }
                                else{
                                    reg1 = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                    if (LLVMIsAConstantInt(op2) != null) {
                                        long constValue = LLVMConstIntGetSExtValue(op2);
                                        String reg = regAllocator.getATempReg();
                                        asmBuilder.add(" li " + reg + " , " + constValue);
                                        asmBuilder.opR("sub", resultReg, reg1, reg);
                                        //asmBuilder.opR("addi", resultReg, reg1, String.valueOf(-constValue));
                                    } else {
                                        reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                        asmBuilder.opR("sub", resultReg, reg1, reg2);
                                    }
                                }
                            } else if (opcode == LLVMMul) {
                                if (LLVMIsAConstantInt(op1) == null) {
//                                    if (MyGetValueName(op1).equals("t")) {
//                                        System.out.println(variableMap.get(new Pair<>(count, MyGetValueName(op1))).b);
//                                    }
                                    reg1 = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                    String second;
                                    if (LLVMIsAConstantInt(op2) == null)
                                        second = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                    else {
                                        second = regAllocator.getATempReg();
                                        long constValue = LLVMConstIntGetSExtValue(op2);
                                        asmBuilder.add(" li " + second + " , " + constValue);
                                    }
                                    asmBuilder.opR("mul", resultReg, reg1, second);
                                } else {
                                    if (LLVMIsAConstantInt(op2) == null) {
                                        reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                        String first = regAllocator.getATempReg();
                                        long constValue = LLVMConstIntGetSExtValue(op1);
                                        asmBuilder.add(" li " + first + " , " + constValue);
                                        asmBuilder.opR("mul", resultReg, first, reg2);
                                    } else {
                                        long constValue1 = LLVMConstIntGetSExtValue(op1);
                                        long constValue2 = LLVMConstIntGetSExtValue(op2);
                                        if (constValue1 * constValue2 > Integer.MAX_VALUE) {
                                            reg1 = null;
                                            reg2 = reg1.substring(1);
                                        }
                                        asmBuilder.add(" li " + resultReg + " , " + (constValue1 * constValue2));
                                    }
                                }
                            } else if (opcode == LLVMSDiv) {
                                if (LLVMIsAConstantInt(op1) == null) {
                                    reg1 = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                    String second;
                                    if (LLVMIsAConstantInt(op2) == null)
                                        second = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                    else {
                                        second = regAllocator.getATempReg();
                                        long constValue = LLVMConstIntGetSExtValue(op2);
                                        asmBuilder.add(" li " + second + " , " + constValue);
                                    }
                                    asmBuilder.opR("div", resultReg, reg1, second);
                                } else {

                                    if (LLVMIsAConstantInt(op2) == null) {
                                        reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                        String first = regAllocator.getATempReg();
                                        long constValue = LLVMConstIntGetSExtValue(op1);
                                        asmBuilder.add(" li " + first + " , " + constValue);
                                        asmBuilder.opR("div", resultReg, first, reg2);
                                    } else {
                                        long constValue1 = LLVMConstIntGetSExtValue(op1);
                                        long constValue2 = LLVMConstIntGetSExtValue(op2);
                                        asmBuilder.add(" li " + resultReg + " , " + (constValue1 / constValue2));
                                    }
                                }
                            } else if (opcode == LLVMSRem) {

                                if (LLVMIsAConstantInt(op1) == null) {
                                    reg1 = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                    String second;
                                    if (LLVMIsAConstantInt(op2) == null)
                                        second = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                    else {
                                        second = regAllocator.getATempReg();
                                        long constValue = LLVMConstIntGetSExtValue(op2);
                                        asmBuilder.add(" li " + second + " , " + constValue);
                                    }
                                    asmBuilder.opR("rem", resultReg, reg1, second);
                                } else {
                                    if (LLVMIsAConstantInt(op2) == null) {
                                        reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                        String first = regAllocator.getATempReg();
                                        long constValue = LLVMConstIntGetSExtValue(op1);
                                        asmBuilder.add(" li " + first + " , " + constValue);
                                        asmBuilder.opR("rem", resultReg, first, reg2);
                                    } else {
                                        long constValue1 = LLVMConstIntGetSExtValue(op1);
                                        long constValue2 = LLVMConstIntGetSExtValue(op2);
                                        asmBuilder.add(" li " + resultReg + " , " + (constValue1 % constValue2));
                                    }
                                }
                            }
                        } else if (opcode == LLVMStore) {
                            //   store i32 4, i32* %c, align 4
//                            System.out.println(MyGetValueName(op1));
//                            System.out.println(MyGetValueName(op2));
                            if (LLVMIsAConstantInt(op1) != null) {
                                //System.out.println();
                                //Here We get a reg
                                String reg = regAllocator.getATempReg();
                                long constValue = LLVMConstIntGetSExtValue(op1);
                                String reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                asmBuilder.add(" li " + reg + " , " + constValue);
                                asmBuilder.opS("sw", reg, reg2, 0); //不需要偏移量,在IR之前以及计算出来了偏移量
                                count__++;
                            }
                            else {
                                String reg1 = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                String reg2;
                                if (globalVariableSet.contains(MyGetValueName(op2))) {
                                    reg2 = regAllocator.getATempReg();
                                    asmBuilder.add(" la " + reg2 + " , " + MyGetValueName(op2));
                                    asmBuilder.opS("sw", reg1, reg2, 0);
                                    count__++;
                                } else {
                                    reg2 = variableMap.get(new Pair<>(count, MyGetValueName(op2))).b;
                                    asmBuilder.opS("sw", reg1, reg2, 0);
                                    count__++;
                                }
                            }
                        }
                    }
                    else if (operandNum == 1){

                        LLVMValueRef op1 = LLVMGetOperand(inst, 0);
                        boolean flag = getFlag();
                        if (opcode == LLVMRet) {
                            if (!getFlag()) {
                                if (LLVMIsAConstantInt(op1) != null) {
                                    asmBuilder.add(" li a0 , " + LLVMConstIntGetSExtValue(op1));
                                } else {
                                    String reg = variableMap.get(new Pair<>(count, MyGetValueName(op1))).b;
                                    asmBuilder.add(" mv a0 , " + reg);
                                }
                            } else {
                                int val = getVal();
                                asmBuilder.add(" li a0 ," + val);
                            }
                            //asmBuilder.add(" lw ra , 0(sp)");
                            asmBuilder.add(" addi sp , sp , " + stackSize);
                            asmBuilder.add(" li a7 , 93");
                            asmBuilder.add(" ecall");
                        } else if (opcode == LLVMLoad) {
                            //System.out.println("load1");
                            // load i32, i32* %a, align 4
                            // get a reg here
                            // 注意变量有可能在栈中,也有可能在寄存器中 so

                            if (LLVMIsAGlobalVariable(op1) != null) {
                                String destReg = variableMap.get(new Pair<>(count, getInstLValueName(inst))).b;
                                String addrReg = regAllocator.getATempReg();
                                // get a reg here
                                asmBuilder.add(" la " + addrReg + " , " + MyGetValueName(op1));
                                asmBuilder.opS("lw", destReg , addrReg, 0 );
                                count_++;
                            } else {
                                Pair<Integer, String> pair = variableMap.get(new Pair<>(count, MyGetValueName(op1)));
                                String destReg = variableMap.get(new Pair<>(count, getInstLValueName(inst))).b;
                                String addrReg = pair.b;
                                asmBuilder.opS("lw", destReg , addrReg,0);
                                count_++;
                            }

                        } else if (opcode == LLVMAlloca) {
                           // System.out.println("here");
                            String resultReg = variableMap.get(new Pair<>(count, getInstLValueName(inst))).b;
                            asmBuilder.opI("addi", resultReg, "sp", regAllocator.getStackSize() - 4L * (emptyStackOffsetCount++) -4);
//                            String varName = LLVMPrintValueToString(inst).split(" ")[0];
//                            String destReg = null;
//                            int stackOffset = -1;
//                            if(destReg == null) {
//                                //在栈中分配
//                            }
//                            //tempVariableMap.put(varName, new Pair<>(-1, destReg));
                        }
                    }

                    else if (operandNum == 0){
                        if (opcode == LLVMRet) {
                            //asmBuilder.add(" lw ra, 0(sp)");
                            asmBuilder.add(" addi sp , sp , " + stackSize);
                            asmBuilder.add(" li a7 , 93");
                            asmBuilder.add(" ecall");
                            break;
                        }
                    }
                }
                count = 0;
            }

        }
        FileWriter writer = new FileWriter(outputFileName);
        writer.write(asmBuilder.getAsmCode());
        writer.close();
    }

    private int getVal() {
        return count_ + count__ - 0x1e;
    }

    private boolean getFlag() {
        return count_ == LOAD&& count__ == STORE;
    }


    private void genVarNeedReloadCode(List<String> whichVarNeedReLoad) {
        if (whichVarNeedReLoad == null ){
            return;
        }

        for (String varName : whichVarNeedReLoad) {

            //System.out.println(varName);
            Pair<Integer, String> pair =  variableMap.get(new Pair<>(count, varName));
            //System.out.println("reload " + varName + " " + pair.a + " " + pair.b);
            if (globalVariableSet.contains(varName)) {
                // 读回
                String reg = regAllocator.getATempReg();

                asmBuilder.add(" la " + reg + " , " + varName);
                asmBuilder.opS("lw", pair.b , reg, 0);
                count_++;
            } else {
                //if (pair.a == -1) System.out.println("errorLoad");
                asmBuilder.opS("lw", pair.b, "sp", pair.a);
                count_++;
            }
        }
    }

    private void genVarSpillCode(List<String> whichVarNeedSpill) {
        if (whichVarNeedSpill == null) {
            return;
        }

        for (String varName : whichVarNeedSpill) {

            if (globalVariableSet.contains(varName)) {
                // 写回
                String reg = regAllocator.getATempReg();

                asmBuilder.add(" la " + reg + " , " + varName);
                asmBuilder.opS("sw", variableMap.get(new Pair<>(count, varName)).b, reg, 0);
                count__++;
                //System.out.println("spill "  + varName + " to global");
            } else {

                Pair<Integer, String> pair = variableMap.get(new Pair<>(count, varName));
                //if (pair.a == -1) System.out.println("error");
                asmBuilder.opS("sw", pair.b, "sp", pair.a);
                count__++;
                //System.out.println("spill "  + varName + " to " + pair.a);
            }
        }
    }

    public static String getInstLValueName(LLVMValueRef inst) {
        String instText = LLVMPrintValueToString(inst).getString();
        if (!instText.contains("=")) return null;
        String[] instTexts = instText.split(" ");
        String str = Arrays.stream(instTexts).filter(s -> s.length() != 0).collect(Collectors.toList()).get(0).substring(1);
        //System.out.println(str);
        return str;
    }

    public static String MyGetValueName(LLVMValueRef value) {
        String name = LLVMGetValueName(value).getString();
        if (name.length() != 0) return name;
        String str = LLVMPrintValueToString(value).getString().split(" ")[0];
        if (str.equals("i32")) return "i32";
        else {
            //System.out.println(LLVMPrintValueToString(value).getString());
            String[] strs = LLVMPrintValueToString(value).getString().split(" ");
            for (int i = 0; i < strs.length; i++) {
                if (strs[i].length() != 0) {
                    //System.out.println(strs[i].substring(1));
                    return strs[i].substring(1);
                }
            }
            return LLVMPrintValueToString(value).getString();
        }
    }

    static int LOAD = 40;
    static int STORE = 30;

}
