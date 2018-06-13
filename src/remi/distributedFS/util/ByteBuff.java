package remi.distributedFS.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Create big-indian buffer from other datas
 * 
 * @author Remi DURAND (remi-j.durand@thalesgroup.com)
 * 
 */
public class ByteBuff
{
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private int position = 0;
	private int limit = -1;
	private byte[] buffer;
	
	/**
	 * Init an short buffer
	 */
	public ByteBuff()
	{
		this.buffer = new byte[16];
	}
	
	/**
	 * Init a buffer with a initial size and limit to maximum.
	 * 
	 * @param initialCapacity
	 *            size of the first backing array.
	 */
	public ByteBuff(final int initialCapacity)
	{
		this.buffer = new byte[initialCapacity];
		this.limit = initialCapacity;
	}
	
	/**
	 * Copy the buffIn buffer into this buffer.
	 * 
	 * @param buffIn
	 *            buffer for intitialization
	 */
	public ByteBuff(final ByteBuffer buffIn)
	{
		this(buffIn, false);
	}
	
	/**
	 * Copy the buffIn buffer into this buffer.
	 * After that, pos is at 0 and limit at buffIn.length.
	 * 
	 * @param buffIn
	 *            buffer for intitialization
	 */
	public ByteBuff(final byte[] buffIn)
	{
		this(buffIn.length);
		this.put(buffIn);
		this.rewind();
	}
	
	/**
	 * Copy the buffIn buffer, or just link it if unsafe is true (if we increase the size, a new byte[] is created, not
	 * linked with the ByteBuffer).
	 * 
	 * @param buffIn
	 *            buffe or intiialization
	 * @param unsafe
	 *            true for sharing an array, for a moment. False to copy.
	 */
	public ByteBuff(final ByteBuffer buffIn, final boolean unsafe)
	{
		if (unsafe)
		{
			this.buffer = buffIn.array();
			this.position = buffIn.position();
			this.limit = buffIn.limit();
		}
		else
		{
			this.buffer = buffIn.array().clone();
			this.position = buffIn.position();
			this.limit = buffIn.limit();
		}
	}
	
	/**
	 * Set limit to at least position() + size.
	 * Shouldn't be called/used directly. But sometimes i try to re-create this so here it is, available.
	 * @param size min size available.
	 */
	public void expand(final int size)
	{
		if (limit < position + size)
		{
			limit = position + size;
		}
		while (buffer.length < position + size)
		{
			doubleSize();
		}
	}
	
	private void readcheck(final int size)
	{
		if (limit < position + size)
		{
			throw new IndexOutOfBoundsException("Error, ByteBuff.limit() = " + limit + " < " + (position + size));
		}
	}
	
	private void doubleSize()
	{
		final byte[] newBuff = new byte[buffer.length * 2];
		System.arraycopy(buffer, 0, newBuff, 0, position);
		buffer = newBuff;
	}
	
	public final int position()
	{
		return position;
	}
	
	public final ByteBuff position(final int newPosition)
	{
		this.position = newPosition;
		return this;
	}
	
	/**
	 * Get the limit, the limit is only used for create().
	 * 
	 * @return the limit
	 */
	public final int limit()
	{
		return limit;
	}
	
	/**
	 * Set the limit, the limit is only used for create().
	 * 
	 * @return this
	 */
	public final ByteBuff limit(final int newLImit)
	{
		while (buffer.length < newLImit)
		{
			doubleSize();
		}
		this.limit = newLImit;
		return this;
	}
	
	/**
	 * Set the limit to the actual position and set the position to 0. This method is useful to call a create() after.
	 * 
	 * @return this
	 */
	public final ByteBuff flip()
	{
		return limit(position()).rewind();
	}
	
	public final ByteBuff rewind()
	{
		position = 0;
		return this;
	}
	
	public byte get()
	{
		readcheck(1);
		// return buffer[position] then incrementing position
		return buffer[position++];
	}
	
	public ByteBuff put(final byte b)
	{
		expand(1);
		buffer[position++] = b;
		return this;
	}
	
	public byte[] get(final int nbElt)
	{
		readcheck(nbElt);
		final byte[] newBuff = new byte[nbElt];
		System.arraycopy(buffer, position, newBuff, 0, nbElt);
		position += nbElt;
		return newBuff;
	}
	
	/**
	 * Put data into dest (from position).
	 * @param dest destination array
	 * @param destPos start idx in dest
	 * @param length nb bytes to copy
	 * @return this
	 */
	public ByteBuff get(final byte[] dest, final int destPos, final int length)
	{
		readcheck(length);
		System.arraycopy(buffer, position, dest, destPos, length);
		position += length;
		return this;
	}
	
