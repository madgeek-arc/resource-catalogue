package eu.einfracentral.utils;

import java.util.Collections;
import java.util.List;

public class SortUtils {

    private SortUtils() {}

    public static <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        if (list != null) {
            Collections.sort(list);
        }
        return list;
    }
}
