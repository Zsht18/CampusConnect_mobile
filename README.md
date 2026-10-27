# CampusConnect 🎓

**CampusConnect** is a native Android application and PHP/MySQL backend system designed for university students to share academic discussions, ask questions, upload study resources, and interact in a moderated campus community.

---

## 🚀 Key Features

### 🔐 1. Institutional Student Authentication
- **School Email Requirement**: Accepts institutional email addresses (`.edu` / `.edu.ph`), automatically rejecting personal email domains (Gmail, Yahoo, Outlook, etc.).
- **Email OTP Verification**: Sends a 6-digit verification code to the student's email before registration.
- **Unique Usernames**: Enforces case-insensitive unique username selection.
- **Strong Passwords**: Requires a minimum of 8 characters containing both letters and numbers.
- **Multi-Account Isolation**: Safely manages local sessions and user profile pictures independently for multiple accounts on the same device.

### 📰 2. Interactive Campus Feed
- **College & Category Filtering**: Filter posts by academic colleges or categories.
- **Real-Time Search & Trending Terms**: Instant filtering as you type with a backend tracker that computes top trending search keywords.
- **Swipe-to-Refresh**: Pull-down gesture to reload live posts and comments from the database.
- **Reactions & Bookmarking**: Express reactions with **Likes (👍)** and **Dislikes (👎)**, or save posts for later offline reference.
- **Public & Anonymous Posting**: Choose whether to post under your real username or anonymously.

### 📎 3. Multi-File & Image Attachments
- **Attachment Support**: Upload multiple study files or photos (PDF, DOC, DOCX, JPG, PNG) up to **100 MB**.
- **In-App Media Viewing**: Tap to view full-screen image attachments directly inside the app without downloading.
- **Download Manager**: Integrated with Android DownloadManager to save document attachments to local storage.

### 👤 4. Profile & Account Management
- **Relative Account Age**: Displays human-readable account age (e.g., *"1 Month"*, *"8 Days"*, *"2 Years"*) based on the database creation timestamp.
- **Profile Picture Upload**: Crop, cache locally, and sync custom profile pictures with the MySQL database.
- **Account Security**: Change username or update password with current password verification.
- **Logout Confirmation**: Custom popup dialog to prevent accidental sign-outs.

### 💬 5. Comments & Moderation
- **Comment Threads**: Add, edit, or delete your own comments on any post.
- **Reporting System**: Report inappropriate posts or comments with custom reason choices (*Inappropriate*, *Spam*, *Harassment*, *Non-Academic*).
- **Admin Web Dashboard Integration**: All reports are sent to `mobile_reports` and `mobile_comment_reports` tables for moderation on the web dashboard.

---

## 🛠️ Technology Stack

### **Android Application**
- **Language**: Java
- **Architecture**: MVC / Android Jetpack
- **Min SDK**: 29 (Android 10) | **Target SDK**: 37
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & Gson Converter
- **UI Components**: Material Components 3, CardView, ConstraintLayout, SwipeRefreshLayout, BottomSheetDialog

### **Backend & Database**
- **Server Language**: PHP
- **Database**: MySQL / MariaDB (Hostinger)
- **Database Access**: PDO (PHP Data Objects) with Prepared Statements
- **Data Exchange Format**: JSON

---

## 📁 Project Structure

```
CampusConnect/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/campusconnect/
│           │   ├── api/                  # Retrofit API interface & client singleton
│           │   ├── models/               # Data model objects (PostModel, CommentModel, etc.)
│           │   ├── LoginActivity.java    # User login screen
│           │   ├── SignUpActivity.java   # Student registration & OTP verification
│           │   ├── HomeActivity.java     # Main feed screen with filters & search
│           │   ├── ProfileActivity.java  # User profile, statistics, & post list
│           │   ├── PostDetailsActivity.java # Open post view, media player, & comment thread
│           │   ├── CreatePostActivity.java # Multi-file upload & post creation
│           │   ├── EditPostActivity.java   # Post editor screen
│           │   └── AvatarHelper.java     # Asynchronous profile avatar loader
│           └── res/
│               ├── layout/               # XML Layouts & custom dialogs
│               └── drawable/             # Custom rounded shapes & UI backgrounds
└── backend_setup/                        # Backend PHP endpoints for Hostinger
    ├── db_connect.php                    # PDO Database Connection
    ├── login.php                         # Login authentication
    ├── signup.php                        # Account registration
    ├── send_otp.php                      # Email OTP generator
    ├── verify_otp.php                    # OTP validator
    ├── create_post.php                   # Post creation & attachment upload
    ├── get_posts.php                     # Fetch feed posts
    ├── upload_profile_pic.php            # Base64 image uploader
    ├── submit_report.php                 # Post report processor
    ├── submit_comment_report.php         # Comment report processor
    └── campusconnect_db.sql              # MySQL Database Schema
```

---

## ⚙️ Setup & Deployment Instructions

### 1. Database Setup (Hostinger phpMyAdmin)
Import `backend_setup/campusconnect_db.sql` into your phpMyAdmin MySQL database on Hostinger:

```sql
-- Tables created by schema:
-- mobile_users, mobile_posts, mobile_comments, mobile_searches, mobile_otps, mobile_reports, mobile_comment_reports
```

### 2. Backend PHP Endpoints
Upload all PHP files in `backend_setup/` to your server directory (e.g., `public_html/mobile/`):
Ensure the `uploads/` folder has write permissions (`0755` or `0777`).

### 3. Android App Configuration
1. Open `CampusConnect` in Android Studio.
2. Update the Base URL in `ApiClient.java` to point to your live server endpoint:
   ```java
   public static final String BASE_URL = "https://yourdomain.com/mobile/";
   ```
3. Sync Gradle and run the app on an Android device or emulator.

---

## 📄 License
This project is developed for Capstone 2 academic presentation. All rights reserved.