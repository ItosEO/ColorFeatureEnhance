#!/system/bin/sh
# ColorOS Features Enhance - Copy脚本（简化版）
# 等待用户解锁后，将临时配置复制到app内部存储
MODDIR=${0%/*}

# 应用数据目录
APP_DATA_DIR="/storage/emulated/0/Android/data/com.itosfish.colorfeatureenhance/files/configs"
SYSTEM_BASELINE_DIR="/storage/emulated/0/Android/data/com.itosfish.colorfeatureenhance/files/configs/system_baseline"

# 模块临时目录（使用新版路径）
MODULE_TEMP_DIR="/data/adb/cos_feat_e/temp_configs"

# 配置文件名
APP_FEATURES_FILE="com.oplus.app-features.xml"
OPLUS_FEATURES_FILE="com.oplus.oplus-feature.xml"

# 日志函数
log_info() {
    echo "[ColorFeatureEnhance-Copy] $(date '+%Y-%m-%d %H:%M:%S') $1" >> /cache/colorfeature_enhance.log
    echo "[ColorFeatureEnhance-Copy] $1"
}

log_debug() {
    echo "[ColorFeatureEnhance-Copy-DEBUG] $(date '+%Y-%m-%d %H:%M:%S') $1" >> /cache/colorfeature_enhance.log
}

log_info "=== Copy脚本启动 ==="
log_debug "MODDIR: $MODDIR"
log_debug "APP_DATA_DIR: $APP_DATA_DIR"
log_debug "SYSTEM_BASELINE_DIR: $SYSTEM_BASELINE_DIR"
log_debug "MODULE_TEMP_DIR: $MODULE_TEMP_DIR"

# 复制临时配置到app内部存储
copy_temp_to_app() {
    log_info "开始复制临时配置到app存储"

    # 检查临时目录
    if [ ! -d "$MODULE_TEMP_DIR" ]; then
        log_info "临时目录不存在，跳过复制: $MODULE_TEMP_DIR"
        return 0
    fi

    log_debug "临时目录内容:"
    ls -la "$MODULE_TEMP_DIR" 2>/dev/null | while read line; do
        log_debug "  $line"
    done

    # 检查目标目录
    if [ ! -d "$SYSTEM_BASELINE_DIR" ]; then
        log_info "目标目录不存在，跳过复制: $SYSTEM_BASELINE_DIR"
        return 0
    fi

    local copied_count=0

    # 复制 app-features.xml
    if [ -f "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" ]; then
        log_debug "复制文件: $APP_FEATURES_FILE"
        cp "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" "$SYSTEM_BASELINE_DIR/" 2>/dev/null
        if [ -f "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" ]; then
            log_info "成功复制: $APP_FEATURES_FILE"
            chmod 660 "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" 2>/dev/null
            chown "${1}:ext_data_rw" "$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE" 2>/dev/null
            copied_count=$((copied_count + 1))
        else
            log_info "复制失败: $APP_FEATURES_FILE"
        fi
    else
        log_debug "临时文件不存在: $APP_FEATURES_FILE"
    fi

    # 复制 oplus-feature.xml
    if [ -f "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" ]; then
        log_debug "复制文件: $OPLUS_FEATURES_FILE"
        cp "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" "$SYSTEM_BASELINE_DIR/" 2>/dev/null
        if [ -f "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" ]; then
            log_info "成功复制: $OPLUS_FEATURES_FILE"
            chmod 660 "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null
            chown "${1}:ext_data_rw" "$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null
            copied_count=$((copied_count + 1))
        else
            log_info "复制失败: $OPLUS_FEATURES_FILE"
        fi
    else
        log_debug "临时文件不存在: $OPLUS_FEATURES_FILE"
    fi

    # 复制时间戳文件
    # if [ -f "$MODULE_TEMP_DIR/last_copy.txt" ]; then
    #     cp "$MODULE_TEMP_DIR/last_copy.txt" "$SYSTEM_BASELINE_DIR/" 2>/dev/null
    #     chmod 660 "$SYSTEM_BASELINE_DIR/last_copy.txt" 2>/dev/null
    #     chown "${1}:ext_data_rw" "$SYSTEM_BASELINE_DIR/last_copy.txt" 2>/dev/null
    #     log_debug "时间戳文件复制完成"
    # fi

    # 最终检查
    log_debug "最终目录状态:"
    if [ -d "$SYSTEM_BASELINE_DIR" ]; then
        ls -la "$SYSTEM_BASELINE_DIR" 2>/dev/null | while read line; do
            log_debug "  $line"
        done
    fi

    log_info "配置文件复制完成，成功复制 $copied_count 个文件"
}

# 执行主流程
main() {
    log_debug "主流程开始执行"

    # 检查是否传入应用 UID 参数
    if [ -z "$1" ]; then
        log_info "未传入应用 UID 参数，跳过操作"
        return 0
    fi

    # 检查临时目录是否有配置文件
    if [ ! -d "$MODULE_TEMP_DIR" ]; then
        log_info "临时目录不存在，跳过操作: $MODULE_TEMP_DIR"
        return 0
    fi

    if [ ! -f "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" ] && [ ! -f "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" ]; then
        log_info "临时目录中没有配置文件，跳过操作"
        return 0
    fi

    # 复制配置文件
    copy_temp_to_app $1

    log_info "=== Copy脚本执行完成 ==="
}

# 执行主流程
main "$@"
