package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextUtils {

    private static final Logger logger = LoggerFactory.getLogger(TextUtils.class);

    private TextUtils() {
    }

    /**
     * Trims excessive whitespace in a text.
     *
     * @param text
     * @return
     */
    public static String trimWhitespace(String text) {
        if (text != null) {
            return text.replaceAll("\\s+", " ");
        }
        return null;
    }

    /**
     * Prettifies text by removing spaces before the specified characters and leaving one space after them.
     *
     * @param text
     * @param characters
     * @return
     */
    public static String prettifyText(String text, String characters) {
        if (text != null) {
            text = trimWhitespace(text);
            for (char c : characters.toCharArray()) {
                text = text.replaceAll("(\\s)?" + String.format("\\%s", c) + "(\\s)?", c + " ");
            }
            text = text.replaceAll("(\\s)$", "");
        }
        return text;
    }

    /**
     * Capitalizes first letter.
     *
     * @param text
     * @return
     */
    public static String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Formats a numerical value's precision to a maximum number of decimal digits.
     *
     * @param value
     * @param precision
     * @return
     */
    public static String formatArithmeticPrecision(String value, int precision) {
        if (!value.matches("^(?:(0?\\.)|([1-9]\\d*))\\.?\\d*$")) {
            logger.error("Error: '{}' is not a valid numeric value", value);
            throw new NumberFormatException("Not a valid numeric value...");
        }
        if (precision < 0) {
            logger.error("Error: precision cannot be a negative value : {}", precision);
            throw new NumberFormatException("Arithmetic precision cannot be a negative value");
        }
        String[] parts = value.split("\\.");
        if (parts.length == 2) {
            if (parts[1].length() > precision) {
                parts[1] = parts[1].substring(0, precision);
            }
            if (precision > 0 && !parts[1].matches("^0+$")) {
                return String.join(".", parts);
            }
            return parts[0];
        } else {
            return value;
        }
    }
}
