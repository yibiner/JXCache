#!/bin/bash

# ===============================================
# Ocean Service 多节点启动脚本
# 启动 node1(20001), node2(20002), node3(20003)
# Ocean 服务包含 Observer 和 Aggregator 功能
# 支持 Nacos 注册中心
# ===============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
DIM='\033[2m'
NC='\033[0m' # No Color

# 配置
SERVICE_NAME="ocean"
NODES=(20001 20002 20003)
PROFILES=(node1 node2 node3)
WAIT_TIMEOUT=90
HEALTH_CHECK_INTERVAL=2

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# 打印分隔线
print_line() {
    echo -e "${DIM}────────────────────────────────────────────────────────${NC}"
}

# 打印标题
print_header() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}        🌊 Ocean Service 多节点启动脚本               ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}${DIM}   启动 node1(20001), node2(20002), node3(20003)      ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 检查 Java 环境
check_java() {
    echo -ne "  ${DIM}[1/2]${NC} 检查 Java 环境 .................... "
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ 未安装${NC}"
        echo -e "        ${RED}请先安装 JDK 1.8+${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 已安装${NC}"
}

# 检查 Maven 环境
check_maven() {
    echo -ne "  ${DIM}[2/2]${NC} 检查 Maven 环境 ................... "
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}✗ 未安装${NC}"
        echo -e "        ${RED}请先安装 Maven 3.6+${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 已安装${NC}"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 1
    fi
    return 0
}

# 等待服务启动
wait_for_service() {
    local port=$1
    local node_name=$2
    local elapsed=0
    
    while [ $elapsed -lt $WAIT_TIMEOUT ]; do
        if curl -s "http://localhost:$port/api/ocean/health" > /dev/null 2>&1; then
            return 0
        fi
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
        echo -ne "\r  ${DIM}[${node_name}]${NC} 等待启动中 ${YELLOW}${elapsed}s${NC} / ${WAIT_TIMEOUT}s ...     "
    done
    
    return 1
}

# 启动单个节点
start_node() {
    local index=$1
    local port=${NODES[$index]}
    local profile=${PROFILES[$index]}
    local node_name="ocean-$profile"
    local log_file="$PROJECT_DIR/$node_name.log"
    local pid_file="$PROJECT_DIR/$node_name.pid"
    
    echo -ne "  ${DIM}[$((index+1))/3]${NC} 启动 ${CYAN}$node_name${NC} (端口: $port) ... "
    
    # 检查端口
    if ! check_port $port; then
        echo -e "${YELLOW}⚠ 端口被占用${NC}"
        return 1
    fi
    
    cd "$PROJECT_DIR"
    mvn spring-boot:run -Dspring-boot.run.profiles=$profile > "$log_file" 2>&1 &
    local pid=$!
    echo $pid > "$pid_file"
    
    echo ""
    
    # 等待服务启动
    if wait_for_service $port $node_name; then
        echo -e "\r  ${DIM}[$((index+1))/3]${NC} 启动 ${CYAN}$node_name${NC} (端口: $port) ... ${GREEN}✓ 启动成功${NC}     "
        return 0
    else
        echo -e "\r  ${DIM}[$((index+1))/3]${NC} 启动 ${CYAN}$node_name${NC} (端口: $port) ... ${RED}✗ 启动超时${NC}     "
        echo -e "        ${DIM}请检查日志: $log_file${NC}"
        kill $pid 2>/dev/null || true
        return 1
    fi
}

# 停止所有节点
stop_all_nodes() {
    echo ""
    print_line
    echo -e "  ${YELLOW}🛑 正在停止所有服务...${NC}"
    print_line
    
    for i in "${!PROFILES[@]}"; do
        local profile=${PROFILES[$i]}
        local pid_file="$PROJECT_DIR/ocean-$profile.pid"
        
        if [ -f "$pid_file" ]; then
            local pid=$(cat "$pid_file")
            if kill -0 $pid 2>/dev/null; then
                kill $pid 2>/dev/null || true
                echo -e "  ${GREEN}✓${NC} ocean-$profile (PID: $pid) 已停止"
            fi
            rm -f "$pid_file"
        fi
    done
    
    echo ""
    echo -e "  ${GREEN}✓ 所有服务已停止${NC}"
    echo ""
}

# 清理函数
cleanup() {
    stop_all_nodes
    exit 0
}

# 主函数
main() {
    print_header
    
    # 提示依赖
    echo -e "  ${YELLOW}⚠ 注意: Ocean 服务需要 Redis 集群支持${NC}"
    echo ""
    
    # 环境检查
    echo -e "${WHITE}▸ 环境检查${NC}"
    print_line
    check_java
    check_maven
    echo ""
    
    # 启动节点
    echo -e "${WHITE}▸ 启动服务节点${NC}"
    print_line
    
    for i in "${!NODES[@]}"; do
        if ! start_node $i; then
            echo ""
            echo -e "  ${RED}✗ 启动失败，正在清理...${NC}"
            stop_all_nodes
            exit 1
        fi
    done
    echo ""
    
    # 显示服务信息
    echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}${WHITE}            ✓ 所有服务启动成功！                      ${NC}${GREEN}║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    echo -e "${WHITE}▸ 服务信息${NC}"
    print_line
    echo -e "  ${DIM}服务名称${NC}          ${DIM}地址${NC}"
    for i in "${!NODES[@]}"; do
        local port=${NODES[$i]}
        local profile=${PROFILES[$i]}
        printf "  %-17s ${CYAN}http://localhost:%s${NC}\n" "ocean-$profile" "$port"
    done
    echo ""
    
    echo -e "${WHITE}▸ 快速测试命令${NC}"
    print_line
    echo -e "  ${DIM}# 1. 生成缓存数据${NC}"
    for i in "${!NODES[@]}"; do
        local port=${NODES[$i]}
        echo -e "  curl http://localhost:$port/api/ocean/$((i+1))"
    done
    echo ""
    echo -e "  ${DIM}# 2. 查看缓存区域${NC}"
    echo -e "  curl http://localhost:20001/api/jxc/observer/areas"
    echo ""
    echo -e "  ${DIM}# 3. 聚合查询 Ocean 服务${NC}"
    echo -e "  curl 'http://localhost:20001/api/jxc/aggregate/nodes?serviceName=ocean'"
    echo ""
    
    echo -e "${WHITE}▸ 日志文件${NC}"
    print_line
    for i in "${!PROFILES[@]}"; do
        local profile=${PROFILES[$i]}
        echo -e "  ocean-$profile: ${DIM}$PROJECT_DIR/ocean-$profile.log${NC}"
    done
    echo ""
    
    print_line
    echo -e "  ${YELLOW}按 Ctrl+C 停止所有服务${NC}"
    print_line
    echo ""
    
    # 设置中断处理
    trap cleanup INT TERM
    
    # 等待
    wait
}

# 运行主函数
main
