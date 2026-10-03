package struct;

import java.lang.reflect.Field;
import java.util.HashMap;

/* loaded from: classes.dex */
public class Constants {
    private static HashMap<String, Primitive> primitiveTypes = new HashMap<>();
    private static HashMap<Character, Primitive> signatures = new HashMap<>();

    static {
        for (Primitive p : Primitive.valuesCustom()) {
            primitiveTypes.put(p.type, p);
            signatures.put(Character.valueOf(p.signature), p);
        }
    }

    public enum Primitive {
        BOOLEAN("boolean", 'Z', 0),
        BYTE("byte", 'B', 1),
        CHAR("char", 'C', 2),
        SHORT("short", 'S', 3),
        INT("int", 'I', 4),
        LONG("long", 'J', 5),
        FLOAT("float", 'F', 6),
        DOUBLE("double", 'D', 7),
        OBJECT("object", 'O', 8);

        int order;
        char signature;
        String type;

        /* renamed from: values, reason: to resolve conflict with enum method */
        public static Primitive[] valuesCustom() {
            Primitive[] valuesCustom = values();
            int length = valuesCustom.length;
            Primitive[] primitiveArr = new Primitive[length];
            System.arraycopy(valuesCustom, 0, primitiveArr, 0, length);
            return primitiveArr;
        }

        Primitive(String type, char signature, int order) {
            this.type = type;
            this.signature = signature;
            this.order = order;
        }
    }

    public static final Primitive getPrimitive(Field field) {
        return !field.getType().isArray() ? getPrimitive(field.getType().getName()) : getPrimitive(field.getType().getName().charAt(1));
    }

    public static final Primitive getPrimitive(String name) {
        Primitive p = primitiveTypes.get(name);
        return p != null ? p : Primitive.OBJECT;
    }

    public static final Primitive getPrimitive(char signature) {
        Primitive p = signatures.get(Character.valueOf(signature));
        return p != null ? p : Primitive.OBJECT;
    }
}
