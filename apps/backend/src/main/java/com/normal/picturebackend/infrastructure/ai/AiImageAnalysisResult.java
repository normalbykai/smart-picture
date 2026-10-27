package com.normal.picturebackend.infrastructure.ai;

import java.util.List;

public record AiImageAnalysisResult(String title, List<String> tags) {
    public boolean isValid() {
        return title != null && !title.isBlank() && title.length() <= 60
                && title.equals(title.strip()) && title.chars().noneMatch(c -> c < 32)
                && tags != null && !tags.isEmpty() && tags.size() <= 5
                && tags.stream().allMatch(tag -> tag != null && !tag.isBlank() && tag.length() <= 12
                    && tag.equals(tag.strip()) && tag.chars().noneMatch(c -> c < 32))
                && tags.stream().distinct().count() == tags.size();
    }
}
