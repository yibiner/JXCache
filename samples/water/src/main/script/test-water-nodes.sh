#!/bin/bash

# ===============================================
# Water Service 多节点功能测试脚本
# 测试 Observer API 和业务 API
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
NODES=(18081 18082 18083)
PROFILES=(node1 node2 node3)
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 打印分隔线
print_line() {
    echo -e "${DIM}────────────────────────────────────────────────────────${NC}"
}

# 打印标题
print_header() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}           Water Service 功能测试                     ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}${DIM}           测试 Observer API 和业务 API               ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 打印步骤标题
print_step() {
    local step=$1
    local title=$2
    echo ""
    echo -e "${WHITE}▸ Step $step: $title${NC}"
    print_line
}

# 记录测试结果
record_test() {
    local result=$1
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$result" = "pass" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# 打印请求信息
print_request() {
    local method=$1
    local url=$2
    local body=$3
    
    echo -e "    ${DIM}┌─ Request ─────────────────────────────────────────${NC}"
    echo -e "    ${DIM}│${NC} ${CYAN}$method${NC} $url"
    if [ -n "$body" ]; then
        echo -e "    ${DIM}│${NC} Body: ${DIM}$body${NC}"
    fi
}

# 打印响应信息
print_response() {
    local status=$1
    local response=$2
    
    if [ "$status" = "success" ]; then
        echo -e "    ${DIM}├─ Response ────────────────────────────────────────${NC}"
        echo -e "    ${DIM}│${NC} ${GREEN}200 OK${NC}"
        echo -e "    ${DIM}│${NC} ${DIM}$response${NC}"
        echo -e "    ${DIM}└─ Result: ${NC}${GREEN}✓ PASS${NC}"
    else
        echo -e "    ${DIM}├─ Response ────────────────────────────────────────${NC}"
        echo -e "    ${DIM}│${NC} ${RED}ERROR${NC}"
        echo -e "    ${DIM}│${NC} ${DIM}$response${NC}"
        echo -e "    ${DIM}└─ Result: ${NC}${RED}✗ FAIL${NC}"
    fi
    echo ""
}

# 检查 curl 是否安装
if ! command -v curl &> /dev/null; then
    echo -e "${RED}✗ curl 未安装，请先安装 curl${NC}"
    exit 1
fi

# 检查 jq 是否安装（可选，用于格式化 JSON）
HAS_JQ=false
if command -v jq &> /dev/null; then
    HAS_JQ=true
fi

# 格式化 JSON 输出
format_json() {
    if $HAS_JQ; then
        jq -c '.' 2>/dev/null || cat
    else
        cat
    fi
}

# 检查服务状态
check_service() {
    local port=$1
    local node_name=$2
    
    printf "  %-20s " "$node_name"
    if curl -s "http://localhost:$port/api/water/health" > /dev/null 2>&1; then
        echo -e "${GREEN}● 运行中${NC}"
        return 0
    else
        echo -e "${RED}○ 未启动${NC}"
        return 1
    fi
}

# 测试业务 API
test_business_api() {
    local port=$1
    local node_name=$2
    local all_passed=true
    
    echo -e "  ${CYAN}$node_name${NC}"
    
    # 测试根据 ID 查询
    local url="http://localhost:$port/api/water/1"
    print_request "GET" "$url" ""
    local response=$(curl -s "$url")
    if [ -n "$response" ]; then
        print_response "success" "$(echo "$response" | format_json)"
        record_test "pass"
    else
        print_response "fail" "Empty response"
        record_test "fail"
        all_passed=false
    fi
    
    # 测试根据名称查询
    url="http://localhost:$port/api/water/name/test"
    print_request "GET" "$url" ""
    response=$(curl -s "$url")
    if [ -n "$response" ]; then
        print_response "success" "$(echo "$response" | format_json)"
        record_test "pass"
    else
        print_response "fail" "Empty response"
        record_test "fail"
        all_passed=false
    fi
    
    # 测试根据颜色查询
    url="http://localhost:$port/api/water/color/蓝色"
    print_request "GET" "$url" ""
    response=$(curl -s "$url")
    if [ -n "$response" ]; then
        print_response "success" "$(echo "$response" | format_json)"
        record_test "pass"
    else
        print_response "fail" "Empty response"
        record_test "fail"
        all_passed=false
    fi
    
    if $all_passed; then
        return 0
    else
        return 1
    fi
}

# 测试 Observer API - 获取缓存区域
test_observer_areas() {
    local port=$1
    local node_name=$2
    
    local url="http://localhost:$port/api/jxc/observer/areas"
    print_request "GET" "$url" ""
    local response=$(curl -s "$url")
    if [ -n "$response" ]; then
        print_response "success" "$(echo "$response" | format_json)"
        record_test "pass"
        return 0
    else
        print_response "fail" "Empty response"
        record_test "fail"
        return 1
    fi
}

# 测试 Observer API - 查询缓存数据
test_observer_query() {
    local port=$1
    local node_name=$2
    local area=$3
    local cache_name=$4
    
    local url="http://localhost:$port/api/jxc/observer/query"
    local body="{\"area\":\"$area\",\"cacheName\":\"$cache_name\",\"pageRequest\":{\"pageNo\":1,\"pageSize\":10}}"
    print_request "POST" "$url" "$body"
    local response=$(curl -s -X POST "$url" \
        -H "Content-Type: application/json" \
        -d "$body")
    
    if [ -n "$response" ]; then
        print_response "success" "$(echo "$response" | format_json)"
        record_test "pass"
        return 0
    else
        print_response "fail" "Empty response"
        record_test "fail"
        return 1
    fi
}

# 测试 Observer API - 获取单个缓存条目
test_observer_entry() {
    local port=$1
    local node_name=$2
    local area=$3
    local cache_name=$4
    local key=$5
    
    local url="http://localhost:$port/api/jxc/observer/entry?area=$area&name=$cache_name&key=$key"
    print_request "GET" "$url" ""
    local response=$(curl -s "$url")
    
    if [ -n "$response" ]; then
        if echo "$response" | grep -q "NOT_FOUND"; then
            echo -e "    ${DIM}├─ Response ────────────────────────────────────────${NC}"
            echo -e "    ${DIM}│${NC} ${YELLOW}404 NOT_FOUND${NC}"
            echo -e "    ${DIM}│${NC} ${DIM}Cache entry not found${NC}"
            echo -e "    ${DIM}└─ Result: ${NC}${YELLOW}⚠ NOT_FOUND${NC}"
            echo ""
            record_test "pass"
        else
            print_response "success" "$(echo "$response" | format_json)"
            record_test "pass"
        fi
        return 0
    else
        print_response "fail" "Empty response"
        record_test "fail"
        return 1
    fi
}

# 测试 Observer API - 失效缓存
test_observer_invalidate() {
    local port=$1
    local node_name=$2
    local area=$3
    local cache_name=$4
    local key=$5
    
    local url="http://localhost:$port/api/jxc/observer/invalidate?area=$area&name=$cache_name&key=$key"
    print_request "DELETE" "$url" ""
    local response=$(curl -s -X DELETE "$url")
    
    if [ -n "$response" ]; then
        print_response "success" "$(echo "$response" | format_json)"
        record_test "pass"
        return 0
    else
        print_response "fail" "Empty response"
        record_test "fail"
        return 1
    fi
}

# 主测试流程
main() {
    print_header
    
    # Step 1: 检查服务状态
    print_step "1" "检查服务状态"
    local all_services_ok=true
    for i in "${!NODES[@]}"; do
        local port=${NODES[$i]}
        local profile=${PROFILES[$i]}
        if ! check_service $port "water-$profile"; then
            all_services_ok=false
        fi
    done
    
    if ! $all_services_ok; then
        echo ""
        echo -e "  ${RED}✗ 部分服务未启动，请先运行 run-water-nodes.sh${NC}"
        echo ""
        exit 1
    fi
    
    # Step 2: 生成缓存数据
    print_step "2" "生成缓存数据"
    for i in "${!NODES[@]}"; do
        local port=${NODES[$i]}
        local profile=${PROFILES[$i]}
        printf "  %-20s " "water-$profile"
        
        # 生成不同的缓存数据
        for j in 1 2 3; do
            local id=$((i * 10 + j))
            curl -s "http://localhost:$port/api/water/$id" > /dev/null
            curl -s "http://localhost:$port/api/water/name/droplet$id" > /dev/null
        done
        curl -s "http://localhost:$port/api/water/color/蓝色" > /dev/null
        curl -s "http://localhost:$port/api/water/color/红色" > /dev/null
        
        echo -e "${GREEN}✓ 已生成${NC}"
    done
    
    # Step 3: 测试业务 API (只测试第一个节点，避免输出过多)
    print_step "3" "测试业务 API"
    local port=${NODES[0]}
    local profile=${PROFILES[0]}
    test_business_api $port "water-$profile"
    
    # Step 4: 测试 Observer API (只测试第一个节点)
    print_step "4" "测试 Observer API"
    echo -e "  ${CYAN}water-node1${NC}"
    test_observer_areas $port "water-node1"
    test_observer_query $port "water-node1" "default" "dropletCacheById"
    test_observer_query $port "water-node1" "water" "dropletCacheByName"
    
    # Step 5: 测试单个缓存条目查询
    print_step "5" "测试单个缓存条目查询"
    echo -e "  ${CYAN}water-node1${NC}"
    test_observer_entry $port "water-node1" "default" "dropletCacheById" "1"
    
    # Step 6: 测试缓存失效
    print_step "6" "测试缓存失效"
    echo -e "  ${CYAN}water-node1${NC}"
    echo -e "    ${DIM}准备: 生成测试缓存 (key=999)${NC}"
    curl -s "http://localhost:$port/api/water/999" > /dev/null
    echo ""
    test_observer_invalidate $port "water-node1" "default" "dropletCacheById" "999"
    
    # Step 7: 验证跨节点数据隔离
    print_step "7" "验证跨节点数据隔离"
    for i in "${!NODES[@]}"; do
        local port=${NODES[$i]}
        local profile=${PROFILES[$i]}
        local url="http://localhost:$port/api/jxc/observer/areas"
        printf "  %-20s " "water-$profile"
        local areas=$(curl -s "$url")
        echo -e "${DIM}$areas${NC}"
    done
    
    # 测试结果汇总
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}                    测试结果汇总                      ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
    printf "  %-20s %s\n" "总测试数:" "$TOTAL_TESTS"
    printf "  %-20s ${GREEN}%s${NC}\n" "通过:" "$PASSED_TESTS"
    printf "  %-20s ${RED}%s${NC}\n" "失败:" "$FAILED_TESTS"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "  ${GREEN}✓ 所有测试通过！${NC}"
    else
        echo -e "  ${RED}✗ 有 $FAILED_TESTS 个测试失败${NC}"
    fi
    
    print_line
    echo ""
    
    return $FAILED_TESTS
}

# 运行测试
main
exit $?
