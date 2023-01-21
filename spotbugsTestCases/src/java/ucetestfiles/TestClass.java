package ucetestfiles;

public class TestClass {
    private static Throwable t;

    private TestClass() throws Throwable {
        throw t;
    }

    static synchronized void undeclaredThrow(Throwable t){
        TestClass.t = t;
        try {
            TestClass.class.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } finally {
            TestClass.t = null;
        }
    }

}
