package io.unlogged.core.processor;

public class Util {

    /**
     * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it onwards.
     * The exception is still thrown - javac will just stop whining about it.
     * <p>
     * Example usage:
     * <pre>public void run() {
     *     throw sneakyThrow(new IOException("You don't need to catch me!"));
     * }</pre>
     * <p>
     * NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does not know or care
     * about the concept of a 'checked exception'. All this method does is hide the act of throwing a checked exception
     * from the java compiler.
     * <p>
     * Note that this method has a return type of {@code RuntimeException}; it is advised you always call this
     * method as argument to the {@code throw} statement to avoid compiler errors regarding no return
     * statement and similar problems. This method won't of course return an actual {@code RuntimeException} -
     * it never returns, it always throws the provided exception.
     *
     * @param t The throwable to throw without requiring you to catch its type.
     * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws an exception!
     */
    public static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) throw new NullPointerException("t");
        return Util.<RuntimeException>sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }
}
