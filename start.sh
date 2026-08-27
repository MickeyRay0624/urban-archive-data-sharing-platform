#!/bin/bash

echo "=========================================="
echo "协同共享服务系统启动脚本"
echo "=========================================="

DB_USERNAME_VALUE="${DB_USERNAME:-root}"

if [ -z "${DB_PASSWORD:-}" ]; then
    echo "未设置数据库密码，请先执行：export DB_PASSWORD='你的MySQL密码'"
    exit 1
fi

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "未找到Java环境，请先安装JDK 8或更高版本"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "未找到Maven环境，请先安装Maven 3.6或更高版本"
    exit 1
fi

# 检查MySQL环境
if ! command -v mysql &> /dev/null; then
    echo "未找到MySQL客户端，请先安装MySQL"
    exit 1
fi

echo "正在检查项目依赖..."
mvn dependency:resolve

echo "正在初始化数据库..."
# 检查MySQL是否运行
if ! pgrep -x "mysqld" > /dev/null; then
    echo "MySQL服务未运行，正在尝试启动..."
    # 尝试启动MySQL（macOS）
    if command -v brew &> /dev/null; then
        brew services start mysql
        echo "等待MySQL服务启动..."
        sleep 3
    else
        echo "请手动启动MySQL服务后重新运行此脚本"
        exit 1
    fi
fi

# 等待MySQL完全启动
echo "等待MySQL服务就绪..."
for i in {1..30}; do
    if mysql -u "$DB_USERNAME_VALUE" -p"$DB_PASSWORD" -e "SELECT 1;" &> /dev/null; then
        break
    fi
    if [ $i -eq 30 ]; then
        echo "MySQL服务启动超时，请检查MySQL配置"
        exit 1
    fi
    sleep 1
done

# 执行数据库初始化脚本
echo "正在创建数据库和表结构..."
mysql -u "$DB_USERNAME_VALUE" -p"$DB_PASSWORD" < database-init.sql

if [ $? -eq 0 ]; then
    echo "数据库初始化成功！"
else
    echo "数据库初始化失败，请检查MySQL连接和权限"
    echo "请确保MySQL服务正在运行，并检查DB_USERNAME和DB_PASSWORD环境变量"
    exit 1
fi

echo "正在编译项目..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "编译成功！"
    echo "正在启动应用..."
    echo "=========================================="
    echo "应用将在 http://localhost:8080 启动"
    echo "=========================================="
    mvn spring-boot:run
else
    echo "编译失败，请检查错误信息"
    exit 1
fi
