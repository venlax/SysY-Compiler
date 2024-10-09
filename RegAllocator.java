import org.antlr.v4.runtime.misc.Pair;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

import java.sql.SQLOutput;
import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class RegAllocator {
    // 为防止混乱,自己在这里先理一下思路, 对于局部变量,首先应该先在寄存器(t0-t6)上分配空间,其次再分配栈,由于本次实现不涉及跳转语句,所以在Interval中从前向后扫描即可
    // 在生成代码之前应该现在此处扫描一次IR,确定每个变量的生命周期,以及栈大小,在翻译load和alloca的时候实现寄存器的切换

    private static final String[] regs = {
            "t0", "t1", "t2", // for temp use
            "t3", "t4", "t5", "t6",
            "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
            "s8", "s9", "s10", "s11","gp", "tp", "ra",
            // not for allocation
            "zero", "sp",
    };
    static  int tempFreeRegPtr = 0;

    private Map<Integer, List<String>> whichVariableNeedSpill = new HashMap<>();
    //<line>, list<VarName>
    private Map<Integer, List<String>> whichVariableNeedReload = new HashMap<>();

    private Map<String, LiveInterval> liveIntervals = new HashMap<>();
    // <VarName, LiveInterval>

    private Map<Integer, List<String>> liveVariablesInLine = new HashMap<>();
    
    private Map<Pair<Integer, String>, Pair<Integer, String>> variableMap = new HashMap<>();
    // <line, VarName>, <offset, RegName>

    private Map<String, String> regMap = new HashMap<>();

    private Map<String, Integer> stackOffsetMap = new HashMap<>();
    private Stack<String> freeRegs = new Stack<>();
    
    private Set<String> variables = new HashSet<>();

    private List<String> variablesSpilledNow = new ArrayList<>();

    private int stackSizeNotAlign = 0;

    public RegAllocator(LLVMModuleRef module) {
        for (int i = 26; i >= 1; i--) {
            freeRegs.push(regs[i]);
        }
        calculateLiveInterval(module);
        allocateRegs();
    }
    

    private void calculateLiveInterval(LLVMModuleRef moduleRef) {
        for (LLVMValueRef value = LLVMGetFirstFunction(moduleRef); value != null; value = LLVMGetNextFunction(value)) {

            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(value); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                int count = 0;

                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                    count++;
                    int operandNum = LLVMGetNumOperands(inst);
                    int opt = LLVMGetInstructionOpcode(inst);
                    if (opt == LLVMAlloca) {
                        stackSizeNotAlign += 4;
                    }
                    List<String> liveVariablesInThisLine = new ArrayList<>();
                    if (operandNum == 2) {
                        LLVMValueRef operand1 = LLVMGetOperand(inst, 0);
                        LLVMValueRef operand2 = LLVMGetOperand(inst, 1);
                        String operand1Name = AsmCodeGenerator.MyGetValueName(operand1);
                        String operand2Name = AsmCodeGenerator.MyGetValueName(operand2);
                        if (LLVMIsAConstantInt(operand1) == null && !AsmCodeGenerator.globalVariableSet.contains(operand1Name)) {
                            liveVariablesInThisLine.add(operand1Name);
                            variables.add(operand1Name);
                        }
                        if (LLVMIsAConstantInt(operand2) == null && !AsmCodeGenerator.globalVariableSet.contains(operand2Name)) {
                            if (! liveVariablesInThisLine.contains(operand2Name)) liveVariablesInThisLine.add(operand2Name);
                            variables.add(operand2Name);
                        }
                    } else if (operandNum == 1) {
                        LLVMValueRef operand1 = LLVMGetOperand(inst, 0);
                        String operand1Name = AsmCodeGenerator.MyGetValueName(operand1);
                        if (LLVMIsAConstantInt(operand1) == null && !AsmCodeGenerator.globalVariableSet.contains(operand1Name)) {
                            liveVariablesInThisLine.add(operand1Name);
                            variables.add(operand1Name);
                        }
                    }
                    String lValName = AsmCodeGenerator.getInstLValueName(inst);
                    //System.out.println("Line " + count + " " + lValName);
                    if (lValName != null) {
                        if (!AsmCodeGenerator.globalVariableSet.contains(lValName)) {
                            if (!liveVariablesInThisLine.contains(lValName))liveVariablesInThisLine.add(lValName);
                            variables.add(lValName);
                        }
                    }
                    liveVariablesInLine.put(count, liveVariablesInThisLine);
//                    for (String var : liveVariablesInThisLine) {
//                        System.out.println("Line " + count + " " + var);
//
//                    }
                }
            }
        }
        for (String var : variables) {
            int start = -1;
            int end = -1;
            for (int i = 1; i <= liveVariablesInLine.size(); i++) {
                if (liveVariablesInLine.get(i).contains(var)) {
                    if (start == -1) {
                        start = i;
                    }
                    end = i;
                }
            }
            liveIntervals.put(var, new LiveInterval(start, end));
        }
        
    }

    private void allocateRegs() {
        // TODO: 这里还有问题,比如在某一行,有一个读变量在这里要释放了,那么它可以将它对应的寄存器分配给写变量(reg = reg + reg'),但注意不能将reg分配给reg'的读变量,会导致重复,记得改!!!

        for (int i = 1; i <= liveVariablesInLine.size(); ++i) {
            List<String> list = liveVariablesInLine.get(i);
            List<String> variablesNeedSpilledNow = new ArrayList<>();
            List<String> variablesNeedReloadNow = new ArrayList<>();
            List<String> regCanPush = new ArrayList<>();
            List<String> hasAllocated = new ArrayList<>();
            for (int j = 0; j < list.size(); ++j) {

                String var = list.get(j);

                if (j == list.size() - 1) {
                    for (String reg : regCanPush) {
                        freeRegs.push(reg);
                    }
                    regCanPush.clear();
                }
                LiveInterval interval = liveIntervals.get(var);
                if (interval.end <= i ) { // 存疑
                    // 归还寄存器
//                    if (var.equals("0")) {
//                        System.out.println("okk" + regMap.get(var) + i);
//                    }
                    if (regMap.containsKey(var)) {
//                        if (var.equals("0")) {
//                            System.out.println("okk" + regMap.get(var) + i);
//                        }
//                        if (var.equals("t")) {
//                            System.out.println("okk pp" + regMap.get(var) + i);
//                        }
                        variableMap.put(new Pair<>(i, var), new Pair<>(stackOffsetMap.containsKey(var) ? stackOffsetMap.get(var) : -1, regMap.get(var)));
                        if (j != list.size() - 1) {
                            regCanPush.add(regMap.get(var));
                            regMap.remove(var);
                        } else {
                            freeRegs.push(regMap.get(var));
                            regMap.remove(var);
                        }
                        continue; // 存疑
                    } else if (interval.end == i) {
                        // 如果不包含则reload
                        //variablesNeedReloadNow.add(var);
                        //variablesSpilledNow.remove(var);
                    }
                }

                if (variablesSpilledNow.contains(var)) {
                    variablesNeedReloadNow.add(var);
                    variablesSpilledNow.remove(var);
                }
                if (interval.start <= i && interval.end >= i) {
                    if (regMap.containsKey(var)) {
                        variableMap.put(new Pair<>(i, var), new Pair<>(stackOffsetMap.containsKey(var) ? stackOffsetMap.get(var) : -1, regMap.get(var)));
//                        if (hasAllocated.contains(regMap.get(var))) {
//                            System.out.println("error");
//                        } else {
//                            hasAllocated.add(regMap.get(var));
//                        }
                    } else {
                        // 这里为还未分配寄存器的变量分配
                        if(freeRegs.empty()) {
                            // Spill
                            //TODO
                            String varToSpill = getVarToSpill(i);
                            variablesNeedSpilledNow.add(varToSpill);
                            variablesSpilledNow.add(varToSpill);
                            if (stackOffsetMap.containsKey(varToSpill)) {
                                // do nothing
                                // if (stackOffsetMap.get(varToSpill) == -1) System.out.println("error");
                            } else {
                                //System.out.println("reach here");
                                stackOffsetMap.put(varToSpill, stackSizeNotAlign);
                                stackSizeNotAlign += 4;
                            }
                            freeRegs.push(regMap.get(varToSpill));
                            variableMap.put(new Pair<>(i, varToSpill), new Pair<>(stackOffsetMap.get(varToSpill), regMap.get(varToSpill)));
                            regMap.remove(varToSpill);
                        }
                        //System.out.println("here");
                        String reg = freeRegs.pop();
//                        if (hasAllocated.contains(reg)) {
//                            System.out.println("error");
//                        } else {
//                            hasAllocated.add(reg);
//
//                        }

//                        if (var.equals("0")) {
//                            System.out.println("okk alloc" + reg);
//                        }
                        variableMap.put(new Pair<>(i, var), new Pair<>(stackOffsetMap.containsKey(var) ? stackOffsetMap.get(var) : -1, reg));
                        //System.out.println("Allocate " + var + " to " + reg);
                        regMap.put(var, reg);

                    }
                }
           }
//            for (String var : variablesNeedSpilledNow) {
//                System.out.println("Spill " + var);
//            }
//            for (String var : variablesNeedReloadNow) {
//                System.out.println("Reload " + var);!
//            }
            whichVariableNeedSpill.put(i, variablesNeedSpilledNow);
            whichVariableNeedReload.put(i, variablesNeedReloadNow);
        }
    }

    private String getVarToSpill(int count) {
        final int[] maxEnd = {-1};
        final String[] varToSpill = {null};
        regMap.forEach((var, reg) -> {
            LiveInterval interval = liveIntervals.get(var);
            if (interval.end > maxEnd[0] && !liveVariablesInLine.get(count).contains(var)) {
                maxEnd[0] = interval.end;
                varToSpill[0] = var;
            }
        });
        return varToSpill[0];
    }

    public Map<Integer, List<String>> getWhichVariableNeedSpill() {
        return whichVariableNeedSpill;
    }
    public Map<Integer, List<String>> getWhichVariableNeedReload() {
        return whichVariableNeedReload;
    }

    public Map<Pair<Integer, String>, Pair<Integer, String>> getVariableMap() {
        return variableMap;
    }

    public String getATempReg() {
        return regs[0];
    }

    public long getStackSize() {
        return (stackSizeNotAlign + 15) / 16 * 16;
    }


    static class LiveInterval {


        private final int start;
        private final int end;

        public LiveInterval(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }
}
