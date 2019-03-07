package eu.einfracentral.utils;

public class TextUtils {

    private TextUtils() {}

    /**
     * Trims excessive whitespace in a text.
     * @param text
     * @return
     */
    public static String trimWhitespace(String text) {
        return text.replaceAll("\\s+", " ");
    }

    /**
     * Prettifies text by removing spaces before the specified characters and leaving one space after them.
     * @param text
     * @param characters
     * @return
     */
    public static String prettifyText(String text, String characters) {
        text = trimWhitespace(text);
        for (char c : characters.toCharArray()) {
            text = text.replaceAll("(\\s)?"+String.format("\\%s", c)+"(\\s)?", c+" ");
        }
        text = text.replaceAll("(\\s)$", "");
        return text;
    }
}