	public final ByteBuff put(final byte[] src)
	{
		expand(src.length);
		System.arraycopy(src, 0, buffer, position, src.length);
		position += src.length;
		return this;
	}
	
	/**
	 * Copy the scr.limit-src.position bytes from src.position to src.limit into this.position.
	 * @param src soruce array
	 * @return this.
	 */
	public final ByteBuff put(final ByteBuff src)
	{
		int size = src.limit-src.position;
		expand(size);
		System.arraycopy(src.array(), src.position, buffer, position, size);
		position += size;
		src.position += size;
		return this;
	}
	
	public final ByteBuff put(final ByteBuff src, int size)
	{
		readcheck(size);
		expand(size);
		System.arraycopy(src.array(), src.position, buffer, position, size);
		position += size;
		src.position += size;
		return this;
	}
	
	public final ByteBuff put(final byte[] src, final int srcPos, final int length)
	{
		expand(length);
		System.arraycopy(src, srcPos, buffer, position, length);
		position += length;
		return this;
	}
	
	/**
	 * Getter to the raw array sed to store data between 0 and limit.
	 * @return The raw array.
	 */
	public final byte[] array()
	{
		return buffer;
	}
	
	@Override
	public String toString()
	{
		return Arrays.toString(buffer);
	}
	
	@Override
	public int hashCode()
	{
		return buffer.hashCode();
	}
	
	@Override
	public boolean equals(final Object ob)
	{
		return buffer.equals(ob);
	}
	
	public char getChar()
	{
		return (char) getShort();
	}
	
	public ByteBuff putChar(final char value)
	{
		return putShort((short) value);
	}
	
	public short getShort()
	{
		readcheck(2);
		final short sh = (short) ((buffer[position++] & 0xFF) << 8 | buffer[position++] & 0xFF);
		return sh;
	}
	
	public ByteBuff putShort(final short value)
	{
		expand(2);
		buffer[position++] = (byte) ((value & 0xFF00) >> 8);
		buffer[position++] = (byte) (value & 0xFF);
		return this;
	}
	
	public static int getByteLengthTrailInt(final int num)
	{
		if ((byte) (num & 0x0000007F | (num & 0x00000040) << 1) == num)
		{
			// 7 bit sur un byte (8)
			return 1;
		}
		else if ((short) (num & 0x00003FFF | (num & 0x00002000) << 1 | (num & 0x00002000) << 2) == num)
		{
			// 14 bitsdeux byte (16)
			return 2;
		}
		else if ((num & 0x001FFFFF | ((num & 0x00100000) == 0 ? 0x00
				: 0XFFF00000)) == num)
		{
			// 21 bit sur trois byte (24)
			return 3;
		}
		else if ((num & 0x0FFFFFFF | ((num & 0x08000000) == 0 ? 0x00
				: 0XF0000000)) == num)
		{
			// 28 bits stocké sur quatre byte (32)
			return 4;
		}
		else
		{
			// quatre byte (32) stocké sur cinq (40)
			return 5;
		}
		
	}
	
	// TODO améliorer le truc pour les chiffres négatifs
	public void putTrailInt(final int num)
	{
		if ((byte) (num & 0x0000007F | (num & 0x00000040) << 1) == num)
		{
			// 7 bit sur un byte (8)
			expand(1);
			buffer[position++] = (byte) (num & 0x7F);
		}
		else if ((short) (num & 0x00003FFF | (num & 0x00002000) << 1 | (num & 0x00002000) << 2) == num)
		{
			// 14 bitsdeux byte (16)
			expand(2);
			buffer[position++] = (byte) ((num & 0x3F00) >> 8 | 0x80);
			buffer[position++] = (byte) (num & 0xFF);
		}
		else if ((num & 0x001FFFFF | ((num & 0x00100000) == 0 ? 0x00
				: 0XFFF00000)) == num)
		{
			// 21 bit sur trois byte (24)
			expand(3);
			buffer[position++] = (byte) ((num & 0x1F0000) >> 16 | 0xC0);
			buffer[position++] = (byte) ((num & 0xFF00) >> 8);
			buffer[position++] = (byte) (num & 0xFF);
		}
		else if ((num & 0x0FFFFFFF | ((num & 0x08000000) == 0 ? 0x00
				: 0XF0000000)) == num)
		{
			// 28 bits stocké sur quatre byte (32)
			// buff.putInt(num&0x0FFFFFFF | 0xE0000000);
			expand(4);
			buffer[position++] = (byte) ((num & 0x0F000000) >> 24 | 0xE0);
			buffer[position++] = (byte) ((num & 0xFF0000) >> 16);
			buffer[position++] = (byte) ((num & 0xFF00) >> 8);
			buffer[position++] = (byte) (num & 0xFF);
		}
		else
		{
			// quatre byte (32) stocké sur cinq (40)
			expand(5);
			buffer[position++] = (byte) 0xF0;
			buffer[position++] = (byte) ((num & 0xFF000000) >> 24);
			buffer[position++] = (byte) ((num & 0xFF0000) >> 16);
			buffer[position++] = (byte) ((num & 0xFF00) >> 8);
			buffer[position++] = (byte) (num & 0xFF);
		}
	}
	
