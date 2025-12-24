package com.ensam.ma.repository;

import com.ensam.ma.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity - Student-Teacher communication
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find messages sent to a user (inbox)
    List<Message> findByRecipientIdOrderBySentAtDesc(Long recipientId);

    // Find unread messages for a user
    List<Message> findByRecipientIdAndReadFalseOrderBySentAtDesc(Long recipientId);

    // Find messages sent by a user (outbox)
    List<Message> findBySenderIdOrderBySentAtDesc(Long senderId);

    // Find messages for a specific course
    List<Message> findByCourseIdOrderBySentAtDesc(Long courseId);

    // Count unread messages for a user
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.read = false")
    long countUnreadByRecipient(@Param("userId") Long userId);

    // Find advice requests for admin (from students)
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :adminId AND m.sender.role = 'ROLE_STUDENT' ORDER BY m.sentAt DESC")
    List<Message> findAdviceRequestsForAdmin(@Param("adminId") Long adminId);

    // Find unreplied advice requests for admin
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :adminId AND m.sender.role = 'ROLE_STUDENT' AND m.replied = false ORDER BY m.sentAt DESC")
    List<Message> findUnrepliedAdviceRequests(@Param("adminId") Long adminId);
}
