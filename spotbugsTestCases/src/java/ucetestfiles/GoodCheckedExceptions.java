package ucetestfiles;

public class GoodCheckedExceptions {
    public void goodTest1(){
        String str = null;
        try {
            str.toString();
        } catch(NullPointerException e){ }
    }

    public void goodTest2(){
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) { }
    }
}
