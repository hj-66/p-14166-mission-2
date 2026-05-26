package com.mysite.sbb.user;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signup(UserCreateForm userCreateForm) {
        return "signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup_form";
        }

        if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "signup_form";
        }

        try {
            userService.create(userCreateForm.getUsername(),
                    userCreateForm.getEmail(), userCreateForm.getPassword1());
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "signup_form";
        } catch (Exception e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", e.getMessage());
            return "signup_form";
        }
        return "redirect:/question/list";
    }

    @GetMapping("/login")
    public String login() {
        return "login_form";
    }

    @GetMapping("/password/forgot")
    public String forgotPassword(PasswordResetRequestForm passwordResetRequestForm) {
        return "password_forgot_form";
    }

    @PostMapping("/password/forgot")
    public String requestPasswordReset(@Valid PasswordResetRequestForm passwordResetRequestForm,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            return "password_forgot_form";
        }

        String token = userService.requestPasswordReset(passwordResetRequestForm.getEmail());
        if (token != null) {
            model.addAttribute("resetUrl", "/user/password/reset/" + token);
        }
        model.addAttribute("message", "비밀번호 재설정 안내를 확인해 주세요.");
        return "password_forgot_form";
    }

    @GetMapping("/password/reset/{token}")
    public String resetPassword(@PathVariable("token") String token, PasswordResetForm passwordResetForm) {
        passwordResetForm.setToken(token);
        return "password_reset_form";
    }

    @PostMapping("/password/reset/{token}")
    public String postResetPassword(@PathVariable("token") String token,
                                    @Valid PasswordResetForm passwordResetForm,
                                    BindingResult bindingResult) {
        passwordResetForm.setToken(token);
        if (bindingResult.hasErrors()) {
            return "password_reset_form";
        }

        if (!passwordResetForm.getPassword1().equals(passwordResetForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "password_reset_form";
        }

        try {
            userService.resetPassword(token, passwordResetForm.getPassword1());
        } catch (IllegalArgumentException e) {
            bindingResult.reject("resetPasswordFailed", e.getMessage());
            return "password_reset_form";
        }

        return "redirect:/user/login";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/password/change")
    public String changePassword(PasswordChangeForm passwordChangeForm) {
        return "password_change_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/password/change")
    public String postChangePassword(@Valid PasswordChangeForm passwordChangeForm,
                                     BindingResult bindingResult,
                                     Principal principal) {
        if (bindingResult.hasErrors()) {
            return "password_change_form";
        }

        if (!passwordChangeForm.getPassword1().equals(passwordChangeForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "password_change_form";
        }

        try {
            userService.changePassword(principal.getName(),
                    passwordChangeForm.getCurrentPassword(),
                    passwordChangeForm.getPassword1());
        } catch (IllegalArgumentException e) {
            bindingResult.reject("changePasswordFailed", e.getMessage());
            return "password_change_form";
        }

        return "redirect:/question/list";
    }
}
