package io.github.shumtugle.hazna;

/**
 * The few drawing operations the board needs. The board is painted only
 * through this, so the same painting code runs on the phone and, for checks,
 * on a desktop JVM that writes the frame to an image.
 *
 * Coordinates are in density-independent units. Shapes are added to the
 * current path, then filled, stroked or used as a clip with the current paint.
 */
interface Pen {
    void save();

    void restore();

    void translate(float x, float y);

    void rotate(float degrees);

    void scale(float sx, float sy);

    /** Multiplies the opacity of everything drawn until the matching restore. */
    void alpha(float a);

    void solid(int argb);

    void linear(float x0, float y0, float x1, float y1, int[] colors, float[] stops);

    void radial(float cx, float cy, float r, int[] colors, float[] stops);

    /** Starts a new, empty path; evenOdd makes inner sub-paths cut holes. */
    void path(boolean evenOdd);

    void moveTo(float x, float y);

    void lineTo(float x, float y);

    void cubicTo(float x1, float y1, float x2, float y2, float x, float y);

    void close();

    void rect(float l, float t, float r, float b);

    void oval(float l, float t, float r, float b);

    void round(float l, float t, float r, float b, float rx, float ry);

    void fill();

    void stroke(float width);

    void clip();
}
