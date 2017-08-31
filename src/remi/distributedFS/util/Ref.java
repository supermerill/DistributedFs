package remi.distributedFS.util;

public class Ref<T> {
	private T data;

	public Ref(T obj){
		this.data = obj;
	}
	
	public T get(){
		return data;
	}
	
	public void set(T obj){
		this.data = obj;
	}
}
