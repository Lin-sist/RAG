"""Second-round editable design boards; all timings and content are simulated.

Uses the existing SVG primitives and existing local PyMuPDF renderer only.
This does not implement the application or capture the Harness conversation.
"""
from render_wireframes import board, rect, txt, line, dot, save

TOTAL = 4.82
OPS = [
    ('工具', '检查回答缓存', '确认是否已有可复用的回答', 0, .03),
    ('工具', '生成查询向量', '将问题转换成用于检索的向量', .03, .15),
    ('工具', '向量召回', '按语义相似度查找候选片段', .15, .44),
    ('工具', '关键词召回', '补充名称与精确匹配的线索', .44, .60),
    ('工具', '融合检索结果', '合并两路召回并消除重复候选', .60, .65),
    ('工具', '重排候选片段', '按相关程度保留 5 个回答依据', .65, .85),
    ('上下文', '组织回答依据', '将已选资料整理为本次输入依据', .85, .95),
    ('助手', '生成回答', '结合本次资料生成带引用的回答', .95, 4.53),
    ('工具', '校验引用', '检查引用与已选资料的对应关系', 4.53, 4.82),
]


def palette(dark):
    return ({'工具': '#E2A653', '助手': '#B19AE7', '上下文': '#59BCA9'},
            {'工具': '#382D1D', '助手': '#2E273E', '上下文': '#19362F'}) if dark else (
        {'工具': '#985E14', '助手': '#7754AF', '上下文': '#127663'},
        {'工具': '#F9EDDB', '助手': '#EEE7F7', '上下文': '#E0F2EB'})


def overview(a, p, colors, x, y, w, selected=None, brushed=False):
    rect(a, x, y, w, 146, p['surface'], 10, p['line'])
    txt(a, p, x+18, y+24, '执行分布', 12)
    start, span = x+88, w-114
    for t in [0, 1.2, 2.4, 3.6, TOTAL]:
        px = start + span*t/TOTAL
        txt(a, p, px, y+42, '0' if t == 0 else f'{t:g} s', 11, p['muted'], anchor='end' if t == TOTAL else 'start')
        line(a, p, px, y+48, px, y+117)
    for i, typ in enumerate(['助手', '工具', '上下文']):
        txt(a, p, x+18, y+65+i*25, typ, 12, colors[typ])
    if brushed:
        rect(a, start+span*.6/TOTAL, y+47, span*.6/TOTAL, 72, '#24453F', 0, p['accent'])
    for i, (typ, _, _, st, en) in enumerate(OPS):
        py = y+55+{'助手': 0, '工具': 25, '上下文': 50}[typ]
        px, pw = start+span*st/TOTAL, span*(en-st)/TOTAL
        rect(a, px, py, pw, 10, colors[typ], 2)
        if i == selected:
            rect(a, px-2, py-3, pw+4, 16, 'none', 3, p['ink'])
    txt(a, p, x+18, y+134, '0.60 — 1.20 s · 已筛选上下文' if brushed else '全部时间 · 9 项操作', 11, p['muted'])
    if w > 600:
        txt(a, p, x+w-18, y+134, '横向拖选范围 · 点选操作查看详情', 11, p['muted'], anchor='end')


