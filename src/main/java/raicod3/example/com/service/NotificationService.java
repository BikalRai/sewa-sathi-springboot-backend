package raicod3.example.com.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.dto.notification.NotificationResponseDto;
import raicod3.example.com.enums.TokenType;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Notification;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.NotificationRepository;
import raicod3.example.com.repository.OTPTokenRepository;
import raicod3.example.com.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {


    private final JavaMailSender mailSender;
    private final OTPTokenRepository otpTokenRepository;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;



    public void sendEmail(EmailRequest req, String otpToken, String templatePath) throws MessagingException {
        User user = userRepository.findUserByEmail(req.getEmail()).orElseThrow(() -> new BadRequestException("User not found"));

        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("otpToken", otpToken);
        context.setVariable("year", LocalDate.now().getYear());
        context.setVariable("category", req.getCategory());
        context.setVariable("description", req.getDescription());  // <-- new, generic passthrough
        context.setVariable("jobLink", req.getJobLink());

        String htmlContent = templateEngine.process(templatePath, context);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");
        messageHelper.setTo(req.getEmail());
        messageHelper.setSubject(req.getSubject());
        messageHelper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email sent to: {}", req.getEmail());
    }


}
