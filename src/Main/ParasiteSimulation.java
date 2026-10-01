package Main;

import Models.*;
import Tools.ReadWriteImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class ParasiteSimulation
{
    public static void main(String[] args) {
        int width = 1024;
        int height = 1024;
        int totalSteps = 600;
        int numSegments = 16;

        Pathmaker pathmaker = new Pathmaker(width, height);
        Shader shader = new Shader();
        Random rng = new Random();

        ColorAbstract[] palette = {
                new Color(1, 0, 0),     // Red
                new Color(0, 1, 0),     // Green
                new Color(0, 0, 1),     // Blue
                new Color(1, 1, 0),     // Yellow
                new Color(1, 1, 1),     // White
                new Color(0, 1, 1),     // Cyan
                new Color(0.6, 0, 1)    // Violet/Purple
        };

        ArrayList<Parasite> parasites = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            ArrayList<VectorAbstract> densePoints = new ArrayList<>();
            VectorAbstract lastEnd = null;
            for (int s = 0; s < numSegments; s++) {
                VectorAbstract[] controlPoints = (s == 0)
                        ? pathmaker.generateControlPoints()
                        : pathmaker.generateControlPointsNear(lastEnd);

                BezierCurve curve = new BezierCurve(controlPoints);
                ArrayList<VectorAbstract> segment = curve.sample(60);
                densePoints.addAll(segment);
                lastEnd = segment.get(segment.size() - 1);
            }

            ArrayList<VectorAbstract> resampled = resamplePath(densePoints, totalSteps);
            ColorAbstract color = palette[i % palette.length];
            Parasite parasite = new Parasite(resampled, 6, color);
            parasite.setLifespan(300);
            parasite.setShowTrail(false);
            parasite.setEnableCulling(true);
            parasites.add(parasite);
        }

        int frameCount = 0;
        boolean anyActive = true;
        while (anyActive) {
            int[][][] framebuffer = new int[3][height][width];
            anyActive = false;

            for (Parasite p : parasites) {
                if (!p.isAlive()) continue;
                p.tick();
                if (!p.isFinished()) p.update();
            }

            Set<Parasite> eliminated = new HashSet<>();
            for (int i = 0; i < parasites.size(); i++) {
                Parasite p1 = parasites.get(i);
                if (!p1.isAlive() || eliminated.contains(p1)) continue;

                Triangle t1 = p1.getTriangle();
                for (int j = i + 1; j < parasites.size(); j++) {
                    Parasite p2 = parasites.get(j);
                    if (!p2.isAlive() || eliminated.contains(p2)) continue;

                    Triangle t2 = p2.getTriangle();
                    if (t1.intersects(t2)) {
                        if (rng.nextBoolean()) {
                            p2.die();
                            p1.grow();
                            eliminated.add(p2);
                        } else {
                            p1.die();
                            p2.grow();
                            eliminated.add(p1);
                            break;
                        }
                    }
                }
            }

            for (Parasite p : parasites) {
                if (!p.isAlive()) continue;
                if (p.isCullingEnabled() && !p.isOnScreen(width, height)) continue;

                if (p.isTrailEnabled()) {
                    ArrayList<VectorAbstract> trail = p.getTrailPoints();
                    for (int k = 1; k < trail.size(); k++) {
                        drawBlueTrailLine(trail.get(k - 1), trail.get(k), framebuffer);
                    }
                }

                shader.solidFill(p.getTriangle(), framebuffer);
                anyActive = true;
            }

            String filename = String.format("frame%03d.png", frameCount++);
            try {
                ReadWriteImage.writeImage(framebuffer, filename);
                System.out.println("Wrote " + filename);
            } catch (IOException e) {
                System.err.println("Failed to write " + filename);
            }
        }

        System.out.println("Smooth-motion simulation complete.");
    }

    public static ArrayList<VectorAbstract> resamplePath(ArrayList<VectorAbstract> dense, int totalSteps) {
        ArrayList<VectorAbstract> resampled = new ArrayList<>();
        if (dense.size() < 2) return resampled;

        double[] arc = new double[dense.size()];
        arc[0] = 0;
        for (int i = 1; i < dense.size(); i++) {
            double dx = dense.get(i).getX() - dense.get(i - 1).getX();
            double dy = dense.get(i).getY() - dense.get(i - 1).getY();
            arc[i] = arc[i - 1] + Math.sqrt(dx * dx + dy * dy);
        }
        double totalLength = arc[arc.length - 1];

        for (int s = 0; s < totalSteps; s++) {
            double target = (s / (double)(totalSteps - 1)) * totalLength;
            int j = 1;
            while (j < arc.length && arc[j] < target) j++;
            if (j >= arc.length) break;

            VectorAbstract p0 = dense.get(j - 1);
            VectorAbstract p1 = dense.get(j);
            double ratio = (target - arc[j - 1]) / (arc[j] - arc[j - 1]);
            double x = p0.getX() + (p1.getX() - p0.getX()) * ratio;
            double y = p0.getY() + (p1.getY() - p0.getY()) * ratio;

            resampled.add(new Vector(x, y, 0));
        }

        return resampled;
    }

    // Always draw trail segments in bright blue
    private static void drawBlueTrailLine(VectorAbstract p1, VectorAbstract p2, int[][][] fb) {
        int x0 = (int) Math.round(p1.getX());
        int y0 = (int) Math.round(p1.getY());
        int x1 = (int) Math.round(p2.getX());
        int y1 = (int) Math.round(p2.getY());

        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < fb[0][0].length && y0 >= 0 && y0 < fb[0].length) {
                fb[0][y0][x0] = 0;     // R
                fb[1][y0][x0] = 255;     // G
                fb[2][y0][x0] = 20;   // B
            }

            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }
        }
    }
}


