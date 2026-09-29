import numpy as np
from PIL import Image, ImageFilter

def generate_marble(width, height, seed, scale=0.008, octaves=4):
    """生成大理石纹理"""
    np.random.seed(seed)

    # 生成多层噪声
    noise = np.zeros((height, width), dtype=np.float64)
    for octave in range(octaves):
        freq = scale * (2 ** octave)
        amplitude = 1.0 / (2 ** octave)
        h = max(1, int(height * freq))
        w = max(1, int(width * freq))
        layer = np.random.rand(h, w)
        layer_img = Image.fromarray((layer * 255).astype(np.uint8))
        layer_img = layer_img.resize((width, height), Image.BICUBIC)
        layer_img = layer_img.filter(ImageFilter.GaussianBlur(radius=max(1, min(width, height) // 40)))
        noise += np.array(layer_img, dtype=np.float64) / 255.0 * amplitude

    # 正弦扭曲制造大理石纹路
    x = np.arange(width)[np.newaxis, :] * 0.01
    y = np.arange(height)[:, np.newaxis] * 0.01
    marble = np.sin(x * 2 + y * 3 + noise * 10)
    marble = (marble + 1) / 2  # 归一化到 0-1

    # 映射到琥珀色系
    r = (marble * 30 + 200).astype(np.uint8)   # 200-230
    g = (marble * 25 + 145).astype(np.uint8)   # 145-170
    b = (marble * 15 + 80).astype(np.uint8)    # 80-95
    a = (marble * 40 + 15).astype(np.uint8)    # 很淡的透明度 15-55

    img = Image.fromarray(np.stack([r, g, b, a], axis=-1), 'RGBA')
    return img

# 生成 5 张纹理
for i, (seed, scale) in enumerate([(7, 0.008), (12, 0.01), (18, 0.006), (23, 0.012), (31, 0.009)]):
    img = generate_marble(512, 512, seed, scale)
    img.save(f"marble_texture_{i+1}.png")
    print(f"Generated marble_texture_{i+1}.png")