	public int getTrailInt()
	{
		readcheck(1);
		final byte b = buffer[position++];
		if ((b & 0x80) == 0)
		{
			return (byte) (b | (b & 0x40) << 1);
		}
		else if ((b & 0x40) == 0)
		{
			readcheck(1);
			final byte b2 = buffer[position++];
			return (short) ((b & 0x3F) << 8 | b2 & 0xFF | (b & 0x20) << 9 | (b & 0x20) << 10);
		}
		else if ((b & 0x20) == 0)
		{
			readcheck(2);
			final byte b2 = buffer[position++];
			final byte b3 = buffer[position++];
			return (b & 0x1F) << 16 | (b2 & 0xFF) << 8 | b3 & 0xFF & 0xFF | ((b & 0x10) == 0 ? 0x00
					: 0XFFF00000);
		}
		else if ((b & 0x10) == 0)
		{
			readcheck(3);
			final byte b2 = buffer[position++];
			final byte b3 = buffer[position++];
			final byte b4 = buffer[position++];
			return (b & 0X0F) << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8
					| b4 & 0xFF | ((b & 0x08) == 0 ? 0x00 : 0XF0000000);
		}
		else if ((b & 0x08) == 0)
		{
			readcheck(4);
			return (buffer[position++] & 0xFF) << 24
					| (buffer[position++] & 0xFF) << 16
					| (buffer[position++] & 0xFF) << 8
					| buffer[position++] & 0xFF;
		}
		else
		{
			// error, unknow int
			throw new RuntimeException("Error, unknow trail int : "
					+ Integer.toHexString(b & 0x000000FF));
		}
		
	}
	
	public int getInt()
	{
		readcheck(4);
		return (buffer[position++] & 0xFF) << 24
				| (buffer[position++] & 0xFF) << 16
				| (buffer[position++] & 0xFF) << 8
				| buffer[position++] & 0xFF;
	}
	
	public ByteBuff putInt(final int value)
	{
		expand(4);
		buffer[position++] = (byte) ((value & 0xFF000000) >> 24);
		buffer[position++] = (byte) ((value & 0xFF0000) >> 16);
		buffer[position++] = (byte) ((value & 0xFF00) >> 8);
		buffer[position++] = (byte) (value & 0xFF);
		return this;
	}
	
	public long getLong()
	{
		readcheck(8);
		return (buffer[position++] & 0xFFL) << 56
				| (buffer[position++] & 0xFFL) << 48
				| (buffer[position++] & 0xFFL) << 40
				| (buffer[position++] & 0xFFL) << 32
				| (buffer[position++] & 0xFFL) << 24
				| (buffer[position++] & 0xFFL) << 16
				| (buffer[position++] & 0xFFL) << 8
				| buffer[position++] & 0xFFL;
	}
	
	public ByteBuff putLong(final long value)
	{
		expand(8);
		buffer[position++] = (byte) ((value & 0xFF00000000000000l) >> 56);
		buffer[position++] = (byte) ((value & 0xFF000000000000l) >> 48);
		buffer[position++] = (byte) ((value & 0xFF0000000000l) >> 40);
		buffer[position++] = (byte) ((value & 0xFF00000000l) >> 32);
		buffer[position++] = (byte) ((value & 0xFF000000l) >> 24);
		buffer[position++] = (byte) ((value & 0xFF0000l) >> 16);
		buffer[position++] = (byte) ((value & 0xFF00l) >> 8);
		buffer[position++] = (byte) (value & 0xFFl);
		return this;
	}
	
	public float getFloat()
	{
		return Float.intBitsToFloat(getInt());
	}
	
	public ByteBuff putFloat(final float value)
	{
		return putInt(Float.floatToRawIntBits(value));
	}
	
	public double getDouble()
	{
		return Double.longBitsToDouble(getLong());
	}
	
