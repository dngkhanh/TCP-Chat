# 💬 TCP Chat — Group Chat System

<div align="center">

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-5.7+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-Academic-green?style=for-the-badge)

Ứng dụng chat TCP Socket.

**Version 2.1 — History Support**

</div>

---

## 📋 Mục Lục

- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Tạo Database](#️-tạo-database)
- [Cấu Hình Database](#-cấu-hình-database)
- [Chạy Local](#-chạy-local-trên-cùng-1-máy)
- [Chạy Trên LAN](#-chạy-trên-lan-nhiều-máy)
- [Cấu Trúc Project](#-cấu-trúc-project)
- [Tính Năng](#-tính-năng)
- [Giao Thức Truyền Tin](#-giao-thức-truyền-tin)
- [Troubleshooting](#-troubleshooting)

---

## 📋 Yêu Cầu Hệ Thống

| Thành phần | Phiên bản |
|---|---|
| **Java (JDK)** | 25 trở lên |
| **MySQL** | 5.7 trở lên |
| **Maven** | 3.6 trở lên |
| **MySQL Connector/J** | 8.3.0 (tự động qua Maven) |

> 💡 **IDE gợi ý:** NetBeans, IntelliJ IDEA, Eclipse — hoặc dùng lệnh terminal thuần.

---

## 🗄️ Tạo Database

### Bước 1 — Kết nối MySQL
```bash
mysql -u root -p
```

### Bước 2 — Tạo Database & Bảng
```sql
-- Tạo database
CREATE DATABASE IF NOT EXISTS Chat;
USE Chat;

-- Bảng nhóm chat
CREATE TABLE IF NOT EXISTS chat_groups (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng lịch sử tin nhắn
CREATE TABLE IF NOT EXISTS chat_history (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    sender     VARCHAR(100) NOT NULL,
    message    TEXT NOT NULL,
    group_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_name) REFERENCES chat_groups(group_name)
);
```

### Bước 3 — Thêm Dữ Liệu Mẫu
```sql
INSERT IGNORE INTO chat_groups (group_name) VALUES
    ('GROUP_PTIT_01'),
    ('GROUP_PTIT_02'),
    ('GROUP_PTIT_03');
```

### (Tuỳ chọn) Tạo User Riêng cho App
```sql
CREATE USER 'chatuser'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON Chat.* TO 'chatuser'@'localhost';
FLUSH PRIVILEGES;
```

---

## 🔧 Cấu Hình Database

Mở file: `src/main/java/dack/database/MyConnect.java`

Cập nhật thông tin kết nối theo MySQL của bạn:
```java
String URL = "jdbc:mysql://localhost:3306/Chat?allowPublicKeyRetrieval=true&useSSL=false";
Connection conn = DriverManager.getConnection(URL, "your_username", "your_password");
```

> ⚠️ **Lưu ý bảo mật:** File `MyConnect.java` đang hardcode username và password. **Thay thế bằng thông tin MySQL của bạn** trước khi chạy.

---

## 💻 Chạy Local (Trên Cùng 1 Máy)

### 1. Biên Dịch
```bash
cd /path/to/DACK
mvn clean compile
```

### 2. Chạy Server
```bash
mvn exec:java -Dexec.mainClass="dack.server.ChatServer"
```

Output mong đợi:
```
Server dang chay tai port 8888
```

### 3. Chạy Client (mở terminal mới)
```bash
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"
```

> Mở thêm terminal và lặp lại lệnh trên để chạy nhiều client cùng lúc.

### 4. Luồng Đăng Nhập

```
┌─────────────────────────────────────┐
│  Dialog 1 — Thông tin kết nối       │
│  ─────────────────────────────────  │
│  IP Server : localhost              │
│  Port      : 8888                   │
│  Họ Tên    : <tên của bạn>          │
│  MSSV      : <mã sinh viên>         │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Dialog 2 — Chọn nhóm               │
│  ─────────────────────────────────  │
│  Danh sách nhóm tải tự động từ DB  │
│  Chọn nhóm từ dropdown ▼            │
└─────────────────────────────────────┘
              ↓
    50 tin nhắn lịch sử hiển thị (màu xám)
    ──────── Tin nhắn mới ────────
    Bắt đầu chat real-time 🚀
```

---

## 🌐 Chạy Trên LAN (Nhiều Máy)

### Yêu cầu
- Tất cả máy cùng mạng WiFi / LAN
- Firewall cho phép port `8888`

### Máy chạy Server

**1. Lấy IP của máy Server:**
```bash
# Linux / macOS
ip addr show | grep "inet "

# Windows
ipconfig
```
Ví dụ kết quả: `192.168.1.100`

**2. (Nếu MySQL ở máy khác) Sửa `MyConnect.java`:**
```java
// Thay localhost → IP máy chứa MySQL
String URL = "jdbc:mysql://192.168.1.50:3306/Chat?allowPublicKeyRetrieval=true&useSSL=false";
```

**3. Chạy Server:**
```bash
mvn exec:java -Dexec.mainClass="dack.server.ChatServer"
```

### Máy chạy Client

```bash
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"
```

Ở Dialog đăng nhập:
- **IP Server:** `192.168.1.100` *(IP máy đang chạy server)*
- **Port:** `8888`

---

## 📁 Cấu Trúc Project

```
DACK/
├── src/
│   └── main/
│       ├── java/dack/
│       │   ├── client/
│       │   │   ├── ChatClientGUI.java     # Giao diện Swing, xử lý đăng nhập & gửi tin
│       │   │   └── IncomingReader.java    # Thread nhận & render tin: HISTORY, BROADCAST, SYSTEM
│       │   ├── server/
│       │   │   ├── ChatServer.java        # Khởi động server, lắng nghe kết nối TCP
│       │   │   └── ClientHandler.java     # Thread xử lý từng client, gửi history sau login
│       │   └── database/
│       │       ├── DBAccess.java          # Thực thi SQL: update() và query()
│       │       └── MyConnect.java         # Kết nối MySQL; getGroups(), getHistory(), validateGroup()
│       └── resources/
│           └── pic/
│               ├── login.png              # Screenshot màn hình đăng nhập
│               └── chat.png              # Screenshot màn hình chat
├── pom.xml                               # Maven config (Java 25, mysql-connector-j 8.3.0)
└── README.md
```

---

## 🚀 Tính Năng

| Tính năng | Mô tả |
|---|---|
| ✅ **Multi-Group Chat** | Hỗ trợ nhiều nhóm chat riêng biệt, quản lý qua database |
| ✅ **Group Selection** | Client chọn nhóm từ dropdown — danh sách tải từ server |
| ✅ **Chat History** | Tự động tải 50 tin nhắn gần nhất khi join nhóm (non-blocking) |
| ✅ **Real-time Chat** | Truyền tin tức thời qua TCP Socket |
| ✅ **HTML Render UI** | `JTextPane` render HTML, tự canh lề khi resize |
| ✅ **Thread-Safe** | `ConcurrentHashMap` + `CopyOnWriteArrayList` cho multi-client |
| ✅ **SQL Injection Safe** | `PreparedStatement` cho toàn bộ database query |
| ✅ **HTML Escape** | Escape `<`, `>`, `&`, `"` tránh vỡ layout |
| ✅ **Socket Timeout** | 180 giây — tránh treo kết nối zombie |
| ✅ **System Messages** | Thông báo khi user tham gia / rời nhóm |

---

## 🖼️ Screenshots

### Màn hình Đăng Nhập
![Login UI](src/main/resources/pic/login.png)

### Màn hình Chat
![Chat UI](src/main/resources/pic/chat.png)

---

## 📡 Giao Thức Truyền Tin

### Client → Server

| Lệnh | Mô tả |
|---|---|
| `GET_GROUPS` | Yêu cầu danh sách nhóm hiện có |
| `LOGIN\|MSSV\|HoTen\|Group` | Đăng nhập vào nhóm cụ thể |
| `CHAT\|noi_dung` | Gửi tin nhắn |

### Server → Client

| Lệnh | Mô tả |
|---|---|
| `GROUPS\|G1,G2,G3` | Danh sách nhóm từ database |
| `LOGIN_SUCCESS\|GroupName` | Đăng nhập thành công |
| `LOGIN_FAILED\|LyDo` | Đăng nhập thất bại |
| `HISTORY\|sender\|content\|time` | Một dòng tin lịch sử (lặp nhiều lần) |
| `HISTORY_END` | Kết thúc phần lịch sử |
| `BROADCAST\|sender\|content\|time` | Tin nhắn real-time từ user khác |
| `SYSTEM\|thong_bao` | Thông báo hệ thống (join / leave) |

### Luồng Kết Nối Đầy Đủ

```
Client                                Server
  │──── GET_GROUPS ─────────────────▶ │
  │◀─── GROUPS|G1,G2,G3 ──────────── │
  │──── LOGIN|MSSV|Ten|Group ───────▶ │  (validate group vs DB)
  │◀─── LOGIN_SUCCESS|Group ───────── │
  │◀─── HISTORY|sender|msg|time ───── │  (lặp lại, tối đa 50 dòng)
  │◀─── HISTORY_END ───────────────── │
  │◀─── SYSTEM|Ten da tham gia ─────  │
  │           [chat bình thường]       │
  │──── CHAT|noi_dung ──────────────▶ │
  │◀─── BROADCAST|sender|msg|time ─── │  (gửi đến các client khác cùng nhóm)
```

---

## 🔒 Troubleshooting

<details>
<summary><b>❌ "Khong the ket noi toi server"</b></summary>

**Nguyên nhân có thể:**
1. Server chưa chạy
2. IP / Port nhập sai
3. Firewall chặn port 8888

**Giải pháp:**
```bash
# Kiểm tra server đang lắng nghe
netstat -an | grep 8888        # Linux / macOS
netstat -an | findstr 8888     # Windows

# Mở port trên Linux (nếu dùng ufw)
sudo ufw allow 8888
```
</details>

<details>
<summary><b>❌ "Loi ket noi DB"</b></summary>

**Nguyên nhân có thể:**
1. MySQL chưa khởi động
2. Username / Password trong `MyConnect.java` chưa đúng
3. Database `Chat` chưa được tạo

**Giải pháp:**
```bash
# Kiểm tra MySQL đang chạy
mysql -u root -p -e "SELECT 1"

# Tạo lại database theo hướng dẫn phần Tạo Database ở trên
```
</details>

<details>
<summary><b>❌ Lịch sử không hiển thị</b></summary>

**Nguyên nhân có thể:**
1. Bảng `chat_history` chưa có dữ liệu (nhóm mới, chưa ai nhắn)
2. Lỗi kết nối DB khi server gọi `getHistory()`

**Giải pháp:** Kiểm tra console của **server** để xem log lỗi chi tiết.
</details>

---

## 📝 Thông Số Mặc Định

| Tham số | Giá trị |
|---|---|
| Port | `8888` |
| Charset | `UTF-8` |
| Database | `Chat` |
| Socket Timeout | `180 giây` (3 phút) |
| Số tin lịch sử | `50 tin nhắn` gần nhất mỗi lần join |

---

## 👨‍💻 Tác Giả

- **Tác giả:** dngnguyen
- **Môn học:** Đánh Giá Hiệu Năng Mạng

---

> 💬 **Hỏi đáp:** Nếu gặp lỗi, kiểm tra console output của **server** để xem log chi tiết.
