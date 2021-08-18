package eu.einfracentral.utils;

import java.util.List;
import java.util.stream.Collectors;

public class ListUtils {

    public static <T> List<T> remainingItems(List<T> items, List<T> itemsToExclude) {
        return items
                .stream()
                .distinct()
                .filter(item -> !itemsToExclude.contains(item))
                .collect(Collectors.toList());
    }

    private ListUtils() {
    }
}
