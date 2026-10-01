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

/**
 * CatRotator - A program that loads a 3D model (presumably of a cat) from an STL file
 * and generates an animation sequence of the model rotating and orbiting.
 * The animation is saved as a series of PNG image files.
 */
public class CatRotator
{
    // Constants defining the size of the rendering window and buffer
    private static final int BUFFER_SIZE = 256; // Size of the 3D framebuffer (256x256 resolution)
    private static final int MULT_INCREMENT = 5; // Step size for the animation loop counter
    private static final int MAX_MULT = 800; // Maximum value for the animation loop counter (determines total frames)

    public static void main(String[] args)
    {
        // Path to the STL file containing the 3D model
        String filePath = "src/Assets/cube-ascii.stl";

        try
        {
            // Create a 3D framebuffer with RGB channels (3D array: [color_channel][x][y])
            int[][][] framebuffer = new int[3][BUFFER_SIZE][BUFFER_SIZE];
            clearFramebuffer(framebuffer); // Initialize all pixels to black (0,0,0)

            // Parse the STL file to extract triangles that make up the 3D model
            List<TriangleAbstract> triangles = STLParser.parseSTLFile(Path.of(filePath));
            System.out.println("Loaded " + triangles.size() + " triangles from STL file");

            // Group triangles by face based on their normal vectors
            // This allows identifying which triangles belong to the same face of the model
            Map<String, List<TriangleAbstract>> faceGroups = groupTrianglesByFace(triangles);
            System.out.println("Identified " + faceGroups.size() + " distinct faces");

            // Define a palette of colors to apply to different faces of the model
            // Currently using green, magenta, and cyan (red and blue are commented out)
            ColorAbstract[] faceColors = {
                    new Color(1.0, 0.0, 0.0),  // Red (commented out)
                    new Color(0.0, 1.0, 0.0),  // Green
                    new Color(0.0, 0.0, 1.0),  // Blue (commented out)
                    new Color(1.0, 1.0, 0.0),  // Yellow (commented out)
                    new Color(1.0, 0.0, 1.0),  // Magenta
                    new Color(0.0, 1.0, 1.0),  // Cyan
                    new Color(1.0, 1.0, 1.0)  //White
            };

            // Apply a different color to each face of the model
            List<TriangleAbstract> coloredTriangles = assignColorsToFaces(faceGroups, faceColors);

            // Create a scene object and add all colored triangles to it
            SceneObject sceneObject = new SceneObject();
            for (TriangleAbstract triangle : coloredTriangles)
            {
                sceneObject.addTriangle(triangle);
            }

            // Center the object in the viewport
            centerObject(sceneObject, BUFFER_SIZE, BUFFER_SIZE);

            // Set up a viewpoint for rendering (positioned at z=1, looking along -z direction)
            VectorAbstract viewpoint = new Vector(0, 0, 1, null);

            // Calculate the total number of frames in the animation
            int totalFrames = (MAX_MULT - 5) / MULT_INCREMENT + 1;

            // Define orbit radius (distance from center)
            double orbitRadius = BUFFER_SIZE * 0.25; // 25% of buffer size

            // Animation loop - create a sequence of frames showing the model rotating and orbiting
            for (int mult = 5; mult <= MAX_MULT; mult += MULT_INCREMENT)
            {
                // Create a fresh framebuffer for each frame to avoid ghosting effects
                int[][][] frameBufferCopy = new int[3][BUFFER_SIZE][BUFFER_SIZE];
                clearFramebuffer(frameBufferCopy);

                // Create a fresh scene object with all colored triangles for this frame
                // This ensures each frame starts with the original untransformed model
                SceneObject newSceneObject = new SceneObject();
                for (TriangleAbstract triangle : coloredTriangles)
                {
                    newSceneObject.addTriangle(triangle);
                }

                // Scale the object to 40% of its original size
                double scaleFactor = 0.4;
                newSceneObject.scale(new Vector(scaleFactor, scaleFactor, scaleFactor, null));

                // Calculate animation progress (from 0.0 to 1.0)
                int frameNumber = (mult - 5) / MULT_INCREMENT;
                double completionRatio = (double) frameNumber / (totalFrames - 1);

                // Calculate the orbital angle for planetary motion (2 complete orbits)
                double orbitalAngle = completionRatio * 2 * Math.PI * 2;

                // Calculate the position in the orbit using parametric circle equations
                double xPos = BUFFER_SIZE / 2 + orbitRadius * Math.cos(orbitalAngle);
                double yPos = BUFFER_SIZE / 2 + orbitRadius * Math.sin(orbitalAngle);

                // Apply self-rotation around X-axis (5 complete rotations during the animation)
                double selfRotationAngle = completionRatio * 360 * 5;
                newSceneObject.rotate(new Vector(1, 0, 0, null), selfRotationAngle);

                // Apply additional rotation around Y-axis (slightly slower - 3.75 rotations)
                newSceneObject.rotate(new Vector(0, 1, 0, null), selfRotationAngle * 0.75);

                // Translate the object to its position in the orbital path
                VectorAbstract center = newSceneObject.getCenter();
                VectorAbstract translation = new Vector(
                        xPos - center.getX(),  // Move X to orbit position
                        yPos - center.getY(),  // Move Y to orbit position
                        0,                     // Keep Z as is
                        null
                );
                newSceneObject.translate(translation);

                // Render the scene object using shader-based rendering with the viewpoint
                newSceneObject.render(frameBufferCopy, false, ShaderAbstract.FILLSTYLE.SHADE, viewpoint);

                // Convert the framebuffer to a BufferedImage (not used in this code)
                BufferedImage image = ReadWriteImage.toBI(frameBufferCopy);

                // Generate filename for this frame and save the image
                String outputPath = "orbiting_cube_" + (mult / 5) + ".png";
                ReadWriteImage.writeImage(frameBufferCopy, outputPath);

                // Log progress information
                System.out.println("Image saved to " + outputPath +
                        " (Orbit position: X=" + String.format("%.1f", xPos) +
                        ", Y=" + String.format("%.1f", yPos) +
                        ", Rotation: " + String.format("%.1f", selfRotationAngle) + "°)");
            }

        } catch (IOException e)
        {
            System.err.println("Error loading STL file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Groups triangles by face based on their normal vectors.
     * Triangles with similar normal vectors are considered part of the same face.
     *
     * @param triangles List of all triangles in the model
     * @return Map of face groups, keyed by normalized normal vector strings
     */
    private static Map<String, List<TriangleAbstract>> groupTrianglesByFace(List<TriangleAbstract> triangles)
    {
        Map<String, List<TriangleAbstract>> faceGroups = new HashMap<>();

        for (TriangleAbstract triangle : triangles)
        {
            VectorAbstract normal = triangle.getNormal();

            // Round normal components to handle floating point precision issues
            // This creates a string key like "0.0,1.0,0.0" to group triangles facing the same direction
            String normalKey = roundVector(normal);

            // Create a new list for this face if it doesn't exist yet
            if (!faceGroups.containsKey(normalKey))
            {
                faceGroups.put(normalKey, new ArrayList<>());
            }

            // Add this triangle to its face group
            faceGroups.get(normalKey).add(triangle);
        }

        return faceGroups;
    }

    /**
     * Rounds vector components to create a string key for face grouping.
     * This helps deal with floating point precision issues when comparing normals.
     *
     * @param vector The vector to round
     * @return String representation of the rounded vector components
     */
    private static String roundVector(VectorAbstract vector)
    {
        // Round to 1 decimal place to group slightly different normals
        double x = Math.round(vector.getX() * 10) / 10.0;
        double y = Math.round(vector.getY() * 10) / 10.0;
        double z = Math.round(vector.getZ() * 10) / 10.0;

        return x + "," + y + "," + z;
    }

    /**
     * Assigns colors to each face of the model.
     * Creates new colored triangles based on the original geometry.
     *
     * @param faceGroups Map of triangles grouped by face
     * @param faceColors Array of colors to assign to faces
     * @return List of triangles with colors assigned
     */
    private static List<TriangleAbstract> assignColorsToFaces(
            Map<String, List<TriangleAbstract>> faceGroups,
            ColorAbstract[] faceColors)
    {
        List<TriangleAbstract> coloredTriangles = new ArrayList<>();
        int colorIndex = 0;

        for (String faceKey : faceGroups.keySet())
        {
            List<TriangleAbstract> faceTris = faceGroups.get(faceKey);
            // Cycle through the available colors
            ColorAbstract faceColor = faceColors[colorIndex % faceColors.length];

            // Log information about this face
            System.out.println("Face " + colorIndex + " (normal: " + faceKey +
                    ") has " + faceTris.size() + " triangles with color " +
                    faceColor.getR() + "," + faceColor.getG() + "," + faceColor.getB());

            // Process each triangle in this face
            for (TriangleAbstract tri : faceTris)
            {
                VectorAbstract[] vertices = tri.getVertices();

                // Create new vertices with the face color applied
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

    /**
     * Sets all pixels in the framebuffer to black (0,0,0).
     *
     * @param framebuffer 3D array representing the RGB framebuffer
     */
    private static void clearFramebuffer(int[][][] framebuffer)
    {
        for (int i = 0; i < framebuffer.length; i++)
        {
            for (int j = 0; j < framebuffer[i].length; j++)
            {
                for (int k = 0; k < framebuffer[i][j].length; k++)
                {
                    framebuffer[i][j][k] = 0; // Set all values to 0 (black)
                }
            }
        }
    }

    /**
     * Centers and scales the object to fit within the viewport.
     *
     * @param sceneObject The object to center
     * @param width Width of the viewport
     * @param height Height of the viewport
     */
    private static void centerObject(SceneObject sceneObject, int width, int height)
    {
        // Get the current bounding box of the object
        VectorAbstract[] extents = sceneObject.getExtents();

        // Calculate the range in each dimension
        double xRange = Math.abs(extents[1].getX() - extents[0].getX());
        double yRange = Math.abs(extents[1].getY() - extents[0].getY());
        double zRange = Math.abs(extents[1].getZ() - extents[0].getZ());

        // Find the largest dimension to maintain aspect ratio
        double maxRange = Math.max(Math.max(xRange, yRange), zRange);

        // Scale to fit 70% of the viewport width
        double scaleFactor = (width * 0.7) / maxRange;

        // Apply uniform scaling in all dimensions
        sceneObject.scale(new Vector(scaleFactor, scaleFactor, scaleFactor, null));

        // Get the center of the scaled object
        VectorAbstract center = sceneObject.getCenter();

        // Translate to center the object in the viewport
        VectorAbstract translation = new Vector(
                width / 2 - center.getX(),  // Move X to center
                height / 2 - center.getY(), // Move Y to center
                0,                          // Keep Z as is
                null
        );

        sceneObject.translate(translation);
    }
}