	public ByteBuff putDouble(final double value)
	{
		return putLong(Double.doubleToLongBits(value));
	}

	public ByteBuff putUTF8(final String str)
	{
		final ByteBuffer buffUTF8 = UTF8.encode(str);
		putTrailInt(buffUTF8.remaining());
		put(buffUTF8.array(), 0, buffUTF8.remaining());
		return this;
	}
	
	public String getUTF8()
	{
		final int size = getTrailInt();
		final CharBuffer buffUTF8 = UTF8.decode(ByteBuffer.wrap(get(size)));
		return buffUTF8.toString();
	}

	public ByteBuff putShortUTF8(final String str)
	{
		final ByteBuffer buffUTF8 = UTF8.encode(str);
		putShort((short) buffUTF8.remaining());
		put(buffUTF8.array(), 0, buffUTF8.remaining());
		return this;
	}

	public String getShortUTF8()
	{
		final int size = getShort();
		final CharBuffer buffUTF8 = UTF8.decode(ByteBuffer.wrap(get(size)));
		return buffUTF8.toString();
	}
	
	/**
	 * Wrap a buffer to a copy of this buffer, start at position, and a maximum size of limit.
	 * 
	 * @return the buffer copy.
	 */
	public ByteBuffer create()
	{
		return ByteBuffer.wrap(get(limit));
	}
	
	/**
	 * Wrap a buffer to this buffer, start at position and the limit set at this limit. !UNSAFE! Do not do this and put
	 * after that some data into this ByteBuff, it can expand and loose the shared array for a bigger one.
	 * 
	 * @return the buffer with the same backing array as this instance.
	 */
	public ByteBuffer wrap()
	{
		final ByteBuffer buff = ByteBuffer.wrap(array());
		buff.position(this.position);
		buff.limit(this.limit);
		return buff;
	}
	
	/**
	 * Create a byteBuff with other position and limit but the same backing buffer. The new values will be <= current
	 * limit. All changes make to the backing buffer are reported.
	 * 
	 * @param start
	 *            buffer start position
	 * @param length
	 *            Set the new limit to position + length
	 * @return A ByteBuff with same buffer but another position and limit.
	 */
	public ByteBuff subBuff(final int start, final int length)
	{
		final ByteBuff retVal = new ByteBuff();
		retVal.buffer = this.buffer;
		retVal.position = Math.min(start, this.limit);
		retVal.limit = Math.min(this.limit, start + length);
		return retVal;
	}

	/**
	 * Clear it. pos and limit are now at 0.
	 * @return
	 */
	public ByteBuff reset() {
		limit = 0;
		position = 0;
		return this;
	}

	/**
	 * wrap this into a bytebuffer.
	 * @return
	 */
	public ByteBuffer toByteBuffer() {
		ByteBuffer buff = ByteBuffer.wrap(this.buffer);
		buff.position(position);
		if(limit<0) buff.limit(this.buffer.length);
		else	buff.limit(limit);
		return buff;
	}

	/**
	 * copy content in an array of limit()-position() size.
	 * @return byte[]
	 */
	public byte[] toArray() {
		readcheck(limit()-position());
		byte[] newBuff = new byte[limit()-position()];
		System.arraycopy(buffer, position, newBuff, 0, limit-position);
		return newBuff;
	}

	/**
	 * Read nbBytes from stream an put it into this.
	 * @param in
	 * @param nbBytes
	 * @return this
	 * @throws IOException
	 */
	public ByteBuff read(InputStream in, int nbBytes) throws IOException{
		expand(nbBytes); 
		in.read(array(),position(),nbBytes);
		position += nbBytes;
		return this;
	}
	
	/**
	 * Read limit()-position() bytes from stream an put it into this.
	 * @param in
	 * @return this
	 * @throws IOException
	 */
	public ByteBuff read(InputStream in) throws IOException{
		readcheck(limit()-position());
		in.read(array(),position(),limit()-position());
		position = limit;
		return this;
	}
	
	/**
	 * write nbBytes from this to outputBuffer
	 * @param out
	 * @param nbBytes
	 * @return
	 * @throws IOException
	 */
	public ByteBuff write(OutputStream out, int nbBytes) throws IOException{
		readcheck(nbBytes);
		out.write(array(),position(),nbBytes);
		position += nbBytes;
		return this;
	}

	public ByteBuff write(BufferedOutputStream out) throws IOException {
		readcheck(limit()-position());
		out.write(array(),position(),limit()-position());
//		System.out.println("write"+Arrays.toString(toArray()));
		position = limit;
		return this;
	}
	
}
