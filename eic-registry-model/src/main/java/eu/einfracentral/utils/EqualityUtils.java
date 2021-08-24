package eu.einfracentral.utils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class EqualityUtils {

    public static <T> boolean equals(T a, T b) {

        if (a == b) {
            return true;
        }

        if (a == null) {
            a = b;
            b = null;
        }

        if (Collection.class.isAssignableFrom(a.getClass())) {
            if (((Collection<?>) a).isEmpty() && (b == null || ((Collection<?>) b).isEmpty())) {
                return true;
            }
        } else if (a instanceof String) {
            if ((b == null) && ("".equals(a))) {
                return true;
            }
        }
        return Objects.equals(a, b);
    }

    public static boolean stringListsAreEqual(List<String> list1, List<String> list2) {
        if (stringListIsEmpty(list1) && stringListIsEmpty(list2)) {
            return true;
        }
        return Objects.equals(list1, list2);
    }

    /**
     * Method checking if a {@link List<String>} object is null or is empty or it contains only one entry
     * with an empty String ("")
     *
     * @param list
     * @return
     */
    public static boolean stringListIsEmpty(List<String> list) {
        if (list == null || list.isEmpty()) {
            return true;
        } else return list.size() == 1 && "".equals(list.get(0));
    }

    private EqualityUtils() {}
}
