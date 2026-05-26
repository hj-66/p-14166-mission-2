package com.mysite.sbb.user;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.answer.AnswerRepository;
import com.mysite.sbb.password.PasswordResetToken;
import com.mysite.sbb.password.PasswordResetTokenRepository;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.question.QuestionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public SiteUser create(String username, String email, String password) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        this.userRepository.save(user);
        return user;
    }

    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByUsername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    public List<Answer> getAnswerList(SiteUser user) {
        return user.getAnswerList();
    }

    public List<Question> getQuestionList(SiteUser user) {
        return user.getQuestionList();
    }

    @Transactional
    public String requestPasswordReset(String email) {
        Optional<SiteUser> siteUser = userRepository.findByEmail(email);
        if (siteUser.isPresent()) {
            SiteUser user = siteUser.get();
            passwordResetTokenRepository.deleteByUser(user);

            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setUser(user);
            passwordResetToken.setToken(UUID.randomUUID().toString());
            passwordResetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(passwordResetToken);

            return passwordResetToken.getToken();
        }
        return null;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("비밀번호 재설정 토큰이 필요합니다.");
        }

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 비밀번호 재설정 토큰입니다."));

        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 비밀번호 재설정 토큰입니다.");
        }

        if (passwordResetToken.getUsedAt() != null) {
            throw new IllegalArgumentException("이미 사용된 비밀번호 재설정 토큰입니다.");
        }

        SiteUser user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        passwordResetToken.setUsedAt(LocalDateTime.now());
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        SiteUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("siteuser not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
    }
}
