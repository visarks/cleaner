#!/bin/bash
set -e

echo "=== 生成应用图标 ==="

# 创建临时目录
ICON_DIR="src/main/resources/icons.iconset"
mkdir -p "$ICON_DIR"

# 创建平台目录
mkdir -p javafx/mac javafx/windows javafx/linux

# 创建图标生成器
cat > /tmp/IconGen.java << 'JAVAEOF'
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class IconGen {
    public static void main(String[] args) throws Exception {
        String outputDir = "src/main/resources/icons.iconset";
        new File(outputDir).mkdirs();

        int[] sizes = {16, 32, 64, 128, 256, 512};

        for (int size : sizes) {
            BufferedImage img = generateLogo(size);
            String filename = String.format("%s/icon_%dx%d.png", outputDir, size, size);
            ImageIO.write(img, "PNG", new File(filename));
            System.out.println("Generated: " + filename);
        }

        // Generate @2x versions
        ImageIO.write(generateLogo(32), "PNG", new File(outputDir + "/icon_16x16@2x.png"));
        ImageIO.write(generateLogo(64), "PNG", new File(outputDir + "/icon_32x32@2x.png"));
        ImageIO.write(generateLogo(128), "PNG", new File(outputDir + "/icon_64x64@2x.png"));
        ImageIO.write(generateLogo(256), "PNG", new File(outputDir + "/icon_128x128@2x.png"));
        ImageIO.write(generateLogo(512), "PNG", new File(outputDir + "/icon_256x256@2x.png"));
        ImageIO.write(generateLogo(1024), "PNG", new File(outputDir + "/icon_512x512@2x.png"));

        // Generate Linux PNG (256x256)
        System.out.println("Generating Linux PNG...");
        new File("javafx/linux").mkdirs();
        ImageIO.write(generateLogo(256), "PNG", new File("javafx/linux/Cleaner.png"));
        System.out.println("Generated: javafx/linux/Cleaner.png");

        // Generate Windows PNG placeholders (will be converted to ICO)
        System.out.println("Generating Windows PNG icons...");
        new File("javafx/windows").mkdirs();
        ImageIO.write(generateLogo(16), "PNG", new File("javafx/windows/icon_16.png"));
        ImageIO.write(generateLogo(32), "PNG", new File("javafx/windows/icon_32.png"));
        ImageIO.write(generateLogo(48), "PNG", new File("javafx/windows/icon_48.png"));
        ImageIO.write(generateLogo(64), "PNG", new File("javafx/windows/icon_64.png"));
        ImageIO.write(generateLogo(128), "PNG", new File("javafx/windows/icon_128.png"));
        ImageIO.write(generateLogo(256), "PNG", new File("javafx/windows/icon_256.png"));
        System.out.println("Generated Windows PNG icons in javafx/windows/");

        System.out.println("All icons generated successfully!");
    }

    static BufferedImage generateLogo(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background circle with gradient
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(0x1f883d), size, size, new Color(0x2ea043));
        g.setPaint(bgGradient);
        g.fillOval(2, 2, size - 4, size - 4);

        // White brush/broom icon
        g.setColor(Color.WHITE);

        // Handle
        double handleWidth = size * 0.1;
        double handleHeight = size * 0.4;
        double handleX = (size - handleWidth) / 2;
        double handleY = size * 0.15;
        g.fill(new RoundRectangle2D.Double(handleX, handleY, handleWidth, handleHeight, 3, 3));

        // Brush head
        double brushWidth = size * 0.4;
        double brushHeight = size * 0.2;
        double brushX = (size - brushWidth) / 2;
        double brushY = handleY + handleHeight - 5;
        g.fill(new RoundRectangle2D.Double(brushX, brushY, brushWidth, brushHeight, 5, 5));

        // Bristles
        double bristleY = brushY + brushHeight;
        double bristleHeight = size * 0.15;
        double bristleWidth = size * 0.04;
        double bristleStartX = brushX + brushWidth * 0.1;
        double bristleSpacing = brushWidth / 5;

        for (int i = 0; i < 5; i++) {
            double x = bristleStartX + i * bristleSpacing;
            double h = bristleHeight + (i % 2 == 0 ? 5 : -3);
            g.fill(new RoundRectangle2D.Double(x, bristleY, bristleWidth, h, 1, 1));
        }

        // Sparkles
        g.setColor(Color.WHITE);
        double sparkleSize = size * 0.04;
        g.fill(new Ellipse2D.Double(size * 0.2, size * 0.25, sparkleSize, sparkleSize));
        g.fill(new Ellipse2D.Double(size * 0.75, size * 0.3, sparkleSize * 0.7, sparkleSize * 0.7));
        g.fill(new Ellipse2D.Double(size * 0.8, size * 0.2, sparkleSize * 1.2, sparkleSize * 1.2));

        g.dispose();
        return img;
    }
}
JAVAEOF

# 编译并运行
echo ">>> 生成 PNG 图标..."
javac /tmp/IconGen.java -d /tmp
java -cp /tmp IconGen

# 使用 iconutil 创建 icns 文件 (macOS)
echo ">>> 创建 icns 文件..."
iconutil -c icns src/main/resources/icons.iconset -o src/main/resources/icon.icns

# 复制到 javafx/mac 目录（用于打包）
cp src/main/resources/icon.icns javafx/mac/Cleaner.icns

# 尝试创建 Windows ICO 文件
echo ">>> 创建 Windows ICO 文件..."
if command -v convert &> /dev/null; then
    # 使用 ImageMagick 创建 ICO
    convert javafx/windows/icon_16.png javafx/windows/icon_32.png \
            javafx/windows/icon_48.png javafx/windows/icon_64.png \
            javafx/windows/icon_128.png javafx/windows/icon_256.png \
            javafx/windows/Cleaner.ico
    echo "Created: javafx/windows/Cleaner.ico"
else
    echo "ImageMagick not found. Creating placeholder ICO from PNG..."
    # 创建一个简单的 ICO 文件（使用 256x256 PNG 作为基础）
    # 注意：这只是一个临时的解决方案，建议安装 ImageMagick 获取更好的 ICO 文件
    cp javafx/windows/icon_256.png javafx/windows/Cleaner.png
    echo "Note: For best results, install ImageMagick and run this script again."
    echo "      brew install imagemagick"
fi

echo ""
echo "=== 图标生成完成 ==="
echo ""
echo "macOS:"
echo "  - 应用内图标: src/main/resources/icon.icns"
echo "  - 打包图标:   javafx/mac/Cleaner.icns"
echo ""
echo "Windows:"
echo "  - PNG 图标:   javafx/windows/icon_*.png"
echo "  - ICO 文件:   javafx/windows/Cleaner.ico (如已安装 ImageMagick)"
echo ""
echo "Linux:"
echo "  - PNG 图标:   javafx/linux/Cleaner.png"
echo ""
ls -la javafx/mac/ javafx/windows/ javafx/linux/
