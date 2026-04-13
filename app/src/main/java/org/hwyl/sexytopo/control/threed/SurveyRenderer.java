package org.hwyl.sexytopo.control.threed;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Map;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

public class SurveyRenderer implements GLSurfaceView.Renderer {

    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;"
                    + "attribute vec4 vPosition;"
                    + "void main() {"
                    + "  gl_Position = uMVPMatrix * vPosition;"
                    + "  gl_PointSize = 8.0;"
                    + "}";

    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;"
                    + "uniform vec4 vColor;"
                    + "void main() {"
                    + "  gl_FragColor = vColor;"
                    + "}";

    private static final float INITIAL_CAMERA_DISTANCE = 50f;
    private static final float MIN_CAMERA_DISTANCE = 1f;
    private static final float MAX_CAMERA_DISTANCE = 500f;

    // Camera state
    private float cameraAngleX = (float) Math.toRadians(45);
    private float cameraAngleY = (float) Math.toRadians(45);
    private float cameraDistance = INITIAL_CAMERA_DISTANCE;
    private float cameraPanX = 0f;
    private float cameraPanY = 0f;
    private float cameraPanZ = 0f;

    // Matrices
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // OpenGL handles
    private int programHandle;
    private int mvpMatrixHandle;
    private int positionHandle;
    private int colourHandle;

    // Geometry buffers
    private FloatBuffer legVertexBuffer;
    private int legVertexCount;
    private FloatBuffer splayVertexBuffer;
    private int splayVertexCount;
    private FloatBuffer stationVertexBuffer;
    private int stationVertexCount;

    // Survey data
    private Space<Coord3D> space;
    private boolean showSplays = true;
    private boolean geometryDirty = true;

    // Centre offset (to centre the survey at origin)
    private float centreX, centreY, centreZ;

    // Colours (RGBA floats)
    private float[] legColour = {0.8f, 0.2f, 0.2f, 1.0f};
    private float[] splayColour = {0.6f, 0.6f, 0.6f, 0.6f};
    private float[] stationColour = {0.2f, 0.4f, 0.8f, 1.0f};
    private float[] backgroundColour = {1.0f, 1.0f, 1.0f, 1.0f};

    public void setSurveyData(Space<Coord3D> space) {
        this.space = space;
        this.geometryDirty = true;
    }

    public void setShowSplays(boolean show) {
        this.showSplays = show;
    }

    public boolean getShowSplays() {
        return showSplays;
    }

    public void setBackgroundColour(float r, float g, float b) {
        backgroundColour[0] = r;
        backgroundColour[1] = g;
        backgroundColour[2] = b;
    }

    public void rotateBy(float dx, float dy) {
        cameraAngleY += dx * 0.01f;
        cameraAngleX += dy * 0.01f;
        // Clamp vertical angle to avoid flipping
        cameraAngleX = Math.max(0.01f, Math.min((float) Math.PI - 0.01f, cameraAngleX));
    }

    public void zoomBy(float factor) {
        cameraDistance *= factor;
        cameraDistance =
                Math.max(MIN_CAMERA_DISTANCE, Math.min(MAX_CAMERA_DISTANCE, cameraDistance));
    }

    public void panBy(float dx, float dy) {
        // Transform screen-space delta to world-space using inverse view matrix
        float[] inverseView = new float[16];
        boolean success = Matrix.invertM(inverseView, 0, viewMatrix, 0);

        if (!success) {
            return;
        }

        float scale = cameraDistance * 0.0005f;
        float[] screenDelta = {dx * scale, -dy * scale, 0, 0};

        float[] worldDelta = new float[4];
        Matrix.multiplyMV(worldDelta, 0, inverseView, 0, screenDelta, 0);

        cameraPanX += worldDelta[0];
        cameraPanY += worldDelta[1];
        cameraPanZ += worldDelta[2];
    }

    public void resetView() {
        cameraAngleX = (float) Math.toRadians(45);
        cameraAngleY = (float) Math.toRadians(45);
        cameraDistance = INITIAL_CAMERA_DISTANCE;
        cameraPanX = 0f;
        cameraPanY = 0f;
        cameraPanZ = 0f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glLineWidth(2.0f);

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, vertexShader);
        GLES20.glAttachShader(programHandle, fragmentShader);
        GLES20.glLinkProgram(programHandle);

        mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
        colourHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 0.1f, 1000f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(
                backgroundColour[0], backgroundColour[1], backgroundColour[2], backgroundColour[3]);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (space == null) {
            return;
        }

        if (geometryDirty) {
            buildGeometry();
            geometryDirty = false;
        }

