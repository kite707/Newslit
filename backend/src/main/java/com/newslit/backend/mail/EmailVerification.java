package com.newslit.backend.mail;

import com.newslit.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_verification")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    @Column(name = "code_issued_at")
    private LocalDateTime codeIssuedAt;

    @Builder.Default
    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Builder.Default
    @Column(name = "send_count")
    private Integer sendCount = 0;

    @Column(name = "send_count_reset_dt")
    private LocalDateTime sendCountResetDt;

    public void increaseSendCount() {
        this.sendCount++;
    }

    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public void startNewSendCycle() {
        this.sendCount = 0;
        this.sendCountResetDt = LocalDateTime.now().plusDays(1);
    }

    public void resetAttemptCount() {
        this.attemptCount = 0;
    }

    public void setCode(String code) {
        this.code = code;
        this.codeIssuedAt = LocalDateTime.now();
    }

}
