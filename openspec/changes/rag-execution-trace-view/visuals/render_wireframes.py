"""Render planning-only SVG/PNG boards. All data are explicitly simulated.

This file is a design artifact generator, not application implementation.
It uses the existing local PyMuPDF installation and makes no network requests.
"""
from pathlib import Path
from xml.sax.saxutils import escape
import fitz

OUT = Path(__file__).resolve().parent
FONT = 'Microsoft YaHei UI,Microsoft YaHei,Segoe UI,sans-serif'
ROWS = [('准备', 0, .03, '0.03 s'), ('检索', .03, .82, '0.79 s'),
        ('生成', .82, 4.53, '3.71 s'), ('校验', 4.53, 4.82, '0.29 s')]


def board(w, h, dark=False):
    p = dict(bg='#000000' if dark else '#FFFFFF',
             surface='#161616' if dark else '#F9F9F9',
             sidebar='#0A0A0A' if dark else '#F9F9F9',
             ink='#ECECEC' if dark else '#0D0D0D',
             muted='#B5B5B5' if dark else '#5D5D5D',
             line='#303030' if dark else '#E5E5E5',
             bar='#747474' if dark else '#858D88', accent='#10A37F',
             amber='#E4AF5A' if dark else '#976414')
    a = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">',
         '<title>RAG 轨迹界面静态设计稿，全部为模拟数据</title>',
         '<desc>非运行界面，不代表真实模型或阶段测量。</desc>',
         f'<rect width="100%" height="100%" fill="{p["bg"]}"/>']
    return a, p


def rect(a, x, y, w, h, fill, rx=0, stroke='none'):
    a.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"/>')


def txt(a, p, x, y, s, size=14, color=None, weight=400, anchor='start'):
    a.append(f'<text x="{x}" y="{y}" fill="{color or p["ink"]}" font-family="{FONT}" font-size="{size}" font-weight="{weight}" text-anchor="{anchor}">{escape(s)}</text>')


def line(a, p, x1, y1, x2, y2, color=None):
    a.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color or p["line"]}"/>')


