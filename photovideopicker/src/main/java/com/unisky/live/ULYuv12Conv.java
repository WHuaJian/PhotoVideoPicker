package com.unisky.live;

/**
 * <code>
 * {@link http://developer.android.com/reference/android/graphics/ImageFormat.html#YV12}<br>
 * {@link http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setPreviewFormat%28int%29}
 * </code>
 * 
 * @author kenping.liu
 * @date 2014-8-18
 */
public class ULYuv12Conv
{
    public int width;
    public int height;
    public int semi_width;
    public int semi_height;

    public int y_stride;
    public int y_size;
    public int uv_stride;
    public int uv_size;
    public int size;
    public int u_offset;
    public int v_offset;

    public static int align(int num, int base)
    {
        // int remainder = num % base;
        // return (remainder == 0)? num : (num + base - remainder);
        return (int) Math.ceil(num / 16.0) * 16;
    }

    public void reset(int videoWidth, int videoHeight)
    {
        width = videoWidth;
        height = videoHeight;
        semi_width = width / 2;
        semi_height = height / 2;

        // uv_padding = ((width == 640 && height == 480) || width == 1280 &&
        // height == 720) ? 0 : 1024;
        y_stride = align(width, 16);
        uv_stride = align(y_stride / 2, 16);
        y_size = y_stride * height;
        // int uvoffset = (width*height + 2047) & ~2047;
        uv_size = uv_stride * semi_height;
        u_offset = y_size + uv_size;
        v_offset = y_size;
        size = y_size + uv_size * 2;

    }

    public void swapTo420SemiPlanar(byte[] yv12bytes, byte[] i420bytes)
    {
        if (width == y_stride)
        {
            System.arraycopy(yv12bytes, 0, i420bytes, 0, y_size);
        }
        else
        {
            for (int r = 0; r < height; r++)
            {
                System.arraycopy(yv12bytes, r * y_stride, i420bytes, r * width, width);
            }
        }

        if (uv_stride == semi_width)
        {
            int uv = width * height;
            int u = u_offset;
            int v = v_offset;

            if ((uv_size % 16) == 0)
            {
                while ( uv<size )
                {
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                }
            }
            else if ((uv_size % 8) == 0)
            {
                while ( uv<size )
                {
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                }
            }
            else if ((uv_size % 4) == 0)
            {
                while ( uv<size )
                {
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                }
            }
            else
            {
                while ( uv<size )
                {
                    i420bytes[uv++] = yv12bytes[u++];
                    i420bytes[uv++] = yv12bytes[v++];
                }
            }
        }
        else
        {
            int uv = width * height;
            for (int r = 0; r < semi_height; r++)
            {
                int u = u_offset + r * uv_stride;
                int v = v_offset + r * uv_stride;
                if ((semi_width % 8) == 0)
                {
                    for (int c = 0; c < semi_width; c += 8)
                    {
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                    }
                }
                else if ((semi_width % 4) == 0)
                {
                    for (int c = 0; c < semi_width; c += 4)
                    {
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                    }
                }
                else if ((semi_width % 2) == 0)
                {
                    for (int c = 0; c < semi_width; c += 2)
                    {
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                    }
                }
                else
                {
                    for (int c = 0; c < semi_width; c++)
                    {
                        i420bytes[uv++] = yv12bytes[u++];
                        i420bytes[uv++] = yv12bytes[v++];
                    }
                }
            }
        }
    }

    public void swapTo420Planar(byte[] yv12bytes, byte[] i420bytes)
    {
        // // Y
        // System.arraycopy(yv12bytes, 0, i420bytes, 0, y_size);
        // // Cr (V)
        // System.arraycopy(yv12bytes, cr_offset, i420bytes, cb_offset, c_size);
        // // Cb (U)
        // System.arraycopy(yv12bytes, cb_offset, i420bytes, cr_offset, c_size);

        // Y
        for (int r = 0; r < height; r++)
        {
            System.arraycopy(yv12bytes, r * y_stride, i420bytes, r * width, width);
        }

        int u_offset = width * height;
        int y_offset = u_offset + u_offset / 4;
        for (int r = 0; r < semi_height; r++)
        {
            // U
            System.arraycopy(yv12bytes, u_offset + r * uv_stride, i420bytes, u_offset + r * semi_width, semi_width);
            // V
            System.arraycopy(yv12bytes, v_offset + r * uv_stride, i420bytes, y_offset + r * semi_width, semi_width);
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Yuv12Conv:\n{");
        sb.append("\n    width=").append(width);
        sb.append("\n    height=").append(height);
        sb.append("\n    y_stride=").append(y_stride);
        sb.append("\n    semi_width=").append(semi_width);
        sb.append("\n    semi_height=").append(semi_height);
        sb.append("\n    y_size=").append(y_size);
        sb.append("\n    uv_stride=").append(uv_stride);
        sb.append("\n    uv_size=").append(uv_size);
        sb.append("\n    size=").append(size);
        sb.append("\n    u_offset=").append(u_offset);
        sb.append("\n    v_offset=").append(v_offset);
        sb.append("\n}\n");
        return sb.toString();
    }
}