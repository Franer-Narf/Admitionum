package nc.admitionum.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "Invitations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_Invitations_AccessCode",
            columnNames = "AccessCode"
        )
    }
)
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(
        name = "AccessCode",
        nullable = false,
        length = 50
    )
    private String accessCode;

    @Nationalized
    @Column(
        name = "DisplayName",
        nullable = false,
        length = 200
    )
    private String displayName;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(
        name = "MaxGuests",
        nullable = false
    )
    private Integer maxGuests;

    @Column(
        name = "IsActive",
        nullable = false
    )
    private Boolean isActive;

    @Column(name = "ExpiresAt")
    private LocalDateTime expiresAt;

    @Column(
        name = "CreatedAt",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
        name = "UpdatedAt",
        nullable = false
    )
    private LocalDateTime updatedAt;

    public Invitation() {
    }

    public Invitation(
            String accessCode,
            String displayName,
            Integer maxGuests,
            Boolean isActive,
            LocalDateTime expiresAt) {

        this.accessCode = accessCode;
        this.displayName = displayName;
        this.maxGuests = maxGuests;
        this.isActive = isActive;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    private void prepareForInsert() {
        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        if (this.isActive == null) {
            this.isActive = true;
        }

        if (this.createdAt == null) {
            this.createdAt = nowUtc;
        }

        this.updatedAt = nowUtc;
    }

    @PreUpdate
    private void prepareForUpdate() {
        this.updatedAt =
            LocalDateTime.now(ZoneOffset.UTC);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}