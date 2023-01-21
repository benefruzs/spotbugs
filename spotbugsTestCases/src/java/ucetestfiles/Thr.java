package ucetestfiles;

interface Thr<EXC extends Exception>{
    void fn() throws EXC;
}