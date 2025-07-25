#!/system/bin/sh
# ColorOS Features Enhance - 优化配置管理模块
# post-fs-data阶段：复制系统配置到临时目录，直接挂载
MODDIR=${0%/*}

# 系统配置源路径
SYSTEM_CONFIG_DIR="/my_product/etc/extension"
APP_FEATURES_FILE="com.oplus.app-features.xml"
OPLUS_FEATURES_FILE="com.oplus.oplus-feature.xml"

# 模块目录
MODULE_CONFIG_DIR="/data/adb/cos_feat_e/my_product/etc/extension"
MODULE_ANYMOUNT_DIR="/data/adb/cos_feat_e/anymount/my_product/etc/extension"
MODULE_TEMP_DIR="/data/adb/cos_feat_e/temp_configs"

# 日志函数
log_info() {
    echo "[ColorFeatureEnhance] $(date '+%Y-%m-%d %H:%M:%S') $1" >> /cache/colorfeature_enhance.log
}

log_debug() {
    echo "[ColorFeatureEnhance-DEBUG] $(date '+%Y-%m-%d %H:%M:%S') $1" >> /cache/colorfeature_enhance.log
}

# 创建必要目录
mkdir -p "$MODULE_CONFIG_DIR"
mkdir -p "$MODULE_ANYMOUNT_DIR"
mkdir -p "$MODULE_TEMP_DIR"

log_info "=== 配置管理模块启动（post-fs-data阶段）==="
log_debug "MODDIR: $MODDIR"
log_debug "SYSTEM_CONFIG_DIR: $SYSTEM_CONFIG_DIR"
log_debug "MODULE_CONFIG_DIR: $MODULE_CONFIG_DIR"
log_debug "MODULE_ANYMOUNT_DIR: $MODULE_ANYMOUNT_DIR"
log_debug "MODULE_TEMP_DIR: $MODULE_TEMP_DIR"

# 1. 复制系统配置到模块临时目录（供service脚本使用）
copy_system_to_temp() {
    log_debug "开始复制系统配置到临时目录"

    # 检查系统配置目录
    if [ ! -d "$SYSTEM_CONFIG_DIR" ]; then
        log_debug "系统配置目录不存在: $SYSTEM_CONFIG_DIR"
        return 1
    fi

    local copied_count=0

    # 复制 app-features.xml
    if [ -f "$SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE" ]; then
        log_debug "源文件存在: $SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE"
        log_debug "源文件大小: $(stat -c%s "$SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE" 2>/dev/null || echo "unknown")"

        cp "$SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE" "$MODULE_TEMP_DIR/" 2>/dev/null
        local cp_result=$?
        log_debug "cp命令返回值: $cp_result"

        if [ -f "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" ]; then
            log_info "复制系统配置到临时目录: $APP_FEATURES_FILE"
            log_debug "目标文件大小: $(stat -c%s "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" 2>/dev/null || echo "unknown")"
            copied_count=$((copied_count + 1))
        else
            log_debug "复制失败: $APP_FEATURES_FILE (cp返回值: $cp_result)"
        fi
    else
        log_debug "源文件不存在: $SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE"
    fi

    # 复制 oplus-feature.xml
    if [ -f "$SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE" ]; then
        log_debug "源文件存在: $SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE"
        log_debug "源文件大小: $(stat -c%s "$SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null || echo "unknown")"

        cp "$SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE" "$MODULE_TEMP_DIR/" 2>/dev/null
        local cp_result=$?
        log_debug "cp命令返回值: $cp_result"

        if [ -f "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" ]; then
            log_info "复制系统配置到临时目录: $OPLUS_FEATURES_FILE"
            log_debug "目标文件大小: $(stat -c%s "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" 2>/dev/null || echo "unknown")"
            copied_count=$((copied_count + 1))
        else
            log_debug "复制失败: $OPLUS_FEATURES_FILE (cp返回值: $cp_result)"
        fi
    else
        log_debug "源文件不存在: $SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE"
    fi

    # 记录复制时间戳
    date "+%Y-%m-%d %H:%M:%S" > "$MODULE_TEMP_DIR/last_copy.txt"

    # 检查临时目录最终状态
    log_debug "临时目录最终状态:"
    ls -la "$MODULE_TEMP_DIR" 2>/dev/null | while read line; do
        log_debug "  $line"
    done

    log_info "系统配置复制到临时目录完成，成功复制 $copied_count 个文件"
}

# 2. 确保模块目录有默认配置（如果不存在）
ensure_default_configs() {
    # 如果模块配置目录为空，使用系统配置作为默认
    if [ ! -f "$MODULE_CONFIG_DIR/$APP_FEATURES_FILE" ] && [ -f "$SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE" ]; then
        cp "$SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE" "$MODULE_CONFIG_DIR/"
        cp "$SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE" "$MODULE_ANYMOUNT_DIR/"
        log_info "使用系统配置作为默认: $APP_FEATURES_FILE"
    fi

    if [ ! -f "$MODULE_CONFIG_DIR/$OPLUS_FEATURES_FILE" ] && [ -f "$SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE" ]; then
        cp "$SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE" "$MODULE_CONFIG_DIR/"
        cp "$SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE" "$MODULE_ANYMOUNT_DIR/"
        log_info "使用系统配置作为默认: $OPLUS_FEATURES_FILE"
    fi
}

# 执行配置管理流程
copy_system_to_temp
ensure_default_configs

# 3. 执行挂载逻辑
log_info "开始挂载配置文件"
mount --bind $MODULE_CONFIG_DIR/$APP_FEATURES_FILE $SYSTEM_CONFIG_DIR/$APP_FEATURES_FILE
mount --bind $MODULE_CONFIG_DIR/$OPLUS_FEATURES_FILE $SYSTEM_CONFIG_DIR/$OPLUS_FEATURES_FILE

# 挂载any目录下的其他文件
# TMPDIR=${0%/*}/anymount
# if [ -d "$TMPDIR" ]; then
    # for i in `/bin/find $TMPDIR -type f -printf "%P "`; do
        # mount /$TMPDIR/$i /$i
        # log_info $i
        # restorecon /$i
    # done
# fi

log_info "=== post-fs-data阶段完成 ==="
