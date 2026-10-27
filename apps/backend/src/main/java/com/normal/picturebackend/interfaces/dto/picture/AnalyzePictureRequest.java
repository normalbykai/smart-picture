package com.normal.picturebackend.interfaces.dto.picture;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnalyzePictureRequest(@NotNull @Positive Long pictureId) {
}
