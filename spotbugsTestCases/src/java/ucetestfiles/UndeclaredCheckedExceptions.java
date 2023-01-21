package ucetestfiles;

import java.io.IOException;

public class UndeclaredCheckedExceptions {
    void test1(){
        TestClass.undeclaredThrow(new Exception("checked exception"));
    }

    void test2(){ GenericClass.undeclaredThrow(); }

    void test3() {
        try {
            TestClass.undeclaredThrow(new IOException("checked exception"));
        } catch (Throwable e) {
            if (e instanceof IOException) {
                System.out.println("IOException occurred");
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                e.printStackTrace();
            }
        }
    }
}



