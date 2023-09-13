package io.unlogged.mocking;

public enum MethodExitType {
    NORMAL, EXCEPTION, NULL;

    @Override
    public String toString() {
        switch (this) {
            case EXCEPTION:
                return "Throw exception";
            case NORMAL:
                return "Return class instance";
            case NULL:
                return "Return null";
        }
        return "n/a";
    }
}
