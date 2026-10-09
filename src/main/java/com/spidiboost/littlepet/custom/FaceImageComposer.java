package com.spidiboost.littlepet.custom;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Iterator;

 
public final class FaceImageComposer {
    public static final int SIZE=1024, SKIN_SCALE=16;
    private FaceImageComposer() { }
    public static BufferedImage readPhoto(Path file) throws IOException {
        if(!Files.isRegularFile(file))throw new IOException("Файл не найден");
        if(Files.size(file)>64L*1024*1024)throw new IOException("Изображение больше 64 МБ");
        try(ImageInputStream input=ImageIO.createImageInputStream(file.toFile())) {
            if(input==null)throw new IOException("Не удалось прочитать файл");
            Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext())throw new IOException("Поддерживаются PNG, JPG, JPEG, BMP, GIF и TIFF");
            ImageReader reader=readers.next();
            try {
                reader.setInput(input,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>32768||h>32768||(long)w*h>250_000_000L)throw new IOException("Слишком большое разрешение изображения");
                ImageReadParam options=reader.getDefaultReadParam();
                int subsample=Math.max(1,(Math.max(w,h)+2047)/2048);
                options.setSourceSubsampling(subsample,subsample,0,0);
                BufferedImage photo=reader.read(0,options);
                if(photo==null)throw new IOException("Не удалось декодировать изображение");
                return photo;
            } finally {reader.dispose();}
        }
    }
    public static BufferedImage normalizeSkin(BufferedImage input) throws IOException {
        if(input==null || input.getWidth()<64 || input.getWidth()>2048 || input.getWidth()%64!=0 || (input.getHeight()!=input.getWidth() && !(input.getWidth()==64 && input.getHeight()==32)))throw new IOException("Некорректные размеры текстуры скина");
        if(input.getHeight()==input.getWidth())return input;
        BufferedImage result=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);g.drawImage(input,0,0,null);
            if(input.getHeight()==32) {
                int[][] rectangles={{4,16,16,32,4,4},{8,16,16,32,4,4},{0,20,24,32,4,12},{4,20,16,32,4,12},{8,20,8,32,4,12},{12,20,16,32,4,12},{44,16,-8,32,4,4},{48,16,-8,32,4,4},{40,20,0,32,4,12},{44,20,-8,32,4,12},{48,20,-16,32,4,12},{52,20,-8,32,4,12}};
                for(int[] r:rectangles)g.drawImage(input,r[0]+r[2],r[1]+r[3],r[0]+r[2]+r[4],r[1]+r[3]+r[5],r[0]+r[4],r[1],r[0],r[1]+r[5],null);
            }
        } finally {g.dispose();}
        return result;
    }
    public static BufferedImage compose(BufferedImage steve,BufferedImage photo) throws IOException {
        steve=normalizeSkin(steve);
        BufferedImage skin=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=skin.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(steve,0,0,SIZE,SIZE,null);
            
            
            if(photo==null)return skin;
            int edge=8*SKIN_SCALE,side=Math.min(photo.getWidth(),photo.getHeight());
            int cropX=(photo.getWidth()-side)/2,cropY=(photo.getHeight()-side)/2;
            g.setClip(edge,edge,edge,edge);
            g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(photo,edge,edge,edge*2,edge*2,cropX,cropY,cropX+side,cropY+side,null);
            g.setClip(null);g.setComposite(AlphaComposite.Clear);
            g.fillRect(40*SKIN_SCALE,edge,edge,edge);
        } finally {g.dispose();}
        return skin;
    }
}
