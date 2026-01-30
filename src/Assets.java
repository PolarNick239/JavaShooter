import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Assets {
    public static BufferedImage loadImage(String resourcePath) throws IOException {
        try (InputStream in = Assets.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    throw new IOException("Unsupported image format: " + resourcePath);
                }
                return image;
            }
        }
        String filePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        BufferedImage image = ImageIO.read(new File(filePath));
        if (image == null) {
            throw new IOException("Image not found or unsupported: " + filePath);
        }
        return image;
    }
}
