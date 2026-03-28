package com.sep490.g28.hvh.be.integration.email;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * EmailService implementation using RabbitMQ.
 * <p>
 * Emails are published to a queue for async processing.
 * </p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Prepare email subject and body</li>
 *   <li>Publish email jobs to message queue</li>
 * </ul>
 *
 * <p>Does NOT:</p>
 * <ul>
 *   <li>Send emails directly</li>
 *   <li>Handle SMTP failures</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RabbitMQEmailService implements EmailService {

    EmailPublisher emailPublisher;

    @Override
    public void sendApproveRegisterVolAccountEmail(String userEmail, String password) {
        String subject = "HVH - Chào mừng bạn";
        String body = String.format("""
                Chúc mừng bạn đã đăng kí tài khoản thành tình nguyện viên thành công trên app Hà Nội Volunteer Hub!
                Hãy sử dụng email này cùng với mật khẩu dưới đây để đăng nhập vào hệ thống. Và để an toàn, hay đổi mật khẩu sau khi đăng nhập thành công!
                Mật khẩu mặc định: %s
                Cảm ơn bạn!
                """, password);
        emailPublisher.enqueue(userEmail, subject, body);
        log.info("Email to inform volunteer about the account was pushed to message queue, toEmail={}", userEmail);
    }

    @Override
    public void sendRejectRegisterVolAccountEmail(String userEmail, String rejectionReason) {
        String subject = "HVH - Hanoi Volunteer Hub";
        String body = String.format("""
                Chào bạn, chúng tôi xin thông báo rằng yêu cầu tạo tài khoản tình nguyện viên trên app Hà Nội Volunteer Hub của bạn đã bị tử chối!
                Lí do: %s
                Hãy thử tạo yêu cầu lại một lần nữa.
                Xin trân trọng cảm ơn bạn
                """, rejectionReason);
        emailPublisher.enqueue(userEmail, subject, body);
        log.info("Email to inform volunteer about the register rejection was pushed to message queue, toEmail={}", userEmail);

    }

    @Override
    public void sendVolAccountRegistrationOtp(String email, String otp) {
        String subject = "HVH - Xác nhận email";
        String body = String.format("""
                Chào bạn, chúng tôi gửi mail này nhằm xác nhận rằng bạn đang sử dụng email này để đăng kí tài khoản tình nguyện viên qua app Hà Nội Volunteer Hub.
                Hãy sử dụng mã OTP dưới đây dể xác nhận.
                Mã OTP: %s
                """, otp);
        emailPublisher.enqueue(email, subject, body);
        log.info("Email contain verify volunteer account registration was pushed to message queue, toEmail={}", email);
    }

    @Override
    public void sendOrgRegistrationOtp(String email, String otp) {
        String subject = "HVH - Xác nhận email dăng kí tổ chức";
        String body = String.format("""
                Chào bạn, chúng tôi gửi mail này nhằm xác nhận rằng bạn đang sử dụng email này để đăng kí một tổ chức tình nguyện trên hệ thống Hà Nội Volunteer Hub.
                Hãy sử dụng mã OTP dưới đây dể xác nhận.
                Mã OTP: %s
                """, otp);
        emailPublisher.enqueue(email, subject, body);
        log.info("Email contain verify organization registration was pushed to message queue, toEmail={}", email);
    }

    @Override
    public void sendVerifyForgotPasswordOtp(String email, String otp) {
        String subject = "HVH - Xác nhận yêu cầu khôi phục mật khẩu";
        String body = String.format("""
                Chào bạn, chúng tôi gửi mail này nhằm xác nhận rằng bạn đang sử dụng email này để khôi phục mật khẩu đăng nhập Hà Nội Volunteer Hub.
                Hãy sử dụng mã OTP dưới đây dể xác nhận.
                Mã OTP: %s
                """, otp);
        emailPublisher.enqueue(email, subject, body);
        log.info("Email contain verify forgot password was pushed to message queue, toEmail={}", email);
    }

    @Override
    public void sendNewPasswordEmail(String email, String newPassword) {
        String subject = "HVH - Khôi phục mật khẩu thành công";
        String body = String.format("""
                Chào bạn, chúng tôi gửi mail thông báo rằng mật khẩu đăng nhập Hà Nội Volunteer Hub của bạn đã được khôi phục.
                Hãy sử dụng email này cùng với mật khẩu dưới đây để đăng nhập vào hệ thống. Và để an toàn, hay đổi mật khẩu sau khi đăng nhập thành công!
                Mật khẩu mặc định: %s
                Cảm ơn bạn!
                """, newPassword);
        emailPublisher.enqueue(email, subject, body);
        log.info("Email contain new password was pushed to message queue, toEmail={}", email);

    }

    @Override
    public void sendApproveRegisterOrganizationEmail(String orgName, String userEmail, String password) {
        String subject = "HVH - Chào mừng " + orgName;
        String body = String.format("""
                Chúc mừng tổ chức %s đã được đăng kí thành công trên hệ thống Hà Nội Volunteer Hub!
                Hãy sử dụng email này cùng với mật khẩu dưới đây để đăng nhập vào hệ thống. Và để an toàn, hay đổi mật khẩu sau khi đăng nhập thành công!
                Mật khẩu mặc định: %s
                Xin trân trong cảm ơn!
                """, orgName, password);
        emailPublisher.enqueue(userEmail, subject, body);
        log.info("Email welcome organization manager was pushed to message queue, toEmail={}", userEmail);

    }

    @Override
    public void sendRejectRegisterOrganizationEmail(String userEmail, String rejectionReason) {
        String subject = "HVH - Hanoi Volunteer Hub";
        String body = String.format("""
                Chào bạn, chúng tôi xin thông báo rằng yêu cầu tạo tổ chức trên hệ thống Hà Nội Volunteer Hub của bạn đã bị tử chối!
                Lí do: %s
                Hãy thử tạo yêu cầu lại một lần nữa.
                Xin trân trọng cảm ơn bạn
                """, rejectionReason);
        emailPublisher.enqueue(userEmail, subject, body);
        log.info("Email inform about organization rejection was pushed to message queue, toEmail={}", userEmail);

    }

    @Override
    public void sendCreateHostAccountEmail(String orgName, String hostEmail, String password) {
        String subject = "HVH - Chào mừng bạn" ;
        String body = String.format("""
                Chào bạn, bạn đã được quản lí của tổ chức %s mời vào tổ chức trên hệ thống Hà Nội Volunteer Hub.
                Hãy sử dụng email này cùng với mật khẩu dưới đây để đăng nhập vào hệ thống. Và để an toàn, hay đổi mật khẩu sau khi đăng nhập thành công!
                Mật khẩu mặc định: %s
                Xin trân trong cảm ơn!
                """, orgName, password);
        emailPublisher.enqueue(hostEmail, subject, body);
        log.info("Email inform host about the account in the organization was pushed to message queue, toEmail={}", hostEmail);

    }

    @Override
    public void sendEventCancelledByHostEmail(String orgManagerEmail, String orgManagerFullName, String organizationName, String eventName, String hostFullName, String hostEmail, String cancelReason) {
        String subject = "HVH - Sự kiện bị hủy";
        String body = String.format("""
                Xin chào %s.
                Hiện tại sự kiện %s của tổ chức %s đã bị hủy bởi host %s (%s) với lí do: %s. \\n
                Do sự kiện của bạn đã bước vào giai đoạn tiến hành cho nên theo quy định của nền tảng, chúng tôi sẽ trừ 3 giờ tín nhiệm của tổ chức bạn.
                Rất tiếc sự kiện của bạn đã phải dừng lại. Chúng tôi mong rằng vẫn có thể đồng hành cùng tổ chức bạn trong những sự kiện sắp tới.
                """,
                orgManagerFullName,
                eventName,
                organizationName,
                hostFullName,
                hostEmail
                );
        emailPublisher.enqueue(orgManagerEmail, subject, body);
        log.info("Email inform organization manager about the event cancellation  by host was pushed to message queue, toEmail={}", orgManagerEmail);
    }

    @Override
    public void sendEventCancelledByAdminEmail(String orgManagerEmail, String orgManagerFullName, String organizationName, String eventName, String cancelReason) {
        String subject = "HVH - Sự kiện bị hủy";
        String body = String.format("""
                Xin chào %s.
                Hiện tại sự kiện %s của tổ chức %s đã bị hủy bởi quản trị viên với lí do: %s. \\n
                Do sự kiện của bạn đã bước vào giai đoạn tiến hành cho nên theo quy định của nền tảng, chúng tôi sẽ trừ 3 giờ tín nhiệm của tổ chức bạn.
                Rất tiếc sự kiện của bạn đã phải dừng lại. Chúng tôi mong rằng vẫn có thể đồng hành cùng tổ chức bạn trong những sự kiện sắp tới.
                """,
                orgManagerFullName,
                eventName,
                organizationName,
                cancelReason
        );
        emailPublisher.enqueue(orgManagerEmail, subject, body);
        log.info("Email inform organization manager about the event cancellation by admin was pushed to message queue, toEmail={}", orgManagerEmail);
    }
}