#!/system/bin/sh
# ColorOS Features Enhance - Service脚本
# 等待用户解锁后，将临时配置移动到app内部存储
MODDIR=${0%/*}

# 应用数据目录
APP_DATA_DIR="/data/media/0/Android/data/com.itosfish.colorfeatureenhance/files/configs"
SYSTEM_BASELINE_DIR="$APP_DATA_DIR/system_baseline"

# 模块临时目录
MODULE_TEMP_DIR="$MODDIR/temp_configs"

# 配置文件名
APP_FEATURES_FILE="com.oplus.app-features.xml"
OPLUS_FEATURES_FILE="com.oplus.oplus-feature.xml"

# 日志函数
log_info() {
    echo "[ColorFeatureEnhance-Service] $(date '+%Y-%m-%d %H:%M:%S') $1" >> /cache/colorfeature_enhance.log
    echo "[ColorFeatureEnhance-Service] $1"
}

log_debug() {
    echo "[ColorFeatureEnhance-Service-DEBUG] $(date '+%Y-%m-%d %H:%M:%S') $1" >> /cache/colorfeature_enhance.log
    echo "[ColorFeatureEnhance-Service-DEBUG] $1"
}

log_info "=== Service脚本启动 ==="
log_debug "MODDIR: $MODDIR"
log_debug "APP_DATA_DIR: $APP_DATA_DIR"
log_debug "SYSTEM_BASELINE_DIR: $SYSTEM_BASELINE_DIR"
log_debug "MODULE_TEMP_DIR: $MODULE_TEMP_DIR"

# 检查初始状态
log_debug "检查临时目录状态:"
if [ -d "$MODULE_TEMP_DIR" ]; then
    log_debug "临时目录存在: $MODULE_TEMP_DIR"
    ls -la "$MODULE_TEMP_DIR" 2>/dev/null | while read line; do
        log_debug "  $line"
    done
else
    log_debug "临时目录不存在: $MODULE_TEMP_DIR"
fi

# 等待系统完全启动（使用用户方案）
wait_for_boot_complete() {
    log_info "等待系统启动完成..."
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        sleep 3
    done
    log_info "系统启动完成"
}


# 移动临时配置到app内部存储
move_temp_to_app() {
    log_info "开始移动临时配置到app存储"

    # 详细检查临时目录状态
    log_debug "详细检查临时目录:"
    if [ -d "$MODULE_TEMP_DIR" ]; then
        log_debug "临时目录存在，内容如下:"
        ls -la "$MODULE_TEMP_DIR" 2>/dev/null | while read line; do
            log_debug "  $line"
        done
    else
        log_debug "错误：临时目录不存在: $MODULE_TEMP_DIR"
        return 1
    fi

    # 检查目标目录是否存在（不创建，假设已存在）
    log_debug "检查目标目录状态..."
    if [ -d "$SYSTEM_BASELINE_DIR" ]; then
        log_debug "SYSTEM_BASELINE_DIR 存在: $SYSTEM_BASELINE_DIR"
        log_debug "目录权限: $(ls -ld "$SYSTEM_BASELINE_DIR" 2>/dev/null || echo "无法获取")"
    else
        log_debug "错误：SYSTEM_BASELINE_DIR 不存在: $SYSTEM_BASELINE_DIR"
        log_debug "检查父目录: $(ls -ld "$APP_DATA_DIR" 2>/dev/null || echo "父目录不存在")"
        return 1
    fi

    local moved_count=0

    # 移动配置文件
    log_debug "开始复制配置文件..."

    if [ -f "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" ]; then
        log_debug "源文件存在: $MODULE_TEMP_DIR/$APP_FEATURES_FILE"
        log_debug "源文件大小: $(stat -c%s "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" 2>/dev/null || echo "unknown")"

        cp "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" "$SYSTEM_BASELINE_DIR/" 2>/dev/null
        local cp_result=$?
        log_debug "cp命令返回值: $cp_result"

        if [ -f "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" ]; then
            log_info "成功移动: $APP_FEATURES_FILE"
            log_debug "目标文件大小: $(stat -c%s "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" 2>/dev/null || echo "unknown")"
            moved_count=$((moved_count + 1))
        else
            log_info "移动失败: $APP_FEATURES_FILE (cp返回值: $cp_result)"
            log_debug "目标目录权限: $(ls -ld "$SYSTEM_BASELINE_DIR" 2>/dev/null || echo "unknown")"
        fi
    else
        log_info "临时文件不存在: $APP_FEATURES_FILE"
        log_debug "检查临时目录: $(ls -la "$MODULE_TEMP_DIR" 2>/dev/null || echo "目录不存在")"
    fi

    if [ -f "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" ]; then
        log_debug "源文件存在: $MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE"
        log_debug "源文件大小: $(stat -c%s "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null || echo "unknown")"

        cp "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" "$SYSTEM_BASELINE_DIR/" 2>/dev/null
        local cp_result=$?
        log_debug "cp命令返回值: $cp_result"

        if [ -f "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" ]; then
            log_info "成功移动: $OPLUS_FEATURES_FILE"
            log_debug "目标文件大小: $(stat -c%s "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null || echo "unknown")"
            moved_count=$((moved_count + 1))
        else
            log_info "移动失败: $OPLUS_FEATURES_FILE (cp返回值: $cp_result)"
            log_debug "目标目录权限: $(ls -ld "$SYSTEM_BASELINE_DIR" 2>/dev/null || echo "unknown")"
        fi
    else
        log_info "临时文件不存在: $OPLUS_FEATURES_FILE"
        log_debug "检查临时目录: $(ls -la "$MODULE_TEMP_DIR" 2>/dev/null || echo "目录不存在")"
    fi

    # 移动时间戳文件
    if [ -f "$MODULE_TEMP_DIR/last_copy.txt" ]; then
        cp "$MODULE_TEMP_DIR/last_copy.txt" "$SYSTEM_BASELINE_DIR/" 2>/dev/null
        local cp_result=$?
        if [ $cp_result -eq 0 ]; then
            log_debug "时间戳文件复制成功"
        else
            log_debug "时间戳文件复制失败，返回值: $cp_result"
        fi
    fi

    # 检查并设置文件权限（仅针对复制的文件）
    log_debug "检查复制文件的权限..."
    if [ -f "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" ]; then
        chmod 644 "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" 2>/dev/null
        log_debug "设置 $APP_FEATURES_FILE 权限"
    fi

    if [ -f "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" ]; then
        chmod 644 "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null
        log_debug "设置 $OPLUS_FEATURES_FILE 权限"
    fi

    # 最终检查结果
    log_debug "最终目录状态:"
    if [ -d "$SYSTEM_BASELINE_DIR" ]; then
        log_debug "SYSTEM_BASELINE_DIR 最终状态:"
        ls -la "$SYSTEM_BASELINE_DIR" 2>/dev/null | while read line; do
            log_debug "  $line"
        done
    fi

    log_info "配置文件移动完成，成功移动 $moved_count 个文件"
}

# 清理临时文件
cleanup_temp() {
    if [ -d "$MODULE_TEMP_DIR" ]; then
        rm -rf "$MODULE_TEMP_DIR"
        log_info "清理临时目录完成"
    fi
}

# 执行主流程
main() {
    log_debug "主流程开始执行"

    # 检查临时目录是否存在配置文件
    log_debug "检查临时目录状态..."
    if [ ! -d "$MODULE_TEMP_DIR" ]; then
        log_info "临时目录不存在，跳过移动操作: $MODULE_TEMP_DIR"
        return 0
    fi

    if [ ! -f "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" ] && [ ! -f "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" ]; then
        log_info "临时目录中没有配置文件，跳过移动操作"
        log_debug "临时目录内容: $(ls -la "$MODULE_TEMP_DIR" 2>/dev/null || echo "无法列出")"
        return 0
    fi

    # 等待系统启动完成（使用用户方案）
    log_debug "等待系统启动完成..."
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        sleep 3
    done
    log_info "系统启动完成"

    # 等待系统启动完成（使用系统方案）
    wait_for_boot_complete

    # 移动配置文件
    move_temp_to_app
    
    # 清理临时文件
    cleanup_temp
    log_debug "跳过清理临时文件"

    log_info "=== Service脚本执行完成 ==="
}

# 在后台执行主流程
main &
