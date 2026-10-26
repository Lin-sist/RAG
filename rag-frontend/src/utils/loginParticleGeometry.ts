// Original procedural models for the login illustration. No external geometry or assets.
import { chapterBlend, LAST_SCENE, LOGIN_STORY } from './loginParticleStory'
export { PARTICLE_SCENES } from './loginParticleStory'
export const clamp = (value: number, low = 0, high = LAST_SCENE) => Math.max(low, Math.min(high, value))
const fraction = (value: number) => value - Math.floor(value)
type Point = [number, number, number]
type Sample = (u: number, v: number) => Point
type Part = { sample: Sample; weight: number; tone: number }

export function makeModels(count: number): Float32Array[] {
  if (!Number.isInteger(count) || count < 1 || count > 4800) throw Error('Invalid particle budget')
  const models: Part[][] = LOGIN_STORY.map(() => [])
  function line(parts: Part[], a: Point, b: Point, weight = 1, tone = 0) {
    parts.push({ weight, tone, sample: (u, v) => [
      a[0] + (b[0] - a[0]) * u + (v - .5) * .008,
      a[1] + (b[1] - a[1]) * u + (v - .5) * .008,
      a[2] + (b[2] - a[2]) * u,
    ] })
  }
  function rectangle(parts: Part[], x: number, y: number, w: number, h: number, z: number, tone = 0) {
    const corners: Point[] = [[x, y, z], [x + w, y, z], [x + w, y + h, z], [x, y + h, z]]
    for (let i = 0; i < 4; i++) line(parts, corners[i], corners[(i + 1) % 4], i % 2 ? h : w, tone)
  }
  // White wings follow the existing wordmark/login-logo.svg. Curves are
  // sampled only during model creation, never parsed or allocated per frame.
  type XY = [number, number]
  const cubic = (p: XY[], t: number): XY => {
    const s = 1 - t
    return [0, 1].map(axis => s ** 3 * p[0][axis] + 3 * s * s * t * p[1][axis]
      + 3 * s * t * t * p[2][axis] + t ** 3 * p[3][axis]) as XY
  }
  const logo = models[0]
  function wing(top: XY[], bottom: XY[], weight: number) {
    logo.push({ weight, tone: .04, sample: (u, v) => {
      const a = cubic(top, u), b = cubic(bottom, 1 - u)
      const across = v < .16 ? 0 : v > .84 ? 1 : (v - .16) / .68
      return [(a[0] + (b[0] - a[0]) * across - 303) / 175,
        (256.5 - a[1] - (b[1] - a[1]) * across) / 175, (across - .5) * .035]
    } })
  }
  const upperInner: XY[] = [[465, 268], [372, 236], [250, 184], [141, 133]]
  wing([[141, 133], [252, 170], [372, 222], [465, 268]], upperInner, 5)
  const lowerWing: XY[] = [[226, 380], [310, 372], [396, 326], [465, 268]]
  wing(lowerWing,
    [[465, 268], [398, 340], [302, 388], [226, 380]], 3.7)
  const upperJoin = cubic(upperInner, .18)
  const tip: XY[] = [upperJoin, [465, 268], cubic(lowerWing, .76), [427, 268], upperJoin]
  const controls: XY[] = [[447, 252], [447, 284], [427, 283], [427, 253]]
  logo.push({ weight: 1.1, tone: 1, sample: (u, v) => {
    const segment = Math.min(3, Math.floor(u * 4)), t = u * 4 - segment, s = 1 - t
    // Tuck the upper corner along the upper wing's inner edge; keep the lower seam.
    const boundary = segment === 0 ? cubic(upperInner, .18 * (1 - t)) : segment === 1 ? cubic(lowerWing, 1 - t * .24) : [
      s * s * tip[segment][0] + 2 * s * t * controls[segment][0] + t * t * tip[segment + 1][0],
      s * s * tip[segment][1] + 2 * s * t * controls[segment][1] + t * t * tip[segment + 1][1],
    ]
    const radius = v < .18 || v > .86 ? 1 : Math.sqrt((v - .18) / .68)
    return [(438 + (boundary[0] - 438) * radius - 303) / 175,
      (256.5 - 268 - (boundary[1] - 268) * radius) / 175, -.0175]
  } })
  const book = models[1]
  for (let layer = 0; layer < 3; layer++) {
    const x = -.64 + layer * .065, y = -.77 + layer * .045, z = -.17 + layer * .17
    rectangle(book, x, y, 1.05, 1.45, z)
    book.push({ weight: 1.5, tone: .08, sample: (u, v) => [x + u * 1.05, y + v * 1.45, z] })
    if (layer === 2) {
      line(book, [x + .73, y + 1.45, z], [x + .73, y + 1.15, z + .13], .5, 1)
      line(book, [x + .73, y + 1.15, z + .13], [x + 1.05, y + 1.15, z], .5, 1)
      line(book, [x + .73, y + 1.45, z], [x + 1.05, y + 1.15, z], .5, 1)
      for (let row = 0; row < 7; row++) {
        const yy = y + .3 + row * .125
        line(book, [x + .16, yy, z + .014], [x + (row === 6 ? .58 : .86), yy, z + .014], .8, row === 6 ? 1 : .2)
      }
    }
  }
  const graph = models[2]
  const nodes: Array<[number, number, number, number]> = [
    [0, 0, .12, .25], [-.82, .48, -.12, .13], [.72, .62, -.18, .16],
    [.91, -.2, .08, .12], [.25, -.79, -.1, .15], [-.7, -.58, .2, .14], [-.98, -.05, -.32, .08],
  ]
  nodes.forEach(([x, y, z, r], index) => {
    graph.push({ weight: index ? 2 : 5, tone: index ? .15 : 1, sample: (u, v) => {
      const latitude = Math.acos(2 * v - 1), angle = u * Math.PI * 2
      return [x + r * Math.sin(latitude) * Math.cos(angle), y + r * Math.cos(latitude), z + r * Math.sin(latitude) * Math.sin(angle)]
    } })
    if (index) line(graph, [0, 0, .12], [x, y, z], 1.8, .4)
  })
  for (const [a, b] of [[1, 2], [2, 3], [3, 4], [4, 5], [5, 6]]) {
    line(graph, nodes[a].slice(0, 3) as Point, nodes[b].slice(0, 3) as Point, .55, .05)
  }
  const answer = models[3]
  rectangle(answer, -.85, -.35, 1.7, 1.13, 0)
  rectangle(answer, -.8, -.4, 1.7, 1.13, -.14)
  answer.push({ weight: 2, tone: .08, sample: (u, v) => [-.85 + u * 1.7, -.35 + v * 1.13, 0] })
  for (let row = 0; row < 5; row++) {
    const y = .54 - row * .15
    line(answer, [-.62, y, .025], [row === 4 ? .19 : .59, y, .025], 1.2, row === 0 ? 1 : .15)
  }
  for (let tag = 0; tag < 3; tag++) {
    const x = -.75 + tag * .58
    rectangle(answer, x, -.86, .42, .26, .12, 1)
    line(answer, [x + .21, -.6, .12], [x + .21, -.35, 0], .35, .45)
    for (let mark = 0; mark <= tag; mark++) line(answer,
      [x + .13 + mark * .075, -.79, .13], [x + .13 + mark * .075, -.68, .13], .18, 1)
  }
  return models.map(parts => {
    const total = parts.reduce((sum, part) => sum + part.weight, 0), data = new Float32Array(count * 4)
    let partIndex = 0, before = 0
    for (let i = 0; i < count; i++) {
      const distance = (i + .5) / count * total
      while (partIndex < parts.length - 1 && distance > before + parts[partIndex].weight) before += parts[partIndex++].weight
      const part = parts[partIndex], u = (distance - before) / part.weight
      const point = part.sample(u, fraction(i * .61803398875 + .31))
      data.set([...point, part.tone], i * 4)
    }
    return data
  })
}

