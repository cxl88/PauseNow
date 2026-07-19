package com.pausenow.app.pass

/**
 * 使用目的（docs/09 R-004）。新通行必须含一个目的；UNSPECIFIED_LEGACY 仅供旧数据迁移，不得由新版 UI 创建。
 */
enum class PassPurpose(val label: String) {
    FIND_SPECIFIC_CONTENT("找一个明确内容"),
    HANDLE_ONE_TASK("处理一件事"),
    RELAX_BRIEFLY("放松一下"),
    NO_CLEAR_PURPOSE("没有明确目的"),
    UNSPECIFIED_LEGACY("旧版本未记录"),
}
