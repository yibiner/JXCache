#!/bin/bash

# ===============================================
# Ocean Service 多节点功能测试脚本
# 测试 Observer API、Aggregator API 和业务 API
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
OCEAN_NODES=(20001 20002 20003)
OCEAN_PROFILES=(node1 node2 node3)
RIVER_NODES=(19001 19002)
WATER_NODES=(18081 18082 18083)
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
    echo -e "${BLUE}║${NC}${WHITE}           Ocean Service 功能测试                     ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}${DIM}      测试 Observer + Aggregator + 业务 API           ${NC}${BLUE}║${NC}"
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

# 检查 jq 是否安装（可选）
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
    local health_url=$3
    
    printf "  %-20s " "$node_name"
    if curl -s "$health_url" > /dev/null 2>&1; then
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
    
    local url="http://localhost:$port/api/ocean/1"
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
    
    url="http://localhost:$port/api/ocean/name/test"
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

# 测试 Observer API
test_observer_areas() {
    local port=$1
    
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

test_observer_query() {
    local port=$1
    local area=$2
    local cache_name=$3
    
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

test_observer_entry() {
    local port=$1
    local area=$2
    local cache_name=$3
    local key=$4
    
    local url="http://localhost:$port/api/jxc/observer/entry?area=$area&name=$cache_name&key=$key"
    print_request "GET" "$url" ""
    local response=$(curl -s "$url")
    
    if [ -n "$response" ]; then
        if echo "$response" | grep -q "NOT_FOUND"; then
            echo -e "    ${DIM}├─ Response ────────────────────────────────────────${NC}"
            echo -e "    ${DIM}│${NC} ${YELLOW}404 NOT_FOUND${NC}"
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

test_observer_invalidate() {
    local port=$1
    local area=$2
    local cache_name=$3
    local key=$4
    
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

# 测试 Aggregator API
test_aggregator_nodes() {
    local port=$1
    local service_name=$2
    
    local url="http://localhost:$port/api/jxc/aggregate/nodes?serviceName=$service_name"
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

test_aggregator_query() {
    local port=$1
    local service_name=$2
    local area=$3
    local cache_name=$4
    
    local url="http://localhost:$port/api/jxc/aggregate/query?serviceName=$service_name"
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

test_aggregator_consistency() {
    local port=$1
    local service_name=$2
    local area=$3
    local cache_name=$4
    local key=$5
    
    local url="http://localhost:$port/api/jxc/aggregate/entry/consistency?serviceName=$service_name&area=$area&name=$cache_name&key=$key"
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

test_aggregator_invalidate() {
    local port=$1
    local service_name=$2
    local area=$3
    local cache_name=$4
    local key=$5
    
    local url="http://localhost:$port/api/jxc/aggregate/invalidate?serviceName=$service_name&area=$area&name=$cache_name&key=$key"
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
    
    # Step 1: 检查 Ocean 服务状态
    print_step "1" "检查 Ocean 服务状态"
    local ocean_services_ok=true
    for i in "${!OCEAN_NODES[@]}"; do
        local port=${OCEAN_NODES[$i]}
        local profile=${OCEAN_PROFILES[$i]}
        if ! check_service $port "ocean-$profile" "http://localhost:$port/api/ocean/health"; then
            ocean_services_ok=false
        fi
    done
    
    if ! $ocean_services_ok; then
        echo ""
        echo -e "  ${RED}✗ Ocean 服务未启动，请先运行 run-ocean-nodes.sh${NC}"
        echo ""
        exit 1
    fi
    
    # Step 2: 检查其他服务状态
    print_step "2" "检查其他服务状态 (用于聚合测试)"
    
    local water_services_ok=true
    for i in "${!WATER_NODES[@]}"; do
        local port=${WATER_NODES[$i]}
        if ! check_service $port "water-node$((i+1))" "http://localhost:$port/api/water/health"; then
            water_services_ok=false
        fi
    done
    
    local river_services_ok=true
    for i in "${!RIVER_NODES[@]}"; do
        local port=${RIVER_NODES[$i]}
        if ! check_service $port "river-node$((i+1))" "http://localhost:$port/api/river/health"; then
            river_services_ok=false
        fi
    done
    
    if ! $water_services_ok || ! $river_services_ok; then
        echo -e "  ${YELLOW}⚠ 部分服务未启动，相关聚合测试将跳过${NC}"
    fi
    
    # Step 3: 生成 Ocean 缓存数据
    print_step "3" "生成 Ocean 缓存数据"
    for i in "${!OCEAN_NODES[@]}"; do
        local port=${OCEAN_NODES[$i]}
        local profile=${OCEAN_PROFILES[$i]}
        printf "  %-20s " "ocean-$profile"
        
        for j in 1 2 3; do
            local id=$((i * 10 + j))
            curl -s "http://localhost:$port/api/ocean/$id" > /dev/null
            curl -s "http://localhost:$port/api/ocean/name/ocean$id" > /dev/null
        done
        
        echo -e "${GREEN}✓ 已生成${NC}"
    done
    
    # Step 4: 测试 Ocean 业务 API
    print_step "4" "测试 Ocean 业务 API"
    local port=${OCEAN_NODES[0]}
    test_business_api $port "ocean-node1"
    
    # Step 5: 测试 Ocean Observer API
    print_step "5" "测试 Ocean Observer API"
    echo -e "  ${CYAN}ocean-node1${NC}"
    test_observer_areas $port
    test_observer_query $port "default" "oceanCacheById"
    
    # Step 6: 测试单个缓存条目
    print_step "6" "测试单个缓存条目操作"
    echo -e "  ${CYAN}ocean-node1${NC}"
    test_observer_entry $port "default" "oceanCacheById" "1"
    
    echo -e "    ${DIM}准备: 生成测试缓存 (key=999)${NC}"
    curl -s "http://localhost:$port/api/ocean/999" > /dev/null
    echo ""
    test_observer_invalidate $port "default" "oceanCacheById" "999"
    
    # Step 7: 测试 Aggregator API - Ocean 自身
    print_step "7" "测试 Aggregator API - Ocean 服务"
    echo -e "  ${CYAN}聚合查询 Ocean 服务${NC}"
    test_aggregator_nodes $port "ocean"
    test_aggregator_query $port "ocean" "default" "oceanCacheById"
    test_aggregator_consistency $port "ocean" "default" "oceanCacheById" "1"
    
    # Step 8: 测试聚合查询其他服务
    print_step "8" "测试聚合查询其他服务"
    
    if $water_services_ok; then
        echo -e "  ${CYAN}聚合查询 Water 服务${NC}"
        echo -e "    ${DIM}准备: 生成 Water 缓存数据${NC}"
        for water_port in "${WATER_NODES[@]}"; do
            curl -s "http://localhost:$water_port/api/water/1" > /dev/null
        done
        echo ""
        test_aggregator_nodes $port "water"
        test_aggregator_query $port "water" "default" "dropletCacheById"
    else
        echo -e "  ${YELLOW}⚠ 跳过 Water 聚合测试${NC}"
    fi
    echo ""
    
    if $river_services_ok; then
        echo -e "  ${CYAN}聚合查询 River 服务${NC}"
        echo -e "    ${DIM}准备: 生成 River 缓存数据${NC}"
        for river_port in "${RIVER_NODES[@]}"; do
            curl -s "http://localhost:$river_port/api/river/1" > /dev/null
        done
        echo ""
        test_aggregator_nodes $port "river"
        test_aggregator_query $port "river" "default" "riverCacheById"
    else
        echo -e "  ${YELLOW}⚠ 跳过 River 聚合测试${NC}"
    fi
    
    # Step 9: 测试聚合失效缓存
    print_step "9" "测试聚合失效缓存"
    echo -e "  ${CYAN}聚合失效 Ocean 缓存${NC}"
    echo -e "    ${DIM}准备: 在所有节点生成缓存 (key=888)${NC}"
    for ocean_port in "${OCEAN_NODES[@]}"; do
        curl -s "http://localhost:$ocean_port/api/ocean/888" > /dev/null
    done
    echo ""
    test_aggregator_invalidate $port "ocean" "default" "oceanCacheById" "888"
    
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
