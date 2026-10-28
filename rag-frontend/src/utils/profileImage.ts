/** The preview and the submitted bytes are the same oriented, center-cropped PNG. */
export async function normalizeProfileImage(file: File): Promise<Blob> {
    if (file.size > 2 * 1024 * 1024) throw new Error('头像不能超过 2 MB。')
    const signature = new Uint8Array(await file.slice(0, 8).arrayBuffer())
    const png = [137,80,78,71,13,10,26,10].every((value, index) => signature[index] === value)
    const jpg = signature[0] === 255 && signature[1] === 216 && signature[2] === 255
    if (!png && !jpg) throw new Error('请选择 JPG 或 PNG 图片。')
    const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' }).catch(() => { throw new Error('图片无法读取，请重新选择。') })
    try {
        if (bitmap.width > 4096 || bitmap.height > 4096 || bitmap.width * bitmap.height > 16_000_000) throw new Error('图片尺寸过大，最长边不能超过 4096 像素。')
        const canvas = document.createElement('canvas'); canvas.width = 256; canvas.height = 256
        const context = canvas.getContext('2d')
        if (!context) throw new Error('当前浏览器暂不能处理图片。')
        const side = Math.min(bitmap.width, bitmap.height)
        context.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, 256, 256)
        const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob(value => value ? resolve(value) : reject(new Error('图片处理失败。')), 'image/png'))
        if (blob.size > 512 * 1024) throw new Error('处理后的图片过大，请选择其他图片。')
        return blob
    } finally { bitmap.close() }
}
