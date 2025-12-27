#!/bin/bash

# JXCache 构建脚本
# 用法：./script/build.sh [选项]
# 可选参数：
#   -s, --skip-tests    跳过测试
#   -u, --skip-ui       跳过前端构建
#   -q, --quick         快速构建（跳过测试和前端）
#   -h, --help          显示帮助

set -e

# 获取脚本目录与项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
UI_DIR="$PROJECT_ROOT/jxcache-ui"

# 默认参数
SKIP_TESTS=false
SKIP_UI=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -u|--skip-ui)
            SKIP_UI=true
            shift
            ;;
        -q|--quick)
            SKIP_TESTS=true
            SKIP_UI=true
            shift
            ;;
        -h|--help)
            echo "用法：./script/build.sh [选项]"
            echo "可选参数："
            echo "  -s, --skip-tests    跳过测试"
            echo "  -u, --skip-ui       跳过前端构建"
            echo "  -q, --quick         快速构建（跳过测试和前端）"
            echo "  -h, --help          显示帮助"
            exit 0
            ;;
        *)
            echo "[ERROR] 未知参数：$1"
            exit 1
            ;;
    esac
done

cd "$PROJECT_ROOT"

echo "============================================"
echo "JXCache 构建"
echo "============================================"
echo "项目目录：$PROJECT_ROOT"
echo "跳过测试：$SKIP_TESTS"
echo "跳过前端：$SKIP_UI"
echo "============================================"

# 检查 Maven 环境
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] 未找到 Maven，请先安装。"
    exit 1
fi

echo ""
echo "[INFO] Maven 版本："
mvn -version | head -1

# 构建前端
if [ "$SKIP_UI" = false ] && [ -d "$UI_DIR" ]; then
    echo ""
    echo "============================================"
    echo "[STEP] 构建前端..."
    echo "============================================"
    
    if ! command -v npm &> /dev/null; then
        echo "[WARN] 未找到 npm，跳过前端构建"
    else
        cd "$UI_DIR"
        
        if [ ! -d "node_modules" ]; then
            echo "[INFO] 安装 npm 依赖..."
            npm install --silent
        fi
        
        echo "[INFO] 开始构建前端..."
        npm run build --silent
        
        if [ $? -eq 0 ]; then
            echo "[OK] 前端构建完成"
        else
            echo "[ERROR] 前端构建失败"
            exit 1
        fi
        
        cd "$PROJECT_ROOT"
    fi
fi

# 构建 Maven 项目
echo ""
echo "============================================"
echo "[STEP] 构建 Maven 项目..."
echo "============================================"

MVN_OPTS="-q"

if [ "$SKIP_TESTS" = true ]; then
    MVN_OPTS="$MVN_OPTS -DskipTests"
fi

echo "[INFO] 执行：mvn clean install $MVN_OPTS"
mvn clean install $MVN_OPTS

if [ $? -ne 0 ]; then
    echo "[ERROR] Maven 构建失败"
    exit 1
fi

echo ""
echo "============================================"
echo "[OK] 构建完成"
echo "============================================"
echo ""
echo "已安装的构件："
echo "  - jxcache-common"
echo "  - jxcache-observer"
echo "  - jxcache-aggregator-core"
echo "  - jxcache-registry-spi"
echo "  - jxcache-registry-nacos"
echo "  - jxcache-registry-fixed"
echo "  - jxcache-starter-observer"
echo "  - jxcache-starter-aggregator-core"
echo "  - jxcache-starter-aggregator-nacos"
echo ""
echo "Maven 本地仓库：~/.m2/repository/dev/yibin/"
echo ""
echo "依赖示例："
echo "  <dependency>"
echo "      <groupId>dev.yibin</groupId>"
echo "      <artifactId>jxcache-starter-observer</artifactId>"
echo "      <version>1.0.0</version>"
echo "  </dependency>"
