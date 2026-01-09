package com.mopl.api.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendMail(String email, String tempPassword) {
        try {
            String emailContent =  "<h2>안녕하세요, MOPL 입니다.</h2>"
                + "<br>"
                + "<p>요청하신 임시 비밀번호를 안내드립니다.</p>"
                + "<p><strong>임시 비밀번호: </strong> " + tempPassword + "</p>"
                + "<br>"
                + "<p>3분 이내로 반드시 비밀번호를 변경해 주세요.</p>";

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[MOPL] 임시 비밀번호 발급");
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("메일 전송 실패: {}", email, e);
            throw new IllegalStateException("메일 전송 실패했습니다.");
        }
    }

}
