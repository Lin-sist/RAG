<template>
  <div class="pixel-art" aria-hidden="true">
    <div class="pixel-grid">
      <span
        v-for="cell in cells"
        :key="cell.index"
        class="pixel"
        :class="[{ lit: cell.lit }, `tone-${cell.tone}`]"
        :style="{ transitionDelay: `${cell.delay}ms` }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

const gridSize = 16
const logoPoints = [
  [1, 4], [2, 4], [1, 5], [2, 5], [1, 10], [2, 10], [1, 11], [2, 11],
  [4, 6], [4, 9], [6, 7], [6, 8],
  [7, 2], [8, 4], [9, 5], [10, 6], [11, 6], [12, 7], [13, 7],
  [7, 13], [8, 11], [9, 10], [10, 9], [11, 9], [12, 8], [13, 8],
]
const shape = (predicate: (x: number, y: number) => boolean) => {
  const result = new Set<number>()
  for (let y = 0; y < gridSize; y++) {
    for (let x = 0; x < gridSize; x++) {
      if (predicate(x, y)) result.add(y * gridSize + x)
    }
  }
  return result
}
const glyphs = [
  new Set(logoPoints.map(([x, y]) => y * gridSize + x)),
  shape((x, y) => x === 7 || x === 8 || y === 7 || y === 8 || Math.abs(x - y) < 1 || Math.abs(x + y - 15) < 1),
  shape((x, y) => x >= 4 && x <= 11 && y >= 1 && y <= 14 && !((y === 5 || y === 8 || y === 11) && x >= 6 && x <= 10)),
  shape((x, y) => {
    const distance = Math.hypot(x + .5 - 7, y + .5 - 7)
    return (distance > 3 && distance < 5) || (x >= 10 && y >= 10 && x <= 14 && y <= 14 && Math.abs(x - y) <= 1)
  }),
  shape((x, y) => (x >= 3 && x <= 12 && y >= 2 && y <= 9 && !((y === 4 || y === 6) && x >= 5 && x <= 10)) || (y >= 10 && y <= 11 && x >= 4 && x <= 6)),
]

const currentGlyph = ref(0)
const cells = computed(() => Array.from({ length: gridSize * gridSize }, (_, index) => {
  const x = index % gridSize
  const y = Math.floor(index / gridSize)
  return {
    index,
    lit: glyphs[currentGlyph.value]?.has(index) ?? false,
    tone: (x * 7 + y * 3) % 5,
    delay: Math.round(Math.hypot(x - 7.5, y - 7.5) * 22),
  }
}))

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  timer = setInterval(() => {
    currentGlyph.value = (currentGlyph.value + 1) % glyphs.length
  }, 2600)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.pixel-art { aspect-ratio: 1; display: grid; place-items: center; }
.pixel-grid { display: grid; grid-template-columns: repeat(16, 1fr); gap: 3px; width: 100%; }
.pixel {
  aspect-ratio: 1;
  border-radius: 26%;
  background: #d97757;
  opacity: 0;
  transform: scale(.2);
  transition: opacity .42s ease, transform .42s ease;
}
.pixel.lit { opacity: 1; transform: scale(.86); }
.tone-1 { background: #c15f3c; }
.tone-2 { background: #b4552d; }
.tone-3 { background: #e5a184; }
.tone-4 { background: #edcdb9; }
@media (prefers-reduced-motion: reduce) {
  .pixel { transition: none; }
}
</style>
