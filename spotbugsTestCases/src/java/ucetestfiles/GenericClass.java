package ucetestfiles;

import java.io.IOException;

public class GenericClass {
    static void undeclaredThrow() throws RuntimeException {
        //@SuppressWarnings("unchecked")
        Thr<RuntimeException> thr = (Thr<RuntimeException>)(Thr)
                new Thr<IOException>(){
                    public void fn() throws IOException {
                        throw new IOException();
                    }
                };
        thr.fn();
    }
}
