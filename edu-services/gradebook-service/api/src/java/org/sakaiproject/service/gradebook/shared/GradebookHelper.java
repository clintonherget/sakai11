package org.sakaiproject.service.gradebook.shared;

import org.apache.commons.lang.StringUtils;

public class GradebookHelper {

    /**
     * Validate a grade item title by checking against the reserved characters
     * @param title
     * @throws InvalidGradeItemNameException
     */
    public static void validateGradeItemName(String title) throws InvalidGradeItemNameException {
        if (StringUtils.isBlank(title)
            || StringUtils.containsAny(title, GradebookService.INVALID_CHARS_IN_GB_ITEM_NAME)
            || StringUtils.startsWithAny(title, GradebookService.INVALID_CHARS_AT_START_OF_GB_ITEM_NAME)) {
            throw new InvalidGradeItemNameException("Grade Item name is invalid: " + title);
        }
    }

    private static String REPLACEMENT_CHARACTER = "-";

    public static String sanitizeGradeItemName(String title) {
        if (title == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        boolean atStart = true;

        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));

            if (atStart &&
                    StringUtils.indexOfAny(ch, GradebookService.INVALID_CHARS_AT_START_OF_GB_ITEM_NAME) >= 0) {
                result.append(REPLACEMENT_CHARACTER);
            } else {
                atStart = false;

                if (StringUtils.containsAny(ch, GradebookService.INVALID_CHARS_IN_GB_ITEM_NAME)) {
                    result.append(REPLACEMENT_CHARACTER);
                } else {
                    result.append(ch);
                }
            }
        }

        return result.toString();
    }
}
