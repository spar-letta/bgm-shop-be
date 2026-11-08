package com.app.itibari.utils.helpers;

public class Validators {

    public static boolean allEqualNull(Object... objs) {

        for (Object o : objs) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean allNotEqualNull(Object... objs) {

        for (Object o : objs) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean anyEqualNull(Object... objs) {

        for (Object o : objs) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    public static int countTrue(Boolean... bools) {
        int count = 0;
        for (Boolean b : bools) {
            if (b) {
                count++;
            }
        }
        return count;
    }
}
