package cwlib.io.streams;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import cwlib.io.ValueEnum;
import cwlib.enums.CompressionFlags;
import cwlib.io.streams.MemoryInputStream.SeekMode;
import cwlib.util.Bytes;
import cwlib.types.data.GUID;
import cwlib.types.data.SHA1;

/**
 * Big-endian binary output stream.
 */
public class MemoryOutputStream {
    private byte[] buffer;

    private int offset = 0;
    private int length;
    private byte compressionFlags;

    private boolean isLittleEndian = false;

    /**
     * Creates a memory output stream with specified size.
     * @param size Size of stream
     */
    public MemoryOutputStream(int size) {
        this.length = size;
        this.buffer = new byte[size];
    }

    /**
     * Creates a memory output stream with specified size and compression flags.
     * @param size Size of stream
     * @param compressionFlags Flags for compression methods used
     */
    public MemoryOutputStream(int size, byte compressionFlags) {
        this(size);
        this.compressionFlags = compressionFlags;
    }

    /**
     * Writes an arbitrary number of bytes to the stream.
     * @param value Bytes to write
     * @return This output stream
     */
    public final MemoryOutputStream bytes(byte[] value) {
        System.arraycopy(value, 0, this.buffer, this.offset, value.length);
        this.offset += value.length;
        return this;
    }

    /**
     * Writes a byte array to the stream.
     * @param value Bytes to write
     * @return This output stream
     */
    public final MemoryOutputStream bytearray(byte[] value) {
        this.i32(value.length);
        return this.bytes(value);
    }

    /**
     * Writes a boolean to the stream.
     * @param value Boolean to write
     * @return This output stream
     */
    public final MemoryOutputStream bool(boolean value) {
        return this.u8(value == true ? 1 : 0);
    }

    /**
     * Writes a byte to the stream.
     * @param value Byte to write
     * @return This output stream
     */
    public final MemoryOutputStream i8(byte value) {
        this.buffer[this.offset++] = value;
        return this;
    }

    /**
     * Writes an integer to the stream as a byte.
     * @param value Byte to write
     * @return This output stream
     */
    public final MemoryOutputStream u8(int value) {
        this.buffer[this.offset++] = (byte) (value & 0xFF);
        return this;
    }

    /**
     * Writes a short to the stream.
     * @param value Short to write
     * @return This output stream
     */
    public final MemoryOutputStream i16(short value) {
        if (this.isLittleEndian)
            return this.bytes(Bytes.toBytesLE(value));
        return this.bytes(Bytes.toBytesBE(value));
    }

    /**
     * Writes an integer to the stream as an unsigned short.
     * @param value Short to write
     * @return This output stream
     */
    public final MemoryOutputStream u16(int value) {
        return this.i16((short) (value & 0xFFFF));
    }

    /**
     * Writes a 24-bit unsigned integer to the stream.
     * @param value Integer to write
     * @return This output stream
     */
    public final MemoryOutputStream u24(int value) {
        value &= 0xFFFFFF;
        byte[] b;
        if (this.isLittleEndian) {
            b = new byte[] {
                (byte) (value & 0xFF),
                (byte) (value >>> 8),
                (byte) (value >>> 16),
            };
        } else {
            b = new byte[] {
                (byte) (value >>> 16), 
                (byte) (value >>> 8), 
                (byte) (value & 0xFF)
            };
        }
        return this.bytes(b);
    }

    /**
     * Writes a 32-bit integer to the stream, compressed depending on flags.
     * @param value Integer to write
     * @param force32 Whether or not to write as a 32-bit integer, regardless of compression flags.
     * @return This output stream
     */
    public final MemoryOutputStream i32(int value, boolean force32) {
        if (!force32 && ((this.compressionFlags & CompressionFlags.USE_COMPRESSED_INTEGERS) != 0))
            return this.uleb128(value & 0xFFFFFFFFl);
        if (this.isLittleEndian)
            return this.bytes(Bytes.toBytesLE(value));
        return this.bytes(Bytes.toBytesBE(value));
    }

