IF OBJECT_ID(N'dbo.Invitations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Invitations (
        Id INT IDENTITY(1,1)
            CONSTRAINT PK_Invitations PRIMARY KEY,

        AccessCode VARCHAR(50) NOT NULL,

        DisplayName NVARCHAR(200) NOT NULL,

        MaxGuests TINYINT NOT NULL,

        IsActive BIT NOT NULL
            CONSTRAINT DF_Invitations_IsActive
            DEFAULT 1,

        ExpiresAt DATETIME2 NULL,

        CreatedAt DATETIME2 NOT NULL
            CONSTRAINT DF_Invitations_CreatedAt
            DEFAULT SYSUTCDATETIME(),

        UpdatedAt DATETIME2 NOT NULL
            CONSTRAINT DF_Invitations_UpdatedAt
            DEFAULT SYSUTCDATETIME(),

        CONSTRAINT UQ_Invitations_AccessCode
            UNIQUE (AccessCode),

        CONSTRAINT CK_Invitations_MaxGuests
            CHECK (
                MaxGuests BETWEEN 1 AND 20
            )
    );
END;

IF OBJECT_ID(N'dbo.RsvpResponses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RsvpResponses (
        Id INT IDENTITY(1,1)
            CONSTRAINT PK_RsvpResponses PRIMARY KEY,

        InvitationId INT NOT NULL,

        GuestName NVARCHAR(200) NOT NULL,

        Contact NVARCHAR(200) NOT NULL,

        AttendanceConfirmed BIT NOT NULL,

        AttendeeCount TINYINT NOT NULL,

        Intolerances NVARCHAR(500) NULL,

        AdditionalComment NVARCHAR(1000) NULL,

        SubmittedAt DATETIME2 NOT NULL
            CONSTRAINT DF_RsvpResponses_SubmittedAt
            DEFAULT SYSUTCDATETIME(),

        UpdatedAt DATETIME2 NOT NULL
            CONSTRAINT DF_RsvpResponses_UpdatedAt
            DEFAULT SYSUTCDATETIME(),

        CONSTRAINT UQ_RsvpResponses_InvitationId
            UNIQUE (InvitationId),

        CONSTRAINT FK_RsvpResponses_Invitations
            FOREIGN KEY (InvitationId)
            REFERENCES dbo.Invitations(Id),

        CONSTRAINT CK_RsvpResponses_AttendeeCount
            CHECK (
                AttendeeCount BETWEEN 0 AND 20
            ),

        CONSTRAINT CK_RsvpResponses_Attendance
            CHECK (
                (
                    AttendanceConfirmed = 1
                    AND AttendeeCount BETWEEN 1 AND 20
                )
                OR
                (
                    AttendanceConfirmed = 0
                    AND AttendeeCount = 0
                )
            )
    );
END;