        // Camera position (spherical coordinates)
        // Survey data uses Z-up, so camera orbits around Z axis
        float eyeX = cameraDistance * (float) (Math.sin(cameraAngleX) * Math.sin(cameraAngleY));
        float eyeY = cameraDistance * (float) (Math.sin(cameraAngleX) * Math.cos(cameraAngleY));
        float eyeZ = cameraDistance * (float) (Math.cos(cameraAngleX));

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0, 0, 0, 0, 0, 1);

        // Model matrix: translate to centre the survey and apply pan
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(
                modelMatrix,
                0,
                -centreX + cameraPanX,
                -centreY + cameraPanY,
                -centreZ + cameraPanZ);

        // MVP = Projection * View * Model
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES20.glUseProgram(programHandle);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw legs
        if (legVertexBuffer != null && legVertexCount > 0) {
            drawLines(legVertexBuffer, legVertexCount, legColour);
        }

        // Draw splays
        if (showSplays && splayVertexBuffer != null && splayVertexCount > 0) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            drawLines(splayVertexBuffer, splayVertexCount, splayColour);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        // Draw stations
        if (stationVertexBuffer != null && stationVertexCount > 0) {
            drawPoints(stationVertexBuffer, stationVertexCount, stationColour);
        }
    }

    private void buildGeometry() {
        if (space == null) {
            return;
        }

        Map<Station, Coord3D> stations = space.getStationMap();
        Map<Leg, Line<Coord3D>> legs = space.getLegMap();

        // Calculate centre of bounding box
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (Coord3D coord : stations.values()) {
            minX = Math.min(minX, coord.x);
            maxX = Math.max(maxX, coord.x);
            minY = Math.min(minY, coord.y);
            maxY = Math.max(maxY, coord.y);
            minZ = Math.min(minZ, coord.z);
            maxZ = Math.max(maxZ, coord.z);
        }

        centreX = (minX + maxX) / 2f;
        centreY = (minY + maxY) / 2f;
        centreZ = (minZ + maxZ) / 2f;

        // Set initial camera distance based on survey size
        float extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        if (extent > 0) {
            cameraDistance = extent * 1.5f;
        }

        // Build leg and splay buffers
        int legCount = 0;
        int splayCount = 0;
        for (Leg leg : legs.keySet()) {
            if (leg.hasDestination()) {
                legCount++;
            } else {
                splayCount++;
            }
        }

        float[] legVertices = new float[legCount * 6]; // 2 points * 3 coords per leg
        float[] splayVertices = new float[splayCount * 6];
        int legIndex = 0;
        int splayIndex = 0;

        for (Map.Entry<Leg, Line<Coord3D>> entry : legs.entrySet()) {
            Leg leg = entry.getKey();
            Line<Coord3D> line = entry.getValue();
            Coord3D start = line.getStart();
            Coord3D end = line.getEnd();

            if (leg.hasDestination()) {
                legVertices[legIndex++] = start.x;
                legVertices[legIndex++] = start.y;
                legVertices[legIndex++] = start.z;
                legVertices[legIndex++] = end.x;
                legVertices[legIndex++] = end.y;
                legVertices[legIndex++] = end.z;
            } else {
                splayVertices[splayIndex++] = start.x;
                splayVertices[splayIndex++] = start.y;
                splayVertices[splayIndex++] = start.z;
                splayVertices[splayIndex++] = end.x;
                splayVertices[splayIndex++] = end.y;
                splayVertices[splayIndex++] = end.z;
            }
        }

        legVertexBuffer = createFloatBuffer(legVertices);
        legVertexCount = legCount * 2;
        splayVertexBuffer = createFloatBuffer(splayVertices);
        splayVertexCount = splayCount * 2;

        // Build station buffer
        float[] stationVertices = new float[stations.size() * 3];
        int stationIndex = 0;
        for (Coord3D coord : stations.values()) {
            stationVertices[stationIndex++] = coord.x;
            stationVertices[stationIndex++] = coord.y;
            stationVertices[stationIndex++] = coord.z;
        }

        stationVertexBuffer = createFloatBuffer(stationVertices);
        stationVertexCount = stations.size();
    }

    private void drawLines(FloatBuffer buffer, int vertexCount, float[] colour) {
        buffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer);
        GLES20.glUniform4fv(colourHandle, 1, colour, 0);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawPoints(FloatBuffer buffer, int vertexCount, float[] colour) {
        buffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer);
        GLES20.glUniform4fv(colourHandle, 1, colour, 0);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.position(0);
        return floatBuffer;
    }

    private static int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
