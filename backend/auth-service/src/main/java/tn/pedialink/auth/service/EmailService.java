package tn.pedialink.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.pedialink.auth.entity.OtpPurpose;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendLoginNotification(String toEmail, String fullName, String ipAddress, String userAgent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PediaLink – New Sign-In Detected");
            helper.setText(buildLoginNotificationHtml(fullName, ipAddress, userAgent), true);
            mailSender.send(message);
            log.info("Login notification sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send login notification to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangedConfirmation(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PediaLink – Password Changed Successfully");
            helper.setText(buildPasswordChangedHtml(fullName), true);
            mailSender.send(message);
            log.info("Password changed email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password changed email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PediaLink – Welcome! Your Account is Verified");
            helper.setText(buildWelcomeHtml(fullName), true);
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOtp(String toEmail, String code, OtpPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);

            if (purpose == OtpPurpose.RESET_PASSWORD) {
                helper.setSubject("PediaLink – Reset Your Password");
                helper.setText(buildResetPasswordHtml(code), true);
            } else {
                helper.setSubject("PediaLink – Verify Your Email");
                helper.setText(buildVerifyEmailHtml(code), true);
            }

            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerifyEmailHtml(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 0;">
                  <div style="max-width: 520px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #1a73e8, #0d47a1); padding: 32px; text-align: center;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 24px;">PediaLink</h1>
                      <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Pediatric Nephrology Platform</p>
                    </div>
                    <div style="padding: 40px 32px;">
                      <h2 style="color: #1a1a1a; margin: 0 0 12px; font-size: 20px;">Verify Your Email Address</h2>
                      <p style="color: #555; line-height: 1.6; margin: 0 0 28px;">Use the code below to verify your email. It expires in <strong>10 minutes</strong>.</p>
                      <div style="background: #f0f4ff; border: 1px solid #c7d7fc; border-radius: 10px; padding: 24px; text-align: center; margin-bottom: 28px;">
                        <span style="font-size: 42px; font-weight: 800; color: #1a73e8; letter-spacing: 10px;">%s</span>
                      </div>
                      <p style="color: #888; font-size: 13px; margin: 0;">If you did not request this, please ignore this email.</p>
                    </div>
                    <div style="background: #f9fafb; padding: 20px 32px; text-align: center; border-top: 1px solid #eee;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2026 PediaLink. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(code);
    }

    private String buildResetPasswordHtml(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 0;">
                  <div style="max-width: 520px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #e53935, #b71c1c); padding: 32px; text-align: center;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 24px;">PediaLink</h1>
                      <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Pediatric Nephrology Platform</p>
                    </div>
                    <div style="padding: 40px 32px;">
                      <h2 style="color: #1a1a1a; margin: 0 0 12px; font-size: 20px;">Reset Your Password</h2>
                      <p style="color: #555; line-height: 1.6; margin: 0 0 28px;">Use the code below to reset your password. It expires in <strong>10 minutes</strong>.</p>
                      <div style="background: #fff0f0; border: 1px solid #fcc; border-radius: 10px; padding: 24px; text-align: center; margin-bottom: 28px;">
                        <span style="font-size: 42px; font-weight: 800; color: #e53935; letter-spacing: 10px;">%s</span>
                      </div>
                      <p style="color: #888; font-size: 13px; margin: 0;">If you did not request a password reset, please ignore this email and your account remains secure.</p>
                    </div>
                    <div style="background: #f9fafb; padding: 20px 32px; text-align: center; border-top: 1px solid #eee;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2026 PediaLink. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(code);
    }

    private String buildLoginNotificationHtml(String fullName, String ipAddress, String userAgent) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 0;">
                  <div style="max-width: 520px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #0f9d58, #00701a); padding: 32px; text-align: center;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 24px;">PediaLink</h1>
                      <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Pediatric Nephrology Platform</p>
                    </div>
                    <div style="padding: 40px 32px;">
                      <h2 style="color: #1a1a1a; margin: 0 0 12px; font-size: 20px;">New Sign-In Detected</h2>
                      <p style="color: #555; line-height: 1.6; margin: 0 0 24px;">Hi <strong>%s</strong>, we detected a new sign-in to your account.</p>
                      <table style="width: 100%%; border-collapse: collapse; background: #f8faff; border-radius: 8px; overflow: hidden; margin-bottom: 24px;">
                        <tr style="border-bottom: 1px solid #e8edf5;">
                          <td style="padding: 12px 16px; color: #888; font-size: 13px; width: 120px;">IP Address</td>
                          <td style="padding: 12px 16px; color: #333; font-size: 13px; font-weight: 600;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding: 12px 16px; color: #888; font-size: 13px;">Device</td>
                          <td style="padding: 12px 16px; color: #333; font-size: 13px; font-weight: 600;">%s</td>
                        </tr>
                      </table>
                      <p style="color: #888; font-size: 13px; margin: 0;">If this was you, no action is needed. If you did not sign in, please reset your password immediately.</p>
                    </div>
                    <div style="background: #f9fafb; padding: 20px 32px; text-align: center; border-top: 1px solid #eee;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2026 PediaLink. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName, ipAddress != null ? ipAddress : "Unknown", userAgent != null ? userAgent : "Unknown");
    }

    private String buildPasswordChangedHtml(String fullName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 0;">
                  <div style="max-width: 520px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #fb8c00, #e65100); padding: 32px; text-align: center;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 24px;">PediaLink</h1>
                      <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Pediatric Nephrology Platform</p>
                    </div>
                    <div style="padding: 40px 32px;">
                      <h2 style="color: #1a1a1a; margin: 0 0 12px; font-size: 20px;">Password Changed Successfully</h2>
                      <p style="color: #555; line-height: 1.6; margin: 0 0 24px;">Hi <strong>%s</strong>, your password has been changed successfully.</p>
                      <div style="background: #fff8f0; border-left: 4px solid #fb8c00; padding: 16px 20px; border-radius: 4px; margin-bottom: 24px;">
                        <p style="color: #555; font-size: 13px; margin: 0;">If you did not make this change, please contact support immediately or use "Forgot Password" to regain access.</p>
                      </div>
                    </div>
                    <div style="background: #f9fafb; padding: 20px 32px; text-align: center; border-top: 1px solid #eee;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2026 PediaLink. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName);
    }

    private String buildWelcomeHtml(String fullName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 0;">
                  <div style="max-width: 520px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #1a73e8, #0d47a1); padding: 32px; text-align: center;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 24px;">PediaLink</h1>
                      <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Pediatric Nephrology Platform</p>
                    </div>
                    <div style="padding: 40px 32px; text-align: center;">
                      <div style="width: 72px; height: 72px; background: #e8f5e9; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 24px;">
                        <span style="font-size: 36px;">✅</span>
                      </div>
                      <h2 style="color: #1a1a1a; margin: 0 0 12px; font-size: 22px;">Welcome to PediaLink, %s!</h2>
                      <p style="color: #555; line-height: 1.6; margin: 0 0 28px;">Your email has been verified and your account is fully activated. You can now access all features of the platform.</p>
                      <p style="color: #888; font-size: 13px; margin: 0;">Thank you for joining us in improving pediatric nephrology care.</p>
                    </div>
                    <div style="background: #f9fafb; padding: 20px 32px; text-align: center; border-top: 1px solid #eee;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2026 PediaLink. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName);
    }
}