    /**
     * Writes a long as a 32-bit integer to the stream, compressed depending on flags.
     * @param value Integer to write
     * @param force32 Whether or not to write as a 32-bit integer, regardless of compression flags.
     * @return This output stream
     */
    public final MemoryOutputStream u32(long value, boolean force32) {
        if (!force32 && ((this.compressionFlags & CompressionFlags.USE_COMPRESSED_INTEGERS) != 0))
            return this.uleb128(value & 0xFFFFFFFFl);
        if (this.isLittleEndian)
            return this.bytes(Bytes.toBytesLE((int) (value & 0xFFFFFFFF)));
        return this.bytes(Bytes.toBytesBE((int) (value & 0xFFFFFFFF)));
    }

    /**
     * Writes a long to the stream, compressed depending on flags.
     * @param value Long to write
     * @param force64 Whether or not to write as a 32-bit integer, regardless of compression flags.
     * @return This output stream
     */
    public final MemoryOutputStream i64(long value, boolean force64) {
        if (!force64 && ((this.compressionFlags & CompressionFlags.USE_COMPRESSED_INTEGERS) != 0))
            return this.uleb128(value);
        if (this.isLittleEndian) {
            return this.bytes(new byte[] {
                (byte) (value),
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24),
                (byte) (value >>> 32),
                (byte) (value >>> 40),
                (byte) (value >>> 48),
                (byte) (value >>> 56),
            });
        }
        return this.bytes(new byte[] {
            (byte) (value >>> 56),
            (byte) (value >>> 48),
            (byte) (value >>> 40),
            (byte) (value >>> 32),
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) (value)
        });
    }

    /**
     * Writes an integer to the stream.
     * @param value Integer to write
     * @return This output stream
     */
    public final MemoryOutputStream i32(int value) { return this.i32(value, false); }

    /**
     * Writes a long as an unsigned integer to the stream.
     * @param value Integer to write
     * @return This output stream
     */
    public final MemoryOutputStream u32(long value) { return this.u32(value, false); }

    /**
     * Writes a long to the stream.
     * @param value Long to write
     * @return This output stream
     */
    public final MemoryOutputStream i64(long value) { return this.i64(value, false); }

    /**
     * Writes a variable length quantity to the stream.
     * @param value Long to write
     * @return This output stream
     */
    public final MemoryOutputStream uleb128(long value) {
        value = value & 0xFFFFFFFFFFFFFFFFl;
        if (value == -1 || value == Long.MAX_VALUE) {
            this.bytes(new byte[] { -1, -1, -1, -1, 0xF }); // 0xFFFFFF0F
            return this;
        }
        if (value == 0) return this.u8(0);
        while (true) {
            byte b = (byte) (value & 0x7fl);
            value >>>= 7l;
            if (value > 0l) b |= 128l;
            this.i8(b);
            if (value == 0) break;
        }
        return this;
    }

    /**
     * Writes a 16-bit integer array to the stream.
     * @param values Short array to write
     * @return This output stream
     */
    public final MemoryOutputStream shortarray(short[] values) {
        if (values == null) return this.i32(0);
        this.i32(values.length);
        for (short value : values)
            this.i16(value);
        return this;
    }

    /**
     * Writes a 32-bit integer array to the stream.
     * @param values Integer array to write
     * @return This output stream
     */
    public final MemoryOutputStream intarray(int[] values) {
        if (values == null) return this.i32(0);
        this.i32(values.length);
        for (int value : values)
            this.i32(value);
        return this;
    }

    /**
     * Writes a 64-bit integer array to the stream.
     * @param values Long array to write
     * @return This output stream
     */
    public final MemoryOutputStream longarray(long[] values) {
        if (values == null) return this.i32(0);
        this.i32(values.length);
        for (long value : values)
            this.i64(value);
        return this;
    }

    /**
     * Writes a 32 bit floating point number to the stream.
     * @param value Float to write
     * @return This output stream
     */
    public final MemoryOutputStream f32(float value) {
        return this.i32(Float.floatToIntBits(value), true);
    }

    /**
     * Writes a 32-bit floating point number array to the stream.
     * @param values Float array to write
     * @return This output stream
     */
    public final MemoryOutputStream floatarray(float[] values) {
        if (values == null) return this.i32(0);
        this.i32(values.length);
        for (float value : values)
            this.f32(value);
        return this;
    }

    /**
     * Writes a 2-dimensional floating point vector to the stream.
     * @param value Vector2f to write
     * @return This output stream
     */
    public final MemoryOutputStream v2(Vector2f value) { 
        if (value == null) value = new Vector2f().zero();
        this.f32(value.x); 
        this.f32(value.y); 
        return this;
    }
    
    /**
     * Writes a 3-dimensional floating point vector to the stream.
     * @param value Vector3f to write
     * @return This output stream
     */
    public final MemoryOutputStream v3(Vector3f value) {
        if (value == null) value = new Vector3f().zero();
        this.f32(value.x);
        this.f32(value.y);
        this.f32(value.z);
        return this;
    }

    /**
     * Writes a 4-dimensional floating point vector to the stream.
     * @param value Vector4f to write
     * @return This output stream
     */
    public final MemoryOutputStream v4(Vector4f value) {
        if (value == null) value = new Vector4f().zero();
        this.f32(value.x);
        this.f32(value.y);
        this.f32(value.z);
        this.f32(value.w);
        return this;
    }

    /**
     * Writes a Matrix4x4 to the stream, compressed depending on flags.
     * @param value Matrix4x4 to write
     * @return This output stream
     */
    public final MemoryOutputStream m44(Matrix4f value) {
        if (value == null) value = new Matrix4f().identity();

        float[] identity = new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
        
        float[] values = new float[16];
        value.get(values);
        
        int flags = 0xFFFF;
        if ((this.compressionFlags & CompressionFlags.USE_COMPRESSED_MATRICES) != 0) {
            flags = 0;
            for (int i = 0; i < 16; ++i)
                if (values[i] != identity[i])
                    flags |= (1 << i);
            this.i16((short) flags);
        }

        for (int i = 0; i < 16; ++i)
            if (((flags >>> i) & 1) != 0)
                this.f32(values[i]);
        
        return this;
    }

    /**
     * Writes a string of fixed size to the stream.
     * @param value String to write
     * @param size Fixed size of string
     * @return This output stream
     */
    public final MemoryOutputStream str(String value, int size) {
        if (value == null) return this.bytes(new byte[size]);
        this.bytes(value.getBytes());
        this.pad(size - value.length());
        return this;
    }

    /**
     * Writes a wide string of fixed size to the stream.
     * @param value String to write
     * @param size Fixed size of string
     * @return This output stream
     */
    public final MemoryOutputStream wstr(String value, int size) {
        if (value == null) return this.bytes(new byte[size]);
        this.bytes(value.getBytes(StandardCharsets.UTF_16BE));
        this.pad((size * 2) - (value.length() * 2));
        return this;
    }

    /**
     * Writes a length-prefixed string to the stream.
     * @param value String to write
     * @return This output stream
     */
    public final MemoryOutputStream str(String value) {
        if (value == null) return this.i32(0);
        int size = value.length();
        if ((this.compressionFlags & CompressionFlags.USE_COMPRESSED_INTEGERS) != 0)
            size *= 2;
        this.i32(size);
        return this.bytes(value.getBytes());
    }

    /**
     * Writes a length-prefixed wide string to the stream.
     * @param value String to write
     * @return This output stream
     */
    public final MemoryOutputStream wstr(String value) {
        if (value == null) return this.i32(0);
        int size = value.length();
        if ((this.compressionFlags & CompressionFlags.USE_COMPRESSED_INTEGERS) != 0)
            size *= 2;
        this.i32(size);
        return this.bytes(value.getBytes(StandardCharsets.UTF_16BE));
    }

    /**
     * Writes a SHA1 hash to the stream.
     * @param value SHA1 hash to write
     * @return This output stream
     */
    public final MemoryOutputStream sha1(SHA1 value) {
        return this.bytes(value.getHash());
    }

    /**
     * Writes a GUID (uint32_t) to the stream.
     * @param value GUID to write
     * @param force32 Whether or not to read as a 32 bit integer, regardless of compression flags.
     * @return This output stream
     */
    public final MemoryOutputStream guid(GUID value, boolean force32) {
        if (value == null) return this.u32(0, force32);
        return this.u32(value.getValue(), force32);
    }

    /**
     * Writes a GUID (uint32_t) to the stream.
     * @param value GUID to write
     * @return This output stream
     */
    public final MemoryOutputStream guid(GUID value) { return this.guid(value, false); }


    /**
     * Writes an 8-bit enum value to the stream.
     * @param <T> Type of enum
     * @param value Enum value
     * @return This output stream
     */
    public final <T extends Enum<T> & ValueEnum<Byte>> MemoryOutputStream enum8(T value) {
        return this.i8(value.getValue().byteValue());
    }

    /**
     * Writes an 32-bit enum value to the stream.
     * @param <T> Type of enum
     * @param value Enum value
     * @return This output stream
     */
    public final <T extends Enum<T> & ValueEnum<Integer>> MemoryOutputStream enum32(T value) {
        return this.i32(value.getValue().intValue());
    }

    /**
     * Writes an 32-bit enum value to the stream.
     * @param <T> Type of enum
     * @param value Enum value
     * @return This output stream
     */
    public final <T extends Enum<T> & ValueEnum<Byte>> MemoryOutputStream enumarray(T[] values) {
        if (values == null) return this.i32(0);
        this.i32(values.length);
        for (T value : values)
            this.enum8(value);
        return this;
    }

    /**
     * Writes a series of null characters to the stream.
     * @param size Number of bytes to write
     * @return This output stream
     */
    public final MemoryOutputStream pad(int size) {
        this.offset += size;
        return this;
    }

    /**
     * Shrinks the size of the buffer to the current offset.
     * @return This output stream
     */
    public final MemoryOutputStream shrink() {
        this.buffer = Arrays.copyOfRange(this.buffer, 0, this.offset); 
        return this;
    }

    /**
     * Seeks to position relative to seek mode.
     * @param offset Offset relative to seek position
     * @param mode Seek origin
     */
    public final void seek(int offset, SeekMode mode) {
        if (mode == null)
            throw new NullPointerException("SeekMode cannot be null!");
        if (offset < 0) throw new IllegalArgumentException("Can't seek to negative offsets.");
        switch (mode) {
            case Begin: {
                if (offset >= this.length)
                    throw new IllegalArgumentException("Can't seek past stream length.");
                this.offset = offset;
                break;
            }
            case Relative: {
                int newOffset = this.offset + offset;
                if (newOffset >= this.length || newOffset < 0)
                    throw new IllegalArgumentException("Can't seek outside bounds of stream.");
                this.offset = newOffset;
                break;
            }
            case End: {
                if (offset < 0 || this.length - offset < 0)
                    throw new IllegalArgumentException("Can't seek outside bounds of stream.");
                this.offset = this.length - offset;
                break;
            }
        }
    }

    /**
     * Seeks ahead in stream relative to offset.
     * @param offset Offset to go to
     */
    public final void seek(int offset) { 
        this.seek(offset, SeekMode.Relative);
    }

    public final byte[] getBuffer() { return this.buffer; }
    public final int getOffset() { return this.offset; }
    public final int getLength() { return this.length; }
    public final byte getCompressionFlags() { return this.compressionFlags; }
    public final boolean isLittleEndian() { return this.isLittleEndian; }

    public final void setLittleEndian(boolean value) { this.isLittleEndian = value; }
}
