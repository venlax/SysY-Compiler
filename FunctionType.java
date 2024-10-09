import java.util.ArrayList;
import java.util.StringJoiner;

public class FunctionType implements Type {
    Type retType;
    ArrayList<Type> paramsType;

    FunctionType(Type retType, ArrayList<Type> paramsType) {
        this.retType = retType;
        this.paramsType = paramsType;
    }

    public Type getRetType() {
        return retType;
    }

    public ArrayList<Type> getParamsType() {
        return paramsType;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", retType + "(", ")");
        for (Type type : paramsType) {
            joiner.add(type.toString());
        }
        return joiner.toString();
    }


}