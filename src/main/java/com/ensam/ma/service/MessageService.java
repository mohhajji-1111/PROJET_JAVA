package com.ensam.ma.service;

import com.ensam.ma.model.Course;
import com.ensam.ma.model.Message;
import com.ensam.ma.model.User;
import com.ensam.ma.repository.MessageRepository;
import com.ensam.ma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Student-Teacher messaging (Advice System)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * Send an advice request from student to course admin
     */
    public Message sendAdviceRequest(User student, Course course, String subject, String content) {
        log.info("Student {} sending advice request for course {}", student.getUsername(), course.getTitle());

        User admin = course.getCreatedBy();
        if (admin == null) {
            // Find any admin
            admin = userRepository.findAll().stream()
                    .filter(u -> u.getRole().name().equals("ROLE_ADMINISTRATOR"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No administrator found"));
        }

        Message message = Message.builder()
                .sender(student)
                .recipient(admin)
                .course(course)
                .subject(subject)
                .content(content)
                .read(false)
                .replied(false)
                .build();

        message = messageRepository.save(message);
        log.info("Advice request sent: ID={}", message.getId());

        return message;
    }

    /**
     * Reply to a message
     */
    public Message reply(Long messageId, User sender, String content) {
        Message original = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Mark original as replied
        original.setReplied(true);
        messageRepository.save(original);

        // Create reply message
        Message reply = Message.builder()
                .sender(sender)
                .recipient(original.getSender())
                .course(original.getCourse())
                .subject("Re: " + original.getSubject())
                .content(content)
                .replyTo(original)
                .read(false)
                .replied(false)
                .build();

        reply = messageRepository.save(reply);
        log.info("Reply sent to message ID={}", messageId);

        return reply;
    }

    /**
     * Get inbox for a user
     */
    public List<Message> getInbox(Long userId) {
        return messageRepository.findByRecipientIdOrderBySentAtDesc(userId);
    }

    /**
     * Get unread messages count
     */
    public long getUnreadCount(Long userId) {
        return messageRepository.countUnreadByRecipient(userId);
    }

    /**
     * Get advice requests for admin
     */
    public List<Message> getAdviceRequests(Long adminId) {
        return messageRepository.findAdviceRequestsForAdmin(adminId);
    }

    /**
     * Get unreplied advice requests for admin
     */
    public List<Message> getUnrepliedAdviceRequests(Long adminId) {
        return messageRepository.findUnrepliedAdviceRequests(adminId);
    }

    /**
     * Mark message as read
     */
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }
    }

    /**
     * Get message by ID
     */
    public Message getMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
    }

    /**
     * Get sent messages for a user
     */
    public List<Message> getSentMessages(Long userId) {
        return messageRepository.findBySenderIdOrderBySentAtDesc(userId);
    }
}
