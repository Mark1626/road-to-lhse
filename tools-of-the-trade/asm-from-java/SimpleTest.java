public class SimpleTest {
    public static void main(String[] args) {
        var x = new SimpleTest();
        int result = x.myadd(5, 10);
        System.out.println("Result: " + result);
    }

    public int myadd(int a, int b) {
        return a + b;
    }
}
