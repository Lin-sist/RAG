// One chapter order for geometry, controls and the accompanying DOM copy.
export const LOGIN_STORY = [
  { label: '品牌标志', title: ['让每个回答', '都有出处'], description: '连接知识与问题，让回答有据可循。' },
  { label: '知识文档', title: ['把分散资料', '变成知识'], description: '将资料整理进知识库，为检索与问答做好准备。' },
  { label: '检索关联', title: ['沿着问题', '找到线索'], description: '从知识库中检索与问题相关的内容。' },
  { label: '引用回答', title: ['答案有依据', '来源可核对'], description: '结合检索内容组织回答，并提供可核对的引用。' },
] as const

export const LAST_SCENE = LOGIN_STORY.length - 1
export const PARTICLE_SCENES = LOGIN_STORY.map(scene => scene.label)

// Establish the arriving silhouette before the final detail-gathering phase.
// Sharing this curve keeps the dominant copy and point cloud together.
export function chapterBlend(phase: number) {
  const t = Math.max(0, Math.min(1, phase / .72))
  return t * t * (3 - 2 * t)
}

export function copyStyle(index: number, progress: number) {
  const distance = Math.max(-1, Math.min(1, index - progress))
  const start = Math.min(LAST_SCENE - 1, Math.floor(progress))
  const mix = chapterBlend(progress - start)
  const opacity = index === start ? 1 - mix : index === start + 1 ? mix : 0
  return { opacity, transform: `translateY(${distance * 16}px)` }
}
