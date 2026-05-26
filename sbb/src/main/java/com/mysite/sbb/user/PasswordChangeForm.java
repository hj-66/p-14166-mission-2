package com.mysite.sbb.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeForm {
    @NotEmpty(message = "현재 비밀번호는 필수항목입니다.")
    private String currentPassword;

    @NotEmpty(message = "새 비밀번호는 필수항목입니다.")
    private String password1;

    @NotEmpty(message = "새 비밀번호 확인은 필수항목입니다.")
    private String password2;
}
