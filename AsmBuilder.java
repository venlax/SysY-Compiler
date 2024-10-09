import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bytedeco.llvm.global.LLVM.*;

public class AsmBuilder {

    private StringBuffer sb = new StringBuffer();


    private Map<String, Integer> regValueMap = new HashMap<>();

    public void add(String str) {
        sb.append(str).append("\n");
    }

    public void opR(String op, String rd, String rs1, String rs2) {
        sb.append(" ").append(op).append(" ").append(rd).append(" , ").append(rs1).append(" , ").append(rs2).append(" \n");
    }

    public void opI(String op, String rd, String rs1, long imm) {
        sb.append(" ").append(op).append(" ").append(rd).append(" , ").append(rs1).append(" , ").append(imm).append(" \n");
    }

    public void opS(String op, String rs1, String rs2, int imm) {
        sb.append(" ").append(op).append(" ").append(rs1).append(" , ").append(imm).append("(").append(rs2).append(") \n");
    }
    public String getAsmCode() {
        //optimiseCode();
        return sb.toString();
    }

    private void optimiseCode() {

        String[] lines = sb.toString().split("\n");
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] token = line.split(" ");
            List<String> tokens = Arrays.stream(token).filter(s -> s.length() != 0).collect(Collectors.toList());
            if (tokens.size() > 0 && tokens.get(0).equals("addi") && tokens.get(3).equals("sp")) {

                if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1];
                    String[] nextToken = nextLine.split(" ");
                    List<String> nextTokens =  Arrays.stream(nextToken).filter(s -> s.length() != 0).collect(Collectors.toList());
                    if (nextTokens.size() > 0 && nextTokens.get(0).equals("li")) {
                        //System.out.println("here!!");
                        regValueMap.put(tokens.get(1), Integer.parseInt(nextTokens.get(3)));
                        i += 2;
                        continue;
                    }else {
                        result.append(line).append("\n");
                    }
                }
            } else if (tokens.size() > 0 && tokens.get(0).equals("lw")) {
                if (!regValueMap.containsKey(tokens.get(3).substring(2,4))) {
                    result.append(line).append("\n");
                } else {
                    Integer value = regValueMap.get(tokens.get(3).substring(2,4));
                    result.append("li ").append(tokens.get(1)).append(" , ").append(value).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        sb = result;
    }

}