def desktop(dark=False):
    a, p = board(1440, 960, dark)
    if dark:
        p.update(bg='#101111', surface='#1B1D1D', sidebar='#151616', line='#303535', muted='#A4ABAA')
        a[-1] = '<rect width="100%" height="100%" fill="#101111"/>'
    colors, fills = palette(dark)
    rect(a, 0, 0, 1440, 32, p['surface'])
    txt(a, p, 20, 22, '第二版 / 三色类型、调用说明与滑动选区', 12, p['muted'])
    txt(a, p, 1420, 22, '静态设计稿 · 模拟数据 · 正式链路未接入', 12, p['muted'], anchor='end')
    rect(a, 0, 32, 216, 928, p['sidebar'])
    txt(a, p, 24, 88, 'RAG', 23, weight=650)
    rect(a, 16, 119, 184, 40, p['bg'], 8, p['line'])
    txt(a, p, 30, 145, '＋ 新问答')
    txt(a, p, 24, 220, '知识库', 14, p['muted'])
    txt(a, p, 24, 268, '历史记录', 14, p['muted'])
    txt(a, p, 24, 341, '当前页面', 12, p['muted'])
    txt(a, p, 24, 376, '检索与引用的处理过程', 13)
    txt(a, p, 280, 82, '对话', 14, p['muted'])
    txt(a, p, 346, 82, '轨迹', 14, weight=600)
    rect(a, 346, 93, 28, 2, p['accent'], 1)
    txt(a, p, 280, 144, '系统如何检索资料，并让回答有据可查？', 20, weight=550)
    dot(a, 284, 177, p['accent'])
    txt(a, p, 297, 181, '已完成  ·  9 项操作', 12, p['muted'])
    txt(a, p, 1360, 145, '服务端执行', 12, p['muted'], anchor='end')
    txt(a, p, 1360, 184, '4.82 s', 30, weight=500, anchor='end')
    overview(a, p, colors, 280, 218, 1080, selected=5 if dark else None)
    txt(a, p, 280, 398, '全部', 12)
    for x, typ in [(332, '工具'), (389, '助手'), (447, '上下文')]:
        txt(a, p, x, 398, typ, 12, colors[typ])
    right = 980 if dark else 1360
    txt(a, p, right, 398, '全部阶段 ▾', 12, p['muted'], anchor='end')
    for i, (typ, name, desc, st, en) in enumerate(OPS):
        y = 420+i*47
        if dark and i == 5:
            rect(a, 273, y, right-273+12, 47, '#242827', 5)
            rect(a, 273, y+7, 3, 33, colors[typ], 1)
        line(a, p, 280, y, right, y)
        rect(a, 280, y+12, 46, 22, fills[typ], 4)
        txt(a, p, 303, y+27, typ, 11, colors[typ], anchor='middle')
        txt(a, p, 344, y+20, name, 14, weight=500)
        txt(a, p, 344, y+38, desc, 12, p['muted'])
        dur = f'{(en-st)*1000:.0f} ms' if en-st < 1 else f'{en-st:.2f} s'
        txt(a, p, right, y+21, dur, 12, p['muted'], anchor='end')
        txt(a, p, right, y+38, '✓ 完成', 10, p['muted'], anchor='end')
    if dark:
        rect(a, 1012, 385, 348, 427, p['bg'], 10, p['line'])
        rect(a, 1034, 406, 40, 22, fills['工具'], 4)
        txt(a, p, 1054, 422, '工具', 11, colors['工具'], anchor='middle')
        txt(a, p, 1090, 422, '操作 6 / 9', 11, p['muted'])
        txt(a, p, 1338, 424, '×', 20, p['muted'], anchor='end')
        txt(a, p, 1034, 465, '重排候选片段', 19, weight=550)
        txt(a, p, 1034, 494, '按相关程度保留 5 个回答依据。', 13, p['muted'])
        for x, t in [(1034, '概览'), (1095, '参数'), (1156, '结果')]:
            txt(a, p, x, 533, t, 12, p['ink'] if t == '概览' else p['muted'])
        line(a, p, 1034, 545, 1338, 545)
        rect(a, 1034, 544, 24, 2, colors['工具'])
        for y, k, v in [(578, '状态', '已完成'), (616, '耗时', '200 ms'), (654, '生效方式', '规则重排'), (692, '保留片段', '5 个'), (730, '模型调用', '0 次')]:
            txt(a, p, 1034, y, k, 12, p['muted'])
            txt(a, p, 1338, y, v, 12, anchor='end')
        line(a, p, 1034, 756, 1338, 756)
        txt(a, p, 1034, 788, '技术信息', 12, p['muted'])
        txt(a, p, 1338, 788, '＋', 14, p['muted'], anchor='end')
    rect(a, 350, 869, 940, 57, p['surface'], 18, p['line'])
    txt(a, p, 370, 893, '继续输入问题…（设计稿，不发送）', 13, p['muted'])
    txt(a, p, 370, 914, '＋  知识库范围', 11, p['muted'])
    txt(a, p, 820, 948, '全部数据为模拟示例，仅用于审阅布局与交互。', 11, p['muted'], anchor='middle')
    save(a, 'v2-tool-dark' if dark else 'v2-overview-light')


def mobile():
    a, p = board(390, 844, True)
    colors, fills = palette(True)
    txt(a, p, 18, 24, '第二版 · 窄屏设计 · 模拟数据', 11, p['muted'])
    txt(a, p, 20, 70, '对话', 14, p['muted'])
    txt(a, p, 82, 70, '轨迹', 14)
    rect(a, 82, 81, 28, 2, p['accent'])
    txt(a, p, 20, 123, '系统如何检索资料并生成回答？', 17, weight=500)
    txt(a, p, 20, 160, '已完成 · 9 项操作', 12, p['muted'])
    txt(a, p, 370, 160, '4.82 s', 24, anchor='end')
    overview(a, p, colors, 16, 186, 358, brushed=True)
    txt(a, p, 20, 369, '全部', 12, p['muted'])
    txt(a, p, 78, 369, '工具', 12, colors['工具'])
    txt(a, p, 132, 369, '助手', 12, colors['助手'])
    txt(a, p, 185, 369, '上下文', 12, colors['上下文'])
    line(a, p, 20, 385, 370, 385)
    rect(a, 20, 404, 46, 22, fills['上下文'], 4)
    txt(a, p, 43, 420, '上下文', 11, colors['上下文'], anchor='middle')
    txt(a, p, 80, 415, '组织回答依据', 14)
    txt(a, p, 80, 436, '整理本次已授权的资料片段', 12, p['muted'])
    txt(a, p, 370, 415, '100 ms', 12, p['muted'], anchor='end')
    rect(a, 0, 483, 390, 361, p['surface'], 16, p['line'])
    txt(a, p, 22, 523, '组织回答依据', 19, weight=550)
    txt(a, p, 366, 524, '×', 22, p['muted'], anchor='end')
    txt(a, p, 22, 560, '概览', 13, p['muted'])
    txt(a, p, 88, 560, '来源', 13, colors['上下文'])
    line(a, p, 22, 577, 368, 577)
    txt(a, p, 22, 615, '资料 A · 检索流程', 14)
    txt(a, p, 22, 640, '本次已授权的选中来源（模拟）', 12, p['muted'])
    line(a, p, 22, 661, 368, 661)
    txt(a, p, 22, 697, '资料 B · 引用规则', 14)
    txt(a, p, 22, 722, '本次已授权的选中来源（模拟）', 12, p['muted'])
    txt(a, p, 22, 784, '来源正文复用本次授权数据，不从轨迹泄露。', 11, p['muted'])
    txt(a, p, 22, 817, '静态设计稿，交互另看 trace-v2.html。', 11, p['muted'])
    save(a, 'v2-context-mobile')


if __name__ == '__main__':
    desktop()
    desktop(True)
    mobile()
