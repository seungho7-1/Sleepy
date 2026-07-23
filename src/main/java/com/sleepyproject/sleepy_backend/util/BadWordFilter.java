package com.sleepyproject.sleepy_backend.util;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class BadWordFilter {

    private static final List<String> BAD_WORDS = Arrays.asList(
            "시발", "씨발", "병신", "개새끼", "존나", "좆", "미친", "지랄", "염병"
    );

    /**
     * 입력된 문자열에서 비속어를 ***로 치환합니다.
     */
    public String filter(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        String filteredText = text;
        for (String badWord : BAD_WORDS) {
            filteredText = filteredText.replaceAll(badWord, "***");
        }
        return filteredText;
    }
}
