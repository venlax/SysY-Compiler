public class ArrayType implements Type {
    Type contained; // type of its elements, may be int or array
     int num_elements;

    public ArrayType(int num_elements,Type contained) {
        this.contained = contained;
        this.num_elements = num_elements;
    }

    @Override
    public String toString() {
        return "array(" + contained + ")";
    }
}