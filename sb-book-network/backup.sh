#!/bin/bash

# Thiết lập biến cho đường dẫn lưu trữ log trong dự án Maven
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/backup.log"

# Tạo thư mục log nếu chưa tồn tại
mkdir -p "$LOG_DIR"

# Sử dụng wait-for-it để kiểm tra kết nối MySQL
/usr/local/bin/wait-for-it.sh mysql-sb-book-social-network:3306 --timeout=30 --strict -- echo "MySQL is up" >> "$LOG_FILE"

# Thiết lập các biến môi trường
TIMESTAMP=$(date +"%F")
BACKUP_DIR="/backups/$TIMESTAMP"
BACKUP_FILE="$BACKUP_DIR/backup.sql"
MYSQL_USER="admin"
MYSQL_PASSWORD="abc@#123"
MYSQL_HOST="mysql-sb-book-social-network"
MYSQL_DB="bsndb"

# Kiểm tra nếu file backup chưa tồn tại, thực hiện backup
if [ ! -f "$BACKUP_FILE" ]; then
    mysqldump --user=$MYSQL_USER --password=$MYSQL_PASSWORD --host=$MYSQL_HOST $MYSQL_DB > "$BACKUP_FILE" 2>> "$LOG_FILE"

    # Kiểm tra lệnh mysqldump có thành công hay không
    if [ $? -eq 0 ]; then
        echo "MySQL backup completed successfully." >> "$LOG_FILE"
    else
        echo "MySQL backup failed. Please check logs for details." >> "$LOG_FILE"
    fi
else
    echo "Backup file already exists. Skipping backup." >> "$LOG_FILE"
fi

# Hiển thị nội dung của file backup sau khi backup hoàn tất
if [ -f "$BACKUP_FILE" ]; then
    echo "Backup file contents:" >> "$LOG_FILE"
    cat "$BACKUP_FILE" >> "$LOG_FILE"
fi

# Đợi 5 phút trước khi tự động đóng cửa sổ
echo "Auto closing in 5 minutes..." >> "$LOG_FILE"
sleep 60  # 60 seconds = 1 minutes

echo "Closing..." >> "$LOG_FILE"

# Đóng cửa sổ khi script backup đã hoàn tất
exit 0
