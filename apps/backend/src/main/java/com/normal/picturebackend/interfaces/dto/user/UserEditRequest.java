package com.normal.picturebackend.interfaces.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户编辑请求")
public class UserEditRequest {


    /**
     * 用户昵称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(
            description = "用户昵称",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String userName;

    /**
     * 用户简介
     */
    @NotNull
    @Size(max = 200, message = "简介最多200字数")
    @Schema(description = "用户简介")
    private String userProfile;

}
