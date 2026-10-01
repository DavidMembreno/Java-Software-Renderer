package Main;

import Models.*;
import Tools.ReadWriteImage;
import Tools.STLParser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class STLR {

    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    private static final int BUFFER_SIZE = 256; // Size of the 3D framebuffer
    private static final int MULT_INCREMENT = 5; // Increment for mult
    private static final int MAX_MULT = 188; // Maximum value of mult for the loop

    public static void main(String[] args) {
        // Path to the STL file
        String filePath = "src/Assets/cube-ascii.stl";

        try {
            // Create a 3D framebuffer (RGB)
            int[][][] framebuffer = new int[3][BUFFER_SIZE][BUFFER_SIZE];
            clearFramebuffer(framebuffer);

            // Parse the STL file
            List<TriangleAbstract> triangles = STLParser.parseSTLFile(Path.of(filePath));
            System.out.println("Loaded " + triangles.size() + " triangles from STL file");

            // Group triangles by face based on their normals
            Map<String, List<TriangleAbstract>> faceGroups = groupTrianglesByFace(triangles);
            System.out.println("Identified " + faceGroups.size() + " distinct faces");

            // Define colors for each face
            ColorAbstract[] faceColors = {
                    new Color(1.0, 0.0, 0.0),  // Red
                    new Color(0.0, 1.0, 0.0),  // Green
                    new Color(0.0, 0.0, 1.0),  // Blue
                    new Color(1.0, 1.0, 0.0),  // Yellow
                    new Color(1.0, 0.0, 1.0),  // Magenta
                    new Color(0.0, 1.0, 1.0)   // Cyan
            };

            // Apply colors to each face
            List<TriangleAbstract> coloredTriangles = assignColorsToFaces(faceGroups, faceColors);

            // Create a scene object and add all colored triangles
            SceneObject sceneObject = new SceneObject();
            for (TriangleAbstract triangle : coloredTriangles) {
                sceneObject.addTriangle(triangle);
            }

            // Center the object in the viewport
            centerObject(sceneObject, BUFFER_SIZE, BUFFER_SIZE);

            // Set up a viewpoint for rendering
            VectorAbstract viewpoint = new Vector(0, 0, 1, null);

            // Loop through mult values and generate images
            for (int mult = 5; mult <= MAX_MULT; mult += MULT_INCREMENT) {
                // Create a new framebuffer for each iteration to avoid cumulative transformations
                int[][][] frameBufferCopy = new int[3][BUFFER_SIZE][BUFFER_SIZE];
                clearFramebuffer(frameBufferCopy);

                // Create a fresh scene object and add all colored triangles for this iteration
                SceneObject newSceneObject = new SceneObject();
                for (TriangleAbstract triangle : coloredTriangles) {
                    newSceneObject.addTriangle(triangle);
                }

                // Center the new object in the viewport again
                centerObject(newSceneObject, BUFFER_SIZE, BUFFER_SIZE);

                // Apply rotation for the current `mult`
                newSceneObject.rotate(new Vector(1, 0, 0, null), 2*mult); // Rotate around X axis
                newSceneObject.rotate(new Vector(0, 1, 0, null), 45); // Rotate around Y axis
                //newSceneObject.rotate(new Vector(0, 0, 1, null), 275*mult);//rotate around z

                // Render the scene object for the current mult value
                newSceneObject.render(frameBufferCopy, false, ShaderAbstract.FILLSTYLE.SHADE, viewpoint);

                // Convert the framebuffer to a BufferedImage
                BufferedImage image = ReadWriteImage.toBI(frameBufferCopy);

                // Save the image with the current iteration number
                String outputPath = "colored_cube_" + (mult / 5) + ".png"; // Save with name like colored_cube_1.png
                ReadWriteImage.writeImage(frameBufferCopy, outputPath);
                System.out.println("Image saved to " + outputPath);
            }

        } catch (IOException e) {
            System.err.println("Error loading STL file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Groups triangles by face based on their normal vectors
     */
    private static Map<String, List<TriangleAbstract>> groupTrianglesByFace(List<TriangleAbstract> triangles) {
        Map<String, List<TriangleAbstract>> faceGroups = new HashMap<>();

        for (TriangleAbstract triangle : triangles) {
            VectorAbstract normal = triangle.getNormal();

            // Round normal components to handle floating point precision issues
            String normalKey = roundVector(normal);

            if (!faceGroups.containsKey(normalKey)) {
                faceGroups.put(normalKey, new ArrayList<>());
            }

            faceGroups.get(normalKey).add(triangle);
        }

        return faceGroups;
    }

    /**
     * Rounds vector components to create a string key for face grouping
     */
    private static String roundVector(VectorAbstract vector) {
        // Round to 1 decimal place to group slightly different normals
        double x = Math.round(vector.getX() * 10) / 10.0;
        double y = Math.round(vector.getY() * 10) / 10.0;
        double z = Math.round(vector.getZ() * 10) / 10.0;

        return x + "," + y + "," + z;
    }

    /**
     * Assigns colors to each face of the cube
     */
    private static List<TriangleAbstract> assignColorsToFaces(
            Map<String, List<TriangleAbstract>> faceGroups,
            ColorAbstract[] faceColors) {

        List<TriangleAbstract> coloredTriangles = new ArrayList<>();
        int colorIndex = 0;

        for (String faceKey : faceGroups.keySet()) {
            List<TriangleAbstract> faceTris = faceGroups.get(faceKey);
            ColorAbstract faceColor = faceColors[colorIndex % faceColors.length];

            System.out.println("Face " + colorIndex + " (normal: " + faceKey +
                    ") has " + faceTris.size() + " triangles with color " +
                    faceColor.getR() + "," + faceColor.getG() + "," + faceColor.getB());

            for (TriangleAbstract tri : faceTris) {
                VectorAbstract[] vertices = tri.getVertices();

                // Create new vertices with the face color
                VectorAbstract v1 = new Vector(vertices[0].getX(), vertices[0].getY(), vertices[0].getZ(), (Color) faceColor);
                VectorAbstract v2 = new Vector(vertices[1].getX(), vertices[1].getY(), vertices[1].getZ(), (Color) faceColor);
                VectorAbstract v3 = new Vector(vertices[2].getX(), vertices[2].getY(), vertices[2].getZ(), (Color) faceColor);

                // Create a new triangle with the colored vertices
                TriangleAbstract coloredTri = new Triangle(v1, v2, v3);
                coloredTriangles.add(coloredTri);
            }

            colorIndex++;
        }

        return coloredTriangles;
    }

    private static void clearFramebuffer(int[][][] framebuffer) {
        for (int i = 0; i < framebuffer.length; i++) {
            for (int j = 0; j < framebuffer[i].length; j++) {
                for (int k = 0; k < framebuffer[i][j].length; k++) {
                    framebuffer[i][j][k] = 0; // Set all values to 0 (black)
                }
            }
        }
    }

    private static void centerObject(SceneObject sceneObject, int width, int height) {
        // Get the current center and extents of the object
        VectorAbstract[] extents = sceneObject.getExtents();

        // Calculate scaling factor to fit object in viewport
        double xRange = Math.abs(extents[1].getX() - extents[0].getX());
        double yRange = Math.abs(extents[1].getY() - extents[0].getY());
        double zRange = Math.abs(extents[1].getZ() - extents[0].getZ());

        double maxRange = Math.max(Math.max(xRange, yRange), zRange);
        double scaleFactor = (width * 0.7) / maxRange;

        // Scale the object
        sceneObject.scale(new Vector(scaleFactor, scaleFactor, scaleFactor, null));

        // Center the object in the viewport
        VectorAbstract center = sceneObject.getCenter();
        VectorAbstract translation = new Vector(
                width / 2 - center.getX(),
                height / 2 - center.getY(),
                0, // Keep Z as is
                null
        );

        sceneObject.translate(translation);

    }
}
