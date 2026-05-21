# TCP Chat Application

Ứng dụng chat TCP Socket.

---


## �📋 Yêu Cầu Hệ Thống

- **Java:** JDK 8 hoặc cao hơn
- **MySQL:** 5.7 hoặc cao hơn
- **IDE:** NetBeans, Eclipse, IntelliJ IDEA (hoặc dùng lệnh terminal)

---

## 🗄️ Tạo Database

### 1. Kết Nối MySQL
```bash
mysql -u root -p
```

### 2. Tạo Database
```sql
CREATE DATABASE Chat;
USE Chat;
```

### 3. Tạo Table Chat History
```sql
CREATE TABLE chat_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME DEFAULT NOW()
);
```

### 4. (Tuỳ chọn) Tạo User cho App
```sql
CREATE USER 'chatuser'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON Chat.* TO 'chatuser'@'localhost';
FLUSH PRIVILEGES;
```

---

## 🔧 Cấu Hình Database

Mở file: `src/main/java/dack/database/MyConnect.java`

**Cập nhật thông tin kết nối:**
```java
String URL = "jdbc:mysql://localhost:3306/Chat?allowPublicKeyRetrieval=true&useSSL=false";
Connection conn = DriverManager.getConnection(URL, "root", "your_password");
// Hoặc nếu dùng user riêng:
// Connection conn = DriverManager.getConnection(URL, "chatuser", "password123");
```

---

## 💻 Chạy Local (Trên Cùng 1 Máy)

### 1. Biên Dịch (Compile)
```bash
cd /home/dngnguyen/Documents/ky_254/DGHNM/DACK
mvn clean compile
```

Hoặc nếu không dùng Maven:
```bash
javac -d target/classes src/main/java/dack/**/*.java
```

### 2. Chạy Server
```bash
mvn exec:java -Dexec.mainClass="dack.server.ChatServer"
```

Output:
```
Server dang chay tai port 8888
```

### 3. Chạy Client (mở terminal mới)
```bash
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"
```

Hoặc chạy nhiều client:
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"

# Terminal 2
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"

# Terminal 3
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"
```

### 4. Đăng Nhập
- **IP Server:** `localhost`
- **Port:** `8888`
- **Họ Tên:** Tên của bạn
- **MSSV:** Mã sinh viên (tuỳ ý)

---

## 🌐 Chạy Trên LAN (Nhiều Máy)

### Yêu Cầu
- Tất cả máy kết nối trên cùng mạng WiFi/LAN
- Tường lửa (Firewall) cho phép port 8888

### 1. Cấu Hình Server (Máy Server)

**Bước 1:** Lấy IP của máy Server
```bash
# Linux/Mac
ifconfig | grep "inet "

# Windows
ipconfig
```

Ví dụ: `192.168.1.100`

**Bước 2:** Cấu hình Database trên Server (nếu DB ở máy khác)

Nếu MySQL ở máy khác, sửa file `MyConnect.java`:
```java
// Thay localhost thành IP máy chứa MySQL
String URL = "jdbc:mysql://192.168.1.50:3306/Chat?allowPublicKeyRetrieval=true&useSSL=false";
```

**Bước 3:** Chạy Server
```bash
mvn exec:java -Dexec.mainClass="dack.server.ChatServer"
```

Output:
```
Server dang chay tai port 8888
```

### 2. Cấu Hình Client (Máy Client)

**Mở ứng dụng client:**
```bash
mvn exec:java -Dexec.mainClass="dack.client.ChatClientGUI"
```

**Cửa sổ đăng nhập:**
- **IP Server:** `192.168.1.100` (IP của máy Server)
- **Port:** `8888`
- **Họ Tên:** Tên của bạn
- **MSSV:** Mã sinh viên

---

## 🔒 Troubleshooting

### Lỗi: "Khong the ket noi toi server"

**Nguyên nhân:**
1. Server chưa chạy
2. IP/Port sai
3. Tường lửa chặn port 8888

**Giải pháp:**
```bash
# Linux: Cho phép port 8888
sudo ufw allow 8888

# Windows: Thêm exception trong Firewall
# Settings > Firewall > Allow app through firewall > Add ChatServer

# Kiểm tra server đang chạy
netstat -an | grep 8888  # Linux/Mac
netstat -an | findstr 8888  # Windows
```

### Lỗi: "Loi ket noi DB"

**Nguyên nhân:**
1. MySQL chưa chạy
2. Username/Password sai
3. Database chưa tạo

**Giải pháp:**
```bash
# Kiểm tra MySQL đang chạy
mysql -u root -p -e "SELECT 1"

# Tạo lại database
mysql -u root -p < create_database.sql
```

---

## 📁 Cấu Trúc Project

```
DACK/
├── src/main/java/dack/
│   ├── client/
│   │   ├── ChatClientGUI.java      (Giao diện client)
│   │   └── IncomingReader.java     (Nhận tin từ server)
│   ├── server/
│   │   ├── ChatServer.java         (Khởi động server)
│   │   └── ClientHandler.java      (Xử lý client)
│   └── database/
│       ├── DBAccess.java           (Truy cập DB)
│       └── MyConnect.java          (Kết nối MySQL)
├── pom.xml                          (Maven config)
└── README.md                        (File này)
```

---

## 🚀 Tính Năng

- ✅ Chat real-time (TCP Socket)
- ✅ Multi-client support (Đa người dùng)
- ✅ Lưu lịch sử chat (Database)
- ✅ Thread-safe (Thread an toàn)
- ✅ Giao diện Swing đơn giản

---

## � Screenshots

Dưới đây là giao diện của ứng dụng chat:

### Màn hình Đăng Nhập
![Login UI](src/main/resources/pic/login.png)

### Màn hình Chat
![Chat UI](src/main/resources/pic/chat.png)

---

## �📊 Giao Thức Truyền Tin

### Từ Client → Server
```
LOGIN|MSSV|HoTen
CHAT|noi_dung_tin_nhan
```

### Từ Server → Client
```
LOGIN_SUCCESS|MaNhom
BROADCAST|sender|content|timestamp
SYSTEM|thong_bao_he_thong
```

---

## 👨‍💻 Tác Giả

- **Tác giả:** dngnguyen
- **Ngày tạo:** ####-##-##
- **Môn học:** Đánh Giá Hiệu Năng Mạng 

---

## 📝 Ghi Chú

- **Port mặc định:** 8888
- **Charset:** UTF-8 (hỗ trợ tiếng Việt)
- **Database mặc định:** Chat
- **Mode:** GROUP_PTIT_01 (tất cả user vào 1 nhóm)

---

## 🔄 Cập Nhật Lần Sau

- [ ] Hỗ trợ tạo nhiều nhóm chat
- [ ] Lưu lịch sử khi tắt/mở lại
- [ ] Mã hóa mật khẩu
- [ ] Giao diện đẹp hơn với JFoenix

---

**Hỏi đáp:** Nếu có lỗi, kiểm tra console output hoặc file log để biết chi tiết lỗi.
