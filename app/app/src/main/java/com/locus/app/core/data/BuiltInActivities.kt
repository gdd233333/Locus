package com.locus.app.core.data

import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory

/** 内置活动清单：Room 预置数据与 Fake 仓库共用的唯一来源 */
object BuiltInActivities {

    val ALL: List<Activity> = listOf(
        Activity(1, "冷水洗脸 + 深呼吸", "物理打断当前状态，让大脑从冲动中抽离。冷水刺激迷走神经，快速降低唤醒水平。", 5, ActivityCategory.EMERGENCY, listOf("冲动急救")),
        Activity(2, "做 20 个俯卧撑", "立刻，就在原地。心率上来，冲动下去。身体是最诚实的开关。", 5, ActivityCategory.EMERGENCY, listOf("冲动急救")),
        Activity(3, "出门快走一圈", "不带手机，只带钥匙。让身体动起来，让视线离开屏幕。夜风是最好的清醒剂。", 20, ActivityCategory.OUTDOOR, listOf("户外")),
        Activity(4, "泡一杯热茶", "双手捧杯，感受温度从掌心传到手臂。什么都不想，就看着热气往上飘。", 5, ActivityCategory.EMERGENCY, listOf("感官唤醒")),
        Activity(5, "整理书桌一角", "不需要收拾整个房间，就整理你伸手可及的那一块。环境清爽了，心也会跟着清爽一点。", 15, ActivityCategory.ENVIRONMENT, listOf("环境整理")),
        Activity(6, "读 20 页书", "纸质书优先。如果读不进去，就从最薄的那本开始。读不下去也是正常的，翻页即是胜利。", 45, ActivityCategory.INPUT, listOf("输入")),
        Activity(7, "写一页手账", "不用写得多好，就写今天最强烈的一个感受。写下来的东西，就不会再在心里翻腾了。", 30, ActivityCategory.EXPRESSION, listOf("表达")),
        Activity(8, "画一幅烂画", "不需要好看，需要动手。纸和笔就行，画你此刻脑子里最混乱的那个画面。画完撕掉也行。", 25, ActivityCategory.CREATIVE, listOf("创作")),
        Activity(9, "听一首完整的歌", "不是背景音，是认真听。戴上耳机，闭上眼睛，跟着节奏走。一首歌结束，你已经换了一个状态。", 15, ActivityCategory.EMERGENCY, listOf("感官切换")),
        Activity(10, "拉伸 5 分钟", "床上就能做，放松肌肉也放松神经。", 5, ActivityCategory.QUICK, listOf("身体")),
        Activity(11, "洗一个苹果慢慢吃", "专注在味道和口感上。", 10, ActivityCategory.QUICK, listOf("感官唤醒")),
        Activity(12, "给朋友发一条消息", "不用说什么重要的，就打个招呼。", 5, ActivityCategory.EXPRESSION, listOf("表达")),
        Activity(13, "做 10 分钟冥想", "跟着呼吸走，走神了就拉回来。", 10, ActivityCategory.EMERGENCY, listOf("冥想")),
        Activity(14, "打扫房间地面", "扫地或拖地，让地面反光。", 20, ActivityCategory.ENVIRONMENT, listOf("环境整理")),
        Activity(15, "写日记三行", "今天做了什么，感受如何，明天想做什么。", 10, ActivityCategory.EXPRESSION, listOf("表达")),
        Activity(16, "看一篇长文", "收藏夹里吃灰的那篇，现在就看。", 20, ActivityCategory.INPUT, listOf("输入")),
        Activity(17, "学一个魔术", "硬币、纸牌都行，B 站搜教程。", 30, ActivityCategory.CREATIVE, listOf("创作")),
        Activity(18, "下楼买一瓶水", "就走出去，哪怕只是为了买水。", 15, ActivityCategory.OUTDOOR, listOf("户外")),
        Activity(19, "整理手机相册", "删截图，删废片，留下真正想留的。", 20, ActivityCategory.ENVIRONMENT, listOf("环境整理")),
        Activity(20, "做一道简单的菜", "煎蛋、煮面都行，重点是从头到尾做完。", 40, ActivityCategory.CREATIVE, listOf("创作")),
    )
}
