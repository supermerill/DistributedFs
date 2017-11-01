package remi.distributedFS.db.impl.bigdata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import remi.distributedFS.util.ByteBuff;

public class ChannelIterator implements Iterator<ByteBuff> {

	final FileChannel channel;
	ByteBuffer readInt = ByteBuffer.allocate(4);
	ByteBuff buffer = new ByteBuff();

	public ChannelIterator(FileChannel channel) {
		super();
		this.channel = channel;
	}

	@Override
	public boolean hasNext() {
		try {
			if(channel.isOpen()) {
				if(channel.position()+4 < channel.size()) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return false;
	}

	@Override
	public ByteBuff next() {
		try {
			if(4 > channel.size() - channel.position()) {
				throw new RuntimeException("Error, channel has size "+channel.size()+" and i want to read an int from "+channel.position());
			}
			readInt.rewind();
			channel.read(readInt);
			readInt.rewind();
			int nbBytes = readInt.getInt();
			if(nbBytes > channel.size() - channel.position()) {
				throw new RuntimeException("Error, channel has size "+channel.size()+" and i want to read from "+channel.position()+" for "+nbBytes);
			}
			buffer.rewind().limit(nbBytes);
			channel.read(buffer.toByteBuffer());
			return buffer.rewind();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
