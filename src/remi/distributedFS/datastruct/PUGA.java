package remi.distributedFS.datastruct;

public class PUGA {
	public boolean computerRead = false;
	public boolean computerWrite = false;
	public boolean userRead = false;
	public boolean userWrite = false;
	public boolean groupRead = false;
	public boolean groupWrite = false;
	public boolean allRead = false;
	public boolean allWrite = false;
	public boolean canExec = false;

	
	public PUGA(short data){
		computerRead = (data & 0x01) != 0;
		computerWrite = (data & 0x02) != 0;
		userRead = (data & 0x04) != 0;
		userWrite = (data & 0x08) != 0;
		groupRead = (data & 0x10) != 0;
		groupWrite = (data & 0x20) != 0;
		allRead = (data & 0x40) != 0;
		allWrite = (data & 0x80) != 0;
		canExec = (data & 0x100) != 0;
	}
	
	public short toShort(){
		short b = 0;
		b |= computerRead?0x01:0;
		b |= computerWrite?0x02:0;
		b |= userRead?0x04:0;
		b |= userWrite?0x08:0;
		b |= groupRead?0x10:0;
		b |= groupWrite?0x20:0;
		b |= allRead?0x40:0;
		b |= allWrite?0x80:0;
		return b;
	}

	@Override
	public String toString() {
		return "PUGA [computerRead=" + computerRead + ", computerWrite=" + computerWrite + ", userRead=" + userRead
				+ ", userWrite=" + userWrite + ", groupRead=" + groupRead + ", groupWrite=" + groupWrite + ", allRead="
				+ allRead + ", allWrite=" + allWrite + ", canExec=" + canExec + "]";
	}
	
	
}