def dot(a, x, y, color, r=3):
    a.append(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{color}"/>')


def save(a, name):
    svg = '\n'.join(a + ['</svg>'])
    (OUT / f'{name}.svg').write_text(svg, encoding='utf-8')
    with fitz.open(stream=svg.encode(), filetype='svg') as doc:
        with fitz.open('pdf', doc.convert_to_pdf()) as pdf:
            pdf[0].get_pixmap(matrix=fitz.Matrix(1, 1), alpha=False).save(str(OUT / f'{name}.png'))
            print(f'{name}: {pdf[0].rect.width:.0f}x{pdf[0].rect.height:.0f}')


def sidebar(a, p, w, h):
    rect(a, 0, 40, w, h-40, p['sidebar'])
    txt(a, p, 28, 100, 'RAG', 23, weight=650)
    rect(a, 16, 130, w-32, 40, p['bg'], 8, p['line'])
    txt(a, p, 32, 156, '＋  新问答')
    for y, t in [(230, '知识库'), (278, '历史记录')]:
        txt(a, p, 28, y, t, 14, p['muted'])
    line(a, p, 24, 326, w-24, 326)
    txt(a, p, 28, 367, '当前页面', 12, p['muted'])
    txt(a, p, 28, 401, '检索降级的处理', 13)
    dot(a, 32, h-42, p['bar'], 11)
    txt(a, p, 56, h-37, '个人空间', 13, p['muted'])


def tabs(a, p, x, y, right):
    txt(a, p, x, y, '对话', 15, p['muted'])
    txt(a, p, x+68, y, '轨迹', 15, weight=600)
    rect(a, x+68, y+11, 29, 2, p['accent'], 1)
    txt(a, p, right, y, '本次问答  ⌄', 13, p['muted'], anchor='end')


def composer(a, p, x, y, w):
    rect(a, x, y, w, 66, p['surface'], 22, p['line'])
    txt(a, p, x+22, y+28, '继续输入问题…', 14, p['muted'])
    txt(a, p, x+22, y+50, '＋', 18, p['muted'])
    dot(a, x+w-30, y+34, p['bar'], 14)
    txt(a, p, x+w-30, y+39, '↑', 17, p['bg'], anchor='middle')


def timeline(a, p, label_x, state_x, start, width, value_x, top, selected=False, mobile=False):
    for t in ([0, 2, 4.82] if selected or mobile else [0, 1, 2, 3, 4.82]):
        x = start + width*t/4.82
        label = '0' if t == 0 else f'{t} s'
        txt(a, p, x, top-50, label, 12, p['muted'], anchor='middle')
        line(a, p, x, top-32, x, top+161, '#202020' if selected else '#F0F0F0')
    for i, (name, st, en, dur) in enumerate(ROWS):
        y = top + i*48
        if selected and i == 1:
            rect(a, label_x-16, y-24, value_x-label_x+33, 48, '#11231E', 8)
            rect(a, label_x-16, y-24, 3, 48, p['accent'], 1)
        txt(a, p, label_x, y+5, name, 14 if mobile else 15, weight=600 if selected and i == 1 else 400)
        txt(a, p, state_x, y+5, '!' if selected and i == 1 else '✓', 12 if mobile else 13,
            p['amber'] if selected and i == 1 else p['muted'])
        rect(a, start+width*st/4.82, y-3, max(1, width*(en-st)/4.82), 6,
             p['accent'] if selected and i == 1 else p['bar'], 3)
        txt(a, p, value_x, y+5, dur, 12 if mobile else 13, p['muted'], anchor='end')


def overview():
    a, p = board(1440, 900)
    rect(a, 0, 0, 1440, 40, p['surface'])
    txt(a, p, 24, 26, '01 / 默认总览', 12, p['muted'], 600)
    txt(a, p, 1416, 26, '静态设计稿 · 模拟数据 · 不代表真实链路', 12, p['muted'], anchor='end')
    sidebar(a, p, 232, 900)
    tabs(a, p, 324, 106, 1336)
    txt(a, p, 324, 180, '这套系统在检索失败时如何继续回答？', 17, weight=500)
    dot(a, 328, 241, p['accent'], 4)
    txt(a, p, 342, 249, '已完成', 24, weight=550)
    txt(a, p, 1336, 226, '服务端执行', 12, p['muted'], anchor='end')
    txt(a, p, 1336, 262, '4.82 s', 32, weight=500, anchor='end')
    line(a, p, 324, 295, 1336, 295)
    timeline(a, p, 324, 445, 520, 692, 1336, 390)
    line(a, p, 324, 571, 1336, 571)
    txt(a, p, 324, 603, '选择一个阶段，查看细节。', 13, p['muted'])
    composer(a, p, 432, 756, 820)
    txt(a, p, 842, 855, 'AI 可能产生不准确内容，请核对引用来源', 12, p['muted'], anchor='middle')
    save(a, '01-overview-light')


def detail():
    a, p = board(1600, 960, True)
    rect(a, 0, 0, 1600, 40, p['surface'])
    txt(a, p, 24, 26, '02 / 选中后再看细节', 12, p['muted'], 600)
    txt(a, p, 1576, 26, '静态设计稿 · 模拟降级场景 · 非实测数据', 12, p['muted'], anchor='end')
    sidebar(a, p, 224, 960)
    tabs(a, p, 300, 106, 1520)
    txt(a, p, 300, 180, '这套系统在检索失败时如何继续回答？', 17, weight=500)
    dot(a, 304, 241, p['accent'], 4)
    txt(a, p, 318, 249, '已完成', 24, weight=550)
    txt(a, p, 440, 247, '1 项降级', 13, p['amber'])
    txt(a, p, 1520, 226, '服务端执行', 12, p['muted'], anchor='end')
    txt(a, p, 1520, 262, '4.82 s', 32, weight=500, anchor='end')
    line(a, p, 300, 295, 1520, 295)
    timeline(a, p, 300, 409, 475, 488, 1052, 396, selected=True)
    line(a, p, 300, 583, 1069, 583)
    txt(a, p, 300, 616, '仅突出需要关注的阶段。', 13, p['muted'])
    line(a, p, 1111, 327, 1111, 796)
    txt(a, p, 1143, 353, '检索', 19, weight=600)
    txt(a, p, 1520, 353, '×', 22, p['muted'], anchor='end')
    txt(a, p, 1143, 386, '已降级 · 0.79 s', 13, p['amber'])
    txt(a, p, 1143, 428, '向量检索暂不可用，')
    txt(a, p, 1143, 453, '已改用关键词检索。')
    line(a, p, 1143, 480, 1520, 480)
    for y, label, value in [(512, '生效路线', '关键词'), (548, '重试', '0 次'), (584, '保留片段', '5 个')]:
        txt(a, p, 1143, y, label, 13, p['muted'])
        txt(a, p, 1520, y, value, 13, anchor='end')
    line(a, p, 1143, 610, 1520, 610)
    txt(a, p, 1143, 643, '子阶段', 12, p['muted'])
    for y, label, value, color in [(679, '向量检索', '失败', p['amber']), (715, '关键词检索', '完成', p['muted'])]:
        txt(a, p, 1143, y, label, 13)
        txt(a, p, 1520, y, value, 12, color, anchor='end')
    line(a, p, 1143, 740, 1520, 740)
    txt(a, p, 1143, 775, '技术信息', 13, p['muted'])
    txt(a, p, 1520, 775, '＋', 16, p['muted'], anchor='end')
    composer(a, p, 472, 850, 820)
    save(a, '02-detail-dark')


def mobile():
    a, p = board(390, 844)
    rect(a, 0, 0, 390, 32, p['surface'])
    txt(a, p, 18, 21, '03 / 窄屏设计 · 模拟数据', 12, p['muted'])
    for y in (57, 63, 69):
        line(a, p, 24, y, 40, y, p['ink'])
    txt(a, p, 195, 71, 'RAG', 18, weight=650, anchor='middle')
    txt(a, p, 370, 71, '＋', 21, p['muted'], anchor='end')
    tabs(a, p, 24, 125, 366)
    txt(a, p, 24, 184, '检索失败时，系统如何继续回答？', 16, weight=500)
    dot(a, 28, 238, p['accent'], 4)
    txt(a, p, 42, 246, '已完成', 22, weight=550)
    txt(a, p, 366, 229, '服务端执行', 12, p['muted'], anchor='end')
    txt(a, p, 366, 257, '4.82 s', 26, weight=500, anchor='end')
    line(a, p, 24, 285, 366, 285)
    timeline(a, p, 24, 86, 118, 174, 366, 371, mobile=True)
    line(a, p, 24, 558, 366, 558)
    txt(a, p, 24, 590, '点选阶段，查看细节。', 13, p['muted'])
    composer(a, p, 16, 716, 358)
    txt(a, p, 195, 815, '静态设计稿，不代表真实运行结果。', 12, p['muted'], anchor='middle')
    save(a, '03-mobile-light')


if __name__ == '__main__':
    overview()
    detail()
    mobile()
