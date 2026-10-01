package Tests;

import Models.*;
import Tools.ReadWriteImage;

import java.util.ArrayList;

public class TestPathmakerRender {
    public static void main(String[] args) throws Exception {
        int width = 1000, height = 1000;
        int[][][] framebuffer = new int[3][height][width]; // RGB layers

        Pathmaker pathmaker = new Pathmaker(width, height);
        ScanConvertLine scanline = new ScanConvertLine();

        for (int i = 0; i < 100; i++) {
            VectorAbstract[] cps = pathmaker.generateControlPoints();
            BezierCurve bezier = new BezierCurve(cps);
            ArrayList<VectorAbstract> points = bezier.sample(300);

            for (int j = 1; j < points.size(); j++) {
                VectorAbstract p0 = points.get(j - 1);
                VectorAbstract p1 = points.get(j);
                scanline.bresenham(
                        (int) p0.getX(), (int) p0.getY(),
                        (int) p1.getX(), (int) p1.getY(),
                        new Models.Color(0.0, 1.0, 1.0),
                        new Models.Color(0.0, 1.0, 1.0),
                        framebuffer
                );
            }
        }

        ReadWriteImage.writeImage(framebuffer,"A_Test_Of_Paths.png");
    }
}
