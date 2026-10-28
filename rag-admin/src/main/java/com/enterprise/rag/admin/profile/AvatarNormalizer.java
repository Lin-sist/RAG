package com.enterprise.rag.admin.profile;

import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Component
public class AvatarNormalizer {
    public static final int SOURCE_LIMIT = 2 * 1024 * 1024;
    public byte[] normalize(byte[] data) {
        if (data.length > SOURCE_LIMIT) throw new BusinessException("PROFILE_AVATAR_TOO_LARGE", "头像不能超过 2 MB", HttpStatus.PAYLOAD_TOO_LARGE);
        boolean png = data.length >= 8 && data[0] == (byte)137 && data[1] == 80 && data[2] == 78 && data[3] == 71
                && data[4] == 13 && data[5] == 10 && data[6] == 26 && data[7] == 10;
        boolean jpg = data.length >= 3 && data[0] == (byte)255 && data[1] == (byte)216 && data[2] == (byte)255;
        if (!png && !jpg) throw new BusinessException("PROFILE_AVATAR_UNSUPPORTED", "请选择 JPG 或 PNG 图片", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(png && format.equals("png") || jpg && (format.equals("jpeg") || format.equals("jpg")))) throw invalid();
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w > 4096 || h > 4096 || (long)w * h > 16_000_000) throw invalid();
                BufferedImage source = reader.read(0);
                BufferedImage target = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                var graphics = target.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int side = Math.min(w, h), x = (w - side) / 2, y = (h - side) / 2;
                    graphics.drawImage(source, 0, 0, 256, 256, x, y, x + side, y + side, null);
                } finally { graphics.dispose(); source.flush(); }
                var output = new ByteArrayOutputStream();
                try { if (!ImageIO.write(target, "png", output) || output.size() > 512 * 1024) throw invalid(); }
                finally { target.flush(); }
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw invalid(); }
    }
    private BusinessException invalid() { return new BusinessException("PROFILE_AVATAR_INVALID", "图片无法读取或尺寸超过限制"); }
}