// Analytic damped recovery keeps reversals continuous at different frame rates.
export function spring(value: number, velocity: number, target: number, dt: number, frequency = 15): [number, number] {
  const t = clamp(dt, 0, .08), gap = value - target, impulse = velocity + frequency * gap
  const decay = Math.exp(-frequency * t)
  return [target + (gap + impulse * t) * decay, (velocity - frequency * impulse * t) * decay]
}

export function morphPoint(models: Float32Array[], index: number, progress: number, out: Float32Array, offset = 0) {
  const p = clamp(progress), scene = Math.min(models.length - 2, Math.floor(p)), phase = p - scene
  const mix = chapterBlend(phase), spread = Math.sin(Math.PI * phase) ** 2
  const start = index * 4, a = models[scene], b = models[scene + 1]
  for (let axis = 0; axis < 4; axis++) out[offset + axis] = a[start + axis] * (1 - mix) + b[start + axis] * mix
  // A bounded, centre-weighted disk follows the interpolated silhouette.
  // Radial/tangential coordinates avoid independent Cartesian scatter edges.
  const angle = fraction(index * .61803398875 + .17) * Math.PI * 2
  const radius = fraction(Math.sin(index * 17.13 + 3.7) * 9271.83) ** 2 * .26 * spread
  const x = out[offset], y = out[offset + 1], length = Math.hypot(x, y)
  const nx = length > .001 ? x / length : Math.cos(angle), ny = length > .001 ? y / length : Math.sin(angle)
  const radial = Math.cos(angle) * radius, tangent = Math.sin(angle) * radius
  out[offset] += nx * radial - ny * tangent
  out[offset + 1] += ny * radial + nx * tangent
  out[offset + 2] += Math.sin(angle * 2) * radius * .3
}

// Stable framing for every chapter, including scatter and the .2-axis brush.
// Short viewports scale down as a whole instead of clipping or fitting each frame.
export const projectionScale = (aspect: number) => .61 * Math.min(1, Math.max(.01, aspect))

export interface WheelInput { deltaY: number; deltaX: number; deltaMode: number; ctrlKey: boolean; metaKey: boolean; cancelable: boolean }
export function wheelTarget(current: number, event: WheelInput, height: number): number {
  if (!event.cancelable || event.ctrlKey || event.metaKey || !Number.isFinite(event.deltaY)
    || !Number.isFinite(event.deltaX) || Math.abs(event.deltaX) > Math.abs(event.deltaY)) return current
  const pixels = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? height : 1)
  return clamp(current + clamp(pixels / 1100, -.18, .18))
